(ns uni.style.impl.interaction
  "Selection, pointer, will-change, touch, cursor, resize, and appearance utilities."
  (:require [uni.style.impl.compiler :as compiler]
            [uni.style.impl.value :as value]))

(def static-utilities
  {:select-none ["select-none" {:-webkit-user-select :none :user-select :none}]
   :select-text ["select-text" {:-webkit-user-select :text :user-select :text}]
   :select-all ["select-all" {:-webkit-user-select :all :user-select :all}]
   :select-auto ["select-auto" {:-webkit-user-select :auto :user-select :auto}]
   :pointer-events-none ["pointer-events-none" {:pointer-events :none}]
   :pointer-events-auto ["pointer-events-auto" {:pointer-events :auto}]
   :will-change-auto ["will-change-auto" {:will-change :auto}]
   :will-change-scroll ["will-change-scroll" {:will-change :scroll-position}]
   :will-change-contents ["will-change-contents" {:will-change :contents}]
   :will-change-transform ["will-change-transform" {:will-change :transform}]
   :touch-auto ["touch-auto" {:touch-action :auto}]
   :touch-none ["touch-none" {:touch-action :none}]
   :touch-pan-x ["touch-pan-x" {:touch-action :pan-x}]
   :touch-pan-left ["touch-pan-left" {:touch-action :pan-left}]
   :touch-pan-right ["touch-pan-right" {:touch-action :pan-right}]
   :touch-pan-y ["touch-pan-y" {:touch-action :pan-y}]
   :touch-pan-up ["touch-pan-up" {:touch-action :pan-up}]
   :touch-pan-down ["touch-pan-down" {:touch-action :pan-down}]
   :touch-pinch-zoom ["touch-pinch-zoom" {:touch-action :pinch-zoom}]
   :touch-manipulation ["touch-manipulation" {:touch-action :manipulation}]
   :cursor-auto ["cursor-auto" {:cursor :auto}]
   :cursor-default ["cursor-default" {:cursor :default}]
   :cursor-pointer ["cursor-pointer" {:cursor :pointer}]
   :cursor-wait ["cursor-wait" {:cursor :wait}]
   :cursor-text ["cursor-text" {:cursor :text}]
   :cursor-move ["cursor-move" {:cursor :move}]
   :cursor-help ["cursor-help" {:cursor :help}]
   :cursor-not-allowed ["cursor-not-allowed" {:cursor :not-allowed}]
   :cursor-none ["cursor-none" {:cursor :none}]
   :cursor-context-menu ["cursor-context-menu" {:cursor :context-menu}]
   :cursor-progress ["cursor-progress" {:cursor :progress}]
   :cursor-cell ["cursor-cell" {:cursor :cell}]
   :cursor-crosshair ["cursor-crosshair" {:cursor :crosshair}]
   :cursor-vertical-text ["cursor-vertical-text" {:cursor :vertical-text}]
   :cursor-alias ["cursor-alias" {:cursor :alias}]
   :cursor-copy ["cursor-copy" {:cursor :copy}]
   :cursor-no-drop ["cursor-no-drop" {:cursor :no-drop}]
   :cursor-grab ["cursor-grab" {:cursor :grab}]
   :cursor-grabbing ["cursor-grabbing" {:cursor :grabbing}]
   :cursor-all-scroll ["cursor-all-scroll" {:cursor :all-scroll}]
   :cursor-col-resize ["cursor-col-resize" {:cursor :col-resize}]
   :cursor-row-resize ["cursor-row-resize" {:cursor :row-resize}]
   :cursor-n-resize ["cursor-n-resize" {:cursor :n-resize}]
   :cursor-e-resize ["cursor-e-resize" {:cursor :e-resize}]
   :cursor-s-resize ["cursor-s-resize" {:cursor :s-resize}]
   :cursor-w-resize ["cursor-w-resize" {:cursor :w-resize}]
   :cursor-ne-resize ["cursor-ne-resize" {:cursor :ne-resize}]
   :cursor-nw-resize ["cursor-nw-resize" {:cursor :nw-resize}]
   :cursor-se-resize ["cursor-se-resize" {:cursor :se-resize}]
   :cursor-sw-resize ["cursor-sw-resize" {:cursor :sw-resize}]
   :cursor-ew-resize ["cursor-ew-resize" {:cursor :ew-resize}]
   :cursor-ns-resize ["cursor-ns-resize" {:cursor :ns-resize}]
   :cursor-nesw-resize ["cursor-nesw-resize" {:cursor :nesw-resize}]
   :cursor-nwse-resize ["cursor-nwse-resize" {:cursor :nwse-resize}]
   :cursor-zoom-in ["cursor-zoom-in" {:cursor :zoom-in}]
   :cursor-zoom-out ["cursor-zoom-out" {:cursor :zoom-out}]
   :resize-none ["resize-none" {:resize :none}]
   :resize-y ["resize-y" {:resize :vertical}]
   :resize-x ["resize-x" {:resize :horizontal}]
   :resize ["resize" {:resize :both}]
   :appearance-none ["appearance-none" {:appearance :none}]
   :appearance-auto ["appearance-auto" {:appearance :auto}]})

(defn static* [env utility]
  (let [[class-name declaration] (get static-utilities utility)]
    (compiler/simple-utility* env class-name declaration)))

(defn property-value*
  "Macro implementation for token-backed or literal interaction property values."
  [env utility-prefix property value-form label type]
  (let [resolved-value (compiler/resolve-compile-time-constant env value-form)
        [class-part css-value]
        (value/string-or-token-parts value-form resolved-value label type)]
    (compiler/register-utility! env (str utility-prefix "-" class-part)
                                {property css-value})))

(defn will-change*
  "Macro implementation for token-backed or literal will-change values."
  [env value-form]
  (property-value* env "will-change" :will-change value-form "Will change" :will-change))

(defn cursor*
  "Macro implementation for token-backed or literal cursor values."
  [env value-form]
  (property-value* env "cursor" :cursor value-form "Cursor" :cursor))
