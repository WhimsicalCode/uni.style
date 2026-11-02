(ns uni.style.runtime-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [uni.style.core :as s]))

(def ^:const background-token :example-bg)

(deftest constant-token-test
  (is (= "bg--example-bg" (s/bg background-token))))

(deftest class-composition-test
  (testing "later declarations replace the same property"
    (is (= "p-8" (s/class-name (s/c (s/p 4) (s/p 8)))))
    (is (= "p-8" (s/class-name (s/c (s/pr 4) (s/p 8)))))
    (is (= "p-8 pr-4" (s/class-name (s/c (s/p 8) (s/pr 4))))))
  (testing "nested composition preserves metadata until serialization"
    (let [classes (s/c (s/c (s/p 4) (s/flex)) (s/p 8))]
      (is (s/uni-style? classes))
      (is (= "flex p-8" (s/class-name classes)))
      (is (= "flex p-8" (clj->js classes)))))
  (testing "conditions and layers remain independent"
    (is (= "p-4 hover:p-8"
           (s/class-name (s/c (s/p 4) (s/hover (s/p 8))))))
    (let [base (s/base (s/p 4))
          classes (s/c base (s/p 8))]
      (is (= (str (s/class-name base) " p-8") (s/class-name classes))))))
