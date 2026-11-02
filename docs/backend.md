# JVM / backend usage

The same `uni.style.core` namespace supports Clojure views and shared `.cljc`
components. Convert composed values to strings at your HTML renderer boundary:

```clojure
(ns example.views
  (:require [uni.style.core :as s]))

(def ^:const spacing 8)

(s/def-component card
  (s/bg "white")
  (s/rounded 4))

(defn card* [compact?]
  [:div {:class (s/class-name
                  (s/c card (s/p spacing) (when compact? (s/p 4))))}
   "Hello"])
```

## Build the stylesheet

`uni.style.build/css` takes a zero-argument function that loads or compiles styled
source namespaces and returns their CSS. `write-css!` writes the same CSS to a
file, creating parent directories:

```clojure
(require '[uni.style.build :as style.build])

(style.build/write-css!
  #(require 'example.views :reload)
  "resources/public/css/backend.css")

;; Or return a stylesheet string:
(style.build/css #(require 'example.views :reload))
```

Run this in a dedicated build JVM. Load or compile **all styled namespaces inside
that callback, in dependency order**. For multiple namespaces, for example:

```clojure
(style.build/write-css!
  #(doseq [ns-sym '[example.components example.views]]
     (require ns-sym :reload))
  "resources/public/css/backend.css")
```

The callback controls loading; the library does not implicitly reload application
namespaces. Each collection is isolated, so previous builds and failed collections
do not contribute stale CSS. Requiring an already loaded namespace without
reloading it does not expand its macros again. Likewise, `(style.build/css
#(s/flex))` does **not** collect that utility: its macro expanded before the callback
ran. This is a source-compilation API, not request-time style collection.

Serve the generated stylesheet with your backend HTML. No Shadow build or generated
JavaScript is needed. JVM export includes the cascade-layer order and uses the same
CSS sorting, atomic deduplication, and conflict checks as the frontend.

## Development reload

`uni.style.build/wrap-css-reload` provides opt-in, request-triggered CSS rebuilding
for JVM backends. It accepts a Ring-shaped handler but adds no Ring dependency:

```clojure
(def dev-handler
  (style.build/wrap-css-reload #'app
    {:source-paths ["src"]
     :compile-fn #(doseq [ns-sym '[example.components example.views]]
                    (require ns-sym :reload))
     :output-to "resources/public/css/backend.css"}))
```

It builds before the first request, then only when `.clj`/`.cljc` files are added,
removed, or change timestamp/size, or when the output file disappears. The callback
must still reload **all styled namespaces** in dependency order. A complete
stylesheet is published before the handler runs; failed builds propagate their
error, keep the previous stylesheet, and retry on the next request. There is no
background watcher, browser refresh, or JavaScript injection.

For application code reload, put Ring's `wrap-reload` **outside** this middleware,
so application dependencies reload before CSS collection. Pass handler and compiler
Vars when their definitions can change. Coordinate other reloaders with CSS
compilation: the [backend example](../examples/unistyle-backend/README.md) serializes
requests around both middleware and preserves its todo/session stores with
`defonce`. Both Ring handler arities are supported; for asynchronous handlers the
lock covers invocation, not completion of asynchronous work.

Keep this middleware development-only. Production should use `write-css!` in a
separate build step and serve the packaged stylesheet without reloading sources.

## AOT and shared rendering

CSS registration happens during macro expansion. Loading AOT classes cannot
reconstruct the stylesheet. Generate and package CSS as a build artifact, either
before AOT compilation or alongside it (create `target/classes` first):

```clojure
(binding [*compile-path* "target/classes"]
  (style.build/write-css!
    #(doseq [ns-sym '[example.components example.views]]
       (compile ns-sym))
    "resources/public/css/backend.css"))
```

Runtime composition descriptors are emitted into the compiled code, so `s/c`
continues to work in a fresh JVM without the compilation registry. JVM composed
values are immutable and descriptor registration is thread-safe.

JVM builds use deterministic `us-<hash>` release class names by default, matching
ClojureScript release builds for the same namespaces and constant inputs. To get
the readable names of ClojureScript development builds instead, for example when
a development backend renders `.cljc` components styled by a development
frontend's stylesheet, start the JVM with `-Duni.style.short-class-names=false`.
`-Duni.style.qualified-component-classes=true` matches the ClojureScript option of
the same name. The build JVM, server JVM, and AOT compilation must agree on both:
mismatched names fail silently. See
[Development vs release builds](builds.md).
JVM utility arguments accept literals, `^:const` vars, and the supported constant
expressions; ordinary or dynamic vars and lexical locals are rejected.

