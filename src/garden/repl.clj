(ns garden.repl
  "Method definitions for `print-method` with Garden types."
  (:require [garden.util :as util :refer [ToString to-str]])
  (:import garden.types.CSSUnit
           garden.types.CSSFunction))

(extend-protocol ToString
  CSSUnit
  (to-str [{:keys [magnitude unit]}]
    (let [magnitude (if (ratio? magnitude)
                    (float magnitude)
                    magnitude)]
    (str (if (zero? magnitude) 0 magnitude)
         (when-not (zero? magnitude) (name unit)))))

  CSSFunction
  (to-str [{:keys [function args]}]
    (format "%s(%s)"
            (util/to-str function)
            (if (sequential? args)
              (util/comma-join args)
              args))))

(defmethod print-method CSSUnit [unit writer]
  (.write writer (to-str unit)))

(defmethod print-method CSSFunction [cssfn writer]
  (.write writer (to-str cssfn)))
