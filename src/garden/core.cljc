(ns garden.core
  "Convert Clojure data structures to CSS."
  (:require [garden.compiler :as compiler]))

(defn ^String css
  "Convert a variable number of Clojure data structure to a string of
  CSS. The first argument may be a list of flags for the compiler."
  {:arglists '([rules] [flags? rules])}
  [& rules]
  (apply compiler/compile-css rules))

(defn ^String style
  "Convert a variable number of maps into a string of CSS for use with
  the HTML `style` attribute."
  [& maps]
  (compiler/compile-style maps))

