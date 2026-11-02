(ns uni.style.build-reload-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [uni.style.build :as build]
            [uni.style.core :as s])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute FileTime]))

(defn- write-source! [file spacing]
  (let [modified (+ (.lastModified file) 1000)]
    (spit file (pr-str `(s/p ~spacing)))
    (Files/setLastModifiedTime (.toPath file) (FileTime/fromMillis modified))))

(defn- with-workspace [f]
  (let [dir (.toFile (Files/createTempDirectory "uni-style-reload-" (make-array FileAttribute 0)))
        source (io/file dir "view.clj")
        output (io/file dir "public" "bundle.css")
        builds- (atom 0)
        compile! (fn []
                   (swap! builds- inc)
                   (doseq [file (sort (file-seq dir))
                           :when (and (.isFile file) (re-find #"\.cljc?$" (.getName file)))]
                     (load-file (.getPath file))))]
    (try
      (write-source! source 4)
      (f {:dir dir :source source :output output :builds- builds-
          :options {:source-paths [dir] :compile-fn compile! :output-to output}})
      (finally
        (doseq [file (reverse (file-seq dir))]
          (io/delete-file file :silently true))))))

(deftest request-triggered-rebuilds
  (with-workspace
    (fn [{:keys [dir source output builds- options]}]
      (let [app (build/wrap-css-reload (fn [request] {:request request :css (slurp output)}) options)]
        (is (not (.exists output)) "constructing middleware does not build")
        (let [response (app :first)]
          (is (= :first (:request response)))
          (is (str/includes? (:css response) "padding:4px;")))
        (app :unchanged)
        (is (= 1 @builds-) "unchanged requests reuse CSS")
        (write-source! source 8)
        (let [css (:css (app :changed))]
          (is (str/includes? css "padding:8px;"))
          (is (not (str/includes? css "padding:4px;"))))
        (is (= 2 @builds-))
        (spit (io/file dir "notes.txt") "not a Clojure source")
        (app :ignored)
        (is (= 2 @builds-))
        (io/delete-file output)
        (is (str/includes? (:css (app :missing-output)) "padding:8px;"))
        (is (= 3 @builds-))))))

(deftest source-additions-and-deletions
  (with-workspace
    (fn [{:keys [dir builds- options output]}]
      (let [app (build/wrap-css-reload (fn [_] (slurp output)) options)
            extra (io/file dir "extra.cljc")]
        (app {})
        (write-source! extra 12)
        (is (str/includes? (app {}) "padding:12px;"))
        (io/delete-file extra)
        (is (not (str/includes? (app {}) "padding:12px;")))
        (is (= 3 @builds-))))))

(deftest failed-builds-are-retried
  (with-workspace
    (fn [{:keys [dir source output builds- options]}]
      (let [fail?- (atom false)
            handled- (atom 0)
            compile! (:compile-fn options)
            app (build/wrap-css-reload
                  (fn [_] (swap! handled- inc))
                  (assoc options :compile-fn #(do (compile!)
                                                  (when @fail?-
                                                    (throw (ex-info "Compilation failed" {}))))))]
        (app {})
        (let [previous (slurp output)]
          (write-source! source 8)
          (reset! fail?- true)
          (dotimes [_ 2]
            (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Compilation failed" (app {})))
            (is (= previous (slurp output))))
          (is (= 3 @builds-))
          (is (= 1 @handled-) "never render against a failed build")
          (reset! fail?- false)
          (is (= 2 (app {})) "retry even without another source edit")
          (is (= 4 @builds-))
          (is (str/includes? (slurp output) "padding:8px;"))
          (is (not (str/includes? (slurp output) "padding:4px;")))
          (is (not-any? #(str/starts-with? (.getName %) ".uni-style-") (file-seq dir))))))))

(deftest initial-build-failure
  (with-workspace
    (fn [{:keys [source output options]}]
      (spit source "(")
      (let [app (build/wrap-css-reload (fn [_] (slurp output)) options)]
        (is (thrown? clojure.lang.Compiler$CompilerException (app {})))
        (is (not (.exists output)))
        (write-source! source 4)
        (is (str/includes? (app {}) "padding:4px;"))))))

(deftest concurrent-requests-share-a-build
  (with-workspace
    (fn [{:keys [builds- output options]}]
      (let [entered (promise)
            continue (promise)
            compile! (:compile-fn options)
            app (build/wrap-css-reload
                  (fn [_] (slurp output))
                  (assoc options :compile-fn #(do (deliver entered true)
                                                  @continue
                                                  (compile!))))
            requests (mapv (fn [_] (future (app {}))) (range 8))]
        (try
          (is (= true (deref entered 5000 :timeout)))
          (finally (deliver continue true)))
        (doseq [request requests]
          (is (str/includes? (deref request 10000 "timeout") "padding:4px;")))
        (is (= 1 @builds-))))))

(deftest asynchronous-handler-arity
  (with-workspace
    (fn [{:keys [output options]}]
      (let [result (promise)
            app (build/wrap-css-reload
                  (fn [request respond _raise]
                    (respond {:request request :css (slurp output)}))
                  options)]
        (app :async #(deliver result %) #(deliver result %))
        (is (= :async (:request @result)))
        (is (str/includes? (:css @result) "padding:4px;"))))))

(deftest explicit-configuration-required
  (doseq [options [{} {:source-paths [] :compile-fn identity :output-to "bundle.css"}]]
    (testing (pr-str options)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"CSS reload requires"
                            (build/wrap-css-reload identity options))))))
