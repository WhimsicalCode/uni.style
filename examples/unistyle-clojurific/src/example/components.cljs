(ns example.components
  (:require ["@base-ui/react/button" :refer [Button]]
            ["@base-ui/react/radio" :refer [Radio]]
            ["@base-ui/react/radio-group" :refer [RadioGroup]]
            ["@base-ui/react/switch" :refer [Switch]]
            [example.theme :as theme]
            [uni.style.colors :as sc]
            [uni.style.core :as s]))

(s/def-component focus-ring
  (s/outline-none)
  (s/focus-visible
    (s/outline 2 theme/ring)
    (s/outline-offset 3)))

(s/def-component disabled-state
  (s/attr :data-disabled
    (s/pointer-events-none)
    (s/opacity 50)))

(s/def-component button-base
  focus-ring
  disabled-state
  (s/inline-flex)
  (s/items-center)
  (s/justify-center)
  (s/gap 8)
  (s/shrink 0)
  (s/rounded 6)
  (s/border 1 "transparent")
  (s/font-medium)
  (s/whitespace-nowrap)
  (s/select-none)
  (s/cursor-pointer)
  (s/transition-colors)
  (s/media "(prefers-reduced-motion: reduce)"
    (s/transition-none)))

(s/def-variants button-variants
  {:default [(s/bg theme/primary)
             (s/color theme/primary-foreground)
             (s/shadow "0 1px 2px " (s/alpha sc/black 5))
             (s/hover
               (s/bg (s/alpha theme/primary 90)))]
   :secondary [(s/bg theme/muted)
               (s/color theme/foreground)
               (s/hover
                 (s/bg (s/alpha theme/input 70)))]
   :outline [(s/border-color theme/border)
             (s/bg theme/background)
             (s/shadow "0 1px 2px " (s/alpha sc/black 5))
             (s/hover
               (s/bg theme/muted))]
   :ghost [(s/bg-transparent)
           (s/hover
             (s/bg theme/muted))]
   :destructive [(s/bg theme/destructive)
                 (s/color theme/destructive-foreground)
                 (s/hover
                   (s/bg (s/alpha theme/destructive 90)))]
   :link [(s/bg-transparent)
          (s/color theme/foreground)
          (s/underline-offset 4)
          (s/hover
            (s/underline))]})

(s/def-variants button-sizes
  {:sm [(s/h 32)
        (s/px 12)
        (s/font-size 12)]
   :default [(s/h 36)
             (s/px 16)
             (s/font-size 14)]
   :lg [(s/h 40)
        (s/px 24)
        (s/font-size 14)]
   :icon [(s/size 36)
          (s/p 0)]})

(s/def-animation spin
  {:duration "1s"
   :easing "linear"
   :iterations "infinite"
   :frames {0 [(s/rotate 0)]
            100 [(s/rotate 360)]}})

(defn button*
  "Base UI button. Adds :variant, :size, and :loading? to its usual props."
  [{:keys [variant size loading? class] :or {variant :default size :default} :as props}
   & children]
  (into
    [:> Button
     (-> props
         (dissoc :variant :size :loading?)
         (assoc :disabled (boolean (or loading? (:disabled props)))
                :aria-busy (boolean loading?)
                :class (s/c button-base
                            (get button-variants variant)
                            (get button-sizes size)
                            class)))
     (when loading?
       [:span {:aria-hidden true
               :class (s/c spin
                           (s/size 14)
                           (s/rounded "50%")
                           (s/border 2 "currentColor")
                           (s/border-t 2 "transparent")
                           (s/media "(prefers-reduced-motion: reduce)"
                             (s/animation-none)))}])]
    children))

(s/def-component switch-base
  focus-ring
  disabled-state
  (s/inline-flex)
  (s/items-center)
  (s/shrink 0)
  (s/w 32)
  (s/h 18)
  (s/p 2)
  (s/rounded 999)
  (s/bg theme/input)
  (s/cursor-pointer)
  (s/transition-colors)
  (s/attr :data-checked
    (s/bg theme/primary))
  (s/media "(prefers-reduced-motion: reduce)"
    (s/transition-none)))

(s/def-component switch-thumb
  (s/block)
  (s/size 14)
  (s/rounded "50%")
  (s/bg theme/thumb)
  (s/shadow "0 1px 3px " (s/alpha sc/black 15))
  (s/transition-transform)
  (s/attr :data-checked
    (s/translate-x 14)
    (s/bg theme/primary-foreground))
  (s/media "(prefers-reduced-motion: reduce)"
    (s/transition-none)))

(s/def-variants switch-sizes
  {:sm [(s/w 26)
        (s/h 14)]
   :default [(s/w 32)
             (s/h 18)]})

(s/def-variants thumb-sizes
  {:sm [(s/size 10)
        (s/attr :data-checked
          (s/translate-x 12))]
   :default (s/size 14)})

(defn switch*
  "Base UI switch. Adds :size (:default or :sm); pass a label via aria-label/labelledby."
  [{:keys [size class] :or {size :default} :as props}]
  [:> (.-Root Switch)
   (-> props
       (dissoc :size)
       (assoc :class (s/c switch-base
                          (get switch-sizes size)
                          class)))
   [:> (.-Thumb Switch)
    {:class (s/c switch-thumb
                 (get thumb-sizes size))}]])

(s/def-component radio-control
  focus-ring
  (s/inline-flex)
  (s/items-center)
  (s/justify-center)
  (s/shrink 0)
  (s/size 16)
  (s/rounded "50%")
  (s/border theme/input)
  (s/shadow "0 1px 2px " (s/alpha sc/black 5))
  (s/attr :data-checked
    (s/border-color theme/primary)))

(s/def-component radio-option
  (s/flex)
  (s/items-center)
  (s/gap 10)
  (s/cursor-pointer)
  (s/has "[data-disabled]"
    (s/opacity 50)
    (s/cursor-not-allowed)))

(s/def-component radio-card
  (s/p 16)
  (s/rounded 8)
  (s/border theme/border)
  (s/transition-colors)
  (s/has "[data-checked]"
    (s/border-color theme/primary)
    (s/bg theme/muted))
  (s/media "(prefers-reduced-motion: reduce)"
    (s/transition-none)))

(defn radio-group*
  "Base UI radio group. Adds :options [{:value :label :description :disabled}] and :variant (:default or :cards)."
  [{:keys [options variant class] :as props}]
  (into
    [:> RadioGroup
     (-> props
         (dissoc :options :variant)
         (assoc :class (s/c (s/grid)
                            (s/gap 12)
                            class)))]
    (for [{:keys [value label description disabled]} options]
      ^{:key value}
      [:label {:class (s/c radio-option
                           (when (= variant :cards) radio-card))}
       [:> (.-Root Radio)
        {:value value :disabled disabled :class radio-control}
        [:> (.-Indicator Radio)
         {:class (s/c (s/size 8)
                      (s/rounded "50%")
                      (s/bg theme/primary))}]]
       [:span
        [:span {:class (s/c (s/block)
                            (s/font-medium))} label]
        (when description
          [:span {:class (s/c (s/block)
                              (s/mt 2)
                              (s/text 12 1.5)
                              (s/color theme/muted-foreground))}
           description])]])))
