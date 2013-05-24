(ns ^{:doc "Utility functions for CSS properties, directives and
            functions."}
    garden.stylesheet
  (:require [garden.util :as util]
            [garden.units :as unit]
            [garden.compiler :refer [make-media-expression]])
  (:refer-clojure :exclude [empty]))

;; # Properties

(defn font-family
  "Returns a font-family declaration for at least one font. Strings
   containing whitespace are automatically escaped."
  [font & fonts]
  (let [f (fn [x] (if (and (string? x)
                          (re-find #" " x))
                   (util/wrap-quotes x)
                   x))
        fonts (flatten (cons font fonts))]
    {:font-family [(map f fonts)]}))

;; # Directives

(defn at-font-face
  "Create a CSS @font-face rule."
  [& font-properties]
  ["@font-face" font-properties])

(defn at-import
  "Create a CSS @import expression."
  ([uri]
     (format "@import %s;" (if (:function uri)
                             uri
                             (util/wrap-quotes uri))))
  ([uri & media-exprs]
     (let [exprs (for [expr media-exprs]
                   (if (map? expr)
                     (make-media-expression expr)
                     (util/to-str expr)))]
       (format "@import %s %s;"
               (if (:function uri)
                 uri
                 (util/wrap-quotes uri))
               (util/comma-join exprs)))))

(defn at-media
  "Wraps the given rules with meta given by `expr`."
  [expr rule & rules]
  (with-meta (cons rule rules) expr))

(declare at-keyframes)

