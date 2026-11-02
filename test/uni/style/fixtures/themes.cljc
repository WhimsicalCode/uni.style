(ns uni.style.fixtures.themes
  (:require [uni.style.core :as s]))

(s/def-theme light)
(s/def-theme dark)
(s/def-token background dark "#111" "#fff")
(s/def-token foreground light "#222" dark "#eee" "#333")
(s/def-token alias-background background)
(s/def-token radius "8px")
(s/def-token factor 0.5)
(s/def-token overlay dark (s/alpha "#fff" 20) (s/alpha "#000" 40))
(s/def-token tinted-background (s/alpha background 50))
(def ^:const shadow (s/str "0 1px 2px " (s/alpha "#000" 20)))
(s/def-token shadow-token shadow)

(defn literal-background-class []
  (s/bg (s/token-value background dark)))
