(ns ^{:doc "Library for rendering Clojure data structures as CSS."
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
