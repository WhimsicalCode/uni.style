(ns uni.style.theme-runtime-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [uni.style.fixtures.themes :as fixture]
            [uni.style.impl.theme-runtime :as runtime]))

(defn- host []
  (let [classes (js/Set. #js ["unrelated" "us-theme-server-old"])
        listeners (js/Set.)
        query #js {:matches false
                   :addEventListener (fn [_ listener] (.add listeners listener))
                   :removeEventListener (fn [_ listener] (.delete listeners listener))}]
    (set! (.-remove classes) #(.delete classes %))
    {:classes classes
     :listeners listeners
     :query query
     :root #js {:classList classes}
     :change! (fn [dark?]
                (set! (.-matches query) dark?)
                (.forEach listeners #(% query)))}))

(deftest theme-controller-lifecycle
  (let [{:keys [classes listeners query root change!]} (host)
        {:keys [set-theme! set-theme-system!]} (runtime/controller root (constantly query))]
    (set-theme! fixture/dark)
    (is (.has classes "unrelated"))
    (is (not (.has classes "us-theme-server-old")))
    (is (.has classes fixture/dark))
    (set-theme-system! fixture/light fixture/dark)
    (is (.has classes fixture/light))
    (is (not (.has classes fixture/dark)))
    (change! true)
    (is (.has classes fixture/dark))
    (set-theme-system! fixture/light fixture/dark)
    (is (= 1 (.-size listeners)))
    (testing "invalid calls leave the current subscription intact"
      (is (thrown? js/Error (set-theme! "unknown")))
      (is (= 1 (.-size listeners))))
    (set-theme! fixture/light)
    (is (zero? (.-size listeners)))
    (change! false)
    (change! true)
    (is (.has classes fixture/light))
    (set-theme! nil)
    (is (= ["unrelated"] (vec (array-seq (js/Array.from classes)))))
    (set-theme-system! fixture/light nil)
    (is (not (.has classes fixture/light)))
    (change! false)
    (is (.has classes fixture/light))
    (set-theme-system! nil fixture/dark)
    (is (not (.has classes fixture/light)))
    (change! true)
    (is (.has classes fixture/dark))
    (set-theme-system! nil nil)
    (is (zero? (.-size listeners)))
    (is (= ["unrelated"] (vec (array-seq (js/Array.from classes)))))))

(deftest missing-host-apis
  (let [{:keys [root classes]} (host)
        c (runtime/controller root nil)]
    ((:set-theme-system! c) fixture/light fixture/dark)
    (is (.has classes fixture/light)))
  (let [c (runtime/controller nil nil)]
    (is (nil? ((:set-theme! c) fixture/dark)))
    (is (nil? ((:set-theme-system! c) fixture/light fixture/dark))))
  (runtime/set-theme! nil)
  (runtime/set-theme-system! nil nil))
