(ns uni.style.impl.color
  "Color, background, shadow, and opacity utilities."
  (:require [clojure.string :as str]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.css :as css]
            [uni.style.impl.palette :as palette]
            [uni.style.impl.value :as value]))

(def static-utilities
  {:bg-transparent ["bg-transparent" {:background-color :transparent}]
   :bg-current ["bg-current" {:background-color "currentColor"}]
   :bg-inherit ["bg-inherit" {:background-color :inherit}]
   :bg-unset ["bg-unset" {:background-color :unset}]
   :bg-none ["bg-none" {:background-image :none}]
   :bg-clip-border ["bg-clip-border" {:background-clip :border-box}]
   :bg-clip-padding ["bg-clip-padding" {:background-clip :padding-box}]
   :bg-clip-content ["bg-clip-content" {:background-clip :content-box}]
   :bg-clip-text ["bg-clip-text" {:background-clip :text}]
   :bg-repeat ["bg-repeat" {:background-repeat :repeat}]
   :bg-repeat-x ["bg-repeat-x" {:background-repeat :repeat-x}]
   :bg-repeat-y ["bg-repeat-y" {:background-repeat :repeat-y}]
   :bg-repeat-space ["bg-repeat-space" {:background-repeat :space}]
   :bg-repeat-round ["bg-repeat-round" {:background-repeat :round}]
   :bg-no-repeat ["bg-no-repeat" {:background-repeat :no-repeat}]
   :bg-auto ["bg-auto" {:background-size :auto}]
   :bg-cover ["bg-cover" {:background-size :cover}]
   :bg-contain ["bg-contain" {:background-size :contain}]
   :bg-top-left ["bg-top-left" {:background-position "top left"}]
   :bg-top ["bg-top" {:background-position :top}]
   :bg-top-right ["bg-top-right" {:background-position "top right"}]
   :bg-left ["bg-left" {:background-position :left}]
   :bg-center ["bg-center" {:background-position :center}]
   :bg-right ["bg-right" {:background-position :right}]
   :bg-bottom-left ["bg-bottom-left" {:background-position "bottom left"}]
   :bg-bottom ["bg-bottom" {:background-position :bottom}]
   :bg-bottom-right ["bg-bottom-right" {:background-position "bottom right"}]
   :bg-blend-normal ["bg-blend-normal" {:background-blend-mode :normal}]
   :bg-blend-multiply ["bg-blend-multiply" {:background-blend-mode :multiply}]
   :bg-blend-screen ["bg-blend-screen" {:background-blend-mode :screen}]
   :bg-blend-overlay ["bg-blend-overlay" {:background-blend-mode :overlay}]
   :bg-blend-darken ["bg-blend-darken" {:background-blend-mode :darken}]
   :bg-blend-lighten ["bg-blend-lighten" {:background-blend-mode :lighten}]
   :bg-blend-color-dodge ["bg-blend-color-dodge" {:background-blend-mode :color-dodge}]
   :bg-blend-color-burn ["bg-blend-color-burn" {:background-blend-mode :color-burn}]
   :bg-blend-hard-light ["bg-blend-hard-light" {:background-blend-mode :hard-light}]
   :bg-blend-soft-light ["bg-blend-soft-light" {:background-blend-mode :soft-light}]
   :bg-blend-difference ["bg-blend-difference" {:background-blend-mode :difference}]
   :bg-blend-exclusion ["bg-blend-exclusion" {:background-blend-mode :exclusion}]
   :bg-blend-hue ["bg-blend-hue" {:background-blend-mode :hue}]
   :bg-blend-saturation ["bg-blend-saturation" {:background-blend-mode :saturation}]
   :bg-blend-color ["bg-blend-color" {:background-blend-mode :color}]
   :bg-blend-luminosity ["bg-blend-luminosity" {:background-blend-mode :luminosity}]
   :mix-blend-normal ["mix-blend-normal" {:mix-blend-mode :normal}]
   :mix-blend-multiply ["mix-blend-multiply" {:mix-blend-mode :multiply}]
   :mix-blend-screen ["mix-blend-screen" {:mix-blend-mode :screen}]
   :mix-blend-overlay ["mix-blend-overlay" {:mix-blend-mode :overlay}]
   :mix-blend-darken ["mix-blend-darken" {:mix-blend-mode :darken}]
   :mix-blend-lighten ["mix-blend-lighten" {:mix-blend-mode :lighten}]
   :mix-blend-color-dodge ["mix-blend-color-dodge" {:mix-blend-mode :color-dodge}]
   :mix-blend-color-burn ["mix-blend-color-burn" {:mix-blend-mode :color-burn}]
   :mix-blend-hard-light ["mix-blend-hard-light" {:mix-blend-mode :hard-light}]
   :mix-blend-soft-light ["mix-blend-soft-light" {:mix-blend-mode :soft-light}]
   :mix-blend-difference ["mix-blend-difference" {:mix-blend-mode :difference}]
   :mix-blend-exclusion ["mix-blend-exclusion" {:mix-blend-mode :exclusion}]
   :mix-blend-hue ["mix-blend-hue" {:mix-blend-mode :hue}]
   :mix-blend-saturation ["mix-blend-saturation" {:mix-blend-mode :saturation}]
   :mix-blend-color ["mix-blend-color" {:mix-blend-mode :color}]
   :mix-blend-luminosity ["mix-blend-luminosity" {:mix-blend-mode :luminosity}]
   :mix-blend-plus-darker ["mix-blend-plus-darker" {:mix-blend-mode :plus-darker}]
   :mix-blend-plus-lighter ["mix-blend-plus-lighter" {:mix-blend-mode :plus-lighter}]
   :backdrop-blur-none ["backdrop-blur-none" {:-webkit-backdrop-filter "" :backdrop-filter ""}]
   :color-transparent ["color-transparent" {:color :transparent}]
   :color-current ["color-current" {:color "currentColor"}]
   :color-inherit ["color-inherit" {:color :inherit}]
   :decoration-inherit ["decoration-inherit" {:text-decoration-color :inherit}]
   :decoration-current ["decoration-current" {:text-decoration-color "currentColor"}]
   :decoration-transparent ["decoration-transparent" {:text-decoration-color :transparent}]
   :caret-inherit ["caret-inherit" {:caret-color :inherit}]
   :caret-current ["caret-current" {:caret-color "currentColor"}]
   :caret-transparent ["caret-transparent" {:caret-color :transparent}]
   :mask-type-alpha ["mask-type-alpha" {:mask-type :alpha}]
   :mask-type-luminance ["mask-type-luminance" {:mask-type :luminance}]})

(defn static* [env utility]
  (let [[class-name declaration] (get static-utilities utility)]
    (compiler/simple-utility* env class-name declaration)))

(defn color-utility*
  "Resolve a color utility's name and CSS value, including palette naming hints."
  ([env utility-prefix color-form label type]
   (palette/color-utility env color-form utility-prefix label type))
  ([env utility-prefix color-form fallback-form label type]
   (let [color (compiler/resolve-compile-time-constant env color-form)]
     (when-not (keyword? color)
       (throw (ex-info (str label " with a fallback must start with a token keyword")
                       {:form color-form :value color :type type})))
     (let [property-name (value/css-var-property-name color-form color label type)
           {fallback-class :class-name fallback-value :value}
           (palette/color-utility env fallback-form "" label type)]
       {:class-name (str utility-prefix property-name fallback-class)
        :value (str "var(" property-name ", " fallback-value ")")}))))

(defn color-property-utility*
  "Macro implementation for color properties."
  ([env utility-prefix property color-form label type]
   (let [{:keys [class-name value]} (color-utility* env utility-prefix color-form label type)]
     (compiler/register-utility! env class-name {property value})))
  ([env utility-prefix property color-form fallback-form label type]
   (let [{:keys [class-name value]} (color-utility* env utility-prefix color-form fallback-form label type)]
     (compiler/register-utility! env class-name {property value}))))

(defn bg*
  "Macro implementation for background colors."
  ([env color-form]
   (color-property-utility* env "bg" :background-color color-form
                            "Background color" :background-color))
  ([env color-form fallback-form]
   (color-property-utility* env "bg" :background-color color-form fallback-form
                            "Background color" :background-color)))

(defn bg-image*
  "Macro implementation for arbitrary compile-time background images."
  [env value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [_ css-value]
        (value/string-or-token-parts value-form value "Background image" :background-image)
        class-name (if (string? value)
                     (str "bg-image-" (compiler/content-hash value))
                     (str "bg-image--" (name value)))]
    (compiler/register-utility! env class-name {:background-image css-value})))

(defn bg-overlay*
  "Macro implementation for a solid color painted over the background color."
  [env color-form]
  (let [{:keys [class-name value]} (color-utility* env "bg-overlay" color-form
                                                   "Background overlay color" :bg-overlay)]
    (compiler/register-utility! env class-name
                                {:background-image (str "linear-gradient(" value ", " value ")")})))

(defn background-value*
  "Macro implementation for token-backed or literal background property values."
  [env utility-prefix property value-form label type]
  (let [resolved-value (compiler/resolve-compile-time-constant env value-form)]
    (cond
      (keyword? resolved-value)
      (compiler/css-var-utility* env utility-prefix property value-form label type)

      (string? resolved-value)
      (do
        (when (str/blank? resolved-value)
          (throw (ex-info (str label " must not be empty")
                          {:form value-form :value resolved-value :type type})))
        (compiler/register-utility! env
                                    (str utility-prefix "-" (compiler/content-hash resolved-value))
                                    {property resolved-value}))

      :else
      (throw (ex-info (str label " must be a compile-time token keyword or CSS string")
                      {:form value-form :value resolved-value :type type})))))

(defn mask-value*
  "Macro implementation for token-backed or literal mask properties, emitted
   with the -webkit- prefix first for browsers without unprefixed masks."
  [env utility-prefix property value-form label type]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value] (value/string-or-token-parts value-form value label type)]
    (compiler/register-utility! env (str utility-prefix "-" class-part)
                                (array-map (keyword (str "-webkit-" (name property))) css-value
                                           property css-value))))

(defn filter*
  "Macro implementation for token-backed or literal CSS filters."
  [env filter-form]
  (background-value* env "filter" :filter filter-form "Filter" :filter))

(defn backdrop-blur*
  "Macro implementation for token-backed or literal backdrop blur radii."
  [env value-form]
  (let [resolved-value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value]
        (value/string-or-token-parts value-form resolved-value "Backdrop blur" :backdrop-filter)]
    (compiler/register-utility! env (str "backdrop-blur-" class-part)
                                (let [css-value (str "blur(" css-value ")")]
                                  (array-map :-webkit-backdrop-filter css-value
                                             :backdrop-filter css-value)))))

(defn blur*
  "Macro implementation for blur: filter: blur(<radius>), with numbers as pixels."
  [env value-form]
  (let [resolved-value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value]
        (value/non-negative-literal-parts value-form resolved-value "Blur" :filter)
        css-value (if (number? css-value) (str css-value "px") css-value)]
    (compiler/register-utility! env (str "blur-" class-part)
                                {:filter (str "blur(" css-value ")")})))

;; Fallbacks keep the shadow valid where the @property rules are absent, such as
;; standalone s/css output.
(def ^:private box-shadow-value
  (str/join ", " (map #(str "var(" % ", " css/empty-shadow ")") css/box-shadow-properties)))

(def ^:private shadow-layers
  {:shadow {:prefix "shadow" :property :--us-shadow}
   :inset-shadow {:prefix "inset-shadow" :property :--us-inset-shadow}
   :ring {:prefix "ring" :property :--us-ring-shadow}
   :inset-ring {:prefix "inset-ring" :property :--us-inset-ring-shadow :inset? true}})

(defn- frame-box-shadow
  "Compose box-shadow for an animation frame from the layers set so far in the
   frame, so it interpolates without @property; other layers keep the element's."
  [property value]
  (let [frame-declaration (some-> compiler/*class-declaration-collector*
                                  deref :declarations first :declaration)]
    (str/join ", " (for [layer-property css/box-shadow-properties
                         :let [k (keyword layer-property)]]
                     (cond
                       (= k property) value
                       (contains? frame-declaration k) (get frame-declaration k)
                       :else (str "var(" layer-property ", " css/empty-shadow ")"))))))

(defn- shadow-layer-utility*
  "Set one box-shadow layer property alongside the shared composed box-shadow."
  [env class-name property value]
  (if compiler/*keyframes?*
    (compiler/register-utility! env class-name
                                (array-map property value
                                           :box-shadow (frame-box-shadow property value)))
    (compiler/combine-style-values
      env
      [(compiler/register-utility! env class-name {property value})
       (compiler/register-utility! env "shadow-layers" {:box-shadow box-shadow-value})])))

(defn shadow-layer-none*
  "Macro implementation for clearing one box-shadow layer."
  [env layer]
  (let [{:keys [prefix property]} (get shadow-layers layer)]
    (shadow-layer-utility* env (str prefix "-none") property css/empty-shadow)))

(defn- shadow-parts
  "Return the class suffix and CSS value of a token-backed or literal shadow."
  [env shadow-form label type]
  (let [shadow (compiler/resolve-compile-time-constant env shadow-form)]
    (cond
      (or (keyword? shadow)
          (and (string? shadow) (re-matches value/var-ref-re shadow)))
      (let [property-name (value/css-var-property-name shadow-form shadow label type)]
        [property-name (str "var(" property-name ")")])

      (string? shadow)
      (do
        (when (str/blank? shadow)
          (throw (ex-info (str label " must not be empty")
                          {:form shadow-form :value shadow :type type})))
        [(str "-" (compiler/content-hash shadow)) shadow])

      :else
      (throw (ex-info (str label " must be a compile-time token keyword or CSS string")
                      {:form shadow-form :value shadow :type type})))))

(defn shadow*
  "Macro implementation for token-backed or literal outer box shadows."
  [env shadow-form]
  (let [[class-suffix css-value] (shadow-parts env shadow-form "Box shadow" :box-shadow)]
    (shadow-layer-utility* env (str "shadow" class-suffix) :--us-shadow css-value)))

(defn- split-shadow-list
  "Split a CSS shadow list on the commas outside parentheses."
  [shadow]
  (let [{:keys [parts start]}
        (reduce (fn [{:keys [depth] :as state} [index ch]]
                  (case ch
                    \( (update state :depth inc)
                    \) (update state :depth dec)
                    \, (if (zero? depth)
                         (-> state
                             (update :parts conj (subs shadow (:start state) index))
                             (assoc :start (inc index)))
                         state)
                    state))
                {:depth 0 :start 0 :parts []}
                (map-indexed vector shadow))]
    (mapv str/trim (conj parts (subs shadow start)))))

(defn- inset-shadow-value [shadow]
  (->> (split-shadow-list shadow)
       (map #(if (re-find #"(?i)(?:^|\s)inset(?:\s|$)" %) % (str "inset " %)))
       (str/join ", ")))

(defn inset-shadow*
  "Macro implementation for token-backed or literal inset box shadows. Each
   literal shadow without the inset keyword gets one; a token must hold a
   single shadow."
  [env shadow-form]
  (let [[class-suffix css-value] (shadow-parts env shadow-form "Inset box shadow" :inset-shadow)]
    (shadow-layer-utility* env (str "inset-shadow" class-suffix) :--us-inset-shadow
                           (inset-shadow-value css-value))))

(defn ring*
  "Macro implementation for solid ring box shadows: color, width and color, or
   width, token color, and fallback color. Width defaults to 1px."
  [env layer args]
  (let [{:keys [prefix property inset?]} (get shadow-layers layer)
        fallback? (= 3 (count args))
        [width-form color-form fallback-form]
        (case (count args)
          1 [1 (first args) nil]
          2 [(first args) (second args) nil]
          3 args
          (throw (ex-info "Ring utilities accept one, two, or three arguments"
                          {:forms args})))
        width (compiler/resolve-compile-time-constant env width-form)
        [width-class-part width-value]
        (value/length-parts width-form width "Ring width" :ring-width true)
        utility-prefix (str prefix "-" width-class-part)
        {:keys [class-name value]}
        (if fallback?
          (color-utility* env utility-prefix color-form fallback-form "Ring color" :ring-color)
          (color-utility* env utility-prefix color-form "Ring color" :ring-color))]
    (shadow-layer-utility* env class-name property
                           (str (when inset? "inset ") "0 0 0 " width-value " " value))))

(defn color*
  "Macro implementation for text colors."
  ([env color-form]
   (color-property-utility* env "color" :color color-form
                            "Text color" :color))
  ([env color-form fallback-form]
   (color-property-utility* env "color" :color color-form fallback-form
                            "Text color" :color)))

(defn decoration*
  "Macro implementation for text decoration colors."
  [env color-form]
  (color-property-utility* env "decoration" :text-decoration-color color-form
                           "Text decoration color" :text-decoration-color))

(defn caret*
  "Macro implementation for caret colors."
  [env color-form]
  (let [color (compiler/resolve-compile-time-constant env color-form)]
    (when-not (or (keyword? color) (string? color))
      (throw (ex-info "Caret color must be a compile-time token keyword or CSS string"
                      {:form color-form :value color :type :caret-color})))
    (color-property-utility* env "caret" :caret-color color-form
                             "Caret color" :caret-color)))

(defn fill*
  "Macro implementation for SVG fill colors."
  ([env color-form]
   (color-property-utility* env "fill" :fill color-form "Fill color" :fill))
  ([env color-form fallback-form]
   (color-property-utility* env "fill" :fill color-form fallback-form
                            "Fill color" :fill)))

(defn stroke*
  "Macro implementation for SVG stroke colors."
  ([env color-form]
   (color-property-utility* env "stroke" :stroke color-form "Stroke color" :stroke))
  ([env color-form fallback-form]
   (color-property-utility* env "stroke" :stroke color-form fallback-form
                            "Stroke color" :stroke)))

(defn opacity*
  "Macro implementation for the element opacity declaration."
  [env value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value]
        (if (number? value)
          (do
            (when-not (and (integer? value) (<= 0 value 100))
              (throw (ex-info "Numeric opacity must be a compile-time integer between 0 and 100"
                              {:form value-form :value value :type :opacity})))
            [(value/number-class-part value) (str value "%")])
          (value/string-or-token-parts value-form value "Opacity" :opacity))]
    (compiler/register-utility! env (str "opacity-" class-part)
                                {:opacity css-value})))

(defn alpha*
  "Macro implementation for applying percentage alpha to a color value."
  [env color-form percentage-form]
  (let [color (compiler/resolve-compile-time-constant env color-form)
        percentage (compiler/resolve-compile-time-constant env percentage-form)]
    (value/color-utility color-form "" color "Color" :color)
    (when-not (and (integer? percentage) (<= 0 percentage 100))
      (throw (ex-info "Alpha must be a compile-time integer between 0 and 100"
                      {:form percentage-form :value percentage :type :alpha})))
    {value/opacity-color-key true
     :color color
     :opacity percentage}))
