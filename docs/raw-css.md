[UniStyle](../README.md) › [Usage](../README.md#usage) › Raw CSS

# Raw CSS

## `raw`

`raw` creates one atomic class per property in an arbitrary compile-time CSS
declaration map. Map entries can use constant references and supported compile-time
expressions. Use it for vendor-prefixed or uncommon declarations that do not have
a uni.style utility:

```clojure
[:div {:class (s/c (s/raw {:-webkit-text-decoration :inherit})
                   (s/p 8))}]
```

Raw styles work anywhere a utility does, including modifiers, `def-base`, `def-component`,
and `def-variants`:

```clojure
(s/def-component link-reset
  (s/raw {:-webkit-text-decoration :inherit})
  (s/color-inherit))
```

Prefer an existing utility when one is available so declaration validation and
readable development class names are preserved. Keep isolated properties such
as CSS masks, vendor app-region flags, and unsupported compound declarations in
`raw`; these do not justify dedicated public utility families.

## `css`

`css` is an expression macro that compiles a non-empty literal selector map into
a CSS string. It is for standalone stylesheets and inline backend HTML, not
generated classes:

```clojure
(def page-css
  (s/css
    {"body" [(s/bg "#F9FAFB")
             (s/m 0)]
     "h1" (s/font-size 34)
     ".callout" [(s/rounded 3)
                 (s/sel "p" (s/my 0))]}))
```

Keys must resolve to non-blank compile-time strings (including selector lists).
Values accept one utility/composed class or a vector of forms. Nested selectors,
pseudo-classes, media queries, and raw declarations work as usual.

**Resolved selectors are trimmed and sorted lexicographically.** Map insertion
order is ignored, and duplicate resolved selectors are rejected. Use specificity
or layers to express precedence between selectors; form order within a vector
controls repeated declarations for the same property.

Output is unlayered by default. Existing layer modifiers can wrap the forms for
a selector; each selector's forms must use one layer. Referenced components retain
their layer unless an enclosing layer modifier overrides it.

The macro embeds a literal string on JVM and CLJS. Use it directly in expressions
or bind it with `def`/`let`. The caller controls var metadata: add `^:const` to a
`def` if the resulting var should also be usable inside `s/str`; an inline
`(s/css ...)` expression already works there.

It does not register CSS with the build pipeline or include token definitions,
referenced animation keyframes, or a layer-priority header. Embed or write the
resulting string explicitly; combine it with `s/token-css` and `s/layer-priority-css`
when needed.
AOT loading needs no registry or source reload.

<!-- usage-nav -->

---

Next: [Differences from Tailwind](tailwind.md) →

**Usage**

1. [Basics](basics.md)
2. [Utility reference](utilities.md)
3. [Modifiers](modifiers.md)
4. [Transforms, transitions, and animations](animations.md)
5. [Themes and colors](themes-and-colors.md)
6. [Components and variants](components.md)
7. [Base styles and preflight](base-styles.md)
8. **Raw CSS**
9. [Differences from Tailwind](tailwind.md)
<!-- /usage-nav -->
