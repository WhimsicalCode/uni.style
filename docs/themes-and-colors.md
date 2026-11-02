[UniStyle](../README.md) › [Usage](../README.md#usage) › Themes and colors

# Themes and colors

## Themes and tokens

```clojure
(s/def-theme light)
(s/def-theme dark)

(s/def-token background
  dark "#111"
  "#fff") ; the final default is mandatory

(s/def-token foreground
  light "#222"
  dark "#eee"
  "#333")

(s/def-token radius "8px") ; default-only tokens are supported

[:div {:class (s/bg background)}]
[:section {:class light} ...] ; nested theme scope
```

`def-theme` defines a constant class-name string. `def-token` defines a constant,
unnamespaced keyword. Generated names use a readable `us-` prefix and a stable
hash of namespace + definition symbol, e.g. `:us-background-a1b2c3d4` references
`--us-background-a1b2c3d4`. Theme classes use `us-theme-`. Names do not depend on
values, load order, optimization mode, or host. Moving/renaming a definition
changes its identity; do not hardcode generated names in integrations.

Theme arguments must be `def-theme` vars. Values must resolve at compile time
to CSS strings, finite unitless numbers, or token keywords (aliases). Include
units in strings: `"8px"` means a length; `8` is unitless. Empty strings, nil,
duplicate themes, and missing defaults are rejected.

Token CSS is emitted in the lowest-priority `theme` layer against `:root` and
`:root.theme, .theme`. The `base` layer and all subsequent layers can override it.
Every reachable theme redeclares every reachable token: an omitted theme override
uses the token's default, **not an outer theme's value**. Token aliases are
redeclared too, so they follow the nested theme. Only one theme per element is
supported. Themes are generic: they do not automatically set `color-scheme`.

Token definitions use the normal stylesheet build pipeline in dev and release;
no browser CSS injection is needed. JVM CSS export must load all theme and token
source namespaces inside its collection callback, as with other styles. Cached
CLJS and AOT output still register concrete values for runtime lookup.

### Selecting a browser theme

```clojure
(s/set-theme! dark) ; replace managed theme classes on <html>
(s/set-theme! nil)  ; remove them, preserving unrelated classes

(s/set-theme-system! light dark) ; apply immediately and follow OS changes
(s/set-theme-system! nil dark)   ; no theme when the OS prefers light
```

Manual selection stops the system listener. Repeated system calls replace it;
both nil removes the theme and listener. Without `matchMedia`, the light argument
is used. Loading the library and calling setters without a DOM are safe.
`us-theme-` is reserved for managed theme classes, including server-applied ones.

Persistence and prepaint are application-owned. Use the shared theme strings in
server markup or an early script to prevent a wrong-theme flash before JS runs.

### Styling by the root theme

`root-theme` applies its nested utilities only when the root element (`<html>`)
has the given `def-theme` class, regardless of theme classes on elements in
between. Use it for styles that follow the page's theme inside a nested theme
scope:

```clojure
[:div {:class (s/c dark                       ; always-dark surface
                   (s/border "transparent")
                   (s/root-theme dark (s/border "#333")))}]
;; :root.us-theme-dark-… .rule { border-color: #333 }
```

The theme argument must be a `def-theme` var.

### Concrete token values

```clojure
(s/token-value background dark)  ; "#111"
(s/token-value background light) ; "#fff" (default)
(s/token-value background)       ; "#fff"
(s/bg (s/token-value background dark)) ; compile-time literal color
```

`token-value` works at runtime in CLJ/CLJS and inside compile-time utility/value
expressions. It recursively follows token aliases for the requested theme,
rejecting cycles, unknown tokens, and unknown themes. It returns declared values,
not browser-computed values: CSS expressions and units remain unchanged. Aliases
to application-owned, unregistered CSS properties can be emitted as CSS, but
cannot be resolved by `token-value`.

### Listing tokens

`(s/all-tokens)` returns a sorted vector of all loaded token keywords, with no
internal definition metadata. Use `s/token-value` to inspect their values. This
runtime list is not restricted to CSS build reachability. After removing definitions
during hot reload, reload the page before inspecting loaded tokens; stylesheet
collection excludes removed definitions.

### Exporting token CSS

`token-css` returns CSS for loaded tokens, optionally limited to an explicit
collection, on both JVM and CLJS. Pass a `def-theme` class or `nil` for root defaults:

```clojure
(str s/layer-priority-css
     (s/token-css nil [background foreground])
     (s/token-css dark [background foreground]))

;; All loaded tokens for a standalone dark document:
(s/token-css dark)
```

The one-argument form exports all loaded tokens; the two-argument form exports
only the explicit selection (including an empty selection). Use `s/all-tokens`
when you need the loaded token names for inspection or selection.

Each non-empty export is wrapped in `@layer theme`. Nil targets `:root`; a theme
targets `:root.<theme>,.<theme>`. Missing overrides use the mandatory default.
Tokens are sorted and deduplicated; an empty collection returns `""`. Unknown
requested tokens/themes are rejected. The layer-priority header is not included:
emit `s/layer-priority-css` once before any named-layer rules.

Unlike `token-value`, this preserves CSS variable references and expressions,
just like the normal build. Include referenced tokens when exporting a standalone
subset; dependencies are not added implicitly. The same pure renderer backs both
paths, but normal builds still read only their build-local definitions, never the
runtime index. The hot-reload caveat above also applies to runtime CSS export.

## Color palettes

```clojure
(ns example.views
  (:require [uni.style.colors :as sc]
            [uni.style.core :as s]))

[:div {:class (s/bg (sc/green 40))}]    ; exact anchor
[:div {:class (s/bg (sc/green 70))}]    ; interpolated
[:div {:class (s/bg (sc/green 20 50))}] ; 50% alpha
[:div {:class (s/bg (sc/green 20 :brand-alpha))
       :style {"--brand-alpha" "50%"}}]

(def ^:const accent (sc/green 40))
[:div {:class (s/bg accent)}]
```

Color calls are **macros**, on both JVM and CLJS. Every call returns an ordinary
literal OKLCH string, with no palette CSS variables, registration, or runtime
interpolation:

```clojure
(sc/green 40)
;; "oklch(62.7% 0.194 149.214)"

(sc/green 40 50)
;; "oklch(62.7% 0.194 149.214 / 50%)"

(sc/green 40 :alpha)
;; "oklch(62.7% 0.194 149.214 / var(--alpha))"

[:div {:style {:background-color (sc/green 40)}}]
```

Strings work directly in inline styles, canvas, utilities, `s/str`, `s/def-token`,
and standalone `s/css` exports. No palette stylesheet or conversion helper is
needed. Standalone calls such as `(def x (sc/green 40))` and
`(let [x (sc/green 40)] x)` produce strings too.

Level and alpha arguments must be literals, constant vars, or supported constant
expressions. Use `^:const` on a definition if other style macros need to resolve
it. Lexical locals still cannot be utility arguments:
`(let [x (sc/green 40)] (s/bg x))` is rejected. Bind the generated class instead:
`(let [bg (s/bg (sc/green 40))] ...)`.

### Converting for legacy consumers

Use dependency-free `sc/hex` and `sc/rgba` functions when an integration (such as
Stripe or email CSS) cannot accept OKLCH. Both work at runtime on JVM and CLJS:

```clojure
(sc/hex (sc/green 40))       ;; "#00A63E"
(sc/hex (sc/green 40 50))    ;; "#00A63E80"
(sc/rgba (sc/green 40 50))   ;; "rgba(0,166,62,0.5)"
```

These helpers only accept numeric palette OKLCH literals, including custom
palettes. Other CSS formats, expressions, and variable alpha throw. They round
sRGB channels to bytes and clip out-of-gamut channels; alpha is preserved (`hex`
adds an alpha byte below 100%). Use native OKLCH in browser styles to retain
wide-gamut colors rather than converting everything to sRGB.

### Readable utility names

Direct palette calls retain their identity while a utility argument is expanded.
Development builds emit names such as `bg-green-10` and `bg-green-40-a50`, with
literal declarations:

```css
.bg-green-40 { background-color: oklch(62.7% 0.194 149.214); }
```

Custom palette names include a hash of the defining namespace and symbol to avoid
collisions. Aliases and refers preserve identity; level and alpha expressions are
normalized before naming. Names stay stable when anchors change. Release and JVM
builds use the usual hashed utility names.

This provenance is local to compilation, not attached to the resulting string.
Already-expanded constant vars and transformed strings without an unambiguous
palette origin use ordinary content-based utility names. They still emit the same
literal CSS values; no global color-to-name lookup is needed.

### Custom color macros

`def-color` generates a macro. Define it in a **JVM macro namespace**:

```clojure
;; example/palette.clj
(ns example.palette
  (:require [uni.style.colors :as sc]))

(sc/def-color brand
  {5 [26.6 0.065 152.934]
   40 [62.7 0.194 149.214]
   95 [98.2 0.018 155.826]})
```

Clojure callers use a normal `:require`. ClojureScript callers use
`:require-macros`, with aliases or refers:

```clojure
;; example/views.cljs
(ns example.views
  (:require [uni.style.core :as s])
  (:require-macros [example.palette :as palette :refer [brand]]))

(def ^:const accent (palette/brand 40))
[:div {:class (s/bg accent)}]
[:div {:class (s/bg (brand 70))}]
```

A shared `.cljc` palette can define macros in its CLJ branch and expose them to
CLJS through the standard same-namespace macro import:

```clojure
;; example/palette.cljc (an alternative to palette.clj)
(ns example.palette
  #?(:clj (:require [uni.style.colors :as sc]))
  #?(:cljs (:require-macros [example.palette])))

#?(:clj (sc/def-color brand {50 [65 0.2 150]}))
```

Consumers can then use `(:require [example.palette :as palette])` on either host.
Calling `def-color` in a `.cljs` file or a CLJS reader branch is rejected with an
explanation; JVM macro definitions cannot be created in JavaScript.

### Anchors and interpolation

Anchors are a non-empty map of level to `[lightness chroma hue]`. Lightness is a
percentage (0–100), chroma is non-negative and unitless, and hue is in degrees.
Levels run from **0 = black to 100 = white**, in the same direction as OKLCH
lightness and opposite to Tailwind's shade numbers. Built-in anchors use
`100 - Tailwind shade / 10`: Tailwind 950, 600, and 50 become **5, 40, and 95**.
A level is a position in the ramp, not necessarily that exact OKLCH lightness.
Custom palettes specify their own anchor levels on this black-to-white scale.
Decimal levels and numeric alpha are supported and normalized to six decimal
places. Levels and numeric alpha outside 0–100 are rejected. Anchor levels must
be strictly inside that range; 0 and 100 are reserved for pure black and white,
borrowing hue from the nearest anchor.

Lightness and chroma use **shape-preserving piecewise cubic interpolation
(PCHIP)**. This eases through anchors without overshooting each segment's values,
and preserves monotonic lightness for monotonic anchors. Hue follows the shortest
arc around the color wheel; achromatic anchors borrow their neighbor's hue.
Values are emitted directly as `oklch(...)`, rounded to six decimal places. They
are not clipped to sRGB; the browser handles display-gamut mapping.

The predefined ramps use Tailwind CSS **v4.1.18**'s three anchors, not its full
static palette. Intermediate shades are approximations. All 22 families are
included: `red`, `orange`, `amber`, `yellow`, `lime`, `green`, `emerald`, `teal`,
`cyan`, `sky`, `blue`, `indigo`, `violet`, `purple`, `fuchsia`, `pink`, `rose`,
`slate`, `gray`, `zinc`, `neutral`, and `stone`. `sc/white` and `sc/black` are
constant OKLCH strings rather than ramps. Tailwind attribution is in
[`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md).

Unused palettes and standalone color calls emit no CSS. Only the consuming
utilities and token definitions register styles in their build-reachable caller
namespace. For JVM CSS export, load/compile those namespaces inside the `build/css`
callback, as with other style macros.

Keyword alpha references a CSS variable containing **0–1 or a percentage** (e.g.
`0.5` or `50%`, not `50`). The variable is used directly in the literal color:

```css
background-color: oklch(62.7% 0.194 149.214 / var(--brand-alpha));
```

Alpha resolves where the declaration is used, so element-local variables work
without relative-color syntax. As with other CSS variable values, putting such a
color into a root-level semantic token resolves its alpha on the root.

Palettes are not `def-token` definitions and are not listed by `s/all-tokens`.
Semantic tokens still use CSS variables for theme switching. Pass palette calls
or constant palette strings to `def-token`; `token-value` and standalone
`token-css` resolve their ordinary string values, including expressions composed
with `s/str` or `s/alpha`. Token aliases still resolve for the selected theme.

## Color fallbacks

`bg`, `color`, `fill`, `stroke`, `outline-color`, and the three-argument
`border*` and `outline` forms accept an optional fallback. The primary value
must be a CSS variable keyword; the fallback can be another CSS variable keyword or a literal
color string:

```clojure
(s/bg :app-bg-base :app-bg-fallback)
;; background-color: var(--app-bg-base, var(--app-bg-fallback));

(s/color :app-fg-base "#fff")
;; color: var(--app-fg-base, #fff);
```

## Opacity and color alpha

`opacity` sets element opacity. Numeric values must be integers from
0 through 100 and are emitted as percentages. CSS strings are literal and token
keywords resolve through CSS custom properties:

```clojure
(s/opacity 50)                 ; opacity: 50%;
(s/opacity "0.45")             ; opacity: 0.45;
(s/opacity :disabled-opacity)  ; opacity: var(--disabled-opacity);
```

`alpha` creates a color value without changing element opacity. Its first
argument follows the normal color rules (keyword → CSS variable, string →
literal), and its second argument is an integer percentage from 0 through 100:

```clojure
(s/bg (s/alpha :col 10))
;; class: bg--col/10
;; background-color: color-mix(in oklab, var(--col) 10%, transparent);

(s/color (s/alpha "oklch(50% 0.2 10)" 25))
```

The slash remains in the DOM class and is escaped when serialized into a CSS
selector. `alpha` works anywhere a color is accepted, including `bg`,
`color`, `border*`, `outline`, and `outline-color`. It also works in `def-token`
and `str`:

```clojure
(s/def-token overlay (s/alpha "#000" 40))
(def ^:const shadow (s/str "0 1px 2px " (s/alpha "#000" 20)))
```

Alpha values serialize as CSS `color-mix` expressions; `token-value` does not
convert them to RGBA or resolve CSS variables inside those expressions.

<!-- usage-nav -->

---

Next: [Components and variants](components.md) →

**Usage**

1. [Basics](basics.md)
2. [Utility reference](utilities.md)
3. [Modifiers](modifiers.md)
4. [Transforms, transitions, and animations](animations.md)
5. **Themes and colors**
6. [Components and variants](components.md)
7. [Base styles and preflight](base-styles.md)
8. [Raw CSS](raw-css.md)
9. [Differences from Tailwind](tailwind.md)
<!-- /usage-nav -->
