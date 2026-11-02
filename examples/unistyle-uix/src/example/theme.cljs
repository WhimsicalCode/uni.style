(ns example.theme
  (:require [uni.style.colors :as sc]
            [uni.style.core :as s]
            [uni.style.preflight :refer [import-preflight]]))

(import-preflight)

(s/def-theme light)
(s/def-theme dark)

;; Semantic tokens keep the components independent of the active theme.

(s/def-token background
  dark (sc/neutral 5)
  sc/white)

(s/def-token foreground
  dark (sc/neutral 95)
  (sc/neutral 5))

(s/def-token muted
  dark (sc/neutral 10)
  (sc/neutral 93))

(s/def-token muted-foreground
  dark (sc/neutral 65)
  (sc/neutral 40))

(s/def-token border
  dark (s/alpha sc/white 12)
  (sc/neutral 87))

(s/def-token input
  dark (sc/neutral 20)
  (sc/neutral 85))

(s/def-token primary
  dark (sc/neutral 87)
  (sc/neutral 10))

(s/def-token primary-foreground
  dark (sc/neutral 10)
  (sc/neutral 95))

(s/def-token destructive
  dark (sc/red 60)
  (sc/red 40))

(s/def-token destructive-foreground
  dark (sc/neutral 5)
  sc/white)

(s/def-token ring
  dark (sc/neutral 40)
  (sc/neutral 65))

(s/def-token thumb
  dark (sc/neutral 95)
  sc/white)

(s/def-token color-scheme
  dark "dark"
  "light")

(s/def-base "html"
  (s/bg background)
  (s/color foreground)
  (s/raw {:color-scheme (s/str color-scheme)}))

(def storage-key "unistyle-uix.theme")
(def modes #{"auto" "light" "dark"})

(defn read-mode []
  (let [stored (try
                 (.getItem js/localStorage storage-key)
                 (catch :default _ nil))]
    (if (modes stored) stored "auto")))

(defn apply-mode! [mode]
  (case mode
    "dark" (s/set-theme! dark)
    "light" (s/set-theme! light)
    (s/set-theme-system! light dark)))

(defn select-mode! [mode]
  (let [mode (if (modes mode) mode "auto")]
    (apply-mode! mode)
    ;; Sandboxed browsers can deny storage; theme switching should still work.
    (try
      (.setItem js/localStorage storage-key mode)
      (catch :default _ nil))))
