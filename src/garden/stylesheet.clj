(ns ^{:doc "Utility functions for CSS properties, directives and
            functions."}
    garden.stylesheet
  (:require [garden.util :as util]
            [garden.units :as unit]
            [garden.compiler :refer [make-media-expression]])
  (:import java.net.URI
           garden.types.CSSFunction))

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

;; # Functions

;; ## Standard functions

(defn url
  "Create CSS url function."
  [uri]
  (CSSFunction. :url (URI. uri)))

(defn attr
  "Create CSS attr function."
  ([attribute-name]
     (CSSFunction. :attr attribute-name))
  ([attribute-name type-or-unit]
     (attr [[attribute-name type-or-unit]]))
  ([attribute-name type-or-unit fallback]
     (attr [[attribute-name type-or-unit] fallback])))

(defn toggle
  "Create CSS toggle function."
  ([value]
     (CSSFunction. :toggle value))
  ([value & more]
     (toggle (cons value more))))

;; ## Filter functions

(defmacro ^:private defpercentage-filter
  "Define a CSS filter function which takes a number or percentage value."
  [name]
  `(defn ~name [v#]
     (if (or (number? v#)
             (unit/percent? (unit/read-unit v#)))
       (CSSFunction. (keyword '~name) v#)
       (throw
        (IllegalArgumentException. "value must be a number or CSS percentage")))))

(defpercentage-filter grayscale)
(defpercentage-filter sepia)
(defpercentage-filter saturate)
(defpercentage-filter invert)
(defpercentage-filter opacity)
(defpercentage-filter brightness)
(defpercentage-filter contrast)

(defn blur
  "Create a CSS blur function with radius v. v must be a non-percentage
   CSS length."
  [v]
  (let [u (unit/read-unit v)]
    (if (and (unit/length? u)
             (not (unit/percent? u)))
      (CSSFunction. :blur u)
      (throw
       (IllegalArgumentException. "value must be a non-percentage CSS length")))))

(defn hue-rotate
  "Create a CSS hue-rotate function. v must be a CSS angular unit."
  [v]
  (if (unit/angle? (unit/read-unit v))
    (CSSFunction. :hue-rotate v)
    (throw
     (IllegalArgumentException. "value must be a CSS angular unit"))))

;; This function currently does not validate if the inputs are correct.
(defn drop-shadow
  "Create a CSS drop-shadow function."
  [offset-x offset-y & [a1 a2 a3]]
  (let [args (filter (complement nil?) [a1 a2 a3])]
    (CSSFunction. :drop-shadow (apply vector offset-x offset-y args))))

