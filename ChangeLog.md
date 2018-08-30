## Changes in 1.3.6

Add `@supports` rule support.

## Changes in 1.3.4

**Breaking Change:** The `function` field in `CSSFunction` has been renamed to avoid Closure Compiler warnings. [#144](https://github.com/noprompt/garden/issues/144)

## Changes from 1.3.0

Migrate cljx to cljc and maintain parity with Clojure/Cljs with 1.7. For
more info see this [PR](https://github.com/noprompt/garden/pull/81)

## Changes between 1.2.0 and 1.2.1

Fix a spelling mistake in `garden.selectors`.

## Changes between 1.1.8 and 1.2.0

Added new namespace `garden.selectors` containing functions and macros
for working with CSS selectors. This namespace defines the most common
type, pseudoclass, and pseudoelement selectors which are actually
instances of a new type `CSSSelector`.

Instances of `CSSSelector` can be treated both as a value and as a
function. When `garden.selectors/css-selector` is applied to a
`CSSSelector` the return value is the string representation of the
selector. When treated as a function the return result is a new
instance of `CSSSelector` which is merely a concatenation of the
original selector value and the selector value of each argument.

```clj
user> (ns foo
        (:use [garden.selectors :as s
               :exclude [+ - > empty first map meta not time var]])
        (:require
         [garden.repl]
         [garden.core :refer [css]]))

foo> a
a
foo> (a)
a
foo> (a hover)
a:hover
foo> (css [(a hover) {:font-weight :bold}])
"a:hover {\n  font-weight: bold;\n}"
```

This namespace should be considered alpha and is subject to change.

## Changes between 1.1.7 and 1.1.8

Added option for configuring line break positions in
`garden.compression/compress-stylesheet` (Clojure only).

## Changes between 1.1.6 and 1.1.7

Allow a preamble to be specified (Clojure only).

Macros in `garden.def` take advantage of `macroexpand` to capture meta
data.

## Changes between 1.1.5 and 1.1.6

Move plugins and dependencies to `:dev` profile

## Changes between 1.1.4 and 1.1.5

Allow vendor prefix overrides at the declaration level.

```clojure
(css {:vendors [:foo]}
  [:a
   ^{:prefix true :vendors [:bar]}
   {:x 1}])
```

```css
a {
  x: 1;
  -bar-x: 1;
}
```


## Changes between 1.1.2 and 1.1.3

Fix incorrect rendering of hsla values by not stripping the unit from
units with `0` magnitude.

## Changes between 1.1.0 and 1.1.1

Fix incorrect rendering of values in media expression.

## Changes between 1.0.2 and 1.1.0

Add two new macros `defstyles` and `defstylesheet`. These macros
eliminate two common patterns when authoring stylesheets with
Garden.

```clojure
(require '[garden.def :refer [defstyles defstylesheet]])

;; This:
(defstyles h1-styles
  [:h1 {:font-weight "normal"}])

;; is equivalent to:
(def h1-styles
  (list
    [:h1 {:font-weight "normal"}]))

;; This:
(defstylesheet screen
  {:output-to (io/resource "public/css/screen.css")}
  h1-styles)

;; is equivalent to:
(def screen
  (css {:output-to (io/resource "public/css/screen.css")}
    h1-styles))
```

## Changes between 1.0.0 and 1.0.2

Fixed incorrect rendering of hsla colors.

## Changes between 1.0.0 and 1.0.1

Fixed rendering for colors with alpha channel.

## Changes between 0.1.0-beta6 and 1.0.0

### ClojureScript support!

Thanks to @jeluard Garden can now be used client-side projects. Expect
more exciting things in this area.

### Syntax Changes

`[]` as a declaration value now implies a comma-separated list. Nested
lists are rendered as space separated lists and the behavior is no longer
recursive. The rational for this is it prevents situations where you
would need to nest vetors/lists more than two levels to acheive the
result you are interested in; gradients being a good example. The
recursive nature was removed simply because it lead to confusing code.

Meta data is no longer interpreted as a media-query. This was adding a
lot of additional complexity that was simply easier to solve by
creating a type. If you need to write a media-query use
`garden.stylesheet/at-media`.

### Library changes

`garden.core/css` is no longer a macro.

Added `garden.core/style` for use with the HTML `style` attribute.

Added new type `garden.types.CSSAtRule`

Added `garden.stylesheet/at-keyframes` for creating `@keyframes` blocks.

Added `garden.def/cssfn` and `garden.def/defcssfn` for defining custom
`CSSFunction`s. `cssfn` and `defcssfn` create functions which
automatically return new instances of `CSSFunction`.

```clojure
(require '[garden.def :refer [cssfn defcss]])

(defcssfn example
  "Create a CSS example function."
  ([arg] arg)
  ([arg1 arg2] [arg1 arg2])
  ([arg1 arg2 arg3] [arg1 [arg2 arg3]])

(css [:sel {:prop (example 1)}])
;; => sel{prop:example(1)}

(css [:sel {:prop (example 1 2)}])
;; => sel{prop:example(1,2)}

(css [:sel {:prop (example 1 2 3)}])
;; => sel{prop:example(1,2 3)}

(let [example (cssfn "example")]
  (css [:sel {:prop (example [1 [2 3]])}]))
;; => sel{prop:example(1,2 3)}
```

Added `garden.def/defkeyframes`. This allows for easy reuse of
animations throughout a project along with providing an in for
creating animation libraries.

This:

```clojure
(defkeyframes my-animation
  [:from
   {:background "red"}]

  [:to
   {:background "yellow"}])

(css
  my-animation ;; Include the animation in the stylesheet.
  [:div
   {:animation [[my-animation "5s"]]}])"
```

will produce:

```css
@keyframes my-animation {

  from {
    background: red;
  }

  to {
    background: yellow;
  }

}

div {
  animation: my-animation 5s;
}
```

Added the `garden.repl` namespace which includes implementations of
`print-method` for Garden's internal record types. `require` this when
you want to see the output of Garden's internal types such as
`garden.types.CSSFunction`, etc. as they would appear in CSS.

Fixed spelling correction from `garden.arithemetic` to
`garden.arithmetic`.

Removed the `garden.stylesheet.selectors`,
`garden.stylesheet.psuedo-classes`, `garden.stylesheet.functions`, and
`garden.stylesheet.functions.filters` namespaces. These namespace
provided a nice convenience but added a lot of extra bloat to the library.

Removed `garden.stylesheet/font-family` for the same reason mentioned above.

### Compiler changes

The [YUI Compressor](https://github.com/yui/yuicompressor) is now used
for stylesheet compression instead of the original compression
techniques. This has the benefit of reducing compiler code and
providing better and more sophisticated compression overall.

Media queries no longer appear at the bottom of the stylesheet but in
roughly the same order they were defined in.

### Compiler flag changes

The `:output-style` flag has been replaced with `:pretty-print?`.
`:pretty-print? true` and `:pretty-print? false` (or simply omitting
the flage) are equivalent to `:output-style :expanded` and
`:output-style :compressed` respectively.

The `:output-to` flag may be specified to save complied CSS to a path
on disk. The return result of `css` will still be a string, however.

The `:vendors` flag may be set to a vector of browser prefixes for
which certain types, when rendered, will automatically be prefixed.
This automatically applies to `@keyframes` and any declarations with
the meta `{:prefix true}`.

For example this

```clojure
(require '[garden.stylesheet :refer [at-keyframes]])

(css {:vendors ["moz" "webkit"]
      :pretty-print? true}
  [:* :*:after :*:before
   ^:prefix {:box-sizing "border-box"}]

  (at-keyframes "foo"
    [:from
     {:foo "bar"}]
    [:to
     {:foo "baz"}]))
```

will produce

```css
*, *:after, *:before {
  -moz-box-sizing: border-box;
  -webkit-box-sizing: border-box;
  box-sizing: border-box;
}

@keyframes foo {

  from {
    foo: bar;
  }

  to {
    foo: baz;
  }

}

@-moz-keyframes foo {

  from {
    foo: bar;
  }

  to {
    foo: baz;
  }

}

@-webkit-keyframes foo {

  from {
    foo: bar;
  }

  to {
    foo: baz;
  }

}
```

The `:media-expressions` flag expected to be hash-map provides
customization for media expression handling. Currently there is only
one flag in this hash-map which may be set: `:nesting-behavior`. It
may have one of two values, `:default` or `:merge`. When set to
`:merge` nested media-queries will use `merge` to combine their
expression values with their parents. When unconfigured or set to
`:default` nested media queries will simply appear after their parent
when rendered.

### Unit changes

`CSSUnit`s with a `magnitude` value for which `(zero? magnitude)`
returns `true` are rendered without their unit type.
