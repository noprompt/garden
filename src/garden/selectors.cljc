(ns garden.selectors
  "Macros and functions for working with CSS selectors."
  (:require
   [clojure.string :as string])
  #?(:clj
     (:refer-clojure :exclude [+ - > empty first map meta not time var]))
  #?(:clj
     (:import clojure.lang.Keyword
              clojure.lang.Symbol
              clojure.lang.IFn
              clojure.lang.Named))
  #?(:cljs
     (:refer-clojure :exclude [+ - > empty first map meta not time]))
  #?(:cljs
     (:require-macros
      [garden.selectors :refer [defselector
                                defid
                                defpseudoclass
                                defpseudoelement
                                gen-type-selector-defs
                                gen-pseudo-class-defs]])))

(defprotocol ICSSSelector
  (css-selector [this]))

(defn selector? [x]
  (satisfies? ICSSSelector x))

(extend-protocol ICSSSelector
  #?(:clj String
     :cljs string)
  (css-selector [this] this)

  Keyword
  (css-selector [this]
    (name this))

  Symbol
  (css-selector [this]
    (name this)))

#?(:clj
   (defrecord CSSSelector [selector]
     ICSSSelector
     (css-selector [this]
       (css-selector (:selector this)))

     IFn
     (invoke [this]
       this)
     (invoke [this a]
       (CSSSelector. (str (css-selector this)
                          (css-selector a))))
     (invoke [this a b]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b))))
     (invoke [this a b c]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c))))
     (invoke [this a b c d]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d))))
     (invoke [this a b c d e]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e))))
     (invoke [this a b c d e f]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f))))
     (invoke [this a b c d e f g]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g))))
     (invoke [this a b c d e f g h]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h))))
     (invoke [this a b c d e f g h i]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i))))
     (invoke [this a b c d e f g h i j]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j))))
     (invoke [this a b c d e f g h i j k]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k))))
     (invoke [this a b c d e f g h i j k l]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l))))
     (invoke [this a b c d e f g h i j k l m]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m))))
     (invoke [this a b c d e f g h i j k l m n]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n))))
     (invoke [this a b c d e f g h i j k l m n o]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o))))
     (invoke [this a b c d e f g h i j k l m n o p]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o)
                          (css-selector p))))
     (invoke [this a b c d e f g h i j k l m n o p q]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o)
                          (css-selector p)
                          (css-selector q))))
     (invoke [this a b c d e f g h i j k l m n o p q r]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o)
                          (css-selector p)
                          (css-selector q)
                          (css-selector r))))
     (invoke [this a b c d e f g h i j k l m n o p q r s]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o)
                          (css-selector p)
                          (css-selector q)
                          (css-selector r)
                          (css-selector s))))
     (invoke [this a b c d e f g h i j k l m n o p q r s t]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o)
                          (css-selector p)
                          (css-selector q)
                          (css-selector r)
                          (css-selector s)
                          (css-selector t))))

     (applyTo [this args]
       (clojure.lang.AFn/applyToHelper this args))))

#?(:cljs
   (defrecord CSSSelector [selector]
     ICSSSelector
     (css-selector [this]
       (css-selector (:selector this)))

     IFn
     (-invoke [this]
       this)
     (-invoke [this a]
       (CSSSelector. (str (css-selector this)
                          (css-selector a))))
     (-invoke [this a b]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b))))
     (-invoke [this a b c]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c))))
     (-invoke [this a b c d]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d))))
     (-invoke [this a b c d e]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e))))
     (-invoke [this a b c d e f]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f))))
     (-invoke [this a b c d e f g]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g))))
     (-invoke [this a b c d e f g h]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h))))
     (-invoke [this a b c d e f g h i]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i))))
     (-invoke [this a b c d e f g h i j]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j))))
     (-invoke [this a b c d e f g h i j k]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k))))
     (-invoke [this a b c d e f g h i j k l]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l))))
     (-invoke [this a b c d e f g h i j k l m]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m))))
     (-invoke [this a b c d e f g h i j k l m n]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n))))
     (-invoke [this a b c d e f g h i j k l m n o]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o))))
     (-invoke [this a b c d e f g h i j k l m n o p]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o)
                          (css-selector p))))
     (-invoke [this a b c d e f g h i j k l m n o p q]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o)
                          (css-selector p)
                          (css-selector q))))
     (-invoke [this a b c d e f g h i j k l m n o p q r]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o)
                          (css-selector p)
                          (css-selector q)
                          (css-selector r))))
     (-invoke [this a b c d e f g h i j k l m n o p q r s]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o)
                          (css-selector p)
                          (css-selector q)
                          (css-selector r)
                          (css-selector s))))
     (-invoke [this a b c d e f g h i j k l m n o p q r s t]
       (CSSSelector. (str (css-selector this)
                          (css-selector a)
                          (css-selector b)
                          (css-selector c)
                          (css-selector d)
                          (css-selector e)
                          (css-selector f)
                          (css-selector g)
                          (css-selector h)
                          (css-selector i)
                          (css-selector j)
                          (css-selector k)
                          (css-selector l)
                          (css-selector m)
                          (css-selector n)
                          (css-selector o)
                          (css-selector p)
                          (css-selector q)
                          (css-selector r)
                          (css-selector s)
                          (css-selector t))))))


(defn selector [x]
    (CSSSelector. x))

;; ---------------------------------------------------------------------
;; Macros

#?(:clj
   (defmacro defselector
     "Define an instance of a CSSSelector named sym for creating a
  CSS selector. This instance doubles as both a function and a
  literal (when passed to the css-selector). When the function is called
  it will return a new instance that possesses the same properties. All
  arguments to the function must satisfy ICSSSelector.

  Example:

    (defselector a)
    ;; => #'user/a
    (a \":hover\")
    ;; => #<CSSSelector garden.selectors.CSSSelector@7c42c2a9>
    (css-selector a)
    ;; => \"a\"
    (css-selector (a \":hover\"))
    ;; => \"a:hover\"
  "
     ([sym]
      `(defselector ~sym ~(name sym)))
     ([sym strval]
      {:pre [(string? strval)]}
      (let [[_ sym v] (macroexpand `(def ~sym (selector ~strval)))
            sym (vary-meta sym assoc :arglists `'([~'& ~'selectors]))]
        `(def ~sym ~v)))))

#?(:clj
   (defmacro defclass [sym]
     `(defselector ~sym ~(str "." (name sym)))))

#?(:clj
   (defmacro defid [sym]
     `(defselector ~sym ~(str "#" (name sym)))))

#?(:clj
   (defmacro defpseudoclass
     "Define an instance of a CSSSelector named sym for creating a CSS
  pseudo class. This instance doubles as both a function and a
  literal (when passed to the css-selector). When the function is called
  it will return a new instance that possesses the same properties. All
  arguments to the function must satisfy ICSSSelector.

  Optionally fn-tail may be passed to create a structual pseudo class.
  The return value of the function constructed from fn-tail will be
  cast to a string via css-selector or str.

  Example:

    (defselector a)
    ;; => #'user/a
    (defpseudoclass hover)
    ;; => #'user/hover
    (hover)
    ;; => #<CssSelector garden.selectors.CssSelector@2a0ca6e1>
    (p/selector (a hover))
    ;; => \"a:hover\"

  Example:

    (defpseudoclass not [x]
      (p/selector x))
    ;; => #'user/not
    (p/selector (a hover (not \"span\"))
    ;; => a:hover:not(span)

    ;; Where p/selector is garden.protocols/selector
  "
     [sym & fn-tail]
     (if (seq fn-tail)
       (let [fn1 (macroexpand `(fn ~fn-tail))
             arglists (clojure.core/map clojure.core/first (rest fn1))
             [_ sym fn2] (macroexpand
                          `(defn ~sym [& args#]
                             (let [v# (apply ~fn1 args#)
                                   v# (if (selector? v#)
                                        (css-selector v#)
                                        v#)]
                               (selector (str \: ~(name sym) "(" v# ")")))))
             sym (vary-meta sym assoc :arglists `'~arglists)]
         `(def ~sym ~fn2))
       `(defselector ~sym ~(str \: (name sym))))))

#?(:clj
   (defmacro defpseudoelement
     "Define an instance of a CSSSelector named sym for creating a CSS
  pseudo element. This instance doubles as both a function and a
  literal (when passed to the css-selector). When the function is called
  it will return a new instance that possesses the same properties. All
  arguments to the function must satisfy ICSSSelector.

  Example:

    (defselector p)
    ;; => #'user/p
    (defpseudoelement first-letter)
    ;; => #'user/first-letter
    (first-letter)
    ;; => #<CssSelector garden.selectors.CssSelector@20aef718>
    (p/selector (p first-letter))
    ;; => \"p::first-letter\"

    ;; Where p/selector is garden.protocols/selector
  "
     [sym]
     `(defselector ~sym ~(str "::" (name sym)))))

;;----------------------------------------------------------------------
;; Type selectors classes

(def ^:private html-tags
  '[a
    abbr
    address
    area
    article
    aside
    audio
    b
    base
    bdi
    bdo
    blockquote
    body
    br
    button
    canvas
    caption
    cite
    code
    col
    colgroup
    command
    datalist
    dd
    del
    details
    dfn
    div
    dl
    dt
    em
    embed
    fieldset
    figcaption
    figure
    footer
    form
    h1
    h2
    h3
    h4
    h5
    h6
    head
    header
    hgroup
    hr
    html
    i
    iframe
    img
    input
    ins
    kbd
    keygen
    label
    legend
    li
    link
    map
    mark
    math
    menu
    meta
    meter
    nav
    noscript
    object
    ol
    optgroup
    option
    output
    p
    param
    pre
    progress
    q
    rp
    rt
    ruby
    s
    samp
    script
    section
    select
    small
    source
    span
    strong
    style
    sub
    summary
    sup
    svg
    table
    tbody
    td
    textarea
    tfoot
    th
    thead
    time
    title
    tr
    track
    u
    ul
    var
    video
    wbr])

#?(:clj
   (defmacro ^:private gen-type-selector-defs []
     `(do
        ~@(for [tag html-tags
                :let [doc (str "CSS " tag " type selector.")
                      tag (vary-meta tag assoc :doc doc)]]
            `(defselector ~tag)))))

(gen-type-selector-defs)

;;----------------------------------------------------------------------
;; Pseudo classes

(def ^:private pseudo-classes
  '[active
    checked
    default
    disabled
    empty
    enabled
    first
    first-child
    first-of-type
    fullscreen
    focus
    hover
    indeterminate
    in-range
    invalid
    last-child
    last-of-type
    left
    links
    only-child
    only-of-type
    optional
    out-of-range
    read-only
    read-write
    required
    right
    root
    scope
    target
    valid
    visited])

#?(:clj
   (defn- gen-pseudo-class-def [p]
     (let [p (vary-meta p assoc :doc (str "CSS :" p " pseudo-class selector."))]
       `(defpseudoclass ~p))))

#?(:clj
   (defmacro ^:private gen-pseudo-class-defs []
     `(do
        ~@(for [p pseudo-classes]
            (gen-pseudo-class-def p)))))

(gen-pseudo-class-defs)

;;----------------------------------------------------------------------
;; Structural pseudo classes

(defpseudoclass lang [language]
  (name language))

(defpseudoclass not [selector]
  (css-selector selector))

;; SEE: http://www.w3.org/TR/selectors/#nth-child-pseudo
(def nth-child-re
  #?(:clj
     #"\s*(?i:[-+]?\d+n\s*(?:[-+]\s*\d+)?|[-+]?\d+|odd|even)\s*")
  #?(:cljs
     (js/RegExp. "\\s*(?:[-+]?\\d+n\\s*(?:[-+]\\s*\\d+)?|[-+]?\\d+|odd|even)\\s*"
                 "i")))

(defn nth-x
  "nth-child helper."
  [x]
  (assert (or (string? x) (keyword? x) (symbol? x))
          "Agument must be a string, keyword, or symbol")
  (let [s (name x)]
    (if-let [m (re-matches nth-child-re s)]
      m
      (throw (ex-info
              "Selector must be either a keyword, string, or symbol." (str "Invalid value " (pr-str s)))))))

(defpseudoclass
  ^{:doc "CSS :nth-child pseudo class selector."} 
  nth-child [x]
  (if (number? x)
    (nth-x (str x "n"))
    (nth-x x)))

(defpseudoclass
  ^{:doc "CSS :nth-last-child pseudo class selector."}
  nth-last-child [x]
  (nth-x x))

(defpseudoclass
  ^{:doc "CSS :nth-of-type pseudo class selector."}
  nth-of-type [x]
  (nth-x x))

(defpseudoclass
  ^{:doc "CSS :nth-last-of-type pseudo class selector."}
  nth-last-of-type [x]
  (nth-x x))

;; ---------------------------------------------------------------------
;; Pseudo elements

(defpseudoelement
  ^{:doc "CSS ::after pseudo element selector."}
  after)

(defpseudoelement
  ^{:doc "CSS ::before pseudo element selector."}
  before)

(defpseudoelement
  ^{:doc "CSS ::first-letter pseudo element selector."}
  first-letter)

(defpseudoelement
  ^{:doc "CSS ::first-line pseudo element selector."}
  first-line)

;; ---------------------------------------------------------------------
;; Attribute selectors

;; SEE: http://www.w3.org/TR/selectors/#attribute-selectors

(defn attr
  ([attr-name]
   (selector (str \[ (name attr-name) \])))
  ([attr-name op attr-value]
   (let [v (name attr-value)
         ;; Wrap the value in quotes unless it's already
         ;; quoted to prevent emitting bad selectors. 
         v (if (re-matches #"\"(\\|[^\"])*\"|'(\\|[^\'])*'" v)
             v
             (pr-str v))]
     (selector (str \[ (name attr-name) (name op) v \])))))

(defn attr= [attr-name attr-value]
  (attr attr-name "=" attr-value))

(defn attr-contains [attr-name attr-value]
  (attr attr-name "~=" attr-value))

(defn attr-starts-with [attr-name attr-value]
  (attr attr-name "^=" attr-value))

;; TODO: This needs a better name.
(defn attr-starts-with* [attr-name attr-value]
  (attr attr-name "|=" attr-value))

(defn attr-ends-with [attr-name attr-value]
  (attr attr-name "$=" attr-value))

(defn attr-matches [attr-name attr-value]
  (attr attr-name "*=" attr-value))

;;----------------------------------------------------------------------
;; Selectors combinators

;; SEE: http://www.w3.org/TR/selectors/#combinators

(defn descendant
  "Descendant combinator."
  ([a b]
   (selector (str (css-selector a) " " (css-selector b))))
  ([a b & more]
   (->> (cons (descendant a b) more)
        (clojure.core/map css-selector)
        (string/join " ")
        (selector))))

(defn +
  "Adjacent sibling combinator."
  [a b]
  (selector (str (css-selector a) " + " (css-selector b))))

(defn -
  "General sibling combinator."
  [a b]
  (selector (str (css-selector a) " ~ " (css-selector b))))

(defn >
  "Child combinator."
  ([a]
   (selector a))
  ([a b]
   (selector (str (css-selector a) " > " (css-selector b))))
  ([a b & more]
   (->> (cons (> a b) more)
        (clojure.core/map css-selector)
        (string/join " > ")
        (selector))))

;; ---------------------------------------------------------------------
;; Special selectors

(defselector
  ^{:doc "Parent selector."}
  &)

;;----------------------------------------------------------------------
;; Specificity

;; SEE: http://www.w3.org/TR/selectors/#specificity

(defn- lex-specificity [s]
  (let [id-selector-re #"^\#[a-zA-Z][\w-]*"
        class-selector-re #"^\.[a-zA-Z][\w-]*"
        attribute-selector-re #"^\[[^\]]*\]"
        type-selector-re #"^[a-zA-Z][\w-]"
        pseudo-class-re #"^:[a-zA-Z][\w-]*(?:\([^\)]+\))?"
        pseudo-element-re #"^::[a-zA-Z][\w-]*"]
    (some
     (fn [[re k]]
       (if-let [m (re-find re s)]
         [m k]))
     [[id-selector-re :a]
      [class-selector-re :b]
      [attribute-selector-re :b]
      [pseudo-class-re :b]
      [type-selector-re :c]
      [pseudo-element-re :c]])))

(defn- specificity* [selector]
  (let [s (css-selector selector)
        score {:a 0 :b 0 :c 0}]
    (loop [s s, score score]
      (if (empty? s)
        score
        (if-let [[m k] (lex-specificity s)]
          ;; The negation pseudo class is a special case.
          (if-let [[_ inner] (re-find #"^:not\(([^\)]*)\)" m)]
            (recur (subs s (count m))
                   (merge-with clojure.core/+ score (specificity* inner)))
            (recur (subs s (count m)) (update-in score [k] inc)))
          (recur (subs s 1) score))))))

(defn specificity
  "Calculate a CSS3 selector's specificity.
  
  Example:

    (specificity \"#s12:not(FOO)\")
    ;; => 101
    (specificity (a hover))
    ;; => 10
  " 
  [selector]
  {:pre [(satisfies? ICSSSelector selector)]}
  (let [{:keys [a b c]} (specificity* selector)
        sv (string/replace (str a b c) #"^0*" "")]
    (if (empty? sv)
      0
      #?(:clj (Integer. sv)
         :cljs (js/parseInt sv)))))
