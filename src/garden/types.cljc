(ns garden.types
  "Internal types used by Garden.")

(defrecord CSSFunction [function args])

(defrecord CSSAtRule [identifier value])
