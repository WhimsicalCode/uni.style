(ns uni.style.impl.oklch
  "Dependency-free conversion of numeric palette OKLCH literals to sRGB.")

(defn parse-literal
  "Parse palette OKLCH as [lightness chroma hue alpha], or nil.
   Lightness and optional alpha are percentages in 0–100, returned in 0–1.
   Chroma and hue must be finite, non-negative numbers. Expressions, CSS
   declarations, markup, and variable alpha are not accepted."
  [color]
  (when-let [[_ l c h alpha]
             (and (string? color)
                  (re-matches #"oklch\((\d+(?:\.\d+)?)% (\d+(?:\.\d+)?) (\d+(?:\.\d+)?)(?: / (\d+(?:\.\d+)?)%)?\)" color))]
    (let [[l c h alpha] (mapv #?(:clj parse-double :cljs js/parseFloat) [l c h (or alpha "100")])]
      (when (and (<= 0 l 100) (<= 0 alpha 100)
                 (not (infinite? c)) (not (infinite? h)))
        [(/ l 100) c h (/ alpha 100)]))))

(defn- round [x]
  #?(:clj (Math/round (double x)) :cljs (js/Math.round x)))

(defn- delinearize [c]
  (if (<= c 0.0031308)
    (* 12.92 c)
    (- (* 1.055 #?(:clj (Math/pow c (/ 1 2.4))
                   :cljs (js/Math.pow c (/ 1 2.4))))
       0.055)))

(defn rgb-bytes
  "Convert [lightness chroma hue] to rounded sRGB bytes, clipping to gamut."
  [[lightness chroma hue]]
  (let [radians (* hue (/ #?(:clj Math/PI :cljs js/Math.PI) 180))
        a (* chroma #?(:clj (Math/cos radians) :cljs (js/Math.cos radians)))
        b (* chroma #?(:clj (Math/sin radians) :cljs (js/Math.sin radians)))
        l (+ lightness (* 0.3963377774 a) (* 0.2158037573 b))
        m (+ lightness (* -0.1055613458 a) (* -0.0638541728 b))
        s (+ lightness (* -0.0894841775 a) (* -1.2914855480 b))
        l (* l l l)
        m (* m m m)
        s (* s s s)]
    (mapv #(round (* 255 (-> (delinearize %) (max 0) (min 1))))
          [(+ (* 4.0767416621 l) (* -3.3077115913 m) (* 0.2309699292 s))
           (+ (* -1.2684380046 l) (* 2.6097574011 m) (* -0.3413193965 s))
           (+ (* -0.0041960863 l) (* -0.7034186147 m) (* 1.7076147010 s))])))

(defn- byte-hex [n]
  (let [digits "0123456789ABCDEF"]
    (str (nth digits (quot n 16)) (nth digits (mod n 16)))))

(defn rgb-hex
  "Format RGB bytes as #RRGGBB, with an alpha byte when alpha is below 1."
  [rgb alpha]
  (str "#" (apply str (map byte-hex rgb))
       (when (< alpha 1) (byte-hex (round (* 255 alpha))))))

(defn- require-literal [color]
  (or (parse-literal color)
      (throw (ex-info "Expected a numeric palette OKLCH literal" {:color color}))))

(defn hex [color]
  (let [[_ _ _ alpha :as lcha] (require-literal color)]
    (rgb-hex (rgb-bytes lcha) alpha)))

(defn rgba [color]
  (let [[_ _ _ alpha :as lcha] (require-literal color)
        [r g b] (rgb-bytes lcha)
        alpha-text #?(:clj (.toPlainString (.stripTrailingZeros (bigdec alpha)))
                      :cljs (str alpha))]
    (str "rgba(" r "," g "," b "," alpha-text ")")))
