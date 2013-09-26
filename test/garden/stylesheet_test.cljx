(ns garden.stylesheet-test
  #+clj
  (:require [clojure.test :refer :all]
            [garden.stylesheet :refer :all])
  #+clj
  (:import clojure.lang.ExceptionInfo)
  #+cljs
  (:require [cemerick.cljs.test :as t]
            [garden.stylesheet :refer [rule]])
  #+cljs
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]))

(deftest rule-test
  (testing "rule"
    (is (= ((rule "a") {:text-decoration "none"})
           ["a" {:text-decoration "none"}]))
    (is (= ((rule :a {:text-decoration "none"}))
           [:a {:text-decoration "none"}]))
    (is (thrown? ExceptionInfo (rule 1)))))

