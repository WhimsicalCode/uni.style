(ns uni.style.utilities-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [clojure.string :as str]
            [uni.style.core :as s]
            [uni.style.fixtures.tokens :as tokens]
            [uni.style.runtime :as style.runtime]))

(def ^:const local-border-token :app-local-border)
(def ^:const class-map-x :x)
(def ^:const class-map-y :y)
(def ^:const class-map-z :z)
(def ^:const compile-time-base-z-index 1)
(def ^:const compile-time-title-z-index (s/inc compile-time-base-z-index))
(def ^:const compile-time-background-z-index (s/dec compile-time-base-z-index))
(def ^:const compile-time-sum (s/+ compile-time-base-z-index 3))
(def ^:const compile-time-difference (s/- compile-time-base-z-index 3))
(def ^:const compile-time-product (s/* compile-time-base-z-index 3))
(def ^:const compile-time-quotient (s// compile-time-base-z-index 2))

(defn runtime-custom-property-props [hover-color]
  {:class (first
            (s/hover
              (s/bg "var(--invite-remove-hover-bg)")))
   :style {"--invite-remove-hover-bg" hover-color}})

(defn var-def-props [hover-color]
  {:class (first
            (s/hover
              (s/bg :invite-remove-hover-bg)))
   :style {(s/var-def :invite-remove-hover-bg) hover-color}})

(s/def-base "html, body"
  (s/raw {:-webkit-text-size-adjust "100%"})
  (s/m 0)
  (s/p 0))

(s/def-component test-animation-frame-base
  (s/opacity 0))

(s/def-animation test-animation
  {:duration "3s"
   :easing "ease-in"
   :iterations 2
   :frames {0 [test-animation-frame-base (s/scale 80)]
            [10 20] [(s/opacity 20) (s/scale 90)]
            100 [(s/opacity 100) (s/scale 100)]}})

(s/def-component base-classes
  (s/raw {:-webkit-text-decoration :inherit})
  (s/flex)
  (s/font-normal))

(s/def-component derived-classes
  base-classes
  (s/font-size 16))

(s/def-component modified-classes
  (s/block)
  (s/hover
    (s/opacity 50)))

(s/def-component derived-modified-classes
  modified-classes
  (s/disabled
    (s/opacity 25)))

(s/def-component global-classes
  (s/block))

(s/def-variants class-map
  {class-map-x [(s/raw {:-webkit-user-select :none})
                (s/p 8)]
   [class-map-y class-map-z] [(s/p 12)
                              (s/m 8)]})

(s/def-variants global-class-map
  {:stable (s/block)})

(def predefined-layer-classes
  [(s/base (s/block))
   (s/components (s/block))
   (s/variants (s/block))])

(def layered-classes
  (s/components
    (s/opacity 50)
    (s/hover (s/opacity 100))))

(def layered-modified-classes
  (s/variants modified-classes))

(s/def-component layered-class
  (s/font-inherit)
  (s/line-height-inherit))

(s/def-component layered-composed-class
  base-classes
  (s/line-height-inherit))

(defn- with-bundle-css-link
  "Calls f with a bundle-css link whose href is the given URL: a link element
   in a browser, a stub document under Node.js (reload-css! only reads and
   sets the link's href)."
  [href f]
  (if (exists? js/document)
    (let [link (.createElement js/document "link")
          existing (.querySelector js/document "#bundle-css")]
      (when existing (.removeAttribute existing "id"))
      (set! (.-id link) "bundle-css")
      (set! (.-rel link) "stylesheet")
      (set! (.-href link) href)
      (.appendChild (.-head js/document) link)
      (try
        (f link)
        (finally
          (.remove link)
          (when existing (set! (.-id existing) "bundle-css")))))
    (let [link #js {:href href}]
      (set! (.-document js/globalThis)
            #js {:querySelector (fn [selector] (when (= "#bundle-css" selector) link))})
      (try
        (f link)
        (finally
          (js-delete js/globalThis "document"))))))

(deftest repeated-css-hot-reload-test
  (with-bundle-css-link "https://example.test/bundle.css?existing=preserved"
    (fn [link]
      (let [versions (repeatedly
                       5
                       #(do
                          (style.runtime/reload-css!)
                          (.get (.-searchParams (js/URL. (.-href link))) "v")))]
        (is (= 5 (count (distinct versions))))
        (is (= "preserved"
               (.get (.-searchParams (js/URL. (.-href link))) "existing")))))))

(deftest compile-time-value-helper-test
  (is (= [2 0 4 -2 3 0.5]
         [compile-time-title-z-index
          compile-time-background-z-index
          compile-time-sum
          compile-time-difference
          compile-time-product
          compile-time-quotient]))
  (is (= "z-2" (s/z compile-time-title-z-index)))
  (is (= "z-0" (s/z compile-time-background-z-index))))

(deftest raw-utility-test
  (is (re-matches #"raw-[0-9a-f]{8}"
                  (s/raw {:-webkit-text-decoration :inherit})))
  (is (re-matches #"supports-[0-9a-f]{8}:raw-[0-9a-f]{8}"
                  (first
                    (s/supports "(-electron-corner-smoothing: 1%)"
                      (s/raw {:-electron-corner-smoothing :system-ui}))))))

(deftest animation-utility-test
  (is (re-matches #"test-animation-[0-9a-f]{8}" test-animation))
  (is (= "animation-none" (s/animation-none))))

(defn- axes-classes [class-name axis-prefix shared]
  (str class-name "----us-" axis-prefix "-x " class-name "----us-" axis-prefix "-y " shared))

(deftest transform-utility-test
  (is (= [(axes-classes "scale-80" "scale" "scale-axes")
          (axes-classes "scale-100" "scale" "scale-axes")
          (axes-classes "scale--scale-factor" "scale" "scale-axes")]
         [(s/scale 80) (s/scale 100) (s/scale :scale-factor)]))
  (is (= ["scale-x--100 scale-axes" "scale-y-50 scale-axes" "scale-none"]
         [(s/scale-x -100) (s/scale-y 50) (s/scale-none)]))
  (is (= ["rotate-45" "rotate--rotation" "rotate-none"]
         [(s/rotate 45) (s/rotate :rotation) (s/rotate-none)]))
  (is (= (axes-classes "translate-10--20" "translate" "translate-axes") (s/translate 10 -20)))
  (is (= (axes-classes "translate-1_5--0_5" "translate" "translate-axes") (s/translate 1.5 -0.5)))
  (is (= (axes-classes "translate--offset-x--offset-y" "translate" "translate-axes")
         (s/translate :offset-x :offset-y)))
  (is (= "translate-x-10 translate-axes" (s/translate-x 10)))
  (is (= "translate-y--0_5 translate-axes" (s/translate-y -0.5)))
  (is (= "translate-x--offset-x translate-axes" (s/translate-x :offset-x)))
  (is (= "translate-y--offset-y translate-axes" (s/translate-y :offset-y)))
  (is (= "translate-x-10 translate-axes rotate-45"
         (s/class-name (s/c (s/translate-x 10) (s/rotate 45)))))
  (is (re-matches #"transform-value-[0-9a-f]{8}" (s/transform "rotate(90deg) translateX(1px)")))
  (is (= ["translate-none" "transform-none"] [(s/translate-none) (s/transform-none)])))

(deftest transition-utility-test
  (is (= ["transition-all--transition-property transition-all--transition-duration transition-all--transition-timing-function"
          "transition-colors--transition-property transition-colors--transition-duration transition-colors--transition-timing-function"
          "transition-opacity--transition-property transition-opacity--transition-duration transition-opacity--transition-timing-function"
          "transition-shadow--transition-property transition-shadow--transition-duration transition-shadow--transition-timing-function"
          "transition-transform--transition-property transition-transform--transition-duration transition-transform--transition-timing-function"
          "transition-none" "transition-normal" "transition-discrete"
          "duration-150" "duration-initial" "duration--duration"
          "ease-linear" "ease-in" "ease-out" "ease-in-out" "ease-initial"
          "ease--ease" "delay-75"]
         [(s/transition-all) (s/transition-colors)
          (s/transition-opacity) (s/transition-shadow) (s/transition-transform)
          (s/transition-none) (s/transition-normal) (s/transition-discrete)
          (s/duration 150) (s/duration-initial) (s/duration :duration)
          (s/ease-linear) (s/ease-in) (s/ease-out) (s/ease-in-out) (s/ease-initial)
          (s/ease :ease) (s/delay 75)]))
  (is (re-matches #"transition-value-[0-9a-f]{8}"
                  (s/transition "background 100ms ease-in-out")))
  (is (= "transition--transition"
         (s/transition :transition)))
  (let [transition (s/transition "opacity")]
    (is (= (str transition " duration-200")
           (s/class-name (s/c transition (s/duration 200)))))
    (is (= transition
           (s/class-name (s/c (s/duration 200) transition)))))
  (is (re-matches #"ease-value-[0-9a-f]{8}"
                  (s/ease "ease-in-out"))))

(deftest child-modifier-test
  (is (= ["*:grow-2"]
         (s/* (s/grow 2))))
  (is (= ["**:min-w-0"]
         (s/** (s/min-w 0))))
  (is (= ["*:hover:opacity-50"]
         (s/* (s/hover (s/opacity 50)))))
  (is (re-matches #"sel-[0-9a-f]{8}:font-bold"
                  (first (s/sel "h2" (s/font-bold)))))
  (is (re-matches #"sel-[0-9a-f]{8}:text-left"
                  (first (s/sel ["td" "th"] (s/text-left)))))
  (is (= ["*:first:mt-0" "*:last:mb-0"]
         (s/*
           (s/first (s/mt 0))
           (s/last (s/mb 0))))))

(deftest selector-and-query-modifier-test
  (is (re-matches #"has-[0-9a-f]{8}:shadow--us-shadow-subtle-0635c9ce has-[0-9a-f]{8}:shadow-layers"
                  (first
                    (s/has ".remove-invite:hover"
                      (s/shadow tokens/shadow-subtle)))))
  (is (= "pseudo--webkit-slider-thumb:fill--us-bg-base-2a5b0631"
         (first
           (s/pseudo "-webkit-slider-thumb"
             (s/fill tokens/bg-base)))))
  (is (= "hover:pseudo--moz-range-thumb:opacity-50"
         (first
           (s/hover
             (s/pseudo :-moz-range-thumb
               (s/opacity 50))))))
  (is (= ["before:opacity-50" "after:opacity-50"]
         [(first (s/before (s/opacity 50)))
          (first (s/after (s/opacity 50)))]))
  (is (re-matches #"container-[0-9a-f]{8}:hidden"
                  (first
                    (s/container "(max-width: 300px)"
                      (s/hidden))))))

(deftest child-pseudo-modifier-test
  (is (= ["first:opacity-50" "last:opacity-50" "only:opacity-50"
          "odd:opacity-50" "even:opacity-50" "empty:opacity-50"
          "first-of-type:opacity-50" "last-of-type:opacity-50"
          "only-of-type:opacity-50"]
         [(first (s/first (s/opacity 50)))
          (first (s/last (s/opacity 50)))
          (first (s/only (s/opacity 50)))
          (first (s/odd (s/opacity 50)))
          (first (s/even (s/opacity 50)))
          (first (s/empty (s/opacity 50)))
          (first (s/first-of-type (s/opacity 50)))
          (first (s/last-of-type (s/opacity 50)))
          (first (s/only-of-type (s/opacity 50)))]))
  (doseq [classes [(s/nth 3 (s/opacity 50))
                   (s/nth-last "2n + 1" (s/opacity 50))
                   (s/nth-of-type "odd" (s/opacity 50))
                   (s/nth-last-of-type 2 (s/opacity 50))]]
    (is (re-matches #"[^:]+-[0-9a-f]{8}:opacity-50" (first classes)))))

(deftest marker-and-ancestor-class-test
  (testing "keyword and string classes are used as is"
    (is (= ["ancestor-menu-9ca8e983:opacity-50"] (s/ancestor :menu (s/opacity 50))))
    (is (= (s/ancestor :menu (s/opacity 50)) (s/ancestor "menu" (s/opacity 50))))
    (is (= "menu flex" (s/class-name (s/c :menu (s/flex))))))

  (testing "ancestor modifiers compose with nested modifiers"
    (is (= ["ancestor-menu-9ca8e983:hover:bg--us-bg-base-2a5b0631"]
           (s/ancestor :menu
             (s/hover
               (s/bg tokens/bg-base)))))
    (is (= ["self-or-ancestor-menu-9ca8e983:focus:color--us-fg-base-ad253ba1"]
           (s/self-or-ancestor :menu
             (s/focus
               (s/color tokens/fg-base))))))

  (testing "ancestor state modifiers apply state to the ancestor"
    (is (= ["ancestor-hover-menu-9ca8e983:bg--us-bg-base-2a5b0631"]
           (s/ancestor-hover :menu (s/bg tokens/bg-base))))
    (is (= ["ancestor-focus-within-menu-9ca8e983:opacity-50"]
           (s/ancestor-focus-within :menu (s/opacity 50))))
    (is (str/starts-with? (first (s/ancestor-has :menu "input" (s/opacity 50)))
                          "ancestor-has-"))
    (is (= ["ancestor-attr-disabled-menu-9ca8e983:opacity-50"]
           (s/ancestor-attr :menu :disabled (s/opacity 50)))))

  (testing "group macros use the group class"
    (is (= "group" (s/group)))
    (is (= (s/ancestor :group (s/opacity 50))
           (s/group (s/opacity 50))))
    (is (= (s/ancestor-hover :group (s/opacity 50))
           (s/group-hover (s/opacity 50))))
    (is (= (s/ancestor-attr= :group :aria-expanded true (s/opacity 50))
           (s/group-attr= :aria-expanded true (s/opacity 50))))))

(deftest peer-modifier-test
  (is (= "peer" (s/peer)))

  (doseq [[variant classes]
          [["peer-hover" (s/peer-hover (s/opacity 50))]
           ["peer-focus" (s/peer-focus (s/opacity 50))]
           ["peer-focus-within" (s/peer-focus-within (s/opacity 50))]
           ["peer-focus-visible" (s/peer-focus-visible (s/opacity 50))]
           ["peer-active" (s/peer-active (s/opacity 50))]
           ["peer-visited" (s/peer-visited (s/opacity 50))]
           ["peer-target" (s/peer-target (s/opacity 50))]
           ["peer-first" (s/peer-first (s/opacity 50))]
           ["peer-last" (s/peer-last (s/opacity 50))]
           ["peer-only" (s/peer-only (s/opacity 50))]
           ["peer-odd" (s/peer-odd (s/opacity 50))]
           ["peer-even" (s/peer-even (s/opacity 50))]
           ["peer-first-of-type" (s/peer-first-of-type (s/opacity 50))]
           ["peer-last-of-type" (s/peer-last-of-type (s/opacity 50))]
           ["peer-only-of-type" (s/peer-only-of-type (s/opacity 50))]
           ["peer-empty" (s/peer-empty (s/opacity 50))]
           ["peer-disabled" (s/peer-disabled (s/opacity 50))]
           ["peer-not-disabled" (s/peer-not-disabled (s/opacity 50))]
           ["peer-enabled" (s/peer-enabled (s/opacity 50))]
           ["peer-checked" (s/peer-checked (s/opacity 50))]
           ["peer-indeterminate" (s/peer-indeterminate (s/opacity 50))]
           ["peer-default" (s/peer-default (s/opacity 50))]
           ["peer-optional" (s/peer-optional (s/opacity 50))]
           ["peer-required" (s/peer-required (s/opacity 50))]
           ["peer-valid" (s/peer-valid (s/opacity 50))]
           ["peer-invalid" (s/peer-invalid (s/opacity 50))]
           ["peer-user-valid" (s/peer-user-valid (s/opacity 50))]
           ["peer-user-invalid" (s/peer-user-invalid (s/opacity 50))]
           ["peer-in-range" (s/peer-in-range (s/opacity 50))]
           ["peer-out-of-range" (s/peer-out-of-range (s/opacity 50))]
           ["peer-placeholder-shown" (s/peer-placeholder-shown (s/opacity 50))]
           ["peer-autofill" (s/peer-autofill (s/opacity 50))]
           ["peer-read-only" (s/peer-read-only (s/opacity 50))]
           ["peer-open" (s/peer-open (s/opacity 50))]]]
    (is (= [(str variant ":opacity-50")] classes)))

  (doseq [classes [(s/peer-nth 3 (s/opacity 50))
                   (s/peer-nth-last "2n + 1" (s/opacity 50))
                   (s/peer-nth-of-type "odd" (s/opacity 50))
                   (s/peer-nth-last-of-type 2 (s/opacity 50))]]
    (is (re-matches #"peer-[^:]+-[0-9a-f]{8}:opacity-50" (first classes))))

  (is (re-matches #"peer-has-[0-9a-f]{8}:opacity-50"
                  (first (s/peer-has "input:checked" (s/opacity 50)))))
  (is (re-matches #"peer-attr-data-state-value-[0-9a-f]{8}:opacity-50"
                  (first (s/peer-attr= :data-state :open (s/opacity 50)))))
  (is (= ["peer-attr-disabled:opacity-50"]
         (s/peer-attr :disabled (s/opacity 50))))
  (is (= ["peer-empty:hover:opacity-50"]
         (s/peer-empty (s/hover (s/opacity 50))))))

(deftest style-class-registration-test
  (let [registrations- (atom 0)
        create-descriptor #(do (swap! registrations- inc)
                               ["[]|display" "test-style-class"])]
    (is (= "test-style-class"
           (s/style-class "test-style-class" 1 create-descriptor)))
    (s/style-class "test-style-class" 1 create-descriptor)
    (is (= 1 @registrations-))
    (s/style-class "test-style-class" 2 create-descriptor)
    (is (= 2 @registrations-))
    (is (= "test-style-class" (s/class-name (s/c "test-style-class"))))))

(deftest class-merge-test
  (is (s/uni-style? (s/c)))
  (is (= "" (s/class-name (s/c))))
  (is (= "base hover active selected"
         (s/class-name (s/c "base" ["hover" ["active" "selected"]]))))
  (is (= "base selected"
         (s/class-name (s/c nil "base" [false "" [nil "selected"]]))))
  (is (identical? (s/c (s/flex) (s/p 8))
                  (s/c (s/flex) (s/p 8))))
  (is (= "p-8 legacy-hook pr-4"
         (s/class-name (s/c (s/p 8) "legacy-hook" (s/pr 4)))))
  (is (= "p-8 pr-4"
         (s/class-name (s/c (s/p 8) (s/pr 4)))))
  (is (= "p-8"
         (s/class-name (s/c (s/pr 4) (s/p 8)))))
  (is (= "top-0 bottom-0"
         (s/class-name (s/c (s/top 0) (s/bottom 0)))))
  (is (= "pt-4 pb-8"
         (s/class-name (s/c (s/pt 4) (s/pb 8)))))
  (is (= "opacity-50 hover:opacity-100"
         (s/class-name (s/c (s/opacity 50)
                            (s/hover (s/opacity 100))))))
  (is (= "hover:opacity-100"
         (s/class-name (s/c (s/hover (s/opacity 50))
                            (s/hover (s/opacity 100))))))
  (testing "cascade layers retain their native priority"
    (is (= "opacity-100 components:opacity-50"
           (s/class-name (s/c (s/opacity 100)
                              (s/components (s/opacity 50))))))
    (is (= "components:opacity-50 opacity-100"
           (s/class-name (s/c (s/components (s/opacity 50))
                              (s/opacity 100))))))
  (is (re-matches #"base-classes-[0-9a-f]{8}" base-classes))
  (is (re-matches #"derived-classes-[0-9a-f]{8}" derived-classes))
  (is (re-matches #"modified-classes-[0-9a-f]{8}" modified-classes))
  (is (re-matches #"derived-modified-classes-[0-9a-f]{8}"
                  derived-modified-classes))
  (is (re-matches #"global-classes-[0-9a-f]{8}" global-classes))
  (is (= 5 (count (set [base-classes
                        derived-classes
                        modified-classes
                        derived-modified-classes
                        global-classes]))))
  (is (= #{:x [:y :z]} (set (keys class-map))))
  (is (re-matches #"class-map-x-[0-9a-f]{8}" (:x class-map)))
  (is (re-matches #"class-map-y-z-[0-9a-f]{8}" (get class-map [:y :z])))
  (is (re-matches #"global-class-map-stable-[0-9a-f]{8}"
                  (:stable global-class-map)))
  (is (= [["base:block"]
          ["components:block"]
          ["variants:block"]]
         predefined-layer-classes))
  (is (= ["components:opacity-50" "components:hover:opacity-100"]
         layered-classes))
  (is (= 2 (count layered-modified-classes)))
  (is (every? #(re-matches #"variants:ref-modified-classes-[0-9a-f]{8}-[01]" %)
              layered-modified-classes))
  (is (re-matches #"layered-class-[0-9a-f]{8}" layered-class))
  (is (re-matches #"layered-composed-class-[0-9a-f]{8}"
                  layered-composed-class)))

(deftest display-and-flex-utility-test
  (is (= ["inline" "block" "inline-block" "flex" "inline-flex"
          "grid" "inline-grid" "contents" "hidden"]
         [(s/inline) (s/block) (s/inline-block) (s/flex) (s/inline-flex)
          (s/grid) (s/inline-grid) (s/contents) (s/hidden)]))
  (is (= ["flex-row" "flex-row-reverse" "flex-col" "flex-col-reverse"]
         [(s/flex-row) (s/flex-row-reverse)
          (s/flex-col) (s/flex-col-reverse)]))
  (is (= ["align-baseline" "align-top" "align-middle" "align-bottom"
          "align-text-top" "align-text-bottom" "align-sub" "align-super"]
         [(s/align-baseline) (s/align-top) (s/align-middle) (s/align-bottom)
          (s/align-text-top) (s/align-text-bottom) (s/align-sub) (s/align-super)])))

(deftest common-layout-utility-test
  (testing "display, sizing, and visibility utilities"
    (is (= ["flow-root" "list-item" "box-border" "box-content"
            "container-normal" "container-size" "container-inline-size"
            "container-scroll-state" "container-anchored"
            "visible" "invisible" "collapse" "border-collapse" "border-separate"]
           [(s/flow-root) (s/list-item) (s/box-border) (s/box-content)
            (s/container-normal) (s/container-size) (s/container-inline-size)
            (s/container-scroll-state) (s/container-anchored)
            (s/visible) (s/invisible) (s/collapse)
            (s/border-collapse) (s/border-separate)])))

  (testing "flex composition utilities"
    (is (= ["flex-wrap" "flex-wrap-reverse" "flex-nowrap"
            "flex-1" "flex-auto" "flex-initial" "flex-none"
            "basis-80" "basis-auto" "basis-full"
            "order--2" "order-first" "order-last" "order-none"]
           [(s/flex-wrap) (s/flex-wrap-reverse) (s/flex-nowrap)
            (s/flex-1) (s/flex-auto) (s/flex-initial) (s/flex-none)
            (s/basis 80) (s/basis-auto) (s/basis-full)
            (s/order -2) (s/order-first) (s/order-last) (s/order-none)])))

  (testing "object fit utilities"
    (is (= ["object-contain" "object-cover" "object-fill"
            "object-none" "object-scale-down"]
           [(s/object-contain) (s/object-cover) (s/object-fill)
            (s/object-none) (s/object-scale-down)]))))

(deftest grid-utility-test
  (is (re-matches #"grid-rows-value-[0-9a-f]{8}" (s/grid-rows "none")))
  (is (re-matches #"grid-auto-cols-value-[0-9a-f]{8}"
                  (s/grid-auto-cols "max-content")))
  (is (re-matches #"grid-auto-rows-value-[0-9a-f]{8}"
                  (s/grid-auto-rows "min-content")))
  (is (= "grid-rows--track-template" (s/grid-rows :track-template)))
  (is (= "grid-col-start-2" (s/grid-col-start 2)))
  (is (= "grid-col-end--1" (s/grid-col-end -1)))
  (is (= "grid-row-start-1" (s/grid-row-start 1)))
  (is (= "grid-row-end-3" (s/grid-row-end 3)))
  (doseq [class-name [(s/grid-cols "1fr 1fr")
                      (s/grid-col "1 / -1")
                      (s/grid-row "2 / 4")
                      (s/grid-area "1 / 2 / 3 / 4")]]
    (is (re-matches #"grid-[a-z-]+-value-[0-9a-f]{8}" class-name))))

(deftest flex-factor-and-alignment-utility-test
  (testing "flex grow and shrink utilities"
    (is (= ["grow" "grow-2" "shrink" "shrink-2" "grow--factor"]
           [(s/grow) (s/grow 2) (s/shrink) (s/shrink 2) (s/grow :factor)])))

  (testing "align-self utilities"
    (is (= ["self-auto" "self-start" "self-end" "self-end-safe"
            "self-center" "self-center-safe" "self-stretch"
            "self-baseline" "self-baseline-last"]
           [(s/self-auto) (s/self-start) (s/self-end) (s/self-end-safe)
            (s/self-center) (s/self-center-safe) (s/self-stretch)
            (s/self-baseline) (s/self-baseline-last)])))

  (testing "align-items utilities"
    (is (= ["items--custom-items" "items-inherit" "items-start" "items-end" "items-end-safe" "items-center"
            "items-center-safe" "items-baseline" "items-baseline-last"
            "items-stretch"]
           [(s/items :custom-items) (s/items-inherit) (s/items-start) (s/items-end) (s/items-end-safe) (s/items-center)
            (s/items-center-safe) (s/items-baseline) (s/items-baseline-last)
            (s/items-stretch)])))

  (testing "justify-content utilities"
    (is (= ["justify-start" "justify-end" "justify-end-safe" "justify-center"
            "justify-center-safe" "justify-between" "justify-around"
            "justify-evenly" "justify-stretch" "justify-baseline" "justify-normal"]
           [(s/justify-start) (s/justify-end) (s/justify-end-safe)
            (s/justify-center) (s/justify-center-safe) (s/justify-between)
            (s/justify-around) (s/justify-evenly) (s/justify-stretch)
            (s/justify-baseline) (s/justify-normal)])))

  (testing "justify-items utilities"
    (is (= ["justify-items-start" "justify-items-end" "justify-items-end-safe"
            "justify-items-center" "justify-items-center-safe"
            "justify-items-stretch" "justify-items-normal"]
           [(s/justify-items-start) (s/justify-items-end)
            (s/justify-items-end-safe) (s/justify-items-center)
            (s/justify-items-center-safe) (s/justify-items-stretch)
            (s/justify-items-normal)])))

  (testing "justify-self utilities"
    (is (= ["justify-self-auto" "justify-self-start" "justify-self-center"
            "justify-self-center-safe" "justify-self-end" "justify-self-end-safe"
            "justify-self-stretch"]
           [(s/justify-self-auto) (s/justify-self-start) (s/justify-self-center)
            (s/justify-self-center-safe) (s/justify-self-end)
            (s/justify-self-end-safe) (s/justify-self-stretch)])))

  (testing "align-content utilities"
    (is (= ["content-normal" "content-center" "content-start" "content-end"
            "content-between" "content-around" "content-evenly"
            "content-baseline" "content-stretch"]
           [(s/content-normal) (s/content-center) (s/content-start)
            (s/content-end) (s/content-between) (s/content-around)
            (s/content-evenly) (s/content-baseline) (s/content-stretch)])))

  (testing "utilities compose with modifiers"
    (is (= ["hover:grow-2" "hover:items-center" "hover:justify-between"]
           (s/hover (s/grow 2) (s/items-center) (s/justify-between))))))

(deftest generated-content-utility-test
  (is (= "content-none" (s/content-none)))
  (is (= "content--generated-content" (s/content :generated-content)))
  (is (re-matches #"content-value-[0-9a-f]{8}"
                  (s/content "")))
  (is (re-matches #"before:content-value-[0-9a-f]{8}"
                  (first (s/before (s/content ""))))))

(deftest gap-utility-test
  (testing "numeric gap values have readable classes"
    (is (= ["gap-4" "gap-x-4" "gap-y-4"]
           [(s/gap 4) (s/gap-x 4) (s/gap-y 4)])))

  (testing "literal and token CSS gap values"
    (is (re-matches #"gap-value-[0-9a-f]{8}" (s/gap "1rem")))
    (is (re-matches #"gap-x-value-[0-9a-f]{8}"
                    (s/gap-x "calc(1rem + 2px)")))
    (is (= "gap--space" (s/gap :space)))))

(deftest dimension-utility-test
  (testing "dynamic dimensions"
    (is (= "min-h-4" (s/min-h 4)))
    (is (re-matches #"min-w-value-[0-9a-f]{8}" (s/min-w "25vw")))
    (is (= "max-h-4" (s/max-h 4)))
    (is (re-matches #"max-w-value-[0-9a-f]{8}" (s/max-w "calc(100% - 4px)")))
    (is (= "w-4" (s/w 4)))
    (is (= "h-4" (s/h 4)))
    (is (= "size-4--width size-4--height" (s/size 4)))
    (is (= "w--panel-width" (s/w :panel-width)))
    (is (re-matches #"aspect-ratio-value-[0-9a-f]{8}"
                    (s/aspect-ratio "16 / 9"))))

  (testing "minimum dimension presets"
    (is (= ["min-h-auto" "min-h-full" "min-h-min" "min-h-max" "min-h-fit"]
           [(s/min-h-auto) (s/min-h-full) (s/min-h-min) (s/min-h-max) (s/min-h-fit)]))
    (is (= ["min-w-auto" "min-w-full" "min-w-min" "min-w-max" "min-w-fit"]
           [(s/min-w-auto) (s/min-w-full) (s/min-w-min) (s/min-w-max) (s/min-w-fit)])))

  (testing "maximum dimension presets"
    (is (= ["max-h-auto" "max-h-full" "max-h-min" "max-h-max" "max-h-fit" "max-h-none"]
           [(s/max-h-auto) (s/max-h-full) (s/max-h-min)
            (s/max-h-max) (s/max-h-fit) (s/max-h-none)]))
    (is (= ["max-w-auto" "max-w-full" "max-w-min" "max-w-max" "max-w-fit" "max-w-none"]
           [(s/max-w-auto) (s/max-w-full) (s/max-w-min)
            (s/max-w-max) (s/max-w-fit) (s/max-w-none)])))

  (testing "width, height, and size presets"
    (is (= ["w-auto" "w-full" "w-min" "w-max" "w-fit"]
           [(s/w-auto) (s/w-full) (s/w-min) (s/w-max) (s/w-fit)]))
    (is (= ["h-auto" "h-full" "h-min" "h-max" "h-fit"]
           [(s/h-auto) (s/h-full) (s/h-min) (s/h-max) (s/h-fit)]))
    (is (= ["size-auto--width size-auto--height"
            "size-full--width size-full--height"
            "size-min--width size-min--height"
            "size-max--width size-max--height"
            "size-fit--width size-fit--height"]
           [(s/size-auto) (s/size-full) (s/size-min) (s/size-max) (s/size-fit)]))))

(deftest inset-and-layer-utility-test
  (testing "numeric and automatic insets"
    (is (= ["inset-0" "inset-x--4" "inset-y-8" "top-1" "right-2"
            "bottom-3" "left-4" "inset-auto" "inset-x-auto" "inset-y-auto"
            "top-auto" "right-auto" "bottom-auto" "left-auto"]
           [(s/inset 0) (s/inset-x -4) (s/inset-y 8) (s/top 1)
            (s/right 2) (s/bottom 3) (s/left 4) (s/inset-auto)
            (s/inset-x-auto) (s/inset-y-auto) (s/top-auto) (s/right-auto)
            (s/bottom-auto) (s/left-auto)])))

  (testing "z-index utilities"
    (is (= ["z--1" "z-10" "z--popover-layer" "z-auto"]
           [(s/z -1) (s/z 10) (s/z :popover-layer) (s/z-auto)]))))

(deftest scroll-margin-utility-test
  (is (= ["scroll-m-100" "scroll-mx-100" "scroll-my-100" "scroll-mt-100"
          "scroll-mr-100" "scroll-mb-100" "scroll-ml-100"]
         [(s/scroll-m 100) (s/scroll-mx 100) (s/scroll-my 100)
          (s/scroll-mt 100) (s/scroll-mr 100) (s/scroll-mb 100)
          (s/scroll-ml 100)])))

(deftest margin-utility-test
  (testing "numeric margins have readable classes"
    (is (= ["m--4" "mx-4" "my-4" "mt-4" "mb-4" "mr-4" "ml-4"
            "ms-4" "me-4"]
           [(s/m -4) (s/mx 4) (s/my 4) (s/mt 4)
            (s/mb 4) (s/mr 4) (s/ml 4) (s/ms 4) (s/me 4)])))

  (testing "literal CSS margins have hashed classes"
    (is (re-matches #"m-value-[0-9a-f]{8}" (s/m "1rem"))))

  (testing "automatic margins"
    (is (= ["m-auto" "mx-auto" "my-auto" "mt-auto" "mb-auto" "mr-auto" "ml-auto"]
           [(s/m-auto) (s/mx-auto) (s/my-auto) (s/mt-auto)
            (s/mb-auto) (s/mr-auto) (s/ml-auto)]))))

(deftest padding-utility-test
  (testing "numeric padding values have readable classes"
    (is (= ["p-4" "px-4" "py-4" "pt-4" "pb-4" "pr-4" "pl-4"
            "ps-4" "pe-4"]
           [(s/p 4) (s/px 4) (s/py 4) (s/pt 4)
            (s/pb 4) (s/pr 4) (s/pl 4) (s/ps 4) (s/pe 4)])))

  (testing "literal CSS padding values have hashed classes"
    (is (re-matches #"p-value-[0-9a-f]{8}" (s/p "1rem")))
    (is (re-matches #"px-value-[0-9a-f]{8}"
                    (s/px "calc(1rem + 2px)"))))

  (testing "padding utilities compose with modifiers"
    (is (= ["hover:px-8"]
           (s/hover (s/px 8))))))

(deftest scroll-padding-utility-test
  (is (= ["scroll-p-20" "scroll-px-20" "scroll-py-20" "scroll-pt-20"
          "scroll-pr-20" "scroll-pb-20" "scroll-pl-20"]
         [(s/scroll-p 20) (s/scroll-px 20) (s/scroll-py 20)
          (s/scroll-pt 20) (s/scroll-pr 20) (s/scroll-pb 20)
          (s/scroll-pl 20)])))

(deftest scrollbar-utility-test
  (doseq [classes [(s/scrollbar-thumb-inherit) (s/scrollbar-thumb-current)
                   (s/scrollbar-thumb-transparent) (s/scrollbar-thumb tokens/fg-base)
                   (s/scrollbar-track-inherit) (s/scrollbar-track-current)
                   (s/scrollbar-track-transparent) (s/scrollbar-track tokens/fg-base)]]
    (is (= 2 (count (str/split classes #" ")))))
  (is (= ["scrollbar-auto" "scrollbar-thin" "scrollbar-none"
          "scrollbar-gutter-auto" "scrollbar-gutter-stable"
          "scrollbar-gutter-both"]
         [(s/scrollbar-auto) (s/scrollbar-thin) (s/scrollbar-none)
          (s/scrollbar-gutter-auto) (s/scrollbar-gutter-stable)
          (s/scrollbar-gutter-both)]))
  (is (re-matches #"scrollbar-thumb-color-[0-9a-f]{8}----us-scrollbar-thumb scrollbar-thumb-color-[0-9a-f]{8}--scrollbar-color"
                  (s/scrollbar-thumb "rebeccapurple")))
  (is (re-matches #"scrollbar-track-color-[0-9a-f]{8}----us-scrollbar-track scrollbar-track-color-[0-9a-f]{8}--scrollbar-color"
                  (s/scrollbar-track "rebeccapurple"))))

(deftest overflow-and-position-utility-test
  (is (= ["overflow-auto" "overflow-hidden" "overflow-clip"
          "overflow-visible" "overflow-scroll"]
         [(s/overflow-auto) (s/overflow-hidden) (s/overflow-clip)
          (s/overflow-visible) (s/overflow-scroll)]))
  (is (= ["overflow-x-auto" "overflow-x-hidden" "overflow-x-clip"
          "overflow-x-visible" "overflow-x-scroll"]
         [(s/overflow-x-auto) (s/overflow-x-hidden) (s/overflow-x-clip)
          (s/overflow-x-visible) (s/overflow-x-scroll)]))
  (is (= ["overflow-y-auto" "overflow-y-hidden" "overflow-y-clip"
          "overflow-y-visible" "overflow-y-scroll"]
         [(s/overflow-y-auto) (s/overflow-y-hidden) (s/overflow-y-clip)
          (s/overflow-y-visible) (s/overflow-y-scroll)]))
  (is (= ["overscroll-auto" "overscroll-contain" "overscroll-none"
          "overscroll-x-auto" "overscroll-x-contain" "overscroll-x-none"
          "overscroll-y-auto" "overscroll-y-contain" "overscroll-y-none"]
         [(s/overscroll-auto) (s/overscroll-contain) (s/overscroll-none)
          (s/overscroll-x-auto) (s/overscroll-x-contain) (s/overscroll-x-none)
          (s/overscroll-y-auto) (s/overscroll-y-contain) (s/overscroll-y-none)]))
  (is (= ["snap-none" "snap-x" "snap-y" "snap-both" "snap-mandatory"
          "snap-proximity" "snap-start" "snap-end" "snap-center" "snap-align-none"]
         [(s/snap-none) (s/snap-x) (s/snap-y) (s/snap-both) (s/snap-mandatory)
          (s/snap-proximity) (s/snap-start) (s/snap-end) (s/snap-center)
          (s/snap-align-none)]))
  (is (re-matches #"clip-path-[0-9a-f]{8}"
                  (s/clip-path "inset(50%)")))
  (is (= "clip-path--clip" (s/clip-path :clip)))
  (is (= ["static" "fixed" "absolute" "relative" "sticky"]
         [(s/static) (s/fixed) (s/absolute) (s/relative) (s/sticky)])))

(deftest user-select-utility-test
  (is (= "select-none---webkit-user-select select-none--user-select" (s/select-none)))
  (is (= "select-text---webkit-user-select select-text--user-select" (s/select-text)))
  (is (= "select-all---webkit-user-select select-all--user-select" (s/select-all)))
  (is (= "select-auto---webkit-user-select select-auto--user-select" (s/select-auto))))

(deftest background-utility-test
  (is (= "bg--us-bg-base-2a5b0631" (s/bg tokens/bg-base)))
  (is (re-matches #"bg-color-[0-9a-f]{8}"
                  (s/bg "var(--us-bg-base-2a5b0631)")))
  (is (re-matches #"bg-color-[0-9a-f]{8}" (s/bg "red")))
  (is (= "bg--us-bg-base-2a5b0631/10" (s/bg (s/alpha tokens/bg-base 10))))
  (is (re-matches #"bg-color-[0-9a-f]{8}/25"
                  (s/bg (s/alpha "oklch(50% 0.2 10)" 25))))
  (is (= "bg--us-bg-base-2a5b0631--app-bg-fallback"
         (s/bg tokens/bg-base :app-bg-fallback)))
  (is (re-matches #"bg--us-bg-base-2a5b0631-color-[0-9a-f]{8}"
                  (s/bg tokens/bg-base "#fff")))
  (is (= "bg-transparent" (s/bg-transparent)))
  (is (= "bg-current" (s/bg-current)))
  (is (= "bg-inherit" (s/bg-inherit)))
  (is (re-matches #"bg-image-[0-9a-f]{8}"
                  (s/bg-image "linear-gradient(red, blue)")))
  (is (= (s/bg-image "linear-gradient(var(--us-bg-base-2a5b0631), transparent)")
         (s/bg-image "linear-gradient(" tokens/bg-base ", transparent)")))
  (is (re-matches #"bg-image-[0-9a-f]{8}"
                  (s/bg-image "url(/image.png)")))
  (is (= "bg-image--hero-image" (s/bg-image :hero-image)))
  (is (= "bg-none" (s/bg-none)))
  (is (= ["bg-clip-border" "bg-clip-padding" "bg-clip-content" "bg-clip-text"]
         [(s/bg-clip-border) (s/bg-clip-padding) (s/bg-clip-content) (s/bg-clip-text)]))
  (is (= ["bg-repeat" "bg-repeat-x" "bg-repeat-y" "bg-repeat-space"
          "bg-repeat-round" "bg-no-repeat"]
         [(s/bg-repeat) (s/bg-repeat-x) (s/bg-repeat-y) (s/bg-repeat-space)
          (s/bg-repeat-round) (s/bg-no-repeat)]))
  (is (= ["bg-auto" "bg-cover" "bg-contain"]
         [(s/bg-auto) (s/bg-cover) (s/bg-contain)]))
  (is (= "bg-size--background-size" (s/bg-size :background-size)))
  (is (re-matches #"bg-size-[0-9a-f]{8}" (s/bg-size "1px 2px")))
  (is (= ["bg-top-left" "bg-top" "bg-top-right" "bg-left" "bg-center"
          "bg-right" "bg-bottom-left" "bg-bottom" "bg-bottom-right"]
         [(s/bg-top-left) (s/bg-top) (s/bg-top-right) (s/bg-left) (s/bg-center)
          (s/bg-right) (s/bg-bottom-left) (s/bg-bottom) (s/bg-bottom-right)]))
  (is (= "bg-position--background-position" (s/bg-position :background-position)))
  (is (re-matches #"bg-position-[0-9a-f]{8}" (s/bg-position "0 100%"))))

(deftest background-blend-mode-utility-test
  (is (= ["bg-blend-normal" "bg-blend-multiply" "bg-blend-screen"
          "bg-blend-overlay" "bg-blend-darken" "bg-blend-lighten"
          "bg-blend-color-dodge" "bg-blend-color-burn" "bg-blend-hard-light"
          "bg-blend-soft-light" "bg-blend-difference" "bg-blend-exclusion"
          "bg-blend-hue" "bg-blend-saturation" "bg-blend-color" "bg-blend-luminosity"]
         [(s/bg-blend-normal) (s/bg-blend-multiply) (s/bg-blend-screen)
          (s/bg-blend-overlay) (s/bg-blend-darken) (s/bg-blend-lighten)
          (s/bg-blend-color-dodge) (s/bg-blend-color-burn) (s/bg-blend-hard-light)
          (s/bg-blend-soft-light) (s/bg-blend-difference) (s/bg-blend-exclusion)
          (s/bg-blend-hue) (s/bg-blend-saturation) (s/bg-blend-color)
          (s/bg-blend-luminosity)])))

(deftest mix-blend-mode-utility-test
  (is (= ["mix-blend-normal" "mix-blend-multiply" "mix-blend-screen"
          "mix-blend-overlay" "mix-blend-darken" "mix-blend-lighten"
          "mix-blend-color-dodge" "mix-blend-color-burn" "mix-blend-hard-light"
          "mix-blend-soft-light" "mix-blend-difference" "mix-blend-exclusion"
          "mix-blend-hue" "mix-blend-saturation" "mix-blend-color"
          "mix-blend-luminosity" "mix-blend-plus-darker" "mix-blend-plus-lighter"]
         [(s/mix-blend-normal) (s/mix-blend-multiply) (s/mix-blend-screen)
          (s/mix-blend-overlay) (s/mix-blend-darken) (s/mix-blend-lighten)
          (s/mix-blend-color-dodge) (s/mix-blend-color-burn) (s/mix-blend-hard-light)
          (s/mix-blend-soft-light) (s/mix-blend-difference) (s/mix-blend-exclusion)
          (s/mix-blend-hue) (s/mix-blend-saturation) (s/mix-blend-color)
          (s/mix-blend-luminosity) (s/mix-blend-plus-darker)
          (s/mix-blend-plus-lighter)])))

(deftest custom-property-and-reset-utility-test
  (let [hover-color "oklch(55% 0.2 20)"
        {:keys [class style]} (runtime-custom-property-props hover-color)]
    (is (re-matches #"hover:bg-color-[0-9a-f]{8}" class))
    (is (= {"--invite-remove-hover-bg" hover-color} style)))
  (let [hover-color "oklch(55% 0.2 20)"
        {:keys [class style]} (var-def-props hover-color)]
    (is (= "hover:bg--invite-remove-hover-bg" class))
    (is (= {"--invite-remove-hover-bg" hover-color} style)))
  (is (= "--us-fg-base-ad253ba1" (s/var-def tokens/fg-base)))
  (is (= "var-shortcut-color-us-fg-base-ad253ba1"
         (s/var :shortcut-color tokens/fg-base)))
  (is (re-matches #"var-shortcut-color-value-[0-9a-f]{8}"
                  (s/var "--shortcut-color" "currentColor")))
  (is (= "shadow-none shadow-layers font-inherit"
         (s/class-name (s/c (s/shadow-none) (s/font-inherit))))))

(deftest shadow-and-color-utility-test
  (is (= "backdrop-blur-none---webkit-backdrop-filter backdrop-blur-none--backdrop-filter"
         (s/backdrop-blur-none)))
  (is (= "backdrop-blur--test-blur---webkit-backdrop-filter backdrop-blur--test-blur--backdrop-filter"
         (s/backdrop-blur :test-blur)))
  (is (re-matches #"backdrop-blur-value-[0-9a-f]{8}---webkit-backdrop-filter backdrop-blur-value-[0-9a-f]{8}--backdrop-filter"
                  (s/backdrop-blur "4px")))
  (is (= "filter--us-shadow-filter-subtler-53b5b2d9"
         (s/filter tokens/shadow-filter-subtler)))
  (is (= "shadow--us-shadow-subtle-0635c9ce shadow-layers"
         (s/shadow tokens/shadow-subtle)))
  (is (= "shadow--us-shadow-subtle-0635c9ce shadow-layers"
         (s/shadow "var(--us-shadow-subtle-0635c9ce)")))
  (is (= "inset 0 0 0 2px var(--us-focus-ring-725b4ca7)"
         (s/str "inset 0 0 0 2px " tokens/focus-ring)))
  (is (= "translate(0, 1.5)"
         (s/str "translate(" 0 ", " 1.5 ")")))
  (is (= (s/shadow "inset 0 0 0 2px var(--us-focus-ring-725b4ca7)")
         (s/shadow "inset 0 0 0 2px " tokens/focus-ring)))
  (is (re-matches #"shadow-[0-9a-f]{8} shadow-layers"
                  (s/shadow "2px 2px 4px rgba(0, 0, 0, 0.12)")))
  (is (= "inset-shadow--us-shadow-subtle-0635c9ce shadow-layers"
         (s/inset-shadow tokens/shadow-subtle)))
  (is (re-matches #"ring-1-color-[0-9a-f]{8} shadow-layers" (s/ring "red")))
  (is (= "ring-2--us-focus-ring-725b4ca7 shadow-layers"
         (s/ring 2 tokens/focus-ring)))
  (is (= "inset-ring-2--us-focus-ring-725b4ca7 shadow-layers"
         (s/inset-ring 2 tokens/focus-ring)))
  (is (= ["inset-shadow-none shadow-layers" "ring-none shadow-layers"
          "inset-ring-none shadow-layers"]
         [(s/inset-shadow-none) (s/ring-none) (s/inset-ring-none)]))
  (is (= "ring-2--us-focus-ring-725b4ca7 shadow-layers inset-ring-1--us-border-base-0425fdad"
         (s/class-name (s/c (s/ring 2 tokens/focus-ring) (s/inset-ring tokens/border-base)))))
  (is (= "color--us-fg-base-ad253ba1" (s/color tokens/fg-base)))
  (is (re-matches #"color-color-[0-9a-f]{8}"
                  (s/color "var(--us-fg-base-ad253ba1)")))
  (is (re-matches #"color-color-[0-9a-f]{8}" (s/color "red")))
  (is (= "color--us-fg-base-ad253ba1/50" (s/color (s/alpha tokens/fg-base 50))))
  (is (= "color--us-fg-base-ad253ba1--app-fg-fallback"
         (s/color tokens/fg-base :app-fg-fallback)))
  (is (re-matches #"color--us-fg-base-ad253ba1-color-[0-9a-f]{8}"
                  (s/color tokens/fg-base "#fff")))
  (is (= "color-transparent" (s/color-transparent)))
  (is (= "color-current" (s/color-current)))
  (is (= "color-inherit" (s/color-inherit)))
  (is (= ["caret-inherit" "caret-current" "caret-transparent" "caret--us-fg-base-ad253ba1"]
         [(s/caret-inherit) (s/caret-current) (s/caret-transparent)
          (s/caret tokens/fg-base)]))
  (is (re-matches #"caret-color-[0-9a-f]{8}" (s/caret "currentColor"))))

(deftest svg-color-utility-test
  (is (= "fill--us-bg-base-2a5b0631" (s/fill tokens/bg-base)))
  (is (= "stroke--us-border-base-0425fdad" (s/stroke tokens/border-base)))
  (is (= "fill--us-bg-base-2a5b0631/50" (s/fill (s/alpha tokens/bg-base 50))))
  (is (re-matches #"stroke-color-[0-9a-f]{8}" (s/stroke "none"))))

(deftest typography-utility-test
  (testing "text alignment and transform utilities"
    (is (= ["text-left" "text-center" "text-right" "text-justify"
            "text-start" "text-end"]
           [(s/text-left) (s/text-center) (s/text-right) (s/text-justify)
            (s/text-start) (s/text-end)]))
    (is (= "uppercase" (s/uppercase))))

  (testing "text transforms, overflow, and wrapping"
    (is (= ["uppercase" "lowercase" "capitalize" "normal-case"
            "truncate--overflow truncate--text-overflow truncate--white-space"
            "text-ellipsis" "text-clip"
            "whitespace-normal" "whitespace-nowrap" "whitespace-pre"
            "whitespace-pre-line" "whitespace-pre-wrap" "whitespace-break-spaces"
            "text-wrap" "text-nowrap" "text-balance" "text-pretty"
            "word-break-normal" "word-break-word" "word-break-all" "word-break-keep"
            "overflow-wrap-normal" "overflow-wrap-break-word" "overflow-wrap-anywhere"]
           [(s/uppercase) (s/lowercase) (s/capitalize) (s/normal-case)
            (s/truncate) (s/text-ellipsis) (s/text-clip)
            (s/whitespace-normal) (s/whitespace-nowrap) (s/whitespace-pre)
            (s/whitespace-pre-line) (s/whitespace-pre-wrap)
            (s/whitespace-break-spaces)
            (s/text-wrap) (s/text-nowrap) (s/text-balance) (s/text-pretty)
            (s/word-break-normal) (s/word-break-word)
            (s/word-break-all) (s/word-break-keep)
            (s/overflow-wrap-normal) (s/overflow-wrap-break-word)
            (s/overflow-wrap-anywhere)])))

  (testing "list style utilities"
    (is (= ["list-inside" "list-outside" "list-none" "list-disc" "list-decimal"]
           [(s/list-inside) (s/list-outside) (s/list-none)
            (s/list-disc) (s/list-decimal)])))

  (testing "font size and line-height utilities"
    (is (= "font-size-16" (s/font-size 16)))
    (is (= "font-size-inherit" (s/font-size-inherit)))
    (is (re-matches #"font-size-value-[0-9a-f]{8}" (s/font-size "1rem")))
    (is (= "font-size--body-size" (s/font-size :body-size)))
    (is (= "text-16-line-height-1_5--font-size text-16-line-height-1_5--line-height"
           (s/text 16 1.5)))
    (is (= "line-height-none" (s/line-height-none)))
    (is (= "line-height-inherit" (s/line-height-inherit)))
    (is (= "line-height-1_5" (s/line-height 1.5)))
    (is (re-matches #"line-height-value-[0-9a-f]{8}" (s/line-height "1.5rem"))))

  (testing "font family, style, and weight utilities"
    (is (re-matches #"font-value-[0-9a-f]{8}"
                    (s/font "Inter, sans-serif")))
    (is (= "font--interface-font" (s/font :interface-font)))
    (is (= ["italic" "not-italic"] [(s/italic) (s/not-italic)]))
    (is (= "font-weight-inherit" (s/font-weight-inherit)))
    (is (= ["font-thin" "font-extralight" "font-light" "font-normal"
            "font-medium" "font-semibold" "font-bold" "font-extrabold"
            "font-black"]
           [(s/font-thin) (s/font-extralight) (s/font-light) (s/font-normal)
            (s/font-medium) (s/font-semibold) (s/font-bold)
            (s/font-extrabold) (s/font-black)])))

  (testing "letter spacing and text-decoration utilities"
    (is (= "tracking--0_5" (s/tracking -0.5)))
    (is (re-matches #"tracking-value-[0-9a-f]{8}" (s/tracking "0.1em")))
    (is (= ["underline" "overline" "line-through" "no-underline"
            "decoration-inherit" "decoration-current" "decoration-transparent"
            "decoration--us-border-base-0425fdad" "decoration-thickness-1" "underline-offset-3"]
           [(s/underline) (s/overline) (s/line-through) (s/no-underline)
            (s/decoration-inherit) (s/decoration-current) (s/decoration-transparent)
            (s/decoration tokens/border-base) (s/decoration-thickness 1)
            (s/underline-offset 3)]))
    (is (re-matches #"decoration-color-[0-9a-f]{8}"
                    (s/decoration "currentColor")))
    (is (= "decoration--us-border-base-0425fdad/40"
           (s/decoration (s/alpha tokens/border-base 40))))))

(deftest outline-utility-test
  (testing "combined and individual outline properties share atomic classes"
    (is (= "outline--us-focus-ring-725b4ca7 outline-2 outline-style"
           (s/outline 2 tokens/focus-ring)))
    (is (= ["outline-2" "outline--us-focus-ring-725b4ca7" "outline-offset--2"]
           [(s/outline-width 2)
            (s/outline-color tokens/focus-ring)
            (s/outline-offset -2)]))
    (is (= ["outline--fancy-focus-ring-width"
            "outline--fancy-focus-ring-width-3"
            "outline--fancy-focus-ring-width--fallback-width"]
           [(s/outline-width :fancy-focus-ring-width)
            (s/outline-width :fancy-focus-ring-width 3)
            (s/outline-width "var(--fancy-focus-ring-width)" :fallback-width)]))
    (is (= ["outline-offset--fancy-focus-ring-offset"
            "outline-offset--fancy-focus-ring-offset--0_5"
            "outline-offset--fancy-focus-ring-offset--fallback-offset"]
           [(s/outline-offset :fancy-focus-ring-offset)
            (s/outline-offset :fancy-focus-ring-offset -0.5)
            (s/outline-offset "var(--fancy-focus-ring-offset)" :fallback-offset)]))
    (is (= "outline--us-focus-ring-725b4ca7--us-border-base-0425fdad"
           (s/outline-color tokens/focus-ring tokens/border-base)))
    (is (= "outline--us-focus-ring-725b4ca7/60 outline-2 outline-style"
           (s/outline 2 (s/alpha tokens/focus-ring 60))))
    (is (= "outline--us-focus-ring-725b4ca7--us-border-base-0425fdad outline-2 outline-style"
           (s/outline 2 tokens/focus-ring tokens/border-base))))

  (testing "outline style presets"
    (is (= ["outline-solid" "outline-dashed" "outline-dotted"
            "outline-double" "outline-none"
            "outline-hidden--outline outline-hidden--outline-offset"]
           [(s/outline-solid) (s/outline-dashed) (s/outline-dotted)
            (s/outline-double) (s/outline-none) (s/outline-hidden)])))

  (testing "outline utilities compose with modifiers"
    (is (= ["focus-visible:outline--us-focus-ring-725b4ca7 focus-visible:outline-2 focus-visible:outline-style"
            "focus-visible:outline-offset-2"]
           (s/focus-visible
             (s/outline 2 tokens/focus-ring)
             (s/outline-offset 2))))))

(deftest rounded-utility-test
  (testing "whole, corner, and side utilities expand to classes"
    (is (= "rounded-4" (s/rounded 4)))
    (is (re-matches #"rounded-value-[0-9a-f]{8}" (s/rounded "50%")))
    (is (= "rounded--radius" (s/rounded :radius)))
    (is (= "rounded-tl-4" (s/rounded-tl 4)))
    (is (= "rounded-tr-4" (s/rounded-tr 4)))
    (is (= "rounded-br-4" (s/rounded-br 4)))
    (is (= "rounded-bl-4" (s/rounded-bl 4)))
    (is (= "rounded-t-4--border-top-left-radius rounded-t-4--border-top-right-radius"
           (s/rounded-t 4)))
    (is (= "rounded-r-4--border-top-right-radius rounded-r-4--border-bottom-right-radius"
           (s/rounded-r 4)))
    (is (= "rounded-b-4--border-bottom-right-radius rounded-b-4--border-bottom-left-radius"
           (s/rounded-b 4)))
    (is (= "rounded-l-4--border-bottom-left-radius rounded-l-4--border-top-left-radius"
           (s/rounded-l 4))))

  (testing "rounded utilities compose with modifiers"
    (is (= ["hover:not-disabled:rounded-t-4--border-top-left-radius hover:not-disabled:rounded-t-4--border-top-right-radius"]
           (s/hover
             (s/not
               (s/disabled
                 (s/rounded-t 4))))))))

(deftest common-interactivity-and-effects-utility-test
  (testing "opacity utilities"
    (is (= ["opacity-0" "opacity-50" "opacity-100" "opacity--disabled-opacity"]
           [(s/opacity 0) (s/opacity 50) (s/opacity 100)
            (s/opacity :disabled-opacity)])))

  (testing "pointer, will-change, touch-action, and cursor utilities"
    (is (= ["pointer-events-none" "pointer-events-auto"
            "will-change-auto" "will-change-scroll" "will-change-contents"
            "will-change-transform" "will-change--performance-hint"
            "touch-auto" "touch-none" "touch-pan-x" "touch-pan-left" "touch-pan-right"
            "touch-pan-y" "touch-pan-up" "touch-pan-down" "touch-pinch-zoom"
            "touch-manipulation" "cursor-auto" "cursor-default" "cursor-pointer" "cursor-wait"
            "cursor-text" "cursor-move" "cursor-help" "cursor-not-allowed"
            "cursor-none" "cursor-context-menu" "cursor-progress" "cursor-cell"
            "cursor-crosshair" "cursor-vertical-text" "cursor-alias" "cursor-copy"
            "cursor-no-drop" "cursor-grab" "cursor-grabbing" "cursor-all-scroll"
            "cursor-col-resize" "cursor-row-resize" "cursor-n-resize" "cursor-e-resize"
            "cursor-s-resize" "cursor-w-resize" "cursor-ne-resize" "cursor-nw-resize"
            "cursor-se-resize" "cursor-sw-resize" "cursor-ew-resize" "cursor-ns-resize"
            "cursor-nesw-resize" "cursor-nwse-resize" "cursor-zoom-in" "cursor-zoom-out"
            "cursor--custom-cursor"]
           [(s/pointer-events-none) (s/pointer-events-auto)
            (s/will-change-auto) (s/will-change-scroll) (s/will-change-contents)
            (s/will-change-transform) (s/will-change :performance-hint)
            (s/touch-auto) (s/touch-none) (s/touch-pan-x) (s/touch-pan-left)
            (s/touch-pan-right) (s/touch-pan-y) (s/touch-pan-up) (s/touch-pan-down)
            (s/touch-pinch-zoom) (s/touch-manipulation) (s/cursor-auto) (s/cursor-default) (s/cursor-pointer)
            (s/cursor-wait) (s/cursor-text) (s/cursor-move)
            (s/cursor-help) (s/cursor-not-allowed) (s/cursor-none)
            (s/cursor-context-menu) (s/cursor-progress) (s/cursor-cell)
            (s/cursor-crosshair) (s/cursor-vertical-text) (s/cursor-alias)
            (s/cursor-copy) (s/cursor-no-drop) (s/cursor-grab) (s/cursor-grabbing)
            (s/cursor-all-scroll) (s/cursor-col-resize) (s/cursor-row-resize)
            (s/cursor-n-resize) (s/cursor-e-resize) (s/cursor-s-resize)
            (s/cursor-w-resize) (s/cursor-ne-resize) (s/cursor-nw-resize)
            (s/cursor-se-resize) (s/cursor-sw-resize) (s/cursor-ew-resize)
            (s/cursor-ns-resize) (s/cursor-nesw-resize) (s/cursor-nwse-resize)
            (s/cursor-zoom-in) (s/cursor-zoom-out) (s/cursor :custom-cursor)]))
    (is (re-matches #"will-change-value-[0-9a-f]{8}"
                    (s/will-change "opacity, transform")))
    (is (re-matches #"cursor-value-[0-9a-f]{8}"
                    (s/cursor "url('/cursor.svg') 4 4, pointer"))))

  (testing "resize and appearance utilities"
    (is (= ["resize-none" "resize-y" "resize-x" "resize"
            "appearance-none" "appearance-auto"]
           [(s/resize-none) (s/resize-y) (s/resize-x) (s/resize)
            (s/appearance-none) (s/appearance-auto)]))))

(deftest modifier-utility-test
  (testing "a modifier accepts multiple nested utilities"
    (is (= ["hover:border--us-border-base-0425fdad hover:border-1 hover:border-style"
            "hover:border-t--us-border-base-0425fdad hover:border-t-2 hover:border-t-style"
            "hover:bg--us-bg-base-2a5b0631"
            "hover:color--us-fg-base-ad253ba1"]
           (s/hover
             (s/border tokens/border-base)
             (s/border-t 2 tokens/border-base)
             (s/bg tokens/bg-base)
             (s/color tokens/fg-base)))))

  (testing "modifiers compose in nesting order"
    (is (= ["hover:disabled:border--us-border-base-0425fdad hover:disabled:border-1 hover:disabled:border-style"]
           (s/hover
             (s/disabled
               (s/border tokens/border-base))))))

  (testing "equivalent modifier orders resolve in application order"
    (is (= "focus:hover:opacity-100"
           (s/class-name
             (s/c (s/hover (s/focus (s/opacity 50)))
                  (s/focus (s/hover (s/opacity 100)))))))
    (is (= "hover:focus:opacity-50"
           (s/class-name
             (s/c (s/focus (s/hover (s/opacity 100)))
                  (s/hover (s/focus (s/opacity 50))))))))

  (testing "layout utilities compose with modifiers"
    (is (= ["hover:flex" "hover:w-4" "hover:overflow-hidden"]
           (s/hover (s/flex) (s/w 4) (s/overflow-hidden)))))

  (testing "attribute modifiers compose with focus, not, and media"
    (let [[focus-class media-class]
          (s/focus-visible
            (s/not
              (s/attr :disabled
                (s/outline-offset -0.5)
                (s/media "(resolution < 2dppx)"
                  (s/outline-offset -1)))))]
      (is (= "focus-visible:not-attr-disabled:outline-offset--0_5" focus-class))
      (is (re-matches #"focus-visible:not-attr-disabled:media-[0-9a-f]{8}:outline-offset--1"
                      media-class))))

  (testing "attribute-value modifiers use content-safe class names"
    (let [[class-name] (s/attr= :aria-pressed "true"
                         (s/opacity 100))]
      (is (re-matches #"attr-aria-pressed-value-[0-9a-f]{8}:opacity-100"
                      class-name))))

  (testing "other pseudo-class modifiers use the shared machinery"
    (is (= ["target:bg--us-bg-base-2a5b0631"]
           (s/target (s/bg tokens/bg-base))))
    (is (= ["focus:border--us-border-base-0425fdad focus:border-1 focus:border-style"]
           (s/focus (s/border tokens/border-base))))
    (is (= ["focus-within:border--us-border-base-0425fdad focus-within:border-1 focus-within:border-style"]
           (s/focus-within (s/border tokens/border-base))))
    (is (= ["focus-visible:border--us-border-base-0425fdad focus-visible:border-1 focus-visible:border-style"]
           (s/focus-visible (s/border tokens/border-base))))
    (is (= ["placeholder:color--us-fg-base-ad253ba1"]
           (s/placeholder (s/color tokens/fg-base))))
    (is (= ["active:border--us-border-base-0425fdad active:border-1 active:border-style"]
           (s/active (s/border tokens/border-base))))
    (is (= ["read-only:border--us-border-base-0425fdad read-only:border-1 read-only:border-style"]
           (s/read-only (s/border tokens/border-base)))))

  (testing "not-disabled wraps multiple interaction states"
    (is (= ["not-disabled:hover:bg--us-bg-base-2a5b0631"
            "not-disabled:active:bg--us-bg-base-2a5b0631"]
           (s/not-disabled
             (s/hover (s/bg tokens/bg-base))
             (s/active (s/bg tokens/bg-base)))))))

(deftest negated-modifier-test
  (testing "not negates the immediately nested modifier"
    (is (= ["not-disabled:border--us-border-base-0425fdad not-disabled:border-1 not-disabled:border-style"]
           (s/not
             (s/disabled
               (s/border tokens/border-base))))))

  (testing "negated modifiers compose with outer and inner modifiers"
    (is (= ["hover:not-disabled:border--us-border-base-0425fdad hover:not-disabled:border-1 hover:not-disabled:border-style"]
           (s/hover
             (s/not
               (s/disabled
                 (s/border tokens/border-base))))))
    (is (= ["not-hover:disabled:border--us-border-base-0425fdad not-hover:disabled:border-1 not-hover:disabled:border-style"]
           (s/not
             (s/hover
               (s/disabled
                 (s/border tokens/border-base)))))))

  (testing "double negation restores the positive modifier"
    (is (= ["disabled:border--us-border-base-0425fdad disabled:border-1 disabled:border-style"]
           (s/not
             (s/not
               (s/disabled
                 (s/border tokens/border-base))))))))

(deftest border-utility-smoke-test
  (testing "utilities expand to plain class strings"
    (let [classes (s/border tokens/border-base)]
      (is (string? classes))
      (is (= "border--us-border-base-0425fdad border-1 border-style" classes))
      (is (re-matches #"border-color-[0-9a-f]{8} border-1 border-style"
                      (s/border "var(--us-border-base-0425fdad)")))))

  (testing "normal require syntax exposes directional macros and local constants"
    (is (= "border-t--app-local-border border-t-2 border-t-style"
           (s/border-t 2 local-border-token)))
    (is (= "border--us-border-base-0425fdad/40 border-1 border-style"
           (s/border (s/alpha tokens/border-base 40))))
    (is (= "border--us-border-base-0425fdad--us-border-subtle-29754feb border-2 border-style"
           (s/border 2 tokens/border-base tokens/border-subtle)))
    (is (= "border-r--us-border-base-0425fdad border-r-1 border-r-style"
           (s/border-r tokens/border-base)))
    (is (= "border-b--us-border-base-0425fdad border-b-1 border-b-style"
           (s/border-b tokens/border-base)))
    (is (= "border-l--us-border-base-0425fdad border-l-1 border-l-style"
           (s/border-l tokens/border-base))))

  (testing "border color, width, and style utilities"
    (is (= "border--us-border-base-0425fdad" (s/border-color tokens/border-base)))
    (is (= "border--us-border-base-0425fdad--us-border-subtle-29754feb"
           (s/border-color tokens/border-base tokens/border-subtle)))
    (is (re-matches #"border-color-[0-9a-f]{8}"
                    (s/border-color "rgba(255,255,255,0.35)")))
    (is (= "border-width-0" (s/border-width 0)))
    (is (= ["border-solid" "border-dashed" "border-dotted"
            "border-double" "border-none"]
           [(s/border-solid) (s/border-dashed) (s/border-dotted)
            (s/border-double) (s/border-none)])))

  (testing "styled borders optionally include width and color"
    (is (= "border--us-border-base-0425fdad border-2 border-dashed"
           (s/border-dashed 2 tokens/border-base)))
    (is (= "border--us-border-base-0425fdad--us-border-subtle-29754feb border-2 border-double"
           (s/border-double 2 tokens/border-base tokens/border-subtle)))
    (is (= ["border-t--us-border-base-0425fdad border-t-1 border-t-dashed"
            "border-r--us-border-base-0425fdad border-r-1 border-r-dotted"
            "border-b--us-border-base-0425fdad border-b-1 border-b-double"
            "border-l--us-border-base-0425fdad border-l-1 border-l-dashed"]
           [(s/border-t-dashed tokens/border-base)
            (s/border-r-dotted tokens/border-base)
            (s/border-b-double tokens/border-base)
            (s/border-l-dashed tokens/border-base)]))))

(deftest tailwind-modifier-class-names-test
  (is (= "md:p-8" (s/class-name (s/md (s/p 8)))))
  (is (= "xxl:p-8" (s/class-name (s/xxl (s/p 8)))))
  (is (= "aria-checked:opacity-50" (s/class-name (s/aria-checked (s/opacity 50)))))
  (is (= "not-aria-disabled:opacity-50" (s/class-name (s/not (s/aria-disabled (s/opacity 50))))))
  (is (= "selection:opacity-50" (s/class-name (s/selection (s/opacity 50)))))
  (is (= "motion-reduce:transition-none" (s/class-name (s/motion-reduce (s/transition-none)))))
  (is (= "checked:opacity-50" (s/class-name (s/checked (s/opacity 50)))))
  (is (= "space-y:mb-8" (s/class-name (s/space-y 8))))
  (is (= "columns-3" (s/class-name (s/columns 3))))
  (is (= "max-md:p-8" (s/class-name (s/max-md (s/p 8)))))
  (is (= "font-mono" (s/class-name (s/font-mono))))
  (is (= "blur-4" (s/class-name (s/blur 4))))
  (is (= "col-span-2" (s/class-name (s/col-span 2)))))
