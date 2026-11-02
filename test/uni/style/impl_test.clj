(ns uni.style.impl-test
  (:require [cljs.analyzer :as ana]
            [cljs.env :as env]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [are deftest is testing]]
            [cognitect.transit :as transit]
            [uni.style.impl.build :as build]
            [uni.style.impl.constant :as constant]
            [uni.style.impl.naming :as naming]
            [uni.style.impl.registry :as registry]
            [uni.style.impl.serialize :as serialize])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(def fixture-env
  {:ns {:meta {:file "uni/style/runtime.cljs"}}
   :line 1
   :column 1})

(defn- register-atomic [compiler ns-sym class-name declaration]
  (binding [env/*compiler* compiler]
    (registry/register-atomic! fixture-env ns-sym class-name declaration))
  @compiler)

(defn- build-state [compiler-env current-namespaces]
  (let [source-ids (mapv #(keyword (str "source-" %)) (range (count current-namespaces)))]
    {:build-sources source-ids
     :sources (into {}
                    (map (fn [source-id ns-sym]
                           [source-id {:ns ns-sym}])
                         source-ids current-namespaces))
     :compiler-env compiler-env}))

(defn- compiler-registry [compiler ns-sym]
  (registry/build-registry (build-state @compiler [ns-sym])))

(deftest compile-time-constant-resolution
  (are [value] (= value (constant/resolve-compile-time-constant {} value))
    nil false "literal" :keyword 42)
  (binding [env/*compiler* (env/default-compiler-env)
            ana/*cljs-ns* 'uni.style.impl-test.synthetic]
    (ana/load-core)
    (let [analyzer-env (assoc (ana/empty-env) :ns {:name ana/*cljs-ns*})]
      (ana/analyze analyzer-env '(def ^:const spacing 8))
      (is (= 8 (constant/resolve-compile-time-constant analyzer-env 'spacing)))
      (is (= "space-8"
             (constant/resolve-compile-time-constant analyzer-env
                                                     '(str "space-" spacing))))
      (let [form '(runtime-value)]
        (try
          (constant/resolve-compile-time-constant analyzer-env form)
          (is false "expected runtime form rejection")
          (catch clojure.lang.ExceptionInfo e
            (is (:dynamic? (ex-data e)))
            (is (= form (:form (ex-data e))))))))))

(deftest hashes-are-canonical-and-fixed-width
  (doseq [content ["" "abc" [:foo :bar] {:color :red :padding 10}
                   "1" "2" "3" "4" "5" "6" "7" "8" "9" "10"]]
    (is (re-matches #"[0-9a-f]{8}" (naming/content-hash content))))
  (is (= (naming/content-hash {:a 1 :b 2})
         (naming/content-hash (array-map :b 2 :a 1)))))

(deftest static-css-serialization
  (are [expected property value] (= expected (serialize/with-unit property value))
    "10px" :width 10
    0 :width 0
    1 :flex 1
    "flex" :display :flex)
  (is (= ".hover\\:border-1:hover{border-width:1px;}"
         (serialize/atomic-css "hover:border-1" {:border-width 1} ["&:hover"] nil)))
  (is (= "@layer base,components;@layer components{@container (max-width: 300px){.control\\:states:hover,.control\\:states:focus{display:none;}}}"
         (serialize/atomic-css "control:states" {:display :none}
                               ["@container (max-width: 300px)" "&:hover, &:focus"]
                               {:name "components" :order ["base" "components"]})))
  (is (= "html, body{margin:0px;padding:0px;}"
         (serialize/selector-css "html, body"
                                 [{:modifiers []
                                   :declaration (array-map :margin "0px" :padding "0px")}]
                                 nil)))
  (is (= "@media (min-width: 1px){.wide{display:block;}}"
         (serialize/atomic-css "wide" {:display :block}
                               [" @media (min-width: 1px)"] nil)))
  (is (= ".peer:is([open], :popover-open, :open) ~ .peer-open{opacity:0.5;}"
         (serialize/atomic-css "peer-open" {:opacity 0.5}
                               [".peer:is([open], :popover-open, :open) ~ &"] nil)))
  (is (= ".attribute[data-label=\"a,b\"]{display:block;}"
         (serialize/atomic-css "attribute" {:display :block}
                               ["&[data-label=\"a,b\"]"] nil)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"must include `&`"
                        (serialize/atomic-css "unscoped" {:display :block}
                                              ["button"] nil)))
  (doseq [declaration [{:color 'runtime-color}
                       {:nested {:color :red}}
                       {:opacity ##Inf}]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"static CSS values"
                          (serialize/declaration-css declaration)))))

(deftest registrations-live-in-analyzer-namespace-data-and-detect-conflicts
  (let [compiler (env/default-compiler-env)
        ns-sym 'uni.style.impl-test.registration]
    (binding [env/*compiler* compiler]
      (registry/register-atomic! fixture-env ns-sym "display" {:display :block})
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"conflicting declarations"
                            (registry/register-atomic! fixture-env ns-sym "display"
                                                       {:display :none}))))
    (is (= {:css-str ".display{display:block;}"
            :file "uni/style/runtime.cljs"
            :loc {:column 1 :line 1}
            :atomic? true}
           (get-in @compiler
                   [:cljs.analyzer/namespaces ns-sym
                    :uni.style.impl.registry/styles "display"])))))

(deftest build-registry-includes-only-current-build-sources
  (let [compiler (env/default-compiler-env)
        current-ns 'uni.style.impl-test.current
        stale-ns 'uni.style.impl-test.stale]
    (register-atomic compiler current-ns "current" {:display :block})
    (register-atomic compiler stale-ns "stale" {:display :none})
    (is (= #{current-ns}
           (set (keys (registry/build-registry
                        (build-state @compiler [current-ns]))))))))

(deftest esm-hook-writes-the-build-namespaces-in-dependency-order
  (let [compiler (env/default-compiler-env)
        first-ns 'uni.style.impl-test.first
        second-ns 'uni.style.impl-test.second
        stale-ns 'uni.style.impl-test.esm-stale
        output (java.io.File/createTempFile "uni-style-esm" ".css")]
    (try
      (register-atomic compiler second-ns "second" {:margin 2})
      (register-atomic compiler first-ns "first" {:padding 1})
      (register-atomic compiler stale-ns "stale" {:display :none})
      (build/esm-hook @compiler [first-ns second-ns] {:output-to (str output)})
      (let [css (slurp output)]
        (is (str/includes? css ".first"))
        (is (str/includes? css ".second"))
        (is (not (str/includes? css ".stale")))
        (is (< (str/index-of css ".first") (str/index-of css ".second"))))
      (finally
        (.delete output)))))

(deftest esm-hook-after-repl-evals-writes-only-changed-styles
  (let [compiler (env/default-compiler-env)
        ns-sym 'uni.style.impl-test.repl
        output (java.io.File/createTempFile "uni-style-esm-repl" ".css")
        hook #(build/esm-hook @compiler [ns-sym] {:output-to (str output)} :repl-eval [ns-sym])]
    (try
      (register-atomic compiler ns-sym "before" {:padding 1})
      (build/esm-hook @compiler [ns-sym] {:output-to (str output)})
      (.delete output)
      (hook)
      (is (not (.exists output)) "unchanged styles aren't written again")
      (register-atomic compiler ns-sym "after" {:margin 1})
      (hook)
      (is (str/includes? (slurp output) ".after"))
      (finally
        (.delete output)))))

(deftest esm-hook-rejects-css-options-read-from-the-compiler-options
  (let [compiler (env/default-compiler-env)
        ns-sym 'uni.style.impl-test.misplaced
        output (java.io.File/createTempFile "uni-style-esm-misplaced" ".css")
        error #(#'build/esm-hook-options-error (merge {:output-to (str output)} %))]
    (try
      (.delete output)
      (is (nil? (error {})))
      (is (str/includes? (error {:short-class-names true})
                         ":short-class-names belong under the compiler options' :css-options"))
      (is (str/includes? (error {:qualified-component-classes true :short-class-names false})
                         ":qualified-component-classes, :short-class-names belong under"))
      (testing "the hook writes no CSS with misplaced options"
        (register-atomic compiler ns-sym "misplaced" {:padding 1})
        (build/esm-hook @compiler [ns-sym] {:output-to (str output) :short-class-names true})
        (is (not (.exists output))))
      (finally
        (.delete output)))))

(deftest compiler-registries-with-the-same-namespace-are-isolated
  (let [first-compiler (env/default-compiler-env)
        second-compiler (env/default-compiler-env)
        ns-sym 'uni.style.impl-test.isolated]
    (register-atomic first-compiler ns-sym "first" {:color :red})
    (register-atomic second-compiler ns-sym "second" {:color :blue})
    (is (= #{"first"} (set (keys (get (compiler-registry first-compiler ns-sym) ns-sym)))))
    (is (= #{"second"} (set (keys (get (compiler-registry second-compiler ns-sym) ns-sym)))))))

(deftest analyzer-snapshots-bundle-their-matching-styles
  (let [compiler-a (env/default-compiler-env)
        compiler-b (env/default-compiler-env)
        ns-sym 'uni.style.impl-test.snapshot
        output (io/file (System/getProperty "java.io.tmpdir")
                        (str "uni-style-snapshot-" (random-uuid) ".css"))]
    (try
      (register-atomic compiler-a ns-sym "revision-a" {:display :block})
      (register-atomic compiler-b ns-sym "revision-b" {:display :none})
      (doseq [[compiler expected unexpected]
              [[compiler-a "revision-a" "revision-b"]
               [compiler-b "revision-b" "revision-a"]
               [compiler-a "revision-a" "revision-b"]]]
        (build/bundle-css! (compiler-registry compiler ns-sym)
                           {:output-to (.getPath output)})
        (let [css (slurp output)]
          (is (str/includes? css expected))
          (is (not (str/includes? css unexpected)))))
      (finally
        (io/delete-file output :silently true)))))

(deftest fresh-analyzer-namespace-does-not-retain-removed-styles
  (let [old-compiler (env/default-compiler-env)
        fresh-compiler (env/default-compiler-env)
        ns-sym 'uni.style.impl-test.fresh]
    (register-atomic old-compiler ns-sym "removed" {:display :none})
    (register-atomic fresh-compiler ns-sym "retained" {:display :block})
    (is (= #{"retained"}
           (set (keys (get (compiler-registry fresh-compiler ns-sym) ns-sym)))))))

(deftest analyzer-style-metadata-survives-transit-round-trip
  (let [compiler (env/default-compiler-env)
        ns-sym 'uni.style.impl-test.transit
        _ (register-atomic compiler ns-sym "cached" {:display :block})
        output (ByteArrayOutputStream.)
        _ (transit/write (transit/writer output :json) @compiler)
        restored (transit/read (transit/reader (ByteArrayInputStream. (.toByteArray output)) :json))]
    (is (= (registry/build-registry (build-state @compiler [ns-sym]))
           (registry/build-registry (build-state restored [ns-sym]))))))

(deftest bundles-follow-dependency-order-and-deduplicate-atomics
  (let [output (io/file (System/getProperty "java.io.tmpdir")
                        (str "uni-style-" (random-uuid) ".css"))
        atomic (fn [file css]
                 {:css-str css :atomic? true :file file :loc {:line 1 :column 1}})
        styles {'fancy.input
                {"reset" (atomic "fancy/input.cljs" ".reset{padding:0;}")
                 "shared" (atomic "fancy/input.cljs" ".shared{display:block;}")}
                'command.views
                {"override" (atomic "command/views.cljs" ".override{padding-left:14px;}")
                 "shared" (atomic "command/views.cljs" ".shared{display:block;}")}}]
    (try
      (build/bundle-css! styles {:output-to (.getPath output)
                                 :ns-order {'fancy.input 0 'command.views 1}})
      (is (= ".reset{padding:0;}.shared{display:block;}.override{padding-left:14px;}"
             (slurp output)))
      (finally
        (io/delete-file output :silently true)))))
