(ns uni.style.impl.build
  "Dependency ordering, CSS output, and build hook implementations."
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [uni.style.impl.registry :as registry]
            [uni.style.impl.theme-css :as theme-css]))

(defn ns-order-from-build-state [{:keys [sources build-sources]}]
  (->> build-sources
       (keep (fn [id] (get-in sources [id :ns])))
       (map-indexed (fn [index ns-sym] [ns-sym index]))
       (into {})))

(defn- source-sort-key [ns-order {:keys [ns file loc]}]
  [(get ns-order ns Long/MAX_VALUE)
   (or file "")
   (:line loc 0)
   (:column loc 0)])

(defn- entry-sort-key [ns-order]
  (fn [{:keys [atomic? sort-meta] :as entry}]
    (if (and atomic? sort-meta)
      [1
       (:condition-rank sort-meta)
       (:property-rank sort-meta)
       (name (:property sort-meta))
       (source-sort-key ns-order entry)]
      [0 (source-sort-key ns-order entry)])))

(defn- dedupe-atomic-styles [styles]
  (:styles
    (reduce (fn [{:keys [atomic-css] :as result}
                 {:keys [atomic? class-name css-str] :as style}]
              (if-not atomic?
                (update result :styles conj style)
                (if-let [existing-css (get atomic-css class-name)]
                  (if (= existing-css css-str)
                    result
                    (throw (ex-info "Atomic CSS class has conflicting declarations"
                                    {:class-name class-name
                                     :existing-css existing-css
                                     :css css-str})))
                  (-> result
                      (update :styles conj style)
                      (assoc-in [:atomic-css class-name] css-str)))))
            {:atomic-css {} :styles []}
            styles)))

(defn- ordered-styles [styles-registry ns-order]
  (->> styles-registry
       (mapcat (fn [[ns-sym styles]]
                 (map (fn [[class-name entry]]
                        (assoc entry :ns ns-sym :class-name class-name))
                      styles)))
       theme-css/expand-definitions
       (sort-by (entry-sort-key (or ns-order {})))
       dedupe-atomic-styles))

(defn bundle-css [styles-registry ns-order]
  (str/join "" (map :css-str (ordered-styles styles-registry ns-order))))

(defn bundle-css! [styles-registry {:keys [output-to ns-order]}]
  (when (seq styles-registry)
    (io/make-parents output-to)
    (spit output-to (bundle-css styles-registry ns-order))))

(spec/def ::output-to string?)
(spec/def ::qualified-component-classes boolean?)
(spec/def ::short-class-names boolean?)
(spec/def ::build-options
  (spec/keys :req-un [::output-to]
             :opt-un [::qualified-component-classes ::short-class-names]))

(defn- build! [build-state styles]
  (let [dev? (= :dev (:mode build-state))
        options (-> build-state :shadow.build/config :css-options)
        release-options (-> build-state :shadow.build/config :release :css-options)
        options (cond-> options (not dev?) (into release-options))
        options (assoc options :ns-order (ns-order-from-build-state build-state))]
    (if (spec/valid? ::build-options options)
      (bundle-css! styles options)
      (log/error (str "uni.style [CONFIG ERROR]\n"
                      (spec/explain-str ::build-options options))))))

(defonce ^:private esm-written
  ;; The styles of the CSS esm-hook last wrote, by :output-to.
  (atom {}))

(def ^:private compiler-css-options
  ;; Read by uni.style's macros from the compiler options' :css-options, not
  ;; from esm-hook's options: the hook runs after the macros named the classes.
  #{:qualified-component-classes :short-class-names})

(defn- esm-hook-options-error
  "Why esm-hook's options are invalid, or nil."
  [options]
  (if-let [misplaced (seq (filter (set (keys options)) compiler-css-options))]
    (str "uni.style [CONFIG ERROR]\n"
         (str/join ", " (sort misplaced))
         " belong under the compiler options' :css-options, not esm-hook's options")
    (when-not (spec/valid? ::build-options options)
      (str "uni.style [CONFIG ERROR]\n"
           (spec/explain-str ::build-options options)))))

(defn esm-hook
  "Writes the CSS of the namespaces (in dependency order) of a ClojureScript
  ES module build (cljf.esm) to the :output-to of options. After a form
  evaluated at the REPL (trigger :repl-eval) the CSS is only written again
  when the styles of the changed namespaces differ from those written."
  ([compiler-env namespaces options]
   (esm-hook compiler-env namespaces options :build namespaces))
  ([compiler-env namespaces options trigger changed]
   (if-let [error (esm-hook-options-error options)]
     (log/error error)
     (let [options (assoc options :ns-order (zipmap namespaces (range)))
           registry (registry/namespaces-registry compiler-env namespaces)
           written (get @esm-written (:output-to options))]
       (when-not (and (= :repl-eval trigger)
                      written
                      (= (select-keys registry changed) (select-keys written changed)))
         (bundle-css! registry options)
         (swap! esm-written assoc (:output-to options) registry))))))

(defn hook [build-state]
  (let [styles (registry/build-registry build-state)]
    (case (:shadow.build/stage build-state)
      :compile-finish
      (when (= :dev (:mode build-state))
        (build! build-state styles))

      :flush
      (when-not (= :dev (:mode build-state))
        (build! build-state styles))
      nil))
  build-state)
