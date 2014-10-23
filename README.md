# Garden

Garden is a library for rendering CSS in Clojure and ClojureScript.
Conceptually similar to
[Hiccup](https://github.com/weavejester/hiccup), it uses vectors to
represent rules and maps to represent declarations. It is designed for
stylesheet authors who are interested in what's possible when you
trade a preprocessor for a programming language.

## Table of contents

* [Installation](#installation)
* [Syntax](#syntax)
  * [Rules](#rules)
    * [Parent selector references](#parent-selector-references)
  * [Declarations](#declarations)
  * [Units](#units)
  * [Color](#color)
  * [Arithmetic](#arithmetic)
  * [Media queries](#media-queries)
* [Plugin](#plugin)
* [Community](#community)

## Installation

Add the following dependency to your `project.clj` file:

```clojure
[garden "1.2.4"]
```

For the current development version use:

```clj
[garden "1.2.5-SNAPSHOT"]
```

Garden requires Clojure `1.6.0` and is known to work with ClojureScript `0.0-2342`.

## Syntax

Garden syntax is very similar to Hiccup. If you're familiar with Hiccup you
should feel right at home working with Garden. If not, don't sweat it! Garden's
syntax is fairly simple.

From your project's root directory start up a new REPL and try the following:

```clojure
user=> (require '[garden.core :refer [css]])
nil
user=> (css [:body {:font-size "16px"}])
"body{font-size:16px}"
```

First you'll notice the use of the `css` function. This function takes
an optional map of compiler flags, any number of rules, and returns a
string of compiled CSS. We'll start off by discussing rules then
follow up with declarations and Garden's other utilities. Then we'll
demonstrate the use of compiler flags.

### Rules

As mentioned, vectors represent rules in CSS. The first _n_ **non-collection**
elements of a vector depict the rule's selector where _n_ > 0. When _n_ = 0 the
rule is not rendered. To produce a rule which selects the `<h1>` and `<h2>` HTML
elements for example, we simply begin a vector with `[:h1 :h2]`.

```clojure
user=> (css [:h1 :h2 {:font-weight "none"}])
"h1,h2{font-weight:none}"
```

To target child selectors nested vectors may be employed:

```clojure
user=> (css [:h1 [:a {:text-decoration "none"}]])
"h1 a{text-decoration:none}"
user=> (css [:h1 :h2 [:a {:text-decoration "none"]])
"h1 a, h2 a{text-decoration:none}"
```

A slightly more complex example demonstrating nested vectors with multiple
selectors:

```clojure
user=> (css [:h1 :h2 {:font-weight "normal"}
             [:strong :b {:font-weight "bold"}]])
"h1,h2{font-weight:normal}h1 strong,h1 b,h2 strong,h2 b{font-weight:bold}"
```

#### Parent selector references

As in Sass, Garden also supports selectors prefixed with the `&`
character allowing you to reference a parent selector.

```clojure
user=> (css [:a
             {:font-weight 'normal
              :text-decoration 'none}
             [:&:hover
              {:font-weight 'bold
               :text-decoration 'underline}]])
"a{text-decoration:none;font-weight:normal}a:hover{text-decoration:underline;font-weight:bold}"
```

### Declarations

Clojure maps represent CSS declarations where map keys and values represent
CSS properties and values respectively. Garden's declaration syntax is a bit
more involved than rules and understanding it is important to make the most of
the library.

#### Properties

Declaration map keys _should_ either be a string, keyword, or symbol:

```clojure
user=> (css [:h1 {"font-weight" "normal"}])
"h1{font-weight:normal}"
user=> (css [:h1 {:font-weight "normal"}])
"h1{font-weight:normal}"
user=> (css [:h1 {'font-weight "normal"}])
"h1{font-weight:normal}"
```

Be aware Garden makes no attempt to validate your declarations and
will not raise an error if other key types are used.

```clojure
user=> (css [:h1 {30000 "nom-nom"}])
"h1{30000:nom-nom}"
```

#### Values

We've already seen strings used as declaration map values, but Garden also
supports keywords, symbols, numbers, maps, vectors, and lists in addition.

##### Strings, keywords, symbols, and numbers

Strings, keywords, symbols, and numbers are rendered as literal CSS values:

```clojure
user=> (css [:body {:font "16px sans-serif"}])
"body{font:16px sans-serif}"
```

Be warned, you must escape literal string values yourself:

```clojure
user=> (css [:pre {:font-family "\"Liberation Mono\", Consolas, monospace"}])
"pre{font-family:\"Liberation Mono\", Consolas, monospace}"
```

##### Maps

In some cases it would be useful target several properties in a "group" of
properties without having to type the same prefix several times. To do this
with Garden we use maps. Maps as declaration values are used to denote a
property suffix (IE. `-family` or `-weight`) and may be nested as deeply as
you like.

Here are a few practical examples of where this technique might be handy:

```clojure
user=> ;; Working with vendor prefixes:
user=> (css [:.box
             {:-moz {:border-radius "3px"
                     :box-sizing "border-box"}}])
".box{-moz-border-radius:3px;-moz-box-sizing:border-box}"
user=> ;; Creating DRY "mixins":
user=> (def reset-text-formatting
         {:font {:weight "normal" :style "normal" :variant "normal"}
          :text {:decoration "none"}})
#'user/reset-text-formatting
user=> (css [:a reset-text-formatting])
"a{font-variant:normal;font-style:normal;font-weight:normal;text-decoration:none}"
user=> (defn partly-rounded
         ([r1] (partly-rounded r1 r1))
         ([r1 r2]
          {:border {:top-right-radius r1
                    :bottom-left-radius r2}}))
#'user/partly-rounded
user=> (css [:.box (partly-rounded "3px")])
".box{border-bottom-left-radius:3px;border-top-right-radius:3px}"
```

##### Vectors and lists

Finally we have vectors and lists which are handled in the same manor when used
as a declaration value. The semantics of these values increment the level of
complexity somewhat so be sure you understand their behavior before you use
them. When you use a vector/list as a value you are asking Garden for a
*comma* separated list.

```clojure
user=> (css [:p {:font ["16px" "sans-serif"]}])
"p{font:16px,sans-serif}"
```

When you nest a vector/list you are asking for a *space* separated list.

```clojure
user=> (css [:p {:font [["16px" 'Helvetica] 'Arial 'sans-serif]}])
"p{font:16px Helvetica,Arial,sans-serif}"
```

### Units

So far we've got all the core pieces to start building basic stylesheets. But
it would be useful to have something for working with one of CSS's most
fundamental data types: units!

Fortunately, Garden has built in support for working with all the major CSS
units. This includes creation, conversion, and arithmetic. To start using units
use/require the `garden.units` namespace.

```clojure
user=> (require '[garden.units :as u :refer [px pt]])
nil
```

For demonstration purposes we're only `refer`ing the `px` and `pt` units but
Garden supports all of the usual suspects. Also we'll use the `css` macro to
render the units as strings but note this is not necessary when authoring a
stylesheet.

Unit creation is straightforward:

```clojure
user=> (px 16)
#garden.types.CSSUnit{:unit :px, :magnitude 1}
```

To see the value as it would appear in CSS require the `garden.repl`
namespace.

```clojure
user=> (require 'garden.repl)
nil
user=> (px 16)
16px
```

Unit functions take a number _n_ and construct a new `garden.types.CSSUnit` record
with _n_ as the magnitude. Unit functions also accept other units as values
returning their conversion if possible. This makes working with unit values
very flexible.

```clojure
user=> (px (px 16))
16px
user=> (px (pt 1))
1.3333333333px
```

Unit arithmetic is available via the - spoiler alert- unit arithmetic functions.

```clojure
user=> (require '[garden.units :refer (px+ px* px- px-div)])
nil
user=> (px+ 1 2 3 4 6)
16px
user=> (px-div 2 4)
0.5px
user=> (px* 2 2)
4px
```

Since the arithmetic functions use the primary unit functions in their
definitions, conversion occurs seamlessly (when possible):

```clojure
user=> (px* 2 (pt 1))
2.6666666666px
```

You might be wondering, which units can be converted to another? This
depends on the type of unit you are working with. The CSS spec outlines a few
categories of units but only absolute (`px`, `pt`, `pc`, `in`, `cm`, and `mm`),
angle (`deg`, `grad`, `rad`, `turn`), time (`s`, `ms`), and frequency
(`Hz`, `kHz`) units may be freely converted and only between their respective
groups. This means you cannot, for example, convert `px` to `rad` or `Hz` to
`cm`. Doing so will raise an error.

In the future, some exceptions to this rule might apply for working with `em`s
since it's technically possible to compute their contextual value.

### Color

What would a stylesheet be like with out color? No fun. That's what it
would be like. And the person who's interested in writing a stylesheet
in Clojure probably wants tools for working with color. Who wants to
write a stylesheet where colors are strings that look like `"#A55"`?
No one. That's who.

Since `0.1.0-beta5` Garden comes with a (mostly) complete set of
functions for dealing with colors. If you've worked with Sass you'll
be pleased to know many of the same color functions are available in
Garden.

Garden's color functions are available in the `garden.color`
namespace.

```clojure
user=> (require '[garden.color :as color :refer [hsl rgb]])
```

Let's create a color to work with.

```clojure
user> (def red (hsl 0 100 50))
#'user/red
user> red
#ff0000
```

We've defined `red` in terms of the HSL value for pure red with the
`hsl` function (`rgb` is also available). When we evaluate the value
of `red` at the REPL we notice it is displayed in the familiar
hexadecimal format.

Let's apply some color functions to it. By the way, if you're
using Emacs, try turning on `rainbow-mode` to see the colors
highlighted.

```clojure
;; Make dark red.
user> (color/darken red 25)
#800000
;; Make light red.
user> (color/lighten red 25)
#ff8080
```

But, wait! There's more!

```clojure
;; Make an orange color...
user> (def orange (color/hsl 30 100 50))
;; ...and mix it with red.
user> (color/mix red orange)
#ff4000
;; Make a green color...
user> (def green (hsl 120 100 50))
;; ...and add it to red to get yellow.
user> (color/color+ red green)
#ffff00
;; Get a set of analogous colors.
user> (color/analogous red)
(#ff0000 #ff8000 #ffff00)
```

As with units, colors can be added, subtracted, divided and
multiplied with `color+`, `color-`, `color*`, and `color-div`
respectively. There are several other nice functions available for
finding color complements, triads, tetrads, and more.

### Arithmetic

Now that we have a solid understanding of how units and colors
operate, we can talk about Garden's generic arithmetic operators.
While working with functions like `px+`, `color+`, etc. have their
advantages, sometimes they can get in the way. To get around this
you can use the operators in the `garden.arithmetic` namespace.

```clojure
(ns user
  ;; Unless you want to see a bunch of warnings add this line.
  (:refer-clojure :exclude '[+ - * /])
  (:require '[garden.arithmetic :refer [+ - * /]]))
```

This will allow you to perform operations like this:

```clojure
user> (+ 20 (color/hsl 0 0 0) 1 (color/rgb 255 0 0))
#ff1515
user> (- 20 (px 1) 5 (pt 5))
7.333333333500001px
```

### Media queries

Authoring stylesheets these days without media queries is somewhat
like having prime rib without horseradish. Garden provides the
`at-media` function available in the `garden.stylesheet` namespace.

```clojure
user=> (require '[garden.stylesheet :refer [at-media]])
nil
user=> (css (at-media {:screen true} [:h1 {:font-weight "bold"}]))
"@media screen{h1{font-weight:bold}}"
user=> (css
         (at-media {:min-width (px 768) :max-width (px 979)}
           [:container {:width (px 960)}])
"@media (max-width:979px) and (min-width:768px){container{width:960px}}"
```

Media queries may also be nested:

```clojure
user=> (css [:a {:font-weight "normal"}
             [:&:hover {:color "red"}]
             (at-media {:screen true}
               [:&:hover {:color "pink"}])])
```

and will out put the equivalent CSS:

```css
a {
  font-weight: normal;
}

a:hover {
  color: red;
}

@media screen {

  a:hover {
    color: pink;
  }

}
```

To understand how media expressions are interpreted refer to this table:

 Map                                         | Interpretation
---------------------------------------------|-------------------------------------------
 `{:screen true}`                            | `screen`
 `{:screen false}`                           | `not screen`
 `{:screen true :braille false}`             | `screen and not braille`
 `{:screen :only}`                           | `only screen`
 `{:min-width (px 768) :max-width (px 959)}` | `(min-width: 768px) and (max-width: 959)`

## Compiler flags

The `css` function optionally takes a map of compiler flags.

### Output flags

#### Printing

Often you are interested in saving a fully expanded result of
compilation for development and a compressed version for production.
This is controlled by the `:pretty-print?` flag which may either be
`true` or `false`. By default this flag is set to `true`.

Assuming:

```clojure
(def styles
  [[:h1 {:font-weight "normal"}]
    [:a {:text-decoration "none"}]])
```

`(css {:pretty-print? true} styles)` results in the following output when
compiled:

```css
h1 {
  font-weight: normal;
}

a {
  text-decoration: none;
}
```

`(css {:pretty-print? false} styles)` results in the following output when
compiled:

```css
"h1{font-weight:normal}a{text-decoration:none}"
```

For Clojure generated stylesheets are compressed using the YUI
Compressor which yeilds much better results when compared with the
previous versions.

```clojure
user=> (css {:pretty-print? false} [:body {:background "#ff0000"}])
"body{background:#f00}"
user> (css {:pretty-print? false} [:div {:box-shadow [[(px 0) (px 0.5) (hsl 0 0 0)]]}])
"div{box-shadow:0 .5px #000}"
```

For ClojureScript compression mostly consists of whitespace
elimination wherever possible.

#### Saving

**Note:** This is currently not available for ClojureScript, but
support is planned.

To save a stylesheet to disk simply set the `:output-to` flag to the
desired path.

```clojure
user=> (css {:output-to "foo.css"} [:h1 {:font-weight "normal"}])
Wrote: foo.css
"h1 {\n  font-weight: normal;\n}"
```

#### Vendors

Vendor prefixing can be a pain but Garden can help with that in some
cases if you set the `:vendors` flag. The value is expeced to be a
vector of prefixes (ie `["webkit" "moz" "o"]`). By specifying this,
Garden will automatically prefix declarations tagged with the
`^:prefix` meta and `@keyfames`.

This:

```clojure
(require '[garden.def :refer [defrule defkeyframes]])

(defkeyframes pulse
  [:from
   {:opacity 0}]

  [:to
   {:opacity 1}])

(css {:vendors ["webkit"]
      :output-to "foo.css"}

  ;; Include our keyframes
  pulse

  [:h1
   ;; Notice we don't need to quote pulse.
   ^:prefix {:animation [[pulse "2s" :infinite :alternate]]}])
```

will produce

```css
@keyframes pulse {

  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }

}

@-webkit-keyframes pulse {

  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }

}

h1 {
  -webkit-animation: pulse 2s infinite alternate;
  animation: pulse 2s infinite alternate;
}
```

#### Auto-prefixing

If you want Garden to automatically vendor prefix
specific properties, add them to the `:auto-prefix` set.

```clj
user=> (css
        {:vendors ["webkit"]
         :auto-prefix #{:border-radius}}
        [:.foo {:border-radius (px 3)}])
".foo{border-radius:3px;-webkit-border-radius:3px;}"
```

## Contributors

Listed by first commit:

* [noprompt](https://github.com/noprompt)
* [malcolmsparks](https://github.com/malcolmsparks)
* [jeluard](https://github.com/jeluard)
* [ToBeReplaced](https://github.com/ToBeReplaced)
* [migroh](https://github.com/migroh)

## Plugin

If you're interested in automatically compiling your stylesheets be
sure to check out the
[`lein-garden`](https://github.com/noprompt/lein-garden)
plugin. 

## Community

* [Mailing list](https://groups.google.com/forum/#!forum/garden-clojure)

## Thanks

A big thank you goes out to [@weavejester](https://github.com/weavejester) for
creating Hiccup, [@briancarper](https://github.com/briancarper) for creating
[gaka](https://github.com/briancarper/gaka/), and
[@paraseba](https://github.com/paraseba) for creating
[cssgen](https://github.com/paraseba/cssgen). I learned a lot
from studying the source code of these libraries (and borrowed several ideas
from them). Writing this library would have been significantly more difficult
without the hard work of these individuals.

I'd also like to thank [@jhardy](https://github.com/jhardy) for putting up with
random questions and pushing me to keep working on this library.

Thanks to everyone in `#clojure` on IRC for answering my questions and being
patient with me. If you're looking for an example of a great community, look no
further.

## License

Copyright © 2013 Joel Holdbrooks

Distributed under the Eclipse Public License, the same as Clojure.
