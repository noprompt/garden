(ns garden.parse-test
  (:require [clojure.test :refer :all]
            [garden.parse]))

(deftest parse-vector-test
  (testing "parse-vector"
    (is (= [:css/rule
            [:css.selector/simple "a"]
            [:css.declaration/block]]
           (garden.parse/parse-vector [:a])))

    (is (= [:css/rule
            [:css.selector/simple "a|b"]
            [:css.declaration/block]]
           (garden.parse/parse-vector [:a/b])))

    (is (= [:css/rule
            [:css.selector/simple "a"]
            [:css.declaration/block]
            [:css/rule
             [:css.selector/simple "b"]
             [:css.declaration/block]]]
           (garden.parse/parse-vector [:a [:b]])))

    (is (= [:css/rule
            [:css.selector/complex
             [:css.selector/simple "a"]
             [:css.selector/simple "b"]]
            [:css.declaration/block]]
           (garden.parse/parse-vector [#{:a :b}])))

    (is (= [:css/rule
            [:css.selector/complex
             [:css.selector/compound
              [:css.selector/simple "a"]
              [:css.selector/simple "b"]]]
            [:css.declaration/block]]
           (garden.parse/parse-vector [#{[:a :b]}])))))

(deftest parse-hash-map-test
  (testing "parse-hash-map"
    (is (= [:css.declaration/block
            [:css/declaration
             [:css.declaration/property "a"]
             [:css.declaration/value
              [:css/identifier "b"]]]]
           (garden.parse/parse-hash-map {:a :b})))

    (is (= [:css.declaration/block
            [:css/declaration
             [:css.declaration/property "x-a"]
             [:css.declaration/value
              [:css/identifier "b"]]]]
           (garden.parse/parse-hash-map {:x/a :b})))))

(deftest parse-seq-test
  (testing "parse-seq"
    (is (= [:css/fragment
            [:css/rule
             [:css.selector/simple "a"]
             [:css.declaration/block
              [:css/declaration
               [:css.declaration/property "a"]
               [:css.declaration/value
                [:css/identifier "b"]]]]]
            [:css/rule
             [:css.selector/simple "b"]
             [:css.declaration/block
              [:css/declaration
               [:css.declaration/property "a"]
               [:css.declaration/value
                [:css/identifier "b"]]]]]
            [:css/rule
             [:css.selector/simple "c"]
             [:css.declaration/block
              [:css/declaration
               [:css.declaration/property "a"]
               [:css.declaration/value
                [:css/identifier "b"]]]]]]
           (garden.parse/parse-seq
            (for [s [:a :b :c]]
              [s
               {:a :b}]))))))



(deftest parse-test
  (testing "parse"
    (is (= [:css/rule
            [:css.selector/simple "x"]
            [:css.declaration/block
             [:css/declaration
              [:css.declaration/property "a"]
              [:css.declaration/value
               [:css/number 1]]]]]
           (garden.parse/parse
            [:x
             {:a 1}])))
    
    (is (= [:css/rule
            [:css.selector/simple "x"]
            [:css.declaration/block
             [:css/declaration
              [:css.declaration/property "a"]
              [:css.declaration/value
               [:css/number 1]]]
             [:css/declaration
              [:css.declaration/property "b"]
              [:css.declaration/value
               [:css/number 2]]]]]
           (garden.parse/parse
            [:x
             {:a 1}
             {:b 2}])))

    (is (= [:css/rule
            [:css.selector/simple "x"]
            [:css.declaration/block
             [:css/declaration
              [:css.declaration/property "a"]
              [:css.declaration/value
               [:css/number 1]]]
             [:css/declaration
              [:css.declaration/property "b"]
              [:css.declaration/value
               [:css/number 2]]]]]
           (garden.parse/parse
            [:x
             (for [[p v] [[:a 1] [:b 2]]]
               {p v})])))))
