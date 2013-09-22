(ns garden.def-test
 (:require #+clj [clojure.test :refer :all]
           #+cljs [cemerick.cljs.test :as t]
           [garden.def :refer [rule #+clj defrule]])
  #+cljs (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                          [garden.def :refer [defrule]])
  #+clj (:import clojure.lang.ExceptionInfo))

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
