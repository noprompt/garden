(ns garden.stylesheet
  "Utility functions for CSS properties, directives and functions."
  (:require [garden.util :as util]
            [garden.color :as color]
            [garden.types])
  (:import garden.types.CSSAtRule))

;; ## At-rules

(defn- at-rule [identifier value]
  (CSSAtRule. identifier value))

(defn at-font-face
  "Create a CSS @font-face rule."
  [& font-properties]
  ["@font-face" font-properties])

(defn at-import
  "Create a CSS @import rule."
  ([url]
     (at-rule :import {:url url
                       :media-queries nil}))
  ([url & media-queries]
     (at-rule :import {:url url
                       :media-queries media-queries})))

(defn at-media
  "Create a CSS @media rule."
  [media-queries & rules]
  (at-rule :media {:media-queries media-queries
                   :rules rules}))

(defn at-keyframes
  "Create a CSS @keyframes rule."
  [identifier & frames]
  (at-rule :keyframes {:identifier identifier
                       :frames frames}))

;;;; Functions

(defn rgb
  "Create a color from RGB values."
  [r g b]
  (color/rgb [r g b]))

(defn hsl
  "Create a color from HSL values."
  [h s l]
  (color/hsl [h s l]))
