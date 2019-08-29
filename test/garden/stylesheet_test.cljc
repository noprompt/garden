(ns garden.stylesheet-test
  (:require
   #?(:cljs [cljs.test :as t :refer-macros [is are deftest testing]]
      :clj  [clojure.test :as t :refer [is are deftest testing]])
   [garden.color.alef]
   [garden.stylesheet.alef :refer [rule]])
  #?(:clj
     (:import clojure.lang.ExceptionInfo)))

(deftest rule-test
    (testing "rule"
      (is (= ((rule "a") {:text-decoration "none"})
             ["a" {:text-decoration "none"}]))
      (is (= ((rule :a {:text-decoration "none"}))
             [:a {:text-decoration "none"}]))
      (is (thrown? ExceptionInfo (rule 1)))))

