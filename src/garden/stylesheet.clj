(ns garden.stylesheet
  (:refer-clojure :exclude [import])
  (:import java.net.URI)
  (:require [clojure.string :refer [join]]))

(defn css-fn [fn-name args]
  (format "%s(%s)" (name fn-name) (#'garden.compiler/comma-join args)))

(defn font-face
  "Make a CSS @font-face rule."
  [& declarations]
  (conj ["@font-face"] declarations))

(defn url
  "Make a CSS url function."
  [uri]
  (css-fn :url (URI. uri)))

(defn import
  "Make a CSS @import expression."
  ([stylesheet-path]
   (str "@import " stylesheet-path))
  ([stylesheet-path & media-types]
   (format
     "@import %s %s"
     stylesheet-path
     (join (#'garden.compiler/comma) (map name media-types)))))
