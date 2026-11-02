(ns example.styles
  (:require [uni.style.colors :as sc]
            [uni.style.core :as s]
            [uni.style.preflight :refer [import-preflight]]))

(import-preflight)

(s/def-theme light)
(s/def-theme dark)

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
  dark (sc/neutral 20)
  (sc/neutral 87))

(s/def-token ring
  dark (sc/neutral 40)
  (sc/neutral 65))

(s/def-token color-scheme
  dark "dark"
  "light")

(s/def-base "html"
  (s/bg background)
  (s/color foreground)
  (s/raw {:color-scheme (s/str color-scheme)}))

;; CSS follows the OS in Auto mode; explicit themes need no browser-side code.
(s/def-base "html[data-theme='auto']"
  (s/media "(prefers-color-scheme: dark)"
    (s/var background (s/token-value background dark))
    (s/var foreground (s/token-value foreground dark))
    (s/var muted (s/token-value muted dark))
    (s/var muted-foreground (s/token-value muted-foreground dark))
    (s/var border (s/token-value border dark))
    (s/var ring (s/token-value ring dark))
    (s/var color-scheme (s/token-value color-scheme dark))))

(def focus-ring
  (s/c (s/outline-none)
       (s/focus-visible
         (s/outline 2 ring)
         (s/outline-offset 3))))

(def row
  (s/c (s/flex)
       (s/items-center)
       (s/gap 12)))

(def button
  (s/c focus-ring
       (s/inline-flex)
       (s/items-center)
       (s/justify-center)
       (s/h 36)
       (s/px 16)
       (s/rounded 6)
       (s/text 14 1.5)
       (s/font-medium)
       (s/cursor-pointer)
       (s/whitespace-nowrap)
       (s/bg foreground)
       (s/color background)
       (s/hover
         (s/opacity 85))))

(def quiet-button
  (s/c button
       (s/bg-transparent)
       (s/color muted-foreground)
       (s/px 10)
       (s/hover
         (s/bg muted)
         (s/color foreground)
         (s/opacity 100))))
