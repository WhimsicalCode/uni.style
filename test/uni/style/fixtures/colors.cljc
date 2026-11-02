(ns uni.style.fixtures.colors
  (:require [uni.style.colors :as sc]
            [uni.style.core :as s]
            #?(:clj [uni.style.fixtures.color-macros :refer [brand]]))
  #?(:cljs (:require-macros [uni.style.fixtures.color-macros :refer [brand]])))

(def ^:const middle 40)
(def ^:const alpha 50)
(def ^:const alpha-token :fixture-alpha)

;; These calls are outside any parent style macro.
(def standalone (sc/green 40))
(def ^:const constant-color (sc/green middle))
(def ^:const constant-alpha-color (sc/green middle alpha))
(def ^:const constant-variable-alpha (brand middle alpha-token))

(defn constant-background-class []
  (s/bg constant-color))

(defn local-reference []
  (let [x (sc/green 40)] x))

(s/def-component panel
  (s/bg (brand middle))
  (s/hover (s/color (sc/green (+ middle 10) alpha)))
  (s/border 1 (sc/blue 40 alpha-token)))

(s/def-token background (sc/green 40))
(s/def-token concrete-background constant-color)
(s/def-theme dark)
(s/def-token themed-background dark (brand 5 25) (sc/green 40))
(s/def-token background-alias themed-background)
(s/def-token translucent-background (s/alpha (brand 40) 50))
(s/def-token variable-alpha-background (brand 40 :fixture-alpha))
(s/def-token gradient-background
  (s/str "linear-gradient(" (sc/green 95) ", " (brand 5 50) ")"))

(def ^:const gradient
  (s/str "linear-gradient(" (sc/green 95) ", " (brand 5 50) ")"))

(defn background-class []
  (s/bg (brand middle)))

(defn border-class []
  (s/border 1 (sc/blue 40 alpha-token)))
