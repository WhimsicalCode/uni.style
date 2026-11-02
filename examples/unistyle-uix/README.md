# uni.style + UIx

A standalone [UIx](https://github.com/pitch-io/uix) version of the
[Reagent component lab](../unistyle-reagent/README.md), using
[Base UI](https://base-ui.com/) and the local [`uni.style`](../../README.md) library.
The styling is inspired by shadcn/ui, with no shadcn, Tailwind, or Reagent dependency.
No backend is required.

## Run

Requires Java, Clojure CLI, and Bun. From this directory:

```bash
bun install --frozen-lockfile
bun run dev
```

Open **http://localhost:8092**. Shadow's built-in `:dev-http` server serves `public/`;
port 8092 lets this example run alongside the Reagent example on 8091.

UIx Fast Refresh preserves component state during development. The development-only
`react-refresh` dependency and `uix.preload` enable it; don't add an after-load
callback that re-renders the root. The `bundle-css` link in `public/index.html`
enables `uni.style` CSS hot reload.

To build and preview optimized output, stop this example's dev watcher first:

```bash
bun run build
bun run serve
```

`serve` starts Shadow's HTTP server without watching or compiling. Deploy the
contents of `public/` after building. Generated JS/CSS are gitignored.

## Explore

- **Button:** default, secondary, outline, ghost, destructive, and link variants;
  small, regular, large, and icon sizes; disabled and loading states. Active
  examples increment a counter. Link is a visual variant, not navigation.
- **Switch:** controlled notification preferences, independent uncontrolled
  switches, small size, and disabled on/off states.
- **Radio group:** controlled density selection, descriptive cards, a disabled
  option, and a disabled group. Try Tab, Space, and the arrow keys.
- **Color mode:** Auto follows live OS preference changes; Light/Dark override it.
  Selection persists in `unistyle-uix.theme` in localStorage, independently of the
  Reagent example, and still works when storage is denied. Tokens set native
  `color-scheme`; reduced-motion preferences disable transitions and the spinner.

## Source

| File | Responsibility |
| --- | --- |
| `src/example/theme.cljs` | Semantic tokens from `uni.style.colors` (`sc`), themes, preflight, and persistence |
| `src/example/components.cljs` | Three `defui` wrappers around Base UI primitives |
| `src/example/core.cljs` | Showcase using `$`, local `use-state` hooks, and `def` + `s/c` styles |
| `shadow-cljs.edn` | CSS build hook, HTTP server, and Fast Refresh |

Base UI owns accessible interactions and controlled/uncontrolled input behavior.
`uni.style` owns component styles, variants, and themes. Style rules are one per
line; pass composed values directly as `:class`. `UniStyle` implements `toString()`
and `IEncodeJS`, so no explicit `s/class-name` or renderer adapter is needed here.
The `process` npm dependency supplies the browser shim required by React under Shadow.

## Component API

Require `[example.components :as ui]` and `[uix.core :as uix :refer [$ defui]]`.
Props use kebab-case; UIx translates them for Base UI. All wrappers accept `:class`
overrides and forward the remaining props with UIx's `:&` spread syntax.

```clojure
(defui settings []
  (let [[enabled? set-enabled!] (uix/use-state true)
        [plan set-plan!] (uix/use-state "free")]
    ($ :div
      ($ ui/button {:variant :outline :size :sm} "Save")
      ($ ui/switch {:aria-label "Notifications"
                    :checked enabled?
                    :on-checked-change (fn [checked _] (set-enabled! checked))})
      ($ ui/radio-group {:aria-label "Plan"
                         :variant :cards
                         :value plan
                         :on-value-change (fn [value _] (set-plan! value))
                         :options [{:value "free" :label "Free"}
                                   {:value "pro" :label "Pro"}]}))))
```

Use `:default-checked` / `:default-value` for uncontrolled inputs. Button `:loading?`
disables activation and adds an aria-hidden spinner. Switches, radio groups, and
icon-only buttons need an accessible name via `:aria-label` or `:aria-labelledby`.

The saved theme is applied before the first render. This client-only example
does not include server-rendered or prepaint theme markup.

## Verify

```bash
bun run lint  # clj-kondo + cljfmt (install both separately)
bun run build
```

Check the showcase in a browser: pointer/keyboard interactions, disabled states,
Auto/Light/Dark modes, state preservation after editing a component, and a narrow
viewport. No browser test framework is included.
