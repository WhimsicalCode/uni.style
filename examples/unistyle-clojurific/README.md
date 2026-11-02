# uni.style + Clojurific + Reagent

The [Reagent example](../unistyle-reagent/README.md)'s component lab, compiled to
ES modules by [Clojurific](https://github.com/WhimsicalCode/clojurific) and
bundled by [Vite](https://vite.dev) instead of Shadow-CLJS. It uses
[Reagent](https://reagent-project.github.io/), [Base UI](https://base-ui.com/), and
the local [`uni.style`](../../README.md) library. No backend is required.

## Run

Requires Java 17+ and Node.js 20+. The Clojure CLI is not needed: the `cljf`
launcher in the `clojurific` npm package resolves `deps.edn` and fetches the
compiler (`npx cljf setup-java` downloads a JDK if there is none). From this
directory:

```bash
npm ci
npm run dev
```

Open **http://localhost:8094**. The Vite plugin runs the compiler's watch: saving
a namespace hot reloads it in place (`defonce` atoms keep their state) and
`uni.style.build/esm-hook` rewrites `public/css/bundle.css`, which the
`bundle-css` link in `index.html` reloads. `npm run repl` starts a ClojureScript
REPL evaluating in the open page; editors connect to the nREPL port in
`.nrepl-port` and evaluate `(cljf.esm.repl/repl)`.

To build and preview the optimized output:

```bash
npm run build
npm run preview
```

`npm run build` bundles into `dist/`, copying the generated stylesheet from
`public/`. Clojurific release builds define `goog.DEBUG false`, so uni.style emits
hashed `us-<hash>` class names; the dev server keeps readable ones.
Stop the dev server before building: both write `public/css/bundle.css`.
Generated JS/CSS are gitignored.

## Explore

The showcase is the Reagent example's: button variants, sizes and states;
controlled and uncontrolled switches; radio groups with option cards and disabled
states; and Auto/Light/Dark color modes persisted in localStorage. See its
[Explore](../unistyle-reagent/README.md#explore) and
[Component API](../unistyle-reagent/README.md#component-api) sections.

## Source

| File | Responsibility |
| --- | --- |
| `src/example/theme.cljs` | Semantic tokens built from `uni.style.colors`, light/dark themes, preflight, and mode persistence |
| `src/example/components.cljs` | Three reusable Reagent wrappers around Base UI primitives |
| `src/example/core.cljs` | Responsive showcase and Reagent atom-backed examples; renders when loaded |
| `index.html` | Loads `src/example/core.cljs` as a module script, Vite's entry point |
| `vite.config.ts` | The `clojurific/vite` plugin and its compiler options (type checked), including the `uni.style.build/esm-hook` build hook |

The plugin's `compilerOptions` are a JavaScript object converted to EDN: keys
become keywords, camelCase ones in kebab-case (`outputTo` is `:output-to`), and
strings starting with `'` become symbols. Only the dev server starts an nREPL
server.

```js
compilerOptions: {
  buildHooks: [["'uni.style.build/esm-hook", { outputTo: 'public/css/bundle.css' }]],
  ...(command === 'serve' && { repl: { nreplPort: 0 } }),
}
```

Compared with the Shadow-CLJS example:

- **No `:init-fn`.** `index.html` loads the namespace directly
  (`<script type="module" src="/src/example/core.cljs">`), and the plugin compiles
  it as a main namespace. `core.cljs` creates its root in a `defonce` and renders
  at load; `^:dev/after-load` re-renders after hot reloads.
- **Build hook.** `uni.style.build/esm-hook` with `:output-to` replaces Shadow's
  `(uni.style.build/hook)` and `:css-options`. It runs after every build, watch
  recompile, and REPL form that changes styles.
- **Release naming.** Both Shadow and Clojurific release builds use hashed names
  (Clojurific defines `goog.DEBUG false` for them).
- **npm requires.** `["@base-ui/react/button" :refer [Button]]` imports the ES
  module directly; React needs no `process` shim under Vite.

## Verify

```bash
npm run lint  # clj-kondo + cljfmt (install both separately)
npm run build
```

Check the showcase in a browser: try pointer and keyboard interactions, disabled
states, Auto/Light/Dark modes, a narrow viewport, and a hot reload of a style
change.
