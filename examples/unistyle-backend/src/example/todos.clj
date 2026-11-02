(ns example.todos
  (:require [clojure.string :as str]))

(def initial-state
  {:next-id 4
   :todos (sorted-map
            1 {:id 1 :title "Build a page with Hiccup" :completed? true}
            2 {:id 2 :title "Style it with uni.style" :completed? false}
            3 {:id 3 :title "Try it without JavaScript" :completed? false})})

(defn normalize-title [title]
  (when (string? title)
    (let [title (str/trim title)]
      (when (<= 1 (count title) 120)
        title))))

(defn add-todo [{:keys [next-id] :as state} title]
  (if-let [title (normalize-title title)]
    (-> state
        (assoc-in [:todos next-id] {:id next-id :title title :completed? false})
        (update :next-id inc))
    state))

(defn toggle-todo [state id]
  (if (contains? (:todos state) id)
    (update-in state [:todos id :completed?] not)
    state))

(defn delete-todo [state id]
  (update state :todos dissoc id))

(defn visible-todos [state filter-mode]
  (let [todos (vals (:todos state))]
    (case filter-mode
      "active" (remove :completed? todos)
      "completed" (filter :completed? todos)
      todos)))
