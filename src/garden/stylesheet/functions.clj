(ns garden.stylesheet.functions
  (:import java.net.URI
           garden.types.CSSFunction))

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

