[UniStyle](../README.md) › [Usage](../README.md#usage) › Differences from Tailwind

# Differences from Tailwind

UniStyle borrows Tailwind's utility names and its general approach, so most
Tailwind knowledge carries over. This page lists where it differs.

## At a glance

| Tailwind | UniStyle |
| --- | --- |
| `class="p-4 hover:bg-gray-100"` | `(s/c (s/p 16) (s/hover (s/bg (sc/gray 90))))` |
| `p-4` is 4 spacing units (1rem by default) | `(s/p 4)` is 4px |
| `p-[13px]`, `w-[50%]` | `(s/p 13)`, `(s/w "50%")` |
| `rounded-lg`, `shadow-sm`, `text-sm` | `(s/rounded 8)`, `(s/shadow "...")`, `(s/font-size 14)` |
| `bg-green-600` | `(s/bg (sc/green 40))` |
| `bg-black/50` | `(s/bg (sc/neutral 0 50))` |
| `2xl:p-8` | `(s/xxl (s/p 32))` |
| `dark:bg-gray-900` | a token with a dark theme value, see [Themes and colors](themes-and-colors.md) |
| `group/item`, `group-hover/item:` | `:item` in `s/c`, `(s/ancestor-hover :item ...)` |
| `@apply` | `def-component`, see [Components and variants](components.md) |
| `p-4!` (`!p-4` in v3) | not supported |
| tailwind-merge | built in: `s/c` keeps the last style for each property |

## Utilities are function calls

Utilities are macros that return class names, not strings you type into
`class`. A misspelled utility is an undefined var, which the compiler and
clj-kondo report, rather than a class that silently does nothing. Your editor
can also complete and navigate to them.

Arguments are values, not a fixed scale:

- **Numbers are pixels.** `(s/p 4)` is `padding: 4px`, not 4 of Tailwind's
  spacing units. There's no `--spacing` scale; use constants if you want one.
- **Strings are literal CSS.** `(s/w "50%")`, `(s/p "1em")`. There's no
  arbitrary-value syntax because every value is arbitrary.
- **Keywords are CSS variables.** `(s/bg :card-bg)` is
  `background-color: var(--card-bg)`.
- **Few named presets.** There's no `rounded-lg`, `shadow-sm`, or `text-sm`;
  write the value, or define a constant or [token](themes-and-colors.md) for it.

See the [Utility reference](utilities.md) for every utility and what it accepts.

## Renamed and missing utilities

Most utilities keep Tailwind's name and take the value as an argument:
`(s/flex)`, `(s/items-center)`, `(s/gap 8)`, `(s/opacity 50)`, `(s/z 10)`,
`(s/line-clamp 3)`. Negative values are negative arguments: `-mt-4` is
`(s/mt -16)`. These differ:

### Renamed

| Tailwind | UniStyle |
| --- | --- |
| `text-red-500` (color) | `(s/color (sc/red 50))` |
| `text-sm` (size) | `(s/font-size 14)`, or `(s/text 14 "20px")` for size and line height |
| `leading-5` | `(s/line-height "20px")` |
| `break-all`, `break-keep` | `(s/word-break-all)`, `(s/word-break-keep)` |
| `wrap-break-word` (`break-words` in v3) | `(s/overflow-wrap-break-word)` |
| `decoration-2` | `(s/decoration-thickness 2)` |
| `blur-sm` | `(s/blur 8)` |
| `aspect-video` | `(s/aspect-ratio "16 / 9")` |
| `@container` | `(s/container-inline-size)` |
| `@md:flex` | `(s/container "(min-width: 28rem)" (s/flex))` |

### Different arguments

- **Borders and rings combine width and color**, and set a solid style:
  `border-2 border-red-500` is `(s/border 2 (sc/red 50))`, and
  `ring-2 ring-blue-500` is `(s/ring 2 (sc/blue 50))`. Use `s/border-width` or
  `s/border-color` to change just one.
- **Grid templates take a CSS string:** `grid-cols-3` is
  `(s/grid-cols "repeat(3, minmax(0, 1fr))")`.
- **`s/text` needs both arguments**, and a number line height is unitless:
  `(s/text 14 20)` sets `line-height: 20`, so write `(s/text 14 "20px")`.
- **Fractions and viewport units are strings:** `w-1/2` is `(s/w "50%")`,
  `h-screen` is `(s/h "100vh")`, `-translate-x-1/2` is `(s/translate-x "-50%")`.
- **Named sizes are numbers:** `rounded-full` is `(s/rounded 9999)`,
  `rounded-lg` is `(s/rounded 8)`, `shadow-sm` is a `(s/shadow "...")` string.
- **`s/transition` needs a value.** Bare `transition` is closest to
  `(s/transition-colors)` or `(s/transition-all)`.

### Not available

| Tailwind | Instead |
| --- | --- |
| `ring-offset-2` | an outer `(s/shadow "...")` or `(s/outline-offset 2)` with `s/outline` |
| `animate-spin` and other animations | `def-animation` |
| `container` (centered max-width wrapper) | `(s/w "100%")` with `(s/max-w ...)` and `(s/mx-auto)`; note that `s/container` is a container query modifier |
| arbitrary properties `[mask-type:luminance]` | `(s/raw {:mask-type "luminance"})` |

## Modifier differences

| Tailwind | UniStyle |
| --- | --- |
| `2xl:` | `(s/xxl ...)` |
| `group/item` and `group-hover/item:` | `:item` in the parent's `s/c`, `(s/ancestor-hover :item ...)` on the child |
| `group-data-[state=open]:` | `(s/group-attr= :data-state "open" ...)` |
| `peer/name` | not available; `s/peer` uses the single `peer` class |
| `data-[state=open]:` | `(s/attr= :data-state "open" ...)` |
| `aria-[sort=ascending]:` | `(s/attr= :aria-sort "ascending" ...)` |
| arbitrary variants like `[&_p]:` | `(s/sel "p" ...)`; selectors are relative to the element and can't contain `&` |

## Overrides work without a merge library

In Tailwind, which of two conflicting classes wins depends on their order in the
generated CSS, not in `class`, so libraries like tailwind-merge exist to drop
the losers. In UniStyle, `s/c` knows which properties each class sets and keeps
the last one:

```clojure
(s/c (s/px 10) (s/py 5) (s/p 20)) ; => "p-20"
```

So a component can accept a `class` prop and append it, and the caller's styles
win. See [Basics](basics.md#composing-classes-with-c).

## Modifiers wrap utilities

Tailwind prefixes each class with its variant (`hover:bg-gray-100
hover:text-black`). UniStyle wraps any number of utilities, and modifiers nest:

```clojure
(s/hover (s/bg "#f3f4f6")
         (s/color "#000"))
(s/md (s/hover (s/p 16)))
```

The breakpoints `sm`, `md`, `lg`, and `xl` keep Tailwind's names and default
widths; `2xl` is `xxl`, since Clojure names can't start with a digit. They can't
be configured: use `s/media` for other widths, or `s/container` for container
queries. See [Modifiers](modifiers.md).

## Dark mode uses themes

There's no `dark:` variant. Define a theme and give tokens a value for it; the
same `(s/bg background)` then switches with the theme, without a second class:

```clojure
(s/def-theme dark)
(s/def-token background
  dark (sc/neutral 10)
  (sc/neutral 98))
```

Themes can be applied to a whole page or to a part of it. See
[Themes and colors](themes-and-colors.md).

## Colors are algorithmic, and inverted

The palette families match Tailwind's (`green`, `neutral`, `sky`, ...), but:

- **Levels run from 0 (black) to 100 (white)**, the opposite direction to
  Tailwind's shades. Tailwind's 600 is level 40, and 50 is level 95.
- **Any level works**, not just fixed steps: `(sc/green 47)` is interpolated
  between Tailwind's anchor shades, so in-between shades are approximations.
- **Colors are literal values.** `(sc/green 40)` is the string
  `"oklch(62.7% 0.194 149.214)"`, usable anywhere a color is, including inline
  styles and canvas. The palette adds no CSS variables to your stylesheet.
- **Opacity is an argument instead of `/50`:** palette colors take it directly,
  as in `(sc/neutral 0 50)`, and `(s/alpha color 50)` adds it to any other color
  or CSS variable.

## CSS is generated from your code, not scanned

Tailwind scans source files for class names. UniStyle generates CSS when the
macros expand, so the bundle contains exactly the styles of the namespaces your
build includes. Two consequences:

- **No dynamic class names.** Tailwind can't find `` `bg-${color}-500` `` either;
  UniStyle rejects any non-constant argument at compile time. For runtime
  values, set a CSS variable in `:style` with `s/var-def` and read it in a
  utility; see [Runtime values](basics.md#runtime-values).
- **No source detection to configure.** Instead of Tailwind's `@source` (or
  `content` in v3), add the [build hook](../README.md#setup).

## Other differences

- **Cascade layers.** UniStyle uses `theme`, `base`, `components`, and
  `variants` layers; utilities are unlayered, so they override component styles.
  See [Cascade layers](basics.md#cascade-layers).
- **No `!important`.** Use composition order or a layer instead.
- **Preflight is opt-in.** Call `import-preflight` to include Tailwind's reset;
  see [Base styles and preflight](base-styles.md).
- **Class names are hashed in release builds**, e.g. `us-f00e74e5`. Don't rely on
  generated names in tests or selectors; see
  [Development vs release builds](builds.md).

<!-- usage-nav -->

---

**Usage**

1. [Basics](basics.md)
2. [Utility reference](utilities.md)
3. [Modifiers](modifiers.md)
4. [Transforms, transitions, and animations](animations.md)
5. [Themes and colors](themes-and-colors.md)
6. [Components and variants](components.md)
7. [Base styles and preflight](base-styles.md)
8. [Raw CSS](raw-css.md)
9. **Differences from Tailwind**
<!-- /usage-nav -->
