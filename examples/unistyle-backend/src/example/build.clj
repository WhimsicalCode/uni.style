(ns example.build
  (:require [uni.style.build :as s.build]))

(defn compile-styles! []
  ;; Registration happens during macro expansion, not while rendering requests.
  (doseq [ns-sym '[example.styles example.views]]
    (require ns-sym :reload)))

(defn -main [& _args]
  (s.build/write-css! compile-styles! "resources/public/css/bundle.css"))
