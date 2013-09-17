## Changes between 0.1.0-beta6 and 1.0.0-SNAPSHOT

### Syntax Changes

`[]` as a declaration value now implies a comma-separated list. Nested
lists are rendered as space separated lists and the behavior is no longer
recursive. The rational for this is it prevents situations where you
would need to nest vetors/lists more than two levels to acheive the
result you are interested in; gradients being a good example. The
recursive nature was removed simply because it lead to confusing code.

When using meta-data as a media query you must now explicitly use the
`:media` key for which the value represents the query. For example,
originally to acheive `@media screen` you would simply attach the meta
`{:screen true}`. The same result now can be produced with `{:media
{:screen true}}`. Although this is not nearly as convenient it
prevents conflicts with other meta data. Because of these changes the
use of `garden.stylesheet/at-media` is strongly encouraged.

### Library changes

`garden.core/css` is no longer a macro.

Added `garden.core/style` for use with the HTML `style` attribute. 

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

Added the `garden.repl` namespace which includes implementations of
`print-method` for Garden's internal record types. `require` this when
you want to see the output of Garden's internal types such as
`garden.types.CSSFunction`, etc. as they would appear in CSS.

Fixed spelling correction from `garden.arithemetic` to
`garden.arithmetic`.

All functions from the `garden.stylesheet.functions.filters` have been
moved to `garden.stylesheet.functions`.

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
Currently this only applies to `@keyframes`.

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
