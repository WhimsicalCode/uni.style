(ns uni.style.fixtures.shared-color-macros
  #?(:clj (:require [uni.style.colors :as sc]))
  #?(:cljs (:require-macros [uni.style.fixtures.shared-color-macros])))

#?(:clj (sc/def-color brand {50 [50 0.1 180]}))
