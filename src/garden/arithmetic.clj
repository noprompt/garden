(ns garden.arithmetic
  "Generic arithmetic operators for computing sums, differences,
   products, and quotients between CSSUnits, CSSColors, and numbers."
  (:refer-clojure :exclude [+ - * /])
  (:require [garden.units :as u :refer [unit?]]
            [garden.color :as c :refer [color?]]))

;; The motivation for the functions in this namespace is the
;; contention that working with unit arithmetic functions (`px+`,
;; `px-`, etc.) and color arithmetic functions (`color+`, `color-`,
;; etc.) can at times be a bit unweildly. In some cases it would be
;; nice to have functions which could transparently perform unit and
;; color math without the verbosity.

;; Here, such functions are provided.

;; All operations favor `CSSUnit` and `CSSColor` types and fall back to
;; the standard `clojure.core` arithmetic functions. The preference for
;; this order stems from the assertion that it is far more likely unit
;; arithmetic will be performed in the context of a stylesheet versus
;; color or numeric.

(defn +
  "Generic addition operator. Transparently computes the sum of
  `CSSUnit`s,`CSSColor`s, and numbers."
  ([] 0)
  ([x] x)
  ([x y]
     (cond
      (unit? x) ((u/make-unit-adder (:unit x)) x y)
      (color? x) (c/color+ x y)
      :else (if (or (unit? y) (color? y))
              (+ y x)
              (clojure.core/+ x y))))
  ([x y & more]
     (reduce + (+ x y) more)))

(defn -
  "Generic subtraction operator. Transparently computes the difference
  between `CSSUnit`s, `CSSColor`s, and numbers."
  ([x]
     (cond
      (unit? x) (update-in x [:magnitude] clojure.core/-)
      ;; Colors shouldn't have negative semantics.
      (color? x) x
      :else (clojure.core/- x)))
  ([x y]
     (cond
      (unit? x) ((u/make-unit-subtractor (:unit x)) x y)
      (color? x) (c/color- x y)
      :else (cond 
             (unit? y) (let [{m :magnitude} y]
                         (assoc y :magnitude (clojure.core/- x m)))
             (color? y) (c/color- x y)
             :else (clojure.core/- x y))))
  ([x y & more]
     (reduce - (- x y) more))) 

(defn *
  "Generic multiplication operation. Transparently computes the product
  between `CSSUnit`s, `CSSColor`s, and numbers."
  ([] 1)
  ([x] x)
  ([x y]
     (cond
      (unit? x) ((u/make-unit-multiplier (:unit x)) x y)
      (color? x) (c/color* x y)
      :else (if (or (unit? y) (color? y))
              (* y x)
              (clojure.core/* x y))))
  ([x y & more]
     (reduce * (* x y) more)))

(defn /
  "Generic division operation. Transparently computes the quotient
  between `CSSUnit`s, `CSSColor`s, and numbers."
  ([x]
     (cond
      (unit? x) (update-in x [:magnitude] clojure.core//)
      (color? x) (c/color-div x)
      :else (clojure.core// x)))
  ([x y]
     (cond
      (unit? x) ((u/make-unit-divider (:unit x)) x y)
      (color? x) (c/color-div x y)
      :else (cond
             (unit? y) (let [{m :magnitude} y]
                         (assoc y :magnitude (clojure.core// x m)))
             (color? y) (c/color-div x y)
             :else (clojure.core// x y))))
  ([x y & more]
     (reduce / (/ x y) more)))
