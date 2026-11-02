(ns uni.style.impl.transition
  "Transition shorthand, property, behavior, duration, timing, and delay utilities."
  (:require [clojure.string :as str]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.value :as value]))

(def default-duration "150ms")
(def default-timing-function "cubic-bezier(0.4, 0, 0.2, 1)")

(def color-properties
  "color, background-color, border-color, outline-color, text-decoration-color, fill, stroke")

(def static-utilities
  {:transition-all ["transition-all" "all"]
   :transition-colors ["transition-colors" color-properties]
   :transition-opacity ["transition-opacity" "opacity"]
   :transition-shadow ["transition-shadow" "box-shadow"]
   :transition-transform ["transition-transform" "transform, translate, scale, rotate"]})

(def behavior-utilities
  {:transition-normal ["transition-normal" :normal]
   :transition-discrete ["transition-discrete" :allow-discrete]})

(def timing-utilities
  {:ease-linear ["ease-linear" :linear]
   :ease-in ["ease-in" "cubic-bezier(0.4, 0, 1, 1)"]
   :ease-out ["ease-out" "cubic-bezier(0, 0, 0.2, 1)"]
   :ease-in-out ["ease-in-out" "cubic-bezier(0.4, 0, 0.2, 1)"]
   :ease-initial ["ease-initial" :initial]})

(defn- transition-declaration [property]
  {:transition-property property
   :transition-duration default-duration
   :transition-timing-function default-timing-function})

(defn static* [env utility]
  (if (= utility :transition-none)
    (compiler/simple-utility* env "transition-none" {:transition-property :none})
    (let [[class-name property] (get static-utilities utility)]
      (compiler/simple-utility* env class-name (transition-declaration property)))))

(defn behavior* [env utility]
  (let [[class-name behavior] (get behavior-utilities utility)]
    (compiler/simple-utility* env class-name {:transition-behavior behavior})))

(defn duration-initial* [env]
  (compiler/simple-utility* env "duration-initial" {:transition-duration :initial}))

(defn timing* [env utility]
  (let [[class-name timing] (get timing-utilities utility)]
    (compiler/simple-utility* env class-name {:transition-timing-function timing})))

(defn- literal-or-var-parts [form resolved label type]
  (cond
    (integer? resolved)
    [(value/number-class-part resolved) resolved]

    (string? resolved)
    (do
      (when (str/blank? resolved)
        (throw (ex-info (str label " must not be empty")
                        {:form form :value resolved :type type})))
      [(str "value-" (compiler/content-hash resolved)) resolved])

    (keyword? resolved)
    (let [property-name (value/css-var-property-name form resolved label type)]
      [(str "-" (name resolved)) (str "var(" property-name ")")])

    :else
    (throw (ex-info (str label " must be a compile-time integer, CSS string, or variable keyword")
                    {:form form :value resolved :type type}))))

(defn transition* [env value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part value] (value/string-or-token-parts value-form value
                                                        "Transition" :transition)]
    (compiler/register-utility! env (str "transition-" class-part)
                                {:transition value})))

(defn- time-parts [form resolved label type]
  (let [[class-part css-value] (literal-or-var-parts form resolved label type)]
    [class-part (if (integer? resolved)
                  (str css-value "ms")
                  css-value)]))

(defn duration* [env duration-form]
  (let [duration (compiler/resolve-compile-time-constant env duration-form)
        [class-part duration] (time-parts duration-form duration
                                          "Transition duration" :transition-duration)]
    (compiler/register-utility! env (str "duration-" class-part)
                                {:transition-duration duration})))

(defn ease* [env timing-form]
  (let [timing (compiler/resolve-compile-time-constant env timing-form)
        [class-part timing] (literal-or-var-parts timing-form timing
                                                  "Transition timing function"
                                                  :transition-timing-function)]
    (compiler/register-utility! env (str "ease-" class-part)
                                {:transition-timing-function timing})))

(defn delay* [env delay-form]
  (let [delay (compiler/resolve-compile-time-constant env delay-form)
        [class-part delay] (time-parts delay-form delay
                                       "Transition delay" :transition-delay)]
    (compiler/register-utility! env (str "delay-" class-part)
                                {:transition-delay delay})))
