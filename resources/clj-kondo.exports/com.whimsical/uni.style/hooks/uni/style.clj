(ns hooks.uni.style
  (:require [clj-kondo.hooks-api :as api]))

(defn def-color [{:keys [node]}]
  (let [[sym anchors] (rest (:children node))
        level (api/token-node 'level)
        alpha (api/token-node 'alpha)]
    {:node (api/list-node
             [(api/token-node 'defmacro) sym
              (api/list-node [(api/vector-node [level])
                              (api/list-node [(api/token-node 'do) anchors level])])
              (api/list-node [(api/vector-node [level alpha])
                              (api/vector-node [level alpha])])])}))

(defn def-theme [{:keys [node]}]
  {:node (api/list-node [(api/token-node 'def)
                         (second (:children node))
                         (api/string-node "")])})

(defn def-component [{:keys [node]}]
  (let [[sym & forms] (rest (:children node))]
    {:node (api/list-node
             [(api/token-node 'def)
              sym
              (api/list-node
                (list* (api/token-node 'do) forms))])}))

(defn css [{:keys [node]}]
  (let [[rules] (rest (:children node))]
    {:node (api/list-node [(api/token-node 'do) rules (api/string-node "")])}))

(defn def-variants [{:keys [node]}]
  (let [[sym variants-map] (rest (:children node))]
    {:node (api/list-node
             [(api/token-node 'def)
              sym
              (api/list-node
                [(api/token-node 'do) variants-map])])}))
