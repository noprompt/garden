(ns garden.types
  "Internal types used by Garden.")

(defrecord CSSUnit [unit magnitude])

(defrecord CSSFunction [f args])

(defrecord CSSAtRule [identifier value])
