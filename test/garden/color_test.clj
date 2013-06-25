(ns garden.color-test
  (:refer-clojure :exclude [complement])
  (:use clojure.test
        garden.color))

(def hex-black "#000000")
(def hex-red "#ff0000")
(def hex-green "#00ff00")
(def hex-blue "#0000ff")
(def hex-white "#ffffff")

(def rgb-black (rgb 0 0 0))
(def rgb-red (rgb 255 0 0))
(def rgb-green (rgb 0 255 0))
(def rgb-blue (rgb 0 0 255))
(def rgb-white (rgb 255 255 255))

(def hsl-black (hsl 0 0 0))
(def hsl-red (hsl 0 100 50))
(def hsl-green (hsl 120 100 50))
(def hsl-blue (hsl 240 100 50))
(def hsl-white (hsl 0 0 100))

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

  (testing "rgb->hsl"
    (is (= (rgb->hsl rgb-black)
           hsl-black))
    (is (= (rgb->hsl rgb-red)
           hsl-red))
    (is (= (rgb->hsl rgb-green)
           hsl-green))
    (is (= (rgb->hsl rgb-blue)
           hsl-blue))
    (is (= (rgb->hsl rgb-white)
           hsl-white)))) 

(deftest color-math-test
  (testing "color+"
    (is (= (color+ (rgb 0 0 0))
           (rgb 0 0 0)))
    (is (= (color+ (rgb 0 0 0) 1)
           (rgb 1 1 1)))
    (is (= (color+ (rgb 0 0 0) 256)
           (rgb 255 255 255)))
    (is (= (color+ 20 (rgb 130 130 130))
           (rgb 150 150 150))))

  (testing "color-"
    (is (= (color- (rgb 0 0 0))
           (rgb 0 0 0)))
    (is (= (color- (rgb 255 255 255) 256)
           (rgb 0 0 0)))
    (is (= (color- 20 (rgb 150 150 150))
           (rgb 0 0 0)))
    (is (= (color- (rgb 150 150 150) 20)
           (rgb 130 130 130)))
    (is (= (color- (rgb 150 150 150) 20 30)
           (rgb 100 100 100))))

  (testing "color*"
    (is (= (color* (rgb 0 0 0))
           (rgb 0 0 0)))
    (is (= (color* (rgb 0 0 0) 1)
           (rgb 0 0 0)))
    (is (= (color* (rgb 1 1 1) 5)
           (rgb 5 5 5)))
    (is (= (color* 5 (rgb 1 1 1) 5)
           (rgb 25 25 25))))

  (testing "color-div"
    (is (= (color-div (rgb 0 0 0))
           (rgb 0 0 0)))
    (is (= (color-div (rgb 0 0 0) 1)
           (rgb 0 0 0)))
    (is (= (color-div (rgb 1 1 1) 5)
           (rgb 1/5 1/5 1/5)))
    (is (= (color-div 5 (rgb 1 1 1))
           (rgb 5 5 5)))
    (is (thrown? ArithmeticException
                 (color-div (rgb 1 1 1) 0)))))

(deftest color-functions-test
  (testing "rotate-hue"
    (is (= (rotate-hue hsl-black 0)
           (hsl 0 0 0)))
    (is (= (rotate-hue hsl-black 180)
           (hsl 180 0 0)))
    (is (= (rotate-hue hsl-black 360)
           (hsl 0 0 0)))
    (is (= (rotate-hue hsl-black -360)
           (hsl 0 0 0)))
    (is (= (rotate-hue hsl-black -180)
           (hsl 180 0 0))))

  (testing "saturate"
    (is (= (saturate hsl-black 0)
           (hsl 0 0 0)))
    (is (= (saturate hsl-black 50)
           (hsl 0 50 0)))
    (is (= (saturate hsl-black 100)
           (hsl 0 100 0)))
    (is (= (saturate hsl-black 200)
           (hsl 0 100 0))))

  (testing "desaturate"
    (is (= (desaturate hsl-red 0)
           (hsl 0 100 50)))
    (is (= (desaturate hsl-red 50)
           (hsl 0 50 50)))
    (is (= (desaturate hsl-red 100)
           (hsl 0 0 50)))
    (is (= (desaturate hsl-red 200)
           (hsl 0 0 50))))

  (testing "lighten"
    (is (= (lighten rgb-black 0)
           (hsl 0 0 0)))
    (is (= (lighten rgb-black 50)
           (hsl 0 0 50)))
    (is (= (lighten rgb-black 100)
           (hsl 0 0 100)))
    (is (= (lighten rgb-black 200)
           (hsl 0 0 100))))

  (testing "darken"
    (is (= (darken rgb-white 0)
           (hsl 0 0 100)))
    (is (= (darken rgb-white 50)
           (hsl 0 0 50)))
    (is (= (darken rgb-white 100)
           (hsl 0 0 0)))
    (is (= (darken rgb-white 200)
           (hsl 0 0 0))))

  (testing "invert"
    (is (= (invert rgb-white)
           rgb-black))
    (is (= (invert rgb-black)
           rgb-white))))
