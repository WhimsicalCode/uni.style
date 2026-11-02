# Development vs release builds

UniStyle detects the build mode automatically. A ClojureScript build counts as a release build when any of these is true:

- `goog.DEBUG` is defined `false` (`:closure-defines {goog.DEBUG false}`). Clojurific release builds (`vite build`) set this by default.
- It is a Shadow-CLJS release build (`shadow-cljs release ...`).
- Closure compiles with `:optimizations :advanced`.

Everything else is a development build.

| | Development | Release |
| --- | --- | --- |
| Utility class names | Readable, e.g. `p-8`, `bg-green-40` | Hashed, e.g. `us-1a2b3c4d` |
| Component and variant class names | Symbol name plus a hash of the qualified symbol, e.g. `button-layout-3f9a1c2e` | Hashed |
| Stylesheet hot reload | `<link id="bundle-css">` reloads after each code reload | Off |

Otherwise the CSS is the same in both modes: same rule order and atomic classes are deduplicated. The bundle contains styles from every styled namespace the build reaches.

Release class names are deterministic hashes, so they stay the same across builds and machines. A utility's hash comes from its readable name; a component's or variant's comes from its fully qualified definition symbol. JVM macro expansion (see [JVM / backend usage](backend.md)) has no build mode to detect, so JVM builds use hashed names by default. As a result, a backend that shares `.cljc` styles with a ClojureScript release build renders the same class names. To match a ClojureScript development build instead, turn hashed names off on the JVM (see below).

To override automatic detection, set `:short-class-names` under `:css-options`. `true` uses hashed names in development too. `false` keeps readable names in a release build, which helps when debugging production output:

```javascript
// Clojurific: in compilerOptions, not in esm-hook's options
cssOptions: { shortClassNames: false }
```

```clojure
;; Shadow-CLJS build config
:css-options {:output-to "public/css/bundle.css"
              :short-class-names false}
```

In the same place, `:qualified-component-classes true` adds the namespace to development names of components and variants (e.g. `my_app--button-layout-3f9a1c2e` for `my.app/button-layout`). Hashed names are unaffected.

On the JVM, set the same options as system properties instead, e.g. in a `deps.edn` alias:

```clojure
:dev {:jvm-opts ["-Duni.style.short-class-names=false"
                 "-Duni.style.qualified-component-classes=true"]}
```

The properties take `true` or `false`. Every JVM that expands styled namespaces for the same stylesheet must use the same values: the one running `write-css!`, the one rendering HTML, and AOT compilation. Otherwise the rendered class names won't match the CSS, and nothing reports an error.

Compilers don't treat `:css-options` as a cache input. After changing these options, delete the build's output or cache directory so every styled namespace compiles again. The same applies to AOT-compiled classes on the JVM.

Every generated name is stable across recompiles, so an unchanged style keeps its class name and browsers can keep caching it. Still, don't hardcode generated class names in tests, CSS selectors, or integrations: names differ between development and release, utility hashes change when the style changes, and component hashes change when the definition is renamed or moved. For a stable hook, add a plain class with a keyword or string in `s/c`, such as `:my-marker`.
