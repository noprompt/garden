(ns garden.stylesheet.functions
  (:require [garden.def :refer [defcssfn]]))

(defcssfn url
  ;; Create a CSS `url` function.
  [url]
  url)

(defcssfn attr
  ;; Create a CSS attr function.
  ([name] name)
  ([name type-or-unit] [[name type-or-unit]])
  ([name type-or-unit fallback] [name [type-or-unit fallback]]))

(defcssfn toggle
  ;; Create CSS toggle function.
  ([value] value)
  ([value & more] (cons value more)))

