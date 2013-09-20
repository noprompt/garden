(ns garden.types
  "Internal types used by Garden.")

(defrecord CSSUnit [unit magnitude])

(defrecord CSSFunction [function args])

(defrecord CSSImport [url media-expr])

(defrecord CSSKeyframes [identifier frames])

(defrecord CSSMediaQuery [expression children])
