(ns ^{:doc "Library for converting Clojure data structures to CSS."
      :author "Joel Holdbrooks"}
  garden.core
  (:require [garden.util :refer [with-output-style]]
            [garden.compiler :refer [compile-css]]))

(defmacro css
  "Convert a Clojure data structure to a string of CSS."
  [options & rules]
  (if-let [output-style (and (map? options) (:output-style options))]
    `(with-output-style ~output-style
      (compile-css ~@rules))
    `(compile-css ~options ~@rules)))
