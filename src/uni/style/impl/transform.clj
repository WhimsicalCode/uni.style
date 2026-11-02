(ns uni.style.impl.transform
  "Transform utilities."
  (:require [clojure.string :as str]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.value :as value]))

(def static-utilities
  {:origin-center ["origin-center" {:transform-origin :center}]
   :origin-top ["origin-top" {:transform-origin :top}]
   :origin-top-right ["origin-top-right" {:transform-origin "top right"}]
   :origin-right ["origin-right" {:transform-origin :right}]
   :origin-bottom-right ["origin-bottom-right" {:transform-origin "bottom right"}]
   :origin-bottom ["origin-bottom" {:transform-origin :bottom}]
   :origin-bottom-left ["origin-bottom-left" {:transform-origin "bottom left"}]
   :origin-left ["origin-left" {:transform-origin :left}]
   :origin-top-left ["origin-top-left" {:transform-origin "top left"}]
   :transform-content ["transform-content" {:transform-box :content-box}]
   :transform-border ["transform-border" {:transform-box :border-box}]
   :transform-fill ["transform-fill" {:transform-box :fill-box}]
   :transform-stroke ["transform-stroke" {:transform-box :stroke-box}]
   :transform-view ["transform-view" {:transform-box :view-box}]
   :backface-hidden ["backface-hidden" {:backface-visibility :hidden}]
   :backface-visible ["backface-visible" {:backface-visibility :visible}]
   :translate-none ["translate-none" {:translate :none}]
   :scale-none ["scale-none" {:scale :none}]
   :rotate-none ["rotate-none" {:rotate :none}]
   :transform-none ["transform-none" {:transform :none}]})

(defn static* [env utility]
  (let [[class-name declaration] (get static-utilities utility)]
    (compiler/simple-utility* env class-name declaration)))

(defn origin*
  "Macro implementation for token-backed or literal transform origins."
  [env value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value]
        (value/string-or-token-parts value-form value "Transform origin" :transform-origin)]
    (compiler/register-utility! env (str "origin-" class-part) {:transform-origin css-value})))

(def ^:private translate-axes "var(--us-translate-x, 0) var(--us-translate-y, 0)")
(def ^:private scale-axes "var(--us-scale-x, 1) var(--us-scale-y, 1)")

(defn- axes-utility*
  "Set axis custom properties alongside a shared class composing them into one
   property, as Tailwind v4 does. Animation frames get the direct value."
  [env class-name axis-declaration shared-class property shared-value frame-value]
  (if compiler/*keyframes?*
    (compiler/register-utility! env class-name {property frame-value})
    (compiler/combine-style-values
      env
      [(compiler/register-utility! env class-name axis-declaration)
       (compiler/register-utility! env shared-class {property shared-value})])))

(defn- scale-parts [form resolved label signed?]
  (if (number? resolved)
    (do
      (if signed?
        (when-not (value/finite-number? resolved)
          (throw (ex-info (str label " must be a finite compile-time number")
                          {:form form :value resolved :type :scale})))
        (value/validate-non-negative-number! form :scale label resolved))
      [(value/number-class-part resolved)
       (-> (bigdec resolved)
           (.divide 100M)
           .stripTrailingZeros
           .toPlainString)])
    (value/string-or-token-parts form resolved label :scale)))

(defn scale* [env percentage-form]
  (let [percentage (compiler/resolve-compile-time-constant env percentage-form)
        [class-part factor] (scale-parts percentage-form percentage "Scale" false)]
    (axes-utility* env (str "scale-" class-part)
                   (array-map :--us-scale-x factor :--us-scale-y factor)
                   "scale-axes" :scale scale-axes factor)))

(defn scale-axis* [env axis percentage-form]
  (let [percentage (compiler/resolve-compile-time-constant env percentage-form)
        [class-part factor] (scale-parts percentage-form percentage "Scale" true)
        x? (= :x axis)]
    (axes-utility* env (str "scale-" (name axis) "-" class-part)
                   {(if x? :--us-scale-x :--us-scale-y) factor}
                   "scale-axes" :scale scale-axes
                   (if x? (str factor " var(--us-scale-y, 1)") (str "var(--us-scale-x, 1) " factor)))))

(defn- rotate-value [form resolved]
  (cond
    (value/finite-number? resolved)
    {:class-part (value/number-class-part resolved)
     :css-value (str resolved "deg")}

    (string? resolved)
    (do
      (when (str/blank? resolved)
        (throw (ex-info "Rotate values must not be empty"
                        {:form form :value resolved :type :rotate})))
      {:class-part (str "value-" (compiler/content-hash resolved))
       :css-value resolved})

    (keyword? resolved)
    (let [property-name (value/css-var-property-name form resolved
                                                     "Rotate value" :rotate)]
      {:class-part (str "-" (name resolved))
       :css-value (str "var(" property-name ")")})

    :else
    (throw (ex-info "Rotate values must be compile-time numbers, CSS strings, or variable keywords"
                    {:form form :value resolved :type :rotate}))))

(defn rotate* [env angle-form]
  (let [angle (compiler/resolve-compile-time-constant env angle-form)
        angle-value (rotate-value angle-form angle)]
    (compiler/simple-utility* env (str "rotate-" (:class-part angle-value))
                              {:rotate (:css-value angle-value)})))

(defn- translate-value [form resolved axis]
  (cond
    (value/finite-number? resolved)
    {:class-part (value/number-class-part resolved)
     :css-value (str resolved "px")}

    (string? resolved)
    (do
      (when (str/blank? resolved)
        (throw (ex-info "Translate values must not be empty"
                        {:form form :value resolved :axis axis :type :translate})))
      {:class-part (str "value-" (compiler/content-hash resolved))
       :css-value resolved})

    (keyword? resolved)
    (let [property-name (value/css-var-property-name form resolved
                                                     "Translate value" :translate)]
      {:class-part (str "-" (name resolved))
       :css-value (str "var(" property-name ")")})

    :else
    (throw (ex-info "Translate values must be compile-time numbers, CSS strings, or variable keywords"
                    {:form form :value resolved :axis axis :type :translate}))))

(defn translate* [env x-form y-form]
  (let [x (compiler/resolve-compile-time-constant env x-form)
        y (compiler/resolve-compile-time-constant env y-form)
        x-value (translate-value x-form x :x)
        y-value (translate-value y-form y :y)]
    (axes-utility* env (str "translate-" (:class-part x-value) "-" (:class-part y-value))
                   (array-map :--us-translate-x (:css-value x-value)
                              :--us-translate-y (:css-value y-value))
                   "translate-axes" :translate translate-axes
                   (str (:css-value x-value) " " (:css-value y-value)))))

(defn translate-axis* [env axis value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        {:keys [class-part css-value]} (translate-value value-form value axis)
        x? (= :x axis)]
    (axes-utility* env (str "translate-" (name axis) "-" class-part)
                   {(if x? :--us-translate-x :--us-translate-y) css-value}
                   "translate-axes" :translate translate-axes
                   (if x?
                     (str css-value " var(--us-translate-y, 0)")
                     (str "var(--us-translate-x, 0) " css-value)))))

(defn transform*
  "Macro implementation for arbitrary token-backed or literal transforms."
  [env value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value] (value/string-or-token-parts value-form value "Transform" :transform)]
    (compiler/register-utility! env (str "transform-" class-part) {:transform css-value})))
