(ns uni.style.theme-build-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [uni.style.build :as build]
            [uni.style.core :as s]
            [uni.style.fixtures.themes :as fixture]
            [uni.style.impl.tokens :as tokens]))

(defn- collect [form]
  (build/css #(binding [*ns* (the-ns 'uni.style.theme-build-test)] (eval form))))

(deftest css-definitions
  (let [css (build/css #(require 'uni.style.fixtures.themes :reload))
        property (str "--" (name fixture/background))]
    (is (str/includes? css (str "@layer theme{:root{" property ":#fff;}}")))
    (is (str/includes? css (str "@layer theme{:root." fixture/dark ",." fixture/dark "{" property ":#111;}}")))
    (is (not (str/includes? (str/replace css s/property-css "") "@layer base{")))
    (testing "a theme without an override redeclares the default for nested scopes"
      (is (str/includes? css (str "@layer theme{:root." fixture/light ",." fixture/light "{" property ":#fff;}}"))))
    (is (= css (build/css #(require 'uni.style.fixtures.themes :reload))))
    (is (= (str s/layer-priority-css s/property-css) (build/css (fn []))))))

(deftest runtime-export-matches-build-output
  (let [css (build/css #(require 'uni.style.fixtures.themes :reload))]
    (is (str/starts-with? css s/layer-priority-css))
    (is (= (str s/layer-priority-css s/property-css) (build/css (fn []))))
    (is (= (str s/layer-priority-css s/property-css)
           (build/css #(s/token-css fixture/dark [fixture/background]))))
    (doseq [theme [nil fixture/light fixture/dark]
            token [fixture/background fixture/foreground fixture/alias-background
                   fixture/radius fixture/factor fixture/overlay fixture/tinted-background fixture/shadow-token]]
      (is (str/includes? css (s/token-css theme [token]))))))

(deftest base-overrides-are-separate-from-theme-defaults
  (let [css (collect '(do
                        (s/def-token base-overridden "#fff")
                        (s/def-base ":root" (s/var base-overridden "#f00"))))
        [_ property] (re-find #"@layer theme\{:root\{(--us-base-overridden-[a-f0-9]{8}):#fff;\}\}" css)]
    (is (some? property))
    (is (str/starts-with? css "@layer theme,base,components,variants;"))
    (is (str/includes? css (str "@layer base{:root{" property ":#f00;}}")))))

(deftest alpha-token-css
  (let [css (build/css #(require 'uni.style.fixtures.themes :reload))
        property (str "--" (name fixture/overlay))]
    (is (str/includes? css (str ":root{" property ":color-mix(in oklab, #000 40%, transparent);}")))
    (is (str/includes? css (str ":root." fixture/dark ",." fixture/dark
                                "{" property ":color-mix(in oklab, #fff 20%, transparent);}")))))

(deftest alpha-and-opacity-arities
  (doseq [form ['(s/opacity "#fff" 50) '(s/alpha 50) '(s/alpha "#fff" 50 20)]]
    (is (thrown? clojure.lang.ArityException (collect form)))))

(deftest token-validation
  (doseq [form ['(s/def-token missing)
                '(s/def-token missing fixture/dark "#111")
                '(s/def-token duplicate fixture/dark "a" fixture/dark "b" "c")
                '(s/def-token invalid "not-a-theme" "a" "b")
                '(s/def-token empty "")
                '(s/def-token nil-value nil)
                '(s/def-token infinite ##Inf)
                '(s/def-token missing fixture/dark "#111" fixture/light)
                '(s/def-token invalid :namespaced/token)
                '(s/def-token invalid (s/alpha "#fff" -1))
                '(s/def-token invalid (s/alpha "#fff" 101))
                '(s/def-token invalid (s/alpha "#fff" 0.5))
                '(s/def-theme qualified/name)]]
    (testing (pr-str form)
      (is (thrown? clojure.lang.Compiler$CompilerException (collect form))))))

(deftest changing-a-token-replaces-all-its-rules
  (let [before (collect '(do (s/def-theme reloadable-theme)
                             (s/def-token reloadable reloadable-theme "old" "default")))
        after (collect '(do (s/def-theme reloadable-theme)
                            (s/def-token reloadable "new")))]
    (is (str/includes? before ":old;"))
    (is (not (str/includes? after ":old;")))
    (is (= 2 (count (re-seq #":new;" after))))))

(deftest definition-identity-not-values-controls-names
  (collect '(s/def-token stable-name "before"))
  (let [before (get (ns-publics 'uni.style.theme-build-test) 'stable-name)
        name-before @before]
    (collect '(s/def-token stable-name "after"))
    (is (= name-before @before)))
  (is (not= fixture/background
            (do (collect '(s/def-token background "#fff"))
                @(get (ns-publics 'uni.style.theme-build-test) 'background)))))

(deftest root-theme-modifier
  (let [css (collect '(s/root-theme fixture/dark (s/bg "#123")))]
    (testing "applies only when the root element has the theme class"
      (is (re-find (re-pattern (str ":root\\." fixture/dark " \\.[^{]+\\{background-color:#123;\\}")) css))))
  (is (= (s/class-name (s/root-theme fixture/dark (s/bg "#123")))
         (s/class-name (s/root-theme fixture/dark (s/bg "#123")))))
  (testing "requires a def-theme var"
    (is (thrown? clojure.lang.Compiler$CompilerException
                 (collect '(s/root-theme :dark (s/bg "#123")))))))

(deftest alias-errors
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unknown token"
                        (s/token-value :unknown)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unknown token"
                        (tokens/value-in {} :unknown nil)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Circular token alias"
                        (tokens/value-in {:a {:default :b} :b {:default :a}} :a nil))))
