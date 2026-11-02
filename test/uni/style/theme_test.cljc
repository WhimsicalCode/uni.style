(ns uni.style.theme-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])
            [uni.style.core :as s]
            [uni.style.fixtures.themes :as fixture]
            [uni.style.fixtures.token-listing :as listing]))

(deftest portable-definitions
  (is (string? fixture/light))
  (is (re-matches #"us-theme-light-[a-f0-9]{8}" fixture/light))
  (is (keyword? fixture/background))
  (is (nil? (namespace fixture/background)))
  (is (re-matches #"us-background-[a-f0-9]{8}" (name fixture/background)))
  ;; Shared assertions pin the exact same identities in JVM, dev CLJS, and release CLJS.
  (is (= "us-theme-light-fafb47b2" fixture/light))
  (is (= "us-theme-dark-f9f5a360" fixture/dark))
  (is (= :us-background-2734fbb7 fixture/background))
  (is (= (s/bg "#111") (fixture/literal-background-class))))

(deftest all-loaded-tokens
  (let [names (s/all-tokens)]
    (is (vector? names))
    (is (every? keyword? names))
    (is (= names (vec (sort (distinct names)))))
    (is (every? (set names) [fixture/background listing/background]))
    (is (not (contains? (set names) listing/background-sunken)))
    (is (not (contains? (set names) listing/background-plain)))))

(deftest alpha-values
  (is (= "color-mix(in oklab, #000 40%, transparent)"
         (s/token-value fixture/overlay)))
  (is (= "color-mix(in oklab, #fff 20%, transparent)"
         (s/token-value fixture/overlay fixture/dark)))
  (is (= "color-mix(in oklab, #000 40%, transparent)"
         (s/token-value fixture/overlay fixture/light)))
  (is (= (str "color-mix(in oklab, var(--" (name fixture/background) ") 50%, transparent)")
         (s/token-value fixture/tinted-background)))
  (is (= "0 1px 2px color-mix(in oklab, #000 20%, transparent)"
         fixture/shadow (s/token-value fixture/shadow-token)))
  (is (= "color-mix(in oklab, color-mix(in oklab, #fff 50%, transparent) 25%, transparent)"
         (s/str (s/alpha (s/alpha "#fff" 50) 25))))
  (is (= "color-mix(in oklab, #fff 0%, transparent)" (s/str (s/alpha "#fff" 0))))
  (is (= "color-mix(in oklab, #fff 100%, transparent)" (s/str (s/alpha "#fff" 100)))))

(deftest token-css-export
  (is (= "@layer theme,base,components,variants;"
         s/layer-priority-css (s/str s/layer-priority-css)))
  (testing "nil exports defaults; theme classes export overrides or defaults"
    (is (= (str "@layer theme{:root{--" (name fixture/background) ":#fff;}}")
           (s/token-css nil [fixture/background]))))
  (testing "selection is sorted and deduplicated, and numbers stay unitless"
    (let [expected (str "@layer theme{:root." fixture/dark ",." fixture/dark
                        "{--" (name fixture/background) ":#111;--" (name fixture/factor) ":0.5;}}")]
      (is (= expected (s/token-css fixture/dark [fixture/factor fixture/background fixture/factor])))
      (is (= expected (s/token-css fixture/dark #{fixture/background fixture/factor})))
      (is (= expected (s/token-css fixture/dark (list fixture/factor fixture/background))))))
  (testing "a theme without an override emits the mandatory default"
    (is (= (str "@layer theme{:root." fixture/light ",." fixture/light
                "{--" (name fixture/radius) ":8px;}}")
           (s/token-css fixture/light [fixture/radius]))))
  (testing "empty selections emit no CSS"
    (doseq [selection [nil [] #{}]]
      (is (= "" (s/token-css nil selection)))
      (is (= "" (s/token-css fixture/dark selection))))))

(deftest token-css-all-loaded
  (doseq [theme [nil fixture/light fixture/dark]]
    (let [css (s/token-css theme)]
      (is (= (s/token-css theme (s/all-tokens)) css))
      (is (not= "" css))
      (is (= "" (s/token-css theme nil)))
      (is (= "" (s/token-css theme [])))))
  (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                        #"Unknown theme" (s/token-css "unknown-theme"))))

(deftest token-css-preserves-references
  (testing "CSS aliases are not resolved or implicitly added to the selection"
    (is (= (str "@layer theme{:root." fixture/dark ",." fixture/dark
                "{--" (name fixture/alias-background) ":var(--" (name fixture/background) ");}}")
           (s/token-css fixture/dark [fixture/alias-background]))))
  (testing "CSS expressions are emitted unchanged"
    (is (= (str "@layer theme{:root{--" (name fixture/overlay)
                ":color-mix(in oklab, #000 40%, transparent);}}")
           (s/token-css nil [fixture/overlay])))))

(deftest token-css-validation
  (doseq [theme [:dark "unknown-theme"]]
    (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                          #"Unknown theme" (s/token-css theme [fixture/background]))))
  (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                        #"Unknown theme" (s/token-css "unknown-theme" [])))
  (doseq [token [:unknown nil "not-a-token"]]
    (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                          #"Unknown token" (s/token-css nil [token]))))
  (doseq [selection [fixture/background {}]]
    (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                          #"collection of token keywords" (s/token-css nil selection)))))

(deftest declared-token-values
  (is (= "#111" (s/token-value fixture/background fixture/dark)))
  (is (= "#fff" (s/token-value fixture/background fixture/light)))
  (is (= "#fff" (s/token-value fixture/background)))
  (is (= "#fff" (s/token-value fixture/background nil)))
  (is (= "8px" (s/token-value fixture/radius fixture/dark)))
  (is (= 0.5 (s/token-value fixture/factor)))
  (testing "aliases follow the requested theme, not the alias's default theme"
    (is (= "#111" (s/token-value fixture/alias-background fixture/dark)))
    (is (= "#fff" (s/token-value fixture/alias-background fixture/light)))))
