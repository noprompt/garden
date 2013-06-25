(ns garden.arithemetic-test
  (:refer-clojure :exclude [+ - * /])
  (:require [garden.units :as u]
            [garden.color :as c])
  (:use clojure.test
        garden.arithemetic))


(deftest sum-test
  (testing "numbers"
    (is (= (+) 0))
    (is (= (+ 1) 1))
    (is (= (+ 1 1) 2))
    (is (= (+ 1 1 1) 3)))

  (testing "units"
   (is (= (+ (u/px 1))
          (u/px 1)))
   (is (= (+ 1 (u/px 1))
          (u/px 2)))
   (is (= (+ 1 (u/px 1) 1)
          (u/px 3)))
   (is (= (+ (c/rgb 0 0 0))
          (c/rgb 0 0 0))))

  (testing "colors"
   (is (= (+ (c/rgb 0 0 0) (c/rgb 1 1 1))
          (c/rgb 1 1 1)))
   (is (= (+ (c/rgb 0 0 0) 1)
          (c/rgb 1 1 1)))
   (is (= (+ 1 (c/rgb 0 0 0))
          (c/rgb 1 1 1)))))

(deftest difference-test
  (testing "numbers"
    (is (= (- 1) -1))
    (is (= (- 1 1) 0))
    (is (= (- 1 1 1) -1)))

  (testing "units"
   (is (= (- (u/px 1))
          (u/px -1)))
   (is (= (- 1 (u/px 1))
          (u/px 0)))
   (is (= (- 1 (u/px 1) 1)
          (u/px -1)))
   (is (= (- (c/rgb 0 0 0))
          (c/rgb 0 0 0))))

  (testing "colors"
   (is (= (- (c/rgb 0 0 0) (c/rgb 1 1 1))
          (c/rgb 0 0 0)))
   (is (= (- (c/rgb 0 0 0) 1)
          (c/rgb 0 0 0)))
   (is (= (- 1 (c/rgb 0 0 0))
          (c/rgb 1 1 1)))))
