(ns uni.style.impl.composition
  "Base styles, standalone stylesheets, component classes, and variants."
  (:require [clojure.string :as str]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.host :as host]
            [uni.style.impl.modifier :as modifier]
            [uni.style.impl.serialize :as serialize]))

;; Keep metadata keys stable across the namespace split.
(def declarations-meta-key :uni.style.core/declarations)
(def layer-meta-key :uni.style.core/layer)

(def ^:private class-declarations- (atom {}))

(defn referenced-class-style [env form]
  (when (symbol? form)
    (let [resolved-var (host/resolve-var env form)
          qualified-sym (:name resolved-var)
          declarations (or (when-not (host/jvm? env)
                             (get @class-declarations- qualified-sym))
                           (get resolved-var declarations-meta-key)
                           (get-in resolved-var [:meta declarations-meta-key]))]
      (when declarations
        {:symbol qualified-sym
         :declarations declarations
         :layer (or (get resolved-var layer-meta-key)
                    (get-in resolved-var [:meta layer-meta-key]))}))))

(defn collect-style-form! [env collector form]
  (if-let [{:keys [declarations layer]} (referenced-class-style env form)]
    (doseq [{:keys [declaration modifiers]} declarations]
      (compiler/collect-declaration! collector declaration modifiers
                                     (or compiler/*layer* layer)))
    (let [registrations (:registrations @collector)]
      (binding [compiler/*class-declaration-collector* collector]
        (dorun (modifier/resolve-style-forms env [form])))
      (when (= registrations (:registrations @collector))
        (throw (ex-info "Style definitions expect uni.style utilities or another defined class"
                        {:form form}))))))

(defn def-base* [env selector-form forms]
  (let [selector (compiler/resolve-compile-time-constant env selector-form)
        layer (compiler/resolve-layer env :base)
        collector (atom {:declarations []
                         :registrations 0
                         :layer compiler/unset-layer})]
    (when-not (and (string? selector) (not (str/blank? selector)))
      (throw (ex-info "A base style requires a non-empty compile-time string selector"
                      {:form selector-form :value selector :type :selector})))
    (binding [compiler/*layer* layer
              compiler/*selector-declaration-collector?* true]
      (doseq [form forms]
        (collect-style-form! env collector form)))
    (let [{:keys [declarations layer]} @collector]
      (when-not (seq declarations)
        (throw (ex-info "A base style requires at least one utility declaration"
                        {:selector selector})))
      (compiler/register-selector!
        env
        (compiler/caller-ns env)
        (str "uni-style-def-base-" (compiler/content-hash [selector declarations]))
        selector
        declarations
        layer))
    nil))

(defn- build-class* [env layer-name qualified-dev-name? qualified-sym bare-name forms]
  (let [layer (compiler/resolve-layer env layer-name)
        collector (atom {:declarations []
                         :registrations 0
                         :layer compiler/unset-layer})]
    (binding [compiler/*layer* layer]
      (doseq [form forms]
        (collect-style-form! env collector form)))
    (let [{:keys [declarations layer]} @collector
          [_ _ class-name]
          (if qualified-dev-name?
            (compiler/composed-class-definition env qualified-sym nil false true)
            (compiler/composed-class-definition env qualified-sym bare-name false true))]
      (when-not (seq declarations)
        (throw (ex-info "A class definition requires at least one utility declaration"
                        {:class-name bare-name})))
      (compiler/register-composed! env (compiler/caller-ns env) class-name declarations layer)
      (when-not (host/jvm? env)
        (swap! class-declarations- assoc qualified-sym declarations))
      {:class-name class-name
       :declarations declarations
       :layer layer})))

(defn def-component* [env sym forms]
  (when-not (symbol? sym)
    (throw (ex-info "A component definition requires a symbol"
                    {:symbol sym})))
  (let [qualified-sym (host/definition-symbol env sym)
        {:keys [class-name declarations layer]}
        (build-class* env :components (compiler/qualified-component-classes? env)
                      qualified-sym (name sym) forms)
        sym (with-meta sym (assoc (meta sym)
                                  declarations-meta-key declarations
                                  layer-meta-key layer))]
    `(def ~sym ~class-name)))

(defn css* [env rules]
  (when-not (and (map? rules) (seq rules))
    (throw (ex-info "css requires a non-empty literal selector map" {:rules rules})))
  (let [rules (mapv (fn [[selector-form forms]]
                      (let [selector (compiler/resolve-compile-time-constant env selector-form)]
                        (when-not (and (string? selector) (not (str/blank? selector)))
                          (throw (ex-info "css selectors must be non-empty compile-time strings"
                                          {:form selector-form :value selector :type :selector})))
                        [(str/trim selector) (if (vector? forms) forms [forms])]))
                    rules)
        selectors (map first rules)]
    (when-not (= (count selectors) (count (distinct selectors)))
      (throw (ex-info "css selectors must be unique after resolution" {:selectors selectors})))
    (apply str
           (for [[selector forms] (sort-by first rules)]
             (let [collector (atom {:declarations []
                                    :registrations 0
                                    :layer compiler/unset-layer})]
               (binding [compiler/*layer* nil
                         compiler/*selector-declaration-collector?* true]
                 (doseq [form forms]
                   (collect-style-form! env collector form)))
               (let [{:keys [declarations layer]} @collector]
                 (when-not (seq declarations)
                   (throw (ex-info "css selectors require at least one style declaration"
                                   {:selector selector})))
                 (serialize/selector-css selector declarations layer)))))))

(defn def-class* [env sym layer declarations-fn]
  (when-not (symbol? sym)
    (throw (ex-info "A class definition requires a symbol"
                    {:symbol sym})))
  (let [qualified-sym (host/definition-symbol env sym)
        [_ _ class-name]
        (if (compiler/qualified-component-classes? env)
          (compiler/composed-class-definition env qualified-sym nil false true)
          (compiler/composed-class-definition env qualified-sym (name sym) false true))
        declarations (declarations-fn class-name)
        sym (with-meta sym (assoc (meta sym)
                                  declarations-meta-key declarations
                                  layer-meta-key layer))]
    (when-not (seq declarations)
      (throw (ex-info "A class definition requires at least one utility declaration"
                      {:class-name class-name})))
    (compiler/register-composed! env (compiler/caller-ns env) class-name declarations layer)
    (when-not (host/jvm? env)
      (swap! class-declarations- assoc qualified-sym declarations))
    `(def ~sym ~class-name)))

(defn resolve-variants-key [env variant-key]
  (let [resolved-key (if (vector? variant-key)
                       (mapv #(compiler/resolve-compile-time-constant env %) variant-key)
                       (compiler/resolve-compile-time-constant env variant-key))
        key-parts (if (vector? resolved-key) resolved-key [resolved-key])]
    (when-not (and (seq key-parts)
                   (every? #(and (keyword? %) (nil? (namespace %))) key-parts))
      (throw (ex-info "A variants key must resolve to an unnamespaced keyword or a non-empty vector of unnamespaced keywords"
                      {:key variant-key :value resolved-key :type :variants-key})))
    {:key resolved-key
     :key-parts (mapv name key-parts)}))

(defn def-variants* [env sym variants-map]
  (when-not (symbol? sym)
    (throw (ex-info "A variants definition requires a symbol"
                    {:symbol sym})))
  (when-not (and (map? variants-map) (seq variants-map))
    (throw (ex-info "A variants definition requires a non-empty map"
                    {:symbol sym :variants variants-map})))
  (let [qualified-sym (host/definition-symbol env sym)
        class-definitions (mapv (fn [[variant-key forms]]
                                  (let [{:keys [key key-parts]}
                                        (resolve-variants-key env variant-key)]
                                    {:key key
                                     :bare-name (str (name sym) "-" (str/join "-" key-parts))
                                     :forms (if (vector? forms) forms [forms])}))
                                variants-map)
        duplicate-name (->> class-definitions
                            (map :bare-name)
                            frequencies
                            (some (fn [[class-name count]]
                                    (when (< 1 count) class-name))))]
    (when duplicate-name
      (throw (ex-info "Variants keys must produce unique class names"
                      {:symbol sym :class-name duplicate-name})))
    `(def ~sym
       ~(into {}
              (map (fn [{:keys [key bare-name forms]}]
                     (let [class-sym (symbol (namespace qualified-sym) bare-name)]
                       [key (:class-name
                              (build-class* env :variants
                                            (compiler/qualified-component-classes? env)
                                            class-sym bare-name forms))]))
                   class-definitions)))))
