[UniStyle](../README.md) › [Usage](../README.md#usage) › Utility reference

# Utility reference

Every utility in `uni.style.core`, grouped like the Tailwind docs. Each row
shows one utility, its variations, and example CSS for one call. Variations
take the same arguments as the utility, except presets such as `w-full` or
`m-auto`, which take none.

Arguments must be known at compile time; see
[Basics](basics.md#compile-time-values-and-class-naming). The notes use these
argument types:

| Type | Accepts |
| --- | --- |
| Length | A non-negative number in pixels, a CSS string such as `"50%"`, or a CSS variable keyword such as `:gap` |
| Signed length | A length that can also be negative |
| Color | A CSS string, a CSS variable keyword, or `(s/alpha color percent)`. A variable keyword can take a fallback color as a second argument: `(s/bg :card-bg "#fff")` |

## Spacing

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| p | px, py, pt, pr, pb, pl, ps, pe | padding: 4px | Length. `px`/`py` are the inline and block axes; `ps`/`pe` are inline start and end. |
| m | mx, my, mt, mr, mb, ml, ms, me, m-auto, mx-auto, my-auto, mt-auto, mr-auto, mb-auto, ml-auto | margin: -4px | Signed length. The `-auto` versions take no argument. |
| gap | gap-x, gap-y | gap: 8px | Length. |
| space-y | space-x | margin-bottom: 8px | Length. A bottom (or end) margin on every child except the last, without specificity. Prefer `gap` in flex and grid layouts. |
| scroll-p | scroll-px, scroll-py, scroll-pt, scroll-pr, scroll-pb, scroll-pl | scroll-padding: 8px | Length. |
| scroll-m | scroll-mx, scroll-my, scroll-mt, scroll-mr, scroll-mb, scroll-ml | scroll-margin: 8px | Signed length. |

## Sizing

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| w | w-auto, w-full, w-min, w-max, w-fit | width: 200px | Length. The presets set `auto`, `100%`, `min-content`, `max-content`, and `fit-content`. |
| h | h-auto, h-full, h-min, h-max, h-fit | height: 100vh | Length. |
| size | size-auto, size-full, size-min, size-max, size-fit | height: 16px;<br>width: 16px; | Length. Sets width and height. |
| min-w | min-w-auto, min-w-full, min-w-min, min-w-max, min-w-fit | min-width: 0 | Length. |
| min-h | min-h-auto, min-h-full, min-h-min, min-h-max, min-h-fit | min-height: 40px | Length. |
| max-w | max-w-auto, max-w-full, max-w-min, max-w-max, max-w-fit, max-w-none | max-width: 640px | Length. |
| max-h | max-h-auto, max-h-full, max-h-min, max-h-max, max-h-fit, max-h-none | max-height: 50vh | Length. |
| aspect-ratio |  | aspect-ratio: 16 / 9 | A CSS string or a CSS variable keyword. |

## Layout

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| flex | block, inline, inline-block, flow-root, inline-flex, grid, inline-grid, list-item, contents, hidden | display: flex | `display`. |
| relative | static, fixed, absolute, sticky | position: relative | `position`. |
| inset | inset-x, inset-y, top, right, bottom, left, inset-auto, inset-x-auto, inset-y-auto, top-auto, right-auto, bottom-auto, left-auto | inset: 0px | Signed length. The `-auto` versions take no argument. |
| z | z-auto | z-index: 10 | An integer or a CSS variable keyword. |
| overflow-hidden | overflow-auto, overflow-clip, overflow-visible, overflow-scroll, overflow-x-auto, overflow-x-hidden, overflow-x-clip, overflow-x-visible, overflow-x-scroll, overflow-y-auto, overflow-y-hidden, overflow-y-clip, overflow-y-visible, overflow-y-scroll | overflow: hidden |  |
| overscroll-contain | overscroll-auto, overscroll-none, overscroll-x-auto, overscroll-x-contain, overscroll-x-none, overscroll-y-auto, overscroll-y-contain, overscroll-y-none | overscroll-behavior: contain |  |
| visible | invisible, collapse | visibility: hidden | `visibility`. |
| box-border | box-content | box-sizing: border-box |  |
| float-left | float-right, float-start, float-end, float-none | float: left |  |
| isolate | isolation-auto | isolation: isolate |  |
| object-cover | object-contain, object-fill, object-none, object-scale-down | object-fit: cover |  |
| columns |  | columns: 3 | A number of columns, or a column width as a CSS string. |
| break-inside-avoid | break-inside-auto, break-inside-avoid-page, break-inside-avoid-column | break-inside: avoid |  |
| container-inline-size | container-normal, container-size, container-scroll-state, container-anchored | container-type: inline-size | `container-type`. The `s/container` modifier queries it. |
| scheme-dark | scheme-normal, scheme-light, scheme-light-dark, scheme-only-dark, scheme-only-light | color-scheme: dark | `color-scheme`. |
| align-middle | align-baseline, align-top, align-bottom, align-text-top, align-text-bottom, align-sub, align-super | vertical-align: middle | `vertical-align`. |
| border-collapse | border-separate, table-auto, table-fixed | border-collapse: collapse |  |
| border-spacing |  | border-spacing: 4px | Length. |
| clip-path |  | clip-path: inset(0 round 4px) | A CSS string or a CSS variable keyword. |
| content | content-none | content: '→' | A string, quoted for you, or a CSS variable keyword. `(s/content "")` gives `content: ''`. |
| sr-only | not-sr-only | border-width: 0;<br>margin: -1px;<br>overflow: hidden;<br>padding: 0;<br>clip-path: inset(50%);<br>height: 1px;<br>position: absolute;<br>white-space: nowrap;<br>width: 1px; | Hides an element visually but keeps it for screen readers. |

## Flexbox and grid

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| flex-row | flex-row-reverse, flex-col, flex-col-reverse | flex-direction: column |  |
| flex-wrap | flex-wrap-reverse, flex-nowrap | flex-wrap: wrap |  |
| flex-1 | flex-auto, flex-initial, flex-none | flex: 1 1 0% |  |
| grow | shrink | flex-grow: 1 | A non-negative number, 1 by default. |
| basis | basis-auto, basis-full | flex-basis: 120px | Length. |
| order | order-first, order-last, order-none | order: 2 | An integer. |
| items-center | items-start, items-end, items-end-safe, items-center-safe, items-baseline, items-baseline-last, items-stretch, items-inherit, items | align-items: center | `align-items`. `items` takes a value. |
| self-center | self-auto, self-start, self-end, self-end-safe, self-center-safe, self-stretch, self-baseline, self-baseline-last | align-self: center | `align-self`. |
| justify-between | justify-start, justify-end, justify-end-safe, justify-center, justify-center-safe, justify-around, justify-evenly, justify-stretch, justify-baseline, justify-normal | justify-content: space-between | `justify-content`. |
| justify-items-center | justify-items-start, justify-items-end, justify-items-end-safe, justify-items-center-safe, justify-items-stretch, justify-items-normal | justify-items: center |  |
| justify-self-center | justify-self-auto, justify-self-start, justify-self-center-safe, justify-self-end, justify-self-end-safe, justify-self-stretch | justify-self: center |  |
| content-center | content-normal, content-start, content-end, content-between, content-around, content-evenly, content-baseline, content-stretch | align-content: center | `align-content`. |
| grid-cols | grid-rows, grid-auto-cols, grid-auto-rows | grid-template-columns: repeat(3, minmax(0, 1fr)) | A CSS string, number, or CSS variable keyword. |
| grid-flow-col | grid-flow-row, grid-flow-dense, grid-flow-row-dense, grid-flow-col-dense | grid-auto-flow: column |  |
| col-span | row-span | grid-column: span 2 / span 2 | A positive integer. |
| grid-col | grid-col-start, grid-col-end, grid-row, grid-row-start, grid-row-end, grid-area | grid-column: 1 / -1 | A CSS string, number, or CSS variable keyword. |

## Typography

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| font-size | font-size-inherit | font-size: 14px | Length. |
| text |  | font-size: 14px;<br>line-height: 20px; | Font size and line height. A number line height is unitless, so use a string for pixels. |
| line-height | line-height-none, line-height-inherit | line-height: 1.5 | A number (unitless), a CSS string, or a CSS variable keyword. |
| font | font-mono, font-inherit | font-family: Inter, sans-serif | `font-family`: a CSS string or a CSS variable keyword. `font-inherit` sets the whole `font` shorthand. |
| font-bold | font-thin, font-extralight, font-light, font-normal, font-medium, font-semibold, font-extrabold, font-black, font-weight-inherit | font-weight: 700 |  |
| italic | not-italic | font-style: italic |  |
| tracking |  | letter-spacing: 0.02em | Signed length: like a length, but negative numbers are allowed. |
| text-center | text-left, text-right, text-justify, text-start, text-end | text-align: center |  |
| uppercase | lowercase, capitalize, normal-case | text-transform: uppercase |  |
| truncate | text-ellipsis, text-clip | overflow: hidden;<br>text-overflow: ellipsis;<br>white-space: nowrap; | `truncate` clips one line with an ellipsis. |
| whitespace-nowrap | whitespace-normal, whitespace-pre, whitespace-pre-line, whitespace-pre-wrap, whitespace-break-spaces | white-space: nowrap |  |
| text-balance | text-wrap, text-nowrap, text-pretty | text-wrap: balance | `text-wrap`. |
| word-break-all | word-break-normal, word-break-word, word-break-keep | word-break: break-all |  |
| overflow-wrap-anywhere | overflow-wrap-normal, overflow-wrap-break-word | overflow-wrap: anywhere |  |
| line-clamp | line-clamp-none | overflow: hidden;<br>-webkit-box-orient: vertical;<br>-webkit-line-clamp: 3;<br>display: -webkit-box; | A positive integer. |
| underline | overline, line-through, no-underline | text-decoration-line: underline |  |
| decoration | decoration-inherit, decoration-current, decoration-transparent | text-decoration-color: #f00 | Color: a CSS string, a CSS variable keyword, or `s/alpha`. A variable keyword can take a fallback color as a second argument. |
| decoration-wavy | decoration-solid, decoration-double, decoration-dotted, decoration-dashed | text-decoration-style: wavy |  |
| decoration-thickness |  | text-decoration-thickness: 2px | Length. |
| underline-offset |  | text-underline-offset: 4px | Length. |
| indent |  | text-indent: 16px | Signed length: like a length, but negative numbers are allowed. |
| text-shadow | text-shadow-none | text-shadow: 0 1px 2px #0003 | A CSS string, a CSS variable keyword, or several strings, keywords, and numbers concatenated without separators. |
| antialiased | subpixel-antialiased | -moz-osx-font-smoothing: grayscale;<br>-webkit-font-smoothing: antialiased; |  |
| list-disc | list-decimal, list-none, list-inside, list-outside, list-image-none | list-style-type: disc |  |

## Colors and backgrounds

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| color | color-transparent, color-current, color-inherit | color: #111 | Color: a CSS string, a CSS variable keyword, or `s/alpha`. A variable keyword can take a fallback color as a second argument. |
| bg | bg-transparent, bg-current, bg-inherit, bg-unset | background-color: var(--card-bg, #fff) | Color: a CSS string, a CSS variable keyword, or `s/alpha`. A variable keyword can take a fallback color as a second argument. |
| bg-overlay |  | background-image: linear-gradient(color-mix(in oklab, #000 5%, transparent), color-mix(in oklab, #000 5%, transparent)) | Color. Paints over the element's own background, so a translucent tint never shows what's behind the element. |
| bg-image | bg-none | background-image: url(/bg.png) | A CSS string, a CSS variable keyword, or several strings, keywords, and numbers concatenated without separators. |
| bg-size | bg-auto, bg-cover, bg-contain | background-size: cover | A CSS string or a CSS variable keyword. |
| bg-position | bg-top-left, bg-top, bg-top-right, bg-left, bg-center, bg-right, bg-bottom-left, bg-bottom, bg-bottom-right | background-position: center | A CSS string or a CSS variable keyword. |
| bg-no-repeat | bg-repeat, bg-repeat-x, bg-repeat-y, bg-repeat-space, bg-repeat-round | background-repeat: no-repeat |  |
| bg-clip-text | bg-clip-border, bg-clip-padding, bg-clip-content | background-clip: text |  |
| bg-blend-multiply | bg-blend-normal, bg-blend-screen, bg-blend-overlay, bg-blend-darken, bg-blend-lighten, bg-blend-color-dodge, bg-blend-color-burn, bg-blend-hard-light, bg-blend-soft-light, bg-blend-difference, bg-blend-exclusion, bg-blend-hue, bg-blend-saturation, bg-blend-color, bg-blend-luminosity | background-blend-mode: multiply |  |
| mix-blend-multiply | mix-blend-normal, mix-blend-screen, mix-blend-overlay, mix-blend-darken, mix-blend-lighten, mix-blend-color-dodge, mix-blend-color-burn, mix-blend-hard-light, mix-blend-soft-light, mix-blend-difference, mix-blend-exclusion, mix-blend-hue, mix-blend-saturation, mix-blend-color, mix-blend-luminosity, mix-blend-plus-darker, mix-blend-plus-lighter | mix-blend-mode: multiply |  |
| fill | stroke | fill: currentColor | Color: a CSS string, a CSS variable keyword, or `s/alpha`. A variable keyword can take a fallback color as a second argument. |
| caret | caret-inherit, caret-current, caret-transparent | caret-color: #00f | A CSS string or a CSS variable keyword. |
| opacity |  | opacity: 50% | An integer percentage from 0 to 100, a CSS string, or a CSS variable keyword. |
| mask-image | mask-size, mask-position | -webkit-mask-image: url(/mask.svg);<br>mask-image: url(/mask.svg); | Like `bg-image`, `bg-size`, and `bg-position`, with `-webkit-` prefixes. |
| mask-type-alpha | mask-type-luminance | mask-type: alpha |  |

## Borders and outlines

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| border | border-t, border-r, border-b, border-l, border-x, border-y | border-color: #ddd;<br>border-style: solid;<br>border-width: 1px; | `[width] color [fallback]`. Width defaults to 1px. Sets a solid style; see [border forms](#border-forms). |
| border-dashed | border-dotted, border-double, border-t-dashed, border-r-dashed, border-b-dashed, border-l-dashed, border-t-dotted, border-r-dotted, border-b-dotted, border-l-dotted, border-t-double, border-r-double, border-b-double, border-l-double | border-color: #ddd;<br>border-style: dashed;<br>border-width: 2px; | Like `border` with another style. Without arguments, sets only the style. |
| border-solid | border-none | border-style: none | Sets only the style. |
| border-width |  | border-width: 2px | Length. |
| border-color |  | border-color: #ddd | Color: a CSS string, a CSS variable keyword, or `s/alpha`. A variable keyword can take a fallback color as a second argument. |
| divide-y | divide-x | border-bottom-color: #ddd;<br>border-bottom-style: solid;<br>border-bottom-width: 1px; | Like `border`. A bottom (or right) border on every child except the last, without specificity. |
| rounded | rounded-t, rounded-r, rounded-b, rounded-l, rounded-tl, rounded-tr, rounded-br, rounded-bl | border-radius: 8px | Length. |
| outline |  | outline-color: #00f;<br>outline-style: solid;<br>outline-width: 2px; | `[width] color [fallback]`, like `border`. |
| outline-dashed | outline-solid, outline-dotted, outline-double, outline-none, outline-hidden | outline-style: dashed | Sets only the style. |
| outline-width |  | outline-width: 2px | Length. |
| outline-color |  | outline-color: #00f | Color: a CSS string, a CSS variable keyword, or `s/alpha`. A variable keyword can take a fallback color as a second argument. |
| outline-offset |  | outline-offset: 2px | Signed length. A CSS variable keyword can take a fallback as a second argument. |

## Effects

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| shadow | shadow-none | --us-shadow: 0 1px 2px #0002;<br>box-shadow: var(--us-inset-shadow, 0 0 #0000), var(--us-inset-ring-shadow, 0 0 #0000), var(--us-ring-shadow, 0 0 #0000), var(--us-shadow, 0 0 #0000); | A CSS string, a CSS variable keyword, or several strings, keywords, and numbers concatenated without separators. Combines with rings; see [shadows and rings](#shadows-and-rings). |
| inset-shadow | inset-shadow-none | --us-inset-shadow: inset 0 1px #0002;<br>box-shadow: var(--us-inset-shadow, 0 0 #0000), var(--us-inset-ring-shadow, 0 0 #0000), var(--us-ring-shadow, 0 0 #0000), var(--us-shadow, 0 0 #0000); | Like `shadow`; adds `inset` to each shadow. |
| ring | ring-none, inset-ring, inset-ring-none | --us-ring-shadow: 0 0 0 2px #00f;<br>box-shadow: var(--us-inset-shadow, 0 0 #0000), var(--us-inset-ring-shadow, 0 0 #0000), var(--us-ring-shadow, 0 0 #0000), var(--us-shadow, 0 0 #0000); | `[width] color [fallback]`, like `border`. |
| filter |  | filter: grayscale(1) | A CSS string or a CSS variable keyword. |
| blur |  | filter: blur(4px) | Length. Sets the whole `filter`. |
| backdrop-blur | backdrop-blur-none | -webkit-backdrop-filter: blur(4px);<br>backdrop-filter: blur(4px); | A CSS string or a CSS variable keyword. |

## Transforms

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| translate | translate-x, translate-y, translate-none | --us-translate-x: 8px;<br>--us-translate-y: -4px;<br>translate: var(--us-translate-x, 0) var(--us-translate-y, 0); | Signed length per axis. Combines with `scale` and `rotate`. |
| scale | scale-x, scale-y, scale-none | --us-scale-x: 0.8;<br>--us-scale-y: 0.8;<br>scale: var(--us-scale-x, 1) var(--us-scale-y, 1); | A percentage. `scale-x` and `scale-y` accept negatives to flip. |
| rotate | rotate-none | rotate: 45deg | Degrees, a CSS string, or a CSS variable keyword. |
| transform | transform-none | transform: rotate(90deg) translateX(1px) | A CSS string or a CSS variable keyword. |
| origin | origin-center, origin-top, origin-top-right, origin-right, origin-bottom-right, origin-bottom, origin-bottom-left, origin-left, origin-top-left | transform-origin: top left | A CSS string or a CSS variable keyword. |
| transform-border | transform-content, transform-fill, transform-stroke, transform-view | transform-box: border-box | `transform-box`. |
| backface-hidden | backface-visible | backface-visibility: hidden |  |

## Transitions

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| transition |  | transition: opacity 150ms ease | A CSS string, a CSS variable keyword, or several strings, keywords, and numbers concatenated without separators. |
| transition-colors | transition-all, transition-opacity, transition-shadow, transition-transform, transition-none | transition-duration: 150ms;<br>transition-property: color, background-color, border-color, outline-color, text-decoration-color, fill, stroke;<br>transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1); | Tailwind's properties, duration, and easing. |
| duration | duration-initial | transition-duration: 200ms | Milliseconds, a CSS string, or a CSS variable keyword. |
| ease | ease-linear, ease-in, ease-out, ease-in-out, ease-initial | transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1) | A CSS string or a CSS variable keyword. |
| delay |  | transition-delay: 75ms | Milliseconds, a CSS string, or a CSS variable keyword. |
| transition-discrete | transition-normal | transition-behavior: allow-discrete | `transition-behavior`. |

## Interactivity

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| cursor-pointer | cursor-auto, cursor-default, cursor-wait, cursor-text, cursor-move, cursor-help, cursor-not-allowed, cursor-none, cursor-context-menu, cursor-progress, cursor-cell, cursor-crosshair, cursor-vertical-text, cursor-alias, cursor-copy, cursor-no-drop, cursor-grab, cursor-grabbing, cursor-all-scroll, cursor-col-resize, cursor-row-resize, cursor-n-resize, cursor-e-resize, cursor-s-resize, cursor-w-resize, cursor-ne-resize, cursor-nw-resize, cursor-se-resize, cursor-sw-resize, cursor-ew-resize, cursor-ns-resize, cursor-nesw-resize, cursor-nwse-resize, cursor-zoom-in, cursor-zoom-out, cursor | cursor: pointer | `cursor` takes a value. |
| pointer-events-none | pointer-events-auto | pointer-events: none |  |
| select-none | select-text, select-all, select-auto | -webkit-user-select: none;<br>user-select: none; | Also sets `-webkit-user-select`. |
| touch-none | touch-auto, touch-pan-x, touch-pan-left, touch-pan-right, touch-pan-y, touch-pan-up, touch-pan-down, touch-pinch-zoom, touch-manipulation | touch-action: none |  |
| resize | resize-none, resize-x, resize-y | resize: none |  |
| appearance-none | appearance-auto | appearance: none |  |
| will-change | will-change-auto, will-change-scroll, will-change-contents, will-change-transform | will-change: transform | A CSS string or a CSS variable keyword. |
| snap-x | snap-none, snap-y, snap-both, snap-mandatory, snap-proximity | scroll-snap-type: x var(--us-scroll-snap-strictness, proximity) | `scroll-snap-type`. |
| snap-start | snap-end, snap-center, snap-align-none | scroll-snap-align: start | `scroll-snap-align`. |
| scrollbar-thin | scrollbar-auto, scrollbar-none | scrollbar-width: thin | `scrollbar-width`. |
| scrollbar-thumb | scrollbar-thumb-inherit, scrollbar-thumb-current, scrollbar-thumb-transparent, scrollbar-track, scrollbar-track-inherit, scrollbar-track-current, scrollbar-track-transparent | --us-scrollbar-thumb: #888;<br>scrollbar-color: var(--us-scrollbar-thumb) var(--us-scrollbar-track); | Color. Thumb and track share `scrollbar-color`. |
| scrollbar-gutter-stable | scrollbar-gutter-auto, scrollbar-gutter-both | scrollbar-gutter: stable |  |

## CSS variables

| Utility | Variations | Example CSS | Notes |
| --- | --- | --- | --- |
| var |  | --gap: 8 | Sets a CSS variable (keyword or `--name` string) to a CSS string, number, or another variable keyword. |

## Border forms

`border`, its side and axis versions, `divide-x`/`divide-y`, `outline`, and
`ring` take an optional width before the color, and an optional fallback
after a CSS variable keyword:

```clojure
(s/border "#ddd")                    ; width 1
(s/border 2 "#ddd")
(s/border 2 :app-border "#ddd")      ; var(--app-border, #ddd)
```

They set width, color, and a solid style, so a complete border is one call.
`border-dashed`, `border-dotted`, and `border-double` take the same arguments
with their style; without arguments they set only the style, so they can
change an existing border. `border-x` and `border-y` set each side separately,
so a later `border-l` still overrides the left side. Use `border-width` or
`border-color` to change one part.

## Shadows and rings

Shadows and rings are independent layers, as in Tailwind. Each utility sets its
own CSS variable, and all of them share one composed `box-shadow`, so they
combine on one element:

```clojure
(s/c (s/shadow "0 1px 2px #0002") (s/ring 2 "#3b82f6"))
```

```css
--us-shadow: 0 1px 2px #0002;
--us-ring-shadow: 0 0 0 2px #3b82f6;
box-shadow: var(--us-inset-shadow, 0 0 #0000), var(--us-inset-ring-shadow, 0 0 #0000),
            var(--us-ring-shadow, 0 0 #0000), var(--us-shadow, 0 0 #0000);
```

The layer variables are registered with `@property` as non-inheriting, so an
ancestor's shadow never leaks into its descendants. The build output includes
these registrations. Standalone CSS, such as [`s/css`](raw-css.md#css) exports,
should prepend `s/property-css`. Inside `def-animation` frames, shadows emit a
direct `box-shadow` so keyframes interpolate.

`translate` and `scale` work the same way, with per-axis variables composed
into the `translate` and `scale` properties. See
[Transforms](animations.md#transforms).

## Elsewhere

- [Modifiers](modifiers.md): `hover`, `focus`, breakpoints, `aria-*`, `group`, `peer`, `sel`, and the rest
- [Animations](animations.md#animations): `def-animation`, `animation-none`
- [Themes and colors](themes-and-colors.md): `def-theme`, `def-token`, `alpha`, and color palettes
- [Runtime values](basics.md#runtime-values): `var-def`, for setting a CSS variable in `:style`
- Composition: [`c`](basics.md#composing-classes-with-c),
  [`def-component`, `def-variants`](components.md), [`def-base`](base-styles.md),
  [`raw`, `css`](raw-css.md), and [compile-time value helpers](basics.md#compile-time-values-and-class-naming)
  `inc`, `dec`, `+`, `-`, `*`, `/`

<!-- usage-nav -->

---

Next: [Modifiers](modifiers.md) →

**Usage**

1. [Basics](basics.md)
2. **Utility reference**
3. [Modifiers](modifiers.md)
4. [Transforms, transitions, and animations](animations.md)
5. [Themes and colors](themes-and-colors.md)
6. [Components and variants](components.md)
7. [Base styles and preflight](base-styles.md)
8. [Raw CSS](raw-css.md)
9. [Differences from Tailwind](tailwind.md)
<!-- /usage-nav -->
