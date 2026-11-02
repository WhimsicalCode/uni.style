(ns uni.style.impl.scrollbar
  "Scrollbar color, width, and gutter utilities."
  (:require [uni.style.impl.compiler :as compiler]
            [uni.style.impl.value :as value]))

(def scrollbar-color-value
  "var(--us-scrollbar-thumb) var(--us-scrollbar-track)")

(def static-utilities
  {:scrollbar-thumb-inherit
   ["scrollbar-thumb-inherit"
    {:--us-scrollbar-thumb :inherit
     :scrollbar-color scrollbar-color-value}]
   :scrollbar-thumb-current
   ["scrollbar-thumb-current"
    {:--us-scrollbar-thumb "currentColor"
     :scrollbar-color scrollbar-color-value}]
   :scrollbar-thumb-transparent
   ["scrollbar-thumb-transparent"
    {:--us-scrollbar-thumb :transparent
     :scrollbar-color scrollbar-color-value}]
   :scrollbar-track-inherit
   ["scrollbar-track-inherit"
    {:--us-scrollbar-track :inherit
     :scrollbar-color scrollbar-color-value}]
   :scrollbar-track-current
   ["scrollbar-track-current"
    {:--us-scrollbar-track "currentColor"
     :scrollbar-color scrollbar-color-value}]
   :scrollbar-track-transparent
   ["scrollbar-track-transparent"
    {:--us-scrollbar-track :transparent
     :scrollbar-color scrollbar-color-value}]
   :scrollbar-auto ["scrollbar-auto" {:scrollbar-width :auto}]
   :scrollbar-thin ["scrollbar-thin" {:scrollbar-width :thin}]
   :scrollbar-none ["scrollbar-none" {:scrollbar-width :none}]
   :scrollbar-gutter-auto ["scrollbar-gutter-auto" {:scrollbar-gutter :auto}]
   :scrollbar-gutter-stable ["scrollbar-gutter-stable" {:scrollbar-gutter :stable}]
   :scrollbar-gutter-both ["scrollbar-gutter-both" {:scrollbar-gutter "stable both-edges"}]})

(defn static* [env utility]
  (let [[class-name declaration] (get static-utilities utility)]
    (compiler/simple-utility* env class-name declaration)))

(defn thumb*
  "Set the scrollbar thumb custom property from a token or literal color."
  [env color-form]
  (let [color (compiler/resolve-compile-time-constant env color-form)]
    (when-not (or (keyword? color) (string? color))
      (throw (ex-info "Scrollbar thumb must be a compile-time token keyword or CSS string"
                      {:form color-form :value color :type :scrollbar-thumb})))
    (let [{:keys [class-name value]}
          (value/color-utility color-form "scrollbar-thumb" color
                               "Scrollbar thumb" :scrollbar-thumb)]
      (compiler/register-utility!
        env class-name
        {:--us-scrollbar-thumb value
         :scrollbar-color scrollbar-color-value}))))

(defn track*
  "Set the scrollbar track custom property from a token or literal color."
  [env color-form]
  (let [color (compiler/resolve-compile-time-constant env color-form)]
    (when-not (or (keyword? color) (string? color))
      (throw (ex-info "Scrollbar track must be a compile-time token keyword or CSS string"
                      {:form color-form :value color :type :scrollbar-track})))
    (let [{:keys [class-name value]}
          (value/color-utility color-form "scrollbar-track" color
                               "Scrollbar track" :scrollbar-track)]
      (compiler/register-utility!
        env class-name
        {:--us-scrollbar-track value
         :scrollbar-color scrollbar-color-value}))))
