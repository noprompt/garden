(ns garden.selectors-test
  #+clj
  (:require
   [clojure.test :refer :all]
   [garden.selectors :as s])
  #+cljs
  (:require
   [cemerick.cljs.test :as t]
   [garden.selectors :as s])
  #+cljs
  (:require-macros
   [cemerick.cljs.test :refer [deftest is testing are]]))

(deftest selector-test
  (testing "selector?"
    (is (s/selector? "h1"))
    (is (s/selector? :h1))
    (is (s/selector? 'h1))
    (is (s/selector? s/h1))
    (is (s/selector? s/hover))
    (is (s/selector? s/first-child))
    (is (s/selector? (s/a)))
    (is (s/selector? (s/a s/hover)))
    (is (s/selector? (s/attr :foo)))
    (is (s/selector? (s/attr :foo := :bar))))

  (testing "rendering selectors"
    (are [x y] (= (s/css-selector x) y)
      s/h1 "h1"
      s/hover ":hover"
      s/first-line "::first-line"
      (s/a s/hover) "a:hover"
      (s/not s/h1) ":not(h1)"
      (s/a s/hover (s/not s/h1)) "a:hover:not(h1)"
      (s/attr :foo := :bar) "[foo=\"bar\"]"
      (s/attr :foo := "\"bar\"") "[foo=\"bar\"]"
      (s/attr :foo := "'bar'") "[foo='bar']"
      (s/descendent s/ul s/li) "ul li"
      (s/descendent s/ul (s/+ s/ol s/li)) "ul ol + li")))

(deftest specificity-test
  (testing "specificity"
    (are [x y] (= (s/specificity x) y)
      "*" 0
      "li" 1
      s/li 1
      "ul li" 2
      (s/descendent s/ul s/li) 2
      "ul ol+li" 3
      (s/descendent s/ul (s/+ s/ol s/li)) 3
      "h1 + *[REL=up]" 11
      "ul ol li.red" 13
      "li.red.level" 21
      "#x34y" 100
      "#s12:not(FOO)" 101)))
