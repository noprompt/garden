(ns garden.stylesheet.functions.filters
  (:require [garden.units :as unit])
  (:import garden.types.CSSFunction))

(defmacro ^:private defpercentage-filter
  "Define a CSS filter function which takes a number or percentage value."
  [name]
  `(defn ~name [v#]
     (if (or (number? v#)
             (unit/percent? (unit/read-unit v#)))
       (CSSFunction. (keyword '~name) v#)
       (throw
        (IllegalArgumentException. "value must be a number or CSS percentage")))))

(defpercentage-filter grayscale)
(defpercentage-filter sepia)
(defpercentage-filter saturate)
(defpercentage-filter invert)
(defpercentage-filter opacity)
(defpercentage-filter brightness)
(defpercentage-filter contrast)

(defn blur
  "Create a CSS blur function with radius v. v must be a non-percentage
   CSS length."
  [v]
  (let [u (unit/read-unit v)]
    (if (and (unit/length? u)
             (not (unit/percent? u)))
      (CSSFunction. :blur u)
      (throw
       (IllegalArgumentException. "value must be a non-percentage CSS length")))))

(defn hue-rotate
  "Create a CSS hue-rotate function. v must be a CSS angular unit."
  [v]
  (if (unit/angle? (unit/read-unit v))
    (CSSFunction. :hue-rotate v)
    (throw
     (IllegalArgumentException. "value must be a CSS angular unit"))))

;; This function currently does not validate if the inputs are correct.
(defn drop-shadow
  "Create a CSS drop-shadow function."
  [offset-x offset-y & [a1 a2 a3]]
  (let [args (filter (complement nil?) [a1 a2 a3])]
    (CSSFunction. :drop-shadow (apply vector offset-x offset-y args))))
