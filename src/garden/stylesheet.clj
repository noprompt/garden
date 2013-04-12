(ns garden.stylesheet
  (:import java.net.URI)
  (:require [clojure.string :refer [join]]
            [garden.util :refer :all]
            [garden.compiler :refer [comma-join make-media-expression]]))

(defrecord CSSFunction [function args]
  Object
  (toString [this]
    (let [args (if (sequential? args)
                 (comma-join args)
                 (to-str args))]
      (format "%s(%s)" (to-str function) args))))

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
                             (wrap-quotes uri))))
  ([uri & media-exprs]
     (let [exprs (for [expr media-exprs]
                   (if (map? expr)
                     (make-media-expression expr)
                     (to-str expr)))]
       (format "@import %s %s;"
               (if (:function uri)
                 uri
                 (wrap-quotes uri))
               (comma-join exprs)))))

(declare at-media)
