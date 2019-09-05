(ns garden.units.alef
  "Utilities for working with units."
  (:refer-clojure :exclude [rem + * - /])
  #?@(:clj
      [(:require
        [clojure.core :as clj]
        [clojure.string :as string])
       (:import
        (clojure.lang Keyword))]
      :cljs
      [(:require
        [cljs.core :as clj]
        [clojure.string :as string])
       (:require-macros
        [garden.units.alef :refer [defunit]])]))

;; ---------------------------------------------------------------------
;; Protocols

(defprotocol IMagnitude
  (-magnitude [this]))

(defprotocol IMeasurement
  (-measurement [this]))

(defprotocol IUnit
  (-unit [this]))


;; ---------------------------------------------------------------------
;; Types

(defrecord Unit [magnitude measurement]
  IUnit
  (-unit [this] this)

  IMeasurement
  (-measurement [this]
    (if-let [m (.-measurement this)]
      (keyword m)))

  IMagnitude
  (-magnitude [this]
    (.-magnitude this)))


(defn unit?
  "true if `x` is an instance of `Unit`, false otherwise."
  [x]
  (instance? Unit x))


;; ---------------------------------------------------------------------
;; Functions

(defn magnitude
  "Return the magnitude of x. x must satisfy IMeasurement."
  [x]
  {:post [(number? %)
          #?@(:clj [(not= % Double/POSITIVE_INFINITY)
                    (not= % Double/NEGATIVE_INFINITY)
                    (not= % Double/NaN)
                    (not= % Float/NaN)])]}
  (-magnitude x))

(defn measurement
  "Return the measurement type of x. x must satisfy IMeasurement."
  [x]
  (when-let [m (-measurement x)]
    (keyword m)))

(defn unit
  "Return x as an instance of Unit. x must satisfy IUnit."
  [x]
  {:post [(instance? Unit %)]}
  (-unit x))


#?(:clj
   (defmacro defunit
     "Define a unit constructor function named sym."
     ([sym]
      (let [measurement (keyword sym)]
        `(defn ~sym
           {:arglists '~'([n])}
           [x#]
           (let [mg# (magnitude x#)
                 ms# (measurement x#)
                 i# (convert mg# ms# ~measurement)]
             (Unit. i# ~measurement)))))
     ([sym unit-name]
      (let [measurement (keyword unit-name)]
        `(defn ~sym
           {:arglists '~'([n])}
           [x#]
           (let [mg# (magnitude x#)
                 ms# (measurement x#)
                 i# (convert mg# ms# ~measurement)]
             (Unit. i# ~measurement)))))))


;;; Conversion

(def
  ^{:private true
    :doc "Map associating unit types to their conversion values."}
  conversion-table
  (atom {}))

(defn add-conversion!
  "Add a bidirection measurement conversion between to units m1 and m2
  with amount amt. amt represents one unit of m1 with respect to m2."
  [m1 m2 amt]
  (let [m1k (keyword m1)
        m2k (keyword m2)]
    (swap! conversion-table
           (fn [ct]
             (-> ct
                 (assoc-in [m1k m2k] amt)
                 (assoc-in [m2k m1k] (clj// 1.0 amt)))))))

(defn get-conversion
  "Return the conversion amount of one unit of m1 with respect to
  m2. If either m1 or m2 or both are nil the result will be 1."
  [m1 m2]
  (if (or (= m1 m2)
          (nil? m1)
          (nil? m2))
    1
    (get-in @conversion-table [m1 m2])))

(defn convert
  "Convert an amount amt of measurement m1 to and amount of
  measurement m2. If either m1 or m2 or both are nil result will be
  equal to amt.


  Example:

    (convert 2 :in :px)
    => 192

    (convert 1 :px :in)
    => 0.010416666666666666

    (convert 42 nil :px)
    => 42

    (convert 42 nil nil)
    => 42"
  [amt m1 m2]
  (if-let [c (get-conversion m1 m2)]
    (clj/* c amt)
    (throw (ex-info (str "Unable to convert measurement "
                         (pr-str m1)
                         " to "
                         (pr-str m2))
                    {:given [amt m1 m2]
                     :expected
                     (let [candidates (-> (get @conversion-table m1)
                                          (keys)
                                          (set))]
                       `(~'contains? ~candidates ~m2))}))))

;;; Arithemetic

(defn ^Unit +
  "Return the sum of units. The leftmost summand with a non-nil unit
  of measurement determines the resulting unit's measurement value. If
  none of the summands have a measurement value the resulting unit
  will be without measurement.

  Example:

    (+)
    => #garden.units.Unit{:magnitude 0, :measurement nil}

    (+ 1)
    => #garden.units.Unit{:magnitude 1, :measurement nil}

    (+ 1 (px 5))
    => #garden.units.Unit{:magnitude 6, :measurement :px}

    (+ (cm 5) (in 5) (mm 5))
    => #garden.units.Unit{:magnitude 18.2, :measurement :cm}"
  ([]
   (unit 0))
  ([u]
   (unit u))
  ([u1 u2]
   (let [mg1 (magnitude u1)
         mg2 (magnitude u2)
         ms1 (or (measurement u1)
                 (measurement u2))
         ms2 (or (measurement u2)
                 (measurement u1))
         mg3 (convert mg2 ms2 ms1)]
     (Unit. (clj/+ mg1 mg3) (or ms1 ms2))))
  ([u1 u2 & more]
   (reduce + (+ u1 u2) more)))

(defn ^Unit *
  "Return the product of units. The leftmost multiplicand with a
  non-nil unit of measurement determines the resulting unit's
  measurement value. If none of the multiplicands have a measurement
  value the resulting unit will be without measurement.

  Example:

    (*)
    => #garden.units.Unit{:magnitude 1, :measurement nil}

    (* 1)
    => #garden.units.Unit{:magnitude 1, :measurement nil}

    (* 1 (px 5))
    => #garden.units.Unit{:magnitude 5, :measurement :px}

    (* (cm 5) (in 5) (mm 5))
    => #garden.units.Unit{:magnitude 31.75, :measurement :cm}"
  ([]
   (unit 1))
  ([u]
   (unit u))
  ([u1 u2]
   (let [mg1 (magnitude u1)
         mg2 (magnitude u2)
         ms1 (or (measurement u1)
                 (measurement u2))
         ms2 (or (measurement u2)
                 (measurement u1))
         mg3 (convert mg2 ms2 ms1)]
     (Unit. (clj/* mg1 mg3) (or ms1 ms2))))
  ([u1 u2 & more]
   (reduce * (* u1 u2) more)))

(defn ^Unit -
  "Return the difference of units. The leftmost minuend or subtrahend
  with a non-nil unit of measurement determines the resulting unit's
  measurement value. If neither minuend or subtrahends have a
  measurement value the resulting unit will be without measurement.


  Example:

    (- 1)
    => #garden.units.Unit{:magnitude -1, :measurement nil}

    (- (px 1))
    => #garden.units.Unit{:magnitude -1, :measurement :px}

    (- 1 (px 5))
    => #garden.units.Unit{:magnitude -4, :measurement :px}

    (- (cm 5) (in 5) (mm 5))
    => #garden.units.Unit{:magnitude -8.2, :measurement :cm}"
  ([u]
   (update-in (unit u) [:magnitude] clj/-))
  ([u1 u2]
   (let [mg1 (magnitude u1)
         mg2 (magnitude u2)
         ms1 (or (measurement u1)
                 (measurement u2))
         ms2 (or (measurement u2)
                 (measurement u1))
         mg3 (convert mg2 ms2 ms1)]
     (Unit. (clj/- mg1 mg3) (or ms1 ms2))))
  ([u1 u2 & more]
   (reduce - (- u1 u2) more)))

(defn ^Unit /
  "Return the quotient of units. The leftmost dividend or divisor with
  a non-nil unit of measurement determines the resulting unit's
  measurement value. If neither dividend or divisors have a
  measurement value the resulting unit will be without measurement.

  Example:

    (/ 1)
    => #garden.units.Unit{:magnitude 1, :measurement nil}

    (/ (px 1))
    => #garden.units.Unit{:magnitude 1, :measurement :px}

    (/ 1 (px 5))
    => #garden.units.Unit{:magnitude 1/5, :measurement :px}

    (/ (cm 5) (in 5) (mm 5))
    => #garden.units.Unit{:magnitude 0.7874015748031497, :measurement :cm}"
  ([u]
   (update-in (unit u) [:magnitude] clj//))
  ([u1 u2]
   (let [mg1 (magnitude u1)
         mg2 (magnitude u2)
         ms1 (or (measurement u1)
                 (measurement u2))
         ms2 (or (measurement u2)
                 (measurement u1))
         mg3 (convert mg2 ms2 ms1)]
     (Unit. (clj// mg1 mg3) (or ms1 ms2))))
  ([u1 u2 & more]
   (reduce / (/ u1 u2) more)))

;; ---------------------------------------------------------------------
;; Predefined units

;;; Absolute units

(defunit cm)
(defunit mm)
(defunit in)
(defunit px)
(defunit pt)
(defunit pc)
(defunit percent "%")

;;; Font-relative units

(defunit em)
(defunit ex)
(defunit ch)
(defunit rem)

;;; Viewport-percentage lengths

(defunit vw)
(defunit vh)
(defunit vmin)
(defunit vmax)

;;; Angles

(defunit deg)
(defunit grad)
(defunit rad)
(defunit turn)

;;; Times

(defunit s)
(defunit ms)

;;; Frequencies

(defunit Hz)
(defunit kHz)

;;; Resolutions

(defunit dpi)
(defunit dpcm)
(defunit dppx)


;; ---------------------------------------------------------------------
;; Predefined conversions

;;; Absolute units

(add-conversion! :cm :mm 10)
(add-conversion! :cm :pc 2.36220473)
(add-conversion! :cm :pt 28.3464567)
(add-conversion! :cm :px 37.795275591)
(add-conversion! :in :cm 2.54)
(add-conversion! :in :mm 25.4)
(add-conversion! :in :pc 6)
(add-conversion! :in :pt 72)
(add-conversion! :in :px 96)
(add-conversion! :mm :pt 2.83464567)
(add-conversion! :mm :px 3.7795275591)
(add-conversion! :pc :mm 4.23333333)
(add-conversion! :pc :pt 12)
(add-conversion! :pc :px 16)
(add-conversion! :pt :px 1.3333333333)

;;; Angles

(add-conversion! :deg :grad 1.111111111)
(add-conversion! :deg :rad 0.0174532925)
(add-conversion! :deg :turn 0.002777778)
(add-conversion! :grad :rad 63.661977237)
(add-conversion! :grad :turn 0.0025)
(add-conversion! :rad :turn 0.159154943)

;;; Times

(add-conversion! :s :ms 1000)

;;; Frequencies

(add-conversion! :Hz :kHz 0.001)


;; ---------------------------------------------------------------------
;; Protocol implementation

#?(:clj
   (extend-type Number
     IUnit
     (-unit [this]
       (Unit. this nil))

     IMagnitude
     (-magnitude [this] this)

     IMeasurement
     (-measurement [this] nil))

   :cljs
   (extend-type number
     IUnit
     (-unit [this]
       (Unit. this nil))

     IMagnitude
     (-magnitude [this] this)

     IMeasurement
     (-measurement [this] nil)))

#?(:clj
   (extend-type clojure.lang.Ratio
     IUnit
     (-unit [this]
       (Unit. this nil))

     IMagnitude
     (-magnitude [this] this)

     IMeasurement
     (-measurement [this] nil)))

;;; String

(def
  ^{:private true
    :doc "Regular expression for matching a CSS unit. The magnitude
  and unit are captured."}
  unit-re
  #"([+-]?\d+(?:\.?\d+(?:[eE][+-]?\d+)?)?)(p[xtc]|in|[cm]m|%|r?em|ex|ch|v(?:[wh]|m(?:in|ax))|deg|g?rad|turn|m?s|k?Hz|dp(?:i|cm|px))?")

(defn ^Unit parse-unit [s]
  (let [s' (string/trim s)]
    (if-let [[_ magnitude measurement] (re-matches unit-re s')]
      (let [magnitude #?(:clj
                         (if (.contains magnitude ".")
                           (Double/parseDouble magnitude)
                           (Long/parseLong magnitude))
                         :cljs
                         (js/parseFloat magnitude))
            measurement (and measurement
                             (keyword measurement))]
        (Unit. magnitude measurement))
      (throw (ex-info (str "Unable to convert " (pr-str s) " to Unit")
                      {:given s
                       :expected
                       `(~'re-matches ~unit-re ~s)})))))

#?(:clj
   (extend-type String
     IUnit
     (-unit [this]
       (parse-unit this))

     IMagnitude
     (-magnitude [this]
       (.-magnitude ^Unit (parse-unit this)))

     IMeasurement
     (-measurement [this]
       (.-measurement ^Unit (parse-unit this))))

   :cljs
   (extend-type string
     IUnit
     (-unit [this]
       (parse-unit this))

     IMagnitude
     (-magnitude [this]
       (.-magnitude ^Unit (parse-unit this)))

     IMeasurement
     (-measurement [this]
       (.-measurement ^Unit (parse-unit this)))))


;;; Keyword

(extend-type Keyword
  IUnit
  (-unit [this]
    (parse-unit (name this)))

  IMagnitude
  (-magnitude [this]
    (.-magnitude ^Unit (parse-unit (name this))))

  IMeasurement
  (-measurement [this]
    (.-measurement ^Unit (parse-unit (name this)))))
