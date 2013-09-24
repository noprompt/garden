(ns garden.def-test
 (:require #+clj [clojure.test :refer :all]
           #+cljs [cemerick.cljs.test :as t]
           [garden.def :refer [rule #+clj defcssfn #+clj defkeyframes #+clj defrule]])
  #+cljs (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                          [garden.def :refer [defcssfn defkeyframes defrule]])
  #+clj (:import clojure.lang.ExceptionInfo)
  (:import garden.types.CSSFunction
           garden.types.CSSAtRule))

#+clj (defrule a)
#+clj (defrule sub-headings :h4 :h5 :h6)

(deftest rule-test
  (testing "rule"
    (is (= ((rule "a") {:text-decoration "none"})
           ["a" {:text-decoration "none"}]))
    (is (= ((rule :a {:text-decoration "none"}))
           [:a {:text-decoration "none"}]))
    (is (thrown? ExceptionInfo (rule 1)))))

#+clj
(deftest defrule-test
  (testing "defrule"
    (is (= (a {:font-weight "bold"})
           [:a {:font-weight "bold"}]))
    (is (= (sub-headings {:font-weight "normal"})
           [:h4 :h5 :h6 {:font-weight "normal"}]))))

(defcssfn bar)

(defcssfn foo
  ([x] x)
  ([x y] [x y]))

(deftest defcssfn-test
  (is (instance? CSSFunction (bar 1)))
  (is (= '(1) (:args (bar 1))))
  (is (= 1 (:args (foo 1))))
  (is (= [1 2] (:args (foo 1 2)))))

(defkeyframes fade-out
  [:from {:opacity 1}]
  [:to {:opacity 0}])

(deftest defkeyframes-test
  (is (instance? CSSAtRule fade-out))
  (is (= :keyframes (:identifier fade-out)))
  (is (= "fade-out" (get-in fade-out [:value :identifier]))))
