(ns garden.color
  "Utilities for color creation, conversion, and manipulation."
  (:refer-clojure :exclude [complement])
  #?(:cljs
     (:require-macros
      [garden.color :refer [defcolor-operation]]))
  (:require
   [clojure.string :as string]
   [garden.util :as util])
  #?(:clj
     (:import clojure.lang.IFn)))

;; Many of the functions in this namespace were ported or inspired by
;; the implementations included with Sass
;; (http://sass-lang.com/docs/yardoc/Sass/Script/Functions.html).
;; Some additional functions have been added such as `triad` and
;; `tetrad` for generating sets of colors. 

;; Converts a color to a hexadecimal string (implementation below). 
(declare as-hex)

(defrecord CSSColor [red green blue hue saturation lightness alpha]
  IFn
  #?(:clj
      (invoke [this] this))
  #?(:clj
      (invoke [this k]
              (get this k)))
  #?(:clj
      (invoke [this k missing]
              (get this k missing)))
  #?(:cljs
      (-invoke [this] this))
  #?(:cljs
      (-invoke [this k]
               (get this k)))
  #?(:cljs
      (-invoke [this k missing]
               (get this k missing)))
  #?(:clj
      (applyTo [this args]
               (clojure.lang.AFn/applyToHelper this args))))

(def as-color map->CSSColor)

(defn rgb
  "Create an RGB color."
  ([[r g b :as vs]]
     (if (every? #(util/between? % 0 255) vs)
       (as-color {:red r :green g :blue b})
       (throw
        (ex-info "RGB values must be between 0 and 255" {}))))
  ([r g b]
     (rgb [r g b])))

(defn rgba
  "Create an RGBA color."
  ([[r g b a]]
     (if (util/between? a 0 1)
       (as-color (assoc (rgb [r g b]) :alpha a))
       (throw
        (ex-info "Alpha value must be between 0 and 1" {}))))
  ([r g b a]
     (rgba [r g b a])))

(defn hsl
  "Create an HSL color."
  ([[h s l]]
     ;; Handle CSSUnits. 
     (let [[h s l] (map #(get % :magnitude %) [h s l])]
       (if (and (util/between? s 0 100)
                (util/between? l 0 100))
         (as-color {:hue (mod h 360) :saturation s :lightness l})
         (throw
          (ex-info "Saturation and lightness must be between 0(%) and 100(%)" {})))))
  ([h s l]
     (hsl [h s l])))

(defn hsla
  "Create an HSLA color."
  ([[h s l a]]
     (if (util/between? a 0 1)
       (as-color (assoc (hsl [h s l]) :alpha a))
       (throw
        (ex-info "Alpha value must be between 0 and 1" {}))))
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
  ;; Quantifier must be in this order or JavaScript engines will match
  ;; 3 chars even when 6 are provided (failing re-matches).
  hex-re #"#?([\da-fA-F]{6}|[\da-fA-F]{3})")

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
      (->> (re-seq #"[\da-fA-F]{2}" hex)
           (map #(util/string->int % 16))
           (rgb)))))

(defn rgb->hex
  "Convert an RGB color map to a hexadecimal color."
  [{r :red g :green b :blue}]
  (letfn [(hex-part [v]
            (-> (util/format "%2s" (util/int->string v 16))
                (string/replace " " "0")))]
    (apply str "#" (map hex-part [r g b]))))

(defn trim-one [x]
  (if (< 1 x) 1 x))

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
          l (trim-one (/ (+ mx mn) 2))
          s (trim-one
              (cond
                (= mx mn) 0
                (< l 0.5) (/ d (* 2 l))
                :else (/ d (- 2 (* 2 l)))))]
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
  "Convert a hexadecimal color to an HSL color."
  [color]
  (-> color hex->rgb rgb->hsl))

(def percent-clip
  (partial util/clip 0 100))

(def rgb-clip
  (partial util/clip 0 255))

(defn as-hex
  "Convert a color to a hexadecimal string."
  [x]
  (cond
   (hex? x) x
   (rgb? x) (rgb->hex x)
   (hsl? x) (hsl->hex x)
   :else (throw (ex-info (str "Can't convert " x " to a color.") {}))))

(defn as-rgb
  "Convert a color to a RGB."
  [x]
  (cond
   (rgb? x) x
   (hsl? x) (hsl->rgb x)
   (hex? x) (hex->rgb x)
   (number? x) (rgb (map rgb-clip [x x x]))
   :else (throw (ex-info (str "Can't convert " x " to a color.") {}))))

(defn as-hsl
  "Convert a color to a HSL."
  [x]
  (cond
   (hsl? x) x
   (rgb? x) (rgb->hsl x)
   (hex? x) (hex->hsl x)
   (number? x) (hsl [x (percent-clip x) (percent-clip x)])
   :else (throw (ex-info (str "Can't convert " x " to a color.") {}))))

(defn- restrict-rgb
  [m]
  (select-keys m [:red :green :blue]))

(defn- make-color-operation
  [op]
  (fn color-op
    ([a] a)
    ([a b]
       (let [o (comp rgb-clip op)
             a (restrict-rgb (as-rgb a))
             b (restrict-rgb (as-rgb b))]
         (as-color (merge-with o a b))))
    ([a b & more]
       (reduce color-op (color-op a b) more))))

#?(:clj
   (defmacro ^:private defcolor-operation [name operator]
     `(def ~name (make-color-operation ~operator))))

(defcolor-operation
  ^{:doc "Add the RGB components of two or more colors."
    :arglists '([a] [a b] [a b & more])}
  color+ +)

(defcolor-operation
  ^{:doc "Subtract the RGB components of two or more colors."
    :arglists '([a] [a b] [a b & more])}
  color- -)

(defcolor-operation
  ^{:doc "Multiply the RGB components of two or more colors."
    :arglists '([a] [a b] [a b & more])}
  color* *)

(defcolor-operation
  ^{:doc "Multiply the RGB components of two or more colors."
    :arglists '([a] [a b] [a b & more])}
  color-div /)

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

(defn mix
  "Mix two or more colors by averaging their RGB channels."
  ([color-1 color-2]
     (let [c1 (restrict-rgb (as-rgb color-1))
           c2 (restrict-rgb (as-rgb color-2))]
       (as-color (merge-with util/average c1 c2))))
  ([color-1 color-2 & more]
     (reduce mix (mix color-1 color-2) more)))

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
     (analogous color true))
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
     (let [d (util/clip 1 179 distance-from-complement)]
         (hue-rotations color 0 d (- d)))))

(defn tetrad
  "Given a color return a quadruple of four colors which are
  equidistance on the color wheel (ie. a pair of complements). An
  optional angle may be given for color of the second complement in the
  pair (this defaults to 90 when only color is passed)."
  ([color]
     (tetrad color 90))
  ([color angle]
     (let [a (util/clip 1 90 (Math/abs (:magnitude angle angle)))
           color-2 (rotate-hue color a)]
       [(rotate-hue color 0)
        (complement color)
        color-2
        (complement color-2)])))

(defn shades
  "Given a color return a list of shades from lightest to darkest by
  a step. By default the step is 10. White and black are excluded from
  the returned list."
  ([color]
     (shades color 10))
  ([color step]
     (let [c (as-hsl color)]
       (for [i (range 1 (Math/floor (/ 100.0 step)))]
         (assoc c :lightness (* i step))))))

;; ---------------------------------------------------------------------
;; CSS color name conversion

(def color-name->hex
  {:aquamarine "#7fffd4"
   :aliceblue "#f0f8ff"
   :antiquewhite "#faebd7"
   :aqua "#00ffff"
   :azure "#f0ffff"
   :beige "#f5f5dc"
   :bisque "#ffe4c4"
   :black "#000000"
   :blanchedalmond "#ffebcd"
   :blue "#0000ff"
   :blueviolet "#8a2be2"
   :brown "#a52a2a"
   :burlywood "#deb887"
   :cadetblue "#5f9ea0"
   :chartreuse "#7fff00"
   :chocolate "#d2691e"
   :coral "#ff7f50"
   :cornflowerblue "#6495ed"
   :cornsilk "#fff8dc"
   :crimson "#dc143c"
   :cyan "#00ffff"
   :darkblue "#00008b"
   :darkcyan "#008b8b"
   :darkgoldenrod "#b8860b"
   :darkgray "#a9a9a9"
   :darkgreen "#006400"
   :darkgrey "#a9a9a9"
   :darkkhaki "#bdb76b"
   :darkmagenta "#8b008b"
   :darkolivegreen "#556b2f"
   :darkorange "#ff8c00"
   :darkorchid "#9932cc"
   :darkred "#8b0000"
   :darksalmon "#e9967a"
   :darkseagreen "#8fbc8f"
   :darkslateblue "#483d8b"
   :darkslategray "#2f4f4f"
   :darkslategrey "#2f4f4f"
   :darkturquoise "#00ced1"
   :darkviolet "#9400d3"
   :deeppink "#ff1493"
   :deepskyblue "#00bfff"
   :dimgray "#696969"
   :dimgrey "#696969"
   :dodgerblue "#1e90ff"
   :firebrick "#b22222"
   :floralwhite "#fffaf0"
   :forestgreen "#228b22"
   :fuchsia "#ff00ff"
   :gainsboro "#dcdcdc"
   :ghostwhite "#f8f8ff"
   :gold "#ffd700"
   :goldenrod "#daa520"
   :gray "#808080"
   :green "#008000"
   :greenyellow "#adff2f"
   :honeydew "#f0fff0"
   :hotpink "#ff69b4"
   :indianred "#cd5c5c"
   :indigo "#4b0082"
   :ivory "#fffff0"
   :khaki "#f0e68c"
   :lavender "#e6e6fa"
   :lavenderblush "#fff0f5"
   :lawngreen "#7cfc00"
   :lemonchiffon "#fffacd"
   :lightblue "#add8e6"
   :lightcoral "#f08080"
   :lightcyan "#e0ffff"
   :lightgoldenrodyellow "#fafad2"
   :lightgray "#d3d3d3"
   :lightgreen "#90ee90"
   :lightgrey "#d3d3d3"
   :lightpink "#ffb6c1"
   :lightsalmon "#ffa07a"
   :lightseagreen "#20b2aa"
   :lightskyblue "#87cefa"
   :lightslategray "#778899"
   :lightslategrey "#778899"
   :lightsteelblue "#b0c4de"
   :lightyellow "#ffffe0"
   :lime "#00ff00"
   :limegreen "#32cd32"
   :linen "#faf0e6"
   :magenta "#ff00ff"
   :maroon "#800000"
   :mediumaquamarine "#66cdaa"
   :mediumblue "#0000cd"
   :mediumorchid "#ba55d3"
   :mediumpurple "#9370db"
   :mediumseagreen "#3cb371"
   :mediumslateblue "#7b68ee"
   :mediumspringgreen "#00fa9a"
   :mediumturquoise "#48d1cc"
   :mediumvioletred "#c71585"
   :midnightblue "#191970"
   :mintcream "#f5fffa"
   :mistyrose "#ffe4e1"
   :moccasin "#ffe4b5"
   :navajowhite "#ffdead"
   :navy "#000080"
   :oldlace "#fdf5e6"
   :olive "#808000"
   :olivedrab "#6b8e23"
   :orange "#ffa500"
   :orangered "#ff4500"
   :orchid "#da70d6"
   :palegoldenrod "#eee8aa"
   :palegreen "#98fb98"
   :paleturquoise "#afeeee"
   :palevioletred "#db7093"
   :papayawhip "#ffefd5"
   :peachpuff "#ffdab9"
   :peru "#cd853f"
   :pink "#ffc0cb"
   :plum "#dda0dd"
   :powderblue "#b0e0e6"
   :purple "#800080"
   :red "#ff0000"
   :rosybrown "#bc8f8f"
   :royalblue "#4169e1"
   :saddlebrown "#8b4513"
   :salmon "#fa8072"
   :sandybrown "#f4a460"
   :seagreen "#2e8b57"
   :seashell "#fff5ee"
   :sienna "#a0522d"
   :silver "#c0c0c0"
   :skyblue "#87ceeb"
   :slateblue "#6a5acd"
   :slategray "#708090"
   :slategrey "#708090"
   :snow "#fffafa"
   :springgreen "#00ff7f"
   :steelblue "#4682b4"
   :tan "#d2b48c"
   :teal "#008080"
   :thistle "#d8bfd8"
   :tomato "#ff6347"
   :turquoise "#40e0d0"
   :violet "#ee82ee"
   :wheat "#f5deb3"
   :white "#ffffff"
   :whitesmoke "#f5f5f5"
   :yellow "#ffff00"
   :yellowgreen "#9acd32"})

(defn- ex-info-color-name
  "Helper function for from-name. Returns an instance of ExceptionInfo
  for unknown colors."
  [n]
  (ex-info
   (str "Unknown color " (pr-str n) " see (:expected (ex-data e)) for a list of color names")
   {:given n
    :expected (set (keys color-name->hex))}))

(def
  ^{:private true
    :doc "Helper function for from-name."}
  color-name->color
  (memoize (fn [k] (color-name->hex k))))

(defn from-name
  "Given a CSS color name n return an instance of CSSColor."
  [n]
  (if-let [h (color-name->color (keyword n))]
    h
    (throw (ex-info-color-name n))))

(defn- scale-color-value
  [value amount]
  (+ value (if (pos? amount)
             (* (- 100 value) (/ amount 100))
             (/ (* value amount) 100))))

(defn scale-lightness
  "Scale the lightness of a color by amount"
  [color amount]
  (update-color color :lightness scale-color-value amount))

(defn scale-saturation
  "Scale the saturation of a color by amount"
  [color amount]
  (update-color color :saturation scale-color-value amount))

(defn- decrown-hex [hex]
  (string/replace hex #"^#" ""))

(defn- crown-hex [hex]
  (if (re-find #"^#" hex)
    hex
    (str "#" hex)))

(defn- expand-hex
  "(expand-hex \"#abc\") -> \"aabbcc\"
   (expand-hex \"333333\") -> \"333333\""
  [hex]
  (as-> (decrown-hex hex) _
        (cond
         (= 3 (count _)) (string/join (mapcat vector _ _))
         (= 1 (count _)) (string/join (repeat 6 _))
         :else _)))

(defn- hex->long
  "(hex->long \"#abc\") -> 11189196"
  [hex]
  (-> hex
      (string/replace #"^#" "")
      (expand-hex)
      #?(:clj (Long/parseLong 16)
         :cljs (js/parseInt 16))))

(defn- long->hex
  "(long->hex 11189196) -> \"aabbcc\""
  [long]
  #?(:clj (Integer/toHexString long)
     :cljs (.toString long 16)))

(defn weighted-mix
  "`weight` is number 0 to 100 (%).
   At 0, it weighs color-1 at 100%.
   At 100, it weighs color-2 at 100%.
   Returns hex string."
  [color-1 color-2 weight]
  (let [[weight-1 weight-2] (map #(/ % 100) [(- 100 weight) weight])
        [long-1 long-2] (map (comp hex->long as-hex)
                             [color-1 color-2])]
    (-> (+ (* long-1 weight-1) (* long-2 weight-2))
        (long->hex)
        (expand-hex)
        (crown-hex))))
