(ns uni.style.impl.cljs-host
  "The ClojureScript compiler calls of uni.style's macros. Loaded only when a
   ClojureScript build expands them, so the JVM needs no ClojureScript compiler."
  (:refer-clojure :exclude [macroexpand-1])
  (:require [cljs.analyzer :as ana]
            [cljs.analyzer.api :as ana-api]
            [cljs.tagged-literals :as tags]
            [cljs.vendor.clojure.tools.reader :as reader]
            [cljs.vendor.clojure.tools.reader.reader-types :as readers]
            [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(defn resolve-var [env sym]
  (ana/resolve-var env sym))

(defn resolve-api [env sym]
  (ana-api/resolve env sym))

(defn macroexpand-1 [env form]
  (ana/macroexpand-1 env form))

(defn analyze-no-warn [env form]
  (ana/no-warn (ana/analyze env form)))

(defn read-source-form
  "Read the ClojureScript form starting at line of the source file or resource."
  [filepath line]
  (let [file (io/file filepath)
        file (if (.exists file) file (io/resource filepath))]
    (when file
      (with-open [pbr (PushbackReader. (io/reader file))]
        (let [rdr (readers/source-logging-push-back-reader pbr)]
          (dotimes [_ (dec line)]
            (readers/read-line rdr))
          (binding [reader/*alias-map* identity
                    reader/*data-readers* (merge tags/*cljs-data-readers*
                                                 (ana/load-data-readers))
                    reader/*read-eval* false]
            (reader/read {:read-cond :allow :features #{:cljs}} rdr)))))))
