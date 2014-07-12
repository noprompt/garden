(ns garden.units-test
  (:refer-clojure :exclude [rem])
  (:require
   #+clj
   [clojure.test :refer :all]
   #+cljs
   [cemerick.cljs.test :as t]
   #+clj
   [garden.types :as types]
   #+cljs
   [garden.types :as types :refer [CSSUnit]]
   [garden.units :as units])
  #+cljs
  (:require-macros
   [cemerick.cljs.test :refer [deftest is are testing]])
  #+clj
  (:import garden.types.CSSUnit
           clojure.lang.ExceptionInfo))

(deftest test-unit-arthimetic
  (let [μm (units/make-unit-fn :μm)
        μm+ (units/make-unit-adder :μm)
        μm- (units/make-unit-subtractor :μm)
        μm* (units/make-unit-multiplier :μm)
        μm-div (units/make-unit-divider :μm)]
    (testing "addition"
      (is (= (μm 0) (μm+)))
      (is (= (μm 2) (μm+ 1 1))))
    (testing "subtraction"
      (is (= (μm -1) (μm- 1)))
      (is (= (μm 2) (μm- 4 2))))
    (testing "multiplication"
      (is (= (μm 1) (μm*)))
      (is (= (μm 2) (μm* 1 2))))
    (testing "division"
      (is (= (μm 1) (μm-div 1)))
      (is (= (μm #+clj 1/2 #+cljs 0.5) (μm-div 2)))
      #+clj (is (thrown? ArithmeticException (μm-div 2 0))))))

(deftest test-px
  (testing "px checking"
    (is (units/px? (units/px 0)))
    (is (not (units/px? 1))))
  (testing "px addition"
    (is (= (units/px 2)
           (units/px+ 1 1))))
  (testing "px subtraction"
    (is (= (units/px 2)
           (units/px- 4 2))))
  (testing "px multiplication"
    (is (= (units/px 2)
           (units/px* 1 2))))
  (testing "px division"
    (is (= (units/px 2)
           (units/px-div 4 2)))
    #+clj
    (is (thrown? ArithmeticException (units/px-div 2 0))))
  (testing "px conversion"
    (are [x y] (= x y)
      (units/px 1)            (units/px (units/px 1))
      (units/px 37.795275591) (units/px (units/cm 1))
      (units/px 16)           (units/px (units/pc 1))
      (units/px 3.7795275591) (units/px (units/mm 1))
      (units/px 1.3333333333) (units/px (units/pt 1)))
    (is (thrown? ExceptionInfo (units/px (units/deg 1))))
    (is (thrown? ExceptionInfo (units/px (units/grad 1))))
    (is (thrown? ExceptionInfo (units/px (units/rad 1))))
    (is (thrown? ExceptionInfo (units/px (units/turn 1))))
    (is (thrown? ExceptionInfo (units/px (units/s 1))))
    (is (thrown? ExceptionInfo (units/px (units/ms 1))))
    (is (thrown? ExceptionInfo (units/px (units/Hz 1))))
    (is (thrown? ExceptionInfo (units/px (units/kHz 1))))))

(deftest unit-utils
  (testing "read-unit"
    (are [x y] (= x y)
      (units/cm 1) (units/read-unit "1cm")
      (units/mm 1) (units/read-unit "1mm")
      (units/in 1) (units/read-unit "1in")
      (units/px 1) (units/read-unit "1px")
      (units/pt 1) (units/read-unit "1pt")
      (units/pc 1) (units/read-unit "1pc")
      (units/percent 1) (units/read-unit "1%")
      (units/em 1) (units/read-unit "1em")
      (units/rem 1) (units/read-unit "1rem")
      (units/ex 1) (units/read-unit "1ex")
      (units/ch 1) (units/read-unit "1ch")
      (CSSUnit. :rem 1) (units/read-unit "1rem")
      (units/vw 1) (units/read-unit "1vw")
      (units/vh 1) (units/read-unit "1vh")
      (units/vmin 1) (units/read-unit "1vmin")
      (units/vmax 1) (units/read-unit "1vmax")
      (units/deg 1) (units/read-unit "1deg")
      (units/grad 1) (units/read-unit "1grad")
      (units/rad 1) (units/read-unit "1rad")
      (units/turn 1) (units/read-unit "1turn")
      (units/ms 1) (units/read-unit "1ms")
      (units/s 1) (units/read-unit "1s")
      (units/kHz 1) (units/read-unit "1kHz")
      (units/Hz 1) (units/read-unit "1Hz")
      (units/dpi 1) (units/read-unit "1dpi")
      (units/dpcm 1) (units/read-unit "1dpcm")
      (units/dppx 1) (units/read-unit "1dppx"))))
