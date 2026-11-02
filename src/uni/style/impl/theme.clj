(ns uni.style.impl.theme
  "Theme/token definitions. Names are identical on both hosts and in every build mode."
  (:require [clojure.string :as str]
            [uni.style.impl.constant :as constant]
            [uni.style.impl.host :as host]
            [uni.style.impl.naming :as naming]
            [uni.style.impl.registry :as registry]
            [uni.style.impl.tokens :as tokens]
            [uni.style.impl.value :as value]))

(defn- definition [env sym kind]
  (when-not (and (symbol? sym) (nil? (namespace sym)))
    (throw (ex-info "A theme/token definition requires an unqualified symbol" {:symbol sym})))
  (let [owner (host/definition-symbol env sym)
        readable (str/replace (name sym) #"[^a-zA-Z0-9_-]" "-")
        css-name (str "us-" (when (= kind :theme) "theme-") readable "-"
                      (naming/content-hash [kind (str owner)]))]
    {:owner (str owner) :kind kind :name (if (= kind :token) (keyword css-name) css-name)}))

(defn- register-definition [env sym entry]
  (let [owner (:owner entry)
        ns-sym (symbol (namespace (symbol owner)))]
    (registry/register-definition! env ns-sym owner entry)
    ;; Concrete lookups on the JVM work outside CSS collection, too. Emitted
    ;; registration reconstructs this data when loading AOT or cached CLJS output.
    (tokens/register! owner entry)
    `(do
       (def ~(vary-meta sym assoc :const true :uni.style/definition entry) ~(:name entry))
       (tokens/register! '~owner '~entry))))

(defn def-theme* [env sym]
  (register-definition env sym (definition env sym :theme)))

(defn resolve-theme
  "Generated class name of a `def-theme` var form."
  [env form]
  (let [resolved (when (symbol? form) (host/resolve-var env form))
        entry (or (:uni.style/definition resolved)
                  (get-in resolved [:meta :uni.style/definition]))]
    (when-not (= :theme (:kind entry))
      (throw (ex-info "Expected a def-theme var" {:form form})))
    (:name entry)))

(defn- resolve-value [env form]
  (let [resolved (when (symbol? form) (host/resolve-var env form))
        entry (or (:uni.style/definition resolved)
                  (get-in resolved [:meta :uni.style/definition]))
        _ (when (= :theme (:kind entry))
            (throw (ex-info "Theme vars cannot be token values; a default may be missing"
                            {:form form})))
        v (constant/resolve-compile-time-constant env form)
        ;; Reuse CSS-string validation: keywords are variables, numbers unitless.
        css-value (value/str-component form v)]
    (when (str/blank? css-value)
      (throw (ex-info "Token values must not be empty" {:form form})))
    ;; Preserve plain keyword aliases and numbers for concrete-value lookup.
    ;; Alpha descriptors are CSS expressions, not another kind of token alias.
    (if (map? v) css-value v)))

(defn def-token* [env sym forms]
  (when-not (and (seq forms) (odd? (count forms)))
    (throw (ex-info "def-token requires theme/value pairs followed by a default value"
                    {:symbol sym :forms forms})))
  (let [pairs (mapv (fn [[theme v]] [(resolve-theme env theme) (resolve-value env v)])
                    (partition 2 (butlast forms)))
        themes (map first pairs)]
    (when-not (= (count themes) (count (distinct themes)))
      (throw (ex-info "Duplicate theme in def-token" {:symbol sym :themes themes})))
    (register-definition env sym
                         (assoc (definition env sym :token)
                                :default (resolve-value env (last forms))
                                :values (into {} pairs)))))
