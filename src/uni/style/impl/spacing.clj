(ns uni.style.impl.spacing
  "Gap, dimension, basis, margin, and padding utilities."
  (:require [uni.style.impl.compiler :as compiler]))

(def static-utilities
  {:basis-auto ["basis-auto" {:flex-basis :auto}]
   :basis-full ["basis-full" {:flex-basis "100%"}]
   :min-h-auto ["min-h-auto" {:min-height :auto}]
   :min-h-full ["min-h-full" {:min-height "100%"}]
   :min-h-min ["min-h-min" {:min-height :min-content}]
   :min-h-max ["min-h-max" {:min-height :max-content}]
   :min-h-fit ["min-h-fit" {:min-height :fit-content}]
   :min-w-auto ["min-w-auto" {:min-width :auto}]
   :min-w-full ["min-w-full" {:min-width "100%"}]
   :min-w-min ["min-w-min" {:min-width :min-content}]
   :min-w-max ["min-w-max" {:min-width :max-content}]
   :min-w-fit ["min-w-fit" {:min-width :fit-content}]
   :max-h-auto ["max-h-auto" {:max-height :auto}]
   :max-h-full ["max-h-full" {:max-height "100%"}]
   :max-h-min ["max-h-min" {:max-height :min-content}]
   :max-h-max ["max-h-max" {:max-height :max-content}]
   :max-h-fit ["max-h-fit" {:max-height :fit-content}]
   :max-h-none ["max-h-none" {:max-height :none}]
   :max-w-auto ["max-w-auto" {:max-width :auto}]
   :max-w-full ["max-w-full" {:max-width "100%"}]
   :max-w-min ["max-w-min" {:max-width :min-content}]
   :max-w-max ["max-w-max" {:max-width :max-content}]
   :max-w-fit ["max-w-fit" {:max-width :fit-content}]
   :max-w-none ["max-w-none" {:max-width :none}]
   :w-auto ["w-auto" {:width :auto}]
   :w-full ["w-full" {:width "100%"}]
   :w-min ["w-min" {:width :min-content}]
   :w-max ["w-max" {:width :max-content}]
   :w-fit ["w-fit" {:width :fit-content}]
   :h-auto ["h-auto" {:height :auto}]
   :h-full ["h-full" {:height "100%"}]
   :h-min ["h-min" {:height :min-content}]
   :h-max ["h-max" {:height :max-content}]
   :h-fit ["h-fit" {:height :fit-content}]
   :size-auto ["size-auto" {:width :auto :height :auto}]
   :size-full ["size-full" {:width "100%" :height "100%"}]
   :size-min ["size-min" {:width :min-content :height :min-content}]
   :size-max ["size-max" {:width :max-content :height :max-content}]
   :size-fit ["size-fit" {:width :fit-content :height :fit-content}]
   :m-auto ["m-auto" {:margin :auto}]
   :mx-auto ["mx-auto" {:margin-inline :auto}]
   :my-auto ["my-auto" {:margin-block :auto}]
   :mt-auto ["mt-auto" {:margin-top :auto}]
   :mb-auto ["mb-auto" {:margin-bottom :auto}]
   :mr-auto ["mr-auto" {:margin-right :auto}]
   :ml-auto ["ml-auto" {:margin-left :auto}]})

(defn static* [env utility]
  (let [[class-name declaration] (get static-utilities utility)]
    (compiler/simple-utility* env class-name declaration)))

(defn gap*
  "Macro implementation shared by the gap utilities."
  [env utility-prefix property value-form]
  (compiler/length-utility* env utility-prefix property value-form "Gap" :gap))

(defn dimension*
  "Macro implementation for width and height utilities."
  [env utility-prefix properties value-form]
  (compiler/dimension-utility* env utility-prefix properties value-form))

(defn margin*
  "Macro implementation shared by the margin utilities."
  [env utility-prefix property value-form]
  (compiler/signed-length-utility* env utility-prefix property value-form "Margin" :margin))

(defn scroll-margin*
  "Macro implementation shared by the scroll margin utilities."
  [env utility-prefix property value-form]
  (compiler/signed-length-utility* env utility-prefix property value-form "Scroll margin" :scroll-margin))

(defn scroll-padding*
  "Macro implementation shared by the scroll padding utilities."
  [env utility-prefix property value-form]
  (compiler/length-utility* env utility-prefix property value-form "Scroll padding" :scroll-padding))

(defn padding*
  "Macro implementation shared by the padding utilities."
  [env utility-prefix property value-form]
  (compiler/length-utility* env utility-prefix property value-form "Padding" :padding))
