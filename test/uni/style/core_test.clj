(ns uni.style.core-test
  (:require [cljs.analyzer :as ana]
            [cljs.env :as cljs.env]
            [clojure.string :as str]
            [clojure.test :refer [are deftest is testing]]
            [uni.style.core]
            [uni.style.impl.animation :as animation]
            [uni.style.impl.border :as border]
            [uni.style.impl.color :as color]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.composition :as composition]
            [uni.style.impl.constant :as constant]
            [uni.style.impl.interaction :as interaction]
            [uni.style.impl.layer :as layer]
            [uni.style.impl.layout :as layout]
            [uni.style.impl.modifier :as modifier]
            [uni.style.impl.position :as position]
            [uni.style.impl.resolution :as resolution]
            [uni.style.impl.scrollbar :as scrollbar]
            [uni.style.impl.serialize :as serialize]
            [uni.style.impl.spacing :as spacing]
            [uni.style.impl.transform :as transform]
            [uni.style.impl.transition :as transition]
            [uni.style.impl.typography :as typography]
            [uni.style.impl.var :as style-var]))

(def test-env
  {:line 10
   :column 4
   :ns {:name 'uni.style.fixture
        :meta {:file "uni/style/fixture.cljs"}}})

(def expected-public-macros
  '#{def-theme def-token def-animation def-base raw str inc dec + - / def-component css def-variants animation-none scale rotate translate translate-x translate-y transition transition-all transition-colors transition-opacity
     transition-shadow transition-transform transition-none transition-normal transition-discrete duration duration-initial
     ease ease-linear ease-in ease-out ease-in-out ease-initial delay
     border border-t border-r border-b border-l border-width border-color border-solid
     border-dashed border-t-dashed border-r-dashed border-b-dashed border-l-dashed
     border-dotted border-t-dotted border-r-dotted border-b-dotted border-l-dotted
     border-double border-t-double border-r-double border-b-double border-l-double
     border-none outline outline-width outline-color outline-offset
     outline-solid outline-dashed outline-dotted outline-double outline-none outline-hidden rounded rounded-tl
     rounded-tr rounded-br rounded-bl rounded-t rounded-r rounded-b rounded-l select-none
     select-text select-all select-auto var var-def bg bg-transparent bg-current bg-inherit bg-unset
     bg-image bg-overlay bg-none bg-clip-border bg-clip-padding bg-clip-content bg-clip-text
     bg-repeat bg-repeat-x bg-repeat-y bg-repeat-space bg-repeat-round bg-no-repeat
     bg-auto bg-cover bg-contain bg-size bg-top-left bg-top bg-top-right bg-left bg-center bg-right
     bg-bottom-left bg-bottom bg-bottom-right bg-position mask-image mask-size mask-position
     bg-blend-normal bg-blend-multiply bg-blend-screen bg-blend-overlay bg-blend-darken bg-blend-lighten
     bg-blend-color-dodge bg-blend-color-burn bg-blend-hard-light bg-blend-soft-light bg-blend-difference
     bg-blend-exclusion bg-blend-hue bg-blend-saturation bg-blend-color bg-blend-luminosity
     mix-blend-normal mix-blend-multiply mix-blend-screen mix-blend-overlay mix-blend-darken mix-blend-lighten
     mix-blend-color-dodge mix-blend-color-burn mix-blend-hard-light mix-blend-soft-light mix-blend-difference
     mix-blend-exclusion mix-blend-hue mix-blend-saturation mix-blend-color mix-blend-luminosity
     mix-blend-plus-darker mix-blend-plus-lighter backdrop-blur-none backdrop-blur filter shadow shadow-none
     inset-shadow inset-shadow-none ring ring-none inset-ring inset-ring-none
     color color-transparent color-current color-inherit caret-inherit caret-current caret-transparent caret fill stroke
     text-left text-center text-right text-justify text-start text-end uppercase lowercase
     capitalize normal-case truncate text-ellipsis text-clip whitespace-normal whitespace-nowrap whitespace-pre
     whitespace-pre-line whitespace-pre-wrap whitespace-break-spaces text-wrap text-nowrap text-balance text-pretty
     word-break-normal word-break-word word-break-all word-break-keep
     overflow-wrap-normal overflow-wrap-break-word overflow-wrap-anywhere list-inside
     list-outside list-none list-disc list-decimal inline block inline-block flow-root
     flex inline-flex grid inline-grid grid-cols grid-rows grid-auto-cols grid-auto-rows
     grid-col grid-col-start grid-col-end grid-row grid-row-start grid-row-end grid-area
     list-item contents hidden box-border box-content container-normal container-size container-inline-size
     container-scroll-state container-anchored visible invisible collapse border-collapse border-separate flex-row flex-row-reverse flex-col flex-col-reverse
     flex-wrap flex-wrap-reverse flex-nowrap flex-1 flex-auto flex-initial flex-none align-baseline
     align-top align-middle align-bottom align-text-top align-text-bottom align-sub align-super grow
     shrink self-auto self-start self-end self-end-safe self-center self-center-safe self-stretch
     self-baseline self-baseline-last items items-inherit items-start items-end items-end-safe items-center items-center-safe items-baseline
     items-baseline-last items-stretch justify-start justify-end justify-end-safe justify-center justify-center-safe justify-between
     justify-around justify-evenly justify-stretch justify-baseline justify-normal justify-items-start justify-items-end justify-items-end-safe
     justify-items-center justify-items-center-safe justify-items-stretch justify-items-normal justify-self-auto justify-self-start justify-self-center justify-self-center-safe
     justify-self-end justify-self-end-safe justify-self-stretch content-normal content-center content-start content-end content-between
     content-around content-evenly content-baseline content-stretch content content-none order order-first order-last order-none
     opacity alpha font-size text font-size-inherit line-height-none line-height-inherit line-height font font-inherit font-weight-inherit italic
     not-italic font-mono font-thin font-extralight font-light font-normal font-medium font-semibold font-bold
     font-extrabold font-black underline overline line-through no-underline decoration-inherit decoration-current decoration-transparent decoration
     decoration-thickness underline-offset tracking
     gap gap-x gap-y basis basis-auto
     basis-full min-h min-h-auto min-h-full min-h-min min-h-max min-h-fit min-w
     min-w-auto min-w-full min-w-min min-w-max min-w-fit max-h max-h-auto max-h-full
     max-h-min max-h-max max-h-fit max-h-none max-w max-w-auto max-w-full max-w-min
     max-w-max max-w-fit max-w-none w w-auto w-full w-min w-max
     w-fit h h-auto h-full h-min h-max h-fit size
     size-auto size-full size-min size-max size-fit aspect-ratio object-contain object-cover object-fill
     object-none object-scale-down m mx my mt mb mr
     ml ms me scroll-m scroll-mx scroll-my scroll-mt scroll-mr scroll-mb scroll-ml
     m-auto mx-auto my-auto mt-auto mb-auto mr-auto ml-auto
     p px py pt pb pr pl ps pe scroll-p scroll-px scroll-py scroll-pt scroll-pr scroll-pb scroll-pl clip-path overflow-auto
     overflow-hidden overflow-clip overflow-visible overflow-scroll overflow-x-auto overflow-x-hidden overflow-x-clip overflow-x-visible
     overflow-x-scroll overflow-y-auto overflow-y-hidden overflow-y-clip overflow-y-visible overflow-y-scroll
     overscroll-auto overscroll-contain overscroll-none overscroll-x-auto overscroll-x-contain overscroll-x-none
     overscroll-y-auto overscroll-y-contain overscroll-y-none snap-none snap-x snap-y snap-both snap-mandatory
     snap-proximity snap-start snap-end snap-center snap-align-none
     scrollbar-thumb-inherit scrollbar-thumb-current scrollbar-thumb-transparent scrollbar-thumb
     scrollbar-track-inherit scrollbar-track-current scrollbar-track-transparent scrollbar-track
     scrollbar-auto scrollbar-thin scrollbar-none scrollbar-gutter-auto scrollbar-gutter-stable scrollbar-gutter-both
     static fixed absolute relative sticky inset inset-x inset-y top right
     bottom left inset-auto inset-x-auto inset-y-auto top-auto right-auto bottom-auto
     left-auto z z-auto pointer-events-none pointer-events-auto
     will-change-auto will-change-scroll will-change-contents will-change-transform will-change
     touch-auto touch-none touch-pan-x touch-pan-left touch-pan-right touch-pan-y touch-pan-up touch-pan-down
     touch-pinch-zoom touch-manipulation cursor-auto cursor-default cursor-pointer
     cursor-wait cursor-text cursor-move cursor-help cursor-not-allowed cursor-none cursor-context-menu
     cursor-progress cursor-cell cursor-crosshair cursor-vertical-text cursor-alias cursor-copy cursor-no-drop
     cursor-grab cursor-grabbing cursor-all-scroll cursor-col-resize cursor-row-resize cursor-n-resize cursor-e-resize
     cursor-s-resize cursor-w-resize cursor-ne-resize cursor-nw-resize cursor-se-resize cursor-sw-resize
     cursor-ew-resize cursor-ns-resize cursor-nesw-resize cursor-nwse-resize cursor-zoom-in cursor-zoom-out cursor
     resize-none resize-y resize-x
     resize appearance-none appearance-auto declare-layer-order declare-properties base
     scale-x scale-y scale-none rotate-none translate-none transform transform-none
     border-spacing indent text-shadow origin line-clamp float-right float-left float-start float-end float-none table-auto table-fixed grid-flow-row grid-flow-col grid-flow-dense grid-flow-row-dense grid-flow-col-dense isolate isolation-auto break-inside-auto break-inside-avoid break-inside-avoid-page break-inside-avoid-column scheme-normal scheme-dark scheme-light scheme-light-dark scheme-only-dark scheme-only-light line-clamp-none antialiased subpixel-antialiased decoration-solid decoration-double decoration-dotted decoration-dashed decoration-wavy list-image-none text-shadow-none origin-center origin-top origin-top-right origin-right origin-bottom-right origin-bottom origin-bottom-left origin-left origin-top-left transform-content transform-border transform-fill transform-stroke transform-view backface-hidden backface-visible mask-type-alpha mask-type-luminance components variants
     sel-class ancestor self-or-ancestor root-theme
     ancestor-hover ancestor-focus ancestor-focus-within ancestor-focus-visible ancestor-active ancestor-disabled ancestor-not-disabled
     ancestor-has ancestor-attr ancestor-attr= group group-hover group-focus group-focus-visible group-active group-disabled
     group-not-disabled group-attr group-attr=
     peer peer-hover peer-focus peer-focus-within peer-focus-visible peer-active peer-visited peer-target
     peer-first peer-last peer-only peer-odd peer-even peer-first-of-type peer-last-of-type peer-only-of-type
     peer-nth peer-nth-last peer-nth-of-type peer-nth-last-of-type peer-empty
     peer-disabled peer-not-disabled peer-enabled peer-checked peer-indeterminate peer-default peer-optional
     peer-required peer-valid peer-invalid peer-user-valid peer-user-invalid peer-in-range peer-out-of-range
     peer-placeholder-shown peer-autofill peer-read-only peer-open peer-has peer-attr peer-attr=
     * ** sel has pseudo before after first last only odd even nth nth-last
     nth-of-type nth-last-of-type empty first-of-type last-of-type only-of-type hover focus
     focus-within focus-visible placeholder target attr attr= media container supports active disabled not-disabled read-only not
     border-x border-y space-x space-y divide-x divide-y sr-only not-sr-only columns enabled checked indeterminate default optional required valid invalid user-valid user-invalid in-range out-of-range placeholder-shown autofill visited open selection marker first-letter first-line file backdrop aria-busy aria-checked aria-disabled aria-expanded aria-hidden aria-pressed aria-readonly aria-required aria-selected motion-reduce motion-safe print sm md lg xl xxl
     max-sm max-md max-lg max-xl max-xxl blur col-span row-span})

(deftest test-public-macro-contract
  (is (= expected-public-macros
         (->> (ns-publics 'uni.style.core)
              (keep (fn [[macro-sym macro-var]]
                      (when (:macro (meta macro-var))
                        macro-sym)))
              set))))

(deftest test-utility-macro-docstrings
  (doseq [[macro-sym macro-var] (ns-publics 'uni.style.core)
          :when (:macro (meta macro-var))]
    (is (string? (:doc (meta macro-var)))
        (str macro-sym " must have a docstring"))))

(defn- expand-border [prefix property-prefix args]
  (let [registered (atom [])
        result (with-redefs [compiler/register-atomic!
                             (fn [_env ns-sym class-name declaration modifiers]
                               (swap! registered conj
                                      (cond-> [ns-sym class-name declaration]
                                              (seq modifiers) (conj modifiers))))]
                 (border/border* test-env prefix property-prefix args))]
    {:result result :registered @registered}))

(defn- one-or-many [values]
  (if (= 1 (count values)) (first values) values))

(defn- expand-rounded [prefix properties radius-form]
  (let [registered (atom [])
        result (with-redefs [compiler/register-atomic!
                             (fn [_env ns-sym class-name declaration modifiers]
                               (swap! registered conj
                                      [ns-sym class-name declaration modifiers]))]
                 (border/rounded* test-env prefix properties radius-form))]
    {:result result :registered (one-or-many @registered)}))

(defn- expand-utility [f]
  (let [registered (atom [])
        result (with-redefs [compiler/register-atomic!
                             (fn [_env ns-sym class-name declaration modifiers]
                               (swap! registered conj
                                      [ns-sym class-name declaration modifiers]))]
                 (f))]
    {:result result :registered (one-or-many @registered)}))

(def ^:private shadow-layers-registration
  ['uni.style.fixture "shadow-layers"
   {:box-shadow (str "var(--us-inset-shadow, 0 0 #0000), var(--us-inset-ring-shadow, 0 0 #0000), "
                     "var(--us-ring-shadow, 0 0 #0000), var(--us-shadow, 0 0 #0000)")}
   []])

(defn- shadow-layer-expansion [class-name property value]
  {:result (str class-name " shadow-layers")
   :registered [['uni.style.fixture class-name {property value} []]
                shadow-layers-registration]})

(deftest test-property-resolution
  (testing "broad declarations block earlier narrow declarations"
    (is (= [["[]|padding-block" nil]
            ["[]|padding-block-end" nil]
            ["[]|padding-block-start" nil]
            ["[]|padding-bottom" nil]
            ["[]|padding-inline" nil]
            ["[]|padding-inline-end" nil]
            ["[]|padding-inline-start" nil]
            ["[]|padding-left" nil]
            ["[]|padding-right" nil]
            ["[]|padding-top" nil]
            ["[]|padding" "p-8"]]
           (resolution/descriptor-operation [] :padding "p-8"))))

  (testing "logical shorthands conservatively block overlapping physical sides"
    (let [writes (resolution/descriptor-operation [] :padding-inline "px-8")]
      (is (some #{["[]|padding-left" nil]} writes))
      (is (some #{["[]|padding-right" nil]} writes))))

  (testing "independent physical sides do not conflict"
    (is (not (contains? (resolution/conflicts :top) :bottom)))
    (is (not (contains? (resolution/conflicts :padding-top) :padding-bottom)))
    (is (contains? (resolution/conflicts :top) :inset-block-start)))

  (testing "broad properties sort before retained narrow properties"
    (is (< (resolution/property-rank :padding)
           (resolution/property-rank :padding-right))))

  (testing "commutative self conditions have a canonical order"
    (let [hover-focus ["&:hover" "&:focus"]
          focus-hover ["&:focus" "&:hover"]]
      (is (= ["&:focus" "&:hover"]
             (resolution/canonical-condition-path hover-focus)))
      (is (= (resolution/resolution-condition hover-focus nil)
             (resolution/resolution-condition focus-hover nil)))
      (is (= (resolution/resolution-condition hover-focus {:name "components"})
             (resolution/resolution-condition focus-hover {:name "components"})))
      (is (= (resolution/condition-rank hover-focus)
             (resolution/condition-rank focus-hover)))))

  (testing "non-commutative selectors preserve their order"
    (is (not= (resolution/resolution-condition ["&:hover" "& .item"] nil)
              (resolution/resolution-condition ["& .item" "&:hover"] nil)))
    (is (not= (resolution/resolution-condition ["&:hover" "&::before"] nil)
              (resolution/resolution-condition ["&::before" "&:hover"] nil))))

  (testing "important and unsupported raw shorthands are rejected"
    (is (resolution/important? "0 !important"))
    (is (not (resolution/important? "url('/important!.svg')")))
    (is (resolution/unknown-shorthand? :grid-template))))

(deftest test-release-utility-class-names
  (let [release-env (assoc test-env :shadow.build/mode :release)
        basic (compiler/utility-class-name release-env "flex")
        variant (binding [compiler/*variants* [{:prefix "hover"
                                                :selector "&:hover"}]]
                  (compiler/utility-class-name release-env "flex"))
        layered (binding [compiler/*layer* {:name "components" :order nil}]
                  (compiler/utility-class-name release-env "flex"))]
    (is (every? #(re-matches #"us-[0-9a-f]{8}" %)
                [basic variant layered]))
    (is (= 3 (count (set [basic variant layered]))))
    (is (= basic (compiler/utility-class-name release-env "flex")))
    (is (= basic
           (:result (expand-utility #(compiler/simple-utility* release-env
                                                               "flex"
                                                               {:display :flex})))))
    (binding [cljs.env/*compiler*
              (atom {:shadow.build.cljs-bridge/state
                     {:shadow.build/mode :release}})]
      (is (= basic (compiler/utility-class-name test-env "flex"))))
    (is (= "flex" (compiler/utility-class-name test-env "flex")))))

(deftest test-raw-styles
  (testing "declaration maps register as content-addressed atomic utilities"
    (let [declaration {:-webkit-text-decoration :inherit}
          class-name (str "raw-" (compiler/content-hash declaration))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name declaration []]}
             (expand-utility #(compiler/raw* test-env declaration))))))

  (testing "raw declarations participate in composed-class collection"
    (let [collector (atom {:declarations []
                           :registrations 0
                           :layer compiler/unset-layer})]
      (binding [compiler/*class-declaration-collector* collector]
        (compiler/raw* test-env {:-webkit-text-decoration :inherit}))
      (is (= [{:modifiers []
               :declaration {:-webkit-text-decoration :inherit}}]
             (:declarations @collector)))))

  (testing "declarations must be compile-time CSS maps"
    (doseq [declaration [nil
                         {}
                         {:nested {:color :inherit}}
                         {:color true}
                         {:opacity ##Inf}]]
      (is (= :raw-declaration
             (try
               (compiler/raw* test-env declaration)
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-css-strings
  (testing "components are concatenated and tokens become variable references"
    (is (= "inset 0 0 0 2px var(--app-border-focus-elevated)"
           (compiler/str-values* test-env
                                 ["inset 0 0 0 2px " :app-border-focus-elevated])))
    (is (= "translate(0, 1.5)"
           (compiler/str-values* test-env ["translate(" 0 ", " 1.5 ")"]))))

  (testing "concatenated values can be passed to CSS utilities"
    (let [value (compiler/str-values* test-env
                                      ["inset 0 0 0 2px " :app-border-focus-elevated])
          class-name (str "shadow-" (compiler/content-hash value))]
      (is (= (shadow-layer-expansion class-name :--us-shadow value)
             (expand-utility #(color/shadow* test-env value))))))

  (testing "empty string components are preserved"
    (is (= "var(--token)"
           (compiler/str-values* test-env ["" :token ""]))))

  (testing "invalid components fail during expansion"
    (doseq [components [[] [:token/namespaced] [nil] [##Inf]]]
      (is (= :css-string
             (try
               (compiler/str-values* test-env components)
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-marker-classes
  (testing "class names are used as is and do not register styles"
    (with-redefs [compiler/register-atomic!
                  (fn [& _]
                    (throw (ex-info "unexpected style registration" {})))]
      (is (= "menu" (modifier/class* test-env :menu)))
      (is (= "menu" (modifier/class* test-env "menu")))))

  (testing "namespaced keywords include their namespace"
    (is (= "app_views--menu" (modifier/class* test-env :app.views/menu))))

  (testing "class names are the same in release builds"
    (is (= "menu" (modifier/class* (assoc test-env :shadow.build/mode :release) :menu))))

  (testing "class names are validated"
    (doseq [class-key [:menu! :app.views/menu! "menu item" "" 42 "1col" :2x "-1" "--x"]]
      (is (= :class-key
             (try
               (modifier/class* test-env class-key)
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-marker-selectors-in-build-modes
  (doseq [mode [:dev :release]]
    (let [env (assoc test-env :shadow.build/mode mode)
          marker (modifier/class* env :last-tab)
          collector (atom {:declarations []
                           :registrations 0
                           :layer compiler/unset-layer})]
      (binding [cljs.env/*compiler* (cljs.env/default-compiler-env)
                compiler/*class-declaration-collector* collector]
        (ana/load-core)
        (modifier/ancestor-has* env :tabs
                                '(uni.style.core/str "." "last-tab" ":hover")
                                '[(uni.style.core/hidden)])
        (modifier/sel-class* env :last-tab '[(uni.style.core/hidden)]))
      (is (= [(str "." (modifier/class* env :tabs) ":has(." marker ":hover) &")
              (str "& ." marker)]
             (mapv #(-> % :modifiers first) (:declarations @collector)))))))

(deftest test-marker-class-modifiers
  (testing "sel-class, ancestor, and self-or-ancestor selectors reference the class"
    (let [captured (atom [])]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (swap! captured conj [variant forms]))]
        (modifier/sel-class* test-env :menu [:rules])
        (modifier/ancestor* test-env :menu [:rules])
        (modifier/self-or-ancestor* test-env :menu [:rules]))
      (is (= [[{:prefix "sel-class-menu-9ca8e983"
                :selector "& .menu"}
               [:rules]]
              [{:prefix "ancestor-menu-9ca8e983"
                :selector ".menu &"}
               [:rules]]
              [{:prefix "self-or-ancestor-menu-9ca8e983"
                :selector ".menu&, .menu &"}
               [:rules]]]
             @captured))))

  (testing "class names don't collide with modifier variants in generated names"
    (let [captured (atom [])]
      (with-redefs [modifier/modifier* (fn [_env variant _forms]
                                         (swap! captured conj (:prefix variant)))]
        (modifier/ancestor* test-env :hover-card [:rules])
        (modifier/ancestor-modifier* test-env :card
                                     {:prefix "hover" :selector "&:hover"}
                                     [:rules]))
      (is (apply not= @captured))))

  (testing "ancestor composes with nested interaction modifiers"
    (let [registered (atom nil)
          result (with-redefs [compiler/resolve-compile-time-constant
                               (fn [_env form]
                                 (case form
                                   :hover-rules
                                   (modifier/modifier* test-env
                                                       {:prefix "hover"
                                                        :selector "&:hover"}
                                                       [:background-rule])

                                   :background-rule
                                   (compiler/simple-utility* test-env "bg-red"
                                                             {:background-color "red"})

                                   form))
                               compiler/register-atomic!
                               (fn [_env ns-sym class-name declaration modifiers]
                                 (reset! registered
                                         [ns-sym class-name declaration modifiers]))]
                   (modifier/ancestor* test-env :menu [:hover-rules]))]
      (is (= ["ancestor-menu-9ca8e983:hover:bg-red"] result))
      (is (= ['uni.style.fixture
              "ancestor-menu-9ca8e983:hover:bg-red"
              {:background-color "red"}
              [".menu &" "&:hover"]]
             @registered))))

  (testing "ancestor interaction modifiers apply state to the ancestor"
    (let [captured (atom nil)]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (reset! captured [variant forms]))]
        (modifier/ancestor-modifier* test-env :menu
                                     {:prefix "hover" :selector "&:hover"}
                                     [:rules]))
      (is (= [{:prefix "ancestor-hover-menu-9ca8e983"
               :selector ".menu:hover &"}
              [:rules]]
             @captured))))

  (testing "ancestor :has modifiers apply the selector to the ancestor"
    (let [captured (atom nil)]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (reset! captured [variant forms]))]
        (modifier/ancestor-has* test-env :menu "input" [:rules]))
      (let [[variant forms] @captured]
        (is (re-matches #"ancestor-has-[0-9a-f]{8}-menu-9ca8e983"
                        (:prefix variant)))
        (is (= ".menu:has(input) &" (:selector variant)))
        (is (= [:rules] forms)))))

  (testing "ancestor attribute modifiers apply the attribute to the ancestor"
    (let [captured (atom [])]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (swap! captured conj [variant forms]))]
        (modifier/ancestor-attr* test-env :menu :disabled [:rules])
        (modifier/ancestor-attr=* test-env :menu :aria-expanded true [:rules]))
      (is (= {:prefix "ancestor-attr-disabled-menu-9ca8e983"
              :selector ".menu[disabled] &"
              :inverse-selector ".menu:not([disabled]) &"}
             (ffirst @captured)))
      (let [[variant forms] (second @captured)]
        (is (re-matches #"ancestor-attr-aria-expanded-value-[0-9a-f]{8}-menu-9ca8e983"
                        (:prefix variant)))
        (is (= ".menu[aria-expanded=\"true\"] &" (:selector variant)))
        (is (= [:rules] forms))))))

(deftest test-animation-styles
  (testing "animation options use CSS defaults and the components layer"
    (is (= {:layer {:name "components" :order nil}
            :duration "0s"
            :easing "ease"
            :delay "0s"
            :iterations "1"
            :direction "normal"
            :fill-mode "none"
            :play-state "running"}
           (animation/resolve-animation-options test-env {})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unsupported animation options"
                          (animation/resolve-animation-options test-env {:unknown true}))))

  (testing "animation frames and options serialize as global keyframes"
    (let [options (animation/resolve-animation-options
                    test-env
                    {:duration "3s"
                     :easing "ease-in"
                     :delay "1s"
                     :iterations 2
                     :direction "reverse"
                     :fill-mode "both"
                     :play-state "paused"})]
      (is (= (str "@layer components{@keyframes some-animation{"
                  "0%{opacity:0%;transform:scale(0.8);}"
                  "10%,20%{opacity:20%;transform:scale(0.9);}"
                  "100%{opacity:100%;transform:scale(1);}}}")
             (animation/animation-css
               "some-animation"
               (:layer options)
               [{:selector "0%"
                 :declaration (array-map :opacity "0%" :transform "scale(0.8)")}
                {:selector "10%,20%"
                 :declaration (array-map :opacity "20%" :transform "scale(0.9)")}
                {:selector "100%"
                 :declaration (array-map :opacity "100%" :transform "scale(1)")}])))
      (is (= "3s ease-in 1s 2 reverse both paused some-animation"
             (animation/animation-value "some-animation" options)))))

  (testing "animation-none disables animations"
    (is (= {:result "animation-none"
            :registered ['uni.style.fixture "animation-none" {:animation :none} []]}
           (expand-utility #(animation/animation-none* test-env)))))

  (testing "animation definitions require a symbol and properties map"
    (is (= :animation-symbol
           (try
             (animation/def-animation* test-env :animation {})
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e))))))
    (is (= :animation-properties
           (try
             (animation/def-animation* test-env 'animation nil)
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e)))))))

  (testing "animation steps are validated"
    (is (= :animation-step
           (try
             (animation/resolve-step test-env 101)
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e))))))))

(defn- collect-declarations [f]
  (let [collector (atom {:declarations []
                         :registrations 0
                         :layer compiler/unset-layer})
        result (binding [compiler/*class-declaration-collector* collector] (f))]
    [result (:declaration (first (:declarations @collector)))]))

(deftest test-transform-utilities
  (testing "scale sets both axis properties and the shared scale class"
    (are [percentage class-name factor]
      (= [(str class-name "----us-scale-x " class-name "----us-scale-y scale-axes")
          {:--us-scale-x factor :--us-scale-y factor
           :scale "var(--us-scale-x, 1) var(--us-scale-y, 1)"}]
         (collect-declarations #(transform/scale* test-env percentage)))
      80 "scale-80" "0.8"
      33.3 "scale-33_3" "0.333"
      "0.75" (str "scale-value-" (compiler/content-hash "0.75")) "0.75"
      :scale-factor "scale--scale-factor" "var(--scale-factor)"))

  (testing "axis scale sets one axis and accepts negative percentages"
    (is (= ["scale-x--100 scale-axes"
            {:--us-scale-x "-1" :scale "var(--us-scale-x, 1) var(--us-scale-y, 1)"}]
           (collect-declarations #(transform/scale-axis* test-env :x -100))))
    (is (= :scale
           (try (transform/scale* test-env -10) nil
                (catch clojure.lang.ExceptionInfo e (:type (ex-data e)))))))

  (testing "rotate sets the rotate property"
    (are [angle class-name css-value]
      (= [class-name {:rotate css-value}]
         (collect-declarations #(transform/rotate* test-env angle)))
      45 "rotate-45" "45deg"
      -90 "rotate--90" "-90deg"
      "0.5turn" (str "rotate-value-" (compiler/content-hash "0.5turn")) "0.5turn"
      :rotation "rotate--rotation" "var(--rotation)"))

  (testing "rotate rejects unsupported values"
    (doseq [angle [nil :namespaced/value ""]]
      (is (= :rotate
             (try
               (transform/rotate* test-env angle)
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e))))))))

  (testing "translate sets both axis properties and the shared translate class"
    (are [x y class-name x-value y-value]
      (= [(str class-name "----us-translate-x " class-name "----us-translate-y translate-axes")
          {:--us-translate-x x-value :--us-translate-y y-value
           :translate "var(--us-translate-x, 0) var(--us-translate-y, 0)"}]
         (collect-declarations #(transform/translate* test-env x y)))
      10 -20 "translate-10--20" "10px" "-20px"
      1.5 -0.5 "translate-1_5--0_5" "1.5px" "-0.5px"
      "50%" "calc(100% - 4px)"
      (str "translate-value-" (compiler/content-hash "50%")
           "-value-" (compiler/content-hash "calc(100% - 4px)"))
      "50%" "calc(100% - 4px)"
      :offset-x :offset-y "translate--offset-x--offset-y" "var(--offset-x)" "var(--offset-y)"))

  (testing "axis translate sets one axis property"
    (are [axis value class-name property css-value]
      (= [(str class-name " translate-axes")
          {property css-value
           :translate "var(--us-translate-x, 0) var(--us-translate-y, 0)"}]
         (collect-declarations #(transform/translate-axis* test-env axis value)))
      :x 10 "translate-x-10" :--us-translate-x "10px"
      :y -0.5 "translate-y--0_5" :--us-translate-y "-0.5px"
      :x "50%" (str "translate-x-value-" (compiler/content-hash "50%")) :--us-translate-x "50%"
      :y :offset-y "translate-y--offset-y" :--us-translate-y "var(--offset-y)"))

  (testing "animation frames get direct values so keyframes interpolate"
    (binding [compiler/*keyframes?* true]
      (is (= {:scale "0.8"} (second (collect-declarations #(transform/scale* test-env 80)))))
      (is (= {:translate "10px var(--us-translate-y, 0)"}
             (second (collect-declarations #(transform/translate-axis* test-env :x 10)))))
      (is (= {:translate "10px -20px"}
             (second (collect-declarations #(transform/translate* test-env 10 -20)))))
      (is (= {:scale "var(--us-scale-x, 1) -1"}
             (second (collect-declarations #(transform/scale-axis* test-env :y -100)))))))

  (testing "arbitrary transforms and resets"
    (is (= [(str "transform-value-" (compiler/content-hash "rotate(90deg) translateX(1px)"))
            {:transform "rotate(90deg) translateX(1px)"}]
           (collect-declarations #(transform/transform* test-env "rotate(90deg) translateX(1px)"))))
    (is (= ["translate-none" {:translate :none}]
           (collect-declarations #(transform/static* test-env :translate-none)))))

  (testing "translate rejects unsupported values"
    (doseq [[f args] [[transform/translate* [test-env 0 nil]]
                      [transform/translate* [test-env :namespaced/value 0]]
                      [transform/translate* [test-env "" 0]]
                      [transform/translate-axis* [test-env :x nil]]
                      [transform/translate-axis* [test-env :y :namespaced/value]]
                      [transform/translate-axis* [test-env :x ""]]]]
      (is (= :translate
             (try
               (apply f args)
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-transition-utilities
  (testing "transition property presets use Tailwind defaults"
    (doseq [[utility expected-class expected-property]
            [[:transition-all "transition-all" "all"]
             [:transition-colors "transition-colors" transition/color-properties]
             [:transition-opacity "transition-opacity" "opacity"]
             [:transition-shadow "transition-shadow" "box-shadow"]
             [:transition-transform "transition-transform" "transform, translate, scale, rotate"]]]
      (let [collector (atom {:declarations []
                             :registrations 0
                             :layer compiler/unset-layer})]
        (is (= (str expected-class "--transition-property "
                    expected-class "--transition-duration "
                    expected-class "--transition-timing-function")
               (binding [compiler/*class-declaration-collector* collector]
                 (transition/static* test-env utility))))
        (is (= [{:modifiers []
                 :declaration {:transition-property expected-property
                               :transition-duration "150ms"
                               :transition-timing-function "cubic-bezier(0.4, 0, 0.2, 1)"}}]
               (:declarations @collector)))))
    (is (= {:result "transition-none"
            :registered ['uni.style.fixture "transition-none"
                         {:transition-property :none} []]}
           (expand-utility #(transition/static* test-env :transition-none)))))

  (testing "transition shorthand accepts literals and variables"
    (doseq [[form expected-class expected-value]
            [["background 100ms ease-in-out"
              (str "transition-value-" (compiler/content-hash "background 100ms ease-in-out"))
              "background 100ms ease-in-out"]
             [:transition "transition--transition" "var(--transition)"]]]
      (let [{:keys [result registered]} (expand-utility #(transition/transition* test-env form))]
        (is (= expected-class result))
        (is (= expected-value (get-in registered [2 :transition]))))))

  (testing "behavior utilities"
    (is (= {:result "transition-normal"
            :registered ['uni.style.fixture "transition-normal"
                         {:transition-behavior :normal} []]}
           (expand-utility #(transition/behavior* test-env :transition-normal))))
    (is (= {:result "transition-discrete"
            :registered ['uni.style.fixture "transition-discrete"
                         {:transition-behavior :allow-discrete} []]}
           (expand-utility #(transition/behavior* test-env :transition-discrete)))))

  (testing "duration and timing presets"
    (is (= {:result "duration-initial"
            :registered ['uni.style.fixture "duration-initial"
                         {:transition-duration :initial} []]}
           (expand-utility #(transition/duration-initial* test-env))))
    (doseq [[utility expected-class expected-value]
            [[:ease-linear "ease-linear" :linear]
             [:ease-in "ease-in" "cubic-bezier(0.4, 0, 1, 1)"]
             [:ease-out "ease-out" "cubic-bezier(0, 0, 0.2, 1)"]
             [:ease-in-out "ease-in-out" "cubic-bezier(0.4, 0, 0.2, 1)"]
             [:ease-initial "ease-initial" :initial]]]
      (let [{:keys [result registered]} (expand-utility #(transition/timing* test-env utility))]
        (is (= expected-class result))
        (is (= expected-value (get-in registered [2 :transition-timing-function]))))))

  (testing "duration, timing, and delay accept literals and variables"
    (doseq [[f form expected-class property expected-value]
            [[transition/duration* 150 "duration-150" :transition-duration "150ms"]
             [transition/duration* "0.3s"
              (str "duration-value-" (compiler/content-hash "0.3s"))
              :transition-duration "0.3s"]
             [transition/duration* :duration "duration--duration"
              :transition-duration "var(--duration)"]
             [transition/ease* "ease-in-out"
              (str "ease-value-" (compiler/content-hash "ease-in-out"))
              :transition-timing-function "ease-in-out"]
             [transition/ease* :ease "ease--ease"
              :transition-timing-function "var(--ease)"]
             [transition/delay* -50 "delay--50" :transition-delay "-50ms"]
             [transition/delay* :delay "delay--delay"
              :transition-delay "var(--delay)"]]]
      (let [{:keys [result registered]} (expand-utility #(f test-env form))]
        (is (= expected-class result))
        (is (= expected-value (get-in registered [2 property]))))))

  (testing "unsupported and malformed values are rejected"
    (doseq [[f form] [[transition/transition* nil]
                      [transition/transition* 1]
                      [transition/duration* 1.5]
                      [transition/ease* ""]
                      [transition/delay* :namespace/delay]]]
      (is (thrown? clojure.lang.ExceptionInfo (f test-env form))))))

(deftest test-base-styles
  (testing "utilities are collected and registered against the selector"
    (let [registered (atom nil)
          result (with-redefs [compiler/resolve-compile-time-constant
                               (fn [_env form]
                                 (case form
                                   :margin-rule
                                   (spacing/margin* test-env "m" :margin 0)

                                   :padding-rule
                                   (spacing/padding* test-env "p" :padding 0)

                                   :raw-rule
                                   (compiler/raw* test-env {:-webkit-text-decoration :inherit})

                                   form))
                               compiler/register-selector!
                               (fn [_env ns-sym registry-key selector declarations layer]
                                 (reset! registered [ns-sym registry-key selector
                                                     declarations layer]))]
                   (composition/def-base* test-env "html, body"
                     [:margin-rule :padding-rule :raw-rule]))]
      (is (nil? result))
      (is (= 'uni.style.fixture (first @registered)))
      (is (re-matches #"uni-style-def-base-[0-9a-f]{8}" (second @registered)))
      (is (= ["html, body"
              [{:modifiers []
                :declaration {:margin "0px"
                              :padding "0px"
                              :-webkit-text-decoration :inherit}}]
              {:name "base" :order nil}]
             (subvec (vec @registered) 2)))))

  (testing "the selector must be a non-empty compile-time string"
    (doseq [selector [nil "" :body]]
      (is (= :selector
             (try
               (composition/def-base* test-env selector ['(uni.style.core/m 0)])
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-component-class-config
  (is (false? (compiler/qualified-component-classes? test-env)))
  (binding [cljs.env/*compiler*
            (atom {:shadow.build.cljs-bridge/state
                   {:shadow.build/config
                    {:css-options {:qualified-component-classes true}}}})]
    (is (true? (compiler/qualified-component-classes? test-env))))
  (testing "the compiler options' :css-options, as in ClojureScript ES module builds"
    (binding [cljs.env/*compiler*
              (atom {:options {:css-options {:qualified-component-classes true}}})]
      (is (true? (compiler/qualified-component-classes? test-env))))))

(deftest test-development-composed-class-names
  (let [composed #(binding [cljs.env/*compiler* (atom {:options {}})]
                    (last (compiler/composed-class-definition
                            test-env % (name %) false true)))]
    (testing "are readable, suffixed with a hash of the qualified symbol"
      (is (re-matches #"panel-[0-9a-f]{8}" (composed 'example/panel))))
    (testing "are stable across recompiles"
      (is (= (composed 'example/panel) (composed 'example/panel))))
    (testing "differ between namespaces"
      (is (not= (composed 'example/panel) (composed 'other/panel))))))

(defn- with-system-properties [properties f]
  (let [previous (into {} (map (fn [k] [k (System/getProperty k)])) (keys properties))]
    (try
      (doseq [[k v] properties]
        (if (some? v) (System/setProperty k v) (System/clearProperty k)))
      (f)
      (finally
        (doseq [[k v] previous]
          (if (some? v) (System/setProperty k v) (System/clearProperty k)))))))

(deftest test-jvm-class-names
  (let [jvm-env {}
        names #(with-system-properties %
                 (fn []
                   {:utility (compiler/utility-class-name jvm-env "flex")
                    :composed (last (compiler/composed-class-definition
                                      jvm-env 'example/panel "panel" false true))
                    :qualified? (compiler/qualified-component-classes? jvm-env)}))
        cljs-dev (binding [cljs.env/*compiler* (atom {:options {}})]
                   {:utility (compiler/utility-class-name test-env "flex")
                    :composed (last (compiler/composed-class-definition
                                      test-env 'example/panel "panel" false true))})]
    (testing "are hashed release names by default"
      (let [{:keys [utility composed qualified?]}
            (names {"uni.style.short-class-names" nil
                    "uni.style.qualified-component-classes" nil})]
        (is (re-matches #"us-[0-9a-f]{8}" utility))
        (is (re-matches #"us-[0-9a-f]{8}" composed))
        (is (false? qualified?))))
    (testing "match ClojureScript development names with short class names off"
      (is (= cljs-dev
             (select-keys (names {"uni.style.short-class-names" "false"})
                          [:utility :composed]))))
    (testing "include the namespace with qualified component classes on"
      (is (true? (:qualified? (names {"uni.style.qualified-component-classes" "true"})))))
    (testing "reject values other than true or false"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"must be true or false"
                            (names {"uni.style.short-class-names" "no"}))))))

(deftest test-short-class-names-option
  (let [hashed? #(boolean (re-matches #"us-[0-9a-f]{8}" %))
        utility #(binding [cljs.env/*compiler* (atom %)]
                   (compiler/utility-class-name test-env "flex"))
        composed #(binding [cljs.env/*compiler* (atom %)]
                    (last (compiler/composed-class-definition
                            test-env 'example/panel nil true false)))]
    (testing "defaults to whether goog.DEBUG is defined false"
      (is (not (hashed? (utility {:options {}}))))
      (is (hashed? (utility {:options {:closure-defines {"goog.DEBUG" false}}}))))
    (testing ":short-class-names under the compiler options' :css-options"
      (is (hashed? (utility {:options {:css-options {:short-class-names true}}})))
      (is (hashed? (composed {:options {:css-options {:short-class-names true}}})))
      (is (= "flex" (utility {:options {:closure-defines {"goog.DEBUG" false}
                                        :css-options {:short-class-names false}}})))
      (is (not (hashed? (composed {:options {:closure-defines {"goog.DEBUG" false}
                                             :css-options {:short-class-names false}}})))))
    (testing ":short-class-names under Shadow's :css-options, over its release mode"
      (is (hashed? (utility {:shadow.build.cljs-bridge/state
                             {:shadow.build/config {:css-options {:short-class-names true}}}})))
      (is (= "flex" (utility {:shadow.build.cljs-bridge/state
                              {:shadow.build/mode :release
                               :shadow.build/config {:css-options {:short-class-names false}}}}))))))

(deftest test-predefined-layers
  (is (= {:name "base" :order nil} (compiler/resolve-layer test-env :base)))
  (is (= {:name "components" :order nil}
         (compiler/resolve-layer test-env :components)))
  (is (= {:name "variants" :order nil}
         (compiler/resolve-layer test-env :variants)))
  (doseq [layer [0 1 :other "components"]]
    (is (= :layer
           (try
             (compiler/resolve-layer test-env layer)
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e))))))))

(deftest test-variants-key
  (is (= {:key :primary :key-parts ["primary"]}
         (composition/resolve-variants-key test-env :primary)))
  (is (= {:key [:primary :purple :elevated]
          :key-parts ["primary" "purple" "elevated"]}
         (composition/resolve-variants-key test-env [:primary :purple :elevated])))
  (doseq [variant-key [nil "primary" [] [:primary "purple"] :button/primary]]
    (is (= :variants-key
           (try
             (composition/resolve-variants-key test-env variant-key)
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e))))))))

(deftest test-border-arities
  (testing "one argument defaults the width and uses a solid style"
    (is (= {:result "border--app-border-base border-1 border-style"
            :registered [['uni.style.fixture "border--app-border-base"
                          {:border-color "var(--app-border-base)"}]
                         ['uni.style.fixture "border-1" {:border-width 1}]
                         ['uni.style.fixture "border-style"
                          {:border-style :solid}]]}
           (expand-border "border" "border" [:app-border-base]))))

  (testing "two arguments set width and color"
    (is (= "border-color-4bc9274d border-2 border-style"
           (:result (expand-border "border" "border" [2 "red"])))))

  (testing "three arguments set a token color fallback"
    (let [fallback "red"
          class-name (str "border--app-border-base-color-"
                          (compiler/content-hash fallback))]
      (is (= {:result (str class-name " border-1_5 border-style")
              :registered [['uni.style.fixture class-name
                            {:border-color "var(--app-border-base, red)"}]
                           ['uni.style.fixture "border-1_5" {:border-width 1.5}]
                           ['uni.style.fixture "border-style"
                            {:border-style :solid}]]}
             (expand-border "border" "border"
                            [1.5 :app-border-base fallback]))))))

(deftest test-directional-border-mappings
  (are [prefix property result declaration]
    (let [{actual-result :result registered :registered}
          (expand-border prefix property [2 :app-border-subtle])]
      (and (= result actual-result)
           (= declaration (mapv #(nth % 2) registered))))
    "border" "border"
    "border--app-border-subtle border-2 border-style"
    [{:border-color "var(--app-border-subtle)"}
     {:border-width 2}
     {:border-style :solid}]

    "border-t" "border-top"
    "border-t--app-border-subtle border-t-2 border-t-style"
    [{:border-top-color "var(--app-border-subtle)"}
     {:border-top-width 2}
     {:border-top-style :solid}]

    "border-r" "border-right"
    "border-r--app-border-subtle border-r-2 border-r-style"
    [{:border-right-color "var(--app-border-subtle)"}
     {:border-right-width 2}
     {:border-right-style :solid}]

    "border-b" "border-bottom"
    "border-b--app-border-subtle border-b-2 border-b-style"
    [{:border-bottom-color "var(--app-border-subtle)"}
     {:border-bottom-width 2}
     {:border-bottom-style :solid}]

    "border-l" "border-left"
    "border-l--app-border-subtle border-l-2 border-l-style"
    [{:border-left-color "var(--app-border-subtle)"}
     {:border-left-width 2}
     {:border-left-style :solid}]))

(deftest test-border-width-utility
  (is (= {:result "border-width-0"
          :registered ['uni.style.fixture "border-width-0" {:border-width 0} []]}
         (expand-utility #(border/border-width* test-env 0))))
  (let [width "0.125rem"
        class-name (str "border-width-value-" (compiler/content-hash width))]
    (is (= {:result class-name
            :registered ['uni.style.fixture class-name {:border-width width} []]}
           (expand-utility #(border/border-width* test-env width)))))
  (is (= {:result "border-width--hairline"
          :registered ['uni.style.fixture "border-width--hairline"
                       {:border-width "var(--hairline)"} []]}
         (expand-utility #(border/border-width* test-env :hairline))))
  (is (= :border-width
         (try
           (expand-utility #(border/border-width* test-env -1))
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))))

(deftest test-border-color-utility
  (testing "literal and token colors"
    (is (= {:result "border--app-border-base"
            :registered ['uni.style.fixture "border--app-border-base"
                         {:border-color "var(--app-border-base)"} []]}
           (expand-utility #(border/border-color* test-env :app-border-base))))
    (let [color "rgba(255,255,255,0.35)"
          class-name (str "border-color-" (compiler/content-hash color))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:border-color color} []]}
             (expand-utility #(border/border-color* test-env color))))))

  (testing "token colors support fallbacks"
    (is (= {:result "border--app-border-base--app-border-subtle"
            :registered ['uni.style.fixture
                         "border--app-border-base--app-border-subtle"
                         {:border-color "var(--app-border-base, var(--app-border-subtle))"}
                         []]}
           (expand-utility #(border/border-color* test-env
                                                  :app-border-base
                                                  :app-border-subtle)))))

  (testing "invalid colors fail during expansion"
    (is (= :border-color
           (try
             (expand-utility #(border/border-color* test-env 42))
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e))))))))

(deftest test-border-style-utilities
  (are [style class-name]
    (= {:result class-name
        :registered ['uni.style.fixture class-name {:border-style style} []]}
       (expand-utility #(border/border-style* test-env style)))
    :solid "border-solid"
    :dashed "border-dashed"
    :dotted "border-dotted"
    :double "border-double"
    :none "border-none"))

(deftest test-outline-arities
  (testing "one argument defaults the width and uses a solid style"
    (is (= {:result "outline--app-border-focus outline-1 outline-style"
            :registered [['uni.style.fixture "outline--app-border-focus"
                          {:outline-color "var(--app-border-focus)"}]
                         ['uni.style.fixture "outline-1" {:outline-width 1}]
                         ['uni.style.fixture "outline-style"
                          {:outline-style :solid}]]}
           (let [registered (atom [])
                 result (with-redefs [compiler/register-atomic!
                                      (fn [_env ns-sym class-name declaration modifiers]
                                        (swap! registered conj
                                               (cond-> [ns-sym class-name declaration]
                                                       (seq modifiers) (conj modifiers))))]
                          (border/outline* test-env [:app-border-focus]))]
             {:result result :registered @registered}))))

  (testing "two arguments set width and color"
    (is (= "outline-color-4bc9274d outline-2 outline-style"
           (:result (expand-utility #(border/outline* test-env [2 "red"]))))))

  (testing "three arguments set a token color fallback"
    (is (= {:result "outline--app-border-focus--app-border-base outline-2 outline-style"
            :registered [['uni.style.fixture
                          "outline--app-border-focus--app-border-base"
                          {:outline-color "var(--app-border-focus, var(--app-border-base))"}]
                         ['uni.style.fixture "outline-2" {:outline-width 2}]
                         ['uni.style.fixture "outline-style"
                          {:outline-style :solid}]]}
           (let [registered (atom [])
                 result (with-redefs [compiler/register-atomic!
                                      (fn [_env ns-sym class-name declaration modifiers]
                                        (swap! registered conj
                                               (cond-> [ns-sym class-name declaration]
                                                       (seq modifiers) (conj modifiers))))]
                          (border/outline* test-env
                                           [2 :app-border-focus :app-border-base]))]
             {:result result :registered @registered})))))

(deftest test-outline-style-utilities
  (are [style class-name]
    (= {:result class-name
        :registered ['uni.style.fixture class-name {:outline-style style} []]}
       (expand-utility #(border/outline-style* test-env style)))
    :solid "outline-solid"
    :dashed "outline-dashed"
    :dotted "outline-dotted"
    :double "outline-double"
    :none "outline-none"))

(deftest test-outline-utilities
  (testing "individual outline properties use matching atomic classes"
    (is (= {:result "outline-2"
            :registered ['uni.style.fixture "outline-2" {:outline-width 2} []]}
           (expand-utility #(border/outline-width* test-env 2))))
    (is (= {:result "outline--focus-width"
            :registered ['uni.style.fixture "outline--focus-width"
                         {:outline-width "var(--focus-width)"} []]}
           (expand-utility #(border/outline-width* test-env :focus-width))))
    (is (= {:result "outline--app-border-focus"
            :registered ['uni.style.fixture "outline--app-border-focus"
                         {:outline-color "var(--app-border-focus)"} []]}
           (expand-utility #(border/outline-color* test-env :app-border-focus))))
    (is (= {:result "outline-offset--2"
            :registered ['uni.style.fixture "outline-offset--2"
                         {:outline-offset "-2px"} []]}
           (expand-utility #(border/outline-offset* test-env -2))))
    (is (= {:result "outline-offset--fancy-focus-ring-offset"
            :registered ['uni.style.fixture "outline-offset--fancy-focus-ring-offset"
                         {:outline-offset "var(--fancy-focus-ring-offset)"} []]}
           (expand-utility #(border/outline-offset* test-env :fancy-focus-ring-offset)))))

  (testing "outline properties support variable fallbacks"
    (is (= {:result "outline--focus-width-3"
            :registered ['uni.style.fixture
                         "outline--focus-width-3"
                         {:outline-width "var(--focus-width, 3px)"}
                         []]}
           (expand-utility #(border/outline-width* test-env :focus-width 3))))
    (is (= {:result "outline--focus-width--fallback-width"
            :registered ['uni.style.fixture
                         "outline--focus-width--fallback-width"
                         {:outline-width "var(--focus-width, var(--fallback-width))"}
                         []]}
           (expand-utility #(border/outline-width* test-env
                                                   "var(--focus-width)"
                                                   :fallback-width))))
    (is (= {:result "outline--app-border-focus--app-border-base"
            :registered ['uni.style.fixture
                         "outline--app-border-focus--app-border-base"
                         {:outline-color "var(--app-border-focus, var(--app-border-base))"}
                         []]}
           (expand-utility #(border/outline-color* test-env :app-border-focus
                                                   :app-border-base))))
    (is (= {:result "outline-offset--fancy-focus-ring-offset--0_5"
            :registered ['uni.style.fixture
                         "outline-offset--fancy-focus-ring-offset--0_5"
                         {:outline-offset "var(--fancy-focus-ring-offset, -0.5px)"}
                         []]}
           (expand-utility #(border/outline-offset* test-env :fancy-focus-ring-offset -0.5))))
    (is (= {:result "outline-offset--fancy-focus-ring-offset--fallback-offset"
            :registered ['uni.style.fixture
                         "outline-offset--fancy-focus-ring-offset--fallback-offset"
                         {:outline-offset "var(--fancy-focus-ring-offset, var(--fallback-offset))"}
                         []]}
           (expand-utility #(border/outline-offset* test-env
                                                    "var(--fancy-focus-ring-offset)"
                                                    :fallback-offset)))))

  (testing "invalid outline values fail during expansion"
    (are [f type]
      (= type
         (try
           (expand-utility f)
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))
      #(border/outline* test-env [-1 :app-border-focus]) :outline-width
      #(border/outline* test-env [1 42]) :outline-color
      #(border/outline* test-env [1 "red" "blue"]) :outline-color
      #(border/outline-width* test-env -1) :outline-width
      #(border/outline-width* test-env 2 1) :outline-width
      #(border/outline-width* test-env :focus-width -1) :outline-width
      #(border/outline-color* test-env 42) :outline-color
      #(border/outline-offset* test-env true) :outline-offset
      #(border/outline-offset* test-env :foo/offset) :outline-offset
      #(border/outline-offset* test-env 2 1) :outline-offset
      #(border/outline-offset* test-env "2px" 1) :outline-offset)))

(deftest test-rounded-mappings
  (are [prefix properties class-name declaration]
    (= {:result class-name
        :registered ['uni.style.fixture class-name declaration []]}
       (expand-rounded prefix properties 4))
    "rounded" [:border-radius]
    "rounded-4" {:border-radius 4}

    "rounded-tl" [:border-top-left-radius]
    "rounded-tl-4" {:border-top-left-radius 4}

    "rounded-tr" [:border-top-right-radius]
    "rounded-tr-4" {:border-top-right-radius 4}

    "rounded-br" [:border-bottom-right-radius]
    "rounded-br-4" {:border-bottom-right-radius 4}

    "rounded-bl" [:border-bottom-left-radius]
    "rounded-bl-4" {:border-bottom-left-radius 4})

  (doseq [[prefix properties]
          [["rounded-t" [:border-top-left-radius :border-top-right-radius]]
           ["rounded-r" [:border-top-right-radius :border-bottom-right-radius]]
           ["rounded-b" [:border-bottom-right-radius :border-bottom-left-radius]]
           ["rounded-l" [:border-bottom-left-radius :border-top-left-radius]]]]
    (let [base-class (str prefix "-4")
          classes (mapv #(str base-class "--" (name %)) properties)]
      (is (= {:result (str/join " " classes)
              :registered (mapv (fn [class-name property]
                                  ['uni.style.fixture class-name {property 4} []])
                                classes properties)}
             (expand-rounded prefix properties 4)))))

  (testing "decimal radii have valid class names"
    (is (= "rounded-1_5"
           (:result (expand-rounded "rounded" [:border-radius] 1.5)))))

  (testing "radii support literal strings and token keywords"
    (let [literal "50%"
          literal-class (str "rounded-value-" (compiler/content-hash literal))]
      (is (= {:result literal-class
              :registered ['uni.style.fixture literal-class {:border-radius literal} []]}
             (expand-rounded "rounded" [:border-radius] literal))))
    (is (= {:result "rounded--radius"
            :registered ['uni.style.fixture "rounded--radius"
                         {:border-radius "var(--radius)"} []]}
           (expand-rounded "rounded" [:border-radius] :radius))))

  (testing "numeric radii must be non-negative and all values must be valid literals"
    (are [radius] (= :border-radius
                     (try
                       (expand-rounded "rounded" [:border-radius] radius)
                       nil
                       (catch clojure.lang.ExceptionInfo e
                         (:type (ex-data e)))))
      -1
      ""
      :theme/radius
      true)))

(deftest test-user-select-utilities
  (are [class-name declaration]
    (= {:result class-name
        :registered ['uni.style.fixture class-name declaration []]}
       (expand-utility #(compiler/simple-utility* test-env class-name declaration)))
    "select-none" {:user-select :none}
    "select-text" {:user-select :text}
    "select-all" {:user-select :all}
    "select-auto" {:user-select :auto}))

(deftest test-background-utilities
  (testing "keywords become variables and strings remain literal"
    (is (= {:result "bg--app-bg-base"
            :registered ['uni.style.fixture "bg--app-bg-base"
                         {:background-color "var(--app-bg-base)"} []]}
           (expand-utility #(color/bg* test-env :app-bg-base))))
    (let [value "var(--custom-bg)"
          class-name (str "bg-color-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name
                           {:background-color value} []]}
             (expand-utility #(color/bg* test-env value)))))
    (let [value "red"
          class-name (str "bg-color-" (compiler/content-hash value))]
      (is (= class-name
             (:result (expand-utility #(color/bg* test-env value)))))))

  (testing "fallbacks accept token variables or literal strings"
    (is (= {:result "bg--app-bg-base--app-bg-fallback"
            :registered ['uni.style.fixture "bg--app-bg-base--app-bg-fallback"
                         {:background-color "var(--app-bg-base, var(--app-bg-fallback))"}
                         []]}
           (expand-utility #(color/bg* test-env :app-bg-base :app-bg-fallback))))
    (let [fallback "#fff"
          class-name (str "bg--app-bg-base-color-" (compiler/content-hash fallback))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name
                           {:background-color "var(--app-bg-base, #fff)"} []]}
             (expand-utility #(color/bg* test-env :app-bg-base fallback))))))

  (testing "static background-color utilities"
    (are [class-name declaration]
      (= class-name
         (:result (expand-utility #(compiler/simple-utility* test-env class-name declaration))))
      "bg-transparent" {:background-color :transparent}
      "bg-current" {:background-color "currentColor"}
      "bg-inherit" {:background-color :inherit}
      "bg-unset" {:background-color :unset}))

  (testing "arbitrary background images use content hashes"
    (let [value "linear-gradient(red, blue)"
          class-name (str "bg-image-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name
                           {:background-image value} []]}
             (expand-utility #(color/bg-image* test-env value))))))

  (testing "mask properties emit prefixed and unprefixed declarations"
    (let [value "linear-gradient(to right, black, transparent)"
          class-name (str "mask-image-value-" (compiler/content-hash value))]
      (is (= {:result (str class-name "---webkit-mask-image " class-name "--mask-image")
              :registered [['uni.style.fixture (str class-name "---webkit-mask-image")
                            {:-webkit-mask-image value} []]
                           ['uni.style.fixture (str class-name "--mask-image")
                            {:mask-image value} []]]}
             (expand-utility #(color/mask-value* test-env "mask-image" :mask-image value
                                                 "Mask image" :mask-image)))))
    (is (= {:result "mask-size--mask-size---webkit-mask-size mask-size--mask-size--mask-size"
            :registered [['uni.style.fixture "mask-size--mask-size---webkit-mask-size"
                          {:-webkit-mask-size "var(--mask-size)"} []]
                         ['uni.style.fixture "mask-size--mask-size--mask-size"
                          {:mask-size "var(--mask-size)"} []]]}
           (expand-utility #(color/mask-value* test-env "mask-size" :mask-size :mask-size
                                               "Mask size" :mask-size))))
    (is (= :mask-position
           (try
             (color/mask-value* test-env "mask-position" :mask-position 1
                                "Mask position" :mask-position)
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e)))))))

  (testing "background overlays paint a solid color over the background color"
    (is (= {:result "bg-overlay--col"
            :registered ['uni.style.fixture "bg-overlay--col"
                         {:background-image "linear-gradient(var(--col), var(--col))"} []]}
           (expand-utility #(color/bg-overlay* test-env :col))))
    (is (= {:result "bg-overlay--col/10"
            :registered ['uni.style.fixture "bg-overlay--col/10"
                         {:background-image
                          (str "linear-gradient(color-mix(in oklab, var(--col) 10%, transparent), "
                               "color-mix(in oklab, var(--col) 10%, transparent))")}
                         []]}
           (expand-utility #(color/bg-overlay* test-env (color/alpha* test-env :col 10)))))
    (is (= :bg-overlay
           (try
             (color/bg-overlay* test-env 42)
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e)))))))

  (testing "background image none"
    (is (= "bg-none"
           (:result (expand-utility #(compiler/simple-utility* test-env "bg-none"
                                                               {:background-image :none}))))))

  (testing "static background repeat, size, and position utilities"
    (are [utility class-name declaration]
      (= {:result class-name
          :registered ['uni.style.fixture class-name declaration []]}
         (expand-utility #(color/static* test-env utility)))
      :bg-repeat "bg-repeat" {:background-repeat :repeat}
      :bg-repeat-x "bg-repeat-x" {:background-repeat :repeat-x}
      :bg-repeat-y "bg-repeat-y" {:background-repeat :repeat-y}
      :bg-repeat-space "bg-repeat-space" {:background-repeat :space}
      :bg-repeat-round "bg-repeat-round" {:background-repeat :round}
      :bg-no-repeat "bg-no-repeat" {:background-repeat :no-repeat}
      :bg-auto "bg-auto" {:background-size :auto}
      :bg-cover "bg-cover" {:background-size :cover}
      :bg-contain "bg-contain" {:background-size :contain}
      :bg-clip-border "bg-clip-border" {:background-clip :border-box}
      :bg-clip-padding "bg-clip-padding" {:background-clip :padding-box}
      :bg-clip-content "bg-clip-content" {:background-clip :content-box}
      :bg-clip-text "bg-clip-text" {:background-clip :text}
      :bg-top-left "bg-top-left" {:background-position "top left"}
      :bg-top "bg-top" {:background-position :top}
      :bg-top-right "bg-top-right" {:background-position "top right"}
      :bg-left "bg-left" {:background-position :left}
      :bg-center "bg-center" {:background-position :center}
      :bg-right "bg-right" {:background-position :right}
      :bg-bottom-left "bg-bottom-left" {:background-position "bottom left"}
      :bg-bottom "bg-bottom" {:background-position :bottom}
      :bg-bottom-right "bg-bottom-right" {:background-position "bottom right"}))

  (testing "arbitrary background size and position values"
    (is (= {:result "bg-size--background-size"
            :registered ['uni.style.fixture "bg-size--background-size"
                         {:background-size "var(--background-size)"} []]}
           (expand-utility #(color/background-value* test-env "bg-size" :background-size
                                                     :background-size "Background size"
                                                     :background-size))))
    (doseq [[prefix property value label type]
            [["bg-size" :background-size "1px 2px" "Background size" :background-size]
             ["bg-position" :background-position "0 100%" "Background position" :background-position]]]
      (let [class-name (str prefix "-" (compiler/content-hash value))]
        (is (= {:result class-name
                :registered ['uni.style.fixture class-name {property value} []]}
               (expand-utility #(color/background-value* test-env prefix property value label type)))))))

  (testing "invalid values fail during expansion"
    (are [f type]
      (= type
         (try
           (expand-utility f)
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))
      #(color/bg* test-env 42) :background-color
      #(color/bg* test-env "red" :app-bg-fallback) :background-color
      #(color/bg* test-env :app-bg-base 42) :background-color
      #(color/bg-image* test-env 42) :background-image
      #(color/background-value* test-env "bg-size" :background-size "" "Background size" :background-size) :background-size
      #(color/background-value* test-env "bg-position" :background-position 42 "Background position" :background-position) :background-position)))

(deftest test-background-blend-mode-utilities
  (doseq [mode [:normal :multiply :screen :overlay :darken :lighten :color-dodge
                :color-burn :hard-light :soft-light :difference :exclusion :hue
                :saturation :color :luminosity]]
    (let [class-name (str "bg-blend-" (name mode))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:background-blend-mode mode} []]}
             (expand-utility #(color/static* test-env (keyword class-name))))))))

(deftest test-mix-blend-mode-utilities
  (are [utility class-name mode]
    (= {:result class-name
        :registered ['uni.style.fixture class-name {:mix-blend-mode mode} []]}
       (expand-utility #(color/static* test-env utility)))
    :mix-blend-normal "mix-blend-normal" :normal
    :mix-blend-multiply "mix-blend-multiply" :multiply
    :mix-blend-screen "mix-blend-screen" :screen
    :mix-blend-overlay "mix-blend-overlay" :overlay
    :mix-blend-darken "mix-blend-darken" :darken
    :mix-blend-lighten "mix-blend-lighten" :lighten
    :mix-blend-color-dodge "mix-blend-color-dodge" :color-dodge
    :mix-blend-color-burn "mix-blend-color-burn" :color-burn
    :mix-blend-hard-light "mix-blend-hard-light" :hard-light
    :mix-blend-soft-light "mix-blend-soft-light" :soft-light
    :mix-blend-difference "mix-blend-difference" :difference
    :mix-blend-exclusion "mix-blend-exclusion" :exclusion
    :mix-blend-hue "mix-blend-hue" :hue
    :mix-blend-saturation "mix-blend-saturation" :saturation
    :mix-blend-color "mix-blend-color" :color
    :mix-blend-luminosity "mix-blend-luminosity" :luminosity
    :mix-blend-plus-darker "mix-blend-plus-darker" :plus-darker
    :mix-blend-plus-lighter "mix-blend-plus-lighter" :plus-lighter))

(deftest test-custom-property-and-reset-utilities
  (testing "custom properties accept token-backed and literal values"
    (is (= {:result "var-shortcut-color-app-fg-base"
            :registered ['uni.style.fixture "var-shortcut-color-app-fg-base"
                         {:--shortcut-color "var(--app-fg-base)"} []]}
           (expand-utility #(style-var/var* test-env :shortcut-color :app-fg-base))))
    (let [value "currentColor"
          class-name (str "var-shortcut-color-value-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name
                           {:--shortcut-color value} []]}
             (expand-utility #(style-var/var* test-env "--shortcut-color" value))))))

  (testing "var-def returns the custom-property name"
    (is (= "--shortcut-color" (style-var/var-def* test-env :shortcut-color)))
    (is (= "--shortcut-color" (style-var/var-def* test-env "--shortcut-color")))
    (are [property]
      (= :custom-property
         (try
           (style-var/var-def* test-env property)
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))
      :bad/name
      "shortcut-color"
      42))

  (testing "custom-property names and values are validated"
    (are [property value type]
      (= type
         (try
           (expand-utility #(style-var/var* test-env property value))
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))
      :bad/name "x" :custom-property
      "shortcut-color" "x" :custom-property
      :shortcut-color "" :custom-property-value
      :shortcut-color :bad! :custom-property-value
      :shortcut-color ##Inf :custom-property-value))

  (testing "runtime values fail during compile-time resolution"
    (let [form '(runtime-value)]
      (with-redefs [ana/resolve-var (fn [_env sym] {:name sym})
                    constant/macroexpand (fn [_env expr] expr)]
        (try
          (expand-utility #(style-var/var* test-env :shortcut-color form))
          (is false "expected expansion to fail")
          (catch clojure.lang.ExceptionInfo e
            (is (= form (:form (ex-data e))))
            (is (:dynamic? (ex-data e))))))))

  (testing "static reset utilities emit their full properties"
    (is (= "shadow-none"
           (:result (expand-utility #(compiler/simple-utility* test-env "shadow-none"
                                                               {:box-shadow :none})))))
    (is (= "font-inherit"
           (:result (expand-utility #(compiler/simple-utility* test-env "font-inherit"
                                                               {:font :inherit})))))))

(deftest test-runtime-custom-property-bridge
  (let [runtime-hover-color "oklch(55% 0.2 20)"
        var-reference "var(--invite-remove-hover-bg)"
        class-name (str "bg-color-" (compiler/content-hash var-reference))
        props {:class (:result (expand-utility #(color/bg* test-env var-reference)))
               :style {"--invite-remove-hover-bg" runtime-hover-color}}]
    (is (= {:class class-name
            :style {"--invite-remove-hover-bg" runtime-hover-color}}
           props))
    (is (= ['uni.style.fixture class-name
            {:background-color "var(--invite-remove-hover-bg)"} []]
           (:registered (expand-utility #(color/bg* test-env var-reference)))))))

(deftest test-attribute-modifiers
  (testing "presence attributes use composable positive and inverse selectors"
    (let [captured (atom nil)]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (reset! captured [variant forms]))]
        (modifier/attr* test-env :disabled [:rules]))
      (is (= [{:prefix "attr-disabled"
               :selector "&[disabled]"
               :inverse-selector "&:not([disabled])"}
              [:rules]]
             @captured))))

  (testing "attribute values are escaped and class names are content-safe"
    (let [captured (atom nil)]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (reset! captured [variant forms]))]
        (modifier/attr=* test-env :data-state "open\"menu" [:rules]))
      (let [[{:keys [prefix selector inverse-selector]}] @captured]
        (is (re-matches #"attr-data-state-value-[0-9a-f]{8}" prefix))
        (is (= "&[data-state=\"open\\\"menu\"]" selector))
        (is (= "&:not([data-state=\"open\\\"menu\"])" inverse-selector)))))

  (testing "not toggles an attribute modifier and supports double negation"
    (let [negate-variant (ns-resolve 'uni.style.impl.modifier 'negate-variant)
          variant {:prefix "attr-disabled"
                   :selector "&[disabled]"
                   :inverse-selector "&:not([disabled])"}
          negated (negate-variant variant)]
      (is (= {:prefix "not-attr-disabled"
              :selector "&:not([disabled])"
              :inverse-selector "&[disabled]"}
             negated))
      (is (= variant (negate-variant negated)))))

  (testing "attribute names and values are validated at expansion"
    (are [f type]
      (= type
         (try
           (f)
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))
      #(modifier/attr* test-env "bad name" []) :attribute-name
      #(modifier/attr=* test-env :state "" []) :attribute-value
      #(modifier/attr=* test-env :state "bad\nvalue" []) :attribute-value)))

(deftest test-peer-modifiers
  (testing "peer uses the peer class"
    (is (= "peer" (modifier/peer* test-env))))

  (testing "peer state applies to a preceding marked sibling"
    (let [captured (atom nil)]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (reset! captured [variant forms]))]
        (modifier/peer-pseudo* test-env "empty" "empty" [:rules]))
      (is (= [{:prefix "peer-empty"
               :selector ".peer:empty ~ &"}
              [:rules]]
             @captured))))

  (testing "peer attributes retain positive and inverse sibling selectors"
    (let [captured (atom nil)]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (reset! captured [variant forms]))]
        (modifier/peer-attr=* test-env :aria-expanded true [:rules]))
      (let [[variant forms] @captured]
        (is (re-matches #"peer-attr-aria-expanded-value-[0-9a-f]{8}"
                        (:prefix variant)))
        (is (= ".peer[aria-expanded=\"true\"] ~ &"
               (:selector variant)))
        (is (= ".peer:not([aria-expanded=\"true\"]) ~ &"
               (:inverse-selector variant)))
        (is (= [:rules] forms)))))

  (testing "peer has targets descendants of the marked sibling"
    (let [captured (atom nil)]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (reset! captured [variant forms]))]
        (modifier/peer-has* test-env "input:checked" [:rules]))
      (let [[variant forms] @captured]
        (is (re-matches #"peer-has-[0-9a-f]{8}" (:prefix variant)))
        (is (= ".peer:has(input:checked) ~ &" (:selector variant)))
        (is (= [:rules] forms)))))

  (testing "indexed peer states use the shared child-index validation"
    (let [captured (atom nil)]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (reset! captured [variant forms]))]
        (modifier/peer-indexed-child-pseudo* test-env "nth" "nth-child" "2n + 1" [:rules]))
      (let [[variant forms] @captured]
        (is (re-matches #"peer-nth-[0-9a-f]{8}" (:prefix variant)))
        (is (= ".peer:nth-child(2n + 1) ~ &" (:selector variant)))
        (is (= [:rules] forms))))))

(deftest test-selector-modifier
  (testing "sel targets one or more descendant selectors"
    (doseq [[selector-form selectors]
            [["h2" ["h2"]]
             [["h1" "h2" "h3"] ["h1" "h2" "h3"]]]]
      (let [captured (atom nil)]
        (with-redefs [modifier/modifier* (fn [_env variant forms]
                                           (reset! captured [variant forms]))]
          (modifier/sel* test-env selector-form [:rules]))
        (let [[variant forms] @captured]
          (is (re-matches #"sel-[0-9a-f]{8}" (:prefix variant)))
          (is (= (str/join ", " (map #(str "& " %) selectors))
                 (:selector variant)))
          (is (= [:rules] forms))))))

  (testing "sel validates selector values"
    (doseq [selector [nil "" [] ["h1" 42] "h1, h2" "&:hover" "h1\n"]]
      (is (= :selector
             (try
               (modifier/sel* test-env selector [])
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-has-modifier
  (testing "has targets matching descendants"
    (let [captured (atom nil)]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (reset! captured [variant forms]))]
        (modifier/has* test-env " .remove-invite:hover " [:rules]))
      (let [[variant forms] @captured]
        (is (re-matches #"has-[0-9a-f]{8}" (:prefix variant)))
        (is (= "&:has(.remove-invite:hover)" (:selector variant)))
        (is (= [:rules] forms)))))

  (testing "has selectors are compile-time and structurally validated"
    (doseq [selector [nil "" ".foo, .bar" "& > .foo" ".foo\n"]]
      (is (= :has-selector
             (try
               (modifier/has* test-env selector [])
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))
    (let [form '(runtime-selector)]
      (with-redefs [ana/resolve-var (fn [_env sym] {:name sym})
                    constant/macroexpand (fn [_env expr] expr)]
        (try
          (modifier/has* test-env form [])
          (is false "expected expansion to fail")
          (catch clojure.lang.ExceptionInfo e
            (is (= form (:form (ex-data e))))
            (is (:dynamic? (ex-data e)))))))))

(deftest test-pseudo-element-modifier
  (testing "pseudo-elements accept string and keyword names"
    (doseq [[name-form pseudo-name]
            [["-webkit-slider-thumb" "-webkit-slider-thumb"]
             [:before "before"]]]
      (let [captured (atom nil)]
        (with-redefs [modifier/modifier* (fn [_env variant forms]
                                           (reset! captured [variant forms]))]
          (modifier/pseudo* test-env name-form [:rules]))
        (is (= [{:prefix (str "pseudo-" pseudo-name)
                 :selector (str "&::" pseudo-name)}
                [:rules]]
               @captured)))))

  (testing "pseudo-element names are compile-time and structurally validated"
    (doseq [name-form [nil "" :svg/before "::before" ":before" "bad name" "before:hover" 42]]
      (is (= :pseudo-element
             (try
               (modifier/pseudo* test-env name-form [])
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))
    (let [form '(runtime-pseudo)]
      (with-redefs [ana/resolve-var (fn [_env sym] {:name sym})
                    constant/macroexpand (fn [_env expr] expr)]
        (try
          (modifier/pseudo* test-env form [])
          (is false "expected expansion to fail")
          (catch clojure.lang.ExceptionInfo e
            (is (= form (:form (ex-data e))))
            (is (:dynamic? (ex-data e)))))))))

(deftest test-child-pseudo-modifiers
  (testing "fixed child pseudo-classes use the shared modifier serializer"
    (doseq [[name selector]
            [["first" "first-child"]
             ["last" "last-child"]
             ["only" "only-child"]
             ["odd" "nth-child(odd)"]
             ["even" "nth-child(even)"]
             ["empty" "empty"]
             ["first-of-type" "first-of-type"]
             ["last-of-type" "last-of-type"]
             ["only-of-type" "only-of-type"]]]
      (let [captured (atom nil)]
        (with-redefs [modifier/modifier* (fn [_env variant forms]
                                           (reset! captured [variant forms]))]
          (modifier/child-pseudo* test-env name selector [:rules]))
        (is (= [{:prefix name :selector (str "&:" selector)} [:rules]]
               @captured)))))

  (testing "indexed child pseudo-classes accept integers and nth expressions"
    (doseq [[name selector value expected-value]
            [["nth" "nth-child" 3 "3"]
             ["nth-last" "nth-last-child" "2n + 1" "2n + 1"]
             ["nth-of-type" "nth-of-type" "odd" "odd"]
             ["nth-last-of-type" "nth-last-of-type" 2 "2"]]]
      (let [captured (atom nil)]
        (with-redefs [modifier/modifier* (fn [_env variant forms]
                                           (reset! captured [variant forms]))]
          (modifier/indexed-child-pseudo* test-env name selector value [:rules]))
        (let [[variant forms] @captured]
          (is (= (str "&:" selector "(" expected-value ")") (:selector variant)))
          (is (re-matches (re-pattern (str name "-[0-9a-f]{8}")) (:prefix variant)))
          (is (= [:rules] forms))))))

  (testing "indexed child pseudo-classes validate their index"
    (doseq [value [nil 1.5 "" "2n, 1" "2n(1)" "2n; color:red"]]
      (is (= :child-index
             (try
               (modifier/indexed-child-pseudo* test-env "nth" "nth-child" value [])
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-child-modifiers
  (testing "* targets direct children"
    (binding [compiler/*variants* [{:prefix "*" :selector "& > *"}]]
      (is (= {:result "*:grow-2"
              :registered ['uni.style.fixture "*:grow-2" {:flex-grow 2}
                           ["& > *"]]}
             (expand-utility #(layout/flex-factor* test-env "grow" :flex-grow 2))))))

  (testing "** targets all descendants"
    (binding [compiler/*variants* [{:prefix "**" :selector "& *"}]]
      (is (= {:result "**:grow-2"
              :registered ['uni.style.fixture "**:grow-2" {:flex-grow 2}
                           ["& *"]]}
             (expand-utility #(layout/flex-factor* test-env "grow" :flex-grow 2)))))))

(deftest test-media-modifier
  (testing "media uses the shared nested modifier serializer"
    (binding [compiler/*variants* [{:prefix "focus-visible"
                                    :selector "&:focus-visible"}
                                   {:prefix "not-attr-disabled"
                                    :selector "&:not([disabled])"
                                    :inverse-selector "&[disabled]"}
                                   {:prefix "media-test"
                                    :selector "@media (resolution < 2dppx)"}]]
      (is (= {:result "focus-visible:not-attr-disabled:media-test:outline-offset--1"
              :registered ['uni.style.fixture
                           "focus-visible:not-attr-disabled:media-test:outline-offset--1"
                           {:outline-offset "-1px"}
                           ["&:focus-visible"
                            "&:not([disabled])"
                            "@media (resolution < 2dppx)"]]}
             (expand-utility #(border/outline-offset* test-env -1))))))

  (testing "media queries are compile-time and parenthesized"
    (is (= :media-query
           (try
             (modifier/media* test-env "screen" [])
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e))))))))

(deftest test-container-modifier
  (testing "container queries use the shared nested modifier serializer"
    (let [query "(max-width: 300px)"
          captured (atom nil)]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (reset! captured [variant forms]))]
        (modifier/container* test-env query [:rules]))
      (is (= [{:prefix (str "container-" (compiler/content-hash query))
               :selector (str "@container " query)}
              [:rules]]
             @captured))))

  (testing "container queries compose with selectors and serialize as at-rules"
    (is (= "@container (max-width: 300px){.container-test\\:hover\\:hidden:hover{display:none;}}"
           (serialize/atomic-css
             "container-test:hover:hidden"
             {:display :none}
             ["@container (max-width: 300px)" "&:hover"]
             nil))))

  (testing "container queries are compile-time, parenthesized, and source-safe"
    (doseq [query [nil "" "max-width: 300px" "(max-width: 300px) {}"
                   "(max-width: 300px);" "(max-width:\n300px)"]]
      (is (= :container-query
             (try
               (modifier/container* test-env query [])
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-supports-modifier
  (testing "supports uses the shared nested modifier serializer"
    (let [condition "(-electron-corner-smoothing: 1%)"
          captured (atom nil)]
      (with-redefs [modifier/modifier* (fn [_env variant forms]
                                         (reset! captured [variant forms]))]
        (modifier/supports* test-env condition [:rules]))
      (is (= [{:prefix (str "supports-" (compiler/content-hash condition))
               :selector (str "@supports " condition)}
              [:rules]]
             @captured))))

  (testing "supports conditions are compile-time and cannot break the at-rule"
    (doseq [condition [nil "" "(display: grid) {}" "(display: grid);"]]
      (is (= :supports-condition
             (try
               (modifier/supports* test-env condition [])
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-cascade-layer
  (testing "the predefined layers have a fixed low-to-high order"
    (let [registered (atom nil)]
      (with-redefs [compiler/register-global!
                    (fn [_env ns-sym style-name css]
                      (reset! registered [ns-sym style-name css]))]
        (layer/declare-layer-order* test-env))
      (is (= ['uni.style.fixture
              "uni-style-layer-order"
              "@layer theme,base,components,variants;"]
             @registered))))

  (testing "composed utility properties are registered without inheritance"
    (let [registered (atom nil)
          property (fn [name initial-value]
                     (str "@property " name "{syntax:\"*\";inherits:false;initial-value:"
                          initial-value ";}"))]
      (with-redefs [compiler/register-global!
                    (fn [_env ns-sym style-name css]
                      (reset! registered [ns-sym style-name css]))]
        (layer/declare-properties* test-env))
      (is (= ['uni.style.fixture
              "uni-style-properties"
              (str (property "--us-inset-shadow" "0 0 #0000")
                   (property "--us-inset-ring-shadow" "0 0 #0000")
                   (property "--us-ring-shadow" "0 0 #0000")
                   (property "--us-shadow" "0 0 #0000")
                   (property "--us-translate-x" "0")
                   (property "--us-translate-y" "0")
                   (property "--us-scale-x" "1")
                   (property "--us-scale-y" "1")
                   "@layer base{@supports ((-webkit-hyphens:none) and (not (margin-trim:inline)))"
                   " or ((-moz-orient:inline) and (not (color:rgb(from red r g b)))){"
                   "*,::before,::after,::backdrop{--us-inset-shadow:0 0 #0000;"
                   "--us-inset-ring-shadow:0 0 #0000;--us-ring-shadow:0 0 #0000;"
                   "--us-shadow:0 0 #0000;--us-translate-x:0;--us-translate-y:0;"
                   "--us-scale-x:1;--us-scale-y:1;}}}")]
             @registered))))

  (testing "layered utilities carry their predefined layer"
    (let [registered (atom nil)
          result (with-redefs [compiler/resolve-compile-time-constant
                               (fn [_env form]
                                 (if (= form '(uni.style.core/opacity 50))
                                   (color/opacity* test-env 50)
                                   form))
                               compiler/register-atomic!
                               (fn [_env ns-sym class-name declaration modifiers layer]
                                 (reset! registered
                                         [ns-sym class-name declaration modifiers layer]))]
                   (layer/layer* test-env :components ['(uni.style.core/opacity 50)]))]
      (is (= ["components:opacity-50"] result))
      (is (= ['uni.style.fixture
              "components:opacity-50"
              {:opacity "50%"}
              []
              {:name "components" :order nil}]
             @registered))))

  (testing "layer names are validated at expansion"
    (doseq [layer [-1 :one "components"]]
      (is (= :layer
             (try
               (layer/layer* test-env layer [])
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-tailwind-parity-utilities
  (testing "static utilities emit their declarations"
    (are [f class-name declaration]
      (= {:result class-name
          :registered ['uni.style.fixture class-name declaration []]}
         (expand-utility f))
      #(layout/static* test-env :float-start) "float-start" {:float :inline-start}
      #(layout/static* test-env :table-fixed) "table-fixed" {:table-layout :fixed}
      #(layout/static* test-env :grid-flow-col-dense) "grid-flow-col-dense" {:grid-auto-flow "column dense"}
      #(layout/static* test-env :isolate) "isolate" {:isolation :isolate}
      #(layout/static* test-env :break-inside-avoid) "break-inside-avoid" {:break-inside :avoid}
      #(layout/static* test-env :scheme-light-dark) "scheme-light-dark" {:color-scheme "light dark"}
      #(typography/static* test-env :decoration-wavy) "decoration-wavy" {:text-decoration-style :wavy}
      #(typography/static* test-env :list-image-none) "list-image-none" {:list-style-image :none}
      #(transform/static* test-env :origin-top-left) "origin-top-left" {:transform-origin "top left"}
      #(transform/static* test-env :transform-fill) "transform-fill" {:transform-box :fill-box}
      #(transform/static* test-env :backface-hidden) "backface-hidden" {:backface-visibility :hidden}
      #(color/static* test-env :mask-type-alpha) "mask-type-alpha" {:mask-type :alpha}))

  (testing "multi-property statics split into atomic classes"
    (is (= ["antialiased---webkit-font-smoothing antialiased---moz-osx-font-smoothing"
            "select-none---webkit-user-select select-none--user-select"]
           [(:result (expand-utility #(typography/static* test-env :antialiased)))
            (:result (expand-utility #(interaction/static* test-env :select-none)))])))

  (testing "value utilities"
    (are [f class-name declaration]
      (= {:result class-name
          :registered ['uni.style.fixture class-name declaration []]}
         (expand-utility f))
      #(transform/origin* test-env "50% 50%")
      (str "origin-value-" (compiler/content-hash "50% 50%")) {:transform-origin "50% 50%"}
      #(transform/origin* test-env :custom-origin) "origin--custom-origin"
      {:transform-origin "var(--custom-origin)"}
      #(typography/indent* test-env -2) "indent--2" {:text-indent "-2px"}
      #(layout/border-spacing* test-env 0) "border-spacing-0" {:border-spacing "0px"}
      #(color/background-value* test-env "text-shadow" :text-shadow :glow "Text shadow" :text-shadow)
      "text-shadow--glow" {:text-shadow "var(--glow)"}))

  (testing "line clamp emits a string count and the box declarations"
    (is (= ["line-clamp-2--overflow" "line-clamp-2--display"
            "line-clamp-2---webkit-box-orient" "line-clamp-2---webkit-line-clamp"]
           (mapv second (:registered (expand-utility #(layout/line-clamp* test-env 2))))))
    (is (= {:-webkit-line-clamp "2"}
           (nth (last (:registered (expand-utility #(layout/line-clamp* test-env 2)))) 2)))
    (is (= :line-clamp
           (try (layout/line-clamp* test-env 0) nil
                (catch clojure.lang.ExceptionInfo e (:type (ex-data e)))))))

  (testing "vendor-prefixed unitless properties keep plain numbers"
    (is (= "-webkit-line-clamp:2;-webkit-box-flex:1;margin:2px;"
           (serialize/declaration-css (array-map :-webkit-line-clamp 2 :-webkit-box-flex 1
                                                 :margin 2))))))

(deftest test-shadow-and-color-utilities
  (testing "backdrop blur accepts tokens and CSS literals, with the -webkit- prefix"
    (are [f class-name value]
      (= {:result (str class-name "---webkit-backdrop-filter " class-name "--backdrop-filter")
          :registered [['uni.style.fixture (str class-name "---webkit-backdrop-filter")
                        {:-webkit-backdrop-filter value} []]
                       ['uni.style.fixture (str class-name "--backdrop-filter")
                        {:backdrop-filter value} []]]}
         (expand-utility f))
      #(color/static* test-env :backdrop-blur-none) "backdrop-blur-none" ""
      #(color/backdrop-blur* test-env :app-backdrop-blur) "backdrop-blur--app-backdrop-blur"
      "blur(var(--app-backdrop-blur))"
      #(color/backdrop-blur* test-env "4px")
      (str "backdrop-blur-value-" (compiler/content-hash "4px")) "blur(4px)"))

  (testing "filters accept tokens and CSS literals"
    (is (= {:result "filter--app-shadow-filter"
            :registered ['uni.style.fixture "filter--app-shadow-filter"
                         {:filter "var(--app-shadow-filter)"} []]}
           (expand-utility #(color/filter* test-env :app-shadow-filter))))
    (let [value "drop-shadow(0 2px 4px rgb(0 0 0 / 20%))"
          class-name (str "filter-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:filter value} []]}
             (expand-utility #(color/filter* test-env value))))))

  (testing "token-backed values become CSS variable utilities"
    (is (= (shadow-layer-expansion "shadow--app-shadow-subtle" :--us-shadow
                                   "var(--app-shadow-subtle)")
           (expand-utility #(color/shadow* test-env :app-shadow-subtle))))
    (is (= {:result "color--app-fg-base"
            :registered ['uni.style.fixture "color--app-fg-base"
                         {:color "var(--app-fg-base)"} []]}
           (expand-utility #(color/color* test-env :app-fg-base))))
    (is (= {:result "caret--app-fg-base"
            :registered ['uni.style.fixture "caret--app-fg-base"
                         {:caret-color "var(--app-fg-base)"} []]}
           (expand-utility #(color/caret* test-env :app-fg-base)))))

  (testing "string shadows remain literal"
    (let [value "2px 2px 4px rgba(0, 0, 0, 0.12)"
          class-name (str "shadow-" (compiler/content-hash value))]
      (is (= (shadow-layer-expansion class-name :--us-shadow value)
             (expand-utility #(color/shadow* test-env value))))))

  (testing "variable references set the shadow layer"
    (is (= (shadow-layer-expansion "shadow--custom-shadow" :--us-shadow
                                   "var(--custom-shadow)")
           (expand-utility #(color/shadow* test-env "var(--custom-shadow)")))))

  (testing "inset shadows add the inset keyword to each literal shadow"
    (is (= (shadow-layer-expansion "inset-shadow--app-shadow-inner" :--us-inset-shadow
                                   "inset var(--app-shadow-inner)")
           (expand-utility #(color/inset-shadow* test-env :app-shadow-inner))))
    (let [value "0 1px 2px rgb(0 0 0 / 5%), 0 0 1px 0 var(--inset-color) inset"
          class-name (str "inset-shadow-" (compiler/content-hash value))]
      (is (= (shadow-layer-expansion class-name :--us-inset-shadow
                                     (str "inset 0 1px 2px rgb(0 0 0 / 5%), "
                                          "0 0 1px 0 var(--inset-color) inset"))
             (expand-utility #(color/inset-shadow* test-env value))))))

  (testing "rings default to 1px and accept width, token, and fallback colors"
    (let [class-name (str "ring-1-color-" (compiler/content-hash "red"))]
      (is (= (shadow-layer-expansion class-name :--us-ring-shadow "0 0 0 1px red")
             (expand-utility #(color/ring* test-env :ring ["red"])))))
    (is (= (shadow-layer-expansion "ring-2--app-border-focus" :--us-ring-shadow
                                   "0 0 0 2px var(--app-border-focus)")
           (expand-utility #(color/ring* test-env :ring [2 :app-border-focus]))))
    (is (= (shadow-layer-expansion "ring-2--app-border-focus--app-border-base"
                                   :--us-ring-shadow
                                   "0 0 0 2px var(--app-border-focus, var(--app-border-base))")
           (expand-utility #(color/ring* test-env :ring
                                         [2 :app-border-focus :app-border-base])))))

  (testing "inset rings set their own layer"
    (let [class-name (str "inset-ring-1-color-" (compiler/content-hash "red"))]
      (is (= (shadow-layer-expansion class-name :--us-inset-ring-shadow
                                     "inset 0 0 0 1px red")
             (expand-utility #(color/ring* test-env :inset-ring ["red"])))))
    (is (= (shadow-layer-expansion "inset-ring-2--something" :--us-inset-ring-shadow
                                   "inset 0 0 0 2px var(--something)")
           (expand-utility #(color/ring* test-env :inset-ring [2 :something])))))

  (testing "animation frames compose a direct box-shadow from the frame's layers"
    (let [collector (atom {:declarations [] :registrations 0 :layer compiler/unset-layer})]
      (binding [compiler/*keyframes?* true
                compiler/*class-declaration-collector* collector]
        (color/shadow* test-env "0 2px 4px black")
        (color/ring* test-env :ring [2 "red"]))
      (is (= {:--us-shadow "0 2px 4px black"
              :--us-ring-shadow "0 0 0 2px red"
              :box-shadow (str "var(--us-inset-shadow, 0 0 #0000), var(--us-inset-ring-shadow, 0 0 #0000), "
                               "0 0 0 2px red, 0 2px 4px black")}
             (:declaration (first (:declarations @collector)))))))

  (testing "none utilities clear only their own layer"
    (are [layer class-name property]
      (= (shadow-layer-expansion class-name property "0 0 #0000")
         (expand-utility #(color/shadow-layer-none* test-env layer)))
      :shadow "shadow-none" :--us-shadow
      :inset-shadow "inset-shadow-none" :--us-inset-shadow
      :ring "ring-none" :--us-ring-shadow
      :inset-ring "inset-ring-none" :--us-inset-ring-shadow))

  (testing "invalid shadow layer values fail during expansion"
    (are [f type]
      (= type
         (try
           (f)
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))
      #(color/shadow* test-env "") :box-shadow
      #(color/inset-shadow* test-env 1) :inset-shadow
      #(color/ring* test-env :ring [-1 "red"]) :ring-width
      #(color/ring* test-env :ring [2 1]) :ring-color))

  (testing "string colors remain literal"
    (let [value "var(--custom-fg)"
          class-name (str "color-color-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:color value} []]}
             (expand-utility #(color/color* test-env value)))))
    (let [value "red"
          class-name (str "color-color-" (compiler/content-hash value))]
      (is (= class-name
             (:result (expand-utility #(color/color* test-env value)))))))

  (testing "fallbacks accept token variables or literal strings"
    (is (= {:result "color--app-fg-base--app-fg-fallback"
            :registered ['uni.style.fixture "color--app-fg-base--app-fg-fallback"
                         {:color "var(--app-fg-base, var(--app-fg-fallback))"}
                         []]}
           (expand-utility #(color/color* test-env :app-fg-base :app-fg-fallback))))
    (let [fallback "#fff"
          class-name (str "color--app-fg-base-color-"
                          (compiler/content-hash fallback))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name
                           {:color "var(--app-fg-base, #fff)"} []]}
             (expand-utility #(color/color* test-env :app-fg-base fallback))))))

  (testing "static text color utilities"
    (are [class-name declaration]
      (= class-name
         (:result (expand-utility #(compiler/simple-utility* test-env class-name declaration))))
      "color-transparent" {:color :transparent}
      "color-current" {:color "currentColor"}
      "color-inherit" {:color :inherit}
      "caret-inherit" {:caret-color :inherit}
      "caret-current" {:caret-color "currentColor"}
      "caret-transparent" {:caret-color :transparent}))

  (testing "invalid values fail during expansion"
    (are [f type]
      (= type
         (try
           (expand-utility f)
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))
      #(color/backdrop-blur* test-env "") :backdrop-filter
      #(color/backdrop-blur* test-env 42) :backdrop-filter
      #(color/shadow* test-env "") :box-shadow
      #(color/shadow* test-env 42) :box-shadow
      #(color/color* test-env 42) :color
      #(color/caret* test-env 42) :caret-color
      #(color/caret* test-env "") :caret-color
      #(color/color* test-env "red" :app-fg-fallback) :color
      #(color/color* test-env :app-fg-base 42) :color)))

(deftest test-svg-color-utilities
  (testing "fill and stroke preserve token-backed SVG colors"
    (is (= {:result "fill--app-handle-bg"
            :registered ['uni.style.fixture "fill--app-handle-bg"
                         {:fill "var(--app-handle-bg)"} []]}
           (expand-utility #(color/fill* test-env :app-handle-bg))))
    (is (= {:result "stroke--app-handle-border--app-handle-fallback"
            :registered ['uni.style.fixture
                         "stroke--app-handle-border--app-handle-fallback"
                         {:stroke "var(--app-handle-border, var(--app-handle-fallback))"}
                         []]}
           (expand-utility #(color/stroke* test-env
                                           :app-handle-border
                                           :app-handle-fallback)))))

  (testing "fill and stroke accept literal CSS colors and color opacity"
    (let [fill "none"
          class-name (str "fill-color-" (compiler/content-hash fill))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:fill fill} []]}
             (expand-utility #(color/fill* test-env fill)))))
    (is (= "stroke--app-handle-border/40"
           (:result (expand-utility #(color/stroke* test-env
                                                    (color/alpha* test-env
                                                                  :app-handle-border
                                                                  40)))))))

  (testing "invalid SVG colors fail during expansion"
    (are [f type]
      (= type
         (try
           (expand-utility f)
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))
      #(color/fill* test-env 42) :fill
      #(color/stroke* test-env :app-handle-border 42) :stroke)))

(deftest test-class-declaration-collection
  (let [collector (atom {:declarations []
                         :registrations 0})]
    (binding [compiler/*class-declaration-collector* collector]
      (typography/font* test-env "Inter, sans-serif")
      (compiler/simple-utility* test-env "font-normal" {:font-weight 400})
      (compiler/simple-utility* test-env "font-medium" {:font-weight 500})
      (typography/line-height* test-env 1.2)
      (border/border* test-env "border" "border" [2 "red"]))
    (is (= [{:modifiers []
             :declaration {:font-family "Inter, sans-serif"
                           :font-weight 500
                           :line-height 1.2
                           :border-color "red"
                           :border-width 2
                           :border-style :solid}}]
           (:declarations @collector)))
    (is (= 7 (:registrations @collector)))))

(deftest test-typography-utilities
  (testing "font sizes accept pixel numbers and literal CSS strings"
    (is (= {:result "font-size-16"
            :registered ['uni.style.fixture "font-size-16" {:font-size "16px"} []]}
           (expand-utility #(typography/font-size* test-env 16))))
    (let [size "clamp(1rem, 2vw, 2rem)"
          class-name (str "font-size-value-" (compiler/content-hash size))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:font-size size} []]}
             (expand-utility #(typography/font-size* test-env size))))))

  (testing "text combines a font size with a literal line height"
    (is (= {:result "text-16-line-height-1_5--font-size text-16-line-height-1_5--line-height"
            :registered [['uni.style.fixture "text-16-line-height-1_5--font-size"
                          {:font-size "16px"} []]
                         ['uni.style.fixture "text-16-line-height-1_5--line-height"
                          {:line-height 1.5} []]]}
           (expand-utility #(typography/text* test-env 16 1.5)))))

  (testing "line-height and font-family values support literals and tokens"
    (is (= {:result "line-height-1_5"
            :registered ['uni.style.fixture "line-height-1_5" {:line-height 1.5} []]}
           (expand-utility #(typography/line-height* test-env 1.5))))
    (let [family "Inter, sans-serif"
          class-name (str "font-value-" (compiler/content-hash family))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:font-family family} []]}
             (expand-utility #(typography/font* test-env family)))))
    (is (= {:result "font--interface-font"
            :registered ['uni.style.fixture "font--interface-font"
                         {:font-family "var(--interface-font)"} []]}
           (expand-utility #(typography/font* test-env :interface-font)))))

  (testing "text decoration colors"
    (are [utility class-name color]
      (= {:result class-name
          :registered ['uni.style.fixture class-name {:text-decoration-color color} []]}
         (expand-utility #(color/static* test-env utility)))
      :decoration-inherit "decoration-inherit" :inherit
      :decoration-current "decoration-current" "currentColor"
      :decoration-transparent "decoration-transparent" :transparent)
    (is (= {:result "decoration--decoration-color"
            :registered ['uni.style.fixture "decoration--decoration-color"
                         {:text-decoration-color "var(--decoration-color)"} []]}
           (expand-utility #(color/decoration* test-env :decoration-color))))
    (is (= {:result "decoration--decoration-color/40"
            :registered ['uni.style.fixture "decoration--decoration-color/40"
                         {:text-decoration-color
                          "color-mix(in oklab, var(--decoration-color) 40%, transparent)"}
                         []]}
           (expand-utility #(color/decoration* test-env
                                               (color/alpha* test-env
                                                             :decoration-color
                                                             40)))))
    (let [decoration-color "oklch(50% 0.2 10)"
          class-name (str "decoration-color-" (compiler/content-hash decoration-color))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name
                           {:text-decoration-color decoration-color} []]}
             (expand-utility #(color/decoration* test-env decoration-color))))))

  (testing "letter spacing and text-decoration lengths"
    (is (= {:result "tracking--0_5"
            :registered ['uni.style.fixture "tracking--0_5"
                         {:letter-spacing "-0.5px"} []]}
           (expand-utility #(typography/tracking* test-env -0.5))))
    (is (= {:result "decoration-thickness-1"
            :registered ['uni.style.fixture "decoration-thickness-1"
                         {:text-decoration-thickness "1px"} []]}
           (expand-utility #(typography/decoration-thickness* test-env 1))))
    (is (= {:result "underline-offset-3"
            :registered ['uni.style.fixture "underline-offset-3"
                         {:text-underline-offset "3px"} []]}
           (expand-utility #(typography/underline-offset* test-env 3)))))

  (testing "static inheritance and text-decoration utilities"
    (are [utility class-name declaration]
      (= {:result class-name
          :registered ['uni.style.fixture class-name declaration []]}
         (expand-utility #(typography/static* test-env utility)))
      :font-size-inherit "font-size-inherit" {:font-size :inherit}
      :line-height-none "line-height-none" {:line-height 1}
      :line-height-inherit "line-height-inherit" {:line-height :inherit}
      :font-weight-inherit "font-weight-inherit" {:font-weight :inherit}
      :text-wrap "text-wrap" {:text-wrap :wrap}
      :text-nowrap "text-nowrap" {:text-wrap :nowrap}
      :text-balance "text-balance" {:text-wrap :balance}
      :text-pretty "text-pretty" {:text-wrap :pretty}
      :word-break-normal "word-break-normal" {:word-break :normal}
      :word-break-word "word-break-word" {:word-break :break-word}
      :word-break-all "word-break-all" {:word-break :break-all}
      :word-break-keep "word-break-keep" {:word-break :keep-all}
      :overflow-wrap-normal "overflow-wrap-normal" {:overflow-wrap :normal}
      :overflow-wrap-break-word "overflow-wrap-break-word" {:overflow-wrap :break-word}
      :overflow-wrap-anywhere "overflow-wrap-anywhere" {:overflow-wrap :anywhere}
      :underline "underline" {:text-decoration-line :underline}
      :overline "overline" {:text-decoration-line :overline}
      :line-through "line-through" {:text-decoration-line :line-through}
      :no-underline "no-underline" {:text-decoration-line :none}))

  (testing "invalid typography values fail during expansion"
    (are [f type]
      (= type
         (try
           (expand-utility f)
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))
      #(typography/font-size* test-env -1) :font-size
      #(typography/text* test-env 16 "") :line-height
      #(typography/line-height* test-env "") :line-height
      #(typography/font* test-env "") :font-family
      #(color/decoration* test-env 42) :text-decoration-color
      #(color/decoration* test-env "") :text-decoration-color
      #(typography/tracking* test-env :theme/wide) :letter-spacing
      #(typography/decoration-thickness* test-env -1) :text-decoration-thickness
      #(typography/underline-offset* test-env :theme/auto) :text-underline-offset)))

(deftest test-static-layout-utilities
  (are [utility class-name declaration]
    (= {:result class-name
        :registered ['uni.style.fixture class-name declaration []]}
       (expand-utility #(layout/static* test-env utility)))
    :items-inherit "items-inherit" {:align-items :inherit}
    :container-normal "container-normal" {:container-type :normal}
    :container-size "container-size" {:container-type :size}
    :container-inline-size "container-inline-size" {:container-type :inline-size}
    :container-scroll-state "container-scroll-state" {:container-type :scroll-state}
    :container-anchored "container-anchored" {:container-type :anchored}
    :border-collapse "border-collapse" {:border-collapse :collapse}
    :border-separate "border-separate" {:border-collapse :separate}
    :content-none "content-none" {:content :none})

  (testing "generated content quotes and escapes string literals"
    (doseq [[content-value expected]
            [["" "''"]
             ["New" "'New'"]
             ["It's\\here\nnow" "'It\\'s\\\\here\\A now'"]]]
      (let [class-name (str "content-value-" (compiler/content-hash content-value))]
        (is (= {:result class-name
                :registered ['uni.style.fixture class-name {:content expected} []]}
               (expand-utility #(layout/content* test-env content-value)))))))

  (testing "generated content supports tokens"
    (is (= {:result "content--generated-content"
            :registered ['uni.style.fixture "content--generated-content"
                         {:content "var(--generated-content)"} []]}
           (expand-utility #(layout/content* test-env :generated-content)))))

  (testing "invalid generated content values fail during expansion"
    (doseq [value [:theme/content 42 nil]]
      (is (= :content
             (try
               (expand-utility #(layout/content* test-env value))
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e))))))))

  (testing "arbitrary align-items values support literals and tokens"
    (is (= {:result "items--custom-items"
            :registered ['uni.style.fixture "items--custom-items"
                         {:align-items "var(--custom-items)"} []]}
           (expand-utility #(layout/items* test-env :custom-items))))
    (is (= :important
           (try
             (expand-utility #(layout/items* test-env "center !important"))
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e)))))))

  (testing "invalid align-items values fail during expansion"
    (doseq [value ["" :theme/items 42]]
      (is (= :align-items
             (try
               (expand-utility #(layout/items* test-env value))
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-aspect-ratio-utility
  (let [value "16 / 9"
        class-name (str "aspect-ratio-value-" (compiler/content-hash value))]
    (is (= {:result class-name
            :registered ['uni.style.fixture class-name {:aspect-ratio value} []]}
           (expand-utility #(layout/aspect-ratio* test-env value)))))
  (let [value "auto"
        class-name (str "aspect-ratio-value-" (compiler/content-hash value))]
    (is (= {:result class-name
            :registered ['uni.style.fixture class-name {:aspect-ratio value} []]}
           (expand-utility #(layout/aspect-ratio* test-env value)))))
  (is (= {:result "aspect-ratio--aspect"
          :registered ['uni.style.fixture "aspect-ratio--aspect"
                       {:aspect-ratio "var(--aspect)"} []]}
         (expand-utility #(layout/aspect-ratio* test-env :aspect))))
  (is (= :aspect-ratio
         (try
           (expand-utility #(layout/aspect-ratio* test-env nil))
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))))

(deftest test-grid-utilities
  (testing "grid template, automatic track, and placement values stay literal"
    (doseq [[prefix property value expected-class]
            [["grid-cols" :grid-template-columns "1fr 1fr"
              (str "grid-cols-value-" (compiler/content-hash "1fr 1fr"))]
             ["grid-rows" :grid-template-rows "none"
              (str "grid-rows-value-" (compiler/content-hash "none"))]
             ["grid-auto-cols" :grid-auto-columns "max-content"
              (str "grid-auto-cols-value-" (compiler/content-hash "max-content"))]
             ["grid-auto-rows" :grid-auto-rows "min-content"
              (str "grid-auto-rows-value-" (compiler/content-hash "min-content"))]
             ["grid-col" :grid-column "1 / -1"
              (str "grid-col-value-" (compiler/content-hash "1 / -1"))]
             ["grid-col-start" :grid-column-start 2 "grid-col-start-2"]
             ["grid-col-end" :grid-column-end -1 "grid-col-end--1"]
             ["grid-row" :grid-row "2 / 4"
              (str "grid-row-value-" (compiler/content-hash "2 / 4"))]
             ["grid-row-start" :grid-row-start 1 "grid-row-start-1"]
             ["grid-row-end" :grid-row-end 3 "grid-row-end-3"]
             ["grid-area" :grid-area "1 / 2 / 3 / 4"
              (str "grid-area-value-" (compiler/content-hash "1 / 2 / 3 / 4"))]]]
      (is (= {:result expected-class
              :registered ['uni.style.fixture expected-class {property value} []]}
             (expand-utility #(layout/grid-value* test-env prefix property value))))))

  (testing "grid utilities compose with modifiers"
    (binding [compiler/*variants* [{:prefix "container-test"
                                    :selector "@container (max-width: 300px)"}]]
      (let [value "1fr"
            class-name (str "container-test:grid-cols-value-"
                            (compiler/content-hash value))]
        (is (= {:result class-name
                :registered ['uni.style.fixture class-name
                             {:grid-template-columns value}
                             ["@container (max-width: 300px)"]]}
               (expand-utility #(layout/grid-value* test-env "grid-cols"
                                                    :grid-template-columns
                                                    value)))))))

  (testing "grid values are compile-time CSS literals"
    (doseq [value [nil true ""]]
      (is (= :grid-value
             (try
               (layout/grid-value* test-env "grid-cols" :grid-template-columns value)
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-dimension-utilities
  (testing "numeric dimensions have readable classes and pixel declarations"
    (is (= {:result "w-12"
            :registered ['uni.style.fixture "w-12" {:width 12} []]}
           (expand-utility #(spacing/dimension* test-env "w" [:width] 12))))
    (is (= {:result "size-1_5--width size-1_5--height"
            :registered [['uni.style.fixture "size-1_5--width" {:width 1.5} []]
                         ['uni.style.fixture "size-1_5--height" {:height 1.5} []]]}
           (expand-utility #(spacing/dimension* test-env "size" [:width :height] 1.5)))))

  (testing "literal CSS dimensions have hashed classes"
    (let [value "calc(100% - 20px)"
          class-name (str "max-h-value-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:max-height value} []]}
             (expand-utility #(spacing/dimension* test-env "max-h" [:max-height] value))))))

  (testing "dimensions compose with modifiers"
    (binding [compiler/*variants* [{:prefix "hover" :selector "&:hover"}]]
      (is (= {:result "hover:min-w-4"
              :registered ['uni.style.fixture "hover:min-w-4" {:min-width 4}
                           ["&:hover"]]}
             (expand-utility #(spacing/dimension* test-env "min-w" [:min-width] 4))))))

  (testing "dimensions support token keywords"
    (is (= {:result "w--panel-width"
            :registered ['uni.style.fixture "w--panel-width"
                         {:width "var(--panel-width)"} []]}
           (expand-utility #(spacing/dimension* test-env "w" [:width] :panel-width)))))

  (testing "invalid dimensions fail during expansion"
    (are [value] (= :dimension
                    (try
                      (expand-utility #(spacing/dimension* test-env "w" [:width] value))
                      nil
                      (catch clojure.lang.ExceptionInfo e
                        (:type (ex-data e)))))
      -1
      ""
      :theme/width)))

(deftest test-flex-factor-utilities
  (testing "flex factors have readable classes and unitless declarations"
    (is (= {:result "grow-2"
            :registered ['uni.style.fixture "grow-2" {:flex-grow 2} []]}
           (expand-utility #(layout/flex-factor* test-env "grow" :flex-grow 2))))
    (is (= {:result "shrink-1_5"
            :registered ['uni.style.fixture "shrink-1_5" {:flex-shrink 1.5} []]}
           (expand-utility #(layout/flex-factor* test-env "shrink" :flex-shrink 1.5)))))

  (testing "flex factors compose with modifiers"
    (binding [compiler/*variants* [{:prefix "hover" :selector "&:hover"}]]
      (is (= {:result "hover:grow-2"
              :registered ['uni.style.fixture "hover:grow-2" {:flex-grow 2}
                           ["&:hover"]]}
             (expand-utility #(layout/flex-factor* test-env "grow" :flex-grow 2))))))

  (testing "flex factors support strings and token keywords"
    (is (re-matches #"grow-value-[0-9a-f]{8}"
                    (:result (expand-utility #(layout/flex-factor* test-env "grow" :flex-grow "1.5")))))
    (is (= {:result "grow--factor"
            :registered ['uni.style.fixture "grow--factor" {:flex-grow "var(--factor)"} []]}
           (expand-utility #(layout/flex-factor* test-env "grow" :flex-grow :factor)))))

  (testing "invalid flex factors fail during expansion"
    (are [value] (= :flex-factor
                    (try
                      (expand-utility #(layout/flex-factor* test-env "grow" :flex-grow value))
                      nil
                      (catch clojure.lang.ExceptionInfo e
                        (:type (ex-data e)))))
      -1
      ""
      :theme/factor)))

(deftest test-integer-utilities
  (testing "integer utilities retain signed unitless values"
    (is (= {:result "z--1"
            :registered ['uni.style.fixture "z--1" {:z-index -1} []]}
           (expand-utility #(compiler/integer-utility* test-env "z" :z-index -1
                                                       "Z-index" :z-index))))
    (is (= {:result "order-3"
            :registered ['uni.style.fixture "order-3" {:order 3} []]}
           (expand-utility #(compiler/integer-utility* test-env "order" :order 3
                                                       "Order" :order)))))

  (testing "integer properties support strings and token keywords"
    (is (= {:result "z--layer"
            :registered ['uni.style.fixture "z--layer" {:z-index "var(--layer)"} []]}
           (expand-utility #(compiler/integer-utility* test-env "z" :z-index :layer
                                                       "Z-index" :z-index))))
    (is (re-matches #"order-value-[0-9a-f]{8}"
                    (:result (expand-utility #(compiler/integer-utility* test-env "order" :order
                                                                         "calc(1 + 2)"
                                                                         "Order" :order))))))

  (testing "invalid values fail during expansion"
    (is (= :z-index
           (try
             (expand-utility #(compiler/integer-utility* test-env "z" :z-index 1.5
                                                         "Z-index" :z-index))
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e))))))))

(deftest test-interaction-value-utilities
  (testing "will-change presets and arbitrary values"
    (are [utility class-name value]
      (= {:result class-name
          :registered ['uni.style.fixture class-name {:will-change value} []]}
         (expand-utility #(interaction/static* test-env utility)))
      :will-change-auto "will-change-auto" :auto
      :will-change-scroll "will-change-scroll" :scroll-position
      :will-change-contents "will-change-contents" :contents
      :will-change-transform "will-change-transform" :transform)
    (is (= {:result "will-change--performance-hint"
            :registered ['uni.style.fixture "will-change--performance-hint"
                         {:will-change "var(--performance-hint)"} []]}
           (expand-utility #(interaction/will-change* test-env :performance-hint))))
    (let [value "opacity, transform"
          class-name (str "will-change-value-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:will-change value} []]}
             (expand-utility #(interaction/will-change* test-env value))))))

  (testing "touch-action presets"
    (doseq [touch-value [:auto :none :pan-x :pan-left :pan-right :pan-y :pan-up
                         :pan-down :pinch-zoom :manipulation]]
      (let [class-name (str "touch-" (name touch-value))]
        (is (= {:result class-name
                :registered ['uni.style.fixture class-name {:touch-action touch-value} []]}
               (expand-utility #(interaction/static* test-env (keyword class-name))))))))

  (testing "invalid will-change values fail during expansion"
    (doseq [value ["" :theme/change 42]]
      (is (= :will-change
             (try
               (expand-utility #(interaction/will-change* test-env value))
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e)))))))))

(deftest test-cursor-utilities
  (testing "static cursor utilities"
    (doseq [cursor-value [:auto :default :pointer :wait :text :move :help :not-allowed
                          :none :context-menu :progress :cell :crosshair :vertical-text
                          :alias :copy :no-drop :grab :grabbing :all-scroll :col-resize
                          :row-resize :n-resize :e-resize :s-resize :w-resize :ne-resize
                          :nw-resize :se-resize :sw-resize :ew-resize :ns-resize
                          :nesw-resize :nwse-resize :zoom-in :zoom-out]]
      (let [class-name (str "cursor-" (name cursor-value))]
        (is (= {:result class-name
                :registered ['uni.style.fixture class-name {:cursor cursor-value} []]}
               (expand-utility #(interaction/static* test-env (keyword class-name))))))))

  (testing "arbitrary cursor values support literals and tokens"
    (is (= {:result "cursor--custom-cursor"
            :registered ['uni.style.fixture "cursor--custom-cursor"
                         {:cursor "var(--custom-cursor)"} []]}
           (expand-utility #(interaction/cursor* test-env :custom-cursor))))
    (let [cursor-value "url('/cursor.svg') 4 4, pointer"
          class-name (str "cursor-value-" (compiler/content-hash cursor-value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:cursor cursor-value} []]}
             (expand-utility #(interaction/cursor* test-env cursor-value))))))

  (testing "invalid cursor values fail during expansion"
    (are [value] (= :cursor
                    (try
                      (expand-utility #(interaction/cursor* test-env value))
                      nil
                      (catch clojure.lang.ExceptionInfo e
                        (:type (ex-data e)))))
      ""
      :theme/cursor
      42)))

(deftest test-opacity-utility
  (testing "numeric opacity values become percentages"
    (is (= {:result "opacity-50"
            :registered ['uni.style.fixture "opacity-50" {:opacity "50%"} []]}
           (expand-utility #(color/opacity* test-env 50)))))

  (testing "opacity supports CSS strings and token keywords"
    (is (re-matches #"opacity-value-[0-9a-f]{8}"
                    (:result (expand-utility #(color/opacity* test-env "0.45")))))
    (is (= {:result "opacity--disabled-opacity"
            :registered ['uni.style.fixture "opacity--disabled-opacity"
                         {:opacity "var(--disabled-opacity)"} []]}
           (expand-utility #(color/opacity* test-env :disabled-opacity)))))

  (testing "invalid opacity values fail during expansion"
    (are [value] (= :opacity
                    (try
                      (expand-utility #(color/opacity* test-env value))
                      nil
                      (catch clojure.lang.ExceptionInfo e
                        (:type (ex-data e)))))
      -1
      101
      10.5)))

(deftest test-color-alpha
  (testing "color alpha resolves compile-time token and literal colors"
    (is (= {:uni.style.core/opacity-color true
            :color :col
            :opacity 10}
           (color/alpha* test-env :col 10)))
    (is (= {:result "bg--col/10"
            :registered ['uni.style.fixture "bg--col/10"
                         {:background-color
                          "color-mix(in oklab, var(--col) 10%, transparent)"}
                         []]}
           (expand-utility #(color/bg* test-env (color/alpha* test-env :col 10)))))
    (let [color "oklch(50% 0.2 10)"
          class-name (str "color-color-" (compiler/content-hash color) "/25")]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name
                           {:color
                            "color-mix(in oklab, oklch(50% 0.2 10) 25%, transparent)"}
                           []]}
             (expand-utility #(color/color* test-env
                                            (color/alpha* test-env color 25)))))))

  (testing "color alpha works in combined border and outline utilities"
    (is (= "border--col/40 border-1 border-style"
           (:result (expand-border "border" "border"
                                   [(color/alpha* test-env :col 40)]))))
    (is (= "outline--col/60 outline-2 outline-style"
           (:result (expand-utility #(border/outline* test-env
                                                      [2 (color/alpha* test-env :col 60)]))))))

  (testing "alpha must be an integer percentage"
    (are [color opacity type]
      (= type
         (try
           (color/alpha* test-env color opacity)
           nil
           (catch clojure.lang.ExceptionInfo e
             (:type (ex-data e)))))
      :col -1 :alpha
      :col 101 :alpha
      :col 10.5 :alpha
      42 10 :color)))

(deftest test-gap-utilities
  (testing "numeric gap values become pixel declarations"
    (are [prefix property]
      (= {:result (str prefix "-12")
          :registered ['uni.style.fixture (str prefix "-12") {property "12px"} []]}
         (expand-utility #(spacing/gap* test-env prefix property 12)))
      "gap" :gap
      "gap-x" :column-gap
      "gap-y" :row-gap))

  (testing "literal CSS gap values have hashed classes"
    (let [value "clamp(8px, 2vw, 16px)"
          class-name (str "gap-value-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:gap value} []]}
             (expand-utility #(spacing/gap* test-env "gap" :gap value))))))

  (testing "gap values support token keywords"
    (is (= {:result "gap--space"
            :registered ['uni.style.fixture "gap--space" {:gap "var(--space)"} []]}
           (expand-utility #(spacing/gap* test-env "gap" :gap :space)))))

  (testing "invalid gap values fail during expansion"
    (are [value] (= :gap
                    (try
                      (expand-utility #(spacing/gap* test-env "gap" :gap value))
                      nil
                      (catch clojure.lang.ExceptionInfo e
                        (:type (ex-data e)))))
      -1
      ""
      :theme/normal)))

(deftest test-overscroll-and-scroll-snap-utilities
  (doseq [[utility class-name declaration]
          [[:overscroll-auto "overscroll-auto" {:overscroll-behavior :auto}]
           [:overscroll-contain "overscroll-contain" {:overscroll-behavior :contain}]
           [:overscroll-none "overscroll-none" {:overscroll-behavior :none}]
           [:overscroll-x-auto "overscroll-x-auto" {:overscroll-behavior-x :auto}]
           [:overscroll-x-contain "overscroll-x-contain" {:overscroll-behavior-x :contain}]
           [:overscroll-x-none "overscroll-x-none" {:overscroll-behavior-x :none}]
           [:overscroll-y-auto "overscroll-y-auto" {:overscroll-behavior-y :auto}]
           [:overscroll-y-contain "overscroll-y-contain" {:overscroll-behavior-y :contain}]
           [:overscroll-y-none "overscroll-y-none" {:overscroll-behavior-y :none}]
           [:snap-none "snap-none" {:scroll-snap-type :none}]
           [:snap-x "snap-x" {:scroll-snap-type "x var(--us-scroll-snap-strictness, proximity)"}]
           [:snap-y "snap-y" {:scroll-snap-type "y var(--us-scroll-snap-strictness, proximity)"}]
           [:snap-both "snap-both" {:scroll-snap-type "both var(--us-scroll-snap-strictness, proximity)"}]
           [:snap-mandatory "snap-mandatory" {:--us-scroll-snap-strictness :mandatory}]
           [:snap-proximity "snap-proximity" {:--us-scroll-snap-strictness :proximity}]
           [:snap-start "snap-start" {:scroll-snap-align :start}]
           [:snap-end "snap-end" {:scroll-snap-align :end}]
           [:snap-center "snap-center" {:scroll-snap-align :center}]
           [:snap-align-none "snap-align-none" {:scroll-snap-align :none}]]]
    (is (= {:result class-name
            :registered ['uni.style.fixture class-name declaration []]}
           (expand-utility #(position/static* test-env utility))))))

(deftest test-clip-path-utility
  (testing "literal CSS clip paths have hashed classes"
    (let [value "inset(50%)"
          class-name (str "clip-path-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:clip-path value} []]}
             (expand-utility #(position/clip-path* test-env value))))))

  (testing "clip paths support token keywords"
    (is (= {:result "clip-path--clip"
            :registered ['uni.style.fixture "clip-path--clip"
                         {:clip-path "var(--clip)"} []]}
           (expand-utility #(position/clip-path* test-env :clip)))))

  (testing "invalid clip paths fail during expansion"
    (are [value] (= :clip-path
                    (try
                      (expand-utility #(position/clip-path* test-env value))
                      nil
                      (catch clojure.lang.ExceptionInfo e
                        (:type (ex-data e)))))
      ""
      "  "
      :theme/clip
      42)))

(deftest test-inset-utilities
  (testing "numeric inset values become signed pixel declarations"
    (are [prefix property]
      (= {:result (str prefix "--12")
          :registered ['uni.style.fixture (str prefix "--12") {property "-12px"} []]}
         (expand-utility #(position/inset* test-env prefix property -12)))
      "inset" :inset
      "inset-x" :inset-inline
      "inset-y" :inset-block
      "top" :top
      "right" :right
      "bottom" :bottom
      "left" :left))

  (testing "literal CSS inset values have hashed classes"
    (let [value "calc(50% - 8px)"
          class-name (str "top-value-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name {:top value} []]}
             (expand-utility #(position/inset* test-env "top" :top value))))))

  (testing "invalid inset values fail during expansion"
    (are [value] (= :inset
                    (try
                      (expand-utility #(position/inset* test-env "inset" :inset value))
                      nil
                      (catch clojure.lang.ExceptionInfo e
                        (:type (ex-data e)))))
      ""
      :theme/auto)))

(deftest test-scroll-margin-utilities
  (testing "numeric scroll margin values become signed pixel declarations"
    (are [prefix property]
      (= {:result (str prefix "-100")
          :registered ['uni.style.fixture (str prefix "-100")
                       {property "100px"} []]}
         (expand-utility #(spacing/scroll-margin* test-env prefix property 100)))
      "scroll-m" :scroll-margin
      "scroll-mx" :scroll-margin-inline
      "scroll-my" :scroll-margin-block
      "scroll-mt" :scroll-margin-top
      "scroll-mr" :scroll-margin-right
      "scroll-mb" :scroll-margin-bottom
      "scroll-ml" :scroll-margin-left))

  (testing "invalid scroll margin values fail during expansion"
    (is (= :scroll-margin
           (try
             (expand-utility #(spacing/scroll-margin* test-env "scroll-m" :scroll-margin nil))
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e))))))))

(deftest test-scroll-padding-utilities
  (testing "numeric scroll padding values become pixel declarations"
    (are [prefix property]
      (= {:result (str prefix "-20")
          :registered ['uni.style.fixture (str prefix "-20")
                       {property "20px"} []]}
         (expand-utility #(spacing/scroll-padding* test-env prefix property 20)))
      "scroll-p" :scroll-padding
      "scroll-px" :scroll-padding-inline
      "scroll-py" :scroll-padding-block
      "scroll-pt" :scroll-padding-top
      "scroll-pr" :scroll-padding-right
      "scroll-pb" :scroll-padding-bottom
      "scroll-pl" :scroll-padding-left))

  (testing "scroll padding rejects negative values"
    (is (= :scroll-padding
           (try
             (expand-utility #(spacing/scroll-padding* test-env "scroll-p" :scroll-padding -1))
             nil
             (catch clojure.lang.ExceptionInfo e
               (:type (ex-data e))))))))

(deftest test-scrollbar-utilities
  (testing "thumb and track styles are atomized by declaration"
    (doseq [[style expected]
            [[#(scrollbar/static* test-env :scrollbar-thumb-inherit)
              {:--us-scrollbar-thumb :inherit
               :scrollbar-color scrollbar/scrollbar-color-value}]
             [#(scrollbar/static* test-env :scrollbar-thumb-current)
              {:--us-scrollbar-thumb "currentColor"
               :scrollbar-color scrollbar/scrollbar-color-value}]
             [#(scrollbar/static* test-env :scrollbar-thumb-transparent)
              {:--us-scrollbar-thumb :transparent
               :scrollbar-color scrollbar/scrollbar-color-value}]
             [#(scrollbar/static* test-env :scrollbar-track-inherit)
              {:--us-scrollbar-track :inherit
               :scrollbar-color scrollbar/scrollbar-color-value}]
             [#(scrollbar/static* test-env :scrollbar-track-current)
              {:--us-scrollbar-track "currentColor"
               :scrollbar-color scrollbar/scrollbar-color-value}]
             [#(scrollbar/static* test-env :scrollbar-track-transparent)
              {:--us-scrollbar-track :transparent
               :scrollbar-color scrollbar/scrollbar-color-value}]
             [#(scrollbar/thumb* test-env :app-fg-base)
              {:--us-scrollbar-thumb "var(--app-fg-base)"
               :scrollbar-color scrollbar/scrollbar-color-value}]
             [#(scrollbar/thumb* test-env "oklch(50% 0.2 10)")
              {:--us-scrollbar-thumb "oklch(50% 0.2 10)"
               :scrollbar-color scrollbar/scrollbar-color-value}]
             [#(scrollbar/track* test-env :app-fg-base)
              {:--us-scrollbar-track "var(--app-fg-base)"
               :scrollbar-color scrollbar/scrollbar-color-value}]
             [#(scrollbar/track* test-env "oklch(50% 0.2 10)")
              {:--us-scrollbar-track "oklch(50% 0.2 10)"
               :scrollbar-color scrollbar/scrollbar-color-value}]]]
      (let [{:keys [result registered]} (expand-utility style)]
        (is (= 2 (count (str/split result #" "))))
        (is (= expected (apply merge (map #(nth % 2) registered)))))))

  (testing "thumb colors reject unsupported values"
    (doseq [color [nil 42 "" :namespaced/color]]
      (is (= :scrollbar-thumb
             (try
               (expand-utility #(scrollbar/thumb* test-env color))
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e))))))))

  (testing "track colors reject unsupported values"
    (doseq [color [nil 42 "" :namespaced/color]]
      (is (= :scrollbar-track
             (try
               (expand-utility #(scrollbar/track* test-env color))
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:type (ex-data e))))))))

  (testing "width and gutter presets emit their declarations"
    (are [utility class-name declaration]
      (= {:result class-name
          :registered ['uni.style.fixture class-name declaration []]}
         (expand-utility #(scrollbar/static* test-env utility)))
      :scrollbar-auto "scrollbar-auto" {:scrollbar-width :auto}
      :scrollbar-thin "scrollbar-thin" {:scrollbar-width :thin}
      :scrollbar-none "scrollbar-none" {:scrollbar-width :none}
      :scrollbar-gutter-auto "scrollbar-gutter-auto" {:scrollbar-gutter :auto}
      :scrollbar-gutter-stable "scrollbar-gutter-stable" {:scrollbar-gutter :stable}
      :scrollbar-gutter-both "scrollbar-gutter-both" {:scrollbar-gutter "stable both-edges"})))

(deftest test-margin-utilities
  (testing "numeric margin values become signed pixel declarations"
    (are [prefix property]
      (= {:result (str prefix "--12")
          :registered ['uni.style.fixture (str prefix "--12") {property "-12px"} []]}
         (expand-utility #(spacing/margin* test-env prefix property -12)))
      "m" :margin
      "mx" :margin-inline
      "my" :margin-block
      "mt" :margin-top
      "mb" :margin-bottom
      "mr" :margin-right
      "ml" :margin-left
      "ms" :margin-inline-start
      "me" :margin-inline-end))

  (testing "literal CSS margin values have hashed classes"
    (let [value "calc(1rem + 2px)"
          class-name (str "mx-value-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name
                           {:margin-inline value} []]}
             (expand-utility #(spacing/margin* test-env "mx" :margin-inline value))))))

  (testing "invalid margin values fail during expansion"
    (are [value] (= :margin
                    (try
                      (expand-utility #(spacing/margin* test-env "m" :margin value))
                      nil
                      (catch clojure.lang.ExceptionInfo e
                        (:type (ex-data e)))))
      ""
      :theme/auto)))

(deftest test-padding-utilities
  (testing "numeric padding values become pixel declarations"
    (are [prefix property]
      (= {:result (str prefix "-12")
          :registered ['uni.style.fixture (str prefix "-12") {property "12px"} []]}
         (expand-utility #(spacing/padding* test-env prefix property 12)))
      "p" :padding
      "px" :padding-inline
      "py" :padding-block
      "pt" :padding-top
      "pb" :padding-bottom
      "pr" :padding-right
      "pl" :padding-left
      "ps" :padding-inline-start
      "pe" :padding-inline-end))

  (testing "literal CSS padding values have hashed classes"
    (let [value "clamp(8px, 2vw, 16px)"
          class-name (str "px-value-" (compiler/content-hash value))]
      (is (= {:result class-name
              :registered ['uni.style.fixture class-name
                           {:padding-inline value} []]}
             (expand-utility #(spacing/padding* test-env "px" :padding-inline value))))))

  (testing "padding utilities compose with modifiers"
    (binding [compiler/*variants* [{:prefix "hover" :selector "&:hover"}]]
      (is (= {:result "hover:p-4"
              :registered ['uni.style.fixture "hover:p-4" {:padding "4px"}
                           ["&:hover"]]}
             (expand-utility #(spacing/padding* test-env "p" :padding 4))))))

  (testing "invalid padding values fail during expansion"
    (are [value] (= :padding
                    (try
                      (expand-utility #(spacing/padding* test-env "p" :padding value))
                      nil
                      (catch clojure.lang.ExceptionInfo e
                        (:type (ex-data e)))))
      -1
      ""
      :theme/auto)))

(deftest test-variant-border
  (binding [compiler/*variants* [{:prefix "hover" :selector "&:hover"}
                                 {:prefix "disabled" :selector "&:disabled"}]]
    (let [{:keys [result registered]}
          (expand-border "border" "border" [:app-border-base])]
      (is (= "hover:disabled:border--app-border-base hover:disabled:border-1 hover:disabled:border-style"
             result))
      (is (= [["&:hover" "&:disabled"]
              ["&:hover" "&:disabled"]
              ["&:hover" "&:disabled"]]
             (mapv #(nth % 3) registered))))))

(deftest test-not-requires-a-modifier
  (with-redefs [compiler/resolve-compile-time-constant (fn [_env _form]
                                                         "border-1")]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"must wrap a style modifier"
                          (modifier/not* test-env '(s/border :red))))))

(deftest test-compile-time-value-helpers
  (with-redefs [compiler/resolve-compile-time-constant
                (fn [_env form]
                  (if (= 'base-value form) 4 form))]
    (testing "unary helpers resolve and fold a compile-time number"
      (is (= 5 (compiler/inc-value* test-env 'base-value)))
      (is (= 3 (compiler/dec-value* test-env 'base-value))))

    (testing "arithmetic helpers resolve and fold compile-time numbers"
      (is (= 7 (compiler/add-values* test-env ['base-value 3])))
      (is (= 1 (compiler/subtract-values* test-env ['base-value 3])))
      (is (= 12 (compiler/multiply-values* test-env ['base-value 3])))
      (is (= 2 (compiler/divide-values* test-env ['base-value 2])))
      (is (= 0.5 (compiler/divide-values* test-env [2])))))

  (testing "numeric helpers reject non-numeric values"
    (are [f args type] (= type
                          (try
                            (f test-env args)
                            nil
                            (catch clojure.lang.ExceptionInfo e
                              (:type (ex-data e)))))
      compiler/inc-value* :token :increment
      compiler/dec-value* :token :decrement
      compiler/add-values* [:token] :addition
      compiler/subtract-values* [:token] :subtraction
      compiler/multiply-values* [:token] :multiplication
      compiler/divide-values* [:token] :division))

  (testing "subtraction and division require an operand"
    (are [f type] (= type
                     (try
                       (f test-env [])
                       nil
                       (catch clojure.lang.ExceptionInfo e
                         (:type (ex-data e)))))
      compiler/subtract-values* :subtraction
      compiler/divide-values* :division)))

(deftest test-compile-time-resolution
  (testing "supported constant expressions resolve before class naming"
    (with-redefs [ana/resolve-var (fn [_env sym]
                                    {:name ({'+ 'cljs.core/+
                                             '/ 'cljs.core//
                                             'str 'cljs.core/str}
                                            sym sym)})]
      (is (= "border-color-4bc9274d border-0_5 border-style"
             (:result (expand-border "border" "border" ['(/ 1 2) '(str "r" "ed")]))))))

  (testing "an analyzer ^:const token resolves to its custom property"
    (binding [cljs.env/*compiler* (cljs.env/default-compiler-env)
              ana/*cljs-ns* 'uni.style.synthetic]
      (ana/load-core)
      (let [env (assoc (ana/empty-env)
                       :line 1
                       :column 1
                       :ns {:name ana/*cljs-ns*
                            :meta {:file "uni/style/synthetic.cljs"}})]
        (ana/analyze env '(def ^:const border-token :app-border-token))
        (with-redefs [compiler/register-atomic! (fn [& _])]
          (is (= "border--app-border-token border-1 border-style"
                 (second (border/border* env "border" "border" ['border-token])))))))))

(deftest test-shared-and-arbitrary-classes
  (testing "different colors share independent width and style classes"
    (let [a (:result (expand-border "border" "border" [1 "#fff"]))
          b (:result (expand-border "border" "border" [1 "#000"]))]
      (is (not= (first (str/split a #" "))
                (first (str/split b #" "))))
      (is (= ["border-1" "border-style"]
             (rest (str/split a #" "))
             (rest (str/split b #" "))))))

  (testing "arbitrary colors use deterministic content hashes"
    (let [expected (str "border-color-" (compiler/content-hash "oklch(50% 0.2 10)"))]
      (is (= expected
             (first (str/split
                      (:result (expand-border "border" "border" ["oklch(50% 0.2 10)"]))
                      #" ")))))))

(deftest test-invalid-values
  (are [args type]
    (try
      (expand-border "border" "border" args)
      false
      (catch clojure.lang.ExceptionInfo e
        (= type (:type (ex-data e)))))
    [-1 "red"] :border-width
    ["" "red"] :border-width
    [42] :border-color
    [1 "red" :app-border-base] :border-color
    [1 :app-border-base 42] :border-color)

  (testing "runtime expressions fail with the original form"
    (let [form '(runtime-color)]
      (with-redefs [ana/resolve-var (fn [_env sym] {:name sym})
                    constant/macroexpand (fn [_env expr] expr)]
        (try
          (expand-border "border" "border" [form])
          (is false "expected expansion to fail")
          (catch clojure.lang.ExceptionInfo e
            (is (= form (:form (ex-data e))))
            (is (:dynamic? (ex-data e)))))))))
