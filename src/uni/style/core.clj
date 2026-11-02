(ns uni.style.core
  "Compile-time atomic utility classes with left-to-right application-order
   composition. Utility calls must use constants."
  (:refer-clojure :exclude [* + - / dec delay filter inc pr print empty first last nth str]
                  :rename {not not'})
  (:require [uni.style.impl.animation :as animation]
            [uni.style.impl.border :as border]
            [uni.style.impl.color :as color]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.composition :as composition]
            [uni.style.impl.css :as css]
            [uni.style.impl.interaction :as interaction]
            [uni.style.impl.layer :as layer]
            [uni.style.impl.layout :as layout]
            [uni.style.impl.modifier :as modifier]
            [uni.style.impl.position :as position]
            [uni.style.impl.runtime :as runtime]
            [uni.style.impl.scrollbar :as scrollbar]
            [uni.style.impl.spacing :as spacing]
            [uni.style.impl.theme :as theme]
            [uni.style.impl.tokens :as tokens]
            [uni.style.impl.transform :as transform]
            [uni.style.impl.transition :as transition]
            [uni.style.impl.typography :as typography]
            [uni.style.impl.var :as style-var]))

(defn c
  "Compose classes left to right, preserving condition/property conflict metadata."
  [& classes-or-class-sequences]
  (apply runtime/c classes-or-class-sequences))

(defn class-name
  "Serialize a composed style value or compatible class input to a string."
  [value]
  (runtime/class-name value))

(defn uni-style?
  "Return true when value is a metadata-aware uni.style class value."
  [value]
  (runtime/uni-style? value))

(defn style-class
  "Private compiler boundary for registering runtime class descriptors."
  [class-value descriptor-version create-descriptor]
  (runtime/style-class class-value descriptor-version create-descriptor))

(defmacro def-theme
  "Define a stable, unstyled theme class, shared by JVM and browser builds."
  [sym]
  (theme/def-theme* &env sym))

(defmacro def-token
  "Define a CSS-variable keyword from theme/value pairs and a mandatory default.
   Variables are emitted in the first, theme cascade layer.
   Missing theme values use the default, including on nested theme scopes."
  [sym & forms]
  (theme/def-token* &env sym forms))

(def ^:const layer-priority-css
  "Global cascade-layer order. Emit before any named-layer rules."
  css/layer-priority-css)

(def property-css
  "Non-inheriting @property registrations behind composed utilities such as
   the box-shadow layers. Build output emits them after the layer order."
  css/property-css)

(defn token-css
  "Export all loaded tokens, or an explicit subset, in the theme layer.
   Theme is a def-theme class or nil for :root defaults. Explicit collections are
   sorted and deduplicated; empty returns \"\".
   No priority header is included. CSS references are preserved, so standalone
   subsets must include any referenced tokens. Uses loaded definitions, not build state."
  ([theme] (tokens/token-css theme))
  ([theme token-list] (tokens/token-css theme token-list)))

(defn all-tokens
  "Return a sorted vector of all loaded token keywords, not just build-reachable tokens."
  []
  (tokens/all-tokens))

(defn token-value
  "Return a declared token value for a theme (or its default). Resolve token aliases,
   not CSS expressions. Also supported inside compile-time utility arguments."
  ([token] (tokens/token-value token))
  ([token theme] (tokens/token-value token theme)))

(defmacro def-animation
  "Define a CSS animation class. The first argument is the class symbol and the
   second is a properties map containing a non-empty `:frames` map. Supported
   properties are `:duration`, `:easing`, `:delay`, `:iterations`, `:direction`,
   `:fill-mode`, `:play-state`, and `:layer` (default `:components`)."
  [sym props]
  (animation/def-animation* &env sym props))

(defmacro def-base
  "Define styles in the base cascade layer for an arbitrary compile-time CSS
   selector from utility calls and previously composed classes."
  [selector & forms]
  (composition/def-base* &env selector forms))

(defmacro raw
  "Create an atomic class from an arbitrary compile-time CSS declaration map.
   Raw styles can be used anywhere a utility can, including modifiers,
   `def-base`, `def-component`, and `def-variants`."
  [declaration]
  (compiler/raw* &env declaration))

(defmacro str
  "Concatenate compile-time CSS value components. Token keywords become CSS
   custom-property references; alpha values become color-mix expressions.
   No separators are inserted."
  [component & components]
  (compiler/str-values* &env (cons component components)))

(defmacro inc
  "Increment a compile-time numeric value and emit the result as a literal."
  [value]
  (compiler/inc-value* &env value))

(defmacro dec
  "Decrement a compile-time numeric value and emit the result as a literal."
  [value]
  (compiler/dec-value* &env value))

(defmacro +
  "Add compile-time numeric values and emit the result as a literal."
  [& values]
  (compiler/add-values* &env values))

(defmacro -
  "Subtract compile-time numeric values and emit the result as a literal."
  [& values]
  (compiler/subtract-values* &env values))

(defmacro *
  "Multiply compile-time numeric values and emit the result as a literal. When
   the arguments are style classes, apply them to each direct child (`> *`)."
  [& values-or-forms]
  (modifier/direct-children-or-multiply* &env values-or-forms))

(defmacro /
  "Divide compile-time numeric values and emit the result as a literal."
  [& values]
  (compiler/divide-values* &env values))

(defmacro def-component
  "Define one CSS class in the components cascade layer from utility calls and
   previously composed classes. Development names start with the bare
   definition symbol and include a unique suffix unless
   `:qualified-component-classes` is enabled in uni.style's CSS build options.
   Declarations are inlined in order, so later declarations
   replace earlier ones. Style modifiers are emitted as selectors on the
   composed class. Release names are deterministic and compressed."
  [sym & forms]
  (composition/def-component* &env sym forms))

(defmacro css
  "Compile a literal CSS string from a non-empty literal selector map. Keys resolve to
   strings and are sorted lexicographically; values are one style or a vector of
   styles. Nested selectors and layer modifiers work as usual. Output is unlayered
   by default, with no generated classes, token definitions, priority header, or
   stylesheet registration. Each selector's forms must use one cascade layer."
  [rules]
  (composition/css* &env rules))

(defmacro def-variants
  "Define a map of CSS classes in the variants cascade layer. Each key must
   resolve at compile time to an unnamespaced keyword or a non-empty vector of
   unnamespaced keywords. A value is either one class form or a vector of class
   forms. Generated names combine the map symbol and resolved key names, then
   follow the same development naming rules as `def-component`."
  [sym class-map & extra]
  (when (seq extra)
    (throw (ex-info "A variants definition accepts one map"
                    {:symbol sym :class-map class-map :extra extra})))
  (composition/def-variants* &env sym class-map))

(defmacro animation-none
  "animation: none;"
  []
  (animation/animation-none* &env))

(defmacro origin
  "transform-origin: <token variable or literal CSS value>;"
  [value]
  (transform/origin* &env value))

(defmacro origin-center
  "transform-origin: center;"
  []
  (transform/static* &env :origin-center))

(defmacro origin-top
  "transform-origin: top;"
  []
  (transform/static* &env :origin-top))

(defmacro origin-top-right
  "transform-origin: top right;"
  []
  (transform/static* &env :origin-top-right))

(defmacro origin-right
  "transform-origin: right;"
  []
  (transform/static* &env :origin-right))

(defmacro origin-bottom-right
  "transform-origin: bottom right;"
  []
  (transform/static* &env :origin-bottom-right))

(defmacro origin-bottom
  "transform-origin: bottom;"
  []
  (transform/static* &env :origin-bottom))

(defmacro origin-bottom-left
  "transform-origin: bottom left;"
  []
  (transform/static* &env :origin-bottom-left))

(defmacro origin-left
  "transform-origin: left;"
  []
  (transform/static* &env :origin-left))

(defmacro origin-top-left
  "transform-origin: top left;"
  []
  (transform/static* &env :origin-top-left))

(defmacro transform-content
  "transform-box: content-box;"
  []
  (transform/static* &env :transform-content))

(defmacro transform-border
  "transform-box: border-box;"
  []
  (transform/static* &env :transform-border))

(defmacro transform-fill
  "transform-box: fill-box;"
  []
  (transform/static* &env :transform-fill))

(defmacro transform-stroke
  "transform-box: stroke-box;"
  []
  (transform/static* &env :transform-stroke))

(defmacro transform-view
  "transform-box: view-box;"
  []
  (transform/static* &env :transform-view))

(defmacro backface-hidden
  "backface-visibility: hidden;"
  []
  (transform/static* &env :backface-hidden))

(defmacro backface-visible
  "backface-visibility: visible;"
  []
  (transform/static* &env :backface-visible))

(defmacro scale
  "Scale both axes by a numeric percentage, CSS string, or token keyword.

   --us-scale-x: <factor>; --us-scale-y: <factor>;
   scale: var(--us-scale-x, 1) var(--us-scale-y, 1);"
  [percentage]
  (transform/scale* &env percentage))

(defmacro scale-x
  "Scale the horizontal axis like `scale`; negative percentages flip.

   --us-scale-x: <factor>;"
  [percentage]
  (transform/scale-axis* &env :x percentage))

(defmacro scale-y
  "Scale the vertical axis like `scale`; negative percentages flip.

   --us-scale-y: <factor>;"
  [percentage]
  (transform/scale-axis* &env :y percentage))

(defmacro scale-none
  "scale: none;"
  []
  (transform/static* &env :scale-none))

(defmacro rotate
  "rotate: <angle>; Numbers are degrees, strings are CSS literals, and
   unnamespaced keywords reference CSS custom properties."
  [angle]
  (transform/rotate* &env angle))

(defmacro rotate-none
  "rotate: none;"
  []
  (transform/static* &env :rotate-none))

(defmacro translate
  "Translate both axes. Numbers are pixels, strings are CSS literals, and
   unnamespaced keywords reference CSS custom properties.

   --us-translate-x: <x>; --us-translate-y: <y>;
   translate: var(--us-translate-x, 0) var(--us-translate-y, 0);"
  [x y]
  (transform/translate* &env x y))

(defmacro translate-x
  "Translate the horizontal axis like `translate`.

   --us-translate-x: <x>;"
  [x]
  (transform/translate-axis* &env :x x))

(defmacro translate-y
  "Translate the vertical axis like `translate`.

   --us-translate-y: <y>;"
  [y]
  (transform/translate-axis* &env :y y))

(defmacro translate-none
  "translate: none;"
  []
  (transform/static* &env :translate-none))

(defmacro transform
  "Set an arbitrary transform from a token, a literal CSS value, or multiple
   concatenated CSS value components. It applies after translate, rotate,
   and scale."
  ([value]
   (transform/transform* &env value))
  ([value next-value & values]
   (transform/transform* &env (compiler/str-values* &env (list* value next-value values)))))

(defmacro transform-none
  "transform: none;"
  []
  (transform/static* &env :transform-none))

(defmacro transition
  "Set the transition shorthand from a token, a literal CSS value, or multiple
   concatenated CSS value components. Token components become CSS
   custom-property references."
  ([value]
   (transition/transition* &env value))
  ([value next-value & values]
   (transition/transition* &env (compiler/str-values* &env
                                                      (list* value next-value values)))))

(defmacro transition-all
  "Transition all properties with Tailwind duration and timing defaults."
  []
  (transition/static* &env :transition-all))

(defmacro transition-colors
  "Transition color properties with Tailwind duration and timing defaults."
  []
  (transition/static* &env :transition-colors))

(defmacro transition-opacity
  "Transition opacity with Tailwind duration and timing defaults."
  []
  (transition/static* &env :transition-opacity))

(defmacro transition-shadow
  "Transition box-shadow with Tailwind duration and timing defaults."
  []
  (transition/static* &env :transition-shadow))

(defmacro transition-transform
  "Transition transform properties with Tailwind duration and timing defaults."
  []
  (transition/static* &env :transition-transform))

(defmacro transition-none
  "Disable transitions."
  []
  (transition/static* &env :transition-none))

(defmacro transition-normal
  "Use normal transition behavior."
  []
  (transition/behavior* &env :transition-normal))

(defmacro transition-discrete
  "Allow transitions for discrete properties."
  []
  (transition/behavior* &env :transition-discrete))

(defmacro duration
  "Set transition duration. Integers are milliseconds, strings are CSS literals,
   and unnamespaced keywords reference CSS custom properties."
  [value]
  (transition/duration* &env value))

(defmacro duration-initial
  "Reset transition duration to its initial value."
  []
  (transition/duration-initial* &env))

(defmacro ease
  "Set transition timing function. Integers and strings are CSS literals, and
   unnamespaced keywords reference CSS custom properties."
  [value]
  (transition/ease* &env value))

(defmacro ease-linear
  "Use a linear transition timing function."
  []
  (transition/timing* &env :ease-linear))

(defmacro ease-in
  "Use Tailwind's ease-in transition timing function."
  []
  (transition/timing* &env :ease-in))

(defmacro ease-out
  "Use Tailwind's ease-out transition timing function."
  []
  (transition/timing* &env :ease-out))

(defmacro ease-in-out
  "Use Tailwind's ease-in-out transition timing function."
  []
  (transition/timing* &env :ease-in-out))

(defmacro ease-initial
  "Reset transition timing function to its initial value."
  []
  (transition/timing* &env :ease-initial))

(defmacro delay
  "Set transition delay. Integers are milliseconds, strings are CSS literals,
   and unnamespaced keywords reference CSS custom properties."
  [value]
  (transition/delay* &env value))

(defmacro border
  "Set border width, solid style, and color; optionally use a color fallback."
  [& args]
  (border/border* &env "border" "border" args))

(defmacro border-t
  "Set top border width, solid style, and color; optionally use a color fallback."
  [& args]
  (border/border* &env "border-t" "border-top" args))

(defmacro border-r
  "Set right border width, solid style, and color; optionally use a color fallback."
  [& args]
  (border/border* &env "border-r" "border-right" args))

(defmacro border-b
  "Set bottom border width, solid style, and color; optionally use a color fallback."
  [& args]
  (border/border* &env "border-b" "border-bottom" args))

(defmacro border-l
  "Set left border width, solid style, and color; optionally use a color fallback."
  [& args]
  (border/border* &env "border-l" "border-left" args))

(defmacro border-x
  "Set left and right borders, like border-l and border-r with the same arguments."
  [& args]
  (border/border-axis* &env [["border-l" "border-left"] ["border-r" "border-right"]] args))

(defmacro border-y
  "Set top and bottom borders, like border-t and border-b with the same arguments."
  [& args]
  (border/border-axis* &env [["border-t" "border-top"] ["border-b" "border-bottom"]] args))

(defmacro border-width
  "Set border width from a non-negative pixel number, CSS string, or token keyword."
  [width]
  (border/border-width* &env width))

(defmacro border-color
  "border-color: <color>; optional CSS variable fallback."
  ([color]
   (border/border-color* &env color))
  ([color fallback]
   (border/border-color* &env color fallback)))

(defmacro border-solid
  "border-style: solid;"
  []
  (border/border-style* &env :solid))

(defmacro border-dashed
  "Set a dashed border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border" "border" :dashed args))

(defmacro border-t-dashed
  "Set a dashed top border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-t" "border-top" :dashed args))

(defmacro border-r-dashed
  "Set a dashed right border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-r" "border-right" :dashed args))

(defmacro border-b-dashed
  "Set a dashed bottom border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-b" "border-bottom" :dashed args))

(defmacro border-l-dashed
  "Set a dashed left border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-l" "border-left" :dashed args))

(defmacro border-dotted
  "Set a dotted border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border" "border" :dotted args))

(defmacro border-t-dotted
  "Set a dotted top border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-t" "border-top" :dotted args))

(defmacro border-r-dotted
  "Set a dotted right border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-r" "border-right" :dotted args))

(defmacro border-b-dotted
  "Set a dotted bottom border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-b" "border-bottom" :dotted args))

(defmacro border-l-dotted
  "Set a dotted left border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-l" "border-left" :dotted args))

(defmacro border-double
  "Set a double border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border" "border" :double args))

(defmacro border-t-double
  "Set a double top border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-t" "border-top" :double args))

(defmacro border-r-double
  "Set a double right border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-r" "border-right" :double args))

(defmacro border-b-double
  "Set a double bottom border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-b" "border-bottom" :double args))

(defmacro border-l-double
  "Set a double left border style, optionally with width, color, and color fallback."
  [& args]
  (border/styled-border* &env "border-l" "border-left" :double args))

(defmacro border-none
  "border-style: none;"
  []
  (border/border-style* &env :none))

(defmacro outline
  "Set outline width, solid style, and color; optionally use a color fallback."
  [& args]
  (border/outline* &env args))

(defmacro outline-width
  "Set a literal or variable-backed outline width; optionally use a variable fallback."
  ([width]
   (border/outline-width* &env width))
  ([width fallback]
   (border/outline-width* &env width fallback)))

(defmacro outline-color
  "outline-color: <color>; optional CSS variable fallback."
  ([color]
   (border/outline-color* &env color))
  ([color fallback]
   (border/outline-color* &env color fallback)))

(defmacro outline-offset
  "Set a literal or variable-backed outline offset; optionally use a variable fallback."
  ([offset]
   (border/outline-offset* &env offset))
  ([offset fallback]
   (border/outline-offset* &env offset fallback)))

(defmacro outline-solid
  "outline-style: solid;"
  []
  (border/outline-style* &env :solid))

(defmacro outline-dashed
  "outline-style: dashed;"
  []
  (border/outline-style* &env :dashed))

(defmacro outline-dotted
  "outline-style: dotted;"
  []
  (border/outline-style* &env :dotted))

(defmacro outline-double
  "outline-style: double;"
  []
  (border/outline-style* &env :double))

(defmacro outline-none
  "outline-style: none;"
  []
  (border/outline-style* &env :none))

(defmacro outline-hidden
  "outline: 2px solid transparent; outline-offset: 2px;"
  []
  (border/static* &env :outline-hidden))

(defmacro rounded
  "Set border radius from a non-negative pixel number, CSS string, or token keyword."
  [radius]
  (border/rounded* &env "rounded" [:border-radius] radius))

(defmacro rounded-tl
  "border-top-left-radius: <radius>;"
  [radius]
  (border/rounded* &env "rounded-tl" [:border-top-left-radius] radius))

(defmacro rounded-tr
  "border-top-right-radius: <radius>;"
  [radius]
  (border/rounded* &env "rounded-tr" [:border-top-right-radius] radius))

(defmacro rounded-br
  "border-bottom-right-radius: <radius>;"
  [radius]
  (border/rounded* &env "rounded-br" [:border-bottom-right-radius] radius))

(defmacro rounded-bl
  "border-bottom-left-radius: <radius>;"
  [radius]
  (border/rounded* &env "rounded-bl" [:border-bottom-left-radius] radius))

(defmacro rounded-t
  "border-top-left-radius: <radius>; border-top-right-radius: <radius>;"
  [radius]
  (border/rounded* &env "rounded-t" [:border-top-left-radius :border-top-right-radius] radius))

(defmacro rounded-r
  "border-top-right-radius: <radius>; border-bottom-right-radius: <radius>;"
  [radius]
  (border/rounded* &env "rounded-r" [:border-top-right-radius :border-bottom-right-radius] radius))

(defmacro rounded-b
  "border-bottom-right-radius: <radius>; border-bottom-left-radius: <radius>;"
  [radius]
  (border/rounded* &env "rounded-b" [:border-bottom-right-radius :border-bottom-left-radius] radius))

(defmacro rounded-l
  "border-bottom-left-radius: <radius>; border-top-left-radius: <radius>;"
  [radius]
  (border/rounded* &env "rounded-l" [:border-bottom-left-radius :border-top-left-radius] radius))

(defmacro select-none
  "-webkit-user-select: none; user-select: none;"
  []
  (interaction/static* &env :select-none))

(defmacro select-text
  "-webkit-user-select: text; user-select: text;"
  []
  (interaction/static* &env :select-text))

(defmacro select-all
  "-webkit-user-select: all; user-select: all;"
  []
  (interaction/static* &env :select-all))

(defmacro select-auto
  "-webkit-user-select: auto; user-select: auto;"
  []
  (interaction/static* &env :select-auto))

(defmacro var
  "Assign a CSS custom property to a token-backed or literal compile-time value."
  [property value]
  (style-var/var* &env property value))

(defmacro var-def
  "The name of a CSS custom property as a literal string: `(var-def :accent)` is
   \"--accent\". Accepts a compile-time keyword, including def-token constants,
   or a \"--name\" string. Use it as an inline :style key to set a property at
   runtime that a utility reads, e.g. `(s/bg :accent)`."
  [property]
  (style-var/var-def* &env property))

(defmacro bg
  "background-color: <color>; optional CSS variable fallback."
  ([color-var]
   (color/bg* &env color-var))
  ([color-var fallback-var]
   (color/bg* &env color-var fallback-var)))

(defmacro bg-transparent
  "background-color: transparent;"
  []
  (color/static* &env :bg-transparent))

(defmacro bg-current
  "background-color: currentColor;"
  []
  (color/static* &env :bg-current))

(defmacro bg-inherit
  "background-color: inherit;"
  []
  (color/static* &env :bg-inherit))

(defmacro bg-unset
  "background-color: unset;"
  []
  (color/static* &env :bg-unset))

(defmacro bg-image
  "Set background-image from a token, a literal CSS value, or multiple
   concatenated CSS value components. Token components become CSS
   custom-property references."
  ([value]
   (color/bg-image* &env value))
  ([value next-value & values]
   (color/bg-image* &env (compiler/str-values* &env
                                               (list* value next-value values)))))

(defmacro bg-overlay
  "Paint a solid color over the element's background color, as the
   single-color gradient `background-image: linear-gradient(<color>, <color>)`.
   A translucent color, such as a hover tint, then composites on the element's
   own background instead of showing what is behind the element. Accepts the
   same color forms as `bg`, including `alpha`."
  [color]
  (color/bg-overlay* &env color))

(defmacro bg-none
  "background-image: none;"
  []
  (color/static* &env :bg-none))

(defmacro bg-clip-border
  "background-clip: border-box;"
  []
  (color/static* &env :bg-clip-border))

(defmacro bg-clip-padding
  "background-clip: padding-box;"
  []
  (color/static* &env :bg-clip-padding))

(defmacro bg-clip-content
  "background-clip: content-box;"
  []
  (color/static* &env :bg-clip-content))

(defmacro bg-clip-text
  "background-clip: text;"
  []
  (color/static* &env :bg-clip-text))

(defmacro bg-repeat
  "background-repeat: repeat;"
  []
  (color/static* &env :bg-repeat))

(defmacro bg-repeat-x
  "background-repeat: repeat-x;"
  []
  (color/static* &env :bg-repeat-x))

(defmacro bg-repeat-y
  "background-repeat: repeat-y;"
  []
  (color/static* &env :bg-repeat-y))

(defmacro bg-repeat-space
  "background-repeat: space;"
  []
  (color/static* &env :bg-repeat-space))

(defmacro bg-repeat-round
  "background-repeat: round;"
  []
  (color/static* &env :bg-repeat-round))

(defmacro bg-no-repeat
  "background-repeat: no-repeat;"
  []
  (color/static* &env :bg-no-repeat))

(defmacro bg-auto
  "background-size: auto;"
  []
  (color/static* &env :bg-auto))

(defmacro bg-cover
  "background-size: cover;"
  []
  (color/static* &env :bg-cover))

(defmacro bg-contain
  "background-size: contain;"
  []
  (color/static* &env :bg-contain))

(defmacro bg-size
  "Set background-size from a token keyword or literal CSS string."
  [value]
  (color/background-value* &env "bg-size" :background-size value
                           "Background size" :background-size))

(defmacro bg-top-left
  "background-position: top left;"
  []
  (color/static* &env :bg-top-left))

(defmacro bg-top
  "background-position: top;"
  []
  (color/static* &env :bg-top))

(defmacro bg-top-right
  "background-position: top right;"
  []
  (color/static* &env :bg-top-right))

(defmacro bg-left
  "background-position: left;"
  []
  (color/static* &env :bg-left))

(defmacro bg-center
  "background-position: center;"
  []
  (color/static* &env :bg-center))

(defmacro bg-right
  "background-position: right;"
  []
  (color/static* &env :bg-right))

(defmacro bg-bottom-left
  "background-position: bottom left;"
  []
  (color/static* &env :bg-bottom-left))

(defmacro bg-bottom
  "background-position: bottom;"
  []
  (color/static* &env :bg-bottom))

(defmacro bg-bottom-right
  "background-position: bottom right;"
  []
  (color/static* &env :bg-bottom-right))

(defmacro bg-position
  "Set background-position from a token keyword or literal CSS string."
  [value]
  (color/background-value* &env "bg-position" :background-position value
                           "Background position" :background-position))

(defmacro mask-image
  "Set mask-image from a token, a literal CSS value, or multiple concatenated
   CSS value components. Token components become CSS custom-property
   references. Also emits -webkit-mask-image."
  ([value]
   (color/mask-value* &env "mask-image" :mask-image value "Mask image" :mask-image))
  ([value next-value & values]
   (color/mask-value* &env "mask-image" :mask-image
                      (compiler/str-values* &env (list* value next-value values))
                      "Mask image" :mask-image)))

(defmacro mask-size
  "Set mask-size from a token keyword or literal CSS string. Also emits
   -webkit-mask-size."
  [value]
  (color/mask-value* &env "mask-size" :mask-size value "Mask size" :mask-size))

(defmacro mask-position
  "Set mask-position from a token keyword or literal CSS string. Also emits
   -webkit-mask-position."
  [value]
  (color/mask-value* &env "mask-position" :mask-position value
                     "Mask position" :mask-position))

(defmacro mask-type-alpha
  "mask-type: alpha;"
  []
  (color/static* &env :mask-type-alpha))

(defmacro mask-type-luminance
  "mask-type: luminance;"
  []
  (color/static* &env :mask-type-luminance))

(defmacro bg-blend-normal
  "background-blend-mode: normal;"
  []
  (color/static* &env :bg-blend-normal))

(defmacro bg-blend-multiply
  "background-blend-mode: multiply;"
  []
  (color/static* &env :bg-blend-multiply))

(defmacro bg-blend-screen
  "background-blend-mode: screen;"
  []
  (color/static* &env :bg-blend-screen))

(defmacro bg-blend-overlay
  "background-blend-mode: overlay;"
  []
  (color/static* &env :bg-blend-overlay))

(defmacro bg-blend-darken
  "background-blend-mode: darken;"
  []
  (color/static* &env :bg-blend-darken))

(defmacro bg-blend-lighten
  "background-blend-mode: lighten;"
  []
  (color/static* &env :bg-blend-lighten))

(defmacro bg-blend-color-dodge
  "background-blend-mode: color-dodge;"
  []
  (color/static* &env :bg-blend-color-dodge))

(defmacro bg-blend-color-burn
  "background-blend-mode: color-burn;"
  []
  (color/static* &env :bg-blend-color-burn))

(defmacro bg-blend-hard-light
  "background-blend-mode: hard-light;"
  []
  (color/static* &env :bg-blend-hard-light))

(defmacro bg-blend-soft-light
  "background-blend-mode: soft-light;"
  []
  (color/static* &env :bg-blend-soft-light))

(defmacro bg-blend-difference
  "background-blend-mode: difference;"
  []
  (color/static* &env :bg-blend-difference))

(defmacro bg-blend-exclusion
  "background-blend-mode: exclusion;"
  []
  (color/static* &env :bg-blend-exclusion))

(defmacro bg-blend-hue
  "background-blend-mode: hue;"
  []
  (color/static* &env :bg-blend-hue))

(defmacro bg-blend-saturation
  "background-blend-mode: saturation;"
  []
  (color/static* &env :bg-blend-saturation))

(defmacro bg-blend-color
  "background-blend-mode: color;"
  []
  (color/static* &env :bg-blend-color))

(defmacro bg-blend-luminosity
  "background-blend-mode: luminosity;"
  []
  (color/static* &env :bg-blend-luminosity))

(defmacro mix-blend-normal
  "mix-blend-mode: normal;"
  []
  (color/static* &env :mix-blend-normal))

(defmacro mix-blend-multiply
  "mix-blend-mode: multiply;"
  []
  (color/static* &env :mix-blend-multiply))

(defmacro mix-blend-screen
  "mix-blend-mode: screen;"
  []
  (color/static* &env :mix-blend-screen))

(defmacro mix-blend-overlay
  "mix-blend-mode: overlay;"
  []
  (color/static* &env :mix-blend-overlay))

(defmacro mix-blend-darken
  "mix-blend-mode: darken;"
  []
  (color/static* &env :mix-blend-darken))

(defmacro mix-blend-lighten
  "mix-blend-mode: lighten;"
  []
  (color/static* &env :mix-blend-lighten))

(defmacro mix-blend-color-dodge
  "mix-blend-mode: color-dodge;"
  []
  (color/static* &env :mix-blend-color-dodge))

(defmacro mix-blend-color-burn
  "mix-blend-mode: color-burn;"
  []
  (color/static* &env :mix-blend-color-burn))

(defmacro mix-blend-hard-light
  "mix-blend-mode: hard-light;"
  []
  (color/static* &env :mix-blend-hard-light))

(defmacro mix-blend-soft-light
  "mix-blend-mode: soft-light;"
  []
  (color/static* &env :mix-blend-soft-light))

(defmacro mix-blend-difference
  "mix-blend-mode: difference;"
  []
  (color/static* &env :mix-blend-difference))

(defmacro mix-blend-exclusion
  "mix-blend-mode: exclusion;"
  []
  (color/static* &env :mix-blend-exclusion))

(defmacro mix-blend-hue
  "mix-blend-mode: hue;"
  []
  (color/static* &env :mix-blend-hue))

(defmacro mix-blend-saturation
  "mix-blend-mode: saturation;"
  []
  (color/static* &env :mix-blend-saturation))

(defmacro mix-blend-color
  "mix-blend-mode: color;"
  []
  (color/static* &env :mix-blend-color))

(defmacro mix-blend-luminosity
  "mix-blend-mode: luminosity;"
  []
  (color/static* &env :mix-blend-luminosity))

(defmacro mix-blend-plus-darker
  "mix-blend-mode: plus-darker;"
  []
  (color/static* &env :mix-blend-plus-darker))

(defmacro mix-blend-plus-lighter
  "mix-blend-mode: plus-lighter;"
  []
  (color/static* &env :mix-blend-plus-lighter))

(defmacro backdrop-blur-none
  "-webkit-backdrop-filter: ; backdrop-filter: ;"
  []
  (color/static* &env :backdrop-blur-none))

(defmacro backdrop-blur
  "Set backdrop-filter to blur() with a token variable or literal CSS radius.
   Also emits -webkit-backdrop-filter."
  [value]
  (color/backdrop-blur* &env value))

(defmacro blur
  "filter: blur(<radius>); numbers are pixels. Replaces the whole filter, like
   filter."
  [value]
  (color/blur* &env value))

(defmacro filter
  "filter: <token variable or literal CSS filter>;"
  [value]
  (color/filter* &env value))

(defmacro shadow
  "Set the outer box-shadow layer from a token, a literal CSS shadow, or
   multiple concatenated CSS value components. Token components become CSS
   custom-property references.

   --us-shadow: <shadow>;
   box-shadow: var(--us-inset-shadow, 0 0 #0000), var(--us-inset-ring-shadow, 0 0 #0000),
               var(--us-ring-shadow, 0 0 #0000), var(--us-shadow, 0 0 #0000);"
  ([shadow]
   (color/shadow* &env shadow))
  ([shadow next-shadow & shadows]
   (color/shadow* &env (compiler/str-values* &env
                                             (list* shadow next-shadow shadows)))))

(defmacro shadow-none
  "Clear the outer box-shadow layer; other shadow layers remain.

   --us-shadow: 0 0 #0000;"
  []
  (color/shadow-layer-none* &env :shadow))

(defmacro inset-shadow
  "Set the inset box-shadow layer like `shadow`, adding the inset keyword to
   each literal shadow that lacks it. A token must hold a single shadow.

   --us-inset-shadow: inset <shadow>;"
  ([shadow]
   (color/inset-shadow* &env shadow))
  ([shadow next-shadow & shadows]
   (color/inset-shadow* &env (compiler/str-values* &env
                                                   (list* shadow next-shadow shadows)))))

(defmacro inset-shadow-none
  "Clear the inset box-shadow layer.

   --us-inset-shadow: 0 0 #0000;"
  []
  (color/shadow-layer-none* &env :inset-shadow))

(defmacro ring
  "Set a solid outer ring as a box-shadow layer: color, width and color, or
   width, token color, and fallback color. Width defaults to 1px.

   --us-ring-shadow: 0 0 0 <width> <color>;"
  [& args]
  (color/ring* &env :ring args))

(defmacro ring-none
  "Clear the ring box-shadow layer.

   --us-ring-shadow: 0 0 #0000;"
  []
  (color/shadow-layer-none* &env :ring))

(defmacro inset-ring
  "Set a solid inset ring as a box-shadow layer, with the same arguments as
   `ring`.

   --us-inset-ring-shadow: inset 0 0 0 <width> <color>;"
  [& args]
  (color/ring* &env :inset-ring args))

(defmacro inset-ring-none
  "Clear the inset ring box-shadow layer.

   --us-inset-ring-shadow: 0 0 #0000;"
  []
  (color/shadow-layer-none* &env :inset-ring))

(defmacro text-shadow
  "Set text-shadow from a token, a literal CSS shadow, or multiple concatenated
   CSS value components. Token components become CSS custom-property
   references."
  ([shadow]
   (color/background-value* &env "text-shadow" :text-shadow shadow "Text shadow" :text-shadow))
  ([shadow next-shadow & shadows]
   (color/background-value* &env "text-shadow" :text-shadow
                            (compiler/str-values* &env (list* shadow next-shadow shadows))
                            "Text shadow" :text-shadow)))

(defmacro text-shadow-none
  "text-shadow: none;"
  []
  (typography/static* &env :text-shadow-none))

(defmacro color
  "color: <color>; optional CSS variable fallback."
  ([color-var]
   (color/color* &env color-var))
  ([color-var fallback-var]
   (color/color* &env color-var fallback-var)))

(defmacro color-transparent
  "color: transparent;"
  []
  (color/static* &env :color-transparent))

(defmacro color-current
  "color: currentColor;"
  []
  (color/static* &env :color-current))

(defmacro color-inherit
  "color: inherit;"
  []
  (color/static* &env :color-inherit))

(defmacro caret-inherit
  "caret-color: inherit;"
  []
  (color/static* &env :caret-inherit))

(defmacro caret-current
  "caret-color: currentColor;"
  []
  (color/static* &env :caret-current))

(defmacro caret-transparent
  "caret-color: transparent;"
  []
  (color/static* &env :caret-transparent))

(defmacro caret
  "Set caret-color from a token keyword or literal CSS string."
  [color-var]
  (color/caret* &env color-var))

(defmacro fill
  "fill: <color>; optional CSS variable fallback."
  ([color-var]
   (color/fill* &env color-var))
  ([color-var fallback-var]
   (color/fill* &env color-var fallback-var)))

(defmacro stroke
  "stroke: <color>; optional CSS variable fallback."
  ([color-var]
   (color/stroke* &env color-var))
  ([color-var fallback-var]
   (color/stroke* &env color-var fallback-var)))

(defmacro text-left
  "text-align: left;"
  []
  (typography/static* &env :text-left))

(defmacro text-center
  "text-align: center;"
  []
  (typography/static* &env :text-center))

(defmacro text-right
  "text-align: right;"
  []
  (typography/static* &env :text-right))

(defmacro text-justify
  "text-align: justify;"
  []
  (typography/static* &env :text-justify))

(defmacro text-start
  "text-align: start;"
  []
  (typography/static* &env :text-start))

(defmacro text-end
  "text-align: end;"
  []
  (typography/static* &env :text-end))

(defmacro uppercase
  "text-transform: uppercase;"
  []
  (typography/static* &env :uppercase))

(defmacro lowercase
  "text-transform: lowercase;"
  []
  (typography/static* &env :lowercase))

(defmacro capitalize
  "text-transform: capitalize;"
  []
  (typography/static* &env :capitalize))

(defmacro normal-case
  "text-transform: none;"
  []
  (typography/static* &env :normal-case))

(defmacro truncate
  "overflow: hidden; text-overflow: ellipsis; white-space: nowrap;"
  []
  (typography/static* &env :truncate))

(defmacro text-ellipsis
  "text-overflow: ellipsis;"
  []
  (typography/static* &env :text-ellipsis))

(defmacro text-clip
  "text-overflow: clip;"
  []
  (typography/static* &env :text-clip))

(defmacro whitespace-normal
  "white-space: normal;"
  []
  (typography/static* &env :whitespace-normal))

(defmacro whitespace-nowrap
  "white-space: nowrap;"
  []
  (typography/static* &env :whitespace-nowrap))

(defmacro whitespace-pre
  "white-space: pre;"
  []
  (typography/static* &env :whitespace-pre))

(defmacro whitespace-pre-line
  "white-space: pre-line;"
  []
  (typography/static* &env :whitespace-pre-line))

(defmacro whitespace-pre-wrap
  "white-space: pre-wrap;"
  []
  (typography/static* &env :whitespace-pre-wrap))

(defmacro whitespace-break-spaces
  "white-space: break-spaces;"
  []
  (typography/static* &env :whitespace-break-spaces))

(defmacro text-wrap
  "text-wrap: wrap;"
  []
  (typography/static* &env :text-wrap))

(defmacro text-nowrap
  "text-wrap: nowrap;"
  []
  (typography/static* &env :text-nowrap))

(defmacro text-balance
  "text-wrap: balance;"
  []
  (typography/static* &env :text-balance))

(defmacro text-pretty
  "text-wrap: pretty;"
  []
  (typography/static* &env :text-pretty))

(defmacro word-break-normal
  "word-break: normal;"
  []
  (typography/static* &env :word-break-normal))

(defmacro word-break-word
  "word-break: break-word;"
  []
  (typography/static* &env :word-break-word))

(defmacro word-break-all
  "word-break: break-all;"
  []
  (typography/static* &env :word-break-all))

(defmacro word-break-keep
  "word-break: keep-all;"
  []
  (typography/static* &env :word-break-keep))

(defmacro overflow-wrap-normal
  "overflow-wrap: normal;"
  []
  (typography/static* &env :overflow-wrap-normal))

(defmacro overflow-wrap-break-word
  "overflow-wrap: break-word;"
  []
  (typography/static* &env :overflow-wrap-break-word))

(defmacro overflow-wrap-anywhere
  "overflow-wrap: anywhere;"
  []
  (typography/static* &env :overflow-wrap-anywhere))

(defmacro list-inside
  "list-style-position: inside;"
  []
  (typography/static* &env :list-inside))

(defmacro list-outside
  "list-style-position: outside;"
  []
  (typography/static* &env :list-outside))

(defmacro list-none
  "list-style-type: none;"
  []
  (typography/static* &env :list-none))

(defmacro list-disc
  "list-style-type: disc;"
  []
  (typography/static* &env :list-disc))

(defmacro list-decimal
  "list-style-type: decimal;"
  []
  (typography/static* &env :list-decimal))

(defmacro list-image-none
  "list-style-image: none;"
  []
  (typography/static* &env :list-image-none))

(defmacro inline
  "display: inline;"
  []
  (layout/static* &env :inline))

(defmacro block
  "display: block;"
  []
  (layout/static* &env :block))

(defmacro inline-block
  "display: inline-block;"
  []
  (layout/static* &env :inline-block))

(defmacro flow-root
  "display: flow-root;"
  []
  (layout/static* &env :flow-root))

(defmacro flex
  "display: flex;"
  []
  (layout/static* &env :flex))

(defmacro inline-flex
  "display: inline-flex;"
  []
  (layout/static* &env :inline-flex))

(defmacro grid
  "display: grid;"
  []
  (layout/static* &env :grid))

(defmacro inline-grid
  "display: inline-grid;"
  []
  (layout/static* &env :inline-grid))

(defmacro grid-cols
  "grid-template-columns: <value>;"
  [value]
  (layout/grid-value* &env "grid-cols" :grid-template-columns value))

(defmacro grid-rows
  "grid-template-rows: <value>;"
  [value]
  (layout/grid-value* &env "grid-rows" :grid-template-rows value))

(defmacro grid-auto-cols
  "grid-auto-columns: <value>;"
  [value]
  (layout/grid-value* &env "grid-auto-cols" :grid-auto-columns value))

(defmacro grid-auto-rows
  "grid-auto-rows: <value>;"
  [value]
  (layout/grid-value* &env "grid-auto-rows" :grid-auto-rows value))

(defmacro grid-col
  "grid-column: <value>;"
  [value]
  (layout/grid-value* &env "grid-col" :grid-column value))

(defmacro col-span
  "grid-column: span <n> / span <n>;"
  [n]
  (layout/grid-span* &env "col-span" :grid-column n))

(defmacro row-span
  "grid-row: span <n> / span <n>;"
  [n]
  (layout/grid-span* &env "row-span" :grid-row n))

(defmacro grid-col-start
  "grid-column-start: <value>;"
  [value]
  (layout/grid-value* &env "grid-col-start" :grid-column-start value))

(defmacro grid-col-end
  "grid-column-end: <value>;"
  [value]
  (layout/grid-value* &env "grid-col-end" :grid-column-end value))

(defmacro grid-row
  "grid-row: <value>;"
  [value]
  (layout/grid-value* &env "grid-row" :grid-row value))

(defmacro grid-row-start
  "grid-row-start: <value>;"
  [value]
  (layout/grid-value* &env "grid-row-start" :grid-row-start value))

(defmacro grid-row-end
  "grid-row-end: <value>;"
  [value]
  (layout/grid-value* &env "grid-row-end" :grid-row-end value))

(defmacro grid-area
  "grid-area: <value>;"
  [value]
  (layout/grid-value* &env "grid-area" :grid-area value))

(defmacro grid-flow-row
  "grid-auto-flow: row;"
  []
  (layout/static* &env :grid-flow-row))

(defmacro grid-flow-col
  "grid-auto-flow: column;"
  []
  (layout/static* &env :grid-flow-col))

(defmacro grid-flow-dense
  "grid-auto-flow: dense;"
  []
  (layout/static* &env :grid-flow-dense))

(defmacro grid-flow-row-dense
  "grid-auto-flow: row dense;"
  []
  (layout/static* &env :grid-flow-row-dense))

(defmacro grid-flow-col-dense
  "grid-auto-flow: column dense;"
  []
  (layout/static* &env :grid-flow-col-dense))

(defmacro list-item
  "display: list-item;"
  []
  (layout/static* &env :list-item))

(defmacro contents
  "display: contents;"
  []
  (layout/static* &env :contents))

(defmacro hidden
  "display: none;"
  []
  (layout/static* &env :hidden))

(defmacro box-border
  "box-sizing: border-box;"
  []
  (layout/static* &env :box-border))

(defmacro box-content
  "box-sizing: content-box;"
  []
  (layout/static* &env :box-content))

(defmacro container-normal
  "container-type: normal;"
  []
  (layout/static* &env :container-normal))

(defmacro container-size
  "container-type: size;"
  []
  (layout/static* &env :container-size))

(defmacro container-inline-size
  "container-type: inline-size;"
  []
  (layout/static* &env :container-inline-size))

(defmacro container-scroll-state
  "container-type: scroll-state;"
  []
  (layout/static* &env :container-scroll-state))

(defmacro container-anchored
  "container-type: anchored;"
  []
  (layout/static* &env :container-anchored))

(defmacro visible
  "visibility: visible;"
  []
  (layout/static* &env :visible))

(defmacro invisible
  "visibility: hidden;"
  []
  (layout/static* &env :invisible))

(defmacro collapse
  "visibility: collapse;"
  []
  (layout/static* &env :collapse))

(defmacro border-collapse
  "border-collapse: collapse;"
  []
  (layout/static* &env :border-collapse))

(defmacro border-separate
  "border-collapse: separate;"
  []
  (layout/static* &env :border-separate))

(defmacro border-spacing
  "border-spacing: <value>;"
  [value]
  (layout/border-spacing* &env value))

(defmacro table-auto
  "table-layout: auto;"
  []
  (layout/static* &env :table-auto))

(defmacro table-fixed
  "table-layout: fixed;"
  []
  (layout/static* &env :table-fixed))

(defmacro flex-row
  "flex-direction: row;"
  []
  (layout/static* &env :flex-row))

(defmacro flex-row-reverse
  "flex-direction: row-reverse;"
  []
  (layout/static* &env :flex-row-reverse))

(defmacro flex-col
  "flex-direction: column;"
  []
  (layout/static* &env :flex-col))

(defmacro flex-col-reverse
  "flex-direction: column-reverse;"
  []
  (layout/static* &env :flex-col-reverse))

(defmacro flex-wrap
  "flex-wrap: wrap;"
  []
  (layout/static* &env :flex-wrap))

(defmacro flex-wrap-reverse
  "flex-wrap: wrap-reverse;"
  []
  (layout/static* &env :flex-wrap-reverse))

(defmacro flex-nowrap
  "flex-wrap: nowrap;"
  []
  (layout/static* &env :flex-nowrap))

(defmacro flex-1
  "flex: 1 1 0%;"
  []
  (layout/static* &env :flex-1))

(defmacro flex-auto
  "flex: 1 1 auto;"
  []
  (layout/static* &env :flex-auto))

(defmacro flex-initial
  "flex: 0 1 auto;"
  []
  (layout/static* &env :flex-initial))

(defmacro flex-none
  "flex: none;"
  []
  (layout/static* &env :flex-none))

(defmacro align-baseline
  "vertical-align: baseline;"
  []
  (layout/static* &env :align-baseline))

(defmacro align-top
  "vertical-align: top;"
  []
  (layout/static* &env :align-top))

(defmacro align-middle
  "vertical-align: middle;"
  []
  (layout/static* &env :align-middle))

(defmacro align-bottom
  "vertical-align: bottom;"
  []
  (layout/static* &env :align-bottom))

(defmacro align-text-top
  "vertical-align: text-top;"
  []
  (layout/static* &env :align-text-top))

(defmacro align-text-bottom
  "vertical-align: text-bottom;"
  []
  (layout/static* &env :align-text-bottom))

(defmacro align-sub
  "vertical-align: sub;"
  []
  (layout/static* &env :align-sub))

(defmacro align-super
  "vertical-align: super;"
  []
  (layout/static* &env :align-super))

(defmacro grow
  "flex-grow: <value>; defaults to 1."
  ([]
   (layout/static* &env :grow))
  ([value]
   (layout/flex-factor* &env "grow" :flex-grow value)))

(defmacro shrink
  "flex-shrink: <value>; defaults to 1."
  ([]
   (layout/static* &env :shrink))
  ([value]
   (layout/flex-factor* &env "shrink" :flex-shrink value)))

(defmacro self-auto
  "align-self: auto;"
  []
  (layout/static* &env :self-auto))

(defmacro self-start
  "align-self: flex-start;"
  []
  (layout/static* &env :self-start))

(defmacro self-end
  "align-self: flex-end;"
  []
  (layout/static* &env :self-end))

(defmacro self-end-safe
  "align-self: safe flex-end;"
  []
  (layout/static* &env :self-end-safe))

(defmacro self-center
  "align-self: center;"
  []
  (layout/static* &env :self-center))

(defmacro self-center-safe
  "align-self: safe center;"
  []
  (layout/static* &env :self-center-safe))

(defmacro self-stretch
  "align-self: stretch;"
  []
  (layout/static* &env :self-stretch))

(defmacro self-baseline
  "align-self: baseline;"
  []
  (layout/static* &env :self-baseline))

(defmacro self-baseline-last
  "align-self: last baseline;"
  []
  (layout/static* &env :self-baseline-last))

(defmacro items
  "Set align-items from a token keyword or literal CSS string."
  [value]
  (layout/items* &env value))

(defmacro items-inherit
  "align-items: inherit;"
  []
  (layout/static* &env :items-inherit))

(defmacro items-start
  "align-items: flex-start;"
  []
  (layout/static* &env :items-start))

(defmacro items-end
  "align-items: flex-end;"
  []
  (layout/static* &env :items-end))

(defmacro items-end-safe
  "align-items: safe flex-end;"
  []
  (layout/static* &env :items-end-safe))

(defmacro items-center
  "align-items: center;"
  []
  (layout/static* &env :items-center))

(defmacro items-center-safe
  "align-items: safe center;"
  []
  (layout/static* &env :items-center-safe))

(defmacro items-baseline
  "align-items: baseline;"
  []
  (layout/static* &env :items-baseline))

(defmacro items-baseline-last
  "align-items: last baseline;"
  []
  (layout/static* &env :items-baseline-last))

(defmacro items-stretch
  "align-items: stretch;"
  []
  (layout/static* &env :items-stretch))

(defmacro justify-start
  "justify-content: flex-start;"
  []
  (layout/static* &env :justify-start))

(defmacro justify-end
  "justify-content: flex-end;"
  []
  (layout/static* &env :justify-end))

(defmacro justify-end-safe
  "justify-content: safe flex-end;"
  []
  (layout/static* &env :justify-end-safe))

(defmacro justify-center
  "justify-content: center;"
  []
  (layout/static* &env :justify-center))

(defmacro justify-center-safe
  "justify-content: safe center;"
  []
  (layout/static* &env :justify-center-safe))

(defmacro justify-between
  "justify-content: space-between;"
  []
  (layout/static* &env :justify-between))

(defmacro justify-around
  "justify-content: space-around;"
  []
  (layout/static* &env :justify-around))

(defmacro justify-evenly
  "justify-content: space-evenly;"
  []
  (layout/static* &env :justify-evenly))

(defmacro justify-stretch
  "justify-content: stretch;"
  []
  (layout/static* &env :justify-stretch))

(defmacro justify-baseline
  "justify-content: baseline;"
  []
  (layout/static* &env :justify-baseline))

(defmacro justify-normal
  "justify-content: normal;"
  []
  (layout/static* &env :justify-normal))

(defmacro justify-items-start
  "justify-items: start;"
  []
  (layout/static* &env :justify-items-start))

(defmacro justify-items-end
  "justify-items: end;"
  []
  (layout/static* &env :justify-items-end))

(defmacro justify-items-end-safe
  "justify-items: safe end;"
  []
  (layout/static* &env :justify-items-end-safe))

(defmacro justify-items-center
  "justify-items: center;"
  []
  (layout/static* &env :justify-items-center))

(defmacro justify-items-center-safe
  "justify-items: safe center;"
  []
  (layout/static* &env :justify-items-center-safe))

(defmacro justify-items-stretch
  "justify-items: stretch;"
  []
  (layout/static* &env :justify-items-stretch))

(defmacro justify-items-normal
  "justify-items: normal;"
  []
  (layout/static* &env :justify-items-normal))

(defmacro justify-self-auto
  "justify-self: auto;"
  []
  (layout/static* &env :justify-self-auto))

(defmacro justify-self-start
  "justify-self: start;"
  []
  (layout/static* &env :justify-self-start))

(defmacro justify-self-center
  "justify-self: center;"
  []
  (layout/static* &env :justify-self-center))

(defmacro justify-self-center-safe
  "justify-self: safe center;"
  []
  (layout/static* &env :justify-self-center-safe))

(defmacro justify-self-end
  "justify-self: end;"
  []
  (layout/static* &env :justify-self-end))

(defmacro justify-self-end-safe
  "justify-self: safe end;"
  []
  (layout/static* &env :justify-self-end-safe))

(defmacro justify-self-stretch
  "justify-self: stretch;"
  []
  (layout/static* &env :justify-self-stretch))

(defmacro content-normal
  "align-content: normal;"
  []
  (layout/static* &env :content-normal))

(defmacro content-center
  "align-content: center;"
  []
  (layout/static* &env :content-center))

(defmacro content-start
  "align-content: flex-start;"
  []
  (layout/static* &env :content-start))

(defmacro content-end
  "align-content: flex-end;"
  []
  (layout/static* &env :content-end))

(defmacro content-between
  "align-content: space-between;"
  []
  (layout/static* &env :content-between))

(defmacro content-around
  "align-content: space-around;"
  []
  (layout/static* &env :content-around))

(defmacro content-evenly
  "align-content: space-evenly;"
  []
  (layout/static* &env :content-evenly))

(defmacro content-baseline
  "align-content: baseline;"
  []
  (layout/static* &env :content-baseline))

(defmacro content-stretch
  "align-content: stretch;"
  []
  (layout/static* &env :content-stretch))

(defmacro content
  "Set content from a quoted string literal or token keyword."
  [value]
  (layout/content* &env value))

(defmacro content-none
  "content: none;"
  []
  (layout/static* &env :content-none))

(defmacro order
  "order: <integer>;"
  [value]
  (position/order* &env value))

(defmacro order-first
  "order: -9999;"
  []
  (position/static* &env :order-first))

(defmacro order-last
  "order: 9999;"
  []
  (position/static* &env :order-last))

(defmacro order-none
  "order: 0;"
  []
  (position/static* &env :order-none))

(defmacro opacity
  "Set the element's CSS opacity declaration. Numeric values are percentages."
  [value]
  (color/opacity* &env value))

(defmacro alpha
  "Apply percentage alpha to a color value, without changing element opacity.
   Usable in color utilities, token definitions, and s/str."
  [color percentage]
  (color/alpha* &env color percentage))

(defmacro font-size
  "font-size: <size>;"
  [size]
  (typography/font-size* &env size))

(defmacro text
  "font-size: <size>; line-height: <line-height>;"
  [size line-height]
  (typography/text* &env size line-height))

(defmacro font-size-inherit
  "font-size: inherit;"
  []
  (typography/static* &env :font-size-inherit))

(defmacro line-height-none
  "line-height: 1;"
  []
  (typography/static* &env :line-height-none))

(defmacro line-height-inherit
  "line-height: inherit;"
  []
  (typography/static* &env :line-height-inherit))

(defmacro line-height
  "line-height: <value>;"
  [value]
  (typography/line-height* &env value))

(defmacro font
  "font-family: <value>;"
  [value]
  (typography/font* &env value))

(defmacro font-inherit
  "font: inherit;"
  []
  (typography/static* &env :font-inherit))

(defmacro font-weight-inherit
  "font-weight: inherit;"
  []
  (typography/static* &env :font-weight-inherit))

(defmacro italic
  "font-style: italic;"
  []
  (typography/static* &env :italic))

(defmacro not-italic
  "font-style: normal;"
  []
  (typography/static* &env :not-italic))

(defmacro font-mono
  "font-family: Tailwind's monospace stack, ui-monospace first."
  []
  (typography/static* &env :font-mono))

(defmacro font-thin
  "font-weight: 100;"
  []
  (typography/static* &env :font-thin))

(defmacro font-extralight
  "font-weight: 200;"
  []
  (typography/static* &env :font-extralight))

(defmacro font-light
  "font-weight: 300;"
  []
  (typography/static* &env :font-light))

(defmacro font-normal
  "font-weight: 400;"
  []
  (typography/static* &env :font-normal))

(defmacro font-medium
  "font-weight: 500;"
  []
  (typography/static* &env :font-medium))

(defmacro font-semibold
  "font-weight: 600;"
  []
  (typography/static* &env :font-semibold))

(defmacro font-bold
  "font-weight: 700;"
  []
  (typography/static* &env :font-bold))

(defmacro font-extrabold
  "font-weight: 800;"
  []
  (typography/static* &env :font-extrabold))

(defmacro font-black
  "font-weight: 900;"
  []
  (typography/static* &env :font-black))

(defmacro underline
  "text-decoration-line: underline;"
  []
  (typography/static* &env :underline))

(defmacro overline
  "text-decoration-line: overline;"
  []
  (typography/static* &env :overline))

(defmacro line-through
  "text-decoration-line: line-through;"
  []
  (typography/static* &env :line-through))

(defmacro no-underline
  "text-decoration-line: none;"
  []
  (typography/static* &env :no-underline))

(defmacro decoration-solid
  "text-decoration-style: solid;"
  []
  (typography/static* &env :decoration-solid))

(defmacro decoration-double
  "text-decoration-style: double;"
  []
  (typography/static* &env :decoration-double))

(defmacro decoration-dotted
  "text-decoration-style: dotted;"
  []
  (typography/static* &env :decoration-dotted))

(defmacro decoration-dashed
  "text-decoration-style: dashed;"
  []
  (typography/static* &env :decoration-dashed))

(defmacro decoration-wavy
  "text-decoration-style: wavy;"
  []
  (typography/static* &env :decoration-wavy))

(defmacro antialiased
  "-webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale;"
  []
  (typography/static* &env :antialiased))

(defmacro subpixel-antialiased
  "-webkit-font-smoothing: auto; -moz-osx-font-smoothing: auto;"
  []
  (typography/static* &env :subpixel-antialiased))

(defmacro decoration-inherit
  "text-decoration-color: inherit;"
  []
  (color/static* &env :decoration-inherit))

(defmacro decoration-current
  "text-decoration-color: currentColor;"
  []
  (color/static* &env :decoration-current))

(defmacro decoration-transparent
  "text-decoration-color: transparent;"
  []
  (color/static* &env :decoration-transparent))

(defmacro decoration
  "Set text-decoration-color from a token keyword, literal CSS string, or opacity color."
  [color-var]
  (color/decoration* &env color-var))

(defmacro decoration-thickness
  "text-decoration-thickness: <value>;"
  [value]
  (typography/decoration-thickness* &env value))

(defmacro underline-offset
  "text-underline-offset: <value>;"
  [value]
  (typography/underline-offset* &env value))

(defmacro tracking
  "letter-spacing: <value>;"
  [value]
  (typography/tracking* &env value))

(defmacro indent
  "text-indent: <value>;"
  [value]
  (typography/indent* &env value))

(defmacro gap
  "gap: <value>;"
  [value]
  (spacing/gap* &env "gap" :gap value))

(defmacro gap-x
  "column-gap: <value>;"
  [value]
  (spacing/gap* &env "gap-x" :column-gap value))

(defmacro gap-y
  "row-gap: <value>;"
  [value]
  (spacing/gap* &env "gap-y" :row-gap value))

(defmacro space-x
  "Space children horizontally: margin-inline-end: <value> on every child except
   the last, with zero specificity so a child's own margin wins."
  [value]
  (modifier/modifier* &env {:prefix "space-x" :selector ":where(& > :not(:last-child))"}
                      [`(me ~value)]))

(defmacro space-y
  "Space children vertically: margin-bottom: <value> on every child except the
   last, with zero specificity so a child's own margin wins."
  [value]
  (modifier/modifier* &env {:prefix "space-y" :selector ":where(& > :not(:last-child))"}
                      [`(mb ~value)]))

(defmacro divide-x
  "Divide children with a right border on every child except the last. Takes
   the same arguments as border."
  [& args]
  (modifier/modifier* &env {:prefix "divide-x" :selector ":where(& > :not(:last-child))"}
                      [`(border-r ~@args)]))

(defmacro divide-y
  "Divide children with a bottom border on every child except the last. Takes
   the same arguments as border."
  [& args]
  (modifier/modifier* &env {:prefix "divide-y" :selector ":where(& > :not(:last-child))"}
                      [`(border-b ~@args)]))

(defmacro basis
  "flex-basis: <value>;"
  [value]
  (spacing/dimension* &env "basis" [:flex-basis] value))

(defmacro basis-auto
  "flex-basis: auto;"
  []
  (spacing/static* &env :basis-auto))

(defmacro basis-full
  "flex-basis: 100%;"
  []
  (spacing/static* &env :basis-full))

(defmacro min-h
  "min-height: <value>;"
  [value]
  (spacing/dimension* &env "min-h" [:min-height] value))

(defmacro min-h-auto
  "min-height: auto;"
  []
  (spacing/static* &env :min-h-auto))

(defmacro min-h-full
  "min-height: 100%;"
  []
  (spacing/static* &env :min-h-full))

(defmacro min-h-min
  "min-height: min-content;"
  []
  (spacing/static* &env :min-h-min))

(defmacro min-h-max
  "min-height: max-content;"
  []
  (spacing/static* &env :min-h-max))

(defmacro min-h-fit
  "min-height: fit-content;"
  []
  (spacing/static* &env :min-h-fit))

(defmacro min-w
  "min-width: <value>;"
  [value]
  (spacing/dimension* &env "min-w" [:min-width] value))

(defmacro min-w-auto
  "min-width: auto;"
  []
  (spacing/static* &env :min-w-auto))

(defmacro min-w-full
  "min-width: 100%;"
  []
  (spacing/static* &env :min-w-full))

(defmacro min-w-min
  "min-width: min-content;"
  []
  (spacing/static* &env :min-w-min))

(defmacro min-w-max
  "min-width: max-content;"
  []
  (spacing/static* &env :min-w-max))

(defmacro min-w-fit
  "min-width: fit-content;"
  []
  (spacing/static* &env :min-w-fit))

(defmacro max-h
  "max-height: <value>;"
  [value]
  (spacing/dimension* &env "max-h" [:max-height] value))

(defmacro max-h-auto
  "max-height: auto;"
  []
  (spacing/static* &env :max-h-auto))

(defmacro max-h-full
  "max-height: 100%;"
  []
  (spacing/static* &env :max-h-full))

(defmacro max-h-min
  "max-height: min-content;"
  []
  (spacing/static* &env :max-h-min))

(defmacro max-h-max
  "max-height: max-content;"
  []
  (spacing/static* &env :max-h-max))

(defmacro max-h-fit
  "max-height: fit-content;"
  []
  (spacing/static* &env :max-h-fit))

(defmacro max-h-none
  "max-height: none;"
  []
  (spacing/static* &env :max-h-none))

(defmacro max-w
  "max-width: <value>;"
  [value]
  (spacing/dimension* &env "max-w" [:max-width] value))

(defmacro max-w-auto
  "max-width: auto;"
  []
  (spacing/static* &env :max-w-auto))

(defmacro max-w-full
  "max-width: 100%;"
  []
  (spacing/static* &env :max-w-full))

(defmacro max-w-min
  "max-width: min-content;"
  []
  (spacing/static* &env :max-w-min))

(defmacro max-w-max
  "max-width: max-content;"
  []
  (spacing/static* &env :max-w-max))

(defmacro max-w-fit
  "max-width: fit-content;"
  []
  (spacing/static* &env :max-w-fit))

(defmacro max-w-none
  "max-width: none;"
  []
  (spacing/static* &env :max-w-none))

(defmacro w
  "width: <value>;"
  [value]
  (spacing/dimension* &env "w" [:width] value))

(defmacro w-auto
  "width: auto;"
  []
  (spacing/static* &env :w-auto))

(defmacro w-full
  "width: 100%;"
  []
  (spacing/static* &env :w-full))

(defmacro w-min
  "width: min-content;"
  []
  (spacing/static* &env :w-min))

(defmacro w-max
  "width: max-content;"
  []
  (spacing/static* &env :w-max))

(defmacro w-fit
  "width: fit-content;"
  []
  (spacing/static* &env :w-fit))

(defmacro h
  "height: <value>;"
  [value]
  (spacing/dimension* &env "h" [:height] value))

(defmacro h-auto
  "height: auto;"
  []
  (spacing/static* &env :h-auto))

(defmacro h-full
  "height: 100%;"
  []
  (spacing/static* &env :h-full))

(defmacro h-min
  "height: min-content;"
  []
  (spacing/static* &env :h-min))

(defmacro h-max
  "height: max-content;"
  []
  (spacing/static* &env :h-max))

(defmacro h-fit
  "height: fit-content;"
  []
  (spacing/static* &env :h-fit))

(defmacro size
  "width: <value>; height: <value>;"
  [value]
  (spacing/dimension* &env "size" [:width :height] value))

(defmacro size-auto
  "width: auto; height: auto;"
  []
  (spacing/static* &env :size-auto))

(defmacro size-full
  "width: 100%; height: 100%;"
  []
  (spacing/static* &env :size-full))

(defmacro size-min
  "width: min-content; height: min-content;"
  []
  (spacing/static* &env :size-min))

(defmacro size-max
  "width: max-content; height: max-content;"
  []
  (spacing/static* &env :size-max))

(defmacro size-fit
  "width: fit-content; height: fit-content;"
  []
  (spacing/static* &env :size-fit))

(defmacro float-right
  "float: right;"
  []
  (layout/static* &env :float-right))

(defmacro float-left
  "float: left;"
  []
  (layout/static* &env :float-left))

(defmacro float-start
  "float: inline-start;"
  []
  (layout/static* &env :float-start))

(defmacro float-end
  "float: inline-end;"
  []
  (layout/static* &env :float-end))

(defmacro float-none
  "float: none;"
  []
  (layout/static* &env :float-none))

(defmacro isolate
  "isolation: isolate;"
  []
  (layout/static* &env :isolate))

(defmacro isolation-auto
  "isolation: auto;"
  []
  (layout/static* &env :isolation-auto))

(defmacro break-inside-auto
  "break-inside: auto;"
  []
  (layout/static* &env :break-inside-auto))

(defmacro break-inside-avoid
  "break-inside: avoid;"
  []
  (layout/static* &env :break-inside-avoid))

(defmacro break-inside-avoid-page
  "break-inside: avoid-page;"
  []
  (layout/static* &env :break-inside-avoid-page))

(defmacro break-inside-avoid-column
  "break-inside: avoid-column;"
  []
  (layout/static* &env :break-inside-avoid-column))

(defmacro scheme-normal
  "color-scheme: normal;"
  []
  (layout/static* &env :scheme-normal))

(defmacro scheme-dark
  "color-scheme: dark;"
  []
  (layout/static* &env :scheme-dark))

(defmacro scheme-light
  "color-scheme: light;"
  []
  (layout/static* &env :scheme-light))

(defmacro scheme-light-dark
  "color-scheme: light dark;"
  []
  (layout/static* &env :scheme-light-dark))

(defmacro scheme-only-dark
  "color-scheme: only dark;"
  []
  (layout/static* &env :scheme-only-dark))

(defmacro scheme-only-light
  "color-scheme: only light;"
  []
  (layout/static* &env :scheme-only-light))

(defmacro line-clamp
  "Clamp text to a number of lines: overflow: hidden; display: -webkit-box;
   -webkit-box-orient: vertical; -webkit-line-clamp: <value>;"
  [value]
  (layout/line-clamp* &env value))

(defmacro line-clamp-none
  "overflow: visible; display: block; -webkit-box-orient: horizontal; -webkit-line-clamp: unset;"
  []
  (layout/static* &env :line-clamp-none))

(defmacro sr-only
  "Hide an element visually but keep it available to screen readers."
  []
  (layout/static* &env :sr-only))

(defmacro not-sr-only
  "Undo sr-only."
  []
  (layout/static* &env :not-sr-only))

(defmacro columns
  "columns: <value>; a number of columns, or a CSS column width."
  [value]
  (layout/columns* &env value))

(defmacro aspect-ratio
  "aspect-ratio: <value>;"
  [value]
  (layout/aspect-ratio* &env value))

(defmacro object-contain
  "object-fit: contain;"
  []
  (layout/static* &env :object-contain))

(defmacro object-cover
  "object-fit: cover;"
  []
  (layout/static* &env :object-cover))

(defmacro object-fill
  "object-fit: fill;"
  []
  (layout/static* &env :object-fill))

(defmacro object-none
  "object-fit: none;"
  []
  (layout/static* &env :object-none))

(defmacro object-scale-down
  "object-fit: scale-down;"
  []
  (layout/static* &env :object-scale-down))

(defmacro m
  "margin: <value>;"
  [value]
  (spacing/margin* &env "m" :margin value))

(defmacro mx
  "margin-inline: <value>;"
  [value]
  (spacing/margin* &env "mx" :margin-inline value))

(defmacro my
  "margin-block: <value>;"
  [value]
  (spacing/margin* &env "my" :margin-block value))

(defmacro mt
  "margin-top: <value>;"
  [value]
  (spacing/margin* &env "mt" :margin-top value))

(defmacro mb
  "margin-bottom: <value>;"
  [value]
  (spacing/margin* &env "mb" :margin-bottom value))

(defmacro mr
  "margin-right: <value>;"
  [value]
  (spacing/margin* &env "mr" :margin-right value))

(defmacro ml
  "margin-left: <value>;"
  [value]
  (spacing/margin* &env "ml" :margin-left value))

(defmacro ms
  "margin-inline-start: <value>;"
  [value]
  (spacing/margin* &env "ms" :margin-inline-start value))

(defmacro me
  "margin-inline-end: <value>;"
  [value]
  (spacing/margin* &env "me" :margin-inline-end value))

(defmacro scroll-m
  "scroll-margin: <value>;"
  [value]
  (spacing/scroll-margin* &env "scroll-m" :scroll-margin value))

(defmacro scroll-mx
  "scroll-margin-inline: <value>;"
  [value]
  (spacing/scroll-margin* &env "scroll-mx" :scroll-margin-inline value))

(defmacro scroll-my
  "scroll-margin-block: <value>;"
  [value]
  (spacing/scroll-margin* &env "scroll-my" :scroll-margin-block value))

(defmacro scroll-mt
  "scroll-margin-top: <value>;"
  [value]
  (spacing/scroll-margin* &env "scroll-mt" :scroll-margin-top value))

(defmacro scroll-mr
  "scroll-margin-right: <value>;"
  [value]
  (spacing/scroll-margin* &env "scroll-mr" :scroll-margin-right value))

(defmacro scroll-mb
  "scroll-margin-bottom: <value>;"
  [value]
  (spacing/scroll-margin* &env "scroll-mb" :scroll-margin-bottom value))

(defmacro scroll-ml
  "scroll-margin-left: <value>;"
  [value]
  (spacing/scroll-margin* &env "scroll-ml" :scroll-margin-left value))

(defmacro m-auto
  "margin: auto;"
  []
  (spacing/static* &env :m-auto))

(defmacro mx-auto
  "margin-inline: auto;"
  []
  (spacing/static* &env :mx-auto))

(defmacro my-auto
  "margin-block: auto;"
  []
  (spacing/static* &env :my-auto))

(defmacro mt-auto
  "margin-top: auto;"
  []
  (spacing/static* &env :mt-auto))

(defmacro mb-auto
  "margin-bottom: auto;"
  []
  (spacing/static* &env :mb-auto))

(defmacro mr-auto
  "margin-right: auto;"
  []
  (spacing/static* &env :mr-auto))

(defmacro ml-auto
  "margin-left: auto;"
  []
  (spacing/static* &env :ml-auto))

(defmacro p
  "padding: <value>;"
  [value]
  (spacing/padding* &env "p" :padding value))

(defmacro px
  "padding-inline: <value>;"
  [value]
  (spacing/padding* &env "px" :padding-inline value))

(defmacro py
  "padding-block: <value>;"
  [value]
  (spacing/padding* &env "py" :padding-block value))

(defmacro pt
  "padding-top: <value>;"
  [value]
  (spacing/padding* &env "pt" :padding-top value))

(defmacro pb
  "padding-bottom: <value>;"
  [value]
  (spacing/padding* &env "pb" :padding-bottom value))

(defmacro pr
  "padding-right: <value>;"
  [value]
  (spacing/padding* &env "pr" :padding-right value))

(defmacro pl
  "padding-left: <value>;"
  [value]
  (spacing/padding* &env "pl" :padding-left value))

(defmacro ps
  "padding-inline-start: <value>;"
  [value]
  (spacing/padding* &env "ps" :padding-inline-start value))

(defmacro pe
  "padding-inline-end: <value>;"
  [value]
  (spacing/padding* &env "pe" :padding-inline-end value))

(defmacro scroll-p
  "scroll-padding: <value>;"
  [value]
  (spacing/scroll-padding* &env "scroll-p" :scroll-padding value))

(defmacro scroll-px
  "scroll-padding-inline: <value>;"
  [value]
  (spacing/scroll-padding* &env "scroll-px" :scroll-padding-inline value))

(defmacro scroll-py
  "scroll-padding-block: <value>;"
  [value]
  (spacing/scroll-padding* &env "scroll-py" :scroll-padding-block value))

(defmacro scroll-pt
  "scroll-padding-top: <value>;"
  [value]
  (spacing/scroll-padding* &env "scroll-pt" :scroll-padding-top value))

(defmacro scroll-pr
  "scroll-padding-right: <value>;"
  [value]
  (spacing/scroll-padding* &env "scroll-pr" :scroll-padding-right value))

(defmacro scroll-pb
  "scroll-padding-bottom: <value>;"
  [value]
  (spacing/scroll-padding* &env "scroll-pb" :scroll-padding-bottom value))

(defmacro scroll-pl
  "scroll-padding-left: <value>;"
  [value]
  (spacing/scroll-padding* &env "scroll-pl" :scroll-padding-left value))

(defmacro clip-path
  "clip-path: <value>;"
  [value]
  (position/clip-path* &env value))

(defmacro overflow-auto
  "overflow: auto;"
  []
  (position/static* &env :overflow-auto))

(defmacro overflow-hidden
  "overflow: hidden;"
  []
  (position/static* &env :overflow-hidden))

(defmacro overflow-clip
  "overflow: clip;"
  []
  (position/static* &env :overflow-clip))

(defmacro overflow-visible
  "overflow: visible;"
  []
  (position/static* &env :overflow-visible))

(defmacro overflow-scroll
  "overflow: scroll;"
  []
  (position/static* &env :overflow-scroll))

(defmacro overflow-x-auto
  "overflow-x: auto;"
  []
  (position/static* &env :overflow-x-auto))

(defmacro overflow-x-hidden
  "overflow-x: hidden;"
  []
  (position/static* &env :overflow-x-hidden))

(defmacro overflow-x-clip
  "overflow-x: clip;"
  []
  (position/static* &env :overflow-x-clip))

(defmacro overflow-x-visible
  "overflow-x: visible;"
  []
  (position/static* &env :overflow-x-visible))

(defmacro overflow-x-scroll
  "overflow-x: scroll;"
  []
  (position/static* &env :overflow-x-scroll))

(defmacro overflow-y-auto
  "overflow-y: auto;"
  []
  (position/static* &env :overflow-y-auto))

(defmacro overflow-y-hidden
  "overflow-y: hidden;"
  []
  (position/static* &env :overflow-y-hidden))

(defmacro overflow-y-clip
  "overflow-y: clip;"
  []
  (position/static* &env :overflow-y-clip))

(defmacro overflow-y-visible
  "overflow-y: visible;"
  []
  (position/static* &env :overflow-y-visible))

(defmacro overflow-y-scroll
  "overflow-y: scroll;"
  []
  (position/static* &env :overflow-y-scroll))

(defmacro overscroll-auto
  "overscroll-behavior: auto;"
  []
  (position/static* &env :overscroll-auto))

(defmacro overscroll-contain
  "overscroll-behavior: contain;"
  []
  (position/static* &env :overscroll-contain))

(defmacro overscroll-none
  "overscroll-behavior: none;"
  []
  (position/static* &env :overscroll-none))

(defmacro overscroll-x-auto
  "overscroll-behavior-x: auto;"
  []
  (position/static* &env :overscroll-x-auto))

(defmacro overscroll-x-contain
  "overscroll-behavior-x: contain;"
  []
  (position/static* &env :overscroll-x-contain))

(defmacro overscroll-x-none
  "overscroll-behavior-x: none;"
  []
  (position/static* &env :overscroll-x-none))

(defmacro overscroll-y-auto
  "overscroll-behavior-y: auto;"
  []
  (position/static* &env :overscroll-y-auto))

(defmacro overscroll-y-contain
  "overscroll-behavior-y: contain;"
  []
  (position/static* &env :overscroll-y-contain))

(defmacro overscroll-y-none
  "overscroll-behavior-y: none;"
  []
  (position/static* &env :overscroll-y-none))

(defmacro snap-none
  "scroll-snap-type: none;"
  []
  (position/static* &env :snap-none))

(defmacro snap-x
  "scroll-snap-type: x var(--us-scroll-snap-strictness, proximity);"
  []
  (position/static* &env :snap-x))

(defmacro snap-y
  "scroll-snap-type: y var(--us-scroll-snap-strictness, proximity);"
  []
  (position/static* &env :snap-y))

(defmacro snap-both
  "scroll-snap-type: both var(--us-scroll-snap-strictness, proximity);"
  []
  (position/static* &env :snap-both))

(defmacro snap-mandatory
  "--us-scroll-snap-strictness: mandatory;"
  []
  (position/static* &env :snap-mandatory))

(defmacro snap-proximity
  "--us-scroll-snap-strictness: proximity;"
  []
  (position/static* &env :snap-proximity))

(defmacro snap-start
  "scroll-snap-align: start;"
  []
  (position/static* &env :snap-start))

(defmacro snap-end
  "scroll-snap-align: end;"
  []
  (position/static* &env :snap-end))

(defmacro snap-center
  "scroll-snap-align: center;"
  []
  (position/static* &env :snap-center))

(defmacro snap-align-none
  "scroll-snap-align: none;"
  []
  (position/static* &env :snap-align-none))

(defmacro scrollbar-thumb-inherit
  "Use an inherited scrollbar thumb color."
  []
  (scrollbar/static* &env :scrollbar-thumb-inherit))

(defmacro scrollbar-thumb-current
  "Use currentColor for the scrollbar thumb."
  []
  (scrollbar/static* &env :scrollbar-thumb-current))

(defmacro scrollbar-thumb-transparent
  "Use a transparent scrollbar thumb."
  []
  (scrollbar/static* &env :scrollbar-thumb-transparent))

(defmacro scrollbar-thumb
  "Set the scrollbar thumb color from a token keyword or CSS string."
  [color]
  (scrollbar/thumb* &env color))

(defmacro scrollbar-track-inherit
  "Use an inherited scrollbar track color."
  []
  (scrollbar/static* &env :scrollbar-track-inherit))

(defmacro scrollbar-track-current
  "Use currentColor for the scrollbar track."
  []
  (scrollbar/static* &env :scrollbar-track-current))

(defmacro scrollbar-track-transparent
  "Use a transparent scrollbar track."
  []
  (scrollbar/static* &env :scrollbar-track-transparent))

(defmacro scrollbar-track
  "Set the scrollbar track color from a token keyword or CSS string."
  [color]
  (scrollbar/track* &env color))

(defmacro scrollbar-auto
  "scrollbar-width: auto;"
  []
  (scrollbar/static* &env :scrollbar-auto))

(defmacro scrollbar-thin
  "scrollbar-width: thin;"
  []
  (scrollbar/static* &env :scrollbar-thin))

(defmacro scrollbar-none
  "scrollbar-width: none;"
  []
  (scrollbar/static* &env :scrollbar-none))

(defmacro scrollbar-gutter-auto
  "scrollbar-gutter: auto;"
  []
  (scrollbar/static* &env :scrollbar-gutter-auto))

(defmacro scrollbar-gutter-stable
  "scrollbar-gutter: stable;"
  []
  (scrollbar/static* &env :scrollbar-gutter-stable))

(defmacro scrollbar-gutter-both
  "scrollbar-gutter: stable both-edges;"
  []
  (scrollbar/static* &env :scrollbar-gutter-both))

(defmacro static
  "position: static;"
  []
  (position/static* &env :static))

(defmacro fixed
  "position: fixed;"
  []
  (position/static* &env :fixed))

(defmacro absolute
  "position: absolute;"
  []
  (position/static* &env :absolute))

(defmacro relative
  "position: relative;"
  []
  (position/static* &env :relative))

(defmacro sticky
  "position: sticky;"
  []
  (position/static* &env :sticky))

(defmacro inset
  "inset: <value>;"
  [value]
  (position/inset* &env "inset" :inset value))

(defmacro inset-x
  "inset-inline: <value>;"
  [value]
  (position/inset* &env "inset-x" :inset-inline value))

(defmacro inset-y
  "inset-block: <value>;"
  [value]
  (position/inset* &env "inset-y" :inset-block value))

(defmacro top
  "top: <value>;"
  [value]
  (position/inset* &env "top" :top value))

(defmacro right
  "right: <value>;"
  [value]
  (position/inset* &env "right" :right value))

(defmacro bottom
  "bottom: <value>;"
  [value]
  (position/inset* &env "bottom" :bottom value))

(defmacro left
  "left: <value>;"
  [value]
  (position/inset* &env "left" :left value))

(defmacro inset-auto
  "inset: auto;"
  []
  (position/static* &env :inset-auto))

(defmacro inset-x-auto
  "inset-inline: auto;"
  []
  (position/static* &env :inset-x-auto))

(defmacro inset-y-auto
  "inset-block: auto;"
  []
  (position/static* &env :inset-y-auto))

(defmacro top-auto
  "top: auto;"
  []
  (position/static* &env :top-auto))

(defmacro right-auto
  "right: auto;"
  []
  (position/static* &env :right-auto))

(defmacro bottom-auto
  "bottom: auto;"
  []
  (position/static* &env :bottom-auto))

(defmacro left-auto
  "left: auto;"
  []
  (position/static* &env :left-auto))

(defmacro z
  "z-index: <integer>;"
  [value]
  (position/z* &env value))

(defmacro z-auto
  "z-index: auto;"
  []
  (position/static* &env :z-auto))

(defmacro pointer-events-none
  "pointer-events: none;"
  []
  (interaction/static* &env :pointer-events-none))

(defmacro pointer-events-auto
  "pointer-events: auto;"
  []
  (interaction/static* &env :pointer-events-auto))

(defmacro will-change-auto
  "will-change: auto;"
  []
  (interaction/static* &env :will-change-auto))

(defmacro will-change-scroll
  "will-change: scroll-position;"
  []
  (interaction/static* &env :will-change-scroll))

(defmacro will-change-contents
  "will-change: contents;"
  []
  (interaction/static* &env :will-change-contents))

(defmacro will-change-transform
  "will-change: transform;"
  []
  (interaction/static* &env :will-change-transform))

(defmacro will-change
  "Set will-change from a token keyword or literal CSS string."
  [value]
  (interaction/will-change* &env value))

(defmacro touch-auto
  "touch-action: auto;"
  []
  (interaction/static* &env :touch-auto))

(defmacro touch-none
  "touch-action: none;"
  []
  (interaction/static* &env :touch-none))

(defmacro touch-pan-x
  "touch-action: pan-x;"
  []
  (interaction/static* &env :touch-pan-x))

(defmacro touch-pan-left
  "touch-action: pan-left;"
  []
  (interaction/static* &env :touch-pan-left))

(defmacro touch-pan-right
  "touch-action: pan-right;"
  []
  (interaction/static* &env :touch-pan-right))

(defmacro touch-pan-y
  "touch-action: pan-y;"
  []
  (interaction/static* &env :touch-pan-y))

(defmacro touch-pan-up
  "touch-action: pan-up;"
  []
  (interaction/static* &env :touch-pan-up))

(defmacro touch-pan-down
  "touch-action: pan-down;"
  []
  (interaction/static* &env :touch-pan-down))

(defmacro touch-pinch-zoom
  "touch-action: pinch-zoom;"
  []
  (interaction/static* &env :touch-pinch-zoom))

(defmacro touch-manipulation
  "touch-action: manipulation;"
  []
  (interaction/static* &env :touch-manipulation))

(defmacro cursor-auto
  "cursor: auto;"
  []
  (interaction/static* &env :cursor-auto))

(defmacro cursor-default
  "cursor: default;"
  []
  (interaction/static* &env :cursor-default))

(defmacro cursor-pointer
  "cursor: pointer;"
  []
  (interaction/static* &env :cursor-pointer))

(defmacro cursor-wait
  "cursor: wait;"
  []
  (interaction/static* &env :cursor-wait))

(defmacro cursor-text
  "cursor: text;"
  []
  (interaction/static* &env :cursor-text))

(defmacro cursor-move
  "cursor: move;"
  []
  (interaction/static* &env :cursor-move))

(defmacro cursor-help
  "cursor: help;"
  []
  (interaction/static* &env :cursor-help))

(defmacro cursor-not-allowed
  "cursor: not-allowed;"
  []
  (interaction/static* &env :cursor-not-allowed))

(defmacro cursor-none
  "cursor: none;"
  []
  (interaction/static* &env :cursor-none))

(defmacro cursor-context-menu
  "cursor: context-menu;"
  []
  (interaction/static* &env :cursor-context-menu))

(defmacro cursor-progress
  "cursor: progress;"
  []
  (interaction/static* &env :cursor-progress))

(defmacro cursor-cell
  "cursor: cell;"
  []
  (interaction/static* &env :cursor-cell))

(defmacro cursor-crosshair
  "cursor: crosshair;"
  []
  (interaction/static* &env :cursor-crosshair))

(defmacro cursor-vertical-text
  "cursor: vertical-text;"
  []
  (interaction/static* &env :cursor-vertical-text))

(defmacro cursor-alias
  "cursor: alias;"
  []
  (interaction/static* &env :cursor-alias))

(defmacro cursor-copy
  "cursor: copy;"
  []
  (interaction/static* &env :cursor-copy))

(defmacro cursor-no-drop
  "cursor: no-drop;"
  []
  (interaction/static* &env :cursor-no-drop))

(defmacro cursor-grab
  "cursor: grab;"
  []
  (interaction/static* &env :cursor-grab))

(defmacro cursor-grabbing
  "cursor: grabbing;"
  []
  (interaction/static* &env :cursor-grabbing))

(defmacro cursor-all-scroll
  "cursor: all-scroll;"
  []
  (interaction/static* &env :cursor-all-scroll))

(defmacro cursor-col-resize
  "cursor: col-resize;"
  []
  (interaction/static* &env :cursor-col-resize))

(defmacro cursor-row-resize
  "cursor: row-resize;"
  []
  (interaction/static* &env :cursor-row-resize))

(defmacro cursor-n-resize
  "cursor: n-resize;"
  []
  (interaction/static* &env :cursor-n-resize))

(defmacro cursor-e-resize
  "cursor: e-resize;"
  []
  (interaction/static* &env :cursor-e-resize))

(defmacro cursor-s-resize
  "cursor: s-resize;"
  []
  (interaction/static* &env :cursor-s-resize))

(defmacro cursor-w-resize
  "cursor: w-resize;"
  []
  (interaction/static* &env :cursor-w-resize))

(defmacro cursor-ne-resize
  "cursor: ne-resize;"
  []
  (interaction/static* &env :cursor-ne-resize))

(defmacro cursor-nw-resize
  "cursor: nw-resize;"
  []
  (interaction/static* &env :cursor-nw-resize))

(defmacro cursor-se-resize
  "cursor: se-resize;"
  []
  (interaction/static* &env :cursor-se-resize))

(defmacro cursor-sw-resize
  "cursor: sw-resize;"
  []
  (interaction/static* &env :cursor-sw-resize))

(defmacro cursor-ew-resize
  "cursor: ew-resize;"
  []
  (interaction/static* &env :cursor-ew-resize))

(defmacro cursor-ns-resize
  "cursor: ns-resize;"
  []
  (interaction/static* &env :cursor-ns-resize))

(defmacro cursor-nesw-resize
  "cursor: nesw-resize;"
  []
  (interaction/static* &env :cursor-nesw-resize))

(defmacro cursor-nwse-resize
  "cursor: nwse-resize;"
  []
  (interaction/static* &env :cursor-nwse-resize))

(defmacro cursor-zoom-in
  "cursor: zoom-in;"
  []
  (interaction/static* &env :cursor-zoom-in))

(defmacro cursor-zoom-out
  "cursor: zoom-out;"
  []
  (interaction/static* &env :cursor-zoom-out))

(defmacro cursor
  "Set cursor from a token keyword or literal CSS string."
  [value]
  (interaction/cursor* &env value))

(defmacro resize-none
  "resize: none;"
  []
  (interaction/static* &env :resize-none))

(defmacro resize-y
  "resize: vertical;"
  []
  (interaction/static* &env :resize-y))

(defmacro resize-x
  "resize: horizontal;"
  []
  (interaction/static* &env :resize-x))

(defmacro resize
  "resize: both;"
  []
  (interaction/static* &env :resize))

(defmacro appearance-none
  "appearance: none;"
  []
  (interaction/static* &env :appearance-none))

(defmacro appearance-auto
  "appearance: auto;"
  []
  (interaction/static* &env :appearance-auto))

(defmacro declare-layer-order
  "Register the predefined low-to-high cascade-layer order."
  []
  (layer/declare-layer-order* &env))

(defmacro declare-properties
  "Register the non-inheriting @property rules behind composed utilities such
   as the box-shadow layers."
  []
  (layer/declare-properties* &env))

(defmacro base
  "Apply utilities in the base cascade layer, above theme variables."
  [& forms]
  (layer/layer* &env :base forms))

(defmacro components
  "Apply utilities in the components cascade layer."
  [& forms]
  (layer/layer* &env :components forms))

(defmacro variants
  "Apply utilities in the highest-priority variants cascade layer."
  [& forms]
  (layer/layer* &env :variants forms))

(defmacro sel-class
  "Apply utilities to descendants with the class, a keyword or string."
  [class-key & forms]
  (modifier/sel-class* &env class-key forms))

(defmacro ancestor
  "Apply utilities when the element is inside an element with the class, a
   keyword or string."
  [class-key & forms]
  (modifier/ancestor* &env class-key forms))

(defmacro self-or-ancestor
  "Apply utilities when the element has, or is inside an element with, the class,
   a keyword or string."
  [class-key & forms]
  (modifier/self-or-ancestor* &env class-key forms))

(defmacro root-theme
  "Apply utilities when the root element has the given `def-theme` class, regardless
   of theme classes nested in between."
  [theme & forms]
  (modifier/root-theme* &env theme forms))

(defmacro ancestor-hover
  "Apply utilities when the class ancestor is hovered."
  [class-key & forms]
  (modifier/ancestor-modifier* &env class-key
                               {:prefix "hover" :selector "&:hover"} forms))

(defmacro ancestor-focus
  "Apply utilities when the class ancestor has focus."
  [class-key & forms]
  (modifier/ancestor-modifier* &env class-key
                               {:prefix "focus" :selector "&:focus"} forms))

(defmacro ancestor-focus-within
  "Apply utilities when the class ancestor has focus within."
  [class-key & forms]
  (modifier/ancestor-modifier* &env class-key
                               {:prefix "focus-within" :selector "&:focus-within"} forms))

(defmacro ancestor-focus-visible
  "Apply utilities when the class ancestor has visible focus."
  [class-key & forms]
  (modifier/ancestor-modifier* &env class-key
                               {:prefix "focus-visible" :selector "&:focus-visible"} forms))

(defmacro ancestor-active
  "Apply utilities when the class ancestor is active."
  [class-key & forms]
  (modifier/ancestor-modifier* &env class-key
                               {:prefix "active" :selector "&:active"} forms))

(defmacro ancestor-disabled
  "Apply utilities when the class ancestor is disabled."
  [class-key & forms]
  (modifier/ancestor-modifier* &env class-key
                               {:prefix "disabled" :selector "&:disabled"} forms))

(defmacro ancestor-not-disabled
  "Apply utilities when the class ancestor is not disabled."
  [class-key & forms]
  (modifier/ancestor-modifier* &env class-key
                               {:prefix "not-disabled" :selector "&:not(:disabled)"} forms))

(defmacro ancestor-has
  "Apply utilities when the class ancestor has a matching descendant."
  [class-key selector & forms]
  (modifier/ancestor-has* &env class-key selector forms))

(defmacro ancestor-attr
  "Apply utilities when the class ancestor has an attribute."
  [class-key attribute-name & forms]
  (modifier/ancestor-attr* &env class-key attribute-name forms))

(defmacro ancestor-attr=
  "Apply utilities when an ancestor attribute equals a compile-time value."
  [class-key attribute-name attribute-value & forms]
  (modifier/ancestor-attr=* &env class-key attribute-name attribute-value forms))

(defmacro group
  "With no arguments, return the \"group\" class; with rules, apply
   `(ancestor :group ...)`."
  [& forms]
  (if (seq forms)
    (modifier/ancestor* &env :group forms)
    (modifier/class* &env :group)))

(defmacro group-hover
  "Apply utilities when the group ancestor is hovered."
  [& forms]
  (modifier/ancestor-modifier* &env :group
                               {:prefix "hover" :selector "&:hover"} forms))

(defmacro group-focus
  "Apply utilities when the group ancestor has focus."
  [& forms]
  (modifier/ancestor-modifier* &env :group
                               {:prefix "focus" :selector "&:focus"} forms))

(defmacro group-focus-visible
  "Apply utilities when the group ancestor has visible focus."
  [& forms]
  (modifier/ancestor-modifier* &env :group
                               {:prefix "focus-visible" :selector "&:focus-visible"} forms))

(defmacro group-active
  "Apply utilities when the group ancestor is active."
  [& forms]
  (modifier/ancestor-modifier* &env :group
                               {:prefix "active" :selector "&:active"} forms))

(defmacro group-disabled
  "Apply utilities when the group ancestor is disabled."
  [& forms]
  (modifier/ancestor-modifier* &env :group
                               {:prefix "disabled" :selector "&:disabled"} forms))

(defmacro group-not-disabled
  "Apply utilities when the group ancestor is not disabled."
  [& forms]
  (modifier/ancestor-modifier* &env :group
                               {:prefix "not-disabled" :selector "&:not(:disabled)"} forms))

(defmacro group-attr
  "Apply utilities when the group ancestor has an attribute."
  [attribute-name & forms]
  (modifier/ancestor-attr* &env :group attribute-name forms))

(defmacro group-attr=
  "Apply utilities when a group ancestor attribute equals a compile-time value."
  [attribute-name attribute-value & forms]
  (modifier/ancestor-attr=* &env :group attribute-name attribute-value forms))

(defmacro peer
  "Return the \"peer\" class read by peer sibling-state modifiers."
  []
  (modifier/peer* &env))

(defmacro peer-hover
  "Apply utilities when a preceding peer sibling is hovered."
  [& forms]
  (modifier/peer-pseudo* &env "hover" "hover" forms))

(defmacro peer-focus
  "Apply utilities when a preceding peer sibling has focus."
  [& forms]
  (modifier/peer-pseudo* &env "focus" "focus" forms))

(defmacro peer-focus-within
  "Apply utilities when a preceding peer sibling has focus within."
  [& forms]
  (modifier/peer-pseudo* &env "focus-within" "focus-within" forms))

(defmacro peer-focus-visible
  "Apply utilities when a preceding peer sibling has visible focus."
  [& forms]
  (modifier/peer-pseudo* &env "focus-visible" "focus-visible" forms))

(defmacro peer-active
  "Apply utilities when a preceding peer sibling is active."
  [& forms]
  (modifier/peer-pseudo* &env "active" "active" forms))

(defmacro peer-visited
  "Apply utilities when a preceding peer sibling is a visited link."
  [& forms]
  (modifier/peer-pseudo* &env "visited" "visited" forms))

(defmacro peer-target
  "Apply utilities when a preceding peer sibling is the target."
  [& forms]
  (modifier/peer-pseudo* &env "target" "target" forms))

(defmacro peer-first
  "Apply utilities when a preceding peer sibling is the first child."
  [& forms]
  (modifier/peer-pseudo* &env "first" "first-child" forms))

(defmacro peer-last
  "Apply utilities when a preceding peer sibling is the last child."
  [& forms]
  (modifier/peer-pseudo* &env "last" "last-child" forms))

(defmacro peer-only
  "Apply utilities when a preceding peer sibling is the only child."
  [& forms]
  (modifier/peer-pseudo* &env "only" "only-child" forms))

(defmacro peer-odd
  "Apply utilities when a preceding peer sibling is an odd child."
  [& forms]
  (modifier/peer-pseudo* &env "odd" "nth-child(odd)" forms))

(defmacro peer-even
  "Apply utilities when a preceding peer sibling is an even child."
  [& forms]
  (modifier/peer-pseudo* &env "even" "nth-child(even)" forms))

(defmacro peer-first-of-type
  "Apply utilities when a preceding peer sibling is first of its type."
  [& forms]
  (modifier/peer-pseudo* &env "first-of-type" "first-of-type" forms))

(defmacro peer-last-of-type
  "Apply utilities when a preceding peer sibling is last of its type."
  [& forms]
  (modifier/peer-pseudo* &env "last-of-type" "last-of-type" forms))

(defmacro peer-only-of-type
  "Apply utilities when a preceding peer sibling is the only one of its type."
  [& forms]
  (modifier/peer-pseudo* &env "only-of-type" "only-of-type" forms))

(defmacro peer-nth
  "Apply utilities when a preceding peer sibling matches :nth-child."
  [value & forms]
  (modifier/peer-indexed-child-pseudo* &env "nth" "nth-child" value forms))

(defmacro peer-nth-last
  "Apply utilities when a preceding peer sibling matches :nth-last-child."
  [value & forms]
  (modifier/peer-indexed-child-pseudo* &env "nth-last" "nth-last-child" value forms))

(defmacro peer-nth-of-type
  "Apply utilities when a preceding peer sibling matches :nth-of-type."
  [value & forms]
  (modifier/peer-indexed-child-pseudo* &env "nth-of-type" "nth-of-type" value forms))

(defmacro peer-nth-last-of-type
  "Apply utilities when a preceding peer sibling matches :nth-last-of-type."
  [value & forms]
  (modifier/peer-indexed-child-pseudo* &env "nth-last-of-type" "nth-last-of-type" value forms))

(defmacro peer-empty
  "Apply utilities when a preceding peer sibling is empty."
  [& forms]
  (modifier/peer-pseudo* &env "empty" "empty" forms))

(defmacro peer-disabled
  "Apply utilities when a preceding peer sibling is disabled."
  [& forms]
  (modifier/peer-pseudo* &env "disabled" "disabled" forms))

(defmacro peer-not-disabled
  "Apply utilities when a preceding peer sibling is not disabled."
  [& forms]
  (modifier/peer-pseudo* &env "not-disabled" "not(:disabled)" forms))

(defmacro peer-enabled
  "Apply utilities when a preceding peer sibling is enabled."
  [& forms]
  (modifier/peer-pseudo* &env "enabled" "enabled" forms))

(defmacro peer-checked
  "Apply utilities when a preceding peer sibling is checked."
  [& forms]
  (modifier/peer-pseudo* &env "checked" "checked" forms))

(defmacro peer-indeterminate
  "Apply utilities when a preceding peer sibling is indeterminate."
  [& forms]
  (modifier/peer-pseudo* &env "indeterminate" "indeterminate" forms))

(defmacro peer-default
  "Apply utilities when a preceding peer sibling is the default."
  [& forms]
  (modifier/peer-pseudo* &env "default" "default" forms))

(defmacro peer-optional
  "Apply utilities when a preceding peer sibling is optional."
  [& forms]
  (modifier/peer-pseudo* &env "optional" "optional" forms))

(defmacro peer-required
  "Apply utilities when a preceding peer sibling is required."
  [& forms]
  (modifier/peer-pseudo* &env "required" "required" forms))

(defmacro peer-valid
  "Apply utilities when a preceding peer sibling is valid."
  [& forms]
  (modifier/peer-pseudo* &env "valid" "valid" forms))

(defmacro peer-invalid
  "Apply utilities when a preceding peer sibling is invalid."
  [& forms]
  (modifier/peer-pseudo* &env "invalid" "invalid" forms))

(defmacro peer-user-valid
  "Apply utilities when a preceding peer sibling is user-valid."
  [& forms]
  (modifier/peer-pseudo* &env "user-valid" "user-valid" forms))

(defmacro peer-user-invalid
  "Apply utilities when a preceding peer sibling is user-invalid."
  [& forms]
  (modifier/peer-pseudo* &env "user-invalid" "user-invalid" forms))

(defmacro peer-in-range
  "Apply utilities when a preceding peer sibling is in range."
  [& forms]
  (modifier/peer-pseudo* &env "in-range" "in-range" forms))

(defmacro peer-out-of-range
  "Apply utilities when a preceding peer sibling is out of range."
  [& forms]
  (modifier/peer-pseudo* &env "out-of-range" "out-of-range" forms))

(defmacro peer-placeholder-shown
  "Apply utilities when a preceding peer sibling shows its placeholder."
  [& forms]
  (modifier/peer-pseudo* &env "placeholder-shown" "placeholder-shown" forms))

(defmacro peer-autofill
  "Apply utilities when a preceding peer sibling is autofilled."
  [& forms]
  (modifier/peer-pseudo* &env "autofill" "autofill" forms))

(defmacro peer-read-only
  "Apply utilities when a preceding peer sibling is read-only."
  [& forms]
  (modifier/peer-pseudo* &env "read-only" "read-only" forms))

(defmacro peer-open
  "Apply utilities when a preceding peer sibling is open."
  [& forms]
  (modifier/peer-pseudo* &env "open" "is([open], :popover-open, :open)" forms))

(defmacro peer-has
  "Apply utilities when a preceding peer sibling has a matching descendant."
  [selector & forms]
  (modifier/peer-has* &env selector forms))

(defmacro peer-attr
  "Apply utilities when a preceding peer sibling has an attribute."
  [attribute-name & forms]
  (modifier/peer-attr* &env attribute-name forms))

(defmacro peer-attr=
  "Apply utilities when a preceding peer sibling attribute equals a compile-time value."
  [attribute-name attribute-value & forms]
  (modifier/peer-attr=* &env attribute-name attribute-value forms))

(defmacro **
  "Apply utilities to every descendant (`*`)."
  [& forms]
  (modifier/modifier* &env {:prefix "**" :selector "& *"} forms))

(defmacro sel
  "Apply utilities to descendant selectors given as a string or vector of strings."
  [selector & forms]
  (modifier/sel* &env selector forms))

(defmacro has
  "Apply utilities when the styled element has a descendant matching the selector."
  [selector & forms]
  (modifier/has* &env selector forms))

(defmacro pseudo
  "Apply utilities to a named pseudo-element, without the leading `::`."
  [name & forms]
  (modifier/pseudo* &env name forms))

(defmacro before
  "Apply utilities to the ::before pseudo-element."
  [& forms]
  (modifier/modifier* &env {:prefix "before" :selector "&::before"} forms))

(defmacro after
  "Apply utilities to the ::after pseudo-element."
  [& forms]
  (modifier/modifier* &env {:prefix "after" :selector "&::after"} forms))

(defmacro first
  "Apply utilities on :first-child."
  [& forms]
  (modifier/child-pseudo* &env "first" "first-child" forms))

(defmacro last
  "Apply utilities on :last-child."
  [& forms]
  (modifier/child-pseudo* &env "last" "last-child" forms))

(defmacro only
  "Apply utilities on :only-child."
  [& forms]
  (modifier/child-pseudo* &env "only" "only-child" forms))

(defmacro odd
  "Apply utilities on :nth-child(odd)."
  [& forms]
  (modifier/child-pseudo* &env "odd" "nth-child(odd)" forms))

(defmacro even
  "Apply utilities on :nth-child(even)."
  [& forms]
  (modifier/child-pseudo* &env "even" "nth-child(even)" forms))

(defmacro nth
  "Apply utilities on :nth-child(<index or expression>)."
  [value & forms]
  (modifier/indexed-child-pseudo* &env "nth" "nth-child" value forms))

(defmacro nth-last
  "Apply utilities on :nth-last-child(<index or expression>)."
  [value & forms]
  (modifier/indexed-child-pseudo* &env "nth-last" "nth-last-child" value forms))

(defmacro nth-of-type
  "Apply utilities on :nth-of-type(<index or expression>)."
  [value & forms]
  (modifier/indexed-child-pseudo* &env "nth-of-type" "nth-of-type" value forms))

(defmacro nth-last-of-type
  "Apply utilities on :nth-last-of-type(<index or expression>)."
  [value & forms]
  (modifier/indexed-child-pseudo* &env "nth-last-of-type" "nth-last-of-type" value forms))

(defmacro empty
  "Apply utilities on :empty."
  [& forms]
  (modifier/child-pseudo* &env "empty" "empty" forms))

(defmacro first-of-type
  "Apply utilities on :first-of-type."
  [& forms]
  (modifier/child-pseudo* &env "first-of-type" "first-of-type" forms))

(defmacro last-of-type
  "Apply utilities on :last-of-type."
  [& forms]
  (modifier/child-pseudo* &env "last-of-type" "last-of-type" forms))

(defmacro only-of-type
  "Apply utilities on :only-of-type."
  [& forms]
  (modifier/child-pseudo* &env "only-of-type" "only-of-type" forms))

(defmacro hover
  "Apply utilities on :hover."
  [& forms]
  (modifier/modifier* &env {:prefix "hover" :selector "&:hover"} forms))

(defmacro focus
  "Apply utilities on :focus."
  [& forms]
  (modifier/modifier* &env {:prefix "focus" :selector "&:focus"} forms))

(defmacro focus-within
  "Apply utilities on :focus-within."
  [& forms]
  (modifier/modifier* &env {:prefix "focus-within" :selector "&:focus-within"} forms))

(defmacro focus-visible
  "Apply utilities on :focus-visible."
  [& forms]
  (modifier/modifier* &env {:prefix "focus-visible" :selector "&:focus-visible"} forms))

(defmacro placeholder
  "Apply utilities to the ::placeholder pseudo-element."
  [& forms]
  (modifier/modifier* &env {:prefix "placeholder" :selector "&::placeholder"} forms))

(defmacro target
  "Apply utilities on :target."
  [& forms]
  (modifier/modifier* &env {:prefix "target" :selector "&:target"} forms))

(defmacro attr
  "Apply utilities when an attribute is present."
  [attribute-name & forms]
  (modifier/attr* &env attribute-name forms))

(defmacro attr=
  "Apply utilities when an attribute equals a compile-time value."
  [attribute-name attribute-value & forms]
  (modifier/attr=* &env attribute-name attribute-value forms))

(defmacro media
  "Apply utilities inside a compile-time media query such as `(resolution < 2dppx)`."
  [query & forms]
  (modifier/media* &env query forms))

(defmacro container
  "Apply utilities inside a compile-time container query such as `(max-width: 300px)`."
  [query & forms]
  (modifier/container* &env query forms))

(defmacro supports
  "Apply utilities inside a compile-time CSS supports condition."
  [condition & forms]
  (modifier/supports* &env condition forms))

(defmacro active
  "Apply utilities on :active."
  [& forms]
  (modifier/modifier* &env {:prefix "active" :selector "&:active"} forms))

(defmacro disabled
  "Apply utilities on :disabled."
  [& forms]
  (modifier/modifier* &env {:prefix "disabled" :selector "&:disabled"} forms))

(defmacro not-disabled
  "Apply utilities when not :disabled."
  [& forms]
  (modifier/modifier* &env {:prefix "not-disabled" :selector "&:not(:disabled)"} forms))

(defmacro read-only
  "Apply utilities on :read-only."
  [& forms]
  (modifier/modifier* &env {:prefix "read-only" :selector "&:read-only"} forms))

(defmacro enabled
  "Apply utilities on :enabled."
  [& forms]
  (modifier/modifier* &env {:prefix "enabled" :selector "&:enabled"} forms))

(defmacro checked
  "Apply utilities on :checked."
  [& forms]
  (modifier/modifier* &env {:prefix "checked" :selector "&:checked"} forms))

(defmacro indeterminate
  "Apply utilities on :indeterminate."
  [& forms]
  (modifier/modifier* &env {:prefix "indeterminate" :selector "&:indeterminate"} forms))

(defmacro default
  "Apply utilities on :default."
  [& forms]
  (modifier/modifier* &env {:prefix "default" :selector "&:default"} forms))

(defmacro optional
  "Apply utilities on :optional."
  [& forms]
  (modifier/modifier* &env {:prefix "optional" :selector "&:optional"} forms))

(defmacro required
  "Apply utilities on :required."
  [& forms]
  (modifier/modifier* &env {:prefix "required" :selector "&:required"} forms))

(defmacro valid
  "Apply utilities on :valid."
  [& forms]
  (modifier/modifier* &env {:prefix "valid" :selector "&:valid"} forms))

(defmacro invalid
  "Apply utilities on :invalid."
  [& forms]
  (modifier/modifier* &env {:prefix "invalid" :selector "&:invalid"} forms))

(defmacro user-valid
  "Apply utilities on :user-valid."
  [& forms]
  (modifier/modifier* &env {:prefix "user-valid" :selector "&:user-valid"} forms))

(defmacro user-invalid
  "Apply utilities on :user-invalid."
  [& forms]
  (modifier/modifier* &env {:prefix "user-invalid" :selector "&:user-invalid"} forms))

(defmacro in-range
  "Apply utilities on :in-range."
  [& forms]
  (modifier/modifier* &env {:prefix "in-range" :selector "&:in-range"} forms))

(defmacro out-of-range
  "Apply utilities on :out-of-range."
  [& forms]
  (modifier/modifier* &env {:prefix "out-of-range" :selector "&:out-of-range"} forms))

(defmacro placeholder-shown
  "Apply utilities on :placeholder-shown."
  [& forms]
  (modifier/modifier* &env {:prefix "placeholder-shown" :selector "&:placeholder-shown"} forms))

(defmacro autofill
  "Apply utilities on :autofill."
  [& forms]
  (modifier/modifier* &env {:prefix "autofill" :selector "&:autofill"} forms))

(defmacro visited
  "Apply utilities on :visited."
  [& forms]
  (modifier/modifier* &env {:prefix "visited" :selector "&:visited"} forms))

(defmacro open
  "Apply utilities when the element is open: [open], :popover-open, or :open."
  [& forms]
  (modifier/modifier* &env {:prefix "open" :selector "&:is([open], :popover-open, :open)" :inverse-selector "&:not([open], :popover-open, :open)"} forms))

(defmacro selection
  "Apply utilities to ::selection, on the element and its descendants."
  [& forms]
  (modifier/modifier* &env {:prefix "selection" :selector "& *::selection, &::selection"} forms))

(defmacro marker
  "Apply utilities to ::marker, on the element and its descendants, so it works on a list."
  [& forms]
  (modifier/modifier* &env {:prefix "marker" :selector "& *::marker, &::marker"} forms))

(defmacro first-letter
  "Apply utilities to ::first-letter."
  [& forms]
  (modifier/modifier* &env {:prefix "first-letter" :selector "&::first-letter"} forms))

(defmacro first-line
  "Apply utilities to ::first-line."
  [& forms]
  (modifier/modifier* &env {:prefix "first-line" :selector "&::first-line"} forms))

(defmacro file
  "Apply utilities to ::file-selector-button, the button of a file input."
  [& forms]
  (modifier/modifier* &env {:prefix "file" :selector "&::file-selector-button"} forms))

(defmacro backdrop
  "Apply utilities to ::backdrop, behind a dialog or popover."
  [& forms]
  (modifier/modifier* &env {:prefix "backdrop" :selector "&::backdrop"} forms))

(defmacro aria-busy
  "Apply utilities when aria-busy=\"true\"."
  [& forms]
  (modifier/aria* &env "busy" forms))

(defmacro aria-checked
  "Apply utilities when aria-checked=\"true\"."
  [& forms]
  (modifier/aria* &env "checked" forms))

(defmacro aria-disabled
  "Apply utilities when aria-disabled=\"true\"."
  [& forms]
  (modifier/aria* &env "disabled" forms))

(defmacro aria-expanded
  "Apply utilities when aria-expanded=\"true\"."
  [& forms]
  (modifier/aria* &env "expanded" forms))

(defmacro aria-hidden
  "Apply utilities when aria-hidden=\"true\"."
  [& forms]
  (modifier/aria* &env "hidden" forms))

(defmacro aria-pressed
  "Apply utilities when aria-pressed=\"true\"."
  [& forms]
  (modifier/aria* &env "pressed" forms))

(defmacro aria-readonly
  "Apply utilities when aria-readonly=\"true\"."
  [& forms]
  (modifier/aria* &env "readonly" forms))

(defmacro aria-required
  "Apply utilities when aria-required=\"true\"."
  [& forms]
  (modifier/aria* &env "required" forms))

(defmacro aria-selected
  "Apply utilities when aria-selected=\"true\"."
  [& forms]
  (modifier/aria* &env "selected" forms))

(defmacro motion-reduce
  "Apply utilities when the user prefers reduced motion."
  [& forms]
  (modifier/modifier* &env {:prefix "motion-reduce" :selector "@media (prefers-reduced-motion: reduce)"} forms))

(defmacro motion-safe
  "Apply utilities when the user has no reduced-motion preference."
  [& forms]
  (modifier/modifier* &env {:prefix "motion-safe" :selector "@media (prefers-reduced-motion: no-preference)"} forms))

(defmacro print
  "Apply utilities when printing."
  [& forms]
  (modifier/modifier* &env {:prefix "print" :selector "@media print"} forms))

(defmacro sm
  "Apply utilities from a viewport width of 40rem up, like Tailwind's sm:."
  [& forms]
  (modifier/breakpoint* &env "sm" forms))

(defmacro md
  "Apply utilities from a viewport width of 48rem up, like Tailwind's md:."
  [& forms]
  (modifier/breakpoint* &env "md" forms))

(defmacro lg
  "Apply utilities from a viewport width of 64rem up, like Tailwind's lg:."
  [& forms]
  (modifier/breakpoint* &env "lg" forms))

(defmacro xl
  "Apply utilities from a viewport width of 80rem up, like Tailwind's xl:."
  [& forms]
  (modifier/breakpoint* &env "xl" forms))

(defmacro xxl
  "Apply utilities from a viewport width of 96rem up, like Tailwind's 2xl:."
  [& forms]
  (modifier/breakpoint* &env "xxl" forms))

(defmacro max-sm
  "Apply utilities below a viewport width of 40rem, like Tailwind's max-sm:."
  [& forms]
  (modifier/max-breakpoint* &env "sm" forms))

(defmacro max-md
  "Apply utilities below a viewport width of 48rem, like Tailwind's max-md:."
  [& forms]
  (modifier/max-breakpoint* &env "md" forms))

(defmacro max-lg
  "Apply utilities below a viewport width of 64rem, like Tailwind's max-lg:."
  [& forms]
  (modifier/max-breakpoint* &env "lg" forms))

(defmacro max-xl
  "Apply utilities below a viewport width of 80rem, like Tailwind's max-xl:."
  [& forms]
  (modifier/max-breakpoint* &env "xl" forms))

(defmacro max-xxl
  "Apply utilities below a viewport width of 96rem, like Tailwind's max-2xl:."
  [& forms]
  (modifier/max-breakpoint* &env "xxl" forms))

(defmacro not
  "Negate the nested pseudo-class modifier."
  [form]
  (modifier/not* &env form))
