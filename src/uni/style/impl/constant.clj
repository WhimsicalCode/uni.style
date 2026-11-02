(ns uni.style.impl.constant
  "Compile-time constant resolution for Clojure and ClojureScript."
  (:refer-clojure :exclude [macroexpand macroexpand-1])
  (:require [clojure.java.io :as io]
            [uni.style.impl.host :as host]
            [uni.style.impl.resolution :as resolution]
            [uni.style.impl.tokens :as tokens])
  (:import [clojure.lang ExceptionInfo LineNumberingPushbackReader]))

(defn- read-clj-source-form
  "Read the Clojure form starting at line of the source file or resource, with
   the reader settings of the var's namespace, so its aliases resolve."
  [filepath line var-ns]
  (let [file (io/file filepath)
        file (if (.exists file) file (io/resource filepath))]
    (when file
      (with-open [rdr (LineNumberingPushbackReader. (io/reader file))]
        (dotimes [_ (dec line)]
          (.readLine rdr))
        (binding [*ns* (or var-ns *ns*)
                  *read-eval* false]
          (read {:read-cond :allow :features #{:clj}} rdr))))))

(defn- source [env x]
  (if (host/jvm? env)
    (let [v (host/resolve-var env x)]
      (when (and (:file v) (:line v))
        (read-clj-source-form (:file v) (:line v) (:ns v))))
    (let [v ((host/cljs-host 'resolve-api) env x)]
      (when (and (:file v) (:line v))
        ((host/cljs-host 'read-source-form) (:file v) (:line v))))))

(defn referenced-vector
  "Return source forms and their resolution context when sym names a vector def."
  [env sym]
  (when (symbol? sym)
    (let [resolved-var (host/resolve-var env sym)
          definition (source env sym)
          initializer (when (and (seq? definition)
                                 (= "def" (some-> definition first name))
                                 (< 2 (count definition)))
                        (last definition))]
      (when (vector? initializer)
        (let [qualified-sym (:name resolved-var)
              ns-sym (some-> qualified-sym namespace symbol)
              source-env (if (host/jvm? env)
                           env
                           (let [ns-data (get-in @(host/cljs-compiler)
                                                 [:cljs.analyzer/namespaces ns-sym])]
                             (-> env
                                 (assoc :ns (assoc ns-data :name ns-sym))
                                 (assoc :line (:line resolved-var)
                                        :column (:column resolved-var)))))]
          {:env source-env
           :forms initializer
           :ns ns-sym})))))

(def ^:private skip-expand '#{str clojure.core/str + - * / inc dec})

(defn- macroexpand-1 [env form]
  (if (seq? form)
    (let [op (first form)]
      (if (contains? skip-expand op)
        form
        (host/macroexpand-1 env form)))
    form))

(defn macroexpand
  "Expands macros except the constant operations evaluated directly below."
  [env form]
  (if (seq? form)
    (loop [form form
           expanded (macroexpand-1 env form)]
      (if (and (not (identical? form expanded)) (seq? expanded))
        (recur expanded (macroexpand-1 env expanded))
        expanded))
    form))

(defn- resolve-const-ref-result [v env]
  (if (host/jvm? env)
    (let [{:keys [var const dynamic]} (host/resolve-var env v)]
      (if (and const (not dynamic) var (bound? var))
        [true @var]
        [false nil]))
    (let [resolved-var ((host/cljs-host 'resolve-api) env v)]
      (if (and (:const resolved-var)
               (= :const (get-in resolved-var [:const-expr :op])))
        [true (get-in resolved-var [:const-expr :val])]
        (if-let [definition (source env v)]
          (let [ast ((host/cljs-host 'analyze-no-warn) env definition)]
            (if (= :const (-> ast :init :op))
              [true (-> ast :init :val)]
              [false nil]))
          [false nil])))))

(defn- literal? [value]
  (or (string? value) (number? value) (keyword? value)))

(defn- divide [& args]
  (let [result (apply / args)]
    (if (ratio? result) (double result) result)))

(def ^:private evaluators
  {'cljs.core/str [literal? str]
   'clojure.core/str [literal? str]
   'cljs.core/+ [number? +]
   'clojure.core/+ [number? +]
   'cljs.core/- [number? -]
   'clojure.core/- [number? -]
   'cljs.core/* [number? *]
   'clojure.core/* [number? *]
   'cljs.core// [number? divide]
   'clojure.core// [number? divide]
   'cljs.core/inc [number? inc]
   'clojure.core/inc [number? inc]
   'cljs.core/dec [number? dec]
   'clojure.core/dec [number? dec]
   'cljs.core/name [#(or (keyword? %) (string? %) (symbol? %)) name]
   'clojure.core/name [#(or (keyword? %) (string? %) (symbol? %)) name]})

(defn- analyzer-definitions []
  (for [[_ ns-data] (:cljs.analyzer/namespaces @(host/cljs-compiler))
        [_ v] (:defs ns-data)
        :let [entry (or (:uni.style/definition v)
                        (get-in v [:meta :uni.style/definition]))]
        :when entry]
    entry))

(defn- token-value [env args]
  (when-not (<= 1 (count args) 2)
    (throw (ex-info "token-value expects a token and optional theme" {:args args})))
  (if (host/jvm? env)
    (apply tokens/token-value args)
    (let [definitions (analyzer-definitions)
          themes (into #{} (comp (filter #(= :theme (:kind %))) (map :name)) definitions)
          theme (second args)]
      (when-not (or (nil? theme) (contains? themes theme))
        (throw (ex-info "Unknown theme" {:theme theme})))
      (tokens/value-in (into {} (comp (filter #(= :token (:kind %)))
                                      (map (juxt :name identity))) definitions)
                       (first args) theme))))

(declare eval-expression)

(defn- resolve-argument [env argument]
  (if (symbol? argument)
    (let [[resolved? value] (resolve-const-ref-result argument env)]
      (if resolved?
        value
        (throw (ex-info "Dynamic expression" {:dynamic? true}))))
    argument))

(defn- evaluate [env args predicate operation]
  (let [args (map #(resolve-argument env %) args)]
    (if (every? predicate args)
      (apply operation args)
      (throw (ex-info "Dynamic expression" {:dynamic? true})))))

(defn- eval-expression [env expression]
  (let [expression (macroexpand env expression)]
    (cond
      (seq? expression)
      (if (symbol? (first expression))
        (let [[var-sym & arg-forms] expression
              resolved-sym (or (:name (host/resolve-var env var-sym)) var-sym)]
          (cond
            (= 'uni.style.core/style-class resolved-sym)
            (let [[class-form _version-form create-descriptor-form] arg-forms
                  flat-operations (eval-expression env (last create-descriptor-form))
                  operations (mapv vec (partition 2 flat-operations))]
              (resolution/style-ir
                (eval-expression env class-form)
                {:operations operations
                 :classes (vec (distinct (keep second operations)))}))

            (= 'uni.style.core/c resolved-sym)
            (resolution/combine-style-irs
              (map #(eval-expression env %) arg-forms))

            :else
            (let [args (doall (map #(eval-expression env %) arg-forms))]
              (case resolved-sym
                uni.style.core/token-value (token-value env args)
                (if-let [[predicate operation] (evaluators resolved-sym)]
                  (evaluate env args predicate operation)
                  (throw (ex-info "Dynamic expression" {:dynamic? true})))))))
        (doall (map #(eval-expression env %) expression)))

      (vector? expression)
      (mapv #(eval-expression env %) expression)

      (map? expression)
      (into (empty expression)
            (map (fn [[k v]] [(eval-expression env k) (eval-expression env v)]))
            expression)

      (symbol? expression)
      (resolve-argument env expression)

      :else expression)))

(defn resolve-compile-time-constant
  "Resolve a literal, constant var, or supported constant expression.
   Throws when the form depends on a runtime value."
  [env form]
  (try
    (eval-expression env form)
    (catch ExceptionInfo e
      (if (:dynamic? (ex-data e))
        (throw (ex-info "Expected a compile-time constant expression"
                        {:form form :dynamic? true}
                        e))
        (throw e)))))
