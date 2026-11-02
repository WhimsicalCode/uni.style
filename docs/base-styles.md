[UniStyle](../README.md) › [Usage](../README.md#usage) › Base styles and preflight

# Base styles and preflight

## `def-base`

`def-base` registers utility declarations in the `base` cascade layer against
an arbitrary CSS selector. The selector must be a compile-time string. Use it
for global styles such as resets where no generated class can be attached to an
element:

```clojure
(s/def-base "html, body"
  (s/m 0)
  (s/p 0))
```

It has no options. Utilities, modifiers, and previously composed classes can be
used as rules.

## Preflight

`uni.style.preflight/import-preflight` registers the same browser reset as
Tailwind Preflight in the `base` layer using `def-base`. It accepts optional
sans and monospace font configuration:

```clojure
(ns example.styles
  (:require [uni.style.preflight :as s.preflight]))

(s.preflight/import-preflight
  {:font-family "Inter"
   :font-feature-settings "normal"
   :font-variation-settings "normal"
   :mono-font-family "Berkeley Mono"
   :mono-font-feature-settings "normal"
   :mono-font-variation-settings "normal"})
```

The family options default to `nil`, which leaves only Preflight's system
fallback stacks. All feature and variation settings default to `"normal"`.

Preflight uses `raw` for browser-compatibility declarations that do not have a
uni.style utility.

<!-- usage-nav -->

---

Next: [Raw CSS](raw-css.md) →

**Usage**

1. [Basics](basics.md)
2. [Utility reference](utilities.md)
3. [Modifiers](modifiers.md)
4. [Transforms, transitions, and animations](animations.md)
5. [Themes and colors](themes-and-colors.md)
6. [Components and variants](components.md)
7. **Base styles and preflight**
8. [Raw CSS](raw-css.md)
9. [Differences from Tailwind](tailwind.md)
<!-- /usage-nav -->
