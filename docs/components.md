[UniStyle](../README.md) › [Usage](../README.md#usage) › Components and variants

# Components and variants

`def-component` and `def-variants` define named classes that bundle several
styles. They live in the `components` and `variants`
[cascade layers](basics.md#cascade-layers), so variants override components.

## `def-component`

`def-component` creates one class in the `components` cascade layer by inlining
declarations from utility calls and previously composed classes. Later forms
replace earlier declarations for the same property:

```clojure
(s/def-component button-layout
  (s/inline-flex)
  (s/items-center)
  (s/gap 4)
  (s/px 8)
  (s/py 4))

(s/def-component compact-button-layout
  button-layout
  (s/px 4))
```

It has no options. Development names start with the bare definition symbol and
end with a hash of the qualified definition symbol; for example, `button-layout`
produces a name such as `"button-layout-3f9a1c2e"`, which stays the same across
recompiles. Set `:qualified-component-classes true` under
`:css-options` to include the namespace prefix as well.
Release names are deterministic and compressed.

Modifiers can be used inside `def-component`; they emit selectors on the
composed class:

```clojure
(s/def-component button-reset
  (s/cursor-pointer)
  (s/disabled
    (s/cursor-default)))
```

## `def-variants`

`def-variants` defines a map of related composed classes in the `variants`
cascade layer. Keys must resolve at compile time to unnamespaced keywords or
non-empty vectors of unnamespaced keywords. Literal keywords, `^:const` vars,
and vectors mixing either form are supported. A value can be one utility or
composed class, or a vector of forms:

```clojure
(def ^:const variant-primary :primary)
(def ^:const tint-purple :purple)

(s/def-variants button-classes
  {[variant-primary tint-purple] [primary-base
                                   (s/bg :app-bg-button-primary)]
   [variant-primary tint-purple :selected] [primary-base
                                             (s/bg :app-bg-button-primary-hover)]
   :flat (s/bg :app-bg-button-secondary-alt)})
```

Development names follow the same rules as `def-component`. By default they
combine the map symbol and key names with a hash suffix, such as
`button-classes-primary-purple-5d0e7b19`. With `:qualified-component-classes true`,
they also include the namespace prefix, such as
`my_ns--button-classes-primary-purple-5d0e7b19`. Release names are deterministic and
compressed. `def-variants` has no options.

## Layer modifiers

`def-token` automatically emits variables in `theme`. Resets and base styles stay
in `base`, so they can override theme defaults. Three utility layer modifiers are
available: `base`, `components`, and `variants`:

```clojure
(s/base
  (s/font-inherit)
  (s/color-inherit))

(s/components
  (s/bg :app-bg-base))

(s/variants
  (s/bg :app-bg-selected))
```

Prefer `def-base`, `def-component`, and `def-variants` when the
complete definition belongs to its corresponding layer.

<!-- usage-nav -->

---

Next: [Base styles and preflight](base-styles.md) →

**Usage**

1. [Basics](basics.md)
2. [Utility reference](utilities.md)
3. [Modifiers](modifiers.md)
4. [Transforms, transitions, and animations](animations.md)
5. [Themes and colors](themes-and-colors.md)
6. **Components and variants**
7. [Base styles and preflight](base-styles.md)
8. [Raw CSS](raw-css.md)
9. [Differences from Tailwind](tailwind.md)
<!-- /usage-nav -->
