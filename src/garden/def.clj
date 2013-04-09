(ns garden.def)

(defmacro defselector [name & selector]
  (let [s (fn [[& sel] & children]
            (into (vec sel) children))]
    `(def ~name (partial ~s '~selector))))
