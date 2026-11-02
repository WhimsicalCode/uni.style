(ns uni.style.impl.layer
  "Cascade-layer modifiers and global layer-order registration."
  (:require [uni.style.impl.compiler :as compiler]
            [uni.style.impl.composition :as composition]
            [uni.style.impl.css :as css]
            [uni.style.impl.modifier :as modifier]))

(defn layer* [env layer-name forms]
  (when compiler/*layer*
    (throw (ex-info "Cascade layers cannot be nested"
                    {:layer (:name compiler/*layer*) :form layer-name})))
  (let [layer (compiler/resolve-layer env layer-name)]
    (binding [compiler/*layer* layer]
      (->> forms
           (mapcat (fn [form]
                     (if-let [{:keys [symbol declarations]}
                              (composition/referenced-class-style env form)]
                       (map-indexed
                         (fn [index {:keys [declaration modifiers]}]
                           (let [class-name (compiler/utility-class-name
                                              env
                                              (str "ref-" (name symbol) "-"
                                                   (compiler/content-hash (str symbol)) "-" index))
                                 modifiers (into (compiler/variant-selectors) modifiers)]
                             (if compiler/*class-declaration-collector*
                               (compiler/collect-declaration!
                                 compiler/*class-declaration-collector*
                                 declaration modifiers compiler/*layer*)
                               (compiler/register-atomic! env
                                                          (compiler/caller-ns env)
                                                          class-name declaration
                                                          modifiers compiler/*layer*))
                             class-name))
                         declarations)
                       (->> (modifier/resolve-style-forms env [form])
                            (mapcat (fn [[source-form result]]
                                      (modifier/flatten-variant-result source-form result)))))))
           (mapv compiler/emit-style-value)))))

(defn declare-layer-order* [env]
  (compiler/register-global! env
                             (compiler/caller-ns env)
                             "uni-style-layer-order"
                             css/layer-priority-css)
  nil)

(defn declare-properties* [env]
  (compiler/register-global! env
                             (compiler/caller-ns env)
                             "uni-style-properties"
                             css/property-css)
  nil)
