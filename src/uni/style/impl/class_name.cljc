(ns uni.style.impl.class-name
  "The class name of a keyword class, shared by s/c at runtime and the class
   modifiers (ancestor, sel-class, ...) at compile time, so the two agree."
  (:require [clojure.string :as str]))

(defn- namespaced-class-name [ns-name class-name]
  (str (str/replace ns-name "." "_") "--" class-name))

#?(:cljs
   (defonce ^:private namespaced-class-names
     ;; Keyword fqn -> class name, so each namespaced keyword is converted once.
     (js/Map.)))

(defn keyword-class-name
  "The class name of keyword k: its name when unnamespaced, so :menu is
   \"menu\", and <namespace with . as _>--<name> when namespaced, so
   :app.views/menu is \"app_views--menu\"."
  [k]
  (if-let [ns-name (namespace k)]
    #?(:clj (namespaced-class-name ns-name (name k))
       :cljs (let [fqn (.-fqn ^cljs.core/Keyword k)]
               (or (.get namespaced-class-names fqn)
                   (let [class-name (namespaced-class-name ns-name (name k))]
                     (.set namespaced-class-names fqn class-name)
                     class-name))))
    (name k)))
