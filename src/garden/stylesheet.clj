(ns garden.stylesheet
  "Utility functions for CSS properties, directives and functions."
  (:require [garden.util :as u]
            [garden.units :as un]
            [garden.color :as c]
            [garden.compiler :refer [compile-css make-media-expression]]))

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
  ([uri]
     (format "@import %s;" (if (:function uri)
                             uri
                             (u/wrap-quotes uri))))
  ([uri & media-exprs]
     (let [exprs (for [expr media-exprs]
                   (if (map? expr)
                     (make-media-expression expr)
                     (u/to-str expr)))]
       (format "@import %s %s;"
               (if (:function uri)
                 uri
                 (u/wrap-quotes uri))
               (u/comma-join exprs)))))

(defn at-media
  "Wraps the given rules with meta given by `expr`."
  [expr rule & rules]
  (with-meta (cons rule rules) expr))

(defn at-keyframes
  "Create CSS at-rule(s) for `anim-name`."
  ([at-names anim-name rules]
    (let [render #(str "@" (u/to-str %) " " (u/to-str anim-name) " "
                       (u/left-brace) (compile-css rules) (u/right-brace))]
    (reduce str (map render at-names))))
  ([anim-name rules] (at-keyframes ["keyframes"] anim-name rules)))

;;;; Functions

(defn rgb
  "Create a color from RGB values."
  [r g b]
  (c/rgb [r g b]))

(defn hsl
  "Create a color from HSL values."
  [h s l]
  (c/hsl [h s l]))
