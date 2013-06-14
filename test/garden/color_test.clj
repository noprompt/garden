(ns garden.color-test
  (:use clojure.test
        garden.color))

(def hex-black "#000000")
(def hex-red "#ff0000")
(def hex-green "#00ff00")
(def hex-blue "#0000ff")
(def hex-white "#ffffff")

(def rgb-black {:red 0 :green 0 :blue 0})
(def rgb-red {:red 255 :green 0 :blue 0})
(def rgb-green {:red 0 :green 255 :blue 0})
(def rgb-blue {:red 0 :green 0 :blue 255})
(def rgb-white {:red 255 :green 255 :blue 255})

(def hsl-black {:hue 0 :saturation 0 :lightness 0})
(def hsl-red {:hue 0 :saturation 100 :lightness 50})
(def hsl-green {:hue 120 :saturation 100 :lightness 50})
(def hsl-blue {:hue 240 :saturation 100 :lightness 50})
(def hsl-white {:hue 0 :saturation 100 :lightness 100})

(deftest color-conversion-test 
  (testing "hex->rgb"
    (is (= (hex->rgb hex-black)
           rgb-black))
    (is (= (hex->rgb hex-red)
           rgb-red))
    (is (= (hex->rgb hex-green)
           rgb-green))
    (is (= (hex->rgb hex-blue)
           rgb-blue))
    (is (= (hex->rgb hex-white)
           rgb-white)))

  (testing "rgb->hex"
    (is (= (rgb->hex rgb-black)
           hex-black))
    (is (= (rgb->hex rgb-red)
           hex-red))
    (is (= (rgb->hex rgb-green)
           hex-green))
    (is (= (rgb->hex rgb-blue)
           hex-blue)))

  (testing "hsl->rgb"
    (is (= (hsl->rgb hsl-black)
           rgb-black))
    (is (= (hsl->rgb hsl-red)
           rgb-red))
    (is (= (hsl->rgb hsl-green)
           rgb-green))
    (is (= (hsl->rgb hsl-blue)
           rgb-blue))
    (is (= (hsl->rgb hsl-white)
           rgb-white)))
  )
