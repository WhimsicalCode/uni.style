(ns uni.style.impl.registry
  "Host-specific uni.style registrations and conflict detection."
  (:require [clojure.string :as str]
            [uni.style.impl.host :as host]
            [uni.style.impl.serialize :as serialize]))

(def ^:dynamic *atomic-meta* nil)
(def ^:dynamic *jvm-styles* nil)

(def ^:private styles-key ::styles)

(defn- styles-path [ns-sym]
  [:cljs.analyzer/namespaces ns-sym styles-key])

(defn- entry-path [ns-sym registry-key]
  (conj (styles-path ns-sym) registry-key))

(defn- valid-env? [env]
  (and (map? env)
       (get-in env [:ns :meta :file])
       (:line env)
       (:column env)))

(defn- valid-layer? [layer]
  (or (nil? layer)
      (and (string? (:name layer))
           (or (nil? (:order layer))
               (and (sequential? (:order layer))
                    (every? string? (:order layer)))))))

(defn- validate-groups! [declaration-groups context]
  (when-not (and (sequential? declaration-groups) (seq declaration-groups))
    (throw (ex-info "uni.style requires one or more declaration groups" context)))
  (doseq [{:keys [declaration modifiers] :as group} declaration-groups]
    (serialize/validate-declaration! declaration (assoc context :group group))
    (when-not (and (sequential? modifiers) (every? string? modifiers))
      (throw (ex-info "uni.style modifiers must be selector strings"
                      (assoc context :group group))))))

(defn- style-entry [env ns-sym registry-key css-str atomic? global?]
  (when-not (and (valid-env? env)
                 (symbol? ns-sym)
                 (string? registry-key)
                 (string? css-str))
    (throw (ex-info "Invalid uni.style registration"
                    {:env env :ns ns-sym :registry-key registry-key})))
  (cond-> {:css-str css-str
           :file (get-in env [:ns :meta :file])
           :loc (select-keys env [:column :line])}
          atomic? (assoc :atomic? true)
          (and atomic? *atomic-meta*) (assoc :sort-meta *atomic-meta*)
          global? (assoc :global? true)))

(defn- register-style-entry! [env ns-sym registry-key css-str atomic? global? & [definition]]
  (let [jvm? (host/jvm? env)
        entry (cond-> (style-entry (host/source-env env) ns-sym registry-key css-str atomic? global?)
                      definition (assoc :definition definition))
        path (if jvm? [:styles ns-sym registry-key] (entry-path ns-sym registry-key))
        registry (if jvm? *jvm-styles* (host/cljs-compiler))]
    (when (and (not jvm?) (nil? registry))
      (throw (ex-info "uni.style registration requires a ClojureScript compiler environment"
                      {:ns ns-sym})))
    ;; JVM CSS is collected only while a build callback loads or compiles source.
    ;; Ordinary namespace loading still emits runtime composition descriptors.
    (when registry
      (swap! registry
             (fn [state]
               (when-let [existing (get-in state path)]
                 (when (and atomic? (not= css-str (:css-str existing)))
                   (throw (ex-info "Atomic CSS class registered with conflicting declarations"
                                   {:class-name registry-key
                                    :existing-css (:css-str existing)
                                    :css css-str}))))
               (cond-> (assoc-in state path entry)
                       (and jvm? (not (contains? (:ns-order state) ns-sym)))
                       (assoc-in [:ns-order ns-sym] (count (:ns-order state)))))))
    entry))

(defn register-definition! [env ns-sym owner definition]
  (register-style-entry! env ns-sym (str "uni-style-definition-" owner) "" false true definition))

(defn register-global! [env ns-sym registry-key css-str]
  (register-style-entry! env ns-sym registry-key css-str false true))

(defn register-build-global
  "Add hook-generated global CSS to a Shadow build's analyzer-backed registry."
  [build-state env ns-sym registry-key css-str]
  (update build-state :compiler-env assoc-in
          (entry-path ns-sym registry-key)
          (style-entry env ns-sym registry-key css-str false true)))

(declare namespaces-registry)

(defn build-registry
  "Return styles for namespaces reachable from the current Shadow build only."
  [{:keys [build-sources compiler-env sources]}]
  (namespaces-registry compiler-env (keep #(get-in sources [% :ns]) build-sources)))

(defn namespaces-registry
  "Return styles registered by the given namespaces in compiler-env."
  [compiler-env ns-syms]
  (reduce (fn [result ns-sym]
            (let [styles (into {}
                               (remove (fn [[_ {:keys [definition]}]]
                                         (when definition
                                           (let [sym (symbol (name (symbol (:owner definition))))
                                                 v (get-in compiler-env [:cljs.analyzer/namespaces ns-sym :defs sym])]
                                             (not= definition
                                                   (or (:uni.style/definition v)
                                                       (get-in v [:meta :uni.style/definition])))))))
                               (get-in compiler-env (styles-path ns-sym)))]
              (if (seq styles)
                (assoc result ns-sym styles)
                result)))
          {}
          ns-syms))

(defn register-atomic!
  ([env ns-sym class-name declaration]
   (register-atomic! env ns-sym class-name declaration [] nil))
  ([env ns-sym class-name declaration modifiers]
   (register-atomic! env ns-sym class-name declaration modifiers nil))
  ([env ns-sym class-name declaration modifiers layer]
   (serialize/validate-declaration! declaration {:class-name class-name})
   (when-not (and (sequential? modifiers) (every? string? modifiers))
     (throw (ex-info "uni.style modifiers must be selector strings"
                     {:class-name class-name :modifiers modifiers})))
   (when-not (valid-layer? layer)
     (throw (ex-info "Invalid uni.style cascade layer" {:layer layer})))
   (register-style-entry! env ns-sym class-name
                          (serialize/atomic-css class-name declaration modifiers layer)
                          true false)))

(defn register-composed! [env ns-sym class-name declaration-groups layer]
  (validate-groups! declaration-groups {:class-name class-name})
  (when-not (valid-layer? layer)
    (throw (ex-info "Invalid uni.style cascade layer" {:layer layer})))
  (register-style-entry! env ns-sym class-name
                         (serialize/composed-css class-name declaration-groups layer)
                         false false))

(defn register-selector!
  [env ns-sym registry-key selector declaration-groups layer]
  (when-not (and (string? selector) (not (str/blank? selector)))
    (throw (ex-info "uni.style selectors must be non-empty strings"
                    {:selector selector})))
  (validate-groups! declaration-groups {:selector selector})
  (when-not (valid-layer? layer)
    (throw (ex-info "Invalid uni.style cascade layer" {:layer layer})))
  (register-style-entry! env ns-sym registry-key
                         (serialize/selector-css selector declaration-groups layer)
                         false true))
