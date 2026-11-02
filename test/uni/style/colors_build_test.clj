(ns uni.style.colors-build-test
  (:require [cljs.analyzer :as ana]
            [cljs.env :as env]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [uni.style.build :as build]
            [uni.style.colors :as sc]
            [uni.style.core :as s]
            [uni.style.fixtures.color-macros :as color-macros]
            [uni.style.fixtures.colors :as fixture]
            [uni.style.fixtures.shared-color-macros :as shared]
            [uni.style.impl.build :as impl.build]
            [uni.style.impl.constant :as constant]
            [uni.style.impl.palette :as palette]
            [uni.style.impl.registry :as registry]))

(defn- collect [form]
  (build/css #(binding [*ns* (the-ns 'uni.style.colors-build-test)] (eval form))))

(defn- anchors [v]
  (:anchors (:uni.style/color (meta v))))

(defn- close? [a b]
  (< (abs (- a b)) 0.000001))

(defn- macro-env [ns-sym]
  {:ns {:name ns-sym :meta {:file "synthetic.cljs"}
        :require-macros {'sc 'uni.style.colors 's 'uni.style.core
                         'custom 'uni.style.fixtures.color-macros
                         'shared 'uni.style.fixtures.shared-color-macros}}
   :line 1 :column 1})

(deftest exact-anchors-and-endpoints
  (doseq [[_ v] (ns-publics 'uni.style.colors)
          :let [anchors (anchors v)]
          :when anchors]
    (is (= #{5.0 40.0 95.0} (set (keys anchors))))
    (doseq [[level lch] anchors]
      (is (= lch (palette/interpolate anchors level))))
    (is (= [0 0] (take 2 (palette/interpolate anchors 0))))
    (is (= [100 0] (take 2 (palette/interpolate anchors 100)))))
  (is (= 22 (count (filter (comp anchors val) (ns-publics 'uni.style.colors))))))

(deftest shape-preserving-interpolation
  (doseq [[_ v] (ns-publics 'uni.style.colors)
          :let [anchors (anchors v)]
          :when anchors]
    (let [ramp (mapv #(palette/interpolate anchors (/ % 10.0)) (range 1001))]
      (is (apply <= (map first ramp)))
      (is (every? (fn [[l c h]] (and (<= 0 l 100) (<= 0 c 0.3) (<= 0 h 360))) ramp))
      (is (apply <= (map second (take 401 ramp))))
      (is (apply >= (map second (drop 400 ramp))))))
  (testing "cubic lightness/chroma bends between anchors instead of a straight mix"
    (let [[l c _] (palette/interpolate (anchors #'sc/green) 70)]
      (is (close? 84.684180 l))
      (is (close? 0.119653 c))))
  (testing "neutral stays achromatic throughout the ramp"
    (is (every? zero? (map #(second (palette/interpolate (anchors #'sc/neutral) %)) (range 101))))))

(deftest custom-anchor-normalization-and-segment-bounds
  (doseq [points [{50 [50 0.1 180]}
                  {10 [20 0.3 180] 20 [80 0.1 190] 80 [30 0.2 200]}]
          :let [anchors (palette/validate-anchors points)
                points (assoc anchors 0 [0 0 0] 100 [100 0 0])]
          [[x1 [l1 c1]] [x2 [l2 c2]]] (partition 2 1 points)
          step (range 1 10)
          :let [level (+ x1 (* (/ step 10.0) (- x2 x1)))
                [l c] (palette/interpolate anchors level)]]
    (is (<= (min l1 l2) l (max l1 l2)))
    (is (<= (min c1 c2) c (max c1 c2)))
    (is (= (palette/interpolate anchors level)
           (palette/interpolate (into (array-map) (reverse anchors)) level))))
  (is (= {60.0 [60 0.1 350]}
         (palette/validate-anchors {60.00000001 [60 0.1 -10]})))
  (doseq [anchors [{0.00000001 [100 0 0]}
                   {99.99999999 [0 0 0]}
                   {60 [60 0.1 180] 60.00000001 [50 0.1 180]}]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"six decimal places"
                          (palette/validate-anchors anchors)))))

(deftest hue-wrap-and-achromatic-anchors
  (let [anchors (palette/validate-anchors {5 [98 0.01 350] 95 [20 0.1 10]})]
    (is (close? 0 (nth (palette/interpolate anchors 50) 2)))
    (is (close? 350 (nth (palette/interpolate anchors 2.5) 2)))
    (is (close? 10 (nth (palette/interpolate anchors 97.5) 2))))
  (let [anchors (palette/validate-anchors {5 [98 0 0] 95 [20 0.1 280]})]
    (is (close? 280 (nth (palette/interpolate anchors 50) 2))))
  (let [anchors (palette/validate-anchors {50 [50 0.1 -10]})]
    (is (= 350 (nth (palette/interpolate anchors 50) 2)))))

(deftest colors-do-not-register-css
  (doseq [form ['(sc/def-color unused {60 [60 0.1 180]})
                '(sc/green 40) '(sc/green 70 50) '(color-macros/brand 40 :alpha)
                '(def plain-color (sc/green 40))
                '(def ^:const literal-color (sc/green 40))
                '(let [x (sc/green 40)] x)]]
    (testing (pr-str form)
      (is (= (str s/layer-priority-css s/property-css) (collect form)))))
  (is (= (str s/layer-priority-css s/property-css) (build/css #(sc/green 40))))
  (let [css (collect '(do (s/bg (sc/green 40))
                          (s/bg (sc/green 40.0 100))
                          (s/color (sc/green 40))
                          (s/hover (s/border 1 (sc/green 40)))))]
    (is (= 3 (count (re-seq #"oklch\(" css))))
    (is (str/includes? css "background-color:oklch(62.7% 0.194 149.214);"))
    (is (not (str/includes? css "@layer theme{")))
    (is (not (str/includes? css "us-color-"))))
  (is (= (str s/layer-priority-css s/property-css) (build/css (fn []))))
  (is (true? (:macro (meta #'sc/green))))
  (is (= '([level] [level alpha]) (:arglists (meta #'sc/green))))
  (is (true? (:macro (meta #'color-macros/brand)))))

(deftest literal-and-variable-alpha
  (let [css (collect '(do (s/bg (sc/green 40 50))
                          (s/color (sc/green 40 0))
                          (s/fill (sc/green 40 :element-alpha))
                          (s/stroke (sc/green 40 :other-alpha))))]
    (is (str/includes? css "background-color:oklch(62.7% 0.194 149.214 / 50%);"))
    (is (str/includes? css "color:oklch(62.7% 0.194 149.214 / 0%);"))
    (is (str/includes? css "fill:oklch(62.7% 0.194 149.214 / var(--element-alpha));"))
    (is (str/includes? css "stroke:oklch(62.7% 0.194 149.214 / var(--other-alpha));"))
    (is (not (str/includes? css "oklch(from")))
    (is (not (str/includes? css ":root")))))

(deftest palettes-compose-with-themes-and-modifiers
  (let [css (collect '(do (s/def-theme palette-dark)
                          (s/def-token surface palette-dark (sc/blue 40) (sc/green 40))
                          (s/components
                            (s/media "(min-width: 1px)"
                              (s/hover (s/bg (sc/green 40)))))
                          (s/def-variants variants {:accent (s/outline (sc/blue 40))})
                          (s/bg :external (sc/green 40))
                          (s/color (s/alpha (sc/green 40) 50))))]
    (is (re-find #"@layer theme\{:root\{--us-surface-[^:]+:oklch\(62.7% 0.194 149.214\);" css))
    (is (re-find #"\.us-theme-palette-dark-[^{]+\{--us-surface-[^:]+:oklch\(54.6% 0.245 262.881\);" css))
    (is (str/includes? css "background-color:var(--external, oklch(62.7% 0.194 149.214));"))
    (is (str/includes? css "color:color-mix(in oklab, oklch(62.7% 0.194 149.214) 50%, transparent);"))
    (is (str/includes? css "@layer components{@media (min-width: 1px){"))
    (is (str/includes? css "@layer variants{"))
    (is (not (str/includes? css "us-color-")))))

(deftest definitions-work-across-namespaces-and-builds
  (let [css (build/css #(require 'uni.style.fixtures.colors :reload))]
    (is (str/includes? css (str "--" (name fixture/background) ":" (sc/green 40) ";")))
    (is (= css (build/css #(require 'uni.style.fixtures.colors :reload)))))
  (testing "anchor edits retain utility identity but emit new values in the next build"
    (let [before (collect '(do (sc/def-color reloadable {60 [60 0.1 180]})
                               (s/bg (reloadable 60))))
          after (collect '(do (sc/def-color reloadable {60 [65 0.2 190]})
                              (s/bg (reloadable 60))))]
      (is (some? (re-find #"\.us-[^{]+" before)))
      (is (= (re-find #"\.us-[^{]+" before) (re-find #"\.us-[^{]+" after)))
      (is (str/includes? after "oklch(65% 0.2 190)"))
      (is (not (str/includes? after "oklch(60%"))))))

(deftest readable-development-names-and-hashed-release-names
  (binding [env/*compiler* (env/default-compiler-env)]
    (ana/load-core)
    (let [env (macro-env 'caller.naming)
          expand #(constant/macroexpand env %)
          custom-prefix (:prefix (:uni.style/color (meta #'color-macros/brand)))
          shared-prefix (:prefix (:uni.style/color (meta #'shared/brand)))]
      (doseq [[form expected] [['(s/bg (sc/green 10)) "bg-green-10"]
                               ['(s/bg (sc/green 40.0 100)) "bg-green-40"]
                               ['(s/color (sc/green 60.5 12.25)) "color-green-60_5-a12_25"]
                               ['(s/bg (s/str (sc/green 40))) "bg-green-40"]
                               ['(s/bg :external (sc/green 40)) "bg--external-green-40"]
                               ['(s/bg (custom/brand 40)) (str "bg-" custom-prefix "-40")]
                               ['(s/bg (shared/brand 40)) (str "bg-" shared-prefix "-40")]]]
        (is (= expected (expand form)) (pr-str form)))
      (is (not= custom-prefix shared-prefix))
      (is (re-matches #"bg-green-40-a-var-[a-f0-9]+" (expand '(s/bg (sc/green 40 :alpha)))))
      (is (not= (expand '(s/bg (sc/green 40 :alpha))) (expand '(s/bg (sc/green 40 :other)))))
      (testing "naming context does not leak into subsequent literal calls or transformed values"
        (is (re-matches #"bg-color-[a-f0-9]+" (expand '(s/bg "oklch(62.7% 0.194 149.214)"))))
        (is (re-matches #"bg-color-[a-f0-9]+"
                        (expand '(s/bg (s/str "linear-gradient(" (sc/green 40) ", " (sc/blue 40) ")"))))))
      (doseq [form ['(s/border 2 (sc/green 40))
                    '(s/outline 2 :external (sc/green 40))]]
        (expand form))
      (let [css (impl.build/bundle-css (registry/build-registry
                                         {:build-sources [:caller]
                                          :sources {:caller {:ns 'caller.naming}}
                                          :compiler-env @env/*compiler*}) {})]
        (is (str/includes? css ".bg-green-10{background-color:oklch("))
        (is (str/includes? css ".border-green-40{border-color:oklch("))
        (is (str/includes? css ".outline--external-green-40{outline-color:var(--external, oklch(")))
      (let [release (constant/macroexpand (assoc env :shadow.build/mode :release) '(s/bg (sc/green 10)))]
        (is (re-matches #"us-[a-f0-9]+" release))
        (is (= (s/class-name (s/bg (sc/green 10))) release))))))

(deftest caller-reachability-and-cross-namespace-deduplication
  (binding [env/*compiler* (env/default-compiler-env)]
    (ana/load-core)
    (let [state (fn [names]
                  {:build-sources names :sources (zipmap names (map #(hash-map :ns %) names))
                   :compiler-env @env/*compiler*})]
      (doseq [caller '[caller.a caller.b]]
        (constant/macroexpand (macro-env caller) '(s/bg (sc/green 40))))
      (constant/macroexpand (macro-env 'caller.stale) '(s/bg (sc/green 5)))
      (constant/macroexpand (macro-env 'caller.unused) '(sc/green 70))
      (let [css (impl.build/bundle-css (registry/build-registry (state '[caller.a caller.b caller.unused])) {})]
        (is (= ".bg-green-40{background-color:oklch(62.7% 0.194 149.214);}" css)))
      (is (= "" (impl.build/bundle-css (registry/build-registry (state '[caller.unused])) {})))
      (is (= "" (impl.build/bundle-css (registry/build-registry (state [])) {}))))))

(deftest compile-time-validation
  (doseq [form ['(s/bg (sc/green 600)) '(s/bg (sc/green -1))
                '(s/bg (sc/green ##NaN)) '(s/bg (sc/green ##Inf))
                '(s/bg (sc/green 40 nil)) '(s/bg (sc/green 40 101))
                '(s/bg (sc/green 40 :ns/alpha))
                '(let [level 60] (s/bg (sc/green level)))
                '(let [alpha 50] (s/bg (sc/green 40 alpha)))
                '(let [green sc/green] (s/bg (green 60)))
                '(sc/def-color qualified/name {60 [60 0.1 180]})
                '(sc/def-color invalid {}) '(sc/def-color invalid nil)
                '(sc/def-color invalid {0 [0 0 0]})
                '(sc/def-color invalid {100 [100 0 0]})
                '(sc/def-color invalid {60 [60 0.1]})
                '(sc/def-color invalid {60 [101 0.1 180]})
                '(sc/def-color invalid {60 [60 -0.1 180]})
                '(sc/def-color invalid {60 [60 0.1 ##Inf]})]]
    (testing (pr-str form)
      (is (thrown? clojure.lang.Compiler$CompilerException (collect form))))))

(deftest standalone-argument-validation
  (doseq [level [-1 101 nil "60" :level ##Inf ##NaN]]
    (is (thrown? clojure.lang.Compiler$CompilerException (collect (list 'sc/green level)))))
  (doseq [alpha [-1 101 nil "50%" ##Inf ##NaN :ns/alpha (keyword "bad name")]]
    (is (thrown? clojure.lang.Compiler$CompilerException (collect (list 'sc/green 60 alpha)))))
  (doseq [form ['(sc/green) '(sc/green 40 50 1)
                '(s/bg (sc/green)) '(s/bg (sc/green 40 50 1))]]
    (is (thrown? clojure.lang.ArityException (collect form))))
  (doseq [form ['(let [level 60] (sc/green level))
                '(fn [alpha] (sc/green 40 alpha))
                '(let [x (sc/green 40)] (s/bg x))]]
    (testing (pr-str form)
      (is (thrown? clojure.lang.Compiler$CompilerException (collect form))))))

(deftest cljs-standalone-macro-expansion
  (binding [env/*compiler* (env/default-compiler-env)
            ana/*cljs-ns* 'uni.style.colors-build-test.synthetic]
    (ana/load-core)
    (let [analyzer-env (merge (ana/empty-env) (macro-env ana/*cljs-ns*))]
      (is (= "oklch(62.7% 0.194 149.214)" (constant/macroexpand analyzer-env '(sc/green 40))))
      (let [ast (ana/analyze analyzer-env '(def ^:const concrete (sc/green 40)))
            state {:build-sources [:caller]
                   :sources {:caller {:ns ana/*cljs-ns*}}
                   :compiler-env @env/*compiler*}]
        (is (= :const (get-in ast [:init :op])))
        (is (= "oklch(62.7% 0.194 149.214)" (get-in ast [:init :val])))
        (is (empty? (registry/build-registry state))))
      (is (thrown? clojure.lang.ExceptionInfo
                   (constant/resolve-compile-time-constant
                     (assoc-in analyzer-env [:locals 'level] {:name 'level :op :binding})
                     '(sc/green level))))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Define color macros in a .clj namespace"
                            (palette/def-color* analyzer-env 'invalid {60 [60 0.1 180]}))))))
