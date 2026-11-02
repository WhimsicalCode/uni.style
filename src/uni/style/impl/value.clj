(ns uni.style.impl.value
  "Pure compile-time validation and CSS value normalization."
  (:refer-clojure :rename {not not'})
  (:require [clojure.string :as str]
            [uni.style.impl.css :as css]
            [uni.style.impl.naming :as naming]))

(def custom-property-name-re css/custom-property-name-re)

(def var-ref-re
  #"var\((--[-_a-zA-Z][-_a-zA-Z0-9]*)\)")

;; This key is part of observable macro-helper output. Keep its legacy namespace.
(def opacity-color-key :uni.style.core/opacity-color)

(defn finite-number? [v]
  (and (number? v)
       (Double/isFinite (double v))))

(defn validate-non-negative-number! [form type label value]
  (when-not (and (finite-number? value) (not' (neg? value)))
    (throw (ex-info (str label " must be a non-negative compile-time number")
                    {:form form :value value :type type})))
  value)

(defn number-class-part [value]
  (-> (str value)
      str/lower-case
      (str/replace #"\." "_")
      (str/replace #"[^a-z0-9_-]" "_")))

(defn custom-property-utility [form prefix property-name]
  (when-not (re-matches custom-property-name-re property-name)
    (throw (ex-info "Malformed CSS custom-property reference"
                    {:form form :value property-name})))
  {:class-name (str prefix property-name)
   :value (str "var(" property-name ")")})

(defn color-utility [form prefix color label type]
  (cond
    (and (map? color) (true? (get color opacity-color-key)))
    (let [{:keys [color opacity]} color
          {:keys [class-name value]} (color-utility form prefix color label type)]
      {:class-name (str class-name "/" opacity)
       :value (str "color-mix(in oklab, " value " " opacity "%, transparent)")})

    (keyword? color)
    (do
      (when (namespace color)
        (throw (ex-info (str label " token keywords must not be namespaced")
                        {:form form :value color :type type})))
      (custom-property-utility form prefix (str "--" (name color))))

    (string? color)
    (do
      (when (str/blank? color)
        (throw (ex-info (str label " must not be empty")
                        {:form form :value color :type type})))
      {:class-name (str prefix "-color-" (naming/content-hash color))
       :value color})

    :else
    (throw (ex-info (str label " must be a compile-time token keyword or CSS string")
                    {:form form :value color :type type}))))

(defn css-var-property-name [value-form value label type]
  (cond
    (keyword? value)
    (do
      (when (namespace value)
        (throw (ex-info (str label " token keywords must not be namespaced")
                        {:form value-form :value value :type type})))
      (let [property-name (str "--" (name value))]
        (when-not (re-matches custom-property-name-re property-name)
          (throw (ex-info "Malformed CSS custom-property reference"
                          {:form value-form :value property-name :type type})))
        property-name))

    (string? value)
    (if-let [[_ property-name] (re-matches var-ref-re value)]
      property-name
      (throw (ex-info (str label " must be a CSS custom-property reference")
                      {:form value-form :value value :type type})))

    :else
    (throw (ex-info (str label " must be a compile-time token keyword or CSS custom-property reference")
                    {:form value-form :value value :type type}))))

(defn str-component [component-form component]
  (if (and (map? component) (true? (get component opacity-color-key)))
    (:value (color-utility component-form "" component "CSS string" :css-string))
    (css/value-css component-form component)))

(defn token-parts [value-form value label type]
  (let [property-name (css-var-property-name value-form value label type)]
    [(str "-" (name value)) (str "var(" property-name ")")]))

(defn string-parts [value-form value label type]
  (when (str/blank? value)
    (throw (ex-info (str label " must not be empty")
                    {:form value-form :value value :type type})))
  [(str "value-" (naming/content-hash value)) value])

(defn string-or-token-parts [value-form value label type]
  (cond
    (string? value) (string-parts value-form value label type)
    (keyword? value) (token-parts value-form value label type)
    :else
    (throw (ex-info (str label " must be a compile-time CSS string or token keyword")
                    {:form value-form :value value :type type}))))

(defn length-parts
  [value-form value label type non-negative?]
  (cond
    (number? value)
    (do
      (if non-negative?
        (validate-non-negative-number! value-form type label value)
        (when-not (finite-number? value)
          (throw (ex-info (str label " must be a finite compile-time number")
                          {:form value-form :value value :type type}))))
      [(number-class-part value) (str value "px")])

    (string? value) (string-parts value-form value label type)
    (keyword? value) (token-parts value-form value label type)

    :else
    (throw (ex-info (str label " must be a compile-time number, CSS string, or token keyword")
                    {:form value-form :value value :type type}))))

(defn literal-parts [value-form value label type]
  (cond
    (number? value)
    (do
      (when-not (finite-number? value)
        (throw (ex-info (str label " must be a finite compile-time number")
                        {:form value-form :value value :type type})))
      [(number-class-part value) value])

    (string? value) (string-parts value-form value label type)
    (keyword? value) (token-parts value-form value label type)

    :else
    (throw (ex-info (str label " must be a compile-time number, CSS string, or token keyword")
                    {:form value-form :value value :type type}))))

(defn non-negative-literal-parts [value-form value label type]
  (if (number? value)
    (do
      (validate-non-negative-number! value-form type label value)
      [(number-class-part value) value])
    (literal-parts value-form value label type)))

(defn integer-literal-parts [value-form value label type]
  (if (number? value)
    (do
      (when-not (integer? value)
        (throw (ex-info (str label " must be an integer when provided as a number")
                        {:form value-form :value value :type type})))
      [(number-class-part value) value])
    (literal-parts value-form value label type)))
