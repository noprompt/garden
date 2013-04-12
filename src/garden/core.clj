(ns ^{:doc "Library for rendering Clojure data structures as CSS."
      :author "Joel Holdbrooks"}
  garden.core
  (:require [garden.compiler :refer [with-output-style compile-css]]))

(defmacro css
  "Convert a Clojure data structures to a string of CSS."
  [options & rules]
  (if-let [output-style (and (map? options) (:output-style options))]
    `(with-output-style ~output-style
       (compile-css ~@rules))
     `(compile-css ~options ~@rules)))

