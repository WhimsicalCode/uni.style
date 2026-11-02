(ns example.views
  (:require [example.styles :as styles]
            [example.todos :as todos]
            [hiccup2.core :as h]
            [uni.style.colors :as sc]
            [uni.style.core :as s]))

(def panel
  (s/c (s/border styles/border)
       (s/rounded 12)
       (s/overflow-hidden)))

(def secondary-text
  (s/c (s/color styles/muted-foreground)
       (s/text 13 1.6)))

(defn page-url [{:keys [mode filter-mode]}]
  (str "/?theme=" mode "&filter=" filter-mode))

(defn form-fields* [{:keys [csrf-token mode filter-mode]}]
  (list
    [:input {:type "hidden" :name "__anti-forgery-token" :value csrf-token}]
    [:input {:type "hidden" :name "theme" :value mode}]
    [:input {:type "hidden" :name "filter" :value filter-mode}]))

(defn theme-picker* [{:keys [mode] :as options}]
  [:nav {:aria-label "Color mode"
         :class (s/c styles/row
                     (s/gap 2)
                     (s/p 3)
                     (s/rounded 8)
                     (s/bg styles/muted))}
   (for [[value label] [["auto" "Auto"] ["light" "Light"] ["dark" "Dark"]]]
     [:a {:href (page-url (assoc options :mode value))
          :aria-current (when (= mode value) "page")
          :class (s/c styles/quiet-button
                      (s/h 30)
                      (s/font-size 12)
                      (when (= mode value)
                        (s/c (s/bg styles/background)
                             (s/color styles/foreground)
                             (s/shadow "0 1px 3px " (s/alpha sc/black 10)))))}
      label])])

(defn todo-row* [{:keys [id title completed?]} options]
  [:li {:class (s/c styles/row
                    (s/px 24)
                    (s/py 16)
                    (s/not
                      (s/last
                        (s/border-b styles/border))))}
   [:form {:method "post" :action (str "/todos/" id "/toggle")}
    (form-fields* options)
    [:button {:type "submit"
              :aria-label (str (if completed? "Mark active: " "Complete: ") title)
              :aria-pressed (str completed?)
              :class (s/c styles/focus-ring
                          (s/flex)
                          (s/items-center)
                          (s/justify-center)
                          (s/size 22)
                          (s/rounded "50%")
                          (s/border styles/border)
                          (s/cursor-pointer)
                          (when completed?
                            (s/c (s/bg styles/foreground)
                                 (s/color styles/background)
                                 (s/border-color styles/foreground))))}
     [:span {:aria-hidden "true"} (when completed? "✓")]]]
   [:span {:class (s/c (s/flex-1)
                       (s/min-w 0)
                       (s/font-size 14)
                       (s/overflow-wrap-anywhere)
                       (when completed?
                         (s/c (s/line-through)
                              (s/color styles/muted-foreground))))}
    title]
   [:form {:method "post" :action (str "/todos/" id "/delete")}
    (form-fields* options)
    [:button {:type "submit"
              :aria-label (str "Delete: " title)
              :class styles/quiet-button}
     "Delete"]]])

(defn todo-list* [state {:keys [filter-mode] :as options}]
  (let [visible (todos/visible-todos state filter-mode)]
    [:div {:class panel}
     [:nav {:aria-label "Filter todos"
            :class (s/c styles/row
                        (s/flex-wrap)
                        (s/gap 4)
                        (s/p 12)
                        (s/border-b styles/border))}
      (for [[value label] [["all" "All"] ["active" "Active"] ["completed" "Completed"]]]
        [:a {:href (page-url (assoc options :filter-mode value))
             :aria-current (when (= filter-mode value) "page")
             :class (s/c styles/quiet-button
                         (when (= filter-mode value)
                           (s/c (s/bg styles/muted)
                                (s/color styles/foreground))))}
         label " · " (count (todos/visible-todos state value))])]
     (if (seq visible)
       [:ul
        (for [todo visible]
          (todo-row* todo options))]
       [:p {:class (s/c secondary-text
                        (s/p 40)
                        (s/text-center))}
        "Nothing here. Add a task or try another filter."])]))

(defn page* [state {:keys [mode title error] :as options}]
  (let [active-count (count (todos/visible-todos state "active"))]
    [:html {:lang "en"
            :data-theme mode
            :class (case mode "dark" styles/dark "light" styles/light nil)}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title "Todo list · uni.style backend"]
      [:link {:rel "icon" :href "data:,"}]
      [:link {:rel "stylesheet" :href "/css/bundle.css"}]]
     [:body
      [:div {:class (s/c (s/max-w 820)
                         (s/mx-auto)
                         (s/px 24))}
       [:header {:class (s/c styles/row
                             (s/flex-wrap)
                             (s/justify-between)
                             (s/py 24)
                             (s/border-b styles/border))}
        [:a {:href (page-url options)
             :class (s/c styles/focus-ring
                         (s/font-semibold))}
         "uni.style "
         [:span {:class (s/c (s/font-normal)
                             (s/color styles/muted-foreground))}
          "/ Backend example"]]
        (theme-picker* options)]
       [:main {:class (s/c (s/py 48)
                           (s/grid)
                           (s/gap 28))}
        [:form {:method "post" :action "/todos"}
         (form-fields* options)
         [:label {:for "todo-title"
                  :class (s/c (s/block)
                              (s/mb 8)
                              (s/font-size 14)
                              (s/font-medium))}
          "What needs doing?"]
         [:div {:class styles/row}
          [:input {:id "todo-title" :name "title" :type "text" :required true
                   :maxlength 120 :value title :placeholder "Add a task…"
                   :aria-invalid (when error "true")
                   :aria-describedby (when error "todo-error")
                   :class (s/c styles/focus-ring
                               (s/min-w 0)
                               (s/flex-1)
                               (s/h 40)
                               (s/px 12)
                               (s/rounded 6)
                               (s/border styles/border)
                               (s/bg styles/background)
                               (s/font-size 14))}]
          [:button {:type "submit"
                    :class (s/c styles/button
                                (s/h 40))}
           "Add task"]]
         (when error
           [:p {:id "todo-error" :role "alert"
                :class (s/c secondary-text
                            (s/mt 8))}
            error])]
        (todo-list* state options)
        [:p {:class secondary-text :role "status"}
         (if (zero? active-count)
           "All done. Enjoy the breathing room."
           (str active-count " " (if (= active-count 1) "task" "tasks") " left."))]]
       [:footer {:class (s/c secondary-text
                             (s/py 24)
                             (s/border-t styles/border))}
        "Shared, in-memory demo. Restarting the server resets the list."]]]]))

(defn render-page [state options]
  (str "<!doctype html>" (h/html (page* state options))))
