[UniStyle](../README.md) › [Usage](../README.md#usage) › Modifiers

# Modifiers

The `*` modifier applies its utilities to every direct child, matching
Tailwind's `*` variant. Like `*`, the `**` variant can be used to style children
of an element. The main difference is that `**` applies styles to all
descendants, not just the direct children:

```clojure
(s/* (s/grow))   ; selector: .rule > *
(s/** (s/min-w 0)) ; selector: .rule *
```

`sel` applies utilities to one or more descendant selectors. Pass one selector
string or a vector of selector strings; use a vector rather than comma-separated
selectors:

```clojure
(s/sel "h2" (s/font-bold))
(s/sel ["h1" "h2" "h3"] (s/font-inherit))
(s/sel "> *:first-child" (s/mt 0))
```

`has` applies utilities when the styled element contains a descendant matching a
compile-time selector:

```clojure
(s/has ".remove-invite:hover" (s/shadow :app-focus-ring))
```

`pseudo` targets a named pseudo-element. Pass its compile-time keyword or string
name without the leading `::`; vendor-prefixed names can begin with `-`. The
`before` and `after` modifiers target `::before` and `::after` directly. They
compose with other modifiers:

```clojure
(s/pseudo :selection (s/bg :app-bg-selected))
(s/pseudo "-webkit-slider-thumb" (s/appearance-none))
(s/hover (s/pseudo "-moz-range-thumb" (s/opacity 100)))
(s/before
  (s/content "")
  (s/absolute))
(s/after
  (s/content "New")
  (s/block))
```

Prefer `has`, `pseudo`, `hover`, `focus`, `attr`, `not`, ancestor/group/peer
modifiers, and `sel` over arbitrary selectors. Ordinary scrollbar color and
width must use `scrollbar-thumb`, `scrollbar-thin`, and the other dedicated
scrollbar utilities.

Child pseudo-class modifiers are `first`, `last`, `only`, `odd`, `even`,
`empty`, `first-of-type`, `last-of-type`, and `only-of-type`. Indexed variants
are `nth`, `nth-last`, `nth-of-type`, and `nth-last-of-type`; they accept an
integer or a CSS nth expression string:

```clojure
(s/*
  (s/first (s/mt 0))
  (s/last (s/mb 0)))
(s/nth "2n + 1" (s/bg :app-bg-hover))
```

The interaction pseudo-class modifiers are `hover`, `focus`, `focus-within`,
`focus-visible`, `target`, `active`, `visited`, `disabled`, `not-disabled`, and
`read-only`.

Form-state modifiers apply when the element itself is in that state: `enabled`,
`checked`, `indeterminate`, `default`, `optional`, `required`, `valid`,
`invalid`, `user-valid`, `user-invalid`, `in-range`, `out-of-range`,
`placeholder-shown`, and `autofill`. `open` covers open details, dialogs, and
popovers. Each has a `peer-` version that applies when a preceding sibling is in
the state; see [peer](#marker-classes-ancestor-and-peer-modifiers).

Pseudo-element modifiers:

- `placeholder`: `::placeholder`
- `selection`: `::selection`, on the element and its descendants
- `marker`: `::marker` of list items, on the element and its descendants, so
  `(s/marker ...)` works on a `ul`
- `first-letter`, `first-line`: `::first-letter`, `::first-line`
- `file`: `::file-selector-button`, the button of a file input
- `backdrop`: `::backdrop`, behind a dialog or popover

Use `pseudo` for any other pseudo-element.

ARIA modifiers apply when the attribute is `"true"`: `aria-busy`,
`aria-checked`, `aria-disabled`, `aria-expanded`, `aria-hidden`, `aria-pressed`,
`aria-readonly`, `aria-required`, and `aria-selected`. Wrap one in `not` for the
opposite. For other attributes and values, use `attr` for presence and `attr=`
for an exact value. Names and values must resolve at compile time:

```clojure
(s/aria-pressed (s/bg :app-bg-selected))
(s/not (s/aria-disabled (s/hover (s/bg :app-bg-hover))))
(s/attr :disabled (s/opacity 50))
(s/attr= :data-state "open" (s/block))
```

They accept one or more nested utility calls and return a class vector:

```clojure
(s/hover
  (s/bg :app-bg-hover)
  (s/color :app-fg-hover))
(s/focus-within (s/border :app-border-focus))
(s/placeholder (s/color :app-fg-subtle))
```

A top-level vector defined from uni.style macros can be reused directly or
wrapped in a modifier. Modifier expansion reuses the vector's source forms so
it emits classes for the modifier rather than the original unmodified classes:

```clojure
(def focus-style
  [(s/outline-solid)
   (s/outline-color :focus-ring :app-focus-ring)
   (s/outline-width :focus-ring-width "3px")])

(s/def-component focus focus-style)
(s/def-component focus-visible
  (s/focus-visible focus-style))
```

The vector must be the direct initializer of a `def`; runtime-generated vectors
cannot be expanded by modifiers.

Modifiers can nest. `not-disabled` is a shortcut that can wrap multiple
interaction states:

```clojure
(s/not-disabled
  (s/hover (s/bg :app-bg-hover))
  (s/active (s/bg :app-bg-hover)))
;; not-disabled:hover:bg--app-bg-hover
;; not-disabled:active:bg--app-bg-hover
```

`not` negates the immediately nested pseudo-class or attribute modifier. It must
wrap a modifier, not a utility directly. This makes selectors for non-native disabled
renderers composable without a dedicated modifier:

```clojure
(s/focus-visible
  (s/not
    (s/attr :disabled
      (s/outline 4 :app-border-focus))))
;; selector: :focus-visible:not([disabled])
```

Breakpoint modifiers apply from a minimum viewport width up, with Tailwind's
default widths: `sm` (40rem), `md` (48rem), `lg` (64rem), `xl` (80rem), and
`xxl` (96rem, Tailwind's `2xl`). Larger breakpoints override smaller ones:

```clojure
(s/c (s/p 16)
     (s/md (s/p 24))
     (s/lg (s/p 32)))
```

`max-sm`, `max-md`, `max-lg`, `max-xl`, and `max-xxl` apply below a breakpoint
instead, and narrower ones override wider ones. Width queries in `media`, such
as `(min-width: 1000px)`, are ordered by width along with the breakpoints.

`motion-reduce` and `motion-safe` apply when the user does or doesn't prefer
reduced motion, and `print` applies when printing.

`media` is a compile-time modifier taking a parenthesized media query string,
for queries the modifiers above don't cover. It composes with pseudo-class
modifiers through the same serializer:

```clojure
(s/focus-visible
  (s/media "(resolution < 2dppx)"
    (s/outline-offset -1)))
```

`container` similarly takes a parenthesized compile-time container query:

```clojure
(s/container "(max-width: 300px)"
  (s/hidden))
```

`supports` applies utilities inside a compile-time CSS feature query. It can
wrap any utility, including `raw`:

```clojure
(s/supports "(-electron-corner-smoothing: 1%)"
  (s/raw {:-electron-corner-smoothing :system-ui}))
```

## Marker classes, ancestor, and peer modifiers

To style an element based on a parent or sibling, mark that element with a
plain class and refer to it by name. A marker class is an ordinary class with no
styles: add it with a keyword or string in `s/c`, and pass the same keyword or
string to the modifier. Class names are the same in every build mode:

| You write | Class name |
| --- | --- |
| `"menu"` | `menu` |
| `:menu` | `menu` |
| `::menu` in `app.views.snacks` | `app_views_snacks--menu` |

Strings and plain keywords are used exactly as written, so they can clash with
classes from other CSS on the page; prefer specific names such as
`:settings-menu` over `:menu`. A namespaced keyword includes its namespace, so
it's unique to the namespace that defines it. Other namespaces can refer to it
through an alias, such as `::snacks/menu`. Use `def-component` instead when a
class owns reusable declarations.

`sel-class` applies its nested utilities to descendants with the class, and is
the same as `(sel ".menu" ...)`. `ancestor` applies its nested utilities when the
element is inside an element with the class, while `self-or-ancestor` also
matches when the element itself has it:

```clojure
(s/sel-class :menu
  (s/color :app-fg-secondary))

(s/ancestor :menu
  (s/hover
    (s/bg :app-bg-hover)))

(s/c :menu
     (s/self-or-ancestor :menu
       (s/color :app-fg-selected)))
```

These produce selectors equivalent to `.rule .menu`, `.menu .rule`, and
`.menu.rule, .menu .rule`, respectively. Modifiers nested inside `ancestor`
still apply to the styled element. For example, the `hover` above produces
`.menu .rule:hover`.

Use an ancestor-state modifier when the state belongs to the marked ancestor:

```clojure
(s/ancestor-hover :menu
  (s/bg :app-bg-hover))
;; .menu:hover .rule

(s/ancestor-focus-within :menu
  (s/visible))
;; .menu:focus-within .rule

(s/ancestor-has :menu "input"
  (s/hidden))
;; .menu:has(input) .rule

(s/ancestor-attr :menu :disabled
  (s/opacity 50))
;; .menu[disabled] .rule
```

The available forms are `ancestor-hover`, `ancestor-focus`,
`ancestor-focus-within`, `ancestor-focus-visible`, `ancestor-active`,
`ancestor-disabled`, `ancestor-not-disabled`, `ancestor-has`, `ancestor-attr`,
and `ancestor-attr=`. Their nested rules can contain modifiers for the styled
element as usual.

`group` is the conventional `:group` shortcut. `(s/group)` returns the class
`"group"`, and `(s/group ...rules)` is the same as
`(s/ancestor :group ...rules)`. Ancestor-state shortcuts are `group-hover`,
`group-focus`, `group-focus-visible`, `group-active`, `group-disabled`,
`group-not-disabled`, `group-attr`, and `group-attr=`.

`peer` marks an element whose state can style following siblings. Like Tailwind,
the peer must precede the styled element because the generated selectors use the
general sibling combinator (`~`):

```clojure
[:input {:class (s/peer)}]
[:p {:class (s/peer-invalid (s/color :app-fg-danger))}]

[:div {:class (s/c (s/contents) (s/peer))}]
[:div {:class (s/peer-empty (s/hidden))}]
```

Interaction variants are `peer-hover`, `peer-focus`, `peer-focus-within`,
`peer-focus-visible`, `peer-active`, `peer-visited`, and `peer-target`.
Structural variants are `peer-first`, `peer-last`, `peer-only`, `peer-odd`,
`peer-even`, `peer-first-of-type`, `peer-last-of-type`, `peer-only-of-type`,
`peer-nth`, `peer-nth-last`, `peer-nth-of-type`, `peer-nth-last-of-type`, and
`peer-empty`. Form-state variants are `peer-disabled`, `peer-not-disabled`,
`peer-enabled`, `peer-checked`, `peer-indeterminate`, `peer-default`,
`peer-optional`, `peer-required`, `peer-valid`, `peer-invalid`,
`peer-user-valid`, `peer-user-invalid`, `peer-in-range`, `peer-out-of-range`,
`peer-placeholder-shown`, `peer-autofill`, and `peer-read-only`. `peer-open`
covers open details, dialogs, and popovers. `peer-has` accepts a compile-time
descendant selector. Use `peer-attr` and `peer-attr=` for other compile-time
attributes.

<!-- usage-nav -->

---

Next: [Transforms, transitions, and animations](animations.md) →

**Usage**

1. [Basics](basics.md)
2. [Utility reference](utilities.md)
3. **Modifiers**
4. [Transforms, transitions, and animations](animations.md)
5. [Themes and colors](themes-and-colors.md)
6. [Components and variants](components.md)
7. [Base styles and preflight](base-styles.md)
8. [Raw CSS](raw-css.md)
9. [Differences from Tailwind](tailwind.md)
<!-- /usage-nav -->
