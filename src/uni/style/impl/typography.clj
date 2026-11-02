(ns uni.style.impl.typography
  "Typography utility implementations."
  (:require [clojure.string :as str]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.value :as value]))

(def mono-font-stack
  ;; Tailwind's monospace stack, shared by font-mono and preflight.
  ["ui-monospace"
   "SFMono-Regular"
   "Menlo"
   "Monaco"
   "Consolas"
   "'Liberation Mono'"
   "'Courier New'"
   "monospace"])

(def static-utilities
  {:text-left ["text-left" {:text-align :left}]
   :text-center ["text-center" {:text-align :center}]
   :text-right ["text-right" {:text-align :right}]
   :text-justify ["text-justify" {:text-align :justify}]
   :text-start ["text-start" {:text-align :start}]
   :text-end ["text-end" {:text-align :end}]
   :uppercase ["uppercase" {:text-transform :uppercase}]
   :lowercase ["lowercase" {:text-transform :lowercase}]
   :capitalize ["capitalize" {:text-transform :capitalize}]
   :normal-case ["normal-case" {:text-transform :none}]
   :truncate ["truncate" {:overflow :hidden
                          :text-overflow :ellipsis
                          :white-space :nowrap}]
   :text-ellipsis ["text-ellipsis" {:text-overflow :ellipsis}]
   :text-clip ["text-clip" {:text-overflow :clip}]
   :whitespace-normal ["whitespace-normal" {:white-space :normal}]
   :whitespace-nowrap ["whitespace-nowrap" {:white-space :nowrap}]
   :whitespace-pre ["whitespace-pre" {:white-space :pre}]
   :whitespace-pre-line ["whitespace-pre-line" {:white-space :pre-line}]
   :whitespace-pre-wrap ["whitespace-pre-wrap" {:white-space :pre-wrap}]
   :whitespace-break-spaces ["whitespace-break-spaces" {:white-space :break-spaces}]
   :text-wrap ["text-wrap" {:text-wrap :wrap}]
   :text-nowrap ["text-nowrap" {:text-wrap :nowrap}]
   :text-balance ["text-balance" {:text-wrap :balance}]
   :text-pretty ["text-pretty" {:text-wrap :pretty}]
   :word-break-normal ["word-break-normal" {:word-break :normal}]
   :word-break-word ["word-break-word" {:word-break :break-word}]
   :word-break-all ["word-break-all" {:word-break :break-all}]
   :word-break-keep ["word-break-keep" {:word-break :keep-all}]
   :overflow-wrap-normal ["overflow-wrap-normal" {:overflow-wrap :normal}]
   :overflow-wrap-break-word ["overflow-wrap-break-word" {:overflow-wrap :break-word}]
   :overflow-wrap-anywhere ["overflow-wrap-anywhere" {:overflow-wrap :anywhere}]
   :list-inside ["list-inside" {:list-style-position :inside}]
   :list-outside ["list-outside" {:list-style-position :outside}]
   :list-none ["list-none" {:list-style-type :none}]
   :list-disc ["list-disc" {:list-style-type :disc}]
   :list-decimal ["list-decimal" {:list-style-type :decimal}]
   :font-size-inherit ["font-size-inherit" {:font-size :inherit}]
   :line-height-none ["line-height-none" {:line-height 1}]
   :line-height-inherit ["line-height-inherit" {:line-height :inherit}]
   :font-inherit ["font-inherit" {:font :inherit}]
   :font-weight-inherit ["font-weight-inherit" {:font-weight :inherit}]
   :italic ["italic" {:font-style :italic}]
   :not-italic ["not-italic" {:font-style :normal}]
   :font-mono ["font-mono" {:font-family (str/join ", " mono-font-stack)}]
   :font-thin ["font-thin" {:font-weight 100}]
   :font-extralight ["font-extralight" {:font-weight 200}]
   :font-light ["font-light" {:font-weight 300}]
   :font-normal ["font-normal" {:font-weight 400}]
   :font-medium ["font-medium" {:font-weight 500}]
   :font-semibold ["font-semibold" {:font-weight 600}]
   :font-bold ["font-bold" {:font-weight 700}]
   :font-extrabold ["font-extrabold" {:font-weight 800}]
   :font-black ["font-black" {:font-weight 900}]
   :underline ["underline" {:text-decoration-line :underline}]
   :overline ["overline" {:text-decoration-line :overline}]
   :line-through ["line-through" {:text-decoration-line :line-through}]
   :no-underline ["no-underline" {:text-decoration-line :none}]
   :antialiased ["antialiased" {:-webkit-font-smoothing :antialiased :-moz-osx-font-smoothing :grayscale}]
   :subpixel-antialiased ["subpixel-antialiased" {:-webkit-font-smoothing :auto :-moz-osx-font-smoothing :auto}]
   :decoration-solid ["decoration-solid" {:text-decoration-style :solid}]
   :decoration-double ["decoration-double" {:text-decoration-style :double}]
   :decoration-dotted ["decoration-dotted" {:text-decoration-style :dotted}]
   :decoration-dashed ["decoration-dashed" {:text-decoration-style :dashed}]
   :decoration-wavy ["decoration-wavy" {:text-decoration-style :wavy}]
   :list-image-none ["list-image-none" {:list-style-image :none}]
   :text-shadow-none ["text-shadow-none" {:text-shadow :none}]})

(defn static* [env utility]
  (let [[class-name declaration] (get static-utilities utility)]
    (compiler/simple-utility* env class-name declaration)))

(defn font-size*
  "Macro implementation for font-size utilities."
  [env size-form]
  (let [size (compiler/resolve-compile-time-constant env size-form)
        [size-class css-size] (value/length-parts size-form size "Font size" :font-size true)]
    (compiler/register-utility! env (str "font-size-" size-class) {:font-size css-size})))

(defn text*
  "Macro implementation for combined font-size and line-height utilities."
  [env size-form line-height-form]
  (let [size (compiler/resolve-compile-time-constant env size-form)
        [size-class css-size] (value/length-parts size-form size "Font size" :font-size true)
        line-height (compiler/resolve-compile-time-constant env line-height-form)
        [line-height-class line-height]
        (value/literal-parts line-height-form line-height "Line height" :line-height)
        class-name (str "text-" size-class "-line-height-" line-height-class)]
    (compiler/register-utility! env class-name {:font-size css-size
                                                :line-height line-height})))

(defn line-height*
  "Macro implementation for line-height utilities."
  [env value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part value] (value/literal-parts value-form value "Line height" :line-height)]
    (compiler/register-utility! env (str "line-height-" class-part) {:line-height value})))

(defn font*
  "Macro implementation for font-family utilities."
  [env value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value]
        (value/string-or-token-parts value-form value "Font family" :font-family)]
    (compiler/register-utility! env (str "font-" class-part)
                                {:font-family css-value})))

(defn tracking*
  "Macro implementation for letter-spacing utilities."
  [env value-form]
  (compiler/signed-length-utility* env "tracking" :letter-spacing value-form
                                   "Letter spacing" :letter-spacing))

(defn indent*
  "Macro implementation for text-indent utilities."
  [env value-form]
  (compiler/signed-length-utility* env "indent" :text-indent value-form
                                   "Text indent" :text-indent))

(defn decoration-thickness*
  "Macro implementation for text-decoration-thickness utilities."
  [env value-form]
  (compiler/length-utility* env "decoration-thickness" :text-decoration-thickness value-form
                            "Text decoration thickness" :text-decoration-thickness))

(defn underline-offset*
  "Macro implementation for text-underline-offset utilities."
  [env value-form]
  (compiler/signed-length-utility* env "underline-offset" :text-underline-offset value-form
                                   "Text underline offset" :text-underline-offset))
