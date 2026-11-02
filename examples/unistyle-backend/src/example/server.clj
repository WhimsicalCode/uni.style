(ns example.server
  (:require [clojure.java.io :as io]
            [example.todos :as todos]
            [example.views :as views]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :as memory]
            [ring.util.response :as response]))

(defonce store- (atom todos/initial-state))
(defonce session-store (memory/memory-store))

(defn page-options [request]
  (let [{:strs [theme filter]} (:params request)]
    {:mode (if (#{"auto" "light" "dark"} theme) theme "auto")
     :filter-mode (if (#{"all" "active" "completed"} filter) filter "all")
     :csrf-token (:anti-forgery-token request)}))

(defn page-response [store request status extra-options]
  {:status status
   :headers {"Content-Type" "text/html; charset=utf-8"
             "Cache-Control" "no-store"}
   :body (views/render-page @store (merge (page-options request) extra-options))})

(defn redirect-back [request]
  {:status 303
   :headers {"Location" (views/page-url (page-options request))}
   :body ""})

(defn add-handler [store request]
  (let [title (get-in request [:form-params "title"])]
    (if-let [title (todos/normalize-title title)]
      (do
        (swap! store todos/add-todo title)
        (redirect-back request))
      (page-response store request 422
                     {:title (when (string? title) title)
                      :error "Enter a task between 1 and 120 characters."}))))

(defn update-handler [store update-todo request]
  (let [id (parse-long (get-in request [:path-params :id]))]
    (if (contains? (:todos @store) id)
      (do
        (swap! store update-todo id)
        (redirect-back request))
      {:status 404 :headers {"Content-Type" "text/plain; charset=utf-8"}
       :body "Todo not found"})))

(defn stylesheet-handler [_request]
  (if-let [resource (response/resource-response "css/bundle.css" {:root "public"})]
    (-> resource
        (response/content-type "text/css; charset=utf-8")
        (response/header "Cache-Control" "no-cache"))
    {:status 404 :headers {"Content-Type" "text/plain; charset=utf-8"}
     :body "Stylesheet missing. Run clojure -M:build first."}))

(defn wrap-security-headers [handler]
  (fn [request]
    (update (handler request) :headers merge
            {"X-Content-Type-Options" "nosniff"
             "Content-Security-Policy" (str "default-src 'none'; style-src 'self'; img-src 'self' data:; "
                                            "form-action 'self'; base-uri 'none'; frame-ancestors 'none'")})))

(defn make-app
  ([store] (make-app store (memory/memory-store)))
  ([store session-store]
   (-> (ring/ring-handler
         (ring/router
           [["/" {:get #(page-response store % 200 {})}]
            ["/css/bundle.css" {:get stylesheet-handler}]
            ["/todos" {:post #(add-handler store %)}]
            ["/todos/:id/toggle" {:post #(update-handler store todos/toggle-todo %)}]
            ["/todos/:id/delete" {:post #(update-handler store todos/delete-todo %)}]])
         (ring/create-default-handler))
       wrap-anti-forgery
       wrap-params
       (wrap-session {:store session-store :cookie-attrs {:http-only true :same-site :lax}})
       wrap-security-headers)))

(def app (make-app store- session-store))

(defn -main [& _args]
  (when-not (io/resource "public/css/bundle.css")
    (throw (ex-info "Run clojure -M:build before starting the server." {})))
  (println "Todo example: http://localhost:8093")
  (jetty/run-jetty #'app {:host "127.0.0.1" :port 8093 :join? true}))
