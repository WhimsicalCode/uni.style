(ns uni.style.impl.runtime
  "JVM class composition with immutable values and thread-safe descriptors."
  (:require [clojure.string :as str]
            [uni.style.impl.class-name :as class-name]))

(deftype UniStyle [entries class-order generated-classes class-string]
  Object
  (toString [_] class-string))

(defonce ^:private class-descriptors- (atom {}))

(defn uni-style? [value]
  (instance? UniStyle value))

(defn style-class [class-value descriptor-version create-descriptor]
  (when-not (= descriptor-version (first (get @class-descriptors- class-value)))
    (swap! class-descriptors- assoc class-value [descriptor-version (create-descriptor)]))
  class-value)

(defn- append-input [descriptors result input]
  (cond
    (sequential? input)
    (reduce #(append-input descriptors %1 %2) result input)

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
                        :else input)]
      (if-let [[_ operations] (get descriptors class-value)]
        (reduce (fn [result [key value]]
                  (cond-> (update result :entries assoc key value)
                          value (-> (update :class-order conj value)
                                    (update :generated-classes conj value))))
                result
                (partition 2 operations))
        (update result :class-order conj class-value)))))

(defn- serialize [{:keys [entries class-order generated-classes]}]
  (let [active-classes (into #{} (keep val) entries)
        [_ classes]
        (reduce (fn [[seen-generated result] class]
                  (if (contains? generated-classes class)
                    (if (or (contains? seen-generated class)
                            (not (contains? active-classes class)))
                      [seen-generated result]
                      [(conj seen-generated class) (conj result class)])
                    [seen-generated (conj result class)]))
                [#{} []]
                class-order)]
    (str/join " " classes)))

(defn c [& inputs]
  (if (and (= 1 (count inputs)) (uni-style? (first inputs)))
    (first inputs)
    (let [descriptors @class-descriptors-
          {:keys [entries class-order generated-classes] :as result}
          (reduce #(append-input descriptors %1 %2)
                  {:entries {} :class-order [] :generated-classes #{}}
                  inputs)]
      (UniStyle. entries class-order generated-classes (serialize result)))))

(defn class-name [value]
  (.-class-string ^UniStyle (if (uni-style? value) value (c value))))
