(ns uni.style.impl.naming
  "Canonical content hashing and development/release CSS names."
  (:require [clojure.string :as str]
            [uni.style.impl.host :as host])
  (:import [java.security MessageDigest]))

(defn- canonicalize [value]
  (cond
    (map? value)
    (->> value
         (map (fn [[k v]] [(canonicalize k) (canonicalize v)]))
         (sort-by (comp pr-str first))
         vec)

    (sequential? value)
    (mapv canonicalize value)

    (set? value)
    (->> value (map canonicalize) (sort-by pr-str) vec)

    :else value))

(defn content-hash
  "Stable eight-character SHA-256-derived hash of arbitrary content."
  [content]
  (let [bytes (.digest (MessageDigest/getInstance "SHA-256")
                       (.getBytes ^String (pr-str (canonicalize content)) "UTF-8"))]
    (->> (take 4 bytes)
         (map #(format "%02x" (bit-and % 0xff)))
         (apply str))))

(defn hashed-class-name [content]
  (str "us-" (content-hash content)))

(defn release-build?
  "Whether the ClojureScript being compiled is a production build: Closure's
   advanced optimizations, or goog.DEBUG defined false, as ClojureScript ES
   module builds (cljf.esm) do for production."
  []
  (let [{:keys [optimizations closure-defines]} (:options @(host/cljs-compiler))]
    (or (= :advanced optimizations)
        (some #(false? (get closure-defines %)) ['goog.DEBUG "goog.DEBUG"]))))

(defn css-option
  "Option k of uni.style's :css-options: the Shadow build config's, or the
   compiler options' (ClojureScript ES module builds, cljf.esm)."
  [k]
  (when-let [compiler-env (host/cljs-compiler)]
    (let [compiler @compiler-env]
      (get-in compiler [:shadow.build.cljs-bridge/state :shadow.build/config :css-options k]
              (get-in compiler [:options :css-options k])))))

(defn short-class-names-option
  "The :short-class-names CSS option, nil unless set to a boolean."
  []
  (let [option (css-option :short-class-names)]
    (when (boolean? option)
      option)))

(defn jvm-option
  "Option k of JVM macro expansion, from the uni.style.<k> system property:
   true, false, or nil when unset."
  [k]
  (let [property (str "uni.style." (name k))
        value (System/getProperty property)]
    (when (some? value)
      (let [option (parse-boolean value)]
        (when (nil? option)
          (throw (ex-info (str "uni.style [CONFIG ERROR]\n" property
                               " must be true or false, not " (pr-str value))
                          {:property property :value value})))
        option))))

(defn- munge-preserving-dashes [value]
  (->> (str/split (str/replace value #"\." "_") #"-" -1)
       (map munge)
       (str/join "-")))

(defn composed-class-definition
  "Return the quoted def form used by composed classes and its generated name."
  [short-class-names? sym class-name stable-dev-name? preserve-dev-dashes?]
  (let [base (-> class-name
                 (or (str/replace (str sym) #"-?class$" ""))
                 (str/replace "/" "--"))
        dev-base (if preserve-dev-dashes?
                   (munge-preserving-dashes base)
                   (munge base))
        class-name (cond
                     short-class-names?
                     (hashed-class-name (str sym))

                     stable-dev-name?
                     dev-base

                     ;; Hash the qualified symbol rather than counting, so names
                     ;; stay unique across namespaces yet survive recompiles.
                     :else
                     (str dev-base "-" (content-hash (str sym))))]
    (list 'def sym class-name)))
