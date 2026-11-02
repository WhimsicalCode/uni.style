# UniStyle

UniStyle (`uni.style`) is a compile-time CSS library for ClojureScript and Clojure.

* **Style without leaving your markup.** Tailwind-style utilities like `(s/flex)` and `(s/p 8)` go right in your components, and macros turn them into static atomic CSS.

* **Plain Clojure, checked at compile time.** Utilities are ordinary calls and constants, so your editor and clj-kondo understand them. Invalid or runtime values fail the build, not the page.

* **Predictable, composable styles.** Styles are values: pass them as props and merge them with `s/c`. For each property, the last style wins regardless of source order.

* **Themes and tokens built in.** `s/def-token` and `s/def-theme` give you design tokens and nestable themes backed by CSS variables. Fixed cascade layers keep overrides in order.

* **Every state, inline.** Modifiers like `s/hover`, `s/focus-visible`, `s/media` and `s/container` wrap utilities in place and nest freely.

* **Fast and built to scale.** Styles ship as one static, deduplicated stylesheet, with nothing generated at runtime. It stays small as the codebase grows, and the same styles render on the server too.

## Example

```clojure
(ns example.core
  (:require [uni.style.colors :as sc]
            [uni.style.core :as s]))

(defn example-comp [{:keys [class]}]
  ;; s/c merges classes, ensuring that last style for the same CSS prop wins.
  ;; Colors are similar to Tailwind but alghorithmic and levels are inverse 
  ;; (0 = black, 100 = white).
  [:div (s/c (s/bg (sc/green 50)) 
             (s/color (sc/neutral 100))
             (s/px 10)  
             (s/py 5) 
             (s/hover (s/bg (sc/green 40)))
             class)
    "Hey!"])

(defn another-comp []
  ;; (s/p 20) will override (s/px 10) and (s/py 5)
  [example-comp {:class (s/p 20)}])
```

Resulting HTML:

```html

<div class="bg-green-50 color-neutral-100 px-10 py-5 hover:bg-green-40">
  Hey! <!-- example-comp -->
</div>

<div class="bg-green-50 color-neutral-100 hover:bg-green-40 p-20">
  Hey! <!-- another-comp -->
</div>
```

And CSS:

```css
.p-20 { padding: 20px; }
.py-5 { padding-block: 5px; }
.px-10 { padding-inline: 10px; }
.bg-green-50 { background-color: oklch(70.564141% 0.183785 150.416182); }
.color-neutral-100 { color: oklch(100% 0 0); }
.hover\:bg-green-40:hover { background-color: oklch(62.7% 0.194 149.214); }
```

Full project examples:

- [`examples/unistyle-clojurific`](examples/unistyle-clojurific/README.md) — the Reagent + Base UI showcase compiled by Clojurific (`cljf.esm`, `esm-hook`) and bundled by Vite.
- [`examples/unistyle-reagent`](examples/unistyle-reagent/README.md) — Reagent + Base UI components and reactive atoms, bundled by shadow-cljs.
- [`examples/unistyle-uix`](examples/unistyle-uix/README.md) — UIx + Base UI components, state hooks, and Fast Refresh, bundled by shadow-cljs.
- [`examples/unistyle-backend`](examples/unistyle-backend/README.md) — Ring + Reitit + Hiccup todo list with in-memory storage and JVM-generated CSS. No client-side JavaScript.

## Setup

### 1. Add uni.style to deps in `deps.edn`:

```clojure
com.whimsical/uni.style {:mvn/version "1.0.0"}
```

### 2. Add build hook for generating CSS bundle

If using [Clojurific](https://github.com/WhimsicalCode/clojurific), add the build hook with its CSS output to the compiler options under `cljf` Vite plugin config:

```javascript
buildHooks: [["'uni.style.build/esm-hook", { outputTo: "public/css/bundle.css" }]]
```

For a Shadow-CLJS browser build, add the build hook and CSS output configuration:

```clojure
:build-hooks [(uni.style.build/hook)]
:css-options {:output-to "public/css/bundle.css"}
```

For backend setup and usage, see [JVM / backend usage](docs/backend.md).

### 3. Load the generated stylesheet in the HTML shell

The `bundle-css` ID enables development hot reload in ClojureScript builds:

```html
<link id="bundle-css" rel="stylesheet" href="/css/bundle.css">
```

### Development vs release builds

UniStyle uses readable class names in development builds and short hashed names in release builds. See [Development vs release builds](docs/builds.md) for how the mode is detected, how to override it, and JVM settings.

## Usage

1. [Basics](docs/basics.md)
2. [Utility reference](docs/utilities.md)
3. [Modifiers](docs/modifiers.md)
4. [Transforms, transitions, and animations](docs/animations.md)
5. [Themes and colors](docs/themes-and-colors.md)
6. [Components and variants](docs/components.md)
7. [Base styles and preflight](docs/base-styles.md)
8. [Raw CSS](docs/raw-css.md)
9. [Differences from Tailwind](docs/tailwind.md)


## Development

From this directory, with Clojure CLI, Java, Node, clj-kondo, and cljfmt installed:

```bash
script/test          # JVM tests and development/release ClojureScript tests in Node
script/lint
script/test-clojars  # installs the Clojars artifact into an empty Maven repository and builds CSS with it
```

`clojure -M:test --focus uni.style.core-test` runs one JVM test namespace.
The library exports clj-kondo macro hooks under `resources/clj-kondo.exports`.

Also see [architecture](docs/architecture.md).

### Releasing

The version is in [`VERSION`](VERSION) and follows [semantic versioning](https://semver.org).
Record noteworthy changes in [`CHANGELOG.md`](CHANGELOG.md) under the `[Next]` release. To
release, replace `[Next]` with the release date (`[YYYY-MM-DD]`), then publish a GitHub release
from `main` with a tag matching `VERSION`:

```bash
gh release create v1.0.0 --repo WhimsicalCode/uni.style --target main
```

`.github/workflows/release.yml` checks the tag and the changelog, runs the tests and deploys
`com.whimsical/uni.style` to Clojars after a maintainer approves the `release` environment's
deployment. Then start a new `[Next]` release, bump `VERSION`, and point the `deps.edn` coordinate
in [Setup](#setup) at the released version.

## License

UniStyle is licensed under the [MIT License](LICENSE).

Copyright © 2026 Whimsical, Inc.

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for third-party attributions.
