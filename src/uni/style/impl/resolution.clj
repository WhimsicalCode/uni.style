(ns uni.style.impl.resolution
  "Property expansion, application-order conflicts, and style IR helpers."
  (:refer-clojure :exclude [descendants])
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(def style-ir-key ::style-ir)

(defn style-ir [class-value descriptor]
  (assoc descriptor style-ir-key true :class-value class-value))

(defn style-ir? [value]
  (true? (get value style-ir-key)))

(defn emit-style-ir [ir]
  (let [operations (vec (mapcat identity (:operations ir)))]
    `(uni.style.core/style-class ~(:class-value ir)
                                 ~(hash operations)
                                 (fn [] ~operations))))

(defn- flatten-style-irs [styles]
  (mapcat (fn [style]
            (cond
              (sequential? style) (flatten-style-irs style)
              (or (nil? style) (false? style) (= "" style)) []
              :else [style]))
          styles))

(defn combine-style-irs [styles]
  (let [styles (vec (flatten-style-irs styles))]
    (when-not (every? style-ir? styles)
      (throw (ex-info "Compile-time style composition only accepts uni.style utilities"
                      {:values styles :type :style-composition})))
    (style-ir (str/join " " (map :class-value styles))
              {:operations (vec (mapcat :operations styles))
               :classes (vec (mapcat :classes styles))})))

(def ^:private equal-value-expansions
  {:padding [:padding-inline :padding-block :padding-top :padding-right
             :padding-bottom :padding-left :padding-inline-start
             :padding-inline-end :padding-block-start :padding-block-end]
   :padding-inline [:padding-inline-start :padding-inline-end]
   :padding-block [:padding-block-start :padding-block-end]
   :margin [:margin-inline :margin-block :margin-top :margin-right
            :margin-bottom :margin-left :margin-inline-start
            :margin-inline-end :margin-block-start :margin-block-end]
   :margin-inline [:margin-inline-start :margin-inline-end]
   :margin-block [:margin-block-start :margin-block-end]
   :scroll-padding [:scroll-padding-inline :scroll-padding-block
                    :scroll-padding-top :scroll-padding-right
                    :scroll-padding-bottom :scroll-padding-left
                    :scroll-padding-inline-start :scroll-padding-inline-end
                    :scroll-padding-block-start :scroll-padding-block-end]
   :scroll-padding-inline [:scroll-padding-inline-start :scroll-padding-inline-end]
   :scroll-padding-block [:scroll-padding-block-start :scroll-padding-block-end]
   :scroll-margin [:scroll-margin-inline :scroll-margin-block
                   :scroll-margin-top :scroll-margin-right
                   :scroll-margin-bottom :scroll-margin-left
                   :scroll-margin-inline-start :scroll-margin-inline-end
                   :scroll-margin-block-start :scroll-margin-block-end]
   :scroll-margin-inline [:scroll-margin-inline-start :scroll-margin-inline-end]
   :scroll-margin-block [:scroll-margin-block-start :scroll-margin-block-end]
   :inset [:inset-inline :inset-block :top :right :bottom :left
           :inset-inline-start :inset-inline-end
           :inset-block-start :inset-block-end]
   :inset-inline [:inset-inline-start :inset-inline-end]
   :inset-block [:inset-block-start :inset-block-end]
   :border-width [:border-top-width :border-right-width
                  :border-bottom-width :border-left-width]
   :border-style [:border-top-style :border-right-style
                  :border-bottom-style :border-left-style]
   :border-color [:border-top-color :border-right-color
                  :border-bottom-color :border-left-color]
   :border-radius [:border-top-left-radius :border-top-right-radius
                   :border-bottom-right-radius :border-bottom-left-radius]
   :gap [:row-gap :column-gap]
   :overflow [:overflow-x :overflow-y]
   :overscroll-behavior [:overscroll-behavior-x :overscroll-behavior-y]})

(def ^:private shorthand-descendants
  (merge equal-value-expansions
         {:outline [:outline-width :outline-style :outline-color]
          :flex [:flex-grow :flex-shrink :flex-basis]
          :font [:font-family :font-size :font-style :font-variant :font-weight
                 :font-stretch :line-height]
          :transition [:transition-property :transition-duration
                       :transition-timing-function :transition-delay
                       :transition-behavior]
          :animation [:animation-name :animation-duration :animation-timing-function
                      :animation-delay :animation-iteration-count :animation-direction
                      :animation-fill-mode :animation-play-state :animation-timeline
                      :animation-range-start :animation-range-end]
          :background [:background-image :background-position :background-size
                       :background-repeat :background-origin :background-clip
                       :background-attachment :background-color]
          :border [:border-top-width :border-right-width :border-bottom-width
                   :border-left-width :border-top-style :border-right-style
                   :border-bottom-style :border-left-style :border-top-color
                   :border-right-color :border-bottom-color :border-left-color]}))

(defn property-key [property]
  (if (keyword? property) property (keyword property)))

(defn expand-declaration
  "Normalize a declaration into independently emitted property/value pairs."
  [declaration]
  (mapv (fn [[property value]] [(property-key property) value]) declaration))

(defn important? [value]
  (and (string? value)
       (some? (re-find #"(?i)!\s*important\s*$" value))))

(defn unknown-shorthand? [property]
  (contains? #{:all :border-block :border-inline :border-top :border-right
               :border-bottom :border-left :border-block-start :border-block-end
               :border-inline-start :border-inline-end :columns :column-rule
               :grid :grid-template :grid-column :grid-row :grid-area
               :list-style :mask :place-content :place-items :place-self
               :scroll-timeline :text-decoration :text-emphasis}
             (property-key property)))

(defn- logical-properties [prefix suffix]
  #{(keyword (str prefix "-inline-start" suffix))
    (keyword (str prefix "-inline-end" suffix))
    (keyword (str prefix "-block-start" suffix))
    (keyword (str prefix "-block-end" suffix))})

(defn- physical-properties [prefix suffix]
  #{(keyword (str prefix "-top" suffix))
    (keyword (str prefix "-right" suffix))
    (keyword (str prefix "-bottom" suffix))
    (keyword (str prefix "-left" suffix))})

(def ^:private physical-logical-groups
  (concat
    (for [prefix ["padding" "margin" "scroll-padding" "scroll-margin"]]
      [(physical-properties prefix "") (logical-properties prefix "")])
    [[#{:top :right :bottom :left}
      #{:inset-inline-start :inset-inline-end
        :inset-block-start :inset-block-end}]]
    (for [suffix ["-width" "-style" "-color"]]
      [(physical-properties "border" suffix)
       (logical-properties "border" suffix)])))

(def ^:private logical-shorthand-conflicts
  {:padding-inline #{:padding-left :padding-right}
   :padding-block #{:padding-top :padding-bottom}
   :margin-inline #{:margin-left :margin-right}
   :margin-block #{:margin-top :margin-bottom}
   :scroll-padding-inline #{:scroll-padding-left :scroll-padding-right}
   :scroll-padding-block #{:scroll-padding-top :scroll-padding-bottom}
   :scroll-margin-inline #{:scroll-margin-left :scroll-margin-right}
   :scroll-margin-block #{:scroll-margin-top :scroll-margin-bottom}
   :inset-inline #{:left :right}
   :inset-block #{:top :bottom}})

(defn- add-conflicts [result properties conflicts]
  (reduce (fn [result property]
            (update result property (fnil set/union #{}) conflicts))
          result
          properties))

(def ^:private symmetric-conflicts
  (merge
    (reduce (fn [result [physical-properties logical-properties]]
              (-> result
                  (add-conflicts physical-properties logical-properties)
                  (add-conflicts logical-properties physical-properties)))
            {}
            physical-logical-groups)
    logical-shorthand-conflicts))

(defn descendants [property]
  (set (get shorthand-descendants (property-key property))))

(defn conflicts [property]
  (get symmetric-conflicts (property-key property) #{}))

(defn property-rank [property]
  (if (seq (descendants property)) 0 1))

(def ^:private commutative-self-condition-re
  #"^&(?::(?!:)|\[)")

(defn- commutative-self-condition? [condition]
  (and (string? condition)
       (re-find commutative-self-condition-re condition)))

(defn canonical-condition-path [condition-path]
  (->> condition-path
       (partition-by commutative-self-condition?)
       (mapcat (fn [conditions]
                 (if (commutative-self-condition? (first conditions))
                   (sort conditions)
                   conditions)))
       vec))

(defn resolution-condition [condition-path layer]
  (let [condition-path (canonical-condition-path condition-path)]
    (if layer
      [::layer (:name layer) condition-path]
      condition-path)))

(defn resolution-key [condition property]
  (str (pr-str condition) "|" (name (property-key property))))

(defn descriptor-operation
  "Return ordered [resolution-key value] writes for one atomic declaration."
  [condition property class-name]
  (let [property (property-key property)
        blockers (set/union (descendants property) (conflicts property))]
    (into (mapv (fn [blocked]
                  [(resolution-key condition blocked) nil])
                (sort blockers))
          [[(resolution-key condition property) class-name]])))

(def ^:private min-width-media-re
  #"^@media \((?:min-width:\s*|width\s*>=\s*)([0-9]+(?:\.[0-9]+)?)(px|rem|em)\)$")

(def ^:private max-width-media-re
  #"^@media \((?:max-width:\s*|width\s*<=?\s*)([0-9]+(?:\.[0-9]+)?)(px|rem|em)\)$")

(defn- width-in-px [value unit]
  (cond-> (parse-double value) (not= "px" unit) (* 16)))

(defn- padded-width [width]
  (format "%012.3f" (double width)))

(defn- condition-sort-key
  "Width media queries sort numerically, so a wider min-width rule follows a
   narrower one and a narrower max-width rule follows a wider one: the more
   specific query then wins where both match. Other conditions sort as written."
  [condition]
  (or (when-let [[_ value unit] (and (string? condition)
                                     (re-matches min-width-media-re condition))]
        (str "@media min-width " (padded-width (width-in-px value unit))))
      (when-let [[_ value unit] (and (string? condition)
                                     (re-matches max-width-media-re condition))]
        (str "@media max-width " (padded-width (- 1e8 (width-in-px value unit)))))
      condition))

(defn condition-rank [condition-path]
  ;; Base rules sort first. Different matching conditions then use a stable,
  ;; selector-based order; same-condition conflicts are removed at runtime.
  (let [condition-path (canonical-condition-path condition-path)]
    [(count condition-path) (str/join "|" (map condition-sort-key condition-path))]))
