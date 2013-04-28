(ns garden.units-test
  (:require [clojure.test :refer :all]
            [garden.units :as u]))

(deftest test-unit-arthimetic
  (let [μm (u/make-unit-fn :μm)
        μm+ (u/make-unit-adder :μm)
        μm- (u/make-unit-subtractor :μm)
        μm* (u/make-unit-multiplier :μm)
        μm-div (u/make-unit-divider :μm)]
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
      (is (= (μm 1/2) (μm-div 2)))
      (is (thrown? ArithmeticException (μm-div 2 0))))))

(deftest test-px
  (testing "px checking"
    (is (u/px? (u/px 0)))
    (is (not (u/px? 1))))
  (testing "px addition"
    (is (= (u/px 2) (u/px+ 1 1))))
  (testing "px subtraction"
    (is (= (u/px 2) (u/px- 4 2))))
  (testing "px multiplication"
    (is (= (u/px 2) (u/px* 1 2))))
  (testing "px division"
    (is (= (u/px 2) (u/px-div 4 2)))
    (is (thrown? ArithmeticException (u/px-div 2 0))))
  (testing "px conversion"
    (is (= (u/px 1) (u/px (u/px 1))))
    (is (= (u/px 37.795275591) (u/px (u/cm 1))))
    (is (= (u/px 16) (u/px (u/pc 1))))
    (is (= (u/px 3.7795275591) (u/px (u/mm 1))))
    (is (= (u/px 1.3333333333) (u/px (u/pt 1))))
    (is (thrown? IllegalArgumentException (u/px (u/deg 1))))
    (is (thrown? IllegalArgumentException (u/px (u/grad 1))))
    (is (thrown? IllegalArgumentException (u/px (u/rad 1))))
    (is (thrown? IllegalArgumentException (u/px (u/turn 1))))
    (is (thrown? IllegalArgumentException (u/px (u/s 1))))
    (is (thrown? IllegalArgumentException (u/px (u/ms 1))))
    (is (thrown? IllegalArgumentException (u/px (u/Hz 1))))
    (is (thrown? IllegalArgumentException (u/px (u/kHz 1))))))

