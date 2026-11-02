(ns uni.style.fixtures.token-listing
  (:require [uni.style.core :as s]))

(s/def-token background "#111")

;; Non-token definitions must not be listed as tokens.
(s/def-theme background-sunken)
(def ^:const background-plain :ordinary-keyword)
