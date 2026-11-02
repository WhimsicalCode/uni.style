(ns uni.style.impl.modifier
  "Descendant-selector, pseudo-class, attribute, negation, and media-query modifiers."
  (:refer-clojure :rename {not not'})
  (:require [clojure.string :as str]
            [uni.style.impl.class-name :as class-name]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.constant :as constant]
            [uni.style.impl.host :as host]
            [uni.style.impl.theme :as theme]
            [uni.style.impl.value :as value]))

(defn flatten-variant-result [form result]
  (cond
    (compiler/style-ir? result) [result]
    (string? result) [result]
    (vector? result) (mapcat #(flatten-variant-result form %) result)
    :else (throw (ex-info "Style modifiers require nested utility classes"
                          {:form form :value result}))))

(defn- transform-variant [variant]
  (reduce (fn [variant transform]
            (transform variant))
          variant
          compiler/*variant-transforms*))

(defn resolve-style-forms [env forms]
  (letfn [(resolve-form [env form]
            (if-let [{source-env :env source-forms :forms ns-sym :ns}
                     (constant/referenced-vector env form)]
              (let [resolve-source-forms #(mapcat (partial resolve-form source-env)
                                                  source-forms)]
                (if (host/jvm? env)
                  (binding [*ns* (the-ns ns-sym)]
                    (vec (resolve-source-forms)))
                  (resolve-source-forms)))
              [[form (compiler/resolve-compile-time-constant env form)]]))]
    (mapcat (partial resolve-form env) forms)))

(defn modifier* [env variant forms]
  (let [variant (transform-variant variant)]
    (binding [compiler/*variants* (conj compiler/*variants* variant)
              compiler/*variant-transforms* []]
      (->> (resolve-style-forms env forms)
           (mapcat (fn [[form result]]
                     (flatten-variant-result form result)))
           (mapv compiler/emit-style-value)))))

(defn direct-children-or-multiply* [env forms]
  (let [variant (transform-variant {:prefix "*" :selector "& > *"})]
    (binding [compiler/*variants* (conj compiler/*variants* variant)
              compiler/*variant-transforms* []]
      (let [form-results (vec (resolve-style-forms env forms))
            results (mapv second form-results)]
        (if (every? value/finite-number? results)
          (compiler/multiply-resolved-values* forms results)
          (->> form-results
               (mapcat (fn [[form result]]
                         (flatten-variant-result form result)))
               (mapv compiler/emit-style-value)))))))

(def class-name-re
  ;; A CSS identifier, so the class can be used in a selector unescaped: .1col
  ;; or .--x would be invalid, and browsers drop the rule without an error.
  #"^-?[a-zA-Z_][a-zA-Z0-9_-]*$")

(defn class*
  "The class name of a compile-time keyword or string: a string or unnamespaced
   keyword as is, a namespaced keyword as <namespace>--<name>, the same as s/c."
  [env class-form]
  (let [class-value (compiler/resolve-compile-time-constant env class-form)
        class-name (cond
                     (keyword? class-value) (class-name/keyword-class-name class-value)
                     (string? class-value) class-value
                     :else nil)]
    (when-not (and class-name (re-matches class-name-re class-name))
      (throw (ex-info "Class must be a compile-time class name, as a keyword or string"
                      {:form class-form :value class-value :type :class-key})))
    class-name))

(defn- class-prefix
  "The class name followed by its hash, for generated class names. The hash marks
   where the class name ends, so (ancestor :hover-card ...) and
   (ancestor-hover :card ...) can't produce the same class."
  [class-name]
  (str class-name "-" (compiler/content-hash [:uni.style/class class-name])))

(defn sel-class* [env key-form forms]
  (let [class-name (class* env key-form)]
    (modifier* env {:prefix (str "sel-class-" (class-prefix class-name))
                    :selector (str "& ." class-name)}
               forms)))

(defn ancestor* [env key-form forms]
  (let [class-name (class* env key-form)]
    (modifier* env {:prefix (str "ancestor-" (class-prefix class-name))
                    :selector (str "." class-name " &")}
               forms)))

(defn self-or-ancestor* [env key-form forms]
  (let [class-name (class* env key-form)]
    (modifier* env {:prefix (str "self-or-ancestor-" (class-prefix class-name))
                    :selector (str "." class-name "&, ." class-name " &")}
               forms)))

(defn root-theme* [env theme-form forms]
  (let [theme-class (theme/resolve-theme env theme-form)]
    (modifier* env {:prefix (str "root-theme-" theme-class)
                    :selector (str ":root." theme-class " &")}
               forms)))

(defn- ancestor-selector [class-name selector]
  (str (str/replace selector "&" (str "." class-name)) " &"))

(defn ancestor-modifier* [env key-form variant forms]
  (let [class-name (class* env key-form)
        variant (transform-variant variant)
        variant (-> variant
                    (update :prefix #(str "ancestor-" % "-" (class-prefix class-name)))
                    (update :selector #(ancestor-selector class-name %))
                    (cond-> (:inverse-selector variant)
                            (update :inverse-selector #(ancestor-selector class-name %))))]
    (binding [compiler/*variant-transforms* []]
      (modifier* env variant forms))))

(def pseudo-selector-re
  #"^&:([a-zA-Z][a-zA-Z0-9-]*)$")

(def negated-pseudo-selector-re
  #"^&:not\(:([a-zA-Z][a-zA-Z0-9-]*)\)$")

(defn- negate-variant [{:keys [prefix selector inverse-selector] :as variant}]
  (cond
    inverse-selector
    (assoc variant
           :prefix (if (str/starts-with? prefix "not-")
                     (str/replace-first prefix #"^not-" "")
                     (str "not-" prefix))
           :selector inverse-selector
           :inverse-selector selector)

    (re-matches pseudo-selector-re selector)
    (let [[_ pseudo] (re-matches pseudo-selector-re selector)]
      (assoc variant
             :prefix (str "not-" prefix)
             :selector (str "&:not(:" pseudo ")")))

    (re-matches negated-pseudo-selector-re selector)
    (let [[_ pseudo] (re-matches negated-pseudo-selector-re selector)]
      (assoc variant
             :prefix (str/replace-first prefix #"^not-" "")
             :selector (str "&:" pseudo)))

    :else
    (throw (ex-info "Only pseudo-class and attribute style modifiers can be negated"
                    {:variant variant}))))

(defn not* [env form]
  (let [consumed? (atom false)
        transform (fn [variant]
                    (reset! consumed? true)
                    (negate-variant variant))
        result (binding [compiler/*variant-transforms*
                         (conj compiler/*variant-transforms* transform)]
                 (compiler/resolve-compile-time-constant env form))]
    (when-not @consumed?
      (throw (ex-info "`not` must wrap a style modifier"
                      {:form form})))
    (mapv compiler/emit-style-value (flatten-variant-result form result))))

(def attribute-name-re
  #"[-_a-zA-Z][-_a-zA-Z0-9]*")

(defn- resolve-attribute-name [env name-form]
  (let [attribute-name (compiler/resolve-compile-time-constant env name-form)
        attribute-name (cond
                         (keyword? attribute-name)
                         (when-not (namespace attribute-name)
                           (name attribute-name))

                         (string? attribute-name) attribute-name

                         :else nil)]
    (when-not (and attribute-name
                   (re-matches attribute-name-re attribute-name))
      (throw (ex-info "Attribute name must be a valid compile-time keyword or string"
                      {:form name-form
                       :value attribute-name
                       :type :attribute-name})))
    attribute-name))

(defn- attr-variant [env name-form]
  (let [attribute-name (resolve-attribute-name env name-form)
        attribute-selector (str "[" attribute-name "]")]
    {:prefix (str "attr-" attribute-name)
     :selector (str "&" attribute-selector)
     :inverse-selector (str "&:not(" attribute-selector ")")}))

(defn attr* [env name-form forms]
  (modifier* env (attr-variant env name-form) forms))

(defn ancestor-attr* [env key-form name-form forms]
  (ancestor-modifier* env key-form (attr-variant env name-form) forms))

(defn- escape-attribute-value [value]
  (-> value
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")))

(defn- attr=-variant [env name-form value-form]
  (let [attribute-name (resolve-attribute-name env name-form)
        value (compiler/resolve-compile-time-constant env value-form)
        value (cond
                (keyword? value) (if (namespace value) nil (name value))
                (string? value) value
                (or (number? value) (boolean? value)) (str value)
                :else nil)
        _ (when-not (and value
                         (not' (str/blank? value))
                         (not' (re-find #"[\u0000-\u001f\u007f]" value)))
            (throw (ex-info "Attribute value must be a non-empty compile-time keyword, string, number, or boolean without control characters"
                            {:form value-form
                             :value value
                             :type :attribute-value})))
        selector-value (escape-attribute-value value)
        attribute-selector (str "[" attribute-name "=\"" selector-value "\"]")]
    {:prefix (str "attr-" attribute-name "-value-"
                  (compiler/content-hash value))
     :selector (str "&" attribute-selector)
     :inverse-selector (str "&:not(" attribute-selector ")")}))

(defn aria*
  "Apply forms when the aria-<attribute> attribute is \"true\", like Tailwind's
   aria-* variants. Negatable with `not`."
  [env attribute forms]
  (let [attribute-selector (str "[aria-" attribute "=\"true\"]")]
    (modifier* env {:prefix (str "aria-" attribute)
                    :selector (str "&" attribute-selector)
                    :inverse-selector (str "&:not(" attribute-selector ")")}
               forms)))

(def breakpoints
  ;; Tailwind's default breakpoints, as minimum widths. Their media queries sort
  ;; in this order in the bundle, so a larger breakpoint overrides a smaller one.
  (array-map "sm" "40rem" "md" "48rem" "lg" "64rem" "xl" "80rem" "xxl" "96rem"))

(defn breakpoint* [env breakpoint forms]
  (modifier* env {:prefix breakpoint
                  :selector (str "@media (min-width: " (get breakpoints breakpoint) ")")}
             forms))

(defn max-breakpoint*
  "Apply forms below a breakpoint, like Tailwind's max-* variants."
  [env breakpoint forms]
  (modifier* env {:prefix (str "max-" breakpoint)
                  :selector (str "@media (width < " (get breakpoints breakpoint) ")")}
             forms))

(defn attr=* [env name-form value-form forms]
  (modifier* env (attr=-variant env name-form value-form) forms))

(defn ancestor-attr=* [env key-form name-form value-form forms]
  (ancestor-modifier* env key-form (attr=-variant env name-form value-form) forms))

(defn peer* [env]
  (class* env "peer"))

(defn- peer-selector [class-name selector]
  (str (str/replace selector "&" (str "." class-name)) " ~ &"))

(defn peer-modifier* [env variant forms]
  (let [class-name (peer* env)
        variant (transform-variant variant)
        variant (-> variant
                    (update :prefix #(str "peer-" %))
                    (update :selector #(peer-selector class-name %))
                    (cond-> (:inverse-selector variant)
                            (update :inverse-selector #(peer-selector class-name %))))]
    (binding [compiler/*variant-transforms* []]
      (modifier* env variant forms))))

(defn peer-pseudo* [env prefix selector forms]
  (peer-modifier* env {:prefix prefix
                       :selector (str "&:" selector)}
                  forms))

(defn peer-attr* [env name-form forms]
  (peer-modifier* env (attr-variant env name-form) forms))

(defn peer-attr=* [env name-form value-form forms]
  (peer-modifier* env (attr=-variant env name-form value-form) forms))

(defn- valid-descendant-selector? [selector]
  (and (string? selector)
       (not' (str/blank? selector))
       (not' (re-find #"[,&\u0000-\u001f\u007f]" selector))))

(defn sel* [env selector-form forms]
  (let [resolved-selector (compiler/resolve-compile-time-constant env selector-form)
        selectors (cond
                    (string? resolved-selector) [resolved-selector]
                    (vector? resolved-selector) resolved-selector
                    :else nil)]
    (when-not (and (seq selectors)
                   (every? valid-descendant-selector? selectors))
      (throw (ex-info "Selector must be a non-empty compile-time string or vector of strings without commas, ampersands, or control characters"
                      {:form selector-form
                       :value resolved-selector
                       :type :selector})))
    (let [selectors (mapv str/trim selectors)
          selector (str/join ", " (map #(str "& " %) selectors))]
      (modifier* env {:prefix (str "sel-" (compiler/content-hash selectors))
                      :selector selector}
                 forms))))

(defn has* [env selector-form forms]
  (let [selector (compiler/resolve-compile-time-constant env selector-form)]
    (when-not (valid-descendant-selector? selector)
      (throw (ex-info ":has selector must be a non-empty compile-time string without commas, ampersands, or control characters"
                      {:form selector-form
                       :value selector
                       :type :has-selector})))
    (let [selector (str/trim selector)]
      (modifier* env {:prefix (str "has-" (compiler/content-hash selector))
                      :selector (str "&:has(" selector ")")}
                 forms))))

(defn ancestor-has* [env key-form selector-form forms]
  (let [selector (compiler/resolve-compile-time-constant env selector-form)]
    (when-not (valid-descendant-selector? selector)
      (throw (ex-info "Ancestor :has selector must be a non-empty compile-time string without commas, ampersands, or control characters"
                      {:form selector-form
                       :value selector
                       :type :ancestor-has-selector})))
    (let [selector (str/trim selector)]
      (ancestor-modifier* env key-form
                          {:prefix (str "has-" (compiler/content-hash selector))
                           :selector (str "&:has(" selector ")")}
                          forms))))

(defn peer-has* [env selector-form forms]
  (let [selector (compiler/resolve-compile-time-constant env selector-form)]
    (when-not (valid-descendant-selector? selector)
      (throw (ex-info "Peer :has selector must be a non-empty compile-time string without commas, ampersands, or control characters"
                      {:form selector-form
                       :value selector
                       :type :peer-has-selector})))
    (let [selector (str/trim selector)]
      (peer-modifier* env {:prefix (str "has-" (compiler/content-hash selector))
                           :selector (str "&:has(" selector ")")}
                      forms))))

(def pseudo-element-name-re
  #"^-?[a-zA-Z][a-zA-Z0-9-]*$")

(defn pseudo* [env name-form forms]
  (let [resolved-name (compiler/resolve-compile-time-constant env name-form)
        pseudo-name (cond
                      (keyword? resolved-name) (when-not (namespace resolved-name)
                                                 (name resolved-name))
                      (string? resolved-name) resolved-name
                      :else nil)]
    (when-not (and pseudo-name
                   (re-matches pseudo-element-name-re pseudo-name))
      (throw (ex-info "Pseudo-element name must be a valid compile-time unnamespaced keyword or string without leading colons"
                      {:form name-form
                       :value resolved-name
                       :type :pseudo-element})))
    (modifier* env {:prefix (str "pseudo-" pseudo-name)
                    :selector (str "&::" pseudo-name)}
               forms)))

(defn child-pseudo* [env name selector forms]
  (modifier* env {:prefix name
                  :selector (str "&:" selector)}
             forms))

(defn- indexed-child-pseudo-variant [env name selector value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        value (cond
                (integer? value) (str value)
                (and (string? value)
                     (not' (str/blank? value))
                     (not' (re-find #"[(){};,&\u0000-\u001f\u007f]" value)))
                (str/trim value)
                :else nil)]
    (when-not value
      (throw (ex-info "Child index must be a compile-time integer or non-empty CSS nth expression without parentheses, braces, semicolons, commas, ampersands, or control characters"
                      {:form value-form
                       :value value
                       :type :child-index})))
    {:prefix (str name "-" (compiler/content-hash value))
     :selector (str "&:" selector "(" value ")")}))

(defn indexed-child-pseudo* [env name selector value-form forms]
  (modifier* env (indexed-child-pseudo-variant env name selector value-form) forms))

(defn peer-indexed-child-pseudo* [env name selector value-form forms]
  (peer-modifier* env (indexed-child-pseudo-variant env name selector value-form) forms))

(defn media* [env query-form forms]
  (let [query (compiler/resolve-compile-time-constant env query-form)]
    (when-not (and (string? query)
                   (not' (str/blank? query))
                   (str/starts-with? query "(")
                   (str/ends-with? query ")"))
      (throw (ex-info "Media query must be a non-empty compile-time parenthesized string"
                      {:form query-form :value query :type :media-query})))
    (modifier* env {:prefix (str "media-" (compiler/content-hash query))
                    :selector (str "@media " query)}
               forms)))

(defn container* [env query-form forms]
  (let [query (compiler/resolve-compile-time-constant env query-form)]
    (when-not (and (string? query)
                   (not' (str/blank? query))
                   (str/starts-with? query "(")
                   (str/ends-with? query ")")
                   (not' (re-find #"[{};\u0000-\u001f\u007f]" query)))
      (throw (ex-info "Container query must be a non-empty compile-time parenthesized string without braces, semicolons, or control characters"
                      {:form query-form
                       :value query
                       :type :container-query})))
    (let [query (str/trim query)]
      (modifier* env {:prefix (str "container-" (compiler/content-hash query))
                      :selector (str "@container " query)}
                 forms))))

(defn supports* [env condition-form forms]
  (let [condition (compiler/resolve-compile-time-constant env condition-form)]
    (when-not (and (string? condition)
                   (not' (str/blank? condition))
                   (not' (re-find #"[{};\u0000-\u001f\u007f]" condition)))
      (throw (ex-info "CSS supports condition must be a non-empty compile-time string without braces, semicolons, or control characters"
                      {:form condition-form
                       :value condition
                       :type :supports-condition})))
    (let [condition (str/trim condition)]
      (modifier* env {:prefix (str "supports-" (compiler/content-hash condition))
                      :selector (str "@supports " condition)}
                 forms))))
