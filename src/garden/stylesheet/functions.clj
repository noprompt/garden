(ns garden.stylesheet.functions
  "CSS functions."
  (:require [garden.def :refer [defcssfn]]))

(defcssfn url
  "Create a CSS `url` function."
  [url] url)

(defcssfn attr
  "Create a CSS attr function."
  ([name] name)
  ([name type-or-unit]
     [[name type-or-unit]])
  ([name type-or-unit fallback]
     [name [type-or-unit fallback]]))

(defcssfn toggle
  "Create CSS toggle function."
  ([value] value)
  ([value & more] (cons value more)))

;;;; Filters

(defcssfn blur
  "Create a CSS blur function."
  [value] value)

(defcssfn brightness
  "Create a CSS brightness function."
  [value] value)

(defcssfn contrast
  "Create a CSS contrast function."
  [value] value)

(defcssfn drop-shadow
  ([offset-x offset-y]
     [[offset-x offset-y]])
  ([offset-x offset-y & [a1 a2 a3]]
     (->> (remove nil? [a1 a2 a3])
          (into [offset-x offset-y])
          (vector))))

(defcssfn grayscale
  "Create a CSS grayscale function."
  [value] value)

(defcssfn hue-rotate
  "Create a CSS hue-rotate function."
  [value] value)

(defcssfn invert
  "Create a CSS invert function."
  [value] value)

(defcssfn opacity
  "Create a CSS opacity function."
  [value] value)

(defcssfn sepia
  "Create a CSS sepia function."
  [value] value)

(defcssfn saturate
  "Create a CSS saturate function."
  [value] value)
