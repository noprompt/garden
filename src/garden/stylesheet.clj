(ns garden.stylesheet
  "Utility functions for CSS properties, directives and functions."
  (:require [garden.util :as u]
            [garden.units :as un]
            [garden.color :as c]
            garden.types)
  (:import (garden.types CSSImport
                         CSSKeyframes)))

;;;; Properties

(defn font-family
  "Return a font-family declaration for at least one font. Strings
   containing whitespace are automatically escaped."
  [font & fonts]
  (let [f (fn [x]
            (if (and (string? x)
                     (re-find #" " x))
              (u/wrap-quotes x)
              x))
        fonts (flatten (cons font fonts))]
    {:font-family [(map f fonts)]}))

;;;; Directives

(defn at-font-face
  "Create a CSS @font-face rule."
  [& font-properties]
  ["@font-face" font-properties])

(defn at-import
  "Create a CSS @import expression."
  ([url] (CSSImport. url nil))
  ([url media-expr] (CSSImport. url media-expr)))

(defn at-media
  "Wrap the given rules with meta given by `expr`."
  [expr & rules]
  (with-meta rules {:media expr}))

(defn at-keyframes [identifier & frames]
  (CSSKeyframes. identifier frames))

;;;; Functions

(defn rgb
  "Create a color from RGB values."
  [r g b]
  (c/rgb [r g b]))

(defn hsl
  "Create a color from HSL values."
  [h s l]
  (c/hsl [h s l]))
