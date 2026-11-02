# Architecture

`uni.style.core` is the main supported caller-facing namespace. Its Clojure
file provides the macro facade and JVM runtime helpers, while
`uni.style.core.cljs` provides the browser runtime helpers, registers the global
layer order, and loads development hot-reload support. `uni.style.colors` provides interpolated OKLCH
color ramps and custom palette definitions. `uni.style.preflight` is
the supported Tailwind-compatible reset add-on.

`uni.style.build` is the supported JVM CSS export and the build hooks' facade (`esm-hook`, Shadow's `hook`). Build
configuration and infrastructure that registers generated global CSS must use it.
Development and release builds bundle all styles registered by namespaces
reachable in the build. JVM export bundles styles collected during its compilation
callback. Both deduplicate atomic classes; neither scans compiled JavaScript to
prune unused classes. Compile-time constant resolution, naming, serialization,
host-specific registries, bundling, composition, modifiers, layers, and
utility-family implementations live under `uni.style.impl.*`. Namespaces under
`uni.style.impl.*` and `uni.style.runtime` are private implementation details.
They may change without a caller migration and should only be required by
implementation tests and the supported uni.style facades.

All calls into the ClojureScript compiler live in `uni.style.impl.cljs-host`, which
loads only when a ClojureScript build expands the macros. JVM macro expansion
never loads it, so a backend needs no ClojureScript compiler on its classpath.
