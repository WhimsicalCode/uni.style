(ns uni.style.impl.serialize
  "Serialization for uni.style's flat static declaration groups."
  (:require [clojure.string :as str]
            [uni.style.impl.value :as value]))

(def ^:private unitless-properties
  #{:animation-iteration-count :aspect-ratio :border-image-outset
    :border-image-slice :border-image-width :box-flex :box-flex-group
    :box-ordinal-group :column-count :columns :flex :flex-grow :flex-positive
    :flex-shrink :flex-negative :flex-order :grid-area :grid-row :grid-row-end
    :grid-row-span :grid-row-start :grid-column :grid-column-end
    :grid-column-span :grid-column-start :font-weight :line-clamp :line-height
    :opacity :order :orphans :scale :tab-size :widows :z-index :zoom
    :fill-opacity :flood-opacity :stop-opacity :stroke-dasharray
    :stroke-dashoffset :stroke-miterlimit :stroke-opacity :stroke-width
    :moz-animation-iteration-count :moz-box-flex :moz-box-flex-group
    :moz-line-clamp :ms-animation-iteration-count :ms-flex :ms-zoom
    :ms-flex-grow :ms-flex-negative :ms-flex-order :ms-flex-positive
    :ms-flex-shrink :ms-grid-column :ms-grid-column-span :ms-grid-row
    :ms-grid-row-span :webkit-animation-iteration-count :webkit-box-flex
    :webkit-box-flex-group :webkit-box-ordinal-group :webkit-column-count
    :webkit-columns :webkit-flex :webkit-flex-grow :webkit-flex-positive
    :webkit-flex-shrink :webkit-line-clamp})

(defn static-value? [value]
  (or (keyword? value)
      (string? value)
      (value/finite-number? value)))

(defn valid-declaration? [declaration]
  (and (map? declaration)
       (seq declaration)
       (every? #(or (keyword? %) (string? %)) (keys declaration))
       (every? static-value? (vals declaration))))

(defn validate-declaration! [declaration context]
  (when-not (valid-declaration? declaration)
    (throw (ex-info "uni.style declarations must be non-empty flat maps of static CSS values"
                    (assoc context :declaration declaration))))
  declaration)

(defn with-unit [property value]
  (cond-> value
          (keyword? value) name
          (and (number? value)
               (not= 0 value)
               ;; The set names vendor-prefixed properties without the leading dash.
               (not (unitless-properties (keyword (str/replace (name property) #"^-" "")))))
          (str "px")))

(defn declaration-css
  "Serialize one validated declaration map."
  [declaration]
  (validate-declaration! declaration {})
  (str/join ""
            (map (fn [[property value]]
                   (str (name property) ":" (with-unit property value) ";"))
                 declaration)))

(defn- class-selector [class-name]
  (str "." (-> class-name
               (str/replace ":" "\\:")
               (str/replace "/" "\\/")
               (str/replace "*" "\\*"))))

(defn- selector-parts [^String selector]
  (loop [index 0
         part-start 0
         parts []
         paren-depth 0
         bracket-depth 0
         quote-char nil]
    (if (< index (.length selector))
      (let [char (.charAt selector index)]
        (cond
          (= char \\) (recur (+ index 2) part-start parts paren-depth bracket-depth quote-char)
          quote-char (recur (inc index) part-start parts paren-depth bracket-depth
                            (when-not (= char quote-char) quote-char))
          (#{\" \'} char) (recur (inc index) part-start parts paren-depth bracket-depth char)
          (= char \() (recur (inc index) part-start parts (inc paren-depth) bracket-depth nil)
          (= char \)) (recur (inc index) part-start parts (dec paren-depth) bracket-depth nil)
          (= char \[) (recur (inc index) part-start parts paren-depth (inc bracket-depth) nil)
          (= char \]) (recur (inc index) part-start parts paren-depth (dec bracket-depth) nil)
          (and (= char \,) (zero? paren-depth) (zero? bracket-depth))
          (recur (inc index) (inc index)
                 (conj parts (str/trim (subs selector part-start index)))
                 paren-depth bracket-depth nil)
          :else (recur (inc index) part-start parts paren-depth bracket-depth nil)))
      (conj parts (str/trim (subs selector part-start))))))

(defn- serialize-modifiers [selectors modifiers declaration]
  (if-let [modifier (first modifiers)]
    (let [modifier (str/trim modifier)]
      (when (str/blank? modifier)
        (throw (ex-info "uni.style modifiers must not be blank"
                        {:modifier modifier})))
      (if (str/starts-with? modifier "@")
        (str modifier "{"
             (serialize-modifiers selectors (rest modifiers) declaration)
             "}")
        (let [parts (selector-parts modifier)]
          (when-not (and (seq parts)
                         (every? #(str/includes? % "&") parts))
            (throw (ex-info "uni.style selector modifiers must include `&`"
                            {:modifier modifier})))
          (let [selectors (for [selector selectors
                                part parts]
                            (str/replace part "&" selector))]
            (serialize-modifiers selectors (rest modifiers) declaration)))))
    (str (str/join "," selectors) "{" (declaration-css declaration) "}")))

(defn declaration-group-css [selector {:keys [declaration modifiers] :as group}]
  (validate-declaration! declaration {:group group})
  (when-not (and (sequential? modifiers) (every? string? modifiers))
    (throw (ex-info "uni.style modifiers must be a sequence of selector strings"
                    {:group group})))
  (serialize-modifiers (if (seq modifiers) (selector-parts selector) [selector])
                       modifiers declaration))

(defn- wrap-layer [css layer]
  (if layer
    (str (when-let [order (seq (:order layer))]
           (str "@layer " (str/join "," order) ";"))
         "@layer " (:name layer) "{" css "}")
    css))

(defn atomic-css [class-name declaration modifiers layer]
  (-> (declaration-group-css (class-selector class-name)
                             {:declaration declaration :modifiers modifiers})
      (wrap-layer layer)))

(defn composed-css [class-name declaration-groups layer]
  (-> (str/join "" (map #(declaration-group-css (class-selector class-name) %)
                        declaration-groups))
      (wrap-layer layer)))

(defn selector-css [selector declaration-groups layer]
  (-> (str/join "" (map #(declaration-group-css selector %) declaration-groups))
      (wrap-layer layer)))
