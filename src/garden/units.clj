(ns garden.units
  (:refer-clojure :exclude [rem]))

(defrecord Unit [magnitude unit]
  Object
  (toString [this]
    (str (.magnitude this) (name (.unit this)))))

;;;; Unit conversion

(def ^:private
  convertable-units
  {:in  0  :cm   1  :pc  2 :mm   3 :pt 4 :px 5 ;; Absolute units
   :deg 6  :grad 7  :rad 8 :turn 9             ;; Angles
   :s   10 :ms   11                            ;; Times
   :Hz  12 :kHz  13                            ;; Frequencies
   })

;; Note: Typically, commas are avoided in sequences, but in this case they are
;; useful for displaying the table in a readable manor.
(def ^:private
  conversion-table
  ; in   , cm   , pc         , mm         , pt         , px           , deg , grad        , rad          , turn        , s   , ms   , Hz  , kHz
  [[1    , 2.54 , 6          , 25.4       , 72         , 96           , nil , nil         , nil          , nil         , nil , nil  , nil , nil]   ;; in
   [nil  , 1    , 2.36220473 , 10         , 28.3464567 , 37.795275591 , nil , nil         , nil          , nil         , nil , nil  , nil , nil]   ;; cm
   [nil  , nil  , 1          , 4.23333333 , 12         , 16           , nil , nil         , nil          , nil         , nil , nil  , nil , nil]   ;; pc
   [nil  , nil  , nil        , 1          , 2.83464567 , 3.7795275591 , nil , nil         , nil          , nil         , nil , nil  , nil , nil]   ;; mm
   [nil  , nil  , nil        , nil        , 1          , 1.3333333333 , nil , nil         , nil          , nil         , nil , nil  , nil , nil]   ;; pt
   [nil  , nil  , nil        , nil        , nil        , 1            , nil , nil         , nil          , nil         , nil , nil  , nil , nil]   ;; px
   [nil  , nil  , nil        , nil        , nil        , nil          , 1   , 1.111111111 , 0.0174532925 , 0.002777778 , nil , nil  , nil , nil]   ;; deg
   [nil  , nil  , nil        , nil        , nil        , nil          , nil , 1           , 63.661977237 , 0.0025      , nil , nil  , nil , nil]   ;; grad
   [nil  , nil  , nil        , nil        , nil        , nil          , nil , nil         , 1            , 0.159154943 , nil , nil  , nil , nil]   ;; rad
   [nil  , nil  , nil        , nil        , nil        , nil          , nil , nil         , nil          , 1           , nil , nil  , nil , nil]   ;; turn
   [nil  , nil  , nil        , nil        , nil        , nil          , nil , nil         , nil          , nil         , 1   , 1000 , nil , nil]   ;; s
   [nil  , nil  , nil        , nil        , nil        , nil          , nil , nil         , nil          , nil         , nil , 1    , nil , nil]   ;; ms
   [nil  , nil  , nil        , nil        , nil        , nil          , nil , nil         , nil          , nil         , nil , nil  , 1   , 0.001] ;; Hz
   [nil  , nil  , nil        , nil        , nil        , nil          , nil , nil         , nil          , nil         , nil , nil  , nil , 1]     ;; kHz
   ])

(defn- convertable?
  "True if unit is a key of convertable-units, false otherwise."
  [unit]
  (contains? convertable-units unit))

(defn- convert
  "Convert a Unit with :unit left to a Unit with :unit right if possible."
  [{m :magnitude left :unit} right]
  (if (every? convertable? [left right])
    (let [i (left convertable-units)
          j (right convertable-units)
          v1 (get-in conversion-table [i j])
          v2 (get-in conversion-table [j i])]
      (cond
        v1 (->Unit (* v1 m) right)
        v2 (->Unit (/ m v2) right)
        ;; Both units are convertible but no conversion between them exists.
        :else (throw
                (IllegalArgumentException.
                  (format "Can't convert %s to %s" (name left) (name right))))))
    ;; Display the inconvertible unit.
    (let [x (first (drop-while convertable? [left right]))]
      (throw (IllegalArgumentException. (str "Inconvertible unit " (name x)))))))

;;;; Unit helpers

(defn unit?
  "True if x is of type Unit."
  [x]
  (instance? Unit x))

(defn length?
  "True if x is a length Unit (in, cm, pc, mm, pt, or px)."
  [x]
  (boolean (and (unit? x) (#{:in :cm :pc :mm :pt :px} (:unit x)))))

(defn angle?
  "True if x is a angular Unit (deg, grad, rad, or turn)."
  [x]
  (boolean (and (unit? x) (#{:deg :grad :rad :turn} (:unit x)))))

(defn time?
  "True if x is a time Unit (s or ms)."
  [x]
  (boolean (and (unit? x) (#{:s :ms} (:unit x)))))

(defn frequency?
  "True if x is a frequency Unit (Hz or kHz)."
  [x]
  (boolean (and (unit? x) (#{:Hz :kHz} (:unit x)))))

(defn resolution?
  "True if x is a resolution Unit (dpi, dpcm, or dppx)."
  [x]
  (boolean (and (unit? x) (#{:dpi :dpcm :dppx} (:unit x)))))

(defn- make-unit-checker
  "Creates a function for verifying the given unit type."
  [unit]
  (fn [x] (and (unit? x) (= (:unit x) unit))))

(defn- make-unit-fn
  "Creates a function for creating and converting CSS units for the given
   unit. If a number n is passed to the function it will produce a new Unit
   record with a the magnitued set to n. If a Unit is passed the function
   will attempt to convert it."
  [unit]
  (fn [x]
    (cond
      (number? x) (->Unit x unit)
      (unit? x) (if (= (:unit x) unit)
                    x
                    (convert x unit))
      :else (throw
                 (IllegalArgumentException.
                   (format "Don't know how to convert type %s to %s"
                           (.getName (type x))
                           (name unit)))))))

(defn- make-unit-adder
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

(defn- make-unit-subtractor
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


(defn- make-unit-multiplier
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

(defn- make-unit-divider
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

(defmacro defunit
  "Create a suite of functions for unit creation, conversion, validation, and
   arithemetic."
  [unit]
  (let [k (keyword unit)
        unit? (symbol (str unit \?))
        unit+ (symbol (str unit \+))
        unit- (symbol (str unit \-))
        unit* (symbol (str unit \*))
        unit-div (symbol (str unit "-div"))]
    `(do
       (def ~unit (make-unit-fn ~k))
       (def ~unit? (make-unit-checker ~k))
       (def ~unit+ (make-unit-adder ~k))
       (def ~unit- (make-unit-subtractor ~k))
       (def ~unit* (make-unit-multiplier ~k))
       (def ~unit-div (make-unit-divider ~k)))))

(comment
  ; This:
  (defunit px)
  ; Is equivalent to
  (def px  (make-unit-fn :px))
  (def px? (make-unit-checker :px))
  (def px+ (make-unit-adder :px))
  (def px- (make-unit-subtractor :px))
  (def px* (make-unit-multiplier :px))
  (def px-div (make-unit-divider :px)))

;;;; Predefined units

;; Font-relative units

(defunit em)
(defunit ex)
(defunit ch)
;(defunit rem)

;; Viewport-percentage lengths

(defunit vw)
(defunit vh)
(defunit vmin)
(defunit vmax)

;; Absolute units

(defunit cm)
(defunit mm)
(defunit in)
(defunit px)
(defunit pt)
(defunit pc)

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

