(ns garden.types
  "Internal types used by Garden."
  (:require [garden.util :as util]))

(defrecord CSSFunction [function args])

(defmethod print-method CSSFunction [cssfn writer]
  (let [args (:args cssfn)]
    (.write writer
            (format "%s(%s)"
                    (util/to-str (:function cssfn))
                    (if (sequential? args)
                      (util/comma-join (map util/to-str args))
                      args)))))

(defrecord CSSImport [url media-expr])

(defrecord CSSKeyframes [identifier frames])

