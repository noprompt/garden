(ns garden.core
  "Convert Clojure data structures to CSS."
  (:require [garden.compiler]))

(defn ^String css
  "Convert a variable number of Clojure data structure to a string of
  CSS. The first argument may be a list of flags for the compiler."
  {:arglists '([& rules] [flags & rules])}
  [& xs]
  (let [potential-options (first xs)
        options (when (and (map? potential-options)
                           (not (record? potential-options)))
                  potential-options)
        rules (if options
                (rest xs)
                xs)
        options (or options {})]
    (garden.compiler/compile-css options rules)))

(defn ^String style
  "Convert a variable number of maps into a string of CSS for use with
  the HTML `style` attribute."
  [& maps]
  {:pre [(not (empty? maps))]}
  (garden.compiler/compile-style maps))

(defn vdom-style
  "Convert a variable number of maps into a map for use with the a virtual
  DOM `style` attribute."
  [& maps]
  {:pre [(not (empty? maps))]}
  (garden.compiler/compile-vdom-style maps))
