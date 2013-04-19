(ns ^{:doc "Utility functions for stylesheets."}
    garden.stylesheet
  (:refer-clojure :exclude [newline])
  (:require [garden.util :as u]
            [garden.compiler :refer [make-media-expression]])
  (:import java.net.URI
           garden.types.CSSFunction))

(defn font-family
  "Returns a font-family declaration for at least one font. Strings
   containing whitespace are automatically escaped."
  [font & fonts]
  (let [f (fn [x] (if (and (string? x)
                          (re-find #" " x))
                   (u/wrap-quotes x)
                   x))
        fonts (flatten (cons font fonts))]
    {:font-family [(map f fonts)]}))

;; http://dev.w3.org/csswg/css-values/#url
(defn url
  "Create CSS url function."
  [uri]
  (CSSFunction. :url (URI. uri)))

;; SEE: http://dev.w3.org/csswg/css-values/#attr
(defn attr
  "Create CSS attr function."
  ([attribute-name]
     (CSSFunction. :attr attribute-name))
  ([attribute-name type-or-unit]
     (attr [[attribute-name type-or-unit]]))
  ([attribute-name type-or-unit fallback]
     (attr [[attribute-name type-or-unit] fallback])))

;; http://dev.w3.org/csswg/css-values/#toggle-notation
(defn toggle
  "Create CSS toggle function."
  ([value]
     (CSSFunction. :toggle value))
  ([value & more]
     (toggle (cons value more))))

(defn at-font-face
  "Create a CSS @font-face rule."
  [& font-properties]
  ["@font-face" font-properties])

(defn at-import
  "Alpha - Subject to change.

   Create a CSS @import expression."
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

(declare at-media)
