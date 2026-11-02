(ns uni.style.fixtures.portable
  (:require [uni.style.core :as s]
            [uni.style.preflight :as preflight]))

(def ^:const spacing 4)
(def ^:const background-token :example-bg)
(def ^:const doubled-spacing (s/* spacing 2))

(preflight/import-preflight {:font-family "Example Sans"})

(s/def-base "body"
  (s/color "black"))

(s/def-component panel
  (s/p spacing)
  (s/bg background-token)
  (s/hover (s/bg "red")))

(s/def-component compact-panel
  panel
  (s/pr 2))

(def focus-style
  [(s/outline-solid)
   (s/outline-color :focus-ring "blue")
   (s/outline-width :focus-ring-width "3px")
   (s/outline-offset :focus-ring-offset "0px")])

(s/def-component focus-panel
  focus-style)

(s/def-component focus-visible-panel
  (s/focus-visible focus-style))

(def ^:const document-css
  (s/css
    {(s/str "z" "-panel") (s/components (s/color "blue"))
     ".document" [(s/p 4)
                  (s/p 8)
                  (s/sel ["p" "h2"]
                    (s/color :example-text)
                    (s/hover (s/color "red")))
                  (s/media "(min-width: 10px)"
                    (s/sel "p" (s/font-size 18)))]}))

(def selector-list-css
  (s/css {":is(.a,.b), [title=\"],x\"], .escaped\\,comma" (s/hover (s/color "red"))}))

(s/def-variants appearance
  {:primary [(s/color "white")]
   [:large :wide] [(s/p doubled-spacing)]})

(s/def-animation fade-in
  {:duration "200ms"
   :frames {0 [(s/opacity 0)]
            100 [(s/opacity 100)]}})

(defn classes [compact?]
  (s/class-name
    (s/c panel
         (get appearance :primary)
         fade-in
         (s/p doubled-spacing)
         (when compact? (s/p spacing))
         (s/hover (s/color "red")))))

(defn utility-class []
  (s/flex))
