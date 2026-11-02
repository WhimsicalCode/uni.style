(ns uni.style.colors-test
  (:require #?(:clj [clojure.test :refer [are deftest is testing]]
               :cljs [cljs.test :refer-macros [are deftest is testing]])
            [uni.style.colors :as sc]
            [uni.style.core :as s]
            #?(:clj [uni.style.fixtures.color-macros :as palette :refer [brand]])
            [uni.style.fixtures.colors :as fixture]
            [uni.style.fixtures.shared-color-macros :as shared])
  #?(:cljs (:require-macros [uni.style.fixtures.color-macros :as palette :refer [brand]])))

(deftest oklch-to-hex
  (are [color expected] (= expected (sc/hex color))
    sc/black "#000000"
    sc/white "#FFFFFF"
    (sc/green 40) "#00A63E"
    (sc/blue 40) "#155DFC"
    (sc/red 40) "#E7000B"
    (sc/neutral 40) "#525252"
    (brand 40) "#00A63E"
    (sc/green 40 100) "#00A63E"
    (sc/green 40 50) "#00A63E80"
    (sc/green 40 12.25) "#00A63E1F"
    (sc/green 40 0) "#00A63E00"
    "oklch(0% 0 0 / 0%)" "#00000000"
    "oklch(100% 0 0 / 50%)" "#FFFFFF80")
  (testing "out-of-gamut channels clip rather than wrap"
    (is (= "#FF0000" (sc/hex "oklch(50% 1 30)")))))

(deftest oklch-to-rgba
  (are [color expected] (= expected (sc/rgba color))
    sc/black "rgba(0,0,0,1)"
    sc/white "rgba(255,255,255,1)"
    (sc/green 40) "rgba(0,166,62,1)"
    (sc/blue 40) "rgba(21,93,252,1)"
    (sc/neutral 40) "rgba(82,82,82,1)"
    (brand 40 50) "rgba(0,166,62,0.5)"
    (sc/green 40 12.25) "rgba(0,166,62,0.1225)"
    (sc/green 40 0) "rgba(0,166,62,0)"
    "oklch(50% 1 30 / 25%)" "rgba(255,0,0,0.25)"))

(deftest conversions-only-accept-numeric-palette-oklch
  (doseq [convert [sc/hex sc/rgba]
          color [nil "" "#FFFFFF" "red" "rgb(255,0,0)" "var(--color)"
                 (sc/green 40 :alpha) "oklch(50% 0.1 30 / calc(50%))"
                 "oklch(50% 0.1 30); color: red" "oklch(50% 0.1 30)</style>"
                 "oklch(101% 0 0)" "oklch(-1% 0 0)" "oklch(50% -0.1 30)"
                 "oklch(50% NaN 30)" "oklch(50% 0.1 Infinity)"
                 "oklch(50% 0.1 30 / 101%)"
                 (str "oklch(50% " (apply str (repeat 400 "9")) " 30)")]]
    (is (thrown? #?(:clj Exception :cljs js/Error) (convert color))
        (str "Reject " color))))

(deftest portable-color-values
  ;; These are ordinary strings on JVM, development CLJS, and advanced CLJS.
  (is (= "oklch(62.7% 0.194 149.214)" (sc/green 40)))
  (is (= "oklch(62.7% 0.194 149.214 / 50%)" (sc/green 40 50)))
  (is (= (sc/green 40) (sc/green 40.0 100)))
  (is (not= (sc/green 0) (sc/green 0.000001)))
  (is (= (sc/green 40) (sc/green 40.00000001)))
  (is (= (sc/green 0 0) (sc/green 0.00000001 0.00000001)))
  (is (= (sc/green 100) (sc/green 99.99999999 99.99999999)))
  (is (= (sc/green 60.5 12.25) (sc/green (/ 121 2) (/ 49 4))))
  (is (= "oklch(62.7% 0.194 149.214 / var(--opacity))" (sc/green 40 :opacity)))
  (is (= (brand 40) (palette/brand 40) (sc/green 40)))
  (is (not= (shared/brand 40) (palette/brand 40)))
  ;; Different palettes retain different utility names, even for equal colors.
  (is (not= (s/bg (palette/brand 40)) (s/bg (sc/green 40)))))

(deftest portable-color-expressions
  (is (= (s/bg (palette/brand 40)) (fixture/background-class)))
  (is (= (s/border 1 (sc/blue 40 :fixture-alpha)) (fixture/border-class)))
  (is (= (s/bg (sc/green 40)) (s/bg (sc/green (+ 30 10) 100))))
  (is (= (s/bg (sc/green 40)) (s/bg (sc/green fixture/middle))))
  (is (= (s/bg (sc/green 40 50)) (s/bg (sc/green fixture/middle fixture/alpha))))
  (is (= (sc/green 40) (s/str (sc/green 40))))
  (is (= (str "linear-gradient(" (sc/green 95) ", " (palette/brand 5 50) ")") fixture/gradient))
  (is (= "oklch(100% 0 0)" sc/white (s/str sc/white)))
  (is (= "oklch(0% 0 0)" sc/black (s/str sc/black))))

(deftest literal-color-values
  (is (= "oklch(26.6% 0.065 152.934)" (sc/green 5)))
  (is (= "oklch(98.2% 0.018 155.826)" (sc/green 95)))
  (is (= "oklch(62.7% 0.194 149.214)"
         (sc/green 40) (sc/green (+ 30 10)) (sc/green fixture/middle) (brand 40)
         fixture/constant-color (s/token-value fixture/concrete-background)))
  (is (= "oklch(84.68418% 0.119653 152.820545)" (sc/green 70)))
  (is (= "oklch(50% 0.1 180)" (shared/brand 50)))
  (is (= "oklch(62.7% 0.194 149.214 / 50%)" (sc/green 40 50) fixture/constant-alpha-color))
  (is (= "oklch(62.7% 0.194 149.214 / 0%)" (sc/green 40 0)))
  (is (= "oklch(62.7% 0.194 149.214)" (sc/green 40 100)))
  (is (= "oklch(62.7% 0.194 149.214 / 12.25%)" (sc/green 40 12.25)))
  (is (= "oklch(62.7% 0.194 149.214 / var(--fixture-alpha))"
         (sc/green 40 fixture/alpha-token) fixture/constant-variable-alpha))
  (is (= "oklch(0% 0 152.934)" (sc/green 0)))
  (is (= "oklch(100% 0 155.826)" (sc/green 100))))

(deftest palette-backed-token-literals
  (is (= fixture/constant-color (s/token-value fixture/background)))
  (is (= fixture/constant-color (s/str (s/token-value fixture/background))))
  (is (= fixture/constant-color (s/token-value fixture/background-alias)))
  (is (= (brand 5 25)
         (s/token-value fixture/themed-background fixture/dark)
         (s/token-value fixture/background-alias fixture/dark)
         (s/str (s/token-value fixture/themed-background fixture/dark))))
  (is (= (s/str (s/alpha (brand 40) 50)) (s/token-value fixture/translucent-background)))
  (is (= fixture/constant-variable-alpha (s/token-value fixture/variable-alpha-background)))
  (is (= fixture/gradient (s/token-value fixture/gradient-background)))
  (is (= (str "@layer theme{:root{--" (name fixture/background) ":" fixture/constant-color ";}}")
         (s/token-css nil [fixture/background])))
  (is (= (str "@layer theme{:root." fixture/dark ",." fixture/dark "{--"
              (name fixture/themed-background) ":" (brand 5 25) ";}}")
         (s/token-css fixture/dark [fixture/themed-background]))))

(deftest literal-colors-compose-with-style-macros
  (is (= (s/bg "oklch(62.7% 0.194 149.214)") (s/bg fixture/constant-color)))
  (is (= "0 0 2px oklch(62.7% 0.194 149.214 / 50%)" (s/str "0 0 2px " (sc/green 40 50))))
  (is (= (s/color (s/alpha (sc/green 40) 50)) (s/color (s/alpha fixture/constant-color 50))))
  (is (= (s/var :accent (sc/green 40)) (s/var :accent fixture/constant-color)))
  (is (= ".accent{background-color:oklch(62.7% 0.194 149.214);}"
         (s/css {".accent" (s/bg (sc/green 40))})
         (s/css {".accent" (s/bg fixture/constant-color)})))
  (is (= ".accent{background-color:var(--external, oklch(62.7% 0.194 149.214));}"
         (s/css {".accent" (s/bg :external (sc/green 40))})
         (s/css {".accent" (s/bg :external fixture/constant-color)}))))

(deftest standalone-color-macros
  (is (= "oklch(62.7% 0.194 149.214)"
         fixture/standalone fixture/constant-color (fixture/local-reference)))
  (is (= "oklch(62.7% 0.194 149.214 / 50%)" fixture/constant-alpha-color))
  (is (= (s/bg fixture/constant-color) (fixture/constant-background-class)))
  (is (= (s/bg "oklch(62.7% 0.194 149.214 / 50%)") (s/bg fixture/constant-alpha-color)))
  (is (= (s/bg "oklch(62.7% 0.194 149.214 / var(--fixture-alpha))")
         (s/bg fixture/constant-variable-alpha))))
