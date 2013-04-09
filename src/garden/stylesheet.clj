(ns garden.stylesheet
  (:refer-clojure :exclude [import])
  (:import java.net.URI)
  (:require [clojure.string :refer [join]]))

(defn css-fn [fn-name & args]
  (format "%s(%s)" (name fn-name) (join (#'garden.compiler/comma) args)))

(defn font-face
  "Make a CSS @font-face rule."
  [& declarations]
  (apply (partial conj ["@font-face"]) declarations))

(defn url
  "Make a CSS url function."
  [uri]
  (css-fn :url (URI. uri)))

(defn import
  "Make a CSS @import expression."
  ([stylesheet]
   (str "@import " stylesheet))
  ([stylesheet & media-types]
   (format "@import %s %s" stylesheet (join ", " (map name media-types)))))
