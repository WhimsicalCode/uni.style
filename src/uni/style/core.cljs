(ns uni.style.core
  (:require [clojure.string :as str]
            [uni.style.impl.class-name :as class-name]
            [uni.style.impl.css :as css]
            [uni.style.impl.theme-runtime :as theme-runtime]
            [uni.style.impl.tokens :as tokens]
            [uni.style.runtime])
  (:require-macros [uni.style.core]))

(uni.style.core/declare-layer-order)
(uni.style.core/declare-properties)

(def ^:const layer-priority-css
  "Global cascade-layer order. Emit before any named-layer rules."
  (uni.style.core/str css/layer-priority-css))

(def property-css
  "Non-inheriting @property registrations behind composed utilities such as
   the box-shadow layers. Build output emits them after the layer order."
  css/property-css)

(defn token-css
  "Export all loaded tokens, or an explicit subset, in the theme layer.
   Theme is a def-theme class or nil for :root defaults. Empty explicit collections
   return \"\"; no priority header is included.
   CSS references are preserved; standalone subsets must include referenced tokens."
  ([theme] (tokens/token-css theme))
  ([theme token-list] (tokens/token-css theme token-list)))

(defn all-tokens
  "Return a sorted vector of all loaded token keywords, not just build-reachable tokens."
  []
  (tokens/all-tokens))

(defn token-value
  "Return the declared token value for a theme, or the mandatory default."
  ([token] (tokens/token-value token))
  ([token theme] (tokens/token-value token theme)))

(defn set-theme!
  "Replace managed theme classes on html. Nil removes the theme. Stop following the OS."
  [theme]
  (theme-runtime/set-theme! theme))

(defn set-theme-system!
  "Apply and follow the OS light/dark preference. Either theme may be nil."
  [light dark]
  (theme-runtime/set-theme-system! light dark))

(declare style->class-name c)

(deftype UniStyle [entries class-order generated-classes ^:mutable class-string]
  Object
  (toString [this]
    (style->class-name this))

  IEncodeJS
  (-clj->js [this]
    (style->class-name this))
  (-key->js [this]
    (style->class-name this)))

(defonce ^:private class-descriptors- (js/Map.))
(defonce ^:private merge-cache- (js/Map.))
(defonce ^:private merge-cache-size- (volatile! 0))
(def ^:private merge-cache-limit 4096)
(def ^:private cache-value-key (js/Symbol "uni.style/value"))

(defn uni-style?
  "Return true when value is a metadata-aware uni.style class value."
  [value]
  (instance? UniStyle value))

(defn- reset-merge-cache! []
  (.clear merge-cache-)
  (vreset! merge-cache-size- 0))

(defn style-class
  "Private compiler boundary. Register metadata for generated classes and return
   the backwards-compatible class string produced by a utility macro."
  [class-value descriptor-version create-descriptor]
  (let [registered (.get class-descriptors- class-value)]
    (when-not (and registered (= descriptor-version (aget registered 0)))
      (.set class-descriptors- class-value
            #js [descriptor-version (create-descriptor)])
      (reset-merge-cache!)))
  class-value)

(defn- style->class-name [^UniStyle style]
  (or (.-class-string style)
      (let [active-classes (into #{} (keep val) (.-entries style))
            generated-classes (.-generated-classes style)
            [_ classes]
            (reduce (fn [[seen-generated result] class]
                      (if (contains? generated-classes class)
                        (if (or (contains? seen-generated class)
                                (not (contains? active-classes class)))
                          [seen-generated result]
                          [(conj seen-generated class) (conj result class)])
                        [seen-generated (conj result class)]))
                    [#{} []]
                    (.-class-order style))
            class-name (str/join " " classes)]
        (set! (.-class-string style) class-name)
        class-name)))

(defn class-name
  "Serialize a uni.style value (or compatible class input) to a class string.
   Use this only at APIs that require an actual string."
  [value]
  (if (uni-style? value)
    (style->class-name value)
    (style->class-name (c value))))

(defn- append-input! [result input]
  (cond
    (sequential? input)
    (reduce append-input! result input)

    (or (nil? input) (false? input) (= "" input))
    result

    (uni-style? input)
    (let [^UniStyle input input]
      (-> result
          (update :entries into (.-entries input))
          (update :class-order into (.-class-order input))
          (update :generated-classes into (.-generated-classes input))))

    :else
    (let [class-value (cond
                        (keyword? input) (class-name/keyword-class-name input)
                        (symbol? input) (name input)
                        :else input)
          registered (.get class-descriptors- class-value)]
      (if registered
        ;; Descriptors use a flat [resolution-key class-or-nil ...] vector to
        ;; minimize the metadata emitted at every utility call site.
        (let [operations (aget registered 1)]
          (loop [result result
                 index 0]
            (if (< index (count operations))
              (let [key (nth operations index)
                    value (nth operations (inc index))
                    result (update result :entries assoc key value)
                    result (if value
                             (-> result
                                 (update :class-order conj value)
                                 (update :generated-classes conj value))
                             result)]
                (recur result (+ index 2)))
              result)))
        (update result :class-order conj class-value)))))

(defn- flatten-inputs [inputs]
  (reduce (fn flatten-one [result input]
            (if (sequential? input)
              (reduce flatten-one result input)
              (if (or (nil? input) (false? input) (= "" input))
                result
                (conj result input))))
          []
          inputs))

(defn- cached-value [inputs]
  (loop [node merge-cache-
         inputs inputs]
    (if-let [inputs (seq inputs)]
      (when-let [next-node (.get node (first inputs))]
        (recur next-node (next inputs)))
      (.get node cache-value-key))))

(defn- cache-value! [inputs value]
  (loop [node merge-cache-
         inputs inputs]
    (if-let [inputs (seq inputs)]
      (let [input (first inputs)
            next-node (or (.get node input)
                          (let [new-node (js/Map.)]
                            (.set node input new-node)
                            new-node))]
        (recur next-node (next inputs)))
      (.set node cache-value-key value)))
  (vswap! merge-cache-size- inc)
  value)

(defn- cached-style [inputs create]
  (or (cached-value inputs)
      (do
        (when (<= merge-cache-limit @merge-cache-size-)
          (reset-merge-cache!))
        (cache-value! inputs (create)))))

(defn c
  "Compose classes from left to right. For metadata-aware uni.style classes,
   the last declaration for an equivalent condition/property wins. Arbitrary
   strings and keywords are retained as opaque classes."
  [& classes-or-class-sequences]
  (let [inputs (flatten-inputs classes-or-class-sequences)]
    (if (and (= 1 (count inputs)) (uni-style? (first inputs)))
      (first inputs)
      (cached-style
        inputs
        #(let [{:keys [entries class-order generated-classes]}
               (reduce append-input!
                       {:entries {} :class-order [] :generated-classes #{}}
                       inputs)]
           (UniStyle. entries class-order generated-classes nil))))))
