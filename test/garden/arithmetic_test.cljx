(ns garden.arithmetic-test
  (:refer-clojure :exclude [+ - * /])
  (:require #+clj [clojure.test :refer :all]
            #+cljs [cemerick.cljs.test :as t]
            [garden.arithmetic :refer [+ - * /]]
            [garden.units :as u]
            [garden.color :as c])
  #+cljs (:require-macros [cemerick.cljs.test :refer [deftest is testing]]))

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
          (u/px 3))))

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
          (u/px -1))))

  (testing "colors"
   (is (= (- (c/rgb 0 0 0) (c/rgb 1 1 1))
          (c/rgb 0 0 0)))
   (is (= (- (c/rgb 0 0 0) 1)
          (c/rgb 0 0 0)))
   (is (= (- 1 (c/rgb 0 0 0))
          (c/rgb 1 1 1)))))

(deftest product-test
  (testing "numbers"
    (is (= (*) 1))
    (is (= (* 1) 1))
    (is (= (* 1 2) 2))
    (is (= (* 1 2 3) 6)))

  (testing "units"
   (is (= (* (u/px 1))
          (u/px 1)))
   (is (= (* 2 (u/px 1))
          (u/px 2)))
   (is (= (* 1 (u/px 2) 3)
          (u/px 6))))

  (testing "colors"
   (is (= (* (c/rgb 0 0 0) (c/rgb 1 1 1))
          (c/rgb 0 0 0)))
   (is (= (* (c/rgb 2 4 8) 2)
          (c/rgb 4 8 16)))
   (is (= (* 1 (c/rgb 3 6 9) 2)
          (c/rgb 6 12 18)))))

(deftest difference-test
  (testing "numbers"
    (is (= (/ 1) 1))
    (is (= (/ 1 2) 1/2))
    (is (= (/ 1 2 4) 1/8))
    #+clj (is (thrown? ArithmeticException
                (/ 1 0))))

  (testing "units"
   (is (= (/ (u/px 2))
          (u/px 1/2)))
   (is (= (/ 1 (u/px 2))
          (u/px 1/2)))
   (is (= (/ 1 (u/px 2) 4)
          (u/px 1/8)))
   #+clj (is (thrown? ArithmeticException
                (/ (u/px 1) 0))))

  (testing "colors"
    (is (= (/ (c/rgb 0 0 0) (c/rgb 1 1 1))
           (c/rgb 0 0 0)))
    (is (= (/ (c/rgb 4 8 16) 2)
           (c/rgb 2 4 8)))
    (is (= (/ 1 (c/rgb 2 2 2))
           (c/rgb 1/2 1/2 1/2)))
    #+clj (is (thrown? ArithmeticException
                 (/ (c/rgb 1 1 1) 0)))))
