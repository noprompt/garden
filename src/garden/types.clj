(ns garden.types
  "Internal types used by Garden."
  (:require [garden.util :refer [comma-join to-str]]))

(defrecord CSSUnit [magnitude unit]
  Object
  (toString [this]
    (let [m (when (ratio? magnitude)
              (float magnitude))]
      (str (if (ratio? magnitude)
             (float magnitude)
             magnitude)
           (name unit)))))

(defrecord CSSFunction [function args]
  Object
  (toString [this]
    (let [args (if (sequential? args)
                 (comma-join args)
                 (to-str args))]
      (format "%s(%s)" (to-str function) args))))

(defmethod print-method CSSUnit [unit writer]
  (.write writer (str unit)))

(defmethod print-method CSSFunction [function writer]
  (.write writer (str function)))
