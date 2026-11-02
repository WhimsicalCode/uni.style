(ns uni.style.preflight
  "Tailwind-compatible browser preflight styles for uni.style."
  (:require [clojure.string :as str]
            [uni.style.core :as s]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.typography :as typography]))

(def ^:private option-keys
  #{:font-family
    :font-feature-settings
    :font-variation-settings
    :mono-font-family
    :mono-font-feature-settings
    :mono-font-variation-settings})

(def ^:private default-options
  {:font-family nil
   :font-feature-settings "normal"
   :font-variation-settings "normal"
   :mono-font-family nil
   :mono-font-feature-settings "normal"
   :mono-font-variation-settings "normal"})

(def ^:private sans-font-family
  ["-apple-system"
   "BlinkMacSystemFont"
   "'Segoe UI'"
   "Roboto"
   "'Helvetica Neue'"
   "'Noto Sans'"
   "Arial"
   "sans-serif"
   "'Apple Color Emoji'"
   "'Segoe UI Emoji'"
   "'Segoe UI Symbol'"
   "'Noto Color Emoji'"])

(defn- validate-string-option! [options option nilable?]
  (let [value (get options option)]
    (when-not (or (and nilable? (nil? value))
                  (and (string? value) (not (str/blank? value))))
      (throw (ex-info (str option " must be a non-empty compile-time string"
                           (when nilable? " or nil"))
                      {:option option :value value :type :preflight-option})))))

(defn- resolve-options [env options-form]
  (let [options (if (map? options-form)
                  (into {}
                        (map (fn [[option value-form]]
                               [option (compiler/resolve-compile-time-constant env value-form)]))
                        options-form)
                  (compiler/resolve-compile-time-constant env options-form))]
    (when-not (map? options)
      (throw (ex-info "Preflight options must be a compile-time map"
                      {:form options-form :value options :type :preflight-options})))
    (let [unknown-keys (seq (remove option-keys (keys options)))
          options (merge default-options options)]
      (when unknown-keys
        (throw (ex-info "Unsupported preflight options"
                        {:options options :unknown-keys unknown-keys
                         :type :preflight-options})))
      (validate-string-option! options :font-family true)
      (validate-string-option! options :font-feature-settings false)
      (validate-string-option! options :font-variation-settings false)
      (validate-string-option! options :mono-font-family true)
      (validate-string-option! options :mono-font-feature-settings false)
      (validate-string-option! options :mono-font-variation-settings false)
      options)))

(defn- font-stack [custom-family fallback-families]
  (str/join ", " (if custom-family
                   (cons custom-family fallback-families)
                   fallback-families)))

(defmacro import-preflight
  "Import Tailwind-compatible preflight styles in the base cascade layer.
   Accepts an optional compile-time options map with sans and mono font
   families, feature settings, and variation settings."
  ([]
   `(import-preflight {}))
  ([options-form]
   (let [{:keys [font-family
                 font-feature-settings
                 font-variation-settings
                 mono-font-family
                 mono-font-feature-settings
                 mono-font-variation-settings]}
         (resolve-options &env options-form)
         sans-family (font-stack font-family sans-font-family)
         mono-family (font-stack mono-font-family typography/mono-font-stack)]
     `(do
        (s/def-base "*, ::after, ::before, ::backdrop, ::file-selector-button"
          (s/box-border)
          (s/m 0)
          (s/p 0)
          (s/raw {:border "0 solid"}))

        (s/def-base "html, :host"
          (s/line-height 1.5)
          (s/font ~sans-family)
          (s/raw {:-webkit-text-size-adjust "100%"
                  :tab-size 4
                  :font-feature-settings ~font-feature-settings
                  :font-variation-settings ~font-variation-settings
                  :-webkit-tap-highlight-color :transparent}))

        (s/def-base "hr"
          (s/h 0)
          (s/color-inherit)
          (s/raw {:border-top-width 1}))

        (s/def-base "abbr:where([title])"
          (s/raw {:-webkit-text-decoration "underline dotted"
                  :text-decoration "underline dotted"}))

        (s/def-base "h1, h2, h3, h4, h5, h6"
          (s/font-size-inherit)
          (s/font-weight-inherit))

        (s/def-base "a"
          (s/color-inherit)
          (s/raw {:-webkit-text-decoration :inherit
                  :text-decoration :inherit}))

        (s/def-base "b, strong"
          (s/raw {:font-weight :bolder}))

        (s/def-base "code, kbd, samp, pre"
          (s/font ~mono-family)
          (s/font-size "1em")
          (s/raw {:font-feature-settings ~mono-font-feature-settings
                  :font-variation-settings ~mono-font-variation-settings}))

        (s/def-base "small"
          (s/font-size "80%"))

        (s/def-base "sub, sup"
          (s/font-size "75%")
          (s/line-height 0)
          (s/relative)
          (s/align-baseline))

        (s/def-base "sub"
          (s/bottom "-0.25em"))

        (s/def-base "sup"
          (s/top "-0.5em"))

        (s/def-base "table"
          (s/raw {:text-indent 0
                  :border-color :inherit})
          (s/border-collapse))

        (s/def-base ":-moz-focusring:where(:not(iframe))"
          (s/raw {:outline :auto}))

        (s/def-base "progress"
          (s/align-baseline))

        (s/def-base "summary"
          (s/list-item))

        (s/def-base "ol, ul, menu"
          (s/list-none))

        (s/def-base "img, svg, video, canvas, audio, iframe, embed, object"
          (s/block)
          (s/align-middle))

        (s/def-base "img, video"
          (s/max-w-full)
          (s/h-auto))

        (s/def-base "button, input, select, optgroup, textarea, ::file-selector-button"
          (s/font-inherit)
          (s/color-inherit)
          (s/rounded 0)
          (s/bg-transparent)
          (s/opacity 100)
          (s/raw {:font-feature-settings :inherit
                  :font-variation-settings :inherit})
          (s/tracking "inherit"))

        (s/def-base ":where(select:is([multiple], [size])) optgroup"
          (s/raw {:font-weight :bolder}))

        (s/def-base ":where(select:is([multiple], [size])) optgroup option"
          (s/ps 20))

        (s/def-base "::file-selector-button"
          (s/me 4))

        (s/def-base "::placeholder"
          (s/opacity 100))

        (s/def-base "::placeholder"
          (s/supports "(not (-webkit-appearance: -apple-pay-button)) or (contain-intrinsic-size: 1px)"
            (s/color "color-mix(in oklab, currentcolor 50%, transparent)")))

        (s/def-base "textarea"
          (s/resize-y))

        (s/def-base "::-webkit-search-decoration"
          (s/raw {:-webkit-appearance :none}))

        (s/def-base "::-webkit-date-and-time-value"
          (s/min-h "1lh")
          (s/raw {:text-align :inherit}))

        (s/def-base "::-webkit-datetime-edit"
          (s/inline-flex))

        (s/def-base "::-webkit-datetime-edit-fields-wrapper"
          (s/p 0))

        (s/def-base "::-webkit-datetime-edit, ::-webkit-datetime-edit-year-field, ::-webkit-datetime-edit-month-field, ::-webkit-datetime-edit-day-field, ::-webkit-datetime-edit-hour-field, ::-webkit-datetime-edit-minute-field, ::-webkit-datetime-edit-second-field, ::-webkit-datetime-edit-millisecond-field, ::-webkit-datetime-edit-meridiem-field"
          (s/py 0))

        (s/def-base "::-webkit-calendar-picker-indicator"
          (s/line-height 1))

        (s/def-base ":-moz-ui-invalid"
          (s/raw {:box-shadow :none}))

        (s/def-base "button, input:where([type='button'], [type='reset'], [type='submit']), ::file-selector-button"
          (s/raw {:appearance :button}))

        (s/def-base "::-webkit-inner-spin-button, ::-webkit-outer-spin-button"
          (s/h-auto))

        (s/def-base "[hidden]:where(:not([hidden='until-found']))"
          (s/raw {:display "none !important"}))
        nil))))
