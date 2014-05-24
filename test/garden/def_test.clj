(ns garden.def-test
  (:require [garden.def :refer [defcssfn defkeyframes defrule]]
            [clojure.test :refer :all])
  (:import garden.types.CSSFunction
           garden.types.CSSAtRule))

(defrule a)
(defrule sub-headings :h4 :h5 :h6)

(deftest defrule-test
  (testing "defrule"
    (is (= (a {:font-weight "bold"})
           [:a {:font-weight "bold"}]))
    (is (= (sub-headings {:font-weight "normal"})
           [:h4 :h5 :h6 {:font-weight "normal"}]))))

(defcssfn bar)

(defcssfn foo
  "LOL"
  ([x] x)
  ([x y] [x y]))

(deftest defcssfn-test
  (is (instance? CSSFunction (bar 1)))
  (is (= (:args (bar 1))
         '(1)))
  (is (= (:args (foo 1))
         1))
  (is (= (:args (foo 1 2))
         [1 2]))
  (is (= (:doc (meta #'foo))
         "LOL"))
  (is (= (:arglists (meta #'foo))
         '([x] [x y]))))

(defkeyframes fade-out
  [:from {:opacity 1}]
  [:to {:opacity 0}])

(deftest defkeyframes-test
  (is (instance? CSSAtRule fade-out))
  (is (= :keyframes (:identifier fade-out)))
  (is (= "fade-out" (get-in fade-out [:value :identifier]))))
