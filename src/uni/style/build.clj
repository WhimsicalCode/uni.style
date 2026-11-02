(ns uni.style.build
  "Supported JVM CSS export and build integration for uni.style."
  (:require [clojure.java.io :as io]
            [uni.style.impl.build :as impl.build]
            [uni.style.impl.css :as css]
            [uni.style.impl.registry :as registry])
  (:import [java.nio.file AtomicMoveNotSupportedException CopyOption Files LinkOption StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

(defn css
  "Collect JVM CSS while calling the zero-argument compile-fn.

   Load or compile all styled source namespaces inside the callback, in dependency
   order. Already loaded or AOT-compiled namespaces do not replay macro expansion;
   explicitly reload source or compile it again. Each call has an isolated registry.
   Returns a stylesheet string, including the global cascade-layer order."
  [compile-fn]
  (let [collector (atom {:styles {} :ns-order {}})]
    (binding [registry/*jvm-styles* collector]
      (registry/register-global! nil 'uni.style.core "uni-style-layer-order" css/layer-priority-css)
      (registry/register-global! nil 'uni.style.core "uni-style-properties" css/property-css)
      (compile-fn))
    (let [{:keys [styles ns-order]} @collector]
      (impl.build/bundle-css styles ns-order))))

(defn write-css!
  "Collect JVM CSS via compile-fn and write it to output-to. Return the output path.
   Package this stylesheet with the backend; CSS collection is a build-time step."
  [compile-fn output-to]
  (let [stylesheet (css compile-fn)]
    (io/make-parents output-to)
    (spit output-to stylesheet)
    output-to))

(defn- source-snapshot [source-paths]
  (into {}
        (for [path source-paths
              file (file-seq (io/file path))
              :when (and (.isFile file) (re-find #"\.cljc?$" (.getName file)))]
          [(.getCanonicalPath file)
           [(Files/getLastModifiedTime (.toPath file) (make-array LinkOption 0))
            (.length file)]])))

(defn- write-reloaded-css! [compile-fn output-to]
  ;; Publish only complete stylesheets, including when Jetty streams the file.
  (let [stylesheet (css compile-fn)
        output (.toPath (.getAbsoluteFile (io/file output-to)))]
    (io/make-parents output-to)
    (let [temp (Files/createTempFile (.getParent output) ".uni-style-" ".css"
                                     (make-array FileAttribute 0))]
      (try
        (spit (.toFile temp) stylesheet)
        (try
          (Files/move temp output (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE
                                                          StandardCopyOption/REPLACE_EXISTING]))
          (catch AtomicMoveNotSupportedException _
            (Files/move temp output (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING]))))
        (finally (Files/deleteIfExists temp))))))

(defn wrap-css-reload
  "Wrap a Ring-shaped handler with request-triggered JVM CSS rebuilding. Development only.

   - :source-paths - scan these directories/files for .clj and .cljc changes
   - :compile-fn - load/reload all styled namespaces in dependency order (zero arguments)
   - :output-to - write the generated stylesheet to this file

   Build before the first request, after source additions/changes/deletions, and if
   the output disappears. Compare file timestamps and sizes; ignore other extensions.
   Serialize builds and handler invocations. Publish complete CSS before invoking
   the handler; propagate build errors and retry on the next request. Supports both
   synchronous and asynchronous handler arities without depending on Ring.

   This does not reload other application code or refresh the browser. Put Ring's
   wrap-reload outside this middleware to reload application code before collecting
   CSS. The caller owns source loading and coordination with other reloaders."
  [handler {:keys [source-paths compile-fn output-to]}]
  (when-not (and (sequential? source-paths) (seq source-paths) (ifn? compile-fn) output-to)
    (throw (ex-info "CSS reload requires :source-paths, :compile-fn, and :output-to" {})))
  (let [built-sources- (atom nil)
        reload! (fn []
                  (let [sources (source-snapshot source-paths)]
                    (when (or (not= sources @built-sources-)
                              (not (.isFile (io/file output-to))))
                      (write-reloaded-css! compile-fn output-to)
                      (reset! built-sources- sources))))]
    (fn
      ([request]
       (locking built-sources-
         (reload!)
         (handler request)))
      ([request respond raise]
       (locking built-sources-
         (reload!)
         (handler request respond raise))))))

(defn register-global-css
  "Return the Shadow build state with generated global CSS in its analyzer-backed registry.

   For build infrastructure that generates CSS outside uni.style macros.
   Application CSS should be registered through uni.style macros."
  [build-state env ns-sym registry-key css-str]
  (registry/register-build-global
    build-state env ns-sym registry-key css-str))

(defn esm-hook
  "Supported uni.style hook for ClojureScript ES module builds (cljf.esm), as
   :build-hooks [[uni.style.build/esm-hook {:output-to \"...\"}]]."
  [{:keys [compiler-env namespaces trigger changed-namespaces]} options]
  (impl.build/esm-hook compiler-env namespaces options
                       (or trigger :build) (or changed-namespaces namespaces)))

(defn hook
  "Supported uni.style Shadow build hook."
  {:shadow.build/stages #{:compile-finish :flush}}
  [build-state & _args]
  (impl.build/hook build-state))
