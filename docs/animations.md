[UniStyle](../README.md) › [Usage](../README.md#usage) › Transforms, transitions, and animations

# Transforms, transitions, and animations

## Transforms

As in Tailwind v4, transform utilities write the individual `translate`,
`scale`, and `rotate` properties, so they combine on one element. The browser
applies them in that order, followed by any `transform`.

`translate`, `translate-x`, and `translate-y` set per-axis custom properties
that a shared class composes. Integers are pixels, strings are preserved as CSS
literals, and unnamespaced keywords reference CSS custom properties:

```clojure
(s/translate 8 -4)                 ; --us-translate-x: 8px; --us-translate-y: -4px;
(s/translate "50%" "var(--y)")
(s/translate :offset-x :offset-y)  ; var(--offset-x), var(--offset-y)
(s/translate-x "50%")              ; --us-translate-x: 50%;
(s/translate-y -4)                 ; --us-translate-y: -4px;
;; translate: var(--us-translate-x, 0) var(--us-translate-y, 0);
```

`scale` accepts a non-negative percentage and sets both axes; `scale-x` and
`scale-y` set one axis and accept negative percentages to flip:

```clojure
(s/scale 80)     ; --us-scale-x: 0.8; --us-scale-y: 0.8;
(s/scale-y -100) ; --us-scale-y: -1;
;; scale: var(--us-scale-x, 1) var(--us-scale-y, 1);
```

`rotate` accepts an angle. Numbers are degrees, strings are preserved as CSS
literals, and unnamespaced keywords reference CSS custom properties:

```clojure
(s/rotate 45)             ; rotate: 45deg;
(s/rotate "0.5turn")      ; rotate: 0.5turn;
(s/rotate :rotation)      ; rotate: var(--rotation);
```

`transform` sets an arbitrary transform, for function orders the individual
properties cannot express, such as a translation along a rotated axis:

```clojure
(s/transform "rotate(90deg) translateX(1px)")
```

`translate-none`, `scale-none`, `rotate-none`, and `transform-none` reset each
property. Inside `def-animation` frames the axis utilities emit direct
`translate` and `scale` values, so keyframes interpolate even in browsers
without `@property`. Transition `transform, translate, scale, rotate` (as
`transition-transform` does) to animate changes between classes.

## Transitions

`transition` sets the CSS transition shorthand directly. Pass a CSS string, an
unnamespaced keyword referencing a CSS custom property, or multiple concatenated
components:

```clojure
(s/transition "background-color 150ms ease-in-out")
(s/transition :transition) ; transition: var(--transition)
(s/transition "opacity " :duration " " :ease)
;; transition: opacity var(--duration) var(--ease)
```

Transition property presets include Tailwind's default 150ms duration and
cubic-bezier timing:

```clojure
(s/transition-colors)
(s/transition-opacity)
(s/transition-shadow)
(s/transition-transform)
(s/transition-all)
(s/transition-none)
```

Behavior, duration, timing, and delay are separate utilities:

```clojure
(s/transition-discrete)
(s/transition-normal)
(s/duration 150)              ; 150ms
(s/duration "0.3s")
(s/duration :duration)
(s/duration-initial)
(s/ease-in-out)
(s/ease "cubic-bezier(0.2, 0, 0, 1)")
(s/ease :timing-function)
(s/delay 75)                  ; 75ms
(s/delay :delay)
```

Integers are literal millisecond values for `duration` and `delay`; strings are
preserved CSS literals; unnamespaced keywords emit `var(--name)` references.

## Animations

`def-animation` defines a class and its keyframes. It accepts a class symbol and
a properties map containing `:frames`. A frame step is a number from `0` through
`100`, or a vector of numbers that share the same rules:

```clojure
(s/def-animation fade-and-scale
  {:duration "3s"
   :easing "ease-in"
   :delay "1s"
   :iterations 2
   :direction "reverse"
   :fill-mode "both"
   :play-state "paused"
   :frames {0 [(s/opacity 0) (s/scale 80)]
            [10 20] [(s/opacity 20) (s/scale 90)]
            100 [(s/opacity 100) (s/scale 100)]}})
```

Omitted animation properties use their CSS defaults. Definitions are emitted in
the `components` [cascade layer](basics.md#cascade-layers) by default; pass `:layer` with `:base`,
`:components`, or `:variants` to choose another predefined layer. Frame rules
may use utilities and composed classes, but not style modifiers.

Use the defined class directly, or disable animations with `animation-none`:

```clojure
[:div {:class fade-and-scale}]
[:div {:class (s/animation-none)}]
```

In development, the class name is the definition symbol plus a hash of its
qualified name, such as `fade-and-scale-3f9a1c2e`; release names are hashed (see
[Development vs release builds](builds.md)). The generated class sets the animation
shorthand and references its corresponding `@keyframes` definition.

<!-- usage-nav -->

---

Next: [Themes and colors](themes-and-colors.md) →

**Usage**

1. [Basics](basics.md)
2. [Utility reference](utilities.md)
3. [Modifiers](modifiers.md)
4. **Transforms, transitions, and animations**
5. [Themes and colors](themes-and-colors.md)
6. [Components and variants](components.md)
7. [Base styles and preflight](base-styles.md)
8. [Raw CSS](raw-css.md)
9. [Differences from Tailwind](tailwind.md)
<!-- /usage-nav -->
