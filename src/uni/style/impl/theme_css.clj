(ns uni.style.impl.theme-css
  "Expand build-local token data after all reachable themes are known."
  (:require [uni.style.impl.css :as css]))

(defn- token-css [entry themes]
  ;; Keep per-token chunks for build ordering, using the same renderer
  ;; as runtime subset export without reading its global definition registry.
  (apply str (map #(css/token-css % [entry]) (cons nil (sort themes)))))

(defn expand-definitions [styles]
  (let [definitions (keep :definition styles)
        themes (into #{} (comp (filter #(= :theme (:kind %))) (map :name)) definitions)]
    (doseq [[css-name entries] (group-by :name definitions)]
      (when (> (count (distinct (map :owner entries))) 1)
        (throw (ex-info "Generated theme/token name collision"
                        {:name css-name :owners (mapv :owner entries)}))))
    (mapv (fn [{:keys [definition] :as style}]
            (if (= :token (:kind definition))
              (do
                (when-let [missing (seq (remove themes (keys (:values definition))))]
                  (throw (ex-info "Token themes must be included in the CSS build"
                                  {:token (:name definition) :themes missing})))
                (assoc style :css-str (token-css definition themes)))
              style))
          ;; Theme declarations carry no CSS, and should not produce empty mappings.
          (remove #(= :theme (get-in % [:definition :kind])) styles))))
