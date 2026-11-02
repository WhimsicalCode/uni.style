# uni.style + Reagent

A standalone component lab using [Reagent](https://reagent-project.github.io/),
[Base UI](https://base-ui.com/), and the local [`uni.style`](../../README.md)
library. The styling is inspired by shadcn/ui, but uses neither shadcn nor Tailwind.
No backend is required.

## Run

Requires Java, Clojure CLI, and Bun. From this directory:

```bash
bun install --frozen-lockfile
bun run dev
```

Open **http://localhost:8091**. Shadow-CLJS serves `public/` using the `:dev-http`
configuration and reloads both ClojureScript and generated CSS on save. The
`bundle-css` link in `public/index.html` enables CSS hot reload.

To build and preview the optimized output:

```bash
bun run build
bun run serve
```

`bun run serve` starts Shadow's built-in HTTP server without watching or compiling.
Stop the example's dev watcher before building a release so it doesn't overwrite
the optimized output. Both commands use the same `:dev-http` configuration on port 8091.
Deploy the contents of `public/` after building. Generated JS/CSS are gitignored.

## Explore

- **Button:** default, secondary, outline, ghost, destructive, and link variants;
  small, regular, large, and icon sizes; disabled and loading states. Active
  examples increment a counter. Link is a visual variant of a button, not navigation.
- **Switch:** controlled notification preferences, uncontrolled on/off examples,
  small size, and disabled on/off states.
- **Radio group:** controlled density selection, descriptive option cards,
  disabled options, and a fully disabled group. Try Tab and the arrow keys.
- **Color mode:** Auto follows live OS preference changes; Light/Dark override it.
  The selection persists in localStorage, with a fallback when storage is denied.
  Theme tokens also set native `color-scheme`. Reduced-motion preferences disable
  transitions and the loading spinner animation.

## Source

| File | Responsibility |
| --- | --- |
| `src/example/theme.cljs` | Semantic tokens built from `uni.style.colors`, light/dark themes, preflight, and mode persistence |
| `src/example/components.cljs` | Three reusable Reagent wrappers around Base UI primitives |
| `src/example/core.cljs` | Responsive showcase and Reagent atom-backed examples |
| `shadow-cljs.edn` | Standalone build with the public `uni.style.build/hook` |

Base UI owns keyboard handling, ARIA, focus management, hidden form inputs, and
controlled/uncontrolled behavior. `uni.style` owns the visual layer:

- `def-component` for shared styles and Base UI `data-checked` / `data-disabled` states;
- `def-variants` for button variants/sizes and switch sizes;
- `def-theme` / `def-token` for semantic colors from the neutral and red palettes;
- `s/c` for composition, passed directly to `:class` on DOM and `:>` Base UI elements.

`UniStyle` implements `toString()` and `IEncodeJS`. Stock Reagent's prop conversion
serializes it automatically; no explicit `s/class-name` or compiler adapter is needed.

All lengths are literal pixels (not Tailwind spacing units). No application-private
namespaces, runtime stylesheet injection, or handwritten CSS are needed. The
`process` npm dependency supplies the browser shim required by React under Shadow.

## Component API

Require `[example.components :as ui]`. Props use Reagent's kebab-case spelling for
Base UI props. Event callbacks receive the new value and Base UI event details.
All three wrappers accept a composable `:class` override and forward other props.

```clojure
[ui/button* {:variant :outline :size :sm :on-click save!}
 "Save"]

;; :loading? disables the button and adds an aria-hidden spinner.
[ui/button* {:loading? true} "Saving"]

[ui/switch* {:aria-label "Notifications"
             :checked @enabled-
             :on-checked-change (fn [checked _] (reset! enabled- checked))}]

[ui/radio-group* {:aria-label "Plan"
                  :name "plan"
                  :variant :cards
                  :value @plan-
                  :on-value-change (fn [value _] (reset! plan- value))
                  :options [{:value "free" :label "Free"}
                            {:value "pro" :label "Pro" :description "More room"}
                            {:value "team" :label "Team" :disabled true}]}]
```

For uncontrolled inputs, use `:default-checked` or `:default-value` instead of
`:checked` or `:value`. Give switches and radio groups an accessible name through
`:aria-label` or `:aria-labelledby`; icon-only buttons also need `:aria-label`.

Theme selection runs before the first Reagent render. This client-only example
does not include server-rendered or prepaint theme markup.

## Verify

```bash
bun run lint  # clj-kondo + cljfmt (install both separately)
bun run build
```

Check the showcase in a browser: try pointer and keyboard interactions, disabled
states, Auto/Light/Dark modes, and a narrow viewport.
