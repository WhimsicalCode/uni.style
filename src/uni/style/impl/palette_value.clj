(ns uni.style.impl.palette-value
  "Compile-time color argument normalization and utility naming."
  (:require [clojure.string :as str]
            [uni.style.impl.css :as css]
            [uni.style.impl.naming :as naming]
            [uni.style.impl.value :as value]))

(defn- percentage [label v]
  (when-not (and (value/finite-number? v) (<= 0 v 100))
    (throw (ex-info (str label " must be a finite number between 0 and 100") {:value v})))
  ;; Six decimal places are plenty for a palette level or percentage. Normalize
  ;; before both naming and interpolation so 60 and 60.0 share an identity.
  (/ (Math/round (* (double v) 1000000)) 1000000.0))

(defn arguments [level alpha]
  [(percentage "Color level" level)
   (if (keyword? alpha)
     (do (css/value-css alpha alpha) alpha)
     (percentage "Color alpha" alpha))])

(defn number-text [v]
  (let [n (Math/round (* (double v) 1000000))
        whole (quot n 1000000)
        fraction (mod n 1000000)]
    (str whole
         (when (pos? fraction)
           (str "." (-> (str (+ 1000000 fraction))
                        (subs 1)
                        (str/replace #"0+$" "")))))))

(defn color-name [prefix level alpha]
  (str prefix "-" (str/replace (number-text level) "." "_")
       (cond
         (keyword? alpha) (str "-a-var-" (naming/content-hash alpha))
         (not= 100.0 alpha) (str "-a" (str/replace (number-text alpha) "." "_")))))
