(ns uni.style.preflight-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [uni.style.preflight :as preflight]))

(def resolve-options
  (ns-resolve 'uni.style.preflight 'resolve-options))

(def test-env
  {:line 10
   :column 4
   :ns {:name 'uni.style.preflight-fixture
        :meta {:file "uni/style/preflight_fixture.cljs"}}})

(defn- expansion [options]
  (macroexpand-1 `(preflight/import-preflight ~options)))

(defn- base-forms [expanded]
  (->> (rest expanded)
       (filter #(and (seq? %)
                     (= 'uni.style.core/def-base (first %))))))

(defn- styles-for [forms selector]
  (->> forms
       (some (fn [[_ actual-selector & styles]]
               (when (= selector actual-selector)
                 styles)))))

(defn- style-form [styles macro-sym]
  (some #(when (= macro-sym (first %)) %) styles))

(defn- raw-declaration [styles]
  (->> styles
       (keep #(when (= 'uni.style.core/raw (first %))
                (second %)))
       (apply merge)))

(deftest resolve-preflight-options-test
  (testing "defaults"
    (is (= {:font-family nil
            :font-feature-settings "normal"
            :font-variation-settings "normal"
            :mono-font-family nil
            :mono-font-feature-settings "normal"
            :mono-font-variation-settings "normal"}
           (resolve-options test-env {}))))

  (testing "supported options"
    (is (= {:font-family "Inter"
            :font-feature-settings "'cv02'"
            :font-variation-settings "'opsz' 14"
            :mono-font-family "Berkeley Mono"
            :mono-font-feature-settings "'zero'"
            :mono-font-variation-settings "'slnt' 0"}
           (resolve-options
             test-env
             {:font-family "Inter"
              :font-feature-settings "'cv02'"
              :font-variation-settings "'opsz' 14"
              :mono-font-family "Berkeley Mono"
              :mono-font-feature-settings "'zero'"
              :mono-font-variation-settings "'slnt' 0"}))))

  (testing "invalid options"
    (doseq [options [nil
                     {:unknown true}
                     {:layer :variants}
                     {:font-family ""}
                     {:font-feature-settings nil}
                     {:mono-font-family 42}]]
      (is (thrown? clojure.lang.ExceptionInfo
                   (resolve-options test-env options))))))

(deftest import-preflight-expansion-test
  (let [forms (base-forms (expansion {:font-family "Inter"
                                      :mono-font-family "Berkeley Mono"}))
        html-styles (styles-for forms "html, :host")
        code-styles (styles-for forms "code, kbd, samp, pre")
        html-raw (raw-declaration html-styles)
        code-raw (raw-declaration code-styles)]
    (is (= 36 (count forms)))
    (is (= (str "Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, "
                "'Helvetica Neue', 'Noto Sans', Arial, sans-serif, 'Apple Color Emoji', "
                "'Segoe UI Emoji', 'Segoe UI Symbol', 'Noto Color Emoji'")
           (second (style-form html-styles 'uni.style.core/font))))
    (is (= (str "Berkeley Mono, ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "
                "'Liberation Mono', 'Courier New', monospace")
           (second (style-form code-styles 'uni.style.core/font))))
    (is (= "normal" (:font-feature-settings html-raw)))
    (is (= "normal" (:font-variation-settings code-raw)))
    (is (= '[uni.style.core/box-border
             uni.style.core/m
             uni.style.core/p
             uni.style.core/raw]
           (map first (styles-for forms
                                  "*, ::after, ::before, ::backdrop, ::file-selector-button"))))
    (is (some (fn [[_ selector & style-forms]]
                (let [supports-form (first style-forms)]
                  (and (= "::placeholder" selector)
                       (= 'uni.style.core/supports (first supports-form))
                       (= "(not (-webkit-appearance: -apple-pay-button)) or (contain-intrinsic-size: 1px)"
                          (second supports-form))
                       (= 'uni.style.core/color (first (nth supports-form 2))))))
              forms)))

  (testing "the default stacks omit only the custom family"
    (let [forms (base-forms (expansion {}))
          html-font (style-form (styles-for forms "html, :host") 'uni.style.core/font)
          code-font (style-form (styles-for forms "code, kbd, samp, pre") 'uni.style.core/font)]
      (is (str/starts-with? (second html-font) "-apple-system, BlinkMacSystemFont"))
      (is (str/starts-with? (second code-font) "ui-monospace, SFMono-Regular")))))
