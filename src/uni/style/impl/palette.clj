(ns uni.style.impl.palette
  "Literal OKLCH palettes and compile-time utility naming hints."
  (:require [clojure.string :as str]
            [uni.style.impl.constant :as constant]
            [uni.style.impl.host :as host]
            [uni.style.impl.naming :as naming]
            [uni.style.impl.palette-value :as palette-value]
            [uni.style.impl.value :as value]))

;; Scoped to one utility argument. Strings remain ordinary strings on both hosts;
;; this compile-time context remembers their origin without a runtime registry.
(def ^:dynamic *color-names* nil)

(defn validate-anchors [anchors]
  (when-not (and (map? anchors) (seq anchors))
    (throw (ex-info "def-color requires a non-empty map of level -> [lightness chroma hue]" {:anchors anchors})))
  (doseq [[level lch] anchors]
    (when-not (and (value/finite-number? level) (< 0 level 100))
      (throw (ex-info "Color anchor levels must be between 0 and 100, exclusive; 0 is black and 100 is white"
                      {:level level})))
    (when-not (and (vector? lch) (= 3 (count lch))
                   (every? value/finite-number? lch)
                   (<= 0 (first lch) 100) (<= 0 (second lch)))
      (throw (ex-info "Color anchors must be [lightness (0–100), non-negative chroma, hue in degrees]"
                      {:level level :value lch}))))
  (let [normalized (into (sorted-map)
                         (map (fn [[level [l c h]]]
                                [(first (palette-value/arguments level 100)) [l c (mod h 360)]]))
                         anchors)]
    (when-not (and (= (count anchors) (count normalized))
                   (every? #(< 0 % 100) (keys normalized)))
      (throw (ex-info "Color anchor levels must remain distinct and inside 0–100 at six decimal places"
                      {:anchors anchors})))
    normalized))

(defn- endpoint-slope [h1 h2 d1 d2]
  (let [m (/ (- (* (+ (* 2 h1) h2) d1) (* h1 d2)) (+ h1 h2))]
    (cond
      (<= (* m d1) 0) 0
      (and (neg? (* d1 d2)) (> (abs m) (* 3 (abs d1)))) (* 3 d1)
      :else m)))

(defn- slopes [xs ys]
  (let [hs (mapv - (rest xs) xs)
        ds (mapv / (map - (rest ys) ys) hs)]
    (into [(endpoint-slope (hs 0) (hs 1) (ds 0) (ds 1))]
          (concat
            (for [i (range 1 (dec (count xs)))
                  :let [a (ds (dec i)) b (ds i)
                        w1 (+ (* 2 (hs i)) (hs (dec i)))
                        w2 (+ (hs i) (* 2 (hs (dec i))))]]
              (if (<= (* a b) 0) 0 (/ (+ w1 w2) (+ (/ w1 a) (/ w2 b)))))
            [(endpoint-slope (peek hs) (peek (pop hs)) (peek ds) (peek (pop ds)))]))))

(defn- cubic [xs ys i t]
  (let [ms (slopes xs ys)
        h (- (xs (inc i)) (xs i))
        t2 (* t t)
        t3 (* t2 t)]
    (+ (* (+ (* 2 t3) (* -3 t2) 1) (ys i))
       (* (+ t3 (* -2 t2) t) h (ms i))
       (* (+ (* -2 t3) (* 3 t2)) (ys (inc i)))
       (* (- t3 t2) h (ms (inc i))))))

(defn interpolate
  "Shape-preserving cubic lightness/chroma, shortest-arc linear hue.
   Anchors must have passed validate-anchors. Black/white borrow adjacent hue."
  [anchors level]
  ;; Analyzer metadata/cache roundtrips do not preserve sorted-map ordering.
  (let [anchors (into (sorted-map) anchors)
        points (assoc anchors
                      0 [0 0 (nth (val (first anchors)) 2)]
                      100 [100 0 (nth (val (last anchors)) 2)])]
    (if-let [lch (get points level)]
      lch
      (let [xs (vec (keys points))
            ys (vec (vals points))
            i (first (keep-indexed (fn [i x] (when (< level x) (dec i))) xs))
            t (/ (- level (xs i)) (- (xs (inc i)) (xs i)))
            [h1 h2] [(get-in ys [i 2]) (get-in ys [(inc i) 2])]
            ;; Achromatic anchors have no hue; use the chromatic neighbor's hue.
            h1 (if (zero? (get-in ys [i 1])) h2 h1)
            h2 (if (zero? (get-in ys [(inc i) 1])) h1 h2)
            hue (mod (+ h1 (* t (- (mod (+ (- h2 h1) 180) 360) 180))) 360)]
        [(cubic xs (mapv first ys) i t)
         (cubic xs (mapv second ys) i t)
         hue]))))

(defn- css-number [n]
  (-> (String/format java.util.Locale/ROOT "%.6f" (to-array [(double n)]))
      (str/replace #"\.?0+$" "")))

(defn color-css [anchors level alpha]
  (let [[l c h] (interpolate anchors level)]
    (str "oklch(" (css-number l) "% " (css-number c) " " (css-number h)
         (when (not= 100.0 alpha)
           (str " / " (if (keyword? alpha)
                        (value/str-component alpha alpha)
                        (str (palette-value/number-text alpha) "%"))))
         ")")))

(defn def-color* [env sym anchors-form]
  (when-not (host/jvm? env)
    (throw (ex-info "Define color macros in a .clj namespace or the :clj branch of .cljc, then import them with :require-macros"
                    {:symbol sym})))
  (when-not (and (symbol? sym) (nil? (namespace sym)))
    (throw (ex-info "def-color requires an unqualified symbol" {:symbol sym})))
  (let [owner (host/definition-symbol env sym)
        prefix (str (str/replace (name sym) #"[^a-zA-Z0-9_-]" "-")
                    (when-not (= "uni.style.colors" (namespace owner))
                      (str "-" (naming/content-hash [:color (str owner)]))))
        anchors (validate-anchors (constant/resolve-compile-time-constant env anchors-form))
        definition {:prefix prefix :anchors anchors}]
    `(defmacro ~(vary-meta sym assoc :uni.style/color definition)
       "Return this color's literal OKLCH string at compile time.
        Level and optional alpha must be compile-time constants. Numeric alpha is
        a percentage; keyword alpha is a CSS variable (0–1 or a percentage)."
       ([~'level] (color* ~'&env '~definition [~'level]))
       ([~'level ~'alpha] (color* ~'&env '~definition [~'level ~'alpha])))))

(defn color* [env {:keys [prefix anchors]} arg-forms]
  (let [args (mapv #(constant/resolve-compile-time-constant env %) arg-forms)
        [level alpha] (palette-value/arguments (first args) (if (= 1 (count args)) 100 (second args)))
        css (color-css anchors level alpha)]
    (when *color-names*
      (swap! *color-names* update css (fnil conj #{}) (palette-value/color-name prefix level alpha)))
    css))

(defn color-utility
  "Resolve one utility argument, retaining an unambiguous palette name for its exact value.
   Constants and transformed strings with no palette origin keep content-based names."
  [env form prefix label type]
  (let [names (atom {})
        color (binding [*color-names* names]
                (constant/resolve-compile-time-constant env form))
        result (value/color-utility form prefix color label type)
        color-names (get @names color)]
    (cond-> result
            (= 1 (count color-names)) (assoc :class-name (str prefix "-" (first color-names))))))
