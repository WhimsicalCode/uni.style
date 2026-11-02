(ns uni.style.impl.layout
  "Display, visibility, generated content, object-fit, flex, and alignment utilities."
  (:require [clojure.string :as str]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.value :as value]))

(def static-utilities
  {:inline ["inline" {:display :inline}]
   :block ["block" {:display :block}]
   :inline-block ["inline-block" {:display :inline-block}]
   :flow-root ["flow-root" {:display :flow-root}]
   :flex ["flex" {:display :flex}]
   :inline-flex ["inline-flex" {:display :inline-flex}]
   :grid ["grid" {:display :grid}]
   :inline-grid ["inline-grid" {:display :inline-grid}]
   :list-item ["list-item" {:display :list-item}]
   :contents ["contents" {:display :contents}]
   :hidden ["hidden" {:display :none}]
   :content-none ["content-none" {:content :none}]
   :box-border ["box-border" {:box-sizing :border-box}]
   :box-content ["box-content" {:box-sizing :content-box}]
   :container-normal ["container-normal" {:container-type :normal}]
   :container-size ["container-size" {:container-type :size}]
   :container-inline-size ["container-inline-size" {:container-type :inline-size}]
   :container-scroll-state ["container-scroll-state" {:container-type :scroll-state}]
   :container-anchored ["container-anchored" {:container-type :anchored}]
   :visible ["visible" {:visibility :visible}]
   :invisible ["invisible" {:visibility :hidden}]
   :collapse ["collapse" {:visibility :collapse}]
   :border-collapse ["border-collapse" {:border-collapse :collapse}]
   :border-separate ["border-separate" {:border-collapse :separate}]
   :flex-row ["flex-row" {:flex-direction :row}]
   :flex-row-reverse ["flex-row-reverse" {:flex-direction :row-reverse}]
   :flex-col ["flex-col" {:flex-direction :column}]
   :flex-col-reverse ["flex-col-reverse" {:flex-direction :column-reverse}]
   :flex-wrap ["flex-wrap" {:flex-wrap :wrap}]
   :flex-wrap-reverse ["flex-wrap-reverse" {:flex-wrap :wrap-reverse}]
   :flex-nowrap ["flex-nowrap" {:flex-wrap :nowrap}]
   :flex-1 ["flex-1" {:flex "1 1 0%"}]
   :flex-auto ["flex-auto" {:flex "1 1 auto"}]
   :flex-initial ["flex-initial" {:flex "0 1 auto"}]
   :flex-none ["flex-none" {:flex :none}]
   :align-baseline ["align-baseline" {:vertical-align :baseline}]
   :align-top ["align-top" {:vertical-align :top}]
   :align-middle ["align-middle" {:vertical-align :middle}]
   :align-bottom ["align-bottom" {:vertical-align :bottom}]
   :align-text-top ["align-text-top" {:vertical-align :text-top}]
   :align-text-bottom ["align-text-bottom" {:vertical-align :text-bottom}]
   :align-sub ["align-sub" {:vertical-align :sub}]
   :align-super ["align-super" {:vertical-align :super}]
   :grow ["grow" {:flex-grow 1}]
   :shrink ["shrink" {:flex-shrink 1}]
   :self-auto ["self-auto" {:align-self :auto}]
   :self-start ["self-start" {:align-self :flex-start}]
   :self-end ["self-end" {:align-self :flex-end}]
   :self-end-safe ["self-end-safe" {:align-self "safe flex-end"}]
   :self-center ["self-center" {:align-self :center}]
   :self-center-safe ["self-center-safe" {:align-self "safe center"}]
   :self-stretch ["self-stretch" {:align-self :stretch}]
   :self-baseline ["self-baseline" {:align-self :baseline}]
   :self-baseline-last ["self-baseline-last" {:align-self "last baseline"}]
   :items-inherit ["items-inherit" {:align-items :inherit}]
   :items-start ["items-start" {:align-items :flex-start}]
   :items-end ["items-end" {:align-items :flex-end}]
   :items-end-safe ["items-end-safe" {:align-items "safe flex-end"}]
   :items-center ["items-center" {:align-items :center}]
   :items-center-safe ["items-center-safe" {:align-items "safe center"}]
   :items-baseline ["items-baseline" {:align-items :baseline}]
   :items-baseline-last ["items-baseline-last" {:align-items "last baseline"}]
   :items-stretch ["items-stretch" {:align-items :stretch}]
   :justify-start ["justify-start" {:justify-content :flex-start}]
   :justify-end ["justify-end" {:justify-content :flex-end}]
   :justify-end-safe ["justify-end-safe" {:justify-content "safe flex-end"}]
   :justify-center ["justify-center" {:justify-content :center}]
   :justify-center-safe ["justify-center-safe" {:justify-content "safe center"}]
   :justify-between ["justify-between" {:justify-content :space-between}]
   :justify-around ["justify-around" {:justify-content :space-around}]
   :justify-evenly ["justify-evenly" {:justify-content :space-evenly}]
   :justify-stretch ["justify-stretch" {:justify-content :stretch}]
   :justify-baseline ["justify-baseline" {:justify-content :baseline}]
   :justify-normal ["justify-normal" {:justify-content :normal}]
   :justify-items-start ["justify-items-start" {:justify-items :start}]
   :justify-items-end ["justify-items-end" {:justify-items :end}]
   :justify-items-end-safe ["justify-items-end-safe" {:justify-items "safe end"}]
   :justify-items-center ["justify-items-center" {:justify-items :center}]
   :justify-items-center-safe ["justify-items-center-safe" {:justify-items "safe center"}]
   :justify-items-stretch ["justify-items-stretch" {:justify-items :stretch}]
   :justify-items-normal ["justify-items-normal" {:justify-items :normal}]
   :justify-self-auto ["justify-self-auto" {:justify-self :auto}]
   :justify-self-start ["justify-self-start" {:justify-self :start}]
   :justify-self-center ["justify-self-center" {:justify-self :center}]
   :justify-self-center-safe ["justify-self-center-safe" {:justify-self "safe center"}]
   :justify-self-end ["justify-self-end" {:justify-self :end}]
   :justify-self-end-safe ["justify-self-end-safe" {:justify-self "safe end"}]
   :justify-self-stretch ["justify-self-stretch" {:justify-self :stretch}]
   :content-normal ["content-normal" {:align-content :normal}]
   :content-center ["content-center" {:align-content :center}]
   :content-start ["content-start" {:align-content :flex-start}]
   :content-end ["content-end" {:align-content :flex-end}]
   :content-between ["content-between" {:align-content :space-between}]
   :content-around ["content-around" {:align-content :space-around}]
   :content-evenly ["content-evenly" {:align-content :space-evenly}]
   :content-baseline ["content-baseline" {:align-content :baseline}]
   :content-stretch ["content-stretch" {:align-content :stretch}]
   :object-contain ["object-contain" {:object-fit :contain}]
   :object-cover ["object-cover" {:object-fit :cover}]
   :object-fill ["object-fill" {:object-fit :fill}]
   :object-none ["object-none" {:object-fit :none}]
   :object-scale-down ["object-scale-down" {:object-fit :scale-down}]
   :float-right ["float-right" {:float :right}]
   :float-left ["float-left" {:float :left}]
   :float-start ["float-start" {:float :inline-start}]
   :float-end ["float-end" {:float :inline-end}]
   :float-none ["float-none" {:float :none}]
   :table-auto ["table-auto" {:table-layout :auto}]
   :table-fixed ["table-fixed" {:table-layout :fixed}]
   :grid-flow-row ["grid-flow-row" {:grid-auto-flow :row}]
   :grid-flow-col ["grid-flow-col" {:grid-auto-flow :column}]
   :grid-flow-dense ["grid-flow-dense" {:grid-auto-flow :dense}]
   :grid-flow-row-dense ["grid-flow-row-dense" {:grid-auto-flow "row dense"}]
   :grid-flow-col-dense ["grid-flow-col-dense" {:grid-auto-flow "column dense"}]
   :isolate ["isolate" {:isolation :isolate}]
   :isolation-auto ["isolation-auto" {:isolation :auto}]
   :break-inside-auto ["break-inside-auto" {:break-inside :auto}]
   :break-inside-avoid ["break-inside-avoid" {:break-inside :avoid}]
   :break-inside-avoid-page ["break-inside-avoid-page" {:break-inside :avoid-page}]
   :break-inside-avoid-column ["break-inside-avoid-column" {:break-inside :avoid-column}]
   :scheme-normal ["scheme-normal" {:color-scheme :normal}]
   :scheme-dark ["scheme-dark" {:color-scheme :dark}]
   :scheme-light ["scheme-light" {:color-scheme :light}]
   :scheme-light-dark ["scheme-light-dark" {:color-scheme "light dark"}]
   :scheme-only-dark ["scheme-only-dark" {:color-scheme "only dark"}]
   :scheme-only-light ["scheme-only-light" {:color-scheme "only light"}]
   :line-clamp-none ["line-clamp-none" {:overflow :visible :display :block :-webkit-box-orient :horizontal :-webkit-line-clamp :unset}]
   :sr-only ["sr-only" {:position :absolute :width 1 :height 1 :padding 0 :margin -1
                        :overflow :hidden :clip-path "inset(50%)" :white-space :nowrap
                        :border-width 0}]
   :not-sr-only ["not-sr-only" {:position :static :width :auto :height :auto :padding 0
                                :margin 0 :overflow :visible :clip-path :none
                                :white-space :normal}]})

(defn static* [env utility]
  (let [[class-name declaration] (get static-utilities utility)]
    (compiler/simple-utility* env class-name declaration)))

(defn items*
  "Macro implementation for token-backed or literal align-items values."
  [env value-form]
  (let [resolved-value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value]
        (value/string-or-token-parts value-form resolved-value "Align items" :align-items)]
    (compiler/register-utility! env (str "items-" class-part) {:align-items css-value})))

(defn- quoted-content [content]
  (str "'"
       (-> content
           (str/replace "\\" "\\\\")
           (str/replace "'" "\\'")
           (str/replace "\r\n" "\\A ")
           (str/replace "\r" "\\A ")
           (str/replace "\n" "\\A ")
           (str/replace "\f" "\\A "))
       "'"))

(defn content*
  "Macro implementation for literal or token-backed generated content values."
  [env value-form]
  (let [resolved-value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value]
        (cond
          (string? resolved-value)
          [(str "value-" (compiler/content-hash resolved-value))
           (quoted-content resolved-value)]

          (keyword? resolved-value)
          (value/token-parts value-form resolved-value "Content" :content)

          :else
          (throw (ex-info "Content must be a compile-time string or token keyword"
                          {:form value-form :value resolved-value :type :content})))]
    (compiler/register-utility! env (str "content-" class-part) {:content css-value})))

(defn aspect-ratio*
  "Macro implementation for aspect-ratio utilities."
  [env value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part value] (value/literal-parts value-form value "Aspect ratio" :aspect-ratio)]
    (compiler/register-utility! env (str "aspect-ratio-" class-part) {:aspect-ratio value})))

(defn flex-factor*
  "Macro implementation shared by the flex grow and shrink utilities."
  [env utility-prefix property value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value]
        (value/non-negative-literal-parts value-form value "Flex factor" :flex-factor)]
    (compiler/register-utility! env (str utility-prefix "-" class-part)
                                {property css-value})))

(defn columns*
  "Macro implementation for columns: a number of columns, or a CSS column width."
  [env value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value]
        (value/non-negative-literal-parts value-form value "Columns" :columns)]
    (compiler/register-utility! env (str "columns-" class-part) {:columns css-value})))

(defn line-clamp*
  "Macro implementation for clamping text to a number of lines."
  [env value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value]
        (if (number? value)
          (do
            (when-not (and (integer? value) (pos? value))
              (throw (ex-info "Line clamp must be a positive compile-time integer"
                              {:form value-form :value value :type :line-clamp})))
            ;; A string, since numeric values get px units on vendor-prefixed properties.
            [(value/number-class-part value) (str value)])
          (value/string-or-token-parts value-form value "Line clamp" :line-clamp))]
    (compiler/register-utility! env (str "line-clamp-" class-part)
                                (array-map :overflow :hidden
                                           :display :-webkit-box
                                           :-webkit-box-orient :vertical
                                           :-webkit-line-clamp css-value))))

(defn border-spacing*
  "Macro implementation for table border spacing."
  [env value-form]
  (compiler/length-utility* env "border-spacing" :border-spacing value-form
                            "Border spacing" :border-spacing))

(defn grid-span*
  "Macro implementation for col-span and row-span: span a positive integer of
   grid tracks."
  [env utility-prefix property value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)]
    (when-not (and (integer? value) (pos? value))
      (throw (ex-info (str utility-prefix " must be a positive compile-time integer")
                      {:form value-form :value value :type :grid-span})))
    (compiler/register-utility! env (str utility-prefix "-" value)
                                {property (str "span " value " / span " value)})))

(defn grid-value*
  "Macro implementation for grid template, automatic track, and placement utilities."
  [env utility-prefix property value-form]
  (let [value (compiler/resolve-compile-time-constant env value-form)
        [class-part value] (value/literal-parts value-form value "Grid value" :grid-value)]
    (compiler/register-utility! env (str utility-prefix "-" class-part) {property value})))
