(ns uni.style.jvm-test
  (:require [cljs.env :as cljs.env]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [uni.style.build :as build]
            [uni.style.core :as s]
            [uni.style.fixtures.portable :as fixture]
            [uni.style.fixtures.themes :as themes]
            [uni.style.impl.naming :as naming]
            [uni.style.impl.registry :as registry])
  (:import [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:const spacing 6)
(def ordinary-spacing 7)
(def ^:dynamic *spacing* 8)

(defmacro constant-form [form] form)

(defn- evaluate [form]
  (binding [*ns* (the-ns 'uni.style.jvm-test)]
    (eval form)))

(defn- collect [form]
  (build/css #(evaluate form)))

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "uni-style-jvm-" (make-array FileAttribute 0))))

(defn- delete-tree! [dir]
  (doseq [file (reverse (file-seq dir))]
    (io/delete-file file :silently true)))

(deftest constant-resolution
  (is (= (s/p 6) (s/p spacing)))
  (is (= (s/p 8) (s/p (+ spacing 2))))
  (is (= (s/p 3) (s/p (/ spacing 2))))
  (is (= (s/p 6) (s/p (constant-form spacing))))
  (is (= (s/bg "color-6") (s/bg (str "color-" spacing))))
  (is (= (s/p 8) (s/p fixture/doubled-spacing)))
  (doseq [form ['(s/p ordinary-spacing)
                '(s/p *spacing*)
                '(s/p (rand-int 10))
                '(let [spacing 99] (s/p spacing))
                '(fn [spacing] (s/p (constant-form spacing)))
                '(let [+ (fn [& _] 99)] (s/p (+ 2 4)))]]
    (testing (pr-str form)
      (try
        (evaluate form)
        (is false "expected rejection of a runtime expression")
        (catch clojure.lang.Compiler$CompilerException e
          (is (:dynamic? (ex-data (ex-cause e)))))))))

(deftest deterministic-release-names
  (is (= (naming/hashed-class-name [:uni.style/utility "p-8"]) (s/p 8)))
  (is (= (naming/hashed-class-name [:uni.style/utility "hover:p-8"])
         (s/class-name (s/hover (s/p 8)))))
  (is (= (naming/hashed-class-name "uni.style.fixtures.portable/panel") fixture/panel))
  (is (= (naming/hashed-class-name "uni.style.fixtures.portable/appearance-primary")
         (:primary fixture/appearance)))
  (is (= (naming/hashed-class-name "uni.style.fixtures.portable/fade-in") fixture/fade-in)))

(deftest source-loading-collects-complete-css
  (let [css (build/css #(require 'uni.style.fixtures.portable :reload))]
    (is (str/starts-with? css "@layer theme,base,components,variants;"))
    (is (str/includes? css "Example Sans"))
    (is (str/includes? css "body{color:black;}"))
    (is (str/includes? css (str "." fixture/panel "{padding:4px;")))
    (is (str/includes? css (str "." fixture/compact-panel "{padding:4px;")))
    (is (str/includes? css (str "." fixture/panel ":hover{background-color:red;}")))
    (is (str/includes? css (str "." fixture/focus-panel "{outline-style:solid;")))
    (is (str/includes? css (str "." fixture/focus-visible-panel
                                ":focus-visible{outline-style:solid;")))
    (is (str/includes? css (str "." (:primary fixture/appearance) "{color:white;}")))
    (is (str/includes? css (str "@keyframes " fixture/fade-in "{0%{opacity:0%;}100%{opacity:100%;}}")))
    (is (str/includes? css (str "." (fixture/utility-class) "{display:flex;}")))
    (is (= css (build/css #(require 'uni.style.fixtures.portable :reload))))))

(deftest stylesheet-expression-export
  (testing "the caller owns var metadata"
    (is (true? (:const (meta #'fixture/document-css))))
    (is (not (:const (meta #'fixture/selector-list-css)))))
  (testing "literal exports do not register stylesheet output"
    (is (= (str s/layer-priority-css s/property-css)
           (collect '(s/css {"a" (s/color "red")}))))
    (is (= (str s/layer-priority-css s/property-css)
           (collect '(s/css
                       {"a" (s/components
                              (s/media "(min-width: 10px)"
                                (s/sel "p" (s/color :example-text))))}))))
    (is (not (str/includes? (build/css #(require 'uni.style.fixtures.portable :reload))
                            fixture/document-css))))
  (testing "sorting does not depend on map type or insertion order"
    (let [entries (mapv (fn [i] [(str "selector-" i) '(s/p 4)]) (range 12))
          export #(evaluate (list 's/css %))
          expected (apply str (map #(str % "{padding:4px;}") (sort (map first entries))))]
      (is (= expected (export (apply array-map (mapcat identity entries)))))
      (is (= expected (export (into {} (reverse entries)))))))
  (testing "a selector can reuse a component without creating a class"
    (let [css (evaluate '(s/css {"article" fixture/panel}))]
      (is (str/includes? css "@layer components{article{padding:4px;"))
      (is (not (str/includes? css fixture/panel))))))

(deftest stylesheet-expression-validation
  (doseq [form ['(s/css nil)
                '(s/css {})
                '(s/css [["a" (s/flex)]])
                '(s/css {:a (s/flex)})
                '(s/css {"  " (s/flex)})
                '(s/css {ordinary-spacing (s/flex)})
                '(let [selector "a"] (s/css {selector (s/flex)}))
                '(let [color "red"] (s/css {"a" (s/raw {:color color})}))
                '(s/css {"a" []})
                '(s/css {"a" "color:red;"})
                '(s/css {"a" (s/flex) (s/str "a") (s/grid)})
                '(s/css {"a" (s/flex) " a " (s/grid)})
                '(s/css {"a" [(s/flex) (s/components (s/grid))]})]]
    (testing (pr-str form)
      (is (thrown? clojure.lang.Compiler$CompilerException (evaluate form))))))

(deftest nested-modifiers-and-static-maps
  (let [css (collect '(s/media "(min-width: 10px)"
                        (s/hover (s/raw {:color "red" :opacity 0.5}))))]
    (is (str/includes? css "@media (min-width: 10px)"))
    (is (str/includes? css ":hover{color:red;}"))
    (is (str/includes? css ":hover{opacity:0.5;}"))))

(deftest build-collections-are-isolated
  (let [flex-css (collect '(s/flex))
        grid-css (collect '(s/grid))]
    (is (not (str/includes? grid-css "display:flex")))
    (is (not (str/includes? flex-css "display:grid")))
    (is (= (str s/layer-priority-css s/property-css) (build/css (fn [] nil))))
    (is (= flex-css (collect '(s/flex))))
    (is (= flex-css
           (build/css #(do (collect '(s/grid)) (evaluate '(s/flex)))))))
  (let [compiler (cljs.env/default-compiler-env)
        original @compiler]
    (binding [cljs.env/*compiler* compiler]
      (is (str/includes? (collect '(s/flex)) "display:flex")))
    (is (= original @compiler))))

(deftest repeated-registrations-are-deduplicated
  (let [css (collect '(do (s/flex) (s/flex)))
        rule (str "." (s/flex) "{display:flex;}")]
    (is (= (str s/layer-priority-css s/property-css rule) css))))

(deftest namespace-order-and-conflict-checking
  (let [css (build/css #(do
                          (registry/register-atomic! nil 'example.z "first" {:display :block})
                          (registry/register-atomic! nil 'example.a "second" {:display :flex})
                          (registry/register-atomic! nil 'example.a "first" {:display :block})))]
    (is (= (str s/layer-priority-css s/property-css ".first{display:block;}.second{display:flex;}") css)))
  (is (thrown-with-msg?
        clojure.lang.ExceptionInfo #"conflicting declarations"
        (build/css #(do
                      (registry/register-atomic! nil 'example.a "same" {:display :block})
                      (registry/register-atomic! nil 'example.b "same" {:display :flex})))))
  (is (= (str s/layer-priority-css s/property-css) (build/css (fn [] nil)))))

(deftest recompiled-components-use-current-declarations
  (let [compile-component (fn [padding]
                            (collect `(do
                                        (s/def-component ~'reloadable (s/p ~padding))
                                        (s/def-component ~'derived ~'reloadable))))
        before (compile-component 4)
        after (compile-component 8)]
    (is (str/includes? before "padding:4px;"))
    (is (not (str/includes? after "padding:4px;")))
    (is (= 2 (count (re-seq #"padding:8px;" after))))))

(deftest concurrent-composition-and-collection
  (let [tasks (mapv (fn [i]
                      (future
                        (let [expected (if (even? i) (s/p 8) (s/p 4))
                              css (collect (if (even? i) '(s/flex) '(s/grid)))
                              values (repeatedly 100
                                                 #(s/class-name
                                                    (s/c (s/p 2) expected)))]
                          {:expected expected :values (vec values) :css css})))
                    (range 8))]
    (doseq [[i task] (map-indexed vector tasks)]
      (let [{:keys [expected values css]} @task]
        (is (every? #{expected} values))
        (is (= (collect (if (even? i) '(s/flex) '(s/grid))) css))))))

(deftest css-file-export
  (let [dir (temp-dir)
        output (.getPath (io/file dir "nested" "bundle.css"))]
    (try
      (is (= output (build/write-css! #(evaluate '(s/flex)) output)))
      (is (= (collect '(s/flex)) (slurp output)))
      (build/write-css! (fn [] nil) output)
      (is (= (str s/layer-priority-css s/property-css) (slurp output)))
      (finally (delete-tree! dir)))))

(deftest aot-runtime-does-not-depend-on-the-compilation-registry
  (let [dir (temp-dir)
        output (.getPath (io/file dir "bundle.css"))]
    (try
      (binding [*compile-path* (.getPath dir)]
        (build/write-css! #(do (compile 'uni.style.fixtures.portable)
                               (compile 'uni.style.fixtures.themes)) output))
      (let [expected (fixture/classes true)
            result-file (.getPath (io/file dir "result.edn"))
            program `(do
                       (require 'uni.style.build 'uni.style.core)
                       (let [css# (build/css #(require 'uni.style.fixtures.portable 'uni.style.fixtures.themes))
                             classes# ((resolve 'uni.style.fixtures.portable/classes) true)
                             background# @(resolve 'uni.style.fixtures.themes/background)
                             dark# @(resolve 'uni.style.fixtures.themes/dark)]
                         (spit ~result-file (pr-str {:css css# :classes classes#
                                                     :theme dark# :token background#
                                                     :value (s/token-value background# dark#)
                                                     :loaded? (contains? (set (s/all-tokens)) background#)
                                                     :pre-exported-css @(resolve 'uni.style.fixtures.portable/document-css)}))))
            {:keys [exit out err]}
            (shell/sh (.getPath (io/file (System/getProperty "java.home") "bin" "java"))
                      "-cp" (str (.getPath dir) File/pathSeparator
                                 (System/getProperty "java.class.path"))
                      "clojure.main" "-e" (pr-str program))]
        (is (zero? exit) (str out err))
        (when (zero? exit)
          (is (= {:css (str s/layer-priority-css s/property-css) :classes expected
                  :theme themes/dark :token themes/background :value "#111"
                  :loaded? true
                  :pre-exported-css fixture/document-css}
                 (edn/read-string (slurp result-file)))))
        (is (str/includes? (slurp output) "display:flex")))
      (finally (delete-tree! dir)))))

(deftest jvm-needs-no-clojurescript-compiler
  (let [dir (temp-dir)
        result-file (.getPath (io/file dir "result.edn"))
        fixtures '[uni.style.fixtures.portable uni.style.fixtures.themes
                   uni.style.fixtures.colors]
        compile-fixtures #(doseq [ns-sym fixtures] (require ns-sym :reload))
        classpath (->> (str/split (System/getProperty "java.class.path")
                                  (re-pattern File/pathSeparator))
                       (remove #(re-find #"clojurescript|shadow-cljs" %))
                       (str/join File/pathSeparator))
        program `(do
                   (require 'uni.style.build)
                   (spit ~result-file
                         (pr-str {:cljs-loadable? (some? (io/resource "cljs/env.cljc"))
                                  :css (build/css #(doseq [ns-sym# '~fixtures]
                                                     (require ns-sym# :reload)))})))]
    (try
      (let [{:keys [exit out err]}
            (shell/sh (.getPath (io/file (System/getProperty "java.home") "bin" "java"))
                      "-cp" classpath "clojure.main" "-e" (pr-str program))]
        (is (zero? exit) (str out err))
        (when (zero? exit)
          (is (= {:cljs-loadable? false :css (build/css compile-fixtures)}
                 (edn/read-string (slurp result-file))))))
      (finally (delete-tree! dir)))))

(deftest tailwind-modifiers-and-utilities
  (testing "breakpoints are emitted from the smallest to the largest"
    (let [css (collect '(s/c (s/xxl (s/p 16)) (s/sm (s/p 6)) (s/xl (s/p 14))
                             (s/md (s/p 8)) (s/lg (s/p 12))))]
      (is (= ["40rem" "48rem" "64rem" "80rem" "96rem"]
             (map second (re-seq #"@media \(min-width: ([0-9]+rem)\)" css))))))
  (testing "max breakpoints are emitted from the widest to the narrowest"
    (let [css (collect '(s/c (s/max-sm (s/p 1)) (s/max-xxl (s/p 5)) (s/max-md (s/p 2))))]
      (is (= ["96rem" "48rem" "40rem"]
             (map second (re-seq #"@media \(width < ([0-9]+rem)\)" css))))))
  (testing "custom width media queries sort numerically with the breakpoints"
    (let [css (collect '(s/c (s/xxl (s/p 16)) (s/media "(min-width: 1000px)" (s/p 9))
                             (s/md (s/p 8))))]
      (is (= ["48rem" "1000px" "96rem"]
             (map second (re-seq #"@media \(min-width: ([0-9]+(?:rem|px))\)" css))))))
  (testing "font-mono, not-italic, blur, and spans"
    (is (str/includes? (collect '(s/font-mono)) "{font-family:ui-monospace, SFMono-Regular,"))
    (is (str/includes? (collect '(s/not-italic)) "{font-style:normal;}"))
    (is (str/includes? (collect '(s/blur 4)) "{filter:blur(4px);}"))
    (is (str/includes? (collect '(s/blur "0.5rem")) "{filter:blur(0.5rem);}"))
    (is (str/includes? (collect '(s/col-span 2)) "{grid-column:span 2 / span 2;}"))
    (is (str/includes? (collect '(s/row-span 3)) "{grid-row:span 3 / span 3;}")))
  (testing "pseudo-element modifiers"
    (is (str/includes? (collect '(s/selection (s/bg "red"))) " *::selection,"))
    (is (str/includes? (collect '(s/marker (s/color "red"))) " *::marker,"))
    (is (str/includes? (collect '(s/first-letter (s/font-size 20))) "::first-letter{"))
    (is (str/includes? (collect '(s/file (s/p 4))) "::file-selector-button{")))
  (testing "aria modifiers match \"true\" and can be negated"
    (is (str/includes? (collect '(s/aria-checked (s/bg "red"))) "[aria-checked=\"true\"]{"))
    (is (str/includes? (collect '(s/not (s/aria-disabled (s/opacity 50))))
                       ":not([aria-disabled=\"true\"]){")))
  (testing "element state and media modifiers"
    (is (str/includes? (collect '(s/checked (s/bg "red"))) ":checked{"))
    (is (str/includes? (collect '(s/not (s/open (s/hidden))))
                       ":not([open], :popover-open, :open){"))
    (is (str/includes? (collect '(s/motion-reduce (s/transition-none)))
                       "@media (prefers-reduced-motion: reduce){"))
    (is (str/includes? (collect '(s/print (s/hidden))) "@media print{")))
  (testing "space and divide style every child except the last, without specificity"
    (is (re-find #":where\(\.[^ ]+ > :not\(:last-child\)\)\{margin-inline-end:16px;\}"
                 (collect '(s/space-x 16))))
    (is (re-find #":where\(\.[^ ]+ > :not\(:last-child\)\)\{border-bottom-width:1px;\}"
                 (collect '(s/divide-y 1 "red")))))
  (testing "border-x sets each side, so a later side still overrides it"
    (let [classes #(set (str/split (s/class-name %) #" "))]
      (is (= (classes (s/c (s/border-l 1 "red") (s/border-r 1 "red")))
             (classes (s/border-x 1 "red"))))
      (is (= (classes (s/c (s/border-r 1 "red") (s/border-l 2 "blue")))
             (classes (s/c (s/border-x 1 "red") (s/border-l 2 "blue")))))))
  (testing "sr-only and columns"
    (is (str/includes? (collect '(s/sr-only)) "{clip-path:inset(50%);}"))
    (is (str/includes? (collect '(s/columns 3)) "{columns:3;}"))
    (is (str/includes? (collect '(s/columns "12rem")) "{columns:12rem;}"))))
