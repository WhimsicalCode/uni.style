(ns example.dev
  (:require [example.build :as build]
            [example.server :as server]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]
            [uni.style.build :as style.build]))

(defn -main [& _args]
  (let [handler (-> #'server/app
                    (style.build/wrap-css-reload
                      {:source-paths ["src"]
                       :compile-fn #'build/compile-styles!
                       :output-to "resources/public/css/bundle.css"})
                    (wrap-reload {:dirs ["src"]}))]
    (println "Todo example (source + CSS reload): http://localhost:8093")
    ;; Coordinate Ring's namespace reloads with CSS compilation and rendering.
    (jetty/run-jetty (fn [request] (locking handler (handler request)))
                     {:host "127.0.0.1" :port 8093 :join? true})))
