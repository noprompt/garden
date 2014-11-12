(ns garden.color
  (:refer-clojure :exclude [complement long])
  (:require
   [clojure.string :as string]))

;; ---------------------------------------------------------------------
;; Color protocols

(defprotocol IRGB
  "Return an instance of RGB from this."
  (-rgb [this]))

(defprotocol IRGBA
  "Return and instance of RGBA from this."
  (-rgba [this]))

(defprotocol IRed
  "Return the red channel value of this."
  (-red [this]))

(defprotocol IGreen
  "Return the green channel value of this."
  (-green [this]))

(defprotocol IBlue
  "Return the blue channel value of this."
  (-blue [this]))

(defprotocol IHSL
  "Return an instance of HSL from this."
  (-hsl [this]))

(defprotocol IHSLA
  "Return an instance of HSLA from this."
  (-hsla [this]))

(defprotocol IHue
  "Return the hue value of this."
  (-hue [this]))

(defprotocol ISaturation
  "Return the saturation value of this."
  (-saturation [this]))

(defprotocol ILightness
  "Return the lightness value of this."
  (-lightness [this]))

(defprotocol IAlpha
  "Return the alpha channel value of this."
  (-alpha [this]))

;; ---------------------------------------------------------------------
;; Color types

(defrecord RGB [r g b])
(defrecord RGBA [r g b a])
(defrecord HSL [h s l])
(defrecord HSLA [h s l a])

;; ---------------------------------------------------------------------
;; Color functions


;;; RGB functions

(defn ^Number red
  "Return an integer (between 0 and 255) of the red channel of x. x must
  satisfy IRed."
  [x]
  {:post [(number? %) (<= 0 % 255)]}
  (-red x))

(defn ^Number green
  "Return an integer (between 0 and 255) of the green channel of x. x 
  must satisfy IGreen."
  [x]
  {:post [(number? %) (<= 0 % 255)]}
  (-green x))

(defn ^Number blue
  "Return an integer (between 0 and 255) of the blue channel of x. x must
  satisfy IBlue."
  [x]
  {:post [(number? %) (<= 0 % 255)]}
  (-blue x))


;;; HSL functions

(defn ^Number hue
  [x]
  {:post [(number? %)]}
  (-hue x))

(defn ^Number saturation
  [x]
  {:post [(number? %) (<= 0 % 100)]}
  (-saturation x))

(defn ^Number lightness
  [x]
  {:post [(number? %) (<= 0 % 100)]}
  (-lightness x))

(defn ^Number alpha
  [x]
  {:post [(number? %) (<= 0 % 1)]}
  (-alpha x))

;; ---------------------------------------------------------------------
;; Creation

(defn ^RGB rgb
  "Return an instance of RGB. x must satisfy either IRGB or all of
  IRed, IGreen, and IBlue."
  [x]
  {:post [(instance? RGB %)]}
  (if (satisfies? IRGB x)
    (-rgb x)
    (RGB. (red x) (green x) (blue x))))

(defn ^RGBA rgba
  "Return an instance of RGBA. x must satisfy either IRGBA or all of
  IRed, IGreen, IBlue, and IAlpha."
  [x]
  {:post [(instance? RGBA %)]}
  (if (satisfies? IRGBA x)
    (-rgba x)
    (RGBA. (red x) (green x) (blue x) (lightness x))))

(defn ^HSL hsl 
  "Return an instance of HSL. x must satisfy either IHSL or all of
  IHue, ISaturation, and ILightness."
  [x]
  {:post [(instance? HSL %)]}
  (if (satisfies? IHSL x)
    (-hsl x)
    (HSL. (hue x) (saturation x) (lightness x))))

(defn ^HSL hsla
  "Return an instance of HSLA. x must satisfy either IHSLA or all of
  IHue, ISaturation, ILightness, and IAlpha."
  [x]
  {:post [(instance? HSLA %)]}
  (if (satisfies? IHSLA x)
    (-hsla x)
    (HSLA. (hue x) (saturation x) (lightness x) (alpha x))))

(defn ^String hex
  "Convert a color to string in hex format."
  [x]
  (let [c (rgb x)
        f (fn [^Number i]
            (let [s (Integer/toString i 16)]
              (if (= (.length ^String s) 1)
                (str 0 s)
                s)))
        r (f (.-r c))
        g (f (.-g c))
        b (f (.-b c))]
    (str "#" r g b)))

(defn ^Long long
  "Convert a color to long. x must satisfy IRGB."
  [x]
  (if (number? x)
    (clojure.core/long x)
    (let [c (rgb x)
          r (.-r c)
          g (.-g c)
          b (.-b c)]
      (bit-or (bit-shift-left r 16)
              (bit-shift-left g 8)
              b))))

;; ---------------------------------------------------------------------
;; Conversion

(defn ^HSL rgb->hsl
  "Return an instance of HSL from red, green, and blue values."
  [r g b]
  (let [mn (min r g b)
        mx (max r g b)
        h (cond
           (= mn mx)
           0

           (= r mx)
           (* 60 (/ (- g b)
                    (- mx mn)))

           (= g mx)
           (* 60 (+ 2 (/ (- b r)
                         (- mx mn))))

           :else
           (* 60 (+ 4 (/ (- r g)
                         (- mx mn)))))
        l (/ (+ mn mx) 510)
        s (cond
           (= mn mx)
           0

           (< l 0.5)
           (/ (- mx mn)
              (+ mx mn))

           :else
           (/ (- mx mn)
              (- 510 mx mn)))]
    (HSL. (mod (float h) 360) (* 100.0 s) (* 100.0 l))))

(defn ^RGB hsl->rgb
  "Return and instance of RGB from hue, saturation, and lightness values."
  [h s l]
  (let [h (mod h 360)
        s (/ s 100.0)
        l (/ l 100.0)
        ;; C = (1 - |2L - 1|) * s
        c (* (- 1 (Math/abs (double (- (* 2 l) 1)))) s)
        ;; X = C * (1 - |(H/60) mod 2 - 1|)
        x (* c (- 1 (Math/abs (double (- 1 (mod (/ h 60.0) 2))))))
        ;; m = L - C/2
        m (- l (/ c 2.0))
        [r' g' b']
        (cond
         (<= 0 h 59)    [c x 0]
         (<= 60 h 119)  [x c 0]
         (<= 120 h 179) [0 c x]
         (<= 180 h 239) [0 x c]
         (<= 240 h 299) [x 0 c]
         (<= 300 h 359) [c 0 x])
        r (Math/round (* 0xff (+ r' m)))
        g (Math/round (* 0xff (+ g' m)))
        b (Math/round (* 0xff (+ b' m)))]
    (RGB. r g b)))

;; ---------------------------------------------------------------------
;; Utilities

(defn ^HSLA rotate-hue
  "Rotates the hue value of a given color by degrees."
  [color degrees]
  (update-in (hsla color) [:h] (fn [h] (mod (+ h degrees) 360))))

(defn ^HSLA saturate
  "Increase the saturation value of a given color by amount."
  [color amount]
  (update-in (hsla color) [:s] (fn [s] (min (+ s amount) 100))))

(defn ^HSLA desaturate
  "Decrease the saturation value of a given color by amount."
  [color amount]
  (update-in (hsla color) [:s] (fn [s] (max 0 (- s amount)))))

(defn ^HSLA lighten
  "Increase the lightness value a given color by amount."
  [color amount]
  (update-in (hsla color) [:l] (fn [l] (min (+ l amount) 100))))

(defn ^HSLA darken
  "Decrease the saturation value of a given color by amount."
  [color amount]
  (update-in (hsla color) [:l] (fn [l] (max 0 (- l amount)))))

(defn ^RGBA invert
  "Return the inversion of a color."
  [color]
  (let [c (rgba color)]
    (RGBA. (- 255 (.-r c))
           (- 255 (.-g c))
           (- 255 (.-b c))
           (.-a c))))

(defn ^RGBA mix
  "Mix two or more colors by averaging their RGBA channels."
  ([color-1 color-2]
     (let [c1 (rgba color-1)
           c2 (rgba color-2)]
       (merge-with
        (fn [l r]
          (/ (+ l r) 2.0))
        c1 c2)))
  ([color-1 color-2 & more]
     (reduce mix (mix color-1 color-2) more)))

(defn hue-rotations
  [color & amounts]
  (map (fn [amount]
         (rotate-hue color amount))
       amounts))

(defn complement
  "Return the complement of a color."
  [color]
  (rotate-hue color 180))

(defn split-complement
  "Given a color return a triple of the color and the two colors on
  either side of it's complement."
  ([color]
     (split-complement color 130))
  ([color distance-from-complement]
     (let [d (max 1 (min 179 distance-from-complement))]
       (hue-rotations color 0 d (- d)))))

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

(defn tetrad
  "Given a color return a quadruple of four colors which are
  equidistance on the color wheel (ie. a pair of complements). An
  optional angle may be given for color of the second complement in the
  pair (this defaults to 90 when only color is passed)."
  ([color]
     (tetrad color 90))
  ([color angle]
     (let [degrees (max 1 (min 90 (Math/abs (float angle))))
           color' (rotate-hue color degrees)]
       [(rotate-hue color 0)
        (complement color)
        color'
        (complement color')])))

(defn shades
  "Given a color return a list of shades from lightest to darkest by
  a step. By default the step is 10. White and black are excluded from
  the returned list."
  ([color]
     (shades color 10))
  ([color step]
     (let [c (hsl color)]
       (for [i (range 1 (Math/floor (/ 100.0 step)))]
         (assoc c :l (* i step))))))


;; ---------------------------------------------------------------------
;; Implementation


;;; Color types

(extend-type RGB
   IRGB
   (-rgb [this] this)

   IRGBA
   (-rgba [this]
     (RGBA. (.-r this) (.-g this) (.-b this) 1.0))

   IHSL
   (-hsl [this]
     (rgb->hsl (.-r this) (.-g this) (.-b this)))

   IHSLA
   (-hsla [this]
     (let [hsl ^HSL (-hsl this)]
       (HSLA. (.-h hsl) (.-s hsl) (.-l hsl) 1.0)))

   IRed
   (-red [this]
     (.-r this))

   IGreen
   (-green [this]
     (.-g this))

   IBlue
   (-blue [this]
     (.-b this))

   IAlpha
   (-alpha [_] 1.0)

   IHue
   (-hue [this]
     (.-h ^HSL (-hsl this)))

   ISaturation
   (-saturation [this]
     (.-s ^HSL (-hsl this)))

   ILightness
   (-lightness [this]
     (.-l ^HSL (-hsl this))))

(extend-type RGBA
  IRGB
  (-rgb [this]
    (RGB. (.-r this) (.-g this) (.-b this)))

  IRGBA
  (-rgba [this] this)

  
  IHSL
  (-hsl [this]
    (rgb->hsl (.-r this) (.-g this) (.-b this)))

  IHSLA
  (-hsla [this]
    (let [hsl ^HSL (-hsl this)]
      (HSLA. (.-h hsl) (.-s hsl) (.-l hsl) (.-a this))))

  IAlpha
  (-alpha [this]
    (.-a this))

  IRed
  (-red [this]
    (.-r this))

  IGreen
  (-green [this]
    (.-g this))

  IBlue
  (-blue [this]
    (.-b this))

  IHue
  (-hue [this]
    (.-h ^HSL (-hsl this)))

  ISaturation
  (-saturation [this]
    (.-s ^HSL (-hsl this)))

  ILightness
  (-lightness [this]
    (.-l ^HSL (-hsl this)))) 

(extend-type HSL
  IHSL
  (-hsl [this] this)

  IHSLA
  (-hsla [this]
    (HSLA. (.-h this) (.-s this) (.-l this) 1.0))

  IRGB
  (-rgb [this]
    (hsl->rgb (.-h this) (.-s this) (.-l this)))

  IRGBA
  (-rgb [this]
    (let [rgb (hsl->rgb (.-h this) (.-s this) (.-l this))]
      (RGBA. (.-r rgb) (.-g rgb) (.-b rgb) 1.0)))

  IAlpha
  (-alpha [this] 1.0)

  IRed
  (-red [this]
    (.-r (rgb this)))

  IGreen
  (-green [this]
    (.-g (rgb this)))

  IBlue
  (-blue [this]
    (.-b (rgb this)))

  IHue
  (-hue [this]
    (.-h this))

  ISaturation
  (-saturation [this]
    (.-s this))

  ILightness
  (-lightness [this]
    (.-l this)))

(extend-type HSLA
  IHSL
  (-hsl [this]
    (HSL. (.-h this) (.-s this) (.-l this)))

  IHSLA
  (-hsla [this] this)

  IRGB
  (-rgb [this]
    (hsl->rgb (.-h this) (.-s this) (.-l this)))

  IRGBA
  (-rgba [this]
    (let [rgb (hsl->rgb (.-h this) (.-s this) (.-l this))]
      (RGBA. (.-r rgb) (.-g rgb) (.-b rgb) (.-a this))))

  IAlpha
  (-alpha [this]
    (.-a this))

  IRed
  (-red [this]
    (.-r (rgb this)))

  IGreen
  (-green [this]
    (.-g (rgb this)))

  IBlue
  (-blue [this]
    (.-b (rgb this)))

  IHue
  (-hue [this]
    (.-h this))

  ISaturation
  (-saturation [this]
    (.-s this))

  ILightness
  (-lightness [this]
    (.-l this)))


;;; Numbers

(extend-type Long
  IRGB
  (-rgb [l]
    (RGB. (-red l) (-green l) (-blue l)))

  IRGBA
  (-rgba [l]
    (RGBA. (-red l) (-green l) (-blue l) (-alpha l)))

  IHSL
  (-hsl [l]
    (hsl (-rgb l)))

  IAlpha
  (-alpha [l]
    (if (< 0 (bit-and l 0xff000000))
      (/ (bit-and (bit-shift-right l 24) 0xff)
         255.0)
      1.0))

  IRed
  (-red [l]
    (bit-and (bit-shift-right l 16) 0xff))

  IGreen
  (-green [l]
    (bit-and (bit-shift-right l 8) 0xff))

  IBlue
  (-blue [l]
    (bit-and l 0xff))

  IHue
  (-hue [l]
    (hue (hsl l)))

  ISaturation
  (-saturation [l]
    (saturation (hsl l)))

  ILightness
  (-lightness [l]
    (lightness (hsl l))))

(extend-type Integer
  IRGB
  (-rgb [i]
    (RGB. (-red i) (-green i) (-blue i)))

  IRGBA
  (-rgba [i]
    (RGBA. (-red i) (-green i) (-blue i) (-alpha i)))

  IHSL
  (-hsl [i]
    (hsl (-rgb i)))

  IHSLA
  (-hsla [i]
    (hsla (-rgba i)))

  IAlpha
  (-alpha [i]
    ;; Alpha values are out of range for int so we default to 1.0.
    1.0)

  IRed
  (-red [i]
    (bit-and (bit-shift-right i 16) 0xff))

  IGreen
  (-green [i]
    (bit-and (bit-shift-right i 8) 0xff))

  IBlue
  (-blue [i]
    (bit-and i 0xff))

  IHue
  (-hue [i]
    (hue (hsl i)))

  ISaturation
  (-saturation [i]
    (saturation (hsl i)))

  ILightness
  (-lightness [i]
    (lightness (hsl i))))


;;; String

(def ^:private hex-re
  "Regex for matching a hex color with optional leading
  \"#\". Captures the hex value."
  #"#?((?:[\da-fA-F]{3}){1,2})")

(let [;; RGB channel range (0 - 255)
      c "\\s*([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\s*"
      ;; Alpha channel range (0 - 1)
      a "\\s*(0?(?:\\.[0-9]+)?|1(?:\\.0)?)\\s*"]
  (def ^:private rgb-re
    "Regular expression mathing a CSS rgb color value. Captures each
  argument."
    (re-pattern (str "rgb\\(" c "," c "," c "\\)")))

  (def ^:private rgba-re
    "Regular expression matching a CSS rgba color value. Captures each
  argument."
    (re-pattern (str "rgba\\(" c "," c "," c "," a "\\)")))

  (def ^:private hsl-re
    "Regular expression matching a CSS hsl color value. Caputres each
  argument."
    #"hsl\(\s*(\d+)\s*,\s*([0-9]|[1-9][0-9]|100)\s*,\s*([0-9](?:\.[0-9])?|[1-9][0-9](?:\.[0-9]+)?|100)%?\s*\)")

  (def ^:private hsla-re
    "Regular expression matching a CSS hsla color value. Captures each
  argument."
    #"hsla\(\s*(\d+)\s*,\s*([0-9]|[1-9][0-9]|100)\s*,\s*([0-9](?:\.[0-9])?|[1-9][0-9](?:\.[0-9]+)?|100)%?\s*,\s*(0?(?:\.[0-9]+)?|1(?:\.0)?)\s*\)"))

(defn ^RGB hex-str->rgb
  "Convert a CSS hex value to instance of RGB."
  [^String s]
  (if-let [[_ ^String h] (re-matches hex-re s)]
    (let [[r g b] (if (= (.length h) 3)
                    (mapv #(Integer/parseInt (str % %) 16)
                          h)
                    (mapv #(Integer/parseInt % 16)
                          (re-seq #"[0-9a-f]{2}" h)))]
      (RGB. r g b))
    (throw (ex-info (str "Invalid hex string: " (pr-str s))
                    {:given s
                     :expected `(~'re-matches ~hex-re ~s)}))))

(defn ^RGB rgb-str->rgb
  "Convert a CSS rgb value to an instance of RGB."
  [^String s]
  (if-let [[_ & ns] (re-matches rgb-re s)]
    (let [[r g b] (map #(Integer/parseInt ^String %) ns)]
      (RGB. r g b))
    (throw (ex-info (str "Invalid rgb string: " (pr-str s))
                    {:given s
                     :expected `(~'re-matches ~rgb-re ~s)}))))

(defn ^RGBA rgba-str->rgba
  "Convert a CSS rgba value to an instance of RGBA."
  [^String s]
  (if-let [[_ r g b a] (re-matches rgba-re s)]
    (let [[r g b] (map #(Integer/parseInt ^String %) [r g b])
          a (Float. ^String a)]
      (RGBA. r g b a))
    (throw (ex-info (str "Invalid rgb string: " (pr-str s))
                    {:given s
                     :expected `(~'re-matches ~rgba-re ~s)}))))

(defn ^HSL hsl-str->hsl
  "Convert a CSS hsl value to an instance of HSL."
  [^String s]
  (if-let [[_ h s l] (re-matches hsl-re s)]
    (let [[h s] (map #(Integer/parseInt ^String %) [h s])
          l (Float. ^String l)]
      (HSL. h s l))
    (throw (ex-info (str "Invalid hsl string: " (pr-str s))
                    {:given s
                     :expected `(~'re-matches ~hsl-re ~s)}))))

(defn ^HSLA hsla-str->hsla
  "Convert a CSS hsla value to an instance of HSLA."
  [^String s]
  (if-let [[_ h s l a] (re-matches hsla-re s)]
    (let [[h s] (map #(Integer/parseInt ^String %) [h s])
          l (Float. ^String l)
          a (Float. ^String a)]
      (HSLA. h s l a))
    (throw (ex-info (str "Invalid hsla string: " (pr-str s))
                    {:given s
                     :expected `(~'re-matches ~hsla-re ~s)}))))

(def
  ^{:doc "Convert a CSS color name to hex."}
  color-name->hex
  {"aliceblue" "#f0f8ff"
   "antiquewhite" "#faebd7"
   "aqua" "#00ffff"
   "aquamarine" "#7fffd4"
   "azure" "#f0ffff"
   "beige" "#f5f5dc"
   "bisque" "#ffe4c4"
   "black" "#000000"
   "blanchedalmond" "#ffebcd"
   "blue" "#0000ff"
   "blueviolet" "#8a2be2"
   "brown" "#a52a2a"
   "burlywood" "#deb887"
   "cadetblue" "#5f9ea0"
   "chartreuse" "#7fff00"
   "chocolate" "#d2691e"
   "coral" "#ff7f50"
   "cornflowerblue" "#6495ed"
   "cornsilk" "#fff8dc"
   "crimson" "#dc143c"
   "cyan" "#00ffff"
   "darkblue" "#00008b"
   "darkcyan" "#008b8b"
   "darkgoldenrod" "#b8860b"
   "darkgray" "#a9a9a9"
   "darkgreen" "#006400"
   "darkgrey" "#a9a9a9"
   "darkkhaki" "#bdb76b"
   "darkmagenta" "#8b008b"
   "darkolivegreen" "#556b2f"
   "darkorange" "#ff8c00"
   "darkorchid" "#9932cc"
   "darkred" "#8b0000"
   "darksalmon" "#e9967a"
   "darkseagreen" "#8fbc8f"
   "darkslateblue" "#483d8b"
   "darkslategray" "#2f4f4f"
   "darkslategrey" "#2f4f4f"
   "darkturquoise" "#00ced1"
   "darkviolet" "#9400d3"
   "deeppink" "#ff1493"
   "deepskyblue" "#00bfff"
   "dimgray" "#696969"
   "dimgrey" "#696969"
   "dodgerblue" "#1e90ff"
   "firebrick" "#b22222"
   "floralwhite" "#fffaf0"
   "forestgreen" "#228b22"
   "fuchsia" "#ff00ff"
   "gainsboro" "#dcdcdc"
   "ghostwhite" "#f8f8ff"
   "gold" "#ffd700"
   "goldenrod" "#daa520"
   "gray" "#808080"
   "green" "#008000"
   "greenyellow" "#adff2f"
   "honeydew" "#f0fff0"
   "hotpink" "#ff69b4"
   "indianred" "#cd5c5c"
   "indigo" "#4b0082"
   "ivory" "#fffff0"
   "khaki" "#f0e68c"
   "lavender" "#e6e6fa"
   "lavenderblush" "#fff0f5"
   "lawngreen" "#7cfc00"
   "lemonchiffon" "#fffacd"
   "lightblue" "#add8e6"
   "lightcoral" "#f08080"
   "lightcyan" "#e0ffff"
   "lightgoldenrodyellow" "#fafad2"
   "lightgray" "#d3d3d3"
   "lightgreen" "#90ee90"
   "lightgrey" "#d3d3d3"
   "lightpink" "#ffb6c1"
   "lightsalmon" "#ffa07a"
   "lightseagreen" "#20b2aa"
   "lightskyblue" "#87cefa"
   "lightslategray" "#778899"
   "lightslategrey" "#778899"
   "lightsteelblue" "#b0c4de"
   "lightyellow" "#ffffe0"
   "lime" "#00ff00"
   "limegreen" "#32cd32"
   "linen" "#faf0e6"
   "magenta" "#ff00ff"
   "maroon" "#800000"
   "mediumaquamarine" "#66cdaa"
   "mediumblue" "#0000cd"
   "mediumorchid" "#ba55d3"
   "mediumpurple" "#9370db"
   "mediumseagreen" "#3cb371"
   "mediumslateblue" "#7b68ee"
   "mediumspringgreen" "#00fa9a"
   "mediumturquoise" "#48d1cc"
   "mediumvioletred" "#c71585"
   "midnightblue" "#191970"
   "mintcream" "#f5fffa"
   "mistyrose" "#ffe4e1"
   "moccasin" "#ffe4b5"
   "navajowhite" "#ffdead"
   "navy" "#000080"
   "oldlace" "#fdf5e6"
   "olive" "#808000"
   "olivedrab" "#6b8e23"
   "orange" "#ffa500"
   "orangered" "#ff4500"
   "orchid" "#da70d6"
   "palegoldenrod" "#eee8aa"
   "palegreen" "#98fb98"
   "paleturquoise" "#afeeee"
   "palevioletred" "#db7093"
   "papayawhip" "#ffefd5"
   "peachpuff" "#ffdab9"
   "peru" "#cd853f"
   "pink" "#ffc0cb"
   "plum" "#dda0dd"
   "powderblue" "#b0e0e6"
   "purple" "#800080"
   "red" "#ff0000"
   "rosybrown" "#bc8f8f"
   "royalblue" "#4169e1"
   "saddlebrown" "#8b4513"
   "salmon" "#fa8072"
   "sandybrown" "#f4a460"
   "seagreen" "#2e8b57"
   "seashell" "#fff5ee"
   "sienna" "#a0522d"
   "silver" "#c0c0c0"
   "skyblue" "#87ceeb"
   "slateblue" "#6a5acd"
   "slategray" "#708090"
   "slategrey" "#708090"
   "snow" "#fffafa"
   "springgreen" "#00ff7f"
   "steelblue" "#4682b4"
   "tan" "#d2b48c"
   "teal" "#008080"
   "thistle" "#d8bfd8"
   "tomato" "#ff6347"
   "turquoise" "#40e0d0"
   "violet" "#ee82ee"
   "wheat" "#f5deb3"
   "white" "#ffffff"
   "whitesmoke" "#f5f5f5"
   "yellow" "#ffff00"
   "yellowgreen" "#9acd32"})

(defn str->color
  "Attempt to parse a color from a string."
  [^String s]
  (cond
   (re-matches hex-re s)
   (hex-str->rgb s)

   (re-matches rgb-re s)
   (rgb-str->rgb s)

   (re-matches rgba-re s)
   (rgba-str->rgba s)

   (re-matches hsl-re s)
   (hsl-str->hsl s)

   (re-matches hsla-re s)
   (hsla-str->hsla s)

   :else
   (if-let [h (-> (string/lower-case s)
                  (string/replace #"[^a-z]" "")
                  (color-name->hex))]
     (rgb h)
     (throw (ex-info (str "Unable to convert " (pr-str s) " to a color")
                     {:given s
                      :expected `(~'or (~'re-matches ~hex-re ~s)
                                       (~'re-matches ~rgb-re ~s)
                                       (~'re-matches ~rgba-re ~s)
                                       (~'re-matches ~hsl-re ~s)
                                       (~'re-matches ~hsla-re ~s)
                                       (~'color-name->hex ~s))})))))

(extend-type String
  IRGB
  (-rgb [s]
    (rgb (str->color s)))

  IRGBA
  (-rgba [s]
    (rgba (str->color s)))

  IHSL
  (-hsl [s]
    (hsl (str->color s)))

  IHSLA
  (-hsla [s]
    (hsla (str->color s)))

  IRed
  (-red [s]
    (.-r ^RGB (-rgb s)))

  IGreen
  (-green [s]
    (.-g ^RGB (-rgb s)))

  IBlue
  (-blue [s]
    (.-b ^RGB (-rgb s)))

  IHue
  (-hue [s]
    (.-h ^HSL (-hsl s)))

  ISaturation
  (-saturation [s]
    (.-s ^HSL (-hsl s)))

  ILightness
  (-lightness [s]
    (.-l ^HSL (-hsl s)))

  IAlpha
  (-alpha [s]
    (.-a ^RGBA (-rgba s))))

;;; Keyword

;; TODO: Error messages here will appear in the String
;; implementation. This is potentially confusing.
(extend-type clojure.lang.Keyword
  IRGB
  (-rgb [k]
    (rgb (name k)))

  IRGBA
  (-rgba [k]
    (-rgba (name k)))

  IHSL
  (-hsl [k]
    (-hsl (name k)))

  IHSLA
  (-hsla [k]
    (-hsla (name k)))

  IAlpha
  (-alpha [k]
    (-alpha (-rgba k)))

  IRed
  (-red [k]
    (-red (-rgb k)))

  IGreen
  (-green [k]
    (-green (-rgb k)))

  IBlue
  (-blue [k]
    (-blue (-rgb k)))

  IHue
  (-hue [k]
    (-hue (-hsl k)))

  ISaturation
  (-saturation [k]
    (-saturation (-hsl k)))

  ILightness
  (-lightness [k]
    (-lightness (-hsl k))))

;;; Vector

(defn bound
  "Return function which takes a number and returns it if (<= a n b),
  returns a if (< n 0), or b if (< b n)."
  [a b]
  (fn [n]
    (if (<= a b)
      (min b (max n a))
      (min a (max n b)))))

(def
  ^{:arglists '([n])}
  rgb-bounded
  (bound 0 255))

(def
  ^{:arglists '([n])}
  sl-bounded
  (bound 0 100))

(def
  ^{:arglists '([n])}
  alpha-bounded
  (bound 0 1))


(extend-type clojure.lang.PersistentVector
  ;; The wrapper functions are used on purpose so we can leverage
  ;; validation on the return values.
  IRGB
  (-rgb [v]
    (RGB. (red v) (green v) (blue v)))

  IRGBA
  (-rgba [v]
    (RGBA. (red v) (green v) (blue v) (alpha v)))

  IHSL
  (-hsl [v]
    (HSL. (hue v) (saturation v) (lightness v)))

  IHSLA
  (-hsla [v]
    (HSLA. (hue v) (saturation v) (lightness v) (alpha v)))

  IAlpha
  (-alpha [v]
    (get v 3 1.0))

  IRed
  (-red [v]
    (rgb-bounded (get v 0 0)))

  IGreen
  (-green [v]
    (rgb-bounded (get v 1 0)))

  IBlue
  (-blue [v]
    (rgb-bounded (get v 2 0)))

  IHue
  (-hue [v]
    (get v 0 0))

  ISaturation
  (-saturation [v]
    (sl-bounded (get v 1 0)))

  ILightness
  (-lightness [v]
    (sl-bounded (get v 2 0))))
