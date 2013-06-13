(ns garden.color
  (:require [clojure.string :as s]
            [garden.util :as u]))

(defn rgb
  "Create an RGB color map."
  ([[r g b :as vs]]
     (if (every? #(u/between? % 0 255) vs)
       {:red r :green g :blue b}
       (throw
        (IllegalArgumentException. "RGB values must be between 0 and 255"))))
  ([r g b]
     (rgb [r g b])))

(defn rgba
  "Create an RGBA color."
  ([[r g b a]]
     (if (u/between? a 0 1)
       (assoc (rgb [r g b]) :alpha a)
       (throw
        (IllegalArgumentException. "Alpha value must be between 0 and 1"))))
  ([r g b a]
     (rgba [r g b a])))

(defn hsl
  "Create an HSL color."
  ([[h s l]]
     (if (and (between? s 0 100)
              (between? l 0 100))
       {:hue (mod h 360) :saturation s :lightness l}
       (throw
        (IllegalArgumentException. "Saturation and luminosity must be between 0(%) and 100(%)"))))
  ([h s l]
     (hsl [h s l])))

(defn hsla
  "Create an HSLA color."
  ([[h s l a]]
     (if (u/between? a 0 1)
       (assoc (hsl [h s l]) :alpha a)
       (throw
        (IllegalArgumentException. "Alpha value must be between 0 and 1"))))
  ([h s l a]
     (hsla [h s l a])))

(defn rgb?
  "Return true if color is an RGB color."
  [color]
  (and (map? color)
       (every? #{:red :green :blue} color)))

(defn hsl? [color]
  "Return true if color is an HSL color."
  (and (map? color)
       (every? #{:hue :saturation :lightness} color)))

(defn color? [x]
  "Return true if x is a color."
  (or (rgb? x) (hsl? x)))

(def ^{:doc "Regular expression for matching a hexadecimal color.
             Matches hexadecimal colors of length three or six possibly
             lead by a \"#\". The color portion is captured."}
  hex-re #"#?([\da-fA-F]{3}|[\da-fA-F]{6})")

(defn hex->color
  "Convert a hexadecimal color to an RGB color map."
  [s]
  (when-let [[_ hex] (re-matches hex-re s)]
    (let [hex (if (= 3 (count hex))
                (apply str (mapcat #(list % %) hex))
                hex)]
      (rgb (map #(Integer/parseInt % 16) (re-seq #"[\da-fA-F]{2}" hex))))))

(defn color->hex
  "Convert an RGB color map to a hexadecimal color."
  [{r :red g :green b :blue}]
  (letfn [(hex-part [v]
            (s/replace (format "%2s" (Integer/toString v 16)) " " "0"))]
    (apply str \# (map hex-part [r g b]))))
