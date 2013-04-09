(ns garden.core
  (:require [garden.compiler :refer [with-output-style compile-css]]))

(defmacro css
  [options & rules]
  (if-let [output-style (and (map? options) (:output-style options))]
    `(with-output-style ~output-style
       (compile-css ~@rules))
     `(compile-css ~options ~@rules)))

