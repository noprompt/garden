(ns garden.def-test
  (:require [clojure.test :refer :all]
            [garden.def.alef]
            [garden.keyframes.alef]
            [garden.stylesheet.alef]))

(garden.def.alef/defrule a)

(garden.def.alef/defrule sub-headings :h4 :h5 :h6)

(deftest defrule-test
  (testing "defrule"
    (is (= (a {:font-weight "bold"})
           [#{:a}
            {:font-weight "bold"}]))

    (is (= (sub-headings
            {:font-weight "normal"})
           [#{:h4 :h5 :h6}
            {:font-weight "normal"}]))))

(garden.def.alef/defcssfn bar)

(garden.def.alef/defcssfn foo
  "This is a docstring."
  ([x] [x])
  ([x y] [x y]))

(deftest defcssfn-test
  (is (= (:name (bar 1))
         "bar"))

  (is (= (:args (bar 1))
         '(1)))

  (is (= (:args (foo 1))
         [1]))

  (is (= (:args (foo 1 2))
         [1 2]))

  (is (= (:doc (meta #'foo))
         "This is a docstring."))

  (is (= (:arglists (meta #'foo))
         '([x] [x y]))))


(garden.def.alef/defkeyframes fade-out
  [:from
   {:opacity 1}]
  [:to
   {:opacity 0}])

(deftest defkeyframes-test
  (is (= "fade-out"
         (:name fade-out)))

  (is (= {:from [{:opacity 1}]
          :to [{:opacity 0}]}
         (:frames fade-out))))
