(ns garden.stylesheet
  "Utility functions for CSS properties, directives and functions."
  (:require [garden.util :as util]
            [garden.color :as color]
            [garden.types])
  (:import (garden.types CSSImport
                         CSSKeyframes
                         CSSMediaQuery)))

;; ## At-rules

(defn at-font-face
  "Create a CSS @font-face rule."
  [& font-properties]
  ["@font-face" font-properties])

(defn at-import
  "Create a CSS @import expression."
  ([url] (CSSImport. url nil))
  ([url media-expr] (CSSImport. url media-expr)))

(defn at-media
  "Create a CSS @media query."
  [media-expression & children]
  (CSSMediaQuery. media-expression children))

(defn at-keyframes [identifier & frames]
  (CSSKeyframes. identifier frames))

;;;; Functions

(defn rgb
  "Create a color from RGB values."
  [r g b]
  (color/rgb [r g b]))

(defn hsl
  "Create a color from HSL values."
  [h s l]
  (color/hsl [h s l]))
