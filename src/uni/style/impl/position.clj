(ns uni.style.impl.position
  "Clipping, overflow, positioning, inset, order, and z-index utilities."
  (:require [uni.style.impl.compiler :as compiler]
            [uni.style.impl.value :as value]))

(def static-utilities
  {:order-first ["order-first" {:order -9999}]
   :order-last ["order-last" {:order 9999}]
   :order-none ["order-none" {:order 0}]
   :overflow-auto ["overflow-auto" {:overflow :auto}]
   :overflow-hidden ["overflow-hidden" {:overflow :hidden}]
   :overflow-clip ["overflow-clip" {:overflow :clip}]
   :overflow-visible ["overflow-visible" {:overflow :visible}]
   :overflow-scroll ["overflow-scroll" {:overflow :scroll}]
   :overflow-x-auto ["overflow-x-auto" {:overflow-x :auto}]
   :overflow-x-hidden ["overflow-x-hidden" {:overflow-x :hidden}]
   :overflow-x-clip ["overflow-x-clip" {:overflow-x :clip}]
   :overflow-x-visible ["overflow-x-visible" {:overflow-x :visible}]
   :overflow-x-scroll ["overflow-x-scroll" {:overflow-x :scroll}]
   :overflow-y-auto ["overflow-y-auto" {:overflow-y :auto}]
   :overflow-y-hidden ["overflow-y-hidden" {:overflow-y :hidden}]
   :overflow-y-clip ["overflow-y-clip" {:overflow-y :clip}]
   :overflow-y-visible ["overflow-y-visible" {:overflow-y :visible}]
   :overflow-y-scroll ["overflow-y-scroll" {:overflow-y :scroll}]
   :overscroll-auto ["overscroll-auto" {:overscroll-behavior :auto}]
   :overscroll-contain ["overscroll-contain" {:overscroll-behavior :contain}]
   :overscroll-none ["overscroll-none" {:overscroll-behavior :none}]
   :overscroll-x-auto ["overscroll-x-auto" {:overscroll-behavior-x :auto}]
   :overscroll-x-contain ["overscroll-x-contain" {:overscroll-behavior-x :contain}]
   :overscroll-x-none ["overscroll-x-none" {:overscroll-behavior-x :none}]
   :overscroll-y-auto ["overscroll-y-auto" {:overscroll-behavior-y :auto}]
   :overscroll-y-contain ["overscroll-y-contain" {:overscroll-behavior-y :contain}]
   :overscroll-y-none ["overscroll-y-none" {:overscroll-behavior-y :none}]
   :snap-none ["snap-none" {:scroll-snap-type :none}]
   :snap-x ["snap-x" {:scroll-snap-type "x var(--us-scroll-snap-strictness, proximity)"}]
   :snap-y ["snap-y" {:scroll-snap-type "y var(--us-scroll-snap-strictness, proximity)"}]
   :snap-both ["snap-both" {:scroll-snap-type "both var(--us-scroll-snap-strictness, proximity)"}]
   :snap-mandatory ["snap-mandatory" {"--us-scroll-snap-strictness" :mandatory}]
   :snap-proximity ["snap-proximity" {"--us-scroll-snap-strictness" :proximity}]
   :snap-start ["snap-start" {:scroll-snap-align :start}]
   :snap-end ["snap-end" {:scroll-snap-align :end}]
   :snap-center ["snap-center" {:scroll-snap-align :center}]
   :snap-align-none ["snap-align-none" {:scroll-snap-align :none}]
   :static ["static" {:position :static}]
   :fixed ["fixed" {:position :fixed}]
   :absolute ["absolute" {:position :absolute}]
   :relative ["relative" {:position :relative}]
   :sticky ["sticky" {:position :sticky}]
   :inset-auto ["inset-auto" {:inset :auto}]
   :inset-x-auto ["inset-x-auto" {:inset-inline :auto}]
   :inset-y-auto ["inset-y-auto" {:inset-block :auto}]
   :top-auto ["top-auto" {:top :auto}]
   :right-auto ["right-auto" {:right :auto}]
   :bottom-auto ["bottom-auto" {:bottom :auto}]
   :left-auto ["left-auto" {:left :auto}]
   :z-auto ["z-auto" {:z-index :auto}]})

(defn static* [env utility]
  (let [[class-name declaration] (get static-utilities utility)]
    (compiler/simple-utility* env class-name declaration)))

(defn clip-path* [env value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [_ css-value]
        (value/string-or-token-parts value-form value "Clip path" :clip-path)
        class-name (if (string? value)
                     (str "clip-path-" (compiler/content-hash value))
                     (str "clip-path--" (name value)))]
    (compiler/register-utility! env class-name {:clip-path css-value})))

(defn inset*
  "Macro implementation shared by the positional inset utilities."
  [env utility-prefix property value-form]
  (compiler/signed-length-utility* env utility-prefix property value-form "Inset" :inset))

(defn order* [env value-form]
  (compiler/integer-utility* env "order" :order value-form "Order" :order))

(defn z* [env value-form]
  (compiler/integer-utility* env "z" :z-index value-form "Z-index" :z-index))
