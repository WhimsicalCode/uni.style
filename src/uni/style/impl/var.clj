(ns uni.style.impl.var
  "CSS custom-property assignment utilities."
  (:require [clojure.string :as str]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.value :as value]))

(defn- custom-property-name
  "The --name of a compile-time keyword or \"--name\" string property form."
  [env property-form]
  (let [property (compiler/resolve-compile-time-constant env property-form)
        property-name (cond
                        (keyword? property)
                        (do
                          (when (namespace property)
                            (throw (ex-info "CSS custom-property names must not be namespaced"
                                            {:form property-form
                                             :value property
                                             :type :custom-property})))
                          (str "--" (name property)))

                        (string? property) property

                        :else nil)]
    (when-not (and property-name
                   (re-matches value/custom-property-name-re property-name))
      (throw (ex-info "Malformed CSS custom-property name"
                      {:form property-form
                       :value property
                       :type :custom-property})))
    property-name))

(defn var-def*
  "Macro implementation returning a CSS custom-property name as a literal
   string, for setting the property in inline styles at runtime."
  [env property-form]
  (custom-property-name env property-form))

(defn var*
  "Macro implementation for assigning a CSS custom property."
  [env property-form value-form]
  (let [property-name (custom-property-name env property-form)
        value (compiler/resolve-compile-time-constant env value-form)
        [value-class value]
        (cond
          (keyword? value)
          (do
            (when (namespace value)
              (throw (ex-info "CSS custom-property token keywords must not be namespaced"
                              {:form value-form :value value :type :custom-property-value})))
            (let [value-property-name (str "--" (name value))]
              (when-not (re-matches value/custom-property-name-re value-property-name)
                (throw (ex-info "Malformed CSS custom-property token"
                                {:form value-form
                                 :value value
                                 :type :custom-property-value})))
              [(str "-" (name value)) (str "var(" value-property-name ")")]))

          (string? value)
          (do
            (when (str/blank? value)
              (throw (ex-info "CSS custom-property value must not be empty"
                              {:form value-form :value value :type :custom-property-value})))
            [(str "-value-" (compiler/content-hash value)) value])

          (value/finite-number? value)
          [(str "-" (value/number-class-part value)) (str value)]

          :else
          (throw (ex-info "CSS custom-property value must be a compile-time token keyword, CSS string, or finite number"
                          {:form value-form :value value :type :custom-property-value})))]
    (compiler/register-utility! env
                                (str "var-" (subs property-name 2) value-class)
                                {(keyword property-name) value})))
