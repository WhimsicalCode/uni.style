[UniStyle](../README.md) › [Usage](../README.md#usage) › Basics

# Basics

In UniStyle you style elements with small utilities, much like in Tailwind.
Each utility is a macro that returns the class names that set its CSS. Most
return a single class:

```clojure
(ns example.views
  (:require [uni.style.core :as s]))

(s/p 8)            ; => "p-8", which sets padding: 8px
(s/bg "#ffffff")   ; sets background-color: #ffffff
```

The CSS for these classes is generated when your code compiles and is written
to the CSS bundle, so nothing is injected at runtime. Development builds use
readable class names like `p-8`; release builds shorten them to hashes (see
[Development vs release builds](builds.md)).

Combine utilities with `s/c` and pass the result as the element's class:

```clojure
[:div {:class (s/c (s/flex)
                   (s/items-center)
                   (s/gap 8)
                   (s/bg "#ffffff")
                   (s/hover (s/bg "#f3f4f6")))}]
```

This gives you a flex row with an 8px gap and a white background that turns
light gray on hover.

`s/hover` is a modifier: it wraps one or more utilities and applies them only
in that state, like `hover:` in Tailwind. Modifiers can be nested, and there are
ones for other states, pseudo-elements, media queries, and more:

```clojure
(s/hover (s/bg "#f3f4f6")
         (s/color "#111827"))           ; both apply on hover
(s/focus-visible (s/outline 2 "#3b82f6"))
(s/media "(min-width: 768px)"
  (s/hover (s/p 16)))                  ; on hover, on wide screens only
```

A style inside a modifier doesn't replace the same property outside it, so in
the example above the element keeps its white background and only changes it on
hover. See [Modifiers](modifiers.md) for the full list.

Two rules are worth knowing from the start:

- **The last style wins.** If two utilities in `s/c` set the same property,
  the later one is used, no matter where they were defined. That makes it safe
  to override a component's styles by passing in your own.
- **Arguments must be known at compile time.** Use literals, `^:const` vars, and
  simple expressions built from them with `str`, `+`, `-`, `*`, `/`, `inc`,
  `dec`, or `name`:

  ```clojure
  (def ^:const spacing 8)

  (s/p spacing)              ; padding: 8px
  (s/mt (* spacing 2))       ; margin-top: 16px
  (s/w (str spacing "rem"))  ; width: 8rem
  ```

  To compute a `^:const` value that a later utility uses, write `(s/* spacing 2)`
  or `(s/str spacing "rem")` instead: the plain versions only work directly
  inside a utility call (see
  [Compile-time values and class naming](#compile-time-values-and-class-naming)).

  For values that are only known at runtime, such as a color from user
  settings, see [Runtime values](#runtime-values).

The rest of this page covers these in more detail.

## Composing classes with `c`

`(s/c & classes-or-class-sequences)` recursively composes its arguments into a
`UniStyle` value, skipping `nil`, `false`, and empty strings. Metadata-aware
utility classes resolve left to right per condition/property slot; arbitrary
strings and keywords are retained as opaque classes. Stock Reagent accepts the
value directly as `:class`, including on `:>` React components; `UniStyle`'s
`IEncodeJS` and `toString()` implementations provide automatic serialization.
Use `s/class-name` for JVM renderers and APIs that require an actual string,
such as selectors and third-party string-only APIs:

```clojure
(s/c (s/flex)
     (when selected? (s/bg "#dbeafe"))
     (s/hover (s/bg "#f3f4f6"))
     class)

(s/c (s/pr 4) (s/p 8)) ; p-8 replaces the earlier right padding
(s/c (s/p 8) (s/pr 4)) ; p-8 remains for other sides; pr-4 wins on the right

(s/class-name (s/c (s/flex) "legacy-hook"))
```

Opaque classes are render-compatible but cannot participate in property
conflict resolution. Keep composing metadata-aware values with `s/c`; do not
coerce them with `str` before the final string-only boundary. Equivalent nested
self conditions, such as `hover` + `focus` in either order, share a resolution
slot; selector order remains significant for descendants and pseudo-elements.
Cascade layers remain priority boundaries: styles from different layers are
retained and the native layer order decides which one wins.

## Using with Reagent, UIx, and other renderers

`uni.style` does not depend on React or Reagent. ClojureScript `UniStyle` values
implement `toString()` and `IEncodeJS`, both returning the composed class string.
Stock Reagent handles them automatically through its prop conversion, including
`:>` React components: use `:class (s/c ...)` directly, without `s/class-name` or
a custom renderer adapter. Use `s/class-name` for APIs that require an actual
string rather than performing conversion themselves.

## Compile-time values and class naming

All utility arguments pass through uni.style's host-specific compile-time
constant resolver. Literals, `^:const` vars, `def-token` constants, and supported
constant expressions such as `str`, `+`, `-`, `*`, `/`, `inc`, `dec`, and
`name` work. Runtime expressions fail during compilation.

Use `s/str`, `s/inc`, `s/dec`, `s/+`, `s/-`, `s/*`, and `s//` when defining
derived values that a later utility call must resolve at compile time. (`s//` is
the qualified call to the `/` macro.) The macros resolve their inputs and emit the
result as a literal initializer:

```clojure
(def ^:const dropdown-z-index 2)
(def ^:const title-z-index (s/inc dropdown-z-index))
(def ^:const background-z-index (s/dec dropdown-z-index))
(def ^:const overlay-z-index (s/+ dropdown-z-index 4))
(def ^:const doubled-z-index (s/* dropdown-z-index 2))
(def ^:const sidebar-width (s/str 16 "rem"))

(s/z title-z-index)
(s/w sidebar-width)
```

Ordinary `str`, `inc`, `dec`, `+`, `-`, `*`, `/`, and `name` expressions work
directly inside utility arguments, but cannot initialize derived `^:const` values
for later utility expansions. There is no `s/name`; use a literal string or
keyword in the `def` instead. `s/*` retains its direct-child modifier behavior when its
arguments resolve to style classes instead of numbers.

Development builds retain readable utility names. Release builds (or
`:short-class-names true`) compress every generated utility and composed class
name to a deterministic `us-<hash>` name, including classes with modifiers and
cascade layers.

Common value rules:

| Input | Meaning | Typical class naming |
| --- | --- | --- |
| Unnamespaced keyword, e.g. `:app-bg-base` | CSS custom property `var(--app-bg-base)` | `bg--app-bg-base` |
| CSS string, e.g. `"red"`, `"50%"`, or `"normal"` | Preserved as a literal CSS value | Content-hashed, e.g. `rounded-value-a1b2c3d4` |
| Number in a length utility | Pixels | Readable numeric suffix, e.g. `(s/p 8)` → `p-8` |
| Number in a unitless utility | Preserved as a number | Readable numeric suffix |
| Decimal | Preserved; `.` becomes `_` in the class | `(s/grow 1.5)` → `grow-1_5` |
| Negative number where supported | Preserved; the sign produces a double dash | `(s/mt -4)` → `mt--4` |

Prefer numeric utility arguments for pixel values and directional utilities over
CSS shorthand strings—for example, use `(s/py 1)` with `(s/px 6)` rather than
`(s/p "1px 6px")`. Reserve strings for values that need explicit CSS syntax or
units, such as `"50%"`, `"1em"`, or `"calc(...)"`. Metadata-aware utilities
reject `!important`; use application order or a narrowly scoped global/reset
`def-base` rule instead.

Value keywords must not be namespaced and always reference CSS custom properties:
`:app-bg-base` becomes `var(--app-bg-base)`. These docs call them CSS variable
keywords. Your application can define the variables itself, or use `def-token`
to define theme-aware ones (see [Themes and colors](themes-and-colors.md)).
Use strings for literal CSS keywords such as `"normal"`, `"none"`, or `"inherit"`.
CSS custom-property names are validated. Empty strings and non-finite numbers are
rejected. Macros whose arguments are identifiers rather than CSS values—such
as `ancestor`, `sel-class`, and other selector modifiers—document their own
keyword semantics.

## CSS strings

`str` concatenates compile-time CSS value components. Strings and finite numbers
remain literal, CSS variable keywords become `var(...)` references, and `alpha`
values become CSS `color-mix` expressions. It does not insert separators, so
include any required whitespace and punctuation in string components:

```clojure
(s/str "linear-gradient(" :app-bg-base ", transparent)")
;; "linear-gradient(var(--app-bg-base), transparent)"
```

Utilities that naturally accept compound values may expose the same shorthand.
For example, `transition`, `shadow`, and `bg-image` accept concatenated components
directly:

```clojure
(s/transition "opacity " :duration " " :ease)
(s/shadow "inset 0 0 0 2px " :app-border-focus)
(s/bg-image "linear-gradient(" :app-bg-base ", transparent)")
```

All components must resolve at compile time. Include units in string components;
numeric components remain unitless.

## Runtime values

When a pseudo-class, selector template, media query, or container query needs a
runtime value, keep the selector and declaration static and bridge only the
value through an inline CSS custom property. Use a CSS variable keyword in the utility
and `s/var-def` with the same keyword as the inline style key:

```clojure
(ns invite.views
  (:require [uni.style.core :as s]))

(defn remove-button* [{:keys [hover-color]}]
  [:button
   {:class (s/c
             (s/hover
               (s/bg :invite-remove-hover-bg)))
    :style {(s/var-def :invite-remove-hover-bg) hover-color}}])
```

`(s/var-def :invite-remove-hover-bg)` expands to the literal string
`"--invite-remove-hover-bg"` at compile time, so it costs nothing at runtime. It
also accepts a `def-token` constant, and rejects malformed names when the code
compiles. A hand-written `"--invite-remove-hover-bg"` string works too, but
`s/var-def` keeps the utility and the style key in sync.

The utility argument remains a compile-time constant while `hover-color` stays
runtime data. Custom-property names must be stable and component-prefixed (for
example, `invite-remove-...`, not `hover-bg`) because custom properties inherit
and generic names can collide with nested components. Do not generate the name
from runtime data.

## Cascade layers

The cascade-layer order is fixed from lowest to highest:

```css
@layer theme,base,components,variants;
```

The declaration is available as the shared constant `s/layer-priority-css`.

Normal declarations in unlayered author styles have higher priority than all
four layers. See [Components and variants](components.md#layer-modifiers) for
the layer modifiers.

<!-- usage-nav -->

---

Next: [Utility reference](utilities.md) →

**Usage**

1. **Basics**
2. [Utility reference](utilities.md)
3. [Modifiers](modifiers.md)
4. [Transforms, transitions, and animations](animations.md)
5. [Themes and colors](themes-and-colors.md)
6. [Components and variants](components.md)
7. [Base styles and preflight](base-styles.md)
8. [Raw CSS](raw-css.md)
9. [Differences from Tailwind](tailwind.md)
<!-- /usage-nav -->
