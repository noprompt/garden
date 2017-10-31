(ns garden.color-test
  (:refer-clojure :exclude [complement])
  (:require
   #?(:cljs [cljs.test :as t :refer-macros [is are deftest testing]]
      :clj  [clojure.test :as t :refer [is are deftest testing]])
   [garden.color :as color])
  #?(:clj
     (:import clojure.lang.ExceptionInfo)))

(def hex-black "#000000")
(def hex-red "#ff0000")
(def hex-green "#00ff00")
(def hex-blue "#0000ff")
(def hex-white "#ffffff")

(def rgb-black (color/rgb 0 0 0))
(def rgb-red (color/rgb 255 0 0))
(def rgb-green (color/rgb 0 255 0))
(def rgb-blue (color/rgb 0 0 255))
(def rgb-white (color/rgb 255 255 255))
(def rgb-orange (color/rgb 255 133 27))

(def hsl-black (color/hsl 0 0 0))
(def hsl-red (color/hsl 0 100 50))
(def hsl-green (color/hsl 120 100 50))
(def hsl-blue (color/hsl 240 100 50))
(def hsl-white (color/hsl 0 0 100))
(def hsl-orange (color/hsl 530/19 100 940/17))

(deftest color-conversion-test
  (testing "hex->rgb"
    (are [x y] (= x y)
      (color/hex->rgb hex-black) rgb-black
      (color/hex->rgb hex-red) rgb-red
      (color/hex->rgb hex-green) rgb-green
      (color/hex->rgb hex-blue) rgb-blue
      (color/hex->rgb hex-white) rgb-white))

  (testing "rgb->hex"
    (are [x y] (= x y)
      (color/rgb->hex rgb-black) hex-black
      (color/rgb->hex rgb-red) hex-red
      (color/rgb->hex rgb-green) hex-green
      (color/rgb->hex rgb-blue) hex-blue))

  (testing "hsl->rgb"
    (are [x y] (= x y)
      (color/hsl->rgb hsl-black) rgb-black
      (color/hsl->rgb hsl-red) rgb-red
      (color/hsl->rgb hsl-green) rgb-green
      (color/hsl->rgb hsl-blue) rgb-blue
      (color/hsl->rgb hsl-white) rgb-white))

  (testing "rgb->hsl"
    (are [x y] (= x y)
      (color/rgb->hsl rgb-black) hsl-black
      (color/rgb->hsl rgb-red) hsl-red
      (color/rgb->hsl rgb-green) hsl-green
      (color/rgb->hsl rgb-blue) hsl-blue
      (color/rgb->hsl rgb-white) hsl-white
      (color/rgb->hsl rgb-orange) hsl-orange)))

(deftest color-math-test
  (testing "color+"
    (are [x y] (= x y)
      (color/color+ (color/rgb 0 0 0))
      (color/rgb 0 0 0)

      (color/color+ (color/rgb 0 0 0) 1)
      (color/rgb 1 1 1)

      (color/color+ (color/rgb 0 0 0) 256)
      (color/rgb 255 255 255)

      (color/color+ 20 (color/rgb 130 130 130))
      (color/rgb 150 150 150)))

  (testing "color-"
    (are [x y] (= x y)
      (color/color- (color/rgb 0 0 0))
      (color/rgb 0 0 0)

      (color/color- (color/rgb 255 255 255) 256)
      (color/rgb 0 0 0)

      (color/color- 20 (color/rgb 150 150 150))
      (color/rgb 0 0 0)

      (color/color- (color/rgb 150 150 150) 20)
      (color/rgb 130 130 130)

      (color/color- (color/rgb 150 150 150) 20 30)
      (color/rgb 100 100 100)))

  (testing "color*"
    (are [x y] (= x y)
      (color/color* (color/rgb 0 0 0))
      (color/rgb 0 0 0)

      (color/color* (color/rgb 0 0 0) 1)
      (color/rgb 0 0 0)

      (color/color* (color/rgb 1 1 1) 5)
      (color/rgb 5 5 5)

      (color/color* 5 (color/rgb 1 1 1) 5)
      (color/rgb 25 25 25)))

  (testing "color-div"
    (are [x y] (= x y)
      (color/color-div (color/rgb 0 0 0))
      (color/rgb 0 0 0)

      (color/color-div (color/rgb 0 0 0) 1)
      (color/rgb 0 0 0)

      (color/color-div (color/rgb 1 1 1) 5)
      (color/rgb (/ 1 5) (/ 1 5) (/ 1 5))

      (color/color-div 5 (color/rgb 1 1 1))
      (color/rgb 5 5 5))

    #?(:clj
       (is (thrown? ArithmeticException
                    (color/color-div (color/rgb 1 1 1) 0))))))

(deftest color-functions-test
  (testing "rotate-hue"
    (are [x y] (= x y)
      (-> hsl-black (color/rotate-hue    0) :hue)   0
      (-> hsl-black (color/rotate-hue  180) :hue) 180
      (-> hsl-black (color/rotate-hue  360) :hue)   0
      (-> hsl-black (color/rotate-hue -360) :hue)   0
      (-> hsl-black (color/rotate-hue -180) :hue) 180))

  (testing "saturate"
    (are [x y] (= x y)
      (-> hsl-black (color/saturate   0) :saturation)   0
      (-> hsl-black (color/saturate  50) :saturation)  50
      (-> hsl-black (color/saturate 100) :saturation) 100
      (-> hsl-black (color/saturate 200) :saturation) 100))

  (testing "desaturate"
    (are [x y] (= x y)
      (-> hsl-red (color/desaturate   0) :saturation) 100
      (-> hsl-red (color/desaturate  50) :saturation)  50
      (-> hsl-red (color/desaturate 100) :saturation)   0
      (-> hsl-red (color/desaturate 200) :saturation)   0))

  (testing "lighten"
    (are [x y] (= x y)
      (-> rgb-black (color/lighten   0) :lightness)   0
      (-> rgb-black (color/lighten  50) :lightness)  50
      (-> rgb-black (color/lighten 100) :lightness) 100
      (-> rgb-black (color/lighten 200) :lightness) 100))

  (testing "darken"
    (are [x y] (= x y)
      (-> rgb-white (color/darken   0) :lightness) 100
      (-> rgb-white (color/darken  50) :lightness)  50
      (-> rgb-white (color/darken 100) :lightness)   0
      (-> rgb-white (color/darken 200) :lightness)   0))

  (testing "transparentize"
    (are [x y] (= x y)
      (-> (color/hsla 180 50 50 0.50) (color/transparentize 0.10) :alpha) 0.40
      (-> (color/hsla 180 50 50 0.50) (color/transparentize 0.50) :alpha) 0.00
      (-> (color/hsla 180 50 50 1.00) (color/transparentize 0.50) :alpha) 0.50
      (-> (color/hsla 180 50 50 0.01) (color/transparentize 0.10) :alpha) 0.00))

  (testing "opacify"
    (are [x y] (= x y)
      (-> (color/hsla 180 50 50 0.50) (color/opacify 0.10) :alpha) 0.60
      (-> (color/hsla 180 50 50 0.50) (color/opacify 0.50) :alpha) 1.00
      (-> (color/hsla 180 50 50 1.00) (color/opacify 0.50) :alpha) 1.00
      (-> (color/hsla 180 50 50 0.00) (color/opacify 0.12) :alpha) 0.12))

  (testing "invert"
    (are [x y] (= x y)
      (color/invert rgb-white)
      rgb-black

      (color/invert rgb-black)
      rgb-white)))

(deftest color-from-name-test
  (testing "from-name"
    (is (identical? (color/from-name "aquamarine")
                    (color/from-name "aquamarine")))
    (is (thrown? ExceptionInfo (color/from-name "aqualung")))))

(deftest scale-lightness-test []
  (testing "scale-lightness"
    (is (= 75 (-> (color/hsl 50 50 50) (color/scale-lightness 50) :lightness)))
    (is (= 25 (-> (color/hsl 50 50 50) (color/scale-lightness -50) :lightness)))))

(deftest scale-saturation-test []
  (testing "scale-saturation"
    (is (= 75 (-> (color/hsl 50 50 50) (color/scale-saturation 50) :saturation)))
    (is (= 25 (-> (color/hsl 50 50 50) (color/scale-saturation -50) :saturation)))))

(deftest scale-alpha-test []
  (testing "scale-alpha"
    (is (= 0.75 (-> (color/hsla 180 50 50 0.50) (color/scale-alpha  50) :alpha)))
    (is (= 0.25 (-> (color/hsla 180 50 50 0.50) (color/scale-alpha -50) :alpha)))))

(deftest hex-tests []
  (testing "decrown hex"
    (is (= "aabbcc" (#'garden.color/decrown-hex "#aabbcc"))))
  (testing "expand-hex"
    (is (= "aabbcc" (#'garden.color/expand-hex "#abc")))))

(deftest weighted-mix-test []
  (testing "weighted-mix basics"
    (is (= "#000000" (color/weighted-mix "#000" "#fff" 0)))))
