(ns uni.style.impl.host
  "Host-specific macro environments and var resolution."
  (:refer-clojure :exclude [macroexpand-1]))

(defn jvm? [env]
  (not (contains? env :ns)))

(defn cljs-compiler
  "The ClojureScript compiler environment atom being expanded in, or nil. Nil
   without a ClojureScript compiler on the classpath: the JVM doesn't need one."
  []
  (some-> (find-ns 'cljs.env) (ns-resolve '*compiler*) deref))

(defn cljs-host
  "The function named sym of uni.style.impl.cljs-host, loaded on first use: only
   ClojureScript macro expansion calls the ClojureScript compiler."
  [sym]
  (requiring-resolve (symbol "uni.style.impl.cljs-host" (name sym))))

(defn resolve-var [env sym]
  (if (jvm? env)
    (when-not (contains? env sym)
      (let [v (ns-resolve *ns* sym)]
        (when (var? v)
          (assoc (meta v)
                 :var v
                 :name (symbol (str (ns-name (:ns (meta v))))
                               (str (:name (meta v))))))))
    ((cljs-host 'resolve-var) env sym)))

(defn definition-symbol [env sym]
  (if (jvm? env)
    (symbol (str (ns-name *ns*)) (name sym))
    (:name (resolve-var env sym))))

(defn macroexpand-1 [env form]
  (if (jvm? env)
    (if-let [v (and (symbol? (first form))
                    (:var (resolve-var env (first form))))]
      (if (:macro (meta v))
        (apply v form env (rest form))
        form)
      form)
    ((cljs-host 'macroexpand-1) env form)))

(defn source-env [env]
  (if (jvm? env)
    {:ns {:name (ns-name *ns*) :meta {:file (or *file* "NO_SOURCE_FILE")}}
     :line (or @clojure.lang.Compiler/LINE 1)
     :column (or @clojure.lang.Compiler/COLUMN 1)}
    env))
