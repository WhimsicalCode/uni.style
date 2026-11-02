(ns uni.style.colors
  "Compile-time OKLCH color ramps. Use with the alias sc."
  (:require [uni.style.impl.oklch :as oklch]
            #?(:clj [uni.style.impl.palette :as palette]))
  #?(:cljs (:require-macros [uni.style.colors])))

(defn hex
  "Convert a numeric palette OKLCH literal to uppercase #RRGGBB or #RRGGBBAA.
   Clips to sRGB gamut and preserves alpha. Rejects other CSS and variable alpha."
  [color]
  (oklch/hex color))

(defn rgba
  "Convert a numeric palette OKLCH literal to a CSS rgba(r,g,b,a) string.
   Clips to sRGB gamut and preserves alpha. Rejects other CSS and variable alpha."
  [color]
  (oklch/rgba color))

#?(:clj
   (do
     (defmacro def-color
       "Define a color macro from level -> [lightness chroma hue] anchors.
   Lightness/levels use 0–100; chroma is unitless and hue is in degrees.
   Levels increase from implicit black at 0 to white at 100. Each call expands
   to a literal OKLCH string. Optional alpha is a percentage or CSS-var keyword.
   Define custom macros in .clj or the :clj branch of .cljc; ClojureScript
   callers import them with :require-macros."
       [sym anchors]
       (palette/def-color* &env sym anchors))

     ;; Tailwind CSS v4.1.18 (MIT), theme.css: 950 / 600 / 50 at levels 5 / 40 / 95.
     ;; Level = 100 - Tailwind shade / 10, so higher levels are lighter.
     ;; https://github.com/tailwindlabs/tailwindcss/blob/v4.1.18/packages/tailwindcss/theme.css
     (def-color red
       {5 [25.8 0.092 26.042]
        40 [57.7 0.245 27.325]
        95 [97.1 0.013 17.38]})

     (def-color orange
       {5 [26.6 0.079 36.259]
        40 [64.6 0.222 41.116]
        95 [98 0.016 73.684]})

     (def-color amber
       {5 [27.9 0.077 45.635]
        40 [66.6 0.179 58.318]
        95 [98.7 0.022 95.277]})

     (def-color yellow
       {5 [28.6 0.066 53.813]
        40 [68.1 0.162 75.834]
        95 [98.7 0.026 102.212]})

     (def-color lime
       {5 [27.4 0.072 132.109]
        40 [64.8 0.2 131.684]
        95 [98.6 0.031 120.757]})

     (def-color green
       {5 [26.6 0.065 152.934]
        40 [62.7 0.194 149.214]
        95 [98.2 0.018 155.826]})

     (def-color emerald
       {5 [26.2 0.051 172.552]
        40 [59.6 0.145 163.225]
        95 [97.9 0.021 166.113]})

     (def-color teal
       {5 [27.7 0.046 192.524]
        40 [60 0.118 184.704]
        95 [98.4 0.014 180.72]})

     (def-color cyan
       {5 [30.2 0.056 229.695]
        40 [60.9 0.126 221.723]
        95 [98.4 0.019 200.873]})

     (def-color sky
       {5 [29.3 0.066 243.157]
        40 [58.8 0.158 241.966]
        95 [97.7 0.013 236.62]})

     (def-color blue
       {5 [28.2 0.091 267.935]
        40 [54.6 0.245 262.881]
        95 [97 0.014 254.604]})

     (def-color indigo
       {5 [25.7 0.09 281.288]
        40 [51.1 0.262 276.966]
        95 [96.2 0.018 272.314]})

     (def-color violet
       {5 [28.3 0.141 291.089]
        40 [54.1 0.281 293.009]
        95 [96.9 0.016 293.756]})

     (def-color purple
       {5 [29.1 0.149 302.717]
        40 [55.8 0.288 302.321]
        95 [97.7 0.014 308.299]})

     (def-color fuchsia
       {5 [29.3 0.136 325.661]
        40 [59.1 0.293 322.896]
        95 [97.7 0.017 320.058]})

     (def-color pink
       {5 [28.4 0.109 3.907]
        40 [59.2 0.249 0.584]
        95 [97.1 0.014 343.198]})

     (def-color rose
       {5 [27.1 0.105 12.094]
        40 [58.6 0.253 17.585]
        95 [96.9 0.015 12.422]})

     (def-color slate
       {5 [12.9 0.042 264.695]
        40 [44.6 0.043 257.281]
        95 [98.4 0.003 247.858]})

     (def-color gray
       {5 [13 0.028 261.692]
        40 [44.6 0.03 256.802]
        95 [98.5 0.002 247.839]})

     (def-color zinc
       {5 [14.1 0.005 285.823]
        40 [44.2 0.017 285.786]
        95 [98.5 0 0]})

     (def-color neutral
       {5 [14.5 0 0]
        40 [43.9 0 0]
        95 [98.5 0 0]})

     (def-color stone
       {5 [14.7 0.004 49.25]
        40 [44.4 0.011 73.639]
        95 [98.5 0.001 106.423]})))

(def ^:const white "oklch(100% 0 0)")
(def ^:const black "oklch(0% 0 0)")
