(ns uni.style.impl.border
  "Border, outline, and radius utility implementations."
  (:require [uni.style.impl.color :as color]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.value :as value]))

(def static-utilities
  {:outline-hidden ["outline-hidden" {:outline "2px solid transparent"
                                      :outline-offset 2}]})

(defn static* [env utility]
  (let [[class-name declaration] (get static-utilities utility)]
    (compiler/simple-utility* env class-name declaration)))

(defn- border-with-style*
  [env utility-prefix property-prefix style style-class-name args]
  (let [fallback? (= 3 (count args))
        [width-form color-form fallback-form]
        (case (count args)
          1 [1 (first args) nil]
          2 [(first args) (second args) nil]
          3 args
          (throw (ex-info "Border utilities accept one, two, or three arguments"
                          {:forms args})))
        width (compiler/resolve-compile-time-constant env width-form)
        [width-class-part width-value]
        (value/non-negative-literal-parts width-form width "Border width" :border-width)
        {color-class :class-name color-value :value}
        (if fallback?
          (color/color-utility* env utility-prefix color-form fallback-form "Border color" :border-color)
          (color/color-utility* env utility-prefix color-form "Border color" :border-color))
        color-class (compiler/register-utility!
                      env color-class
                      {(keyword (str property-prefix "-color")) color-value})
        width-class (compiler/register-utility!
                      env
                      (str utility-prefix "-" width-class-part)
                      {(keyword (str property-prefix "-width")) width-value})
        style-class (compiler/register-utility!
                      env style-class-name
                      {(keyword (str property-prefix "-style")) style})]
    (compiler/combine-style-values env [color-class width-class style-class])))

(defn border*
  "Macro implementation shared by the directional solid border utilities."
  [env utility-prefix property-prefix args]
  (border-with-style* env utility-prefix property-prefix :solid
                      (str utility-prefix "-style") args))

(defn border-axis*
  "Macro implementation for border-x and border-y: the same border on two sides,
   set per side so a later single-side utility still overrides one of them."
  [env sides args]
  (compiler/combine-style-values
    env (mapv (fn [[utility-prefix property-prefix]]
                (border* env utility-prefix property-prefix args))
              sides)))

(defn styled-border*
  "Macro implementation shared by explicit border-style utilities. With no
   arguments, only set the style; otherwise also set the width and color."
  [env utility-prefix property-prefix style args]
  (let [style-class-name (str utility-prefix "-" (name style))]
    (if (seq args)
      (border-with-style* env utility-prefix property-prefix style style-class-name args)
      (compiler/register-utility! env style-class-name
                                  {(keyword (str property-prefix "-style")) style}))))

(defn border-width*
  "Macro implementation for border-width utilities."
  [env width-form]
  (let [width (compiler/resolve-compile-time-constant env width-form)
        [class-part css-value]
        (value/non-negative-literal-parts width-form width "Border width" :border-width)]
    (compiler/register-utility! env (str "border-width-" class-part)
                                {:border-width css-value})))

(defn border-color*
  "Macro implementation for border-color utilities."
  ([env color-form]
   (color/color-property-utility* env "border" :border-color color-form
                                  "Border color" :border-color))
  ([env color-form fallback-form]
   (color/color-property-utility* env "border" :border-color color-form fallback-form
                                  "Border color" :border-color)))

(defn border-style*
  "Macro implementation shared by border style utilities."
  [env style]
  (compiler/register-utility! env
                              (str "border-" (name style))
                              {:border-style style}))

(defn outline*
  "Macro implementation for outline width, solid style, and color utilities."
  [env args]
  (let [fallback? (= 3 (count args))
        [width-form color-form fallback-form]
        (case (count args)
          1 [1 (first args) nil]
          2 [(first args) (second args) nil]
          3 args
          (throw (ex-info "Outline utilities accept one, two, or three arguments"
                          {:forms args})))
        width (compiler/resolve-compile-time-constant env width-form)
        [width-class-part width-value]
        (value/non-negative-literal-parts width-form width "Outline width" :outline-width)
        {color-class :class-name color-value :value}
        (if fallback?
          (color/color-utility* env "outline" color-form fallback-form "Outline color" :outline-color)
          (color/color-utility* env "outline" color-form "Outline color" :outline-color))
        color-class (compiler/register-utility! env color-class {:outline-color color-value})
        width-class (compiler/register-utility! env
                                                (str "outline-" width-class-part)
                                                {:outline-width width-value})
        style-class (compiler/register-utility! env "outline-style"
                                                {:outline-style :solid})]
    (compiler/combine-style-values env [color-class width-class style-class])))

(defn outline-style*
  "Macro implementation shared by outline style utilities."
  [env style]
  (compiler/register-utility! env
                              (str "outline-" (name style))
                              {:outline-style style}))

(defn- outline-width-value
  [width-form width prefix]
  (let [[class-part css-value]
        (value/non-negative-literal-parts width-form width "Outline width" :outline-width)]
    {:class-name (str prefix class-part)
     :value css-value}))

(defn outline-width*
  "Macro implementation for outline width utilities."
  ([env width-form]
   (let [width (compiler/resolve-compile-time-constant env width-form)
         {:keys [class-name value]} (outline-width-value width-form width "outline-")]
     (compiler/register-utility! env class-name {:outline-width value})))
  ([env width-form fallback-form]
   (let [width (compiler/resolve-compile-time-constant env width-form)
         property-name (value/css-var-property-name width-form width
                                                    "Outline width" :outline-width)
         fallback (compiler/resolve-compile-time-constant env fallback-form)
         [fallback-class fallback-value]
         (value/length-parts fallback-form fallback "Outline width" :outline-width true)]
     (compiler/register-utility! env
                                 (str "outline" property-name "-" fallback-class)
                                 {:outline-width (str "var(" property-name ", " fallback-value ")")}))))

(defn outline-color*
  "Macro implementation for outline color utilities."
  ([env color-form]
   (color/color-property-utility* env "outline" :outline-color color-form
                                  "Outline color" :outline-color))
  ([env color-form fallback-form]
   (color/color-property-utility* env "outline" :outline-color color-form fallback-form
                                  "Outline color" :outline-color)))

(defn- outline-offset-value
  [offset-form offset prefix]
  (if (keyword? offset)
    (let [property-name (value/css-var-property-name offset-form offset
                                                     "Outline offset" :outline-offset)]
      {:class-name (str prefix property-name)
       :value (str "var(" property-name ")")})
    (let [[class-part css-value]
          (value/length-parts offset-form offset "Outline offset" :outline-offset false)]
      {:class-name (str prefix "-" class-part)
       :value css-value})))

(defn outline-offset*
  "Macro implementation for literal or variable-backed outline offset utilities."
  ([env offset-form]
   (let [offset (compiler/resolve-compile-time-constant env offset-form)
         {:keys [class-name value]} (outline-offset-value offset-form offset "outline-offset")]
     (compiler/register-utility! env class-name {:outline-offset value})))
  ([env offset-form fallback-form]
   (let [offset (compiler/resolve-compile-time-constant env offset-form)
         property-name (value/css-var-property-name offset-form offset
                                                    "Outline offset" :outline-offset)
         fallback (compiler/resolve-compile-time-constant env fallback-form)
         {fallback-class :class-name fallback-value :value}
         (outline-offset-value fallback-form fallback "")]
     (compiler/register-utility! env
                                 (str "outline-offset" property-name fallback-class)
                                 {:outline-offset (str "var(" property-name ", " fallback-value ")")}))))

(defn rounded*
  "Macro implementation shared by the border-radius utilities."
  [env utility-prefix properties radius-form]
  (let [radius (compiler/resolve-compile-time-constant env radius-form)
        [class-part css-value]
        (value/non-negative-literal-parts radius-form radius "Border radius" :border-radius)
        class-name (str utility-prefix "-" class-part)
        declaration (into (array-map)
                          (map (fn [property] [property css-value]))
                          properties)]
    (compiler/register-utility! env class-name declaration)))
