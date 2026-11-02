(ns example.core
  (:require [example.components :as ui]
            [example.theme :as theme]
            [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [uni.style.colors :as sc]
            [uni.style.core :as s]))

(def row
  (s/c (s/flex)
       (s/items-center)
       (s/flex-wrap)
       (s/gap 12)))

(def muted-text
  (s/c (s/color theme/muted-foreground)))

(def eyebrow
  (s/c muted-text
       (s/font-size 11)
       (s/font-medium)
       (s/uppercase)
       (s/tracking 1.2)))

(def panel
  (s/c (s/border theme/border)
       (s/rounded 12)
       (s/overflow-hidden)))

(def panel-body
  (s/c (s/p 28)
       (s/media "(max-width: 600px)"
         (s/p 20))))

(def panel-heading
  (s/c (s/font-semibold)
       (s/mb 4)))

(def caption
  (s/c muted-text
       (s/text 12 1.6)))

(def section-grid
  (s/c (s/grid)
       (s/grid-cols "repeat(2, minmax(0, 1fr))")
       (s/gap 20)
       (s/media "(max-width: 760px)"
         (s/grid-cols "minmax(0, 1fr)"))))

(def preference-row
  (s/c (s/flex)
       (s/items-center)
       (s/justify-between)
       (s/gap 24)
       (s/py 18)
       (s/not
         (s/last
           (s/border-b theme/border)))))

(defn icon* [kind]
  [:svg {:width 16 :height 16 :viewBox "0 0 24 24" :fill "none"
         :stroke "currentColor" :stroke-width 1.7 :stroke-linecap "round"
         :stroke-linejoin "round" :aria-hidden true}
   (case kind
     :plus [:path {:d "M12 5v14M5 12h14"}]
     :arrow [:path {:d "M5 12h14m-5-5 5 5-5 5"}]
     :sun [:g [:circle {:cx 12 :cy 12 :r 4}]
           [:path {:d "M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5"}]]
     :moon [:path {:d "M20.5 14A9 9 0 0 1 10 3.5 9 9 0 1 0 20.5 14Z"}]
     :monitor [:g [:rect {:x 3 :y 4 :width 18 :height 13 :rx 2}]
               [:path {:d "M8 21h8m-4-4v4"}]]
     :code [:path {:d "m8 7-5 5 5 5m8-10 5 5-5 5m-3-14-2 18"}])])

(defn theme-picker* []
  (let [selected-mode @theme/mode-]
    [:div {:role "group" :aria-label "Color mode"
           :class (s/c row
                       (s/gap 2)
                       (s/p 3)
                       (s/rounded 8)
                       (s/bg theme/muted))}
     (for [[mode label icon] [["auto" "Auto" :monitor]
                              ["light" "Light" :sun]
                              ["dark" "Dark" :moon]]]
       ^{:key mode}
       [ui/button* {:size :sm :variant :ghost
                    :aria-pressed (= mode selected-mode)
                    :on-click #(theme/select-mode! mode)
                    :class (when (= mode selected-mode)
                             (s/c (s/bg theme/background)
                                  (s/shadow "0 1px 3px " (s/alpha sc/black 10))))}
        [icon* icon] label])]))

(defn section-title* [title]
  [:div {:class (s/c (s/mb 20)
                     (s/scroll-mt 24))}
   [:div {:class row}
    [:h2 {:class (s/c (s/font-semibold)
                      (s/font-size 22)
                      (s/tracking -0.5))}
     title]]])

(defn sample-row* [label & children]
  [:div {:class (s/c (s/grid)
                     (s/gap 12))}
   [:h3 {:class eyebrow} label]
   (into [:div {:class row}] children)])

(defonce clicks- (r/atom 0))
(defonce preferences- (r/atom {:notifications true :marketing false}))
(defonce density- (r/atom "comfortable"))
(defonce plan- (r/atom "pro"))

(defn button-section* []
  [:section
   [section-title* "Button"]
   [:div {:class panel}
    [:div {:class (s/c panel-body
                       (s/grid)
                       (s/gap 28))}
     [sample-row* "Variants"
      [ui/button* {:on-click #(swap! clicks- inc)} "Default"]
      [ui/button* {:variant :secondary :on-click #(swap! clicks- inc)} "Secondary"]
      [ui/button* {:variant :outline :on-click #(swap! clicks- inc)} "Outline"]
      [ui/button* {:variant :ghost :on-click #(swap! clicks- inc)} "Ghost"]
      [ui/button* {:variant :destructive :on-click #(swap! clicks- inc)} "Destructive"]
      [ui/button* {:variant :link :on-click #(swap! clicks- inc)} "Link"]]
     [sample-row* "Sizes & icons"
      [ui/button* {:size :sm :variant :outline :on-click #(swap! clicks- inc)} "Small"]
      [ui/button* {:variant :outline :on-click #(swap! clicks- inc)} "Regular"]
      [ui/button* {:size :lg :variant :outline :on-click #(swap! clicks- inc)} "Large"]
      [ui/button* {:on-click #(swap! clicks- inc)} [icon* :plus] "New project"]
      [ui/button* {:size :icon :variant :outline :aria-label "Add item"
                   :on-click #(swap! clicks- inc)} [icon* :plus]]
      [ui/button* {:variant :ghost :on-click #(swap! clicks- inc)} "Continue" [icon* :arrow]]]
     [sample-row* "States"
      [ui/button* {:disabled true} "Disabled"]
      [ui/button* {:variant :outline :disabled true} "Disabled outline"]
      [ui/button* {:loading? true} "Please wait"]
      [:span {:role "status" :data-testid "button-count" :class caption}
       (str "Button clicks: " @clicks-)]]]]])

(defn preference* [id label description]
  [:div {:class preference-row}
   [:div
    [:label {:id (str (name id) "-label") :for (name id) :class (s/font-medium)} label]
    [:p {:id (str (name id) "-description") :class caption} description]]
   [ui/switch* {:id (name id)
                :aria-labelledby (str (name id) "-label")
                :aria-describedby (str (name id) "-description")
                :checked (get @preferences- id)
                :on-checked-change (fn [checked _] (swap! preferences- assoc id checked))}]])

(defn switch-state* [label props]
  [:div {:class (s/c row
                     (s/justify-between))}
   [:span {:class caption} label]
   [ui/switch* (assoc props :aria-label label)]])

(defn switch-section* []
  [:section
   [section-title* "Switch"]
   [:div {:class section-grid}
    [:div {:class panel}
     [:div {:class panel-body}
      [:h3 {:class panel-heading} "Keep in the loop"]
      [:p {:class caption} "Controlled switches, backed by a Reagent atom."]
      [preference* :notifications "Push notifications" "Updates that matter, right when they happen."]
      [preference* :marketing "Product news" "Occasional notes on what’s new."]
      [:p {:role "status" :class caption :data-testid "preferences-status"}
       (str "Push " (if (:notifications @preferences-) "on" "off")
            " · News " (if (:marketing @preferences-) "on" "off"))]]]
    [:div {:class panel}
     [:div {:class panel-body}
      [:h3 {:class panel-heading} "Size & state"]
      [:p {:class caption} "Uncontrolled examples with independent state."]
      [:div {:class (s/c (s/grid)
                         (s/gap 20)
                         (s/mt 24))}
       [switch-state* "Default, off" {:default-checked false}]
       [switch-state* "Default, on" {:default-checked true}]
       [switch-state* "Small" {:size :sm :default-checked true}]
       [switch-state* "Disabled, off" {:disabled true}]
       [switch-state* "Disabled, on" {:disabled true :default-checked true}]]]]]])

(def density-options
  [{:value "default" :label "Default"}
   {:value "comfortable" :label "Comfortable"}
   {:value "compact" :label "Compact"}])

(defn radio-section* []
  [:section
   [section-title* "Radio group"]
   [:div {:class section-grid}
    [:div {:class panel}
     [:div {:class panel-body}
      [:h3 {:id "density-label" :class panel-heading} "Display density"]
      [:p {:class (s/c caption
                       (s/mb 24))} "A classic radio group with a controlled value."]
      [ui/radio-group* {:aria-labelledby "density-label"
                        :name "density" :value @density-
                        :on-value-change (fn [value _] (reset! density- value))
                        :options density-options}]
      [:p {:role "status" :data-testid "density-status"
           :class (s/c caption
                       (s/mt 20))} (str "Selected: " @density-)]]
     [:div {:class (s/c panel-body
                        (s/border-t theme/border))}
      [:h3 {:id "disabled-density-label" :class (s/c panel-heading
                                                     (s/mb 16))} "Disabled group"]
      [ui/radio-group* {:aria-labelledby "disabled-density-label"
                        :name "disabled-density" :disabled true :default-value "comfortable"
                        :options density-options}]]]
    [:div {:class panel}
     [:div {:class panel-body}
      [:h3 {:id "plan-label" :class panel-heading} "Make it a card"]
      [:p {:class (s/c caption
                       (s/mb 24))} "The same primitive, with a little more context."]
      [ui/radio-group* {:aria-labelledby "plan-label" :variant :cards
                        :name "plan" :value @plan-
                        :on-value-change (fn [value _] (reset! plan- value))
                        :options [{:value "free" :label "Free" :description "A little space to get started."}
                                  {:value "pro" :label "Pro" :description "More room for your next big idea."}
                                  {:value "team" :label "Team" :description "Coming soon" :disabled true}]}]
      [:p {:role "status" :data-testid "plan-status"
           :class (s/c caption
                       (s/mt 20))} (str "Selected: " @plan-)]]]]])

(defn app* []
  [:div {:class (s/c (s/max-w 1080)
                     (s/mx-auto)
                     (s/px 32)
                     (s/media "(max-width: 600px)"
                       (s/px 20)))}
   [:header {:class (s/c row
                         (s/justify-between)
                         (s/py 24)
                         (s/border-b theme/border))}
    [:a {:href "#" :class (s/c row
                               (s/gap 10)
                               (s/font-semibold))}
     [:span {:class (s/c (s/inline-flex)
                         (s/items-center)
                         (s/justify-center)
                         (s/size 28)
                         (s/rounded 6)
                         (s/bg theme/foreground)
                         (s/color theme/background))} [icon* :code]]
     "uni.style" [:span {:class (s/c muted-text
                                     (s/font-normal))} "/ Reagent example"]]
    [theme-picker*]]
   [:main
    [:div {:class (s/c (s/pb 40)
                       (s/media "(max-width: 600px)"
                         (s/pt 36)))}
     [:nav {:aria-label "Components" :class (s/c row
                                                 (s/mt 24)
                                                 (s/gap 24))}]]
    [:div {:class (s/c (s/grid)
                       (s/grid-cols "minmax(0, 1fr)")
                       (s/gap 48)
                       (s/pb 56))}
     [button-section*]
     [switch-section*]
     [radio-section*]]]
   [:footer {:class (s/c row
                         caption
                         (s/justify-between)
                         (s/border-t theme/border)
                         (s/py 24))}
    [:p "Built with Reagent + Base UI + uni.style"]
    [:p "No Tailwind. No runtime stylesheet. Just ClojureScript."]]])

(defonce root- (atom nil))

(defn reload! []
  (theme/apply-mode! @theme/mode-)
  (rdom/render @root- [app*]))

(defn init! []
  (theme/init!)
  (when-not @root-
    (reset! root- (rdom/create-root (.getElementById js/document "app"))))
  (rdom/render @root- [app*]))
