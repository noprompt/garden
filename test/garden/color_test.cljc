(ns garden.color-test
  (:require [clojure.test :as t :include-macros true]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [clojure.test.check.clojure-test :include-macros true :refer [defspec]]
            [clojure.string :as string]
            [garden.color.alef :as c])
  #?(:clj
     (:import garden.color.alef.Hsl
              garden.color.alef.Hsla
              garden.color.alef.Rgb
              garden.color.alef.Rgba)))

;; ---------------------------------------------------------------------
;; Helpers

(defn rgb-str [[r g b]]
  {:pre [(nat-int? r) (nat-int? g) (nat-int? b)]}
  (str "rgb(" (string/join "," [r g b]) ")"))

(defn rgba-str [[r g b a]]
  {:pre [(nat-int? r) (nat-int? g) (nat-int? b) (pos? a)]}
  (str "rgba(" (string/join "," [r g b a]) ")"))

(defn hsl-str [[h s l]]
  {:pre [(nat-int? h) (nat-int? s) (nat-int? l)]}
  (str "hsl(" (string/join "," [h (str s "%") (str l "%")]) ")"))

(defn hsla-str [[h s l a]]
  {:pre [(nat-int? h) (nat-int? s) (nat-int? l) (pos? a)]}
  (str "hsla(" (string/join "," [h (str s "%") (str l "%") a]) ")"))

;; ---------------------------------------------------------------------
;; Generators

(def gen-rgb-channel
  (gen/choose 0 255))

(def gen-hue
  (gen/choose 0 360))

(def gen-saturation
  (gen/choose 0 100))

(def gen-lightness
  (gen/choose 0 100))

(def gen-alpha-channel
  (gen/such-that
   pos?
   (gen/double* {:infinite? false
                 :NaN? false
                 :min 0
                 :max 1})))

(def gen-hex-char
  (gen/fmap char
            (gen/one-of [(gen/choose 0x30 0x39)
                         (gen/choose 0x41 0x46)
                         (gen/choose 0x61 0x66)])))

(def gen-css-hex
  (gen/fmap (fn [chars]
              (str "#" (apply str chars)))
            (gen/one-of [(gen/vector gen-hex-char 3)
                         (gen/vector gen-hex-char 6)])))

;; ---------------------------------------------------------------------
;; Color function tests

(defspec red-spec
  (prop/for-all [ch gen-rgb-channel]
    (let [rgb (c/Rgb. ch 0 0)
          hsl (c/hsl rgb)]
      (= ch
         (c/red rgb)
         (c/red hsl)))))

(defspec green-spec
  (prop/for-all [ch gen-rgb-channel]
    (let [rgb (c/Rgb. 0 ch 0)
          hsl (c/hsl rgb)]
      (= ch
         (c/green rgb)
         (c/green hsl)))))

(defspec blue-spec
  (prop/for-all [ch gen-rgb-channel]
    (let [rgb (c/Rgb. 0 0 ch)
          hsl (c/hsl rgb)]
      (= ch
         (c/blue rgb)
         (c/blue hsl)))))

(defspec hue-spec
  (prop/for-all [h gen-hue]
    (let [hsl (c/Hsl. h 0 0)]
      (= h (c/hue hsl)))))

(defspec saturation-spec
  (prop/for-all [s gen-saturation]
    (let [hsl (c/Hsl. 0 s 0)]
      (= s
         (c/saturation hsl)))))

(defspec lightness-spec
  (prop/for-all [l gen-lightness]
    (let [hsl (c/Hsl. 0 0 l)
          rgb (c/rgb hsl)]
      (= l
         (c/lightness hsl)))))

(defspec alpha-spec
  (prop/for-all [ch gen-alpha-channel]
    (let [rgba (c/Rgba. 0 0 0 ch)
          hsla (c/Hsla. 0 0 0 ch)]
      (= ch
         (c/alpha rgba)
         (c/alpha hsla)))))

(defspec rgb-spec
  (prop/for-all [r-ch gen-rgb-channel
                 g-ch gen-rgb-channel
                 b-ch gen-rgb-channel]
    (let [rgb (c/Rgb. r-ch g-ch b-ch)]
      (and (= rgb (c/rgb rgb))
           (= r-ch (c/red rgb))
           (= g-ch (c/green rgb))
           (= b-ch (c/blue rgb))))))

(defspec rgba-spec
  (prop/for-all [r-ch gen-rgb-channel
                 g-ch gen-rgb-channel
                 b-ch gen-rgb-channel
                 a-ch gen-alpha-channel]
    (let [rgba (c/Rgba. r-ch g-ch b-ch a-ch)]
      (and (= rgba (c/rgba rgba))
           (= r-ch (c/red rgba))
           (= g-ch (c/green rgba))
           (= b-ch (c/blue rgba))
           (= a-ch (c/alpha rgba))))))

;; ---------------------------------------------------------------------
;; Conversion tests

(defspec round-trip 10000
  (prop/for-all [i gen/pos-int]
    (= (c/long i)
       (c/long (c/hex i))
       (c/long (c/rgb i))
       (c/long (c/rgba i))
       (c/long (c/hsl i))
       (c/long (c/hsla i)))))

(defspec hsl-to-rgb-is-equal-to-rgb-to-hsl 10000
  (prop/for-all [i gen/pos-int]
    (= (-> i c/hsl c/rgb c/long)
       (-> i c/rgb c/hsl c/long))))

(defspec invert-of-invert-is-equal-to-the-original-value 10000
  (prop/for-all [i gen/pos-int]
    (= (-> i c/invert c/invert c/long)
       i)))

;; ---------------------------------------------------------------------
;; String parsing tests

(defspec rgb-str->rgb-spec
  (prop/for-all [r gen-rgb-channel
                 g gen-rgb-channel
                 b gen-rgb-channel]
    (let [rgb (c/rgb-str->rgb
               (rgb-str [r g b]))]
      (and (c/rgb? rgb)
           (= r (c/red rgb))
           (= g (c/green rgb))
           (= b (c/blue rgb))))))

(defspec rgba-str->rgba-spec
  (prop/for-all [r gen-rgb-channel
                 g gen-rgb-channel
                 b gen-rgb-channel
                 a gen-alpha-channel]
    (let [rgba (c/rgba-str->rgba
                (rgba-str [r g b a]))]
      (and (c/rgba? rgba)
           (= r (c/red rgba))
           (= g (c/green rgba))
           (= b (c/blue rgba))
           (= a (c/alpha rgba))))))

(defspec hsl-str->hsl-spec
  (prop/for-all [h gen-hue
                 s gen-saturation
                 l gen-lightness]
    (let [hsl (c/hsl-str->hsl
               (hsl-str [h s l]))]
      (and (c/hsl? hsl)
           #?@(:clj
               [(== h (c/hue hsl))
                (== s (c/saturation hsl))
                (== l (c/lightness hsl))]
               :cljs ;; == emits several warnings in ClojureScript
               [(= h (c/hue hsl))
                (= s (c/saturation hsl))
                (= l (c/lightness hsl))])))))

(defspec hsla-str->hsla-spec
  (prop/for-all [h gen-hue
                 s gen-saturation
                 l gen-lightness
                 a gen-alpha-channel]
    (let [hsla (c/hsla-str->hsla
                (hsla-str [h s l a]))]
      (and (c/hsla? hsla)
           #?@(:clj
               [(== h (c/hue hsla))
                (== s (c/saturation hsla))
                (== l (c/lightness hsla))
                (== a (c/alpha hsla))]
               :cljs ;; == emits several warnings in ClojureScript
               [(= h (c/hue hsla))
                (= s (c/saturation hsla))
                (= l (c/lightness hsla))
                (= a (c/alpha hsla))])))))

(defspec hex-str->rgb-spec
  (prop/for-all [s gen-css-hex]
    (c/rgb? (c/hex-str->rgb s))))
