(ns uni.style.impl.tokens
  "Portable token data, concrete-value lookup, and CSS export. Normal builds use their own registry."
  (:require [uni.style.impl.css :as css]))

(defonce definitions- (atom {}))

(defn register! [owner entry]
  (swap! definitions- assoc owner entry)
  nil)

(defonce ^:private index- (atom nil))

(defn- index []
  (let [definitions @definitions-
        cached @index-]
    (if (identical? definitions (:source cached))
      cached
      (let [entries (vals definitions)
            result {:source definitions
                    :tokens (into {} (comp (filter #(= :token (:kind %)))
                                           (map (juxt :name identity))) entries)
                    :themes (into #{} (comp (filter #(= :theme (:kind %))) (map :name)) entries)}]
        (reset! index- result)
        result))))

(defn token-values
  "Index registered tokens by custom-property keyword. Normal builds never collect from this index."
  []
  (:tokens (index)))

(defn all-tokens []
  (vec (sort (keys (token-values)))))

(defn themes []
  (:themes (index)))

(defn value-in
  "Resolve a declared value, following token aliases for the same theme."
  [tokens token theme]
  (loop [token token seen #{}]
    (when (contains? seen token)
      (throw (ex-info "Circular token alias" {:token token :theme theme :cycle seen})))
    (let [entry (get tokens token)]
      (when-not entry
        (throw (ex-info "Unknown token" {:token token})))
      (let [value (get (:values entry) theme (:default entry))]
        (if (keyword? value)
          (recur value (conj seen token))
          value)))))

(defn token-css
  "Export all loaded tokens or explicit token names for a theme or root defaults."
  ([theme] (token-css theme (keys (token-values))))
  ([theme token-list]
   (let [{:keys [tokens themes]} (index)]
     (when-not (or (nil? theme) (contains? themes theme))
       (throw (ex-info "Unknown theme" {:theme theme})))
     (when-not (or (nil? token-list) (sequential? token-list) (set? token-list))
       (throw (ex-info "token-css expects a collection of token keywords" {:tokens token-list})))
     (let [entries (mapv (fn [token]
                           (or (get tokens token)
                               (throw (ex-info "Unknown token" {:token token}))))
                         (distinct (seq token-list)))]
       (css/token-css theme entries)))))

(defn token-value
  ([token] (token-value token nil))
  ([token theme]
   (when-not (or (nil? theme) (contains? (themes) theme))
     (throw (ex-info "Unknown theme" {:theme theme})))
   (value-in (token-values) token theme)))
