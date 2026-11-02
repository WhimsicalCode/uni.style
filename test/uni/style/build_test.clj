(ns uni.style.build-test
  (:require [cljs.env :as cljs.env]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [uni.style.build :as style.build]
            [uni.style.impl.build :as impl.build]
            [uni.style.impl.registry :as registry])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:private fixture-env
  {:ns {:meta {:file "uni/style/runtime.cljs"}}
   :line 1
   :column 1})

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "uni-style-build-test"
                                      (make-array FileAttribute 0))))

(defn- delete-tree! [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (io/delete-file child :silently true))))

(defn- test-build-id [label]
  (keyword (str "uni-style-" label "-" (random-uuid))))

(defn- build-state [build-id stage mode config]
  {:build-id build-id
   :shadow.build/build-id build-id
   :shadow.build/stage stage
   :mode mode
   :shadow.build/config config
   :build-sources []
   :sources {}
   :compiler-env @(cljs.env/default-compiler-env)})

(defn- add-build-source [build-state source-id ns-sym]
  (-> build-state
      (update :build-sources conj source-id)
      (assoc-in [:sources source-id] {:ns ns-sym})))

(defn- register-atomic [build-state ns-sym class-name declaration]
  (let [compiler (atom (:compiler-env build-state))]
    (binding [cljs.env/*compiler* compiler]
      (registry/register-atomic! fixture-env ns-sym class-name declaration))
    (assoc build-state :compiler-env @compiler)))

(deftest atomic-css-sorts-by-condition-and-property-semantics
  (let [dir (temp-dir)
        output (io/file dir "bundle.css")
        entry (fn [css line property-rank property]
                {:atomic? true
                 :css-str css
                 :file "uni/style/runtime.cljs"
                 :loc {:line line :column 1}
                 :sort-meta {:condition-rank [0 ""]
                             :property-rank property-rank
                             :property property}})]
    (try
      (impl.build/bundle-css!
        {'uni.style.fixture
         {"narrow" (entry ".narrow{padding-right:4px;}" 1 1 :padding-right)
          "broad" (entry ".broad{padding:8px;}" 2 0 :padding)}}
        {:output-to (.getPath output)})
      (is (= ".broad{padding:8px;}.narrow{padding-right:4px;}"
             (slurp output)))
      (finally
        (delete-tree! dir)))))

(deftest development-hook-writes-only-css-through-facade
  (let [dir (temp-dir)
        build-id (test-build-id "dev")
        output (io/file dir "bundle.css")
        ns-sym 'uni.style.build-test.dev
        state (-> (build-state
                    build-id :compile-finish :dev
                    {:css-options {:output-to (.getPath output)}})
                  (add-build-source :dev ns-sym)
                  (style.build/register-global-css
                    fixture-env ns-sym "dev-defs" "html{box-sizing:border-box;}"))]
    (try
      (is (= state (style.build/hook state)))
      (is (= "html{box-sizing:border-box;}" (slurp output)))
      (is (not (.exists (io/file (str output ".map")))))
      (finally
        (delete-tree! dir)))))

(deftest release-hook-bundles-build-styles-without-reading-javascript
  (let [dir (temp-dir)
        build-id (test-build-id "release")
        output (io/file dir "bundle.css")
        module (io/file dir "main.js")
        fixture-ns (symbol (str "uni.style.build-test." (name build-id)))
        state (-> (build-state build-id :flush :release
                               {:output-dir (.getPath dir)
                                :css-options {:output-to (.getPath output)}})
                  (assoc :shadow.build.closure/modules [{:output-name "main.js"}])
                  (add-build-source :fixture fixture-ns)
                  (add-build-source :shared 'uni.style.build-test.shared)
                  (register-atomic fixture-ns "keep-atomic" {:display :block})
                  (register-atomic fixture-ns "unreferenced-atomic" {:display :none})
                  (register-atomic 'uni.style.build-test.shared "keep-atomic" {:display :block})
                  (register-atomic 'uni.style.build-test.stale "stale-atomic" {:display :flex})
                  (style.build/register-global-css
                    fixture-env fixture-ns "uni-style-def-base-test"
                    "html,body{margin:0px;}"))]
    (try
      (is (not (.exists module)))
      (is (= state (style.build/hook state)))
      (let [css (slurp output)]
        (is (= 1 (count (re-seq #"\.keep-atomic\{" css))))
        (is (str/includes? css ".keep-atomic{display:block;}"))
        (is (str/includes? css ".unreferenced-atomic{display:none;}"))
        (is (not (str/includes? css "stale-atomic")))
        (is (str/includes? css "html,body{margin:0px;}"))
        (is (not (str/includes? css "sourceMappingURL"))))
      (is (not (.exists (io/file (str output ".map")))))
      (finally
        (delete-tree! dir)))))

(defn- register-definition [state ns-sym sym definition]
  (let [compiler (atom (:compiler-env state))]
    (binding [cljs.env/*compiler* compiler]
      (registry/register-definition! fixture-env ns-sym (:owner definition) definition))
    (-> state
        (assoc :compiler-env @compiler)
        (assoc-in [:compiler-env :cljs.analyzer/namespaces ns-sym :defs sym :uni.style/definition]
                  definition))))

(deftest cached-token-definitions-use-the-standard-build-hook
  (let [ns-sym 'uni.style.build-test.tokens
        theme {:kind :theme :owner "uni.style.build-test.tokens/dark" :name "us-theme-dark-test"}
        token {:kind :token :owner "uni.style.build-test.tokens/background" :name :us-background-test
               :default "#fff" :values {"us-theme-dark-test" "#111"}}
        state (-> (build-state :tokens :flush :release {})
                  (add-build-source :tokens ns-sym)
                  (register-definition ns-sym 'dark theme)
                  (register-definition ns-sym 'background token))
        css (impl.build/bundle-css (registry/build-registry state) {})]
    (is (str/includes? css "@layer theme{:root{--us-background-test:#fff;}}"))
    (is (str/includes? css "@layer theme{:root.us-theme-dark-test,.us-theme-dark-test{--us-background-test:#111;}}"))
    (testing "removed definitions do not survive in the analyzer style registry"
      (is (empty? (impl.build/bundle-css
                    (registry/build-registry
                      (update-in state [:compiler-env :cljs.analyzer/namespaces ns-sym :defs]
                                 dissoc 'background)) {}))))
    (testing "another build sharing the analyzer cache cannot leak these definitions"
      (is (empty? (registry/build-registry (assoc state :build-sources [])))))))

(deftest build-target-registries-are-isolated
  (let [ns-sym 'uni.style.build-test.isolation
        app-state (-> (build-state (test-build-id "app-isolation")
                                   :compile-finish :dev {})
                      (add-build-source :isolation ns-sym)
                      (style.build/register-global-css
                        fixture-env ns-sym "app-only" "html{color:red;}"))
        worker-state (-> (build-state (test-build-id "worker-isolation")
                                      :compile-finish :dev {})
                         (add-build-source :isolation ns-sym)
                         (style.build/register-global-css
                           fixture-env ns-sym "worker-only" "html{color:blue;}"))]
    (is (= #{"app-only"}
           (set (keys (get (registry/build-registry app-state) ns-sym)))))
    (is (= #{"worker-only"}
           (set (keys (get (registry/build-registry worker-state) ns-sym)))))))

(deftest build-facade-exposes-shadow-stages
  (is (= #{:compile-finish :flush}
         (:shadow.build/stages (meta #'style.build/hook)))))
