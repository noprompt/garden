(ns ^{:doc "Library for converting Clojure data structures to CSS."
      :author "Joel Holdbrooks"}
  garden.core
  (:require [garden.compiler :refer [with-output-style compile-css]]))

(defmacro css
  "Convert a Clojure data structure to a string of CSS."
  [options & rules]
  (if-let [output-style (and (map? options) (:output-style options))]
    `(with-output-style ~output-style
      (compile-css ~@rules))
    `(compile-css ~options ~@rules)))
