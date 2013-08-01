(ns garden.types
  "Internal types used by Garden."
  (:require [garden.util :refer [comma-join to-str]]))

(defrecord CSSFunction [function args]
  Object
  (toString [this]
    (let [args (if (sequential? args)
                 (comma-join args)
                 (to-str args))]
      (format "%s(%s)" (to-str function) args))))

(defrecord CSSImport [url media-expr])

(defrecord CSSKeyframes [identifier frames])

(defmethod print-method CSSFunction [function writer]
  (.write writer (str function)))
