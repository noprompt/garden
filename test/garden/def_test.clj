(ns garden.def-test
  (:use clojure.test
        garden.def))

(defrule a)
(defrule sub-headings :h4 :h5 :h6)

(deftest defs
  (testing "defrule"
    (is (= (a {:font-weight "bold"})
           [:a {:font-weight "bold"}]))
    (is (= (sub-headings {:font-weight "normal"})
           [:h4 :h5 :h6 {:font-weight "normal"}]))))
