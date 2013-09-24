(ns garden.units-test
  (:require #+clj [clojure.test :refer :all]
            #+cljs [cemerick.cljs.test :as t]
            [garden.units :refer [make-unit-fn make-unit-adder make-unit-subtractor make-unit-multiplier make-unit-divider read-unit px px+ px- px* px-div px? cm mm in pt pc percent em  ex ch vw vh vmin vmax deg grad rad turn ms s kHz Hz dpi dpcm dppx]])
  #+cljs (:require-macros [cemerick.cljs.test :refer [deftest is testing]])
  (:import garden.types.CSSUnit
           #+clj clojure.lang.ExceptionInfo))

(deftest test-unit-arthimetic
  (let [μm (make-unit-fn :μm)
        μm+ (make-unit-adder :μm)
        μm- (make-unit-subtractor :μm)
        μm* (make-unit-multiplier :μm)
        μm-div (make-unit-divider :μm)]
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
    (is (px? (px 0)))
    (is (not (px? 1))))
  (testing "px addition"
    (is (= (px 2) (px+ 1 1))))
  (testing "px subtraction"
    (is (= (px 2) (px- 4 2))))
  (testing "px multiplication"
    (is (= (px 2) (px* 1 2))))
  (testing "px division"
    (is (= (px 2) (px-div 4 2)))
    #+clj (is (thrown? ArithmeticException (px-div 2 0))))
  (testing "px conversion"
    (is (= (px 1) (px (px 1))))
    (is (= (px 37.795275591) (px (cm 1))))
    (is (= (px 16) (px (pc 1))))
    (is (= (px 3.7795275591) (px (mm 1))))
    (is (= (px 1.3333333333) (px (pt 1))))
    (is (thrown? ExceptionInfo (px (deg 1))))
    (is (thrown? ExceptionInfo (px (grad 1))))
    (is (thrown? ExceptionInfo (px (rad 1))))
    (is (thrown? ExceptionInfo (px (turn 1))))
    (is (thrown? ExceptionInfo (px (s 1))))
    (is (thrown? ExceptionInfo (px (ms 1))))
    (is (thrown? ExceptionInfo (px (Hz 1))))
    (is (thrown? ExceptionInfo (px (kHz 1))))))

(deftest unit-utils
  (testing "read-unit"
    (is (= (cm 1) (read-unit "1cm")))
    (is (= (mm 1) (read-unit "1mm")))
    (is (= (in 1) (read-unit "1in")))
    (is (= (px 1) (read-unit "1px")))
    (is (= (pt 1) (read-unit "1pt")))
    (is (= (pc 1) (read-unit "1pc")))
    (is (= (percent 1) (read-unit "1%")))
    (is (= (em 1) (read-unit "1em")))
    (is (= (ex 1) (read-unit "1ex")))
    (is (= (ch 1) (read-unit "1ch")))
    (is (= (CSSUnit. :rem 1) (read-unit "1rem")))
    (is (= (vw 1) (read-unit "1vw")))
    (is (= (vh 1) (read-unit "1vh")))
    (is (= (vmin 1) (read-unit "1vmin")))
    (is (= (vmax 1) (read-unit "1vmax")))
    (is (= (deg 1) (read-unit "1deg")))
    (is (= (grad 1) (read-unit "1grad")))
    (is (= (rad 1) (read-unit "1rad")))
    (is (= (turn 1) (read-unit "1turn")))
    (is (= (ms 1) (read-unit "1ms")))
    (is (= (s 1) (read-unit "1s")))
    (is (= (kHz 1) (read-unit "1kHz")))
    (is (= (Hz 1) (read-unit "1Hz")))
    (is (= (dpi 1) (read-unit "1dpi")))
    (is (= (dpcm 1) (read-unit "1dpcm")))
    (is (= (dppx 1) (read-unit "1dppx")))))
