(ns garden.units
  "Functions and macros for working with CSS units."
  (:refer-clojure :exclude [rem])
  #?(:cljs
     (:require-macros
      [garden.units :refer [defunit]]))
  (:require
   [garden.types :as t]
   [garden.util :as util]
   #?(:cljs
      [cljs.reader :refer [read-string]]))
  #?(:clj
     (:import garden.types.CSSUnit)))

;;;; ## Unit families

(def length-units #{:in :cm :pc :mm :pt :px (keyword "%")})

(def angular-units #{:deg :grad :rad :turn})

(def time-units #{:s :ms})

(def frequency-units #{:Hz :kHz})

(def resolution-units #{:dpi :dpcm :dppx})

;;;; ## Unit predicates

(defn unit?
  "True if x is of type CSSUnit."
  [x]
  (instance? #?(:clj CSSUnit) #?(:cljs t/CSSUnit) x))

(defn length?
  [x]
  (and (unit? x)
       (contains? length-units (:unit x))))

(defn angle?
  [x]
  (and (unit? x)
       (contains? angular-units (:unit x))))

(defn time?
  [x]
  (and (unit? x)
       (contains? time-units (:unit x))))

(defn frequency?
  [x]
  (and (unit? x)
       (contains? frequency-units (:unit x))))

(defn resolution?
  [x]
  (and (unit? x)
       (contains? resolution-units (:unit x))))

;;;; ## Unit conversion

(def ^{:private true
       :doc "Map associating CSS unit types to their conversion values."}
  conversions
  {:in {:in 1, :cm 2.54, :pc 6, :mm 25.4, :pt 72, :px 96}
   :cm {:cm 1, :pc 2.36220473, :mm 10, :pt 28.3464567, :px 37.795275591}
   :pc {:pc 1, :mm 4.23333333, :pt 12, :px 16}
   :mm {:mm 1, :pt 2.83464567, :px 3.7795275591}
   :pt {:pt 1, :px 1.3333333333}
   :px {:px 1}
   :deg {:deg 1, :grad 1.111111111, :rad 0.0174532925, :turn 0.002777778}
   :grad {:grad 1, :rad 63.661977237, :turn 0.0025}
   :rad {:rad 1, :turn 0.159154943}
   :turn {:turn 1}
   :s {:s 1, :ms 1000}
   :ms {:ms 1}
   :Hz {:Hz 1, :kHz 0.001}
   :kHz {:kHz 1}
   :em {:em 1}
   :rem {:rem 1}
   (keyword "%") {(keyword "%") 1}})

(defn- convertable?
  "True if unit is a key of convertable-units, false otherwise."
  [unit]
  (contains? conversions unit))

(defn- convert
  "Convert a Unit with :unit left to a Unit with :unit right if possible."
  [{m :magnitude left :unit} right]
  (if (every? convertable? [left right])
    (let [v1 (get-in conversions [left right])
          v2 (get-in conversions [right left])]
      (cond
       v1 (t/CSSUnit. right (* v1 m))
       v2 (t/CSSUnit. right (/ m v2))
       ;; Both units are convertible but no conversion between them exists.
       :else (throw
              (ex-info
               (util/format "Can't convert %s to %s" (name left) (name right)) {}))))
    ;; Display the inconvertible unit.
    (let [x (first (drop-while convertable? [left right]))]
      (throw (ex-info (str "Inconvertible unit " (name x)) {})))))

;;;; ## Unit helpers

(def ^{:doc "Regular expression for matching a CSS unit. The magnitude
             and unit are captured."
       :private true}
  unit-re
  #"([+-]?\d+(?:\.?\d+)?)(p[xtc]|in|[cm]m|%|r?em|ex|ch|v(?:[wh]|m(?:in|ax))|deg|g?rad|turn|m?s|k?Hz|dp(?:i|cm|px))")

(defn read-unit
  "Read a `CSSUnit` object from the string `s`."
  [s]
  (when-let [[_ magnitude unit] (re-matches unit-re s)]
    (let [unit (keyword unit)
          magnitude (if magnitude (read-string magnitude) 0)]
      (t/CSSUnit. unit magnitude))))

(defn make-unit-predicate
  "Creates a function for verifying the given unit type."
  [unit]
  (fn [x] (and (unit? x) (= (:unit x) unit))))

(defn make-unit-fn
  "Creates a function for creating and converting `CSSUnit`s for the
  given unit. If a number n is passed the function it will produce a
  new `CSSUnit` record with a the magnitude set to n. If a `CSSUnit`
  is passed the function will attempt to convert it."
  [unit]
  (fn [x]
    (cond
     (number? x) (t/CSSUnit. unit x)
     (unit? x) (or (and (= (unit x) unit) x)
                   (convert x unit))
     :else (throw
            (ex-info
             (util/format "Don't know how to convert type %s to %s"
                     (.getName (type x))
                     (name unit)) {})))))

(defn make-unit-adder
  "Create a addition function for adding Units."
  [unit]
  (let [u (make-unit-fn unit)]
    (fn u+
      ([] (u 0))
      ([x] (u x))
      ([x y]
         (let [{m1 :magnitude} (u x)
               {m2 :magnitude} (u y)]
           (u (+ m1 m2))))
      ([x y & more]
         (reduce u+ (u+ x y) more)))))

(defn make-unit-subtractor
  "Create a subtraction function for subtracting Units."
  [unit]
  (let [u (make-unit-fn unit)]
    (fn u-
      ([x] (u (- x)))
      ([x y]
         (let [{m1 :magnitude} (u x)
               {m2 :magnitude} (u y)]
           (u (- m1 m2))))
      ([x y & more]
         (reduce u- (u- x y) more)))))

(defn make-unit-multiplier
  "Create a multiplication function for multiplying Units."
  [unit]
  (let [u (make-unit-fn unit)]
    (fn u*
      ([] (u 1))
      ([x] (u x))
      ([x y]
         (let [{m1 :magnitude} (u x)
               {m2 :magnitude} (u y)]
           (u (* m1 m2))))
      ([x y & more]
         (reduce u* (u* x y) more)))))

(defn make-unit-divider
  "Create a division function for dividing Units."
  [unit]
  (let [u (make-unit-fn unit)]
    (fn ud
      ([x] (u (/ 1 x)))
      ([x y]
         (let [{m1 :magnitude} (u x)
               {m2 :magnitude} (u y)]
           (u (/ m1 m2))))
      ([x y & more]
         (reduce ud (ud x y) more)))))

#?(:clj
   (defmacro defunit
     "Create a suite of functions for unit creation, conversion,
  validation, and arithmetic."
     ([name]
      `(defunit ~name ~name))
     ([name unit]
      (let [k (keyword unit)
            append #(symbol (str name %))]
        `(do
           (def ~name (make-unit-fn ~k))
           (def ~(append \?) (make-unit-predicate ~k))
           (def ~(append \+) (make-unit-adder ~k))
           (def ~(append \-) (make-unit-subtractor ~k))
           (def ~(append \*) (make-unit-multiplier ~k))
           (def ~(append "-div") (make-unit-divider ~k)))))))

(comment
  ;; This:
  (defunit px)
  ;; Is equivalent to:
  (def px  (make-unit-fn :px))
  (def px? (make-unit-predicate :px))
  (def px+ (make-unit-adder :px))
  (def px- (make-unit-subtractor :px))
  (def px* (make-unit-multiplier :px))
  (def px-div (make-unit-divider :px)))

;; # Predefined units

;; Absolute units

(defunit cm)
(defunit mm)
(defunit in)
(defunit px)
(defunit pt)
(defunit pc)
(defunit percent "%")

;; Font-relative units

(defunit em)
(defunit ex)
(defunit ch)
(defunit rem)

;; Viewport-percentage lengths

(defunit vw)
(defunit vh)
(defunit vmin)
(defunit vmax)

;; Angles

(defunit deg)
(defunit grad)
(defunit rad)
(defunit turn)

;; Times

(defunit s)
(defunit ms)

;; Frequencies

(defunit Hz)
(defunit kHz)

;; Resolutions

(defunit dpi)
(defunit dpcm)
(defunit dppx)
