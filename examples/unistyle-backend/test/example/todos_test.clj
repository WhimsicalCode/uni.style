(ns example.todos-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests testing]]
            [example.server :as server]
            [example.todos :as todos]
            [example.views :as views]
            [ring.middleware.session.memory :as memory]
            [ring.util.codec :as codec]))

(deftest add-todo-test
  (let [state (todos/add-todo todos/initial-state "  A new task  ")]
    (is (= {:id 4 :title "A new task" :completed? false} (get-in state [:todos 4])))
    (is (= 5 (:next-id state)))
    (is (= 4 (count (:todos state)))))
  (testing "invalid input does not change state or consume an ID"
    (doseq [title [nil "" "   " (apply str (repeat 121 "x")) ["one" "two"]]]
      (is (= todos/initial-state (todos/add-todo todos/initial-state title)))))
  (is (= 120 (count (todos/normalize-title (apply str (repeat 120 "x")))))))

(deftest update-todo-test
  (let [state (todos/toggle-todo todos/initial-state 2)]
    (is (true? (get-in state [:todos 2 :completed?])))
    (is (= todos/initial-state (todos/toggle-todo state 2))))
  (is (= todos/initial-state (todos/toggle-todo todos/initial-state 999)))
  (is (= todos/initial-state (todos/delete-todo todos/initial-state 999)))
  (let [state (-> todos/initial-state
                  (todos/delete-todo 3)
                  (todos/add-todo "Another task"))]
    (is (not (contains? (:todos state) 3)))
    (is (= "Another task" (get-in state [:todos 4 :title])))))

(deftest filtering-test
  (is (= [1 2 3] (map :id (todos/visible-todos todos/initial-state "all"))))
  (is (= [2 3] (map :id (todos/visible-todos todos/initial-state "active"))))
  (is (= [1] (map :id (todos/visible-todos todos/initial-state "completed")))))

(deftest render-test
  (let [state (todos/add-todo todos/initial-state "<script>alert(1)</script>")
        html (views/render-page state {:mode "dark" :filter-mode "all" :csrf-token "test-token"})]
    (is (str/starts-with? html "<!doctype html>"))
    (is (str/includes? html "data-theme=\"dark\""))
    (is (str/includes? html "&lt;script&gt;alert(1)&lt;/script&gt;"))
    (is (not (str/includes? html "<script>")))
    (is (not (str/includes? html "UniStyle@")))
    (is (= 9 (count (re-seq #"name=\"__anti-forgery-token\"" html))))
    (is (re-find #"class=\"us-" html))))

(deftest request-test
  (let [store (atom todos/initial-state)
        app (server/make-app store)
        response (app {:request-method :get :uri "/" :headers {}
                       :query-string "theme=dark&filter=completed"})]
    (is (= 200 (:status response)))
    (is (str/includes? (:body response) "Build a page with Hiccup"))
    (is (not (str/includes? (:body response) "Try it without JavaScript"))))
  (testing "POSTs without a session-bound CSRF token cannot mutate the shared list"
    (let [store (atom todos/initial-state)
          response ((server/make-app store)
                    {:request-method :post :uri "/todos" :headers {}
                     :form-params {"title" "Forged task"}})]
      (is (= 403 (:status response)))
      (is (= todos/initial-state @store))))
  (is (= 404 (:status (server/update-handler (atom todos/initial-state) todos/toggle-todo
                                             {:path-params {:id "not-a-number"}}))))
  (is (= {:mode "auto" :filter-mode "all" :csrf-token nil}
         (server/page-options {:params {"theme" "invalid" "filter" "invalid"}}))))

(deftest form-submission-test
  (let [store (atom todos/initial-state)
        sessions (memory/memory-store)
        app (server/make-app store sessions)
        page (app {:request-method :get :uri "/" :headers {}})
        ;; Rebuilding the router must not invalidate already rendered forms.
        app (server/make-app store sessions)
        token (second (re-find #"name=\"__anti-forgery-token\"[^>]*value=\"([^\"]+)\"" (:body page)))
        cookies (get-in page [:headers "Set-Cookie"])
        cookie (first (str/split (if (string? cookies) cookies (first cookies)) #";"))
        fields {"__anti-forgery-token" token "title" "Task from POST"
                "theme" "dark" "filter" "all"}
        post! (fn [uri fields]
                (app {:request-method :post :uri uri
                      :headers {"cookie" cookie "content-type" "application/x-www-form-urlencoded"}
                      :body (java.io.ByteArrayInputStream.
                              (.getBytes (codec/form-encode fields) "UTF-8"))}))
        added (post! "/todos" fields)]
    (is (= 303 (:status added)))
    (is (= "/?theme=dark&filter=all" (get-in added [:headers "Location"])))
    (is (= "Task from POST" (get-in @store [:todos 4 :title])))
    (is (= 303 (:status (post! "/todos/4/toggle" fields))))
    (is (true? (get-in @store [:todos 4 :completed?])))
    (is (= 303 (:status (post! "/todos/4/delete" fields))))
    (is (not (contains? (:todos @store) 4)))
    (is (= 422 (:status (post! "/todos" (assoc fields "title" "   ")))))
    (is (= (:todos todos/initial-state) (:todos @store)))))

(defn -main [& _args]
  (let [{:keys [fail error]} (run-tests 'example.todos-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
