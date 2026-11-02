(ns uni.style.portable-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])
            [clojure.string :as str]
            [uni.style.core :as s]
            [uni.style.fixtures.portable :as fixture]))

(deftest keyword-class-names
  (testing "strings and unnamespaced keywords are used as is"
    (is (= "menu" (s/class-name (s/c :menu))))
    (is (= "menu" (s/class-name (s/c "menu")))))
  (testing "namespaced keywords include their namespace"
    (is (= "app_views_snacks--menu" (s/class-name (s/c :app.views.snacks/menu))))
    (is (= "uni_style_portable-test--menu" (s/class-name (s/c ::menu))))
    (is (not= (s/class-name (s/c :a/menu)) (s/class-name (s/c :b/menu)))))
  (testing "s/c and class modifiers agree on the name"
    (is (= (s/ancestor "uni_style_portable-test--menu" (s/hidden))
           (s/ancestor ::menu (s/hidden))))))

(deftest runtime-composition
  (testing "later declarations replace the same property"
    (is (= (s/p 8) (s/class-name (s/c (s/p 4) (s/p 8)))))
    (is (= (s/p 8) (s/class-name (s/c (s/pr 4) (s/p 8)))))
    (is (= (str (s/p 8) " " (s/pr 4))
           (s/class-name (s/c (s/p 8) (s/pr 4))))))
  (testing "nested composition preserves conflict metadata"
    (let [value (s/c (s/c (s/p 4) (s/flex)) (s/p 8))]
      (is (s/uni-style? value))
      (is (= (str (s/flex) " " (s/p 8)) (s/class-name value)))
      (is (= (s/class-name value) (str value)))
      (is (identical? value (s/c value)))))
  (testing "opaque classes, nested sequences and empty inputs"
    (is (= "legacy other legacy"
           (s/class-name (s/c [:legacy nil [false "" 'other]] "legacy"))))
    (is (= "" (s/class-name (s/c))))
    (is (= "" (s/class-name nil))))
  (testing "modifiers and layers resolve independently"
    (is (= (s/class-name (s/hover (s/p 8)))
           (s/class-name (s/c (s/hover (s/p 4)) (s/hover (s/p 8))))))
    (let [base (s/base (s/p 4))]
      (is (= (str (s/class-name base) " " (s/p 8))
             (s/class-name (s/c base (s/p 8))))))))

(deftest release-class-names-match-on-both-hosts
  (when #?(:clj true :cljs (not ^boolean goog.DEBUG))
    (is (= "us-aa0a7021" (s/p 8)))
    (is (= "us-55f2c1ba" (s/class-name (s/hover (s/p 8)))))
    (is (= "us-e5c293ad" fixture/panel))
    (is (= "us-d253058c" (:primary fixture/appearance)))
    (is (= "us-e7665b1e" fixture/fade-in))))

(deftest stylesheet-expressions
  (is (= "body{color:red;}"
         (let [stylesheet (s/css {"body" (s/color "red")})]
           stylesheet)))
  (is (= "/* inline */body{color:red;}"
         (s/str "/* inline */" (s/css {"body" (s/color "red")})))))

(deftest raw-declaration-constants
  (is (= "p{z-index:8;border-left-color:red;}"
         (s/css {"p" (s/raw {:z-index fixture/doubled-spacing
                             :border-left-color (s/str "r" "ed")})}))))

(deftest concatenated-transition-components
  (is (= (s/transition "opacity var(--duration) var(--ease)")
         (s/transition "opacity " :duration " " :ease))))

(deftest stylesheet-definitions
  (let [expected (str ".document{padding:8px;}"
                      ".document p,.document h2{color:var(--example-text);}"
                      ".document p:hover,.document h2:hover{color:red;}"
                      "@media (min-width: 10px){.document p{font-size:18px;}}"
                      "@layer components{z-panel{color:blue;}}")]
    (is (= expected fixture/document-css))
    (is (= expected (s/str fixture/document-css)))
    (is (not (str/includes? fixture/document-css s/layer-priority-css)))
    (is (not (str/includes? fixture/document-css "--example-text:")))))

(deftest stylesheet-selector-lists
  (is (= ":is(.a,.b):hover,[title=\"],x\"]:hover,.escaped\\,comma:hover{color:red;}"
         fixture/selector-list-css)))

(deftest reusable-style-vectors
  (is (= (s/class-name
           (s/focus-visible
             (s/outline-solid)
             (s/outline-color :focus-ring "blue")
             (s/outline-width :focus-ring-width "3px")
             (s/outline-offset :focus-ring-offset "0px")))
         (s/class-name (s/focus-visible fixture/focus-style)))))

(deftest portable-definitions
  (is (string? fixture/panel))
  (is (string? fixture/compact-panel))
  (is (string? fixture/focus-panel))
  (is (string? fixture/focus-visible-panel))
  (is (string? fixture/fade-in))
  (is (= #{:primary [:large :wide]} (set (keys fixture/appearance))))
  (is (not= (fixture/classes true) (fixture/classes false)))
  (is (= (s/flex) (fixture/utility-class)))
  (is (= (s/p 8) (s/p fixture/doubled-spacing)))
  (is (= (s/bg :example-bg) (s/bg fixture/background-token))))
