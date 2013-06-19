(ns garden.color
  "Utilities for color creation, conversion, and manipulation."
  (:refer-clojure :exclude [complement])
  (:require [clojure.string :as s]
            [garden.util :as u]))

;; Many of the functions in this namespace were ported or inspired by
;; the implementations included with Sass
;; (http://sass-lang.com/docs/yardoc/Sass/Script/Functions.html).
;; Some additional functions have been added such as `triad` and
;; `tetrad` for generating sets of colors. 

;; Converts a color to a hexadecimal string (implementation below). 
(declare as-hex)

(defrecord CSSColor [red green blue hue saturation lightness alpha]
  clojure.lang.IFn
  (invoke [this] this)
  (invoke [this k]
    (get this k))
  (invoke [this k missing]
    (get this k missing))
  (applyTo [this args]
    (clojure.lang.AFn/applyToHelper this args))
  Object
  (toString [this]
    (as-hex this)))

;; Show colors as hexadecimal values (ie. #000000) in the REPL.
(defmethod print-method CSSColor [color writer]
  (.write writer (str color)))

(def as-color map->CSSColor)

(defn rgb
  "Create an RGB color."
  ([[r g b :as vs]]
     (if (every? #(u/between? % 0 255) vs)
       (as-color {:red r :green g :blue b})
       (throw
        (IllegalArgumentException. "RGB values must be between 0 and 255"))))
  ([r g b]
     (rgb [r g b])))

(defn rgba
  "Create an RGBA color."
  ([[r g b a]]
     (if (u/between? a 0 1)
       (as-color (assoc (rgb [r g b]) :alpha a))
       (throw
        (IllegalArgumentException. "Alpha value must be between 0 and 1"))))
  ([r g b a]
     (rgba [r g b a])))

(defn hsl
  "Create an HSL color."
  ([[h s l]]
     ;; Handle CSSUnits. 
     (let [[h s l] (map #(:magnitude % %) [h s l])]
       (if (and (u/between? s 0 100)
                (u/between? l 0 100))
         (as-color {:hue (mod h 360) :saturation s :lightness l})
         (throw
          (IllegalArgumentException. "Saturation and lightness must be between 0(%) and 100(%)")))))
  ([h s l]
     (hsl [h s l])))

(defn hsla
  "Create an HSLA color."
  ([[h s l a]]
     (if (u/between? a 0 1)
       (as-color (assoc (hsl [h s l]) :alpha a))
       (throw
        (IllegalArgumentException. "Alpha value must be between 0 and 1"))))
  ([h s l a]
     (hsla [h s l a])))

(defn rgb?
  "Return true if color is an RGB color."
  [color]
  (and (map? color)
       (every? color #{:red :green :blue})))

(defn hsl?
  "Return true if color is an HSL color."
  [color]
  (and (map? color)
       (every? color #{:hue :saturation :lightness})))

(defn color?
  "Return true if x is a color."
  [x]
  (or (rgb? x) (hsl? x)))

(def ^{:doc "Regular expression for matching a hexadecimal color.
             Matches hexadecimal colors of length three or six possibly
             lead by a \"#\". The color portion is captured."}
  hex-re #"#?([\da-fA-F]{3}|[\da-fA-F]{6})")

(defn hex?
  "Returns true if x is a hexadecimal color."
  [x]
  (boolean (and (string? x) (re-matches hex-re x))))

(defn hex->rgb
  "Convert a hexadecimal color to an RGB color map."
  [s]
  (when-let [[_ hex] (re-matches hex-re s)]
    (let [hex (if (= 3 (count hex))
                (apply str (mapcat #(list % %) hex))
                hex)]
      (rgb (map #(Integer/parseInt % 16) (re-seq #"[\da-fA-F]{2}" hex))))))

(defn rgb->hex
  "Convert an RGB color map to a hexadecimal color."
  [{r :red g :green b :blue}]
  (letfn [(hex-part [v]
            (s/replace (format "%2s" (Integer/toString v 16)) " " "0"))]
    (apply str "#" (map hex-part [r g b]))))

(defn rgb->hsl
  "Convert an RGB color map to an HSL color map."
  [{:keys [red green blue] :as color}]
  (if (hsl? color)
    color
    (let [[r g b] (map #(/ % 255) [red green blue])
          mx (max r g b)
          mn (min r g b)
          d (- mx mn)
          h (condp = mx
              mn 0
              r (* 60 (/ (- g b) d))
              g (+ (* 60 (/ (- b r) d)) 120)
              b (+ (* 60 (/ (- r g) d)) 240))
          l (/ (+ mx mn) 2)
          s (cond
             (= mx mn) 0
             (< l 0.5) (/ d (* 2 l))
             :else (/ d (- 2 (* 2 l))))]
      (hsl (mod h 360) (* 100 s) (* 100 l)))))

(declare hue->rgb)

;; SEE: http://www.w3.org/TR/css3-color/#hsl-color.
(defn hsl->rgb
  "Convert an HSL color map to an RGB color map."
  [{:keys  [hue saturation lightness] :as color}]
  (if (rgb? color)
    color
    (let [h (/ hue 360.0)
          s (/ saturation 100.0)
          l (/ lightness 100.0)
          m2 (if (<= l 0.5)
               (* l (inc s))
               (- (+ l s) (* l s)))
          m1 (- (* 2 l) m2)
          [r g b] (map #(Math/round (* % 0xff))
                       [(hue->rgb m1 m2 (+ h (/ 1.0 3)))
                        (hue->rgb m1 m2 h)
                        (hue->rgb m1 m2 (- h (/ 1.0 3)))])]
      (rgb [r g b]))))

(defn- hue->rgb
  [m1 m2 h]
  (let [h (cond
           (< h 0) (inc h)
           (> h 1) (dec h)
           :else h)]
    (cond
     (< (* 6 h) 1) (+ m1 (* (- m2 m1) h 6))
     (< (* 2 h) 1) m2
     (< (* 3 h) 2) (+ m1 (* (- m2 m1) (- (/ 2.0 3) h) 6))
     :else m1)))

(defn hsl->hex
  "Convert an HSL color map to a hexadecimal string."
  [color]
  (-> color hsl->rgb rgb->hex))

(defn hex->hsl
  [color]
  (-> color hex->rgb rgb->hsl))

(defn as-hex
  "Convert a color to a hexadecimal string."
  [x]
  (cond
   (hex? x) x
   (rgb? x) (rgb->hex x)
   (hsl? x) (hsl->hex x)
   :else (throw (IllegalArgumentException. (str "Can't convert " x " to a color.")))))

(defn as-rgb
  "Convert a color to a RGB."
  [x]
  (cond
   (rgb? x) x
   (hsl? x) (hsl->rgb x)
   (hex? x) (hex->rgb x)
   :else (throw (IllegalArgumentException. (str "Can't convert " x " to a color.")))))

(defn as-hsl
  "Convert a color to a HSL."
  [x]
  (cond
   (hsl? x) x
   (rgb? x) (rgb->hsl x)
   (hex? x) (hex->hsl x)
   :else (throw (IllegalArgumentException. (str "Can't convert " x " to a color.")))))

(defn- make-color-operation
  [op]
  (fn color-op
    ([a] a)
    ([a b]
       (let [f #(select-keys % [:red :green :blue])
             o #(max 0 (min (op %1 %2) 255))
             a (as-rgb a)
             b (as-rgb b)]
         (as-color (merge-with o (f a) (f b)))))
    ([a b & more]
       (reduce color-op (color-op a b) more))))

(defmacro ^:private defcolor-operation [name operator]
  `(def ~name (make-color-operation ~operator)))

(defcolor-operation
  ^{:doc "Add the RGB components of two or more colors."}
  color+ +)

(defcolor-operation
  ^{:doc "Subtract the RGB components of two or more colors."}
  color- -)

(defcolor-operation
  ^{:doc "Multiply the RGB components of two or more colors."}
  color* *)

(defcolor-operation
  ^{:doc "Multiply the RGB components of two or more colors."}
  color-div /)

(def ^:private percent-clip
  (partial u/clip 0 100))

(defn- update-color [color field f v]
  (let [v (or (:magnitude v) v)]
    (update-in (as-hsl color) [field] f v)))

(defn rotate-hue
  "Rotates the hue value of a given color by amount."
  [color amount]
  (update-color color :hue (comp #(mod % 360) +) amount))

(defn saturate
  "Increase the saturation value of a given color by amount."
  [color amount]
  (update-color color :saturation (comp percent-clip +) amount))

(defn desaturate
  "Decrease the saturation value of a given color by amount."
  [color amount]
  (update-color color :saturation (comp percent-clip -) amount))

(defn lighten
  "Increase the lightness value a given color by amount."
  [color amount]
  (update-color color :lightness (comp percent-clip +) amount))

(defn darken
  "Decrease the lightness value a given color by amount."
  [color amount]
  (update-color color :lightness (comp percent-clip -) amount))

(defn invert
  "Return the inversion of a color."
  [color]
  (as-color (merge-with - {:red 255 :green 255 :blue 255} (as-rgb color))))

;;;; Color wheel functions. 

(defn complement
  "Return the complement of a color."
  [color]
  (rotate-hue color 180))

(defn- hue-rotations
  ([color & amounts]
     (map (partial rotate-hue color) amounts)))

(defn analogous
  "Given a color return a triple of colors which are 0, 30, and 60
   degrees clockwise from it. If a second falsy argument is passed the
   returned values will be in a counter-clockwise direction."
  ([color]
     (analogous true))
  ([color clockwise?]
     (let [sign (if clockwise? + -)]
       (hue-rotations color 0 (sign 30) (sign 60)))))

(defn triad
  "Given a color return a triple of colors which are equidistance apart
   on the color wheel."
  [color]
  (hue-rotations color 0 120 240))

(defn split-complement
  "Given a color return a triple of the color and the two colors on
   either side of it's complement."
  ([color]
     (split-complement color 130))
  ([color distance-from-complement]
     (let [d (u/clip 1 179 distance-from-complement)]
         (hue-rotations color 0 d (- d)))))

(defn tetrad
  "Given a color return a quadruple of four colors which are
   equidistance on the color wheel (ie. a pair of complements). An
   optional angle may be given for color of the second complement in the
   pair (this defaults to 90 when only color is passed)."
  ([color]
     (tetrad color 90))
  ([color angle]
     (let [a (u/clip 1 90 (Math/abs (:magnitude angle angle)))
           color-2 (rotate-hue color a)]
       [color (complement color) color-2 (complement color-2)])))
