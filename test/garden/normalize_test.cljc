(ns garden.normalize-test
  (:require
   [clojure.test :include-macros true :refer [are deftest is testing]]
   [garden.normalize.alef]))

(deftest nest-selector-simple-test
  (testing "simple selector nesting"
    (is (= [:css.selector/compound
            [:css.selector/simple "a"]
            [:css.selector/simple "b"]]
           (garden.normalize.alef/nest-selector
            [:css.selector/simple "b"]
            [:css.selector/simple "a"]))
        "a simple selector nested within another simple selector is a compound selector")

    (is (= [:css.selector/compound
            [:css.selector/simple "a"]
            [:css.selector/simple "b"]
            [:css.selector/simple "c"]]
           (garden.normalize.alef/nest-selector
            [:css.selector/simple "c"]
            [:css.selector/compound
             [:css.selector/simple "a"]
             [:css.selector/simple "b"]]))
        "a compound selector nested within a simple selector is a compound selector")

    (is (= [:css.selector/complex
            [:css.selector/compound
             [:css.selector/simple "a"]
             [:css.selector/simple "c"]]
            [:css.selector/compound
             [:css.selector/simple "b"]
             [:css.selector/simple "c"]]]
           (garden.normalize.alef/nest-selector
            [:css.selector/simple "c"]
            [:css.selector/complex
             [:css.selector/simple "a"]
             [:css.selector/simple "b"]]))
        "a complex selector nested within a simple selector is a complex selector")))



(deftest nest-selector-compound-test
  (testing "compound selector nesting"
    (is (= [:css.selector/compound
            [:css.selector/simple "a"]
            [:css.selector/simple "b"]
            [:css.selector/simple "c"]]
           (garden.normalize.alef/nest-selector
            [:css.selector/simple "c"]
            [:css.selector/compound
             [:css.selector/simple "a"]
             [:css.selector/simple "b"]]))
        "a simple selector nested within a compound selector is a compound selector")

    (is (= [:css.selector/compound
            [:css.selector/simple "a"]
            [:css.selector/simple "b"]
            [:css.selector/simple "c"]
            [:css.selector/simple "d"]]
           (garden.normalize.alef/nest-selector
            [:css.selector/compound
             [:css.selector/simple "c"]
             [:css.selector/simple "d"]]
            [:css.selector/compound
             [:css.selector/simple "a"]
             [:css.selector/simple "b"]]))
        "a compound selector nested within a compound selector is a compound selector")

    (is (= [:css.selector/complex
            [:css.selector/compound
             [:css.selector/simple "a"]
             [:css.selector/simple "c"]
             [:css.selector/simple "d"]]
            [:css.selector/compound
             [:css.selector/simple "b"]
             [:css.selector/simple "c"]
             [:css.selector/simple "d"]]]
           (garden.normalize.alef/nest-selector
            [:css.selector/compound
             [:css.selector/simple "c"]
             [:css.selector/simple "d"]]
            [:css.selector/complex
             [:css.selector/simple "a"]
             [:css.selector/simple "b"]]))
        "a compound selector nested within a complex selector is a complex selector")))

(deftest nest-selector-complex-test
  (testing "complex selector nesting"
    (is (= [:css.selector/complex
            [:css.selector/compound
             [:css.selector/simple "a"]
             [:css.selector/simple "b"]]
            [:css.selector/compound
             [:css.selector/simple "a"]
             [:css.selector/simple "c"]]]
           (garden.normalize.alef/nest-selector
            [:css.selector/complex
             [:css.selector/simple "b"]
             [:css.selector/simple "c"]]
            [:css.selector/simple "a"]))
        "a complex selector nested within a simple selector is a complex selector")

    (is (= [:css.selector/complex
            [:css.selector/compound
             [:css.selector/simple "a"]
             [:css.selector/simple "c"]]
            [:css.selector/compound
             [:css.selector/simple "b"]
             [:css.selector/simple "c"]]
            [:css.selector/compound
             [:css.selector/simple "a"]
             [:css.selector/simple "d"]]
            [:css.selector/compound
             [:css.selector/simple "b"]
             [:css.selector/simple "d"]]]
           (garden.normalize.alef/nest-selector
            [:css.selector/complex
             [:css.selector/simple "c"]
             [:css.selector/simple "d"]]
            [:css.selector/complex
             [:css.selector/simple "a"]
             [:css.selector/simple "b"]]))
        "a complex selector nested within a complex selector is a complex selector")))

(deftest nest-selector-parent-reference-test
  (let [child-selector [:css.selector/parent-reference ":hover"]
        parent-selector [:css.selector/simple "a"]
        expected [:css.selector/simple "a:hover"]
        actual (garden.normalize.alef/nest-selector child-selector parent-selector)]
    (is (= expected
           actual)
        "a parent reference selector nested within a simple selector is a simple selector"))

  (let [child-selector [:css.selector/parent-reference ":hover"]
        parent-selector [:css.selector/compound
                         [:css.selector/simple "h1"]
                         [:css.selector/simple "a"]]
        expected [:css.selector/compound
                  [:css.selector/simple "h1"]
                  [:css.selector/simple "a:hover"]]
        actual (garden.normalize.alef/nest-selector child-selector parent-selector)]
    (is (= expected
           actual)
        "a parent reference selector nested within a compound selector is a compound selector"))

  (let [child-selector [:css.selector/parent-reference ":hover"]
        parent-selector [:css.selector/complex
                         [:css.selector/compound
                          [:css.selector/simple "h1"]
                          [:css.selector/simple "a"]]
                         [:css.selector/compound
                          [:css.selector/simple "h2"]
                          [:css.selector/simple "a"]]]
        expected [:css.selector/complex
                  [:css.selector/compound
                   [:css.selector/simple "h1"]
                   [:css.selector/simple "a:hover"]]
                  [:css.selector/compound
                   [:css.selector/simple "h2"]
                   [:css.selector/simple "a:hover"]]]
        actual (garden.normalize.alef/nest-selector child-selector parent-selector)]
    (is (= expected
           actual)
        "a parent reference selector nested within a complex selector is a complex selector")))

(deftest nest-media-query-no-constraints-no-types-test
  (testing "parent no constraint and no type, child no constraint and no type"
    (let [child-query [:css.media/query
                       [:css/noop]
                       [:css/noop]
                       [:css.media.query/conjunction
                        [:css.media.query/expression
                         [:css.media.query/feature "child-feature"]
                         [:css.media.query/feature "child-value"]]]]
          parent-query [:css.media/query
                        [:css/noop]
                        [:css/noop]
                        [:css.media.query/conjunction
                         [:css.media.query/expression
                          [:css.media.query/feature "parent-feature"]
                          [:css.media.query/feature "parent-value"]]]]
          result-query [:css.media/query
                        [:css/noop]
                        [:css/noop]
                        [:css.media.query/conjunction
                         [:css.media.query/expression
                          [:css.media.query/feature "parent-feature"]
                          [:css.media.query/feature "parent-value"]]
                         [:css.media.query/expression
                          [:css.media.query/feature "child-feature"]
                          [:css.media.query/feature "child-value"]]]]]
      (is (= result-query
             (garden.normalize.alef/nest-media-query
              child-query
              parent-query)))))

  (testing "parent no constraint and type, child no constraint and no type"
    (let [child-query [:css.media/query
                       [:css/noop]
                       [:css/noop]
                       [:css.media.query/conjunction
                        [:css.media.query/expression
                         [:css.media.query/feature "child-feature"]
                         [:css.media.query/feature "child-value"]]]]
          parent-query [:css.media/query
                        [:css/noop]
                        [:css.media.query/type "parent-type"]
                        [:css.media.query/conjunction
                         [:css.media.query/expression
                          [:css.media.query/feature "parent-feature"]
                          [:css.media.query/feature "parent-value"]]]]
          result-query [:css.media/query
                        [:css/noop]
                        [:css.media.query/type "parent-type"]
                        [:css.media.query/conjunction
                         [:css.media.query/expression
                          [:css.media.query/feature "parent-feature"]
                          [:css.media.query/feature "parent-value"]]
                         [:css.media.query/expression
                          [:css.media.query/feature "child-feature"]
                          [:css.media.query/feature "child-value"]]]]]
      (is (= result-query
             (garden.normalize.alef/nest-media-query
              child-query
              parent-query)))))

  (testing "parent no constrain and no type, child no constraint and type"
    (let [child-query [:css.media/query
                       [:css/noop]
                       [:css.media.query/type "child-type"]
                       [:css.media.query/conjunction
                        [:css.media.query/expression
                         [:css.media.query/feature "child-feature"]
                         [:css.media.query/feature "child-value"]]]]
          parent-query [:css.media/query
                        [:css/noop]
                        [:css/noop]
                        [:css.media.query/conjunction
                         [:css.media.query/expression
                          [:css.media.query/feature "parent-feature"]
                          [:css.media.query/feature "parent-value"]]]]
          result-query [:css.media/query
                        [:css/noop]
                        [:css.media.query/type "child-type"]
                        [:css.media.query/conjunction
                         [:css.media.query/expression
                          [:css.media.query/feature "parent-feature"]
                          [:css.media.query/feature "parent-value"]]
                         [:css.media.query/expression
                          [:css.media.query/feature "child-feature"]
                          [:css.media.query/feature "child-value"]]]]]
      (is (= result-query
             (garden.normalize.alef/nest-media-query
              child-query
              parent-query)))))

  (testing "parent not constraint and type, child no constraint and no type"
    (let [child-query [:css.media/query
                       [:css/noop]
                       [:css/noop]
                       [:css.media.query/conjunction
                        [:css.media.query/expression
                         [:css.media.query/feature "child-feature"]
                         [:css.media.query/feature "child-value"]]]]
          parent-query [:css.media/query
                        [:css.media.query/constraint "not"]
                        [:css.media.query/type "parent-type"]
                        [:css.media.query/conjunction
                         [:css.media.query/expression
                          [:css.media.query/feature "parent-feature"]
                          [:css.media.query/feature "parent-value"]]]]
          result-query parent-query]
      (is (= result-query
             (garden.normalize.alef/nest-media-query
              child-query
              parent-query))))

    (testing "parent only constraint and type, child no constraint and no type"
      (let [child-query [:css.media/query
                         [:css/noop]
                         [:css/noop]
                         [:css.media.query/conjunction
                          [:css.media.query/expression
                           [:css.media.query/feature "child-feature"]
                           [:css.media.query/feature "child-value"]]]]
            parent-query [:css.media/query
                          [:css.media.query/constraint "only"]
                          [:css.media.query/type "parent-type"]
                          [:css.media.query/conjunction
                           [:css.media.query/expression
                            [:css.media.query/feature "parent-feature"]
                            [:css.media.query/feature "parent-value"]]]]
            result-query [:css.media/query
                          [:css.media.query/constraint "only"]
                          [:css.media.query/type "parent-type"]
                          [:css.media.query/conjunction
                           [:css.media.query/expression
                            [:css.media.query/feature "parent-feature"]
                            [:css.media.query/feature "parent-value"]]
                           [:css.media.query/expression
                            [:css.media.query/feature "child-feature"]
                            [:css.media.query/feature "child-value"]]]]]
        (is (= result-query
               (garden.normalize.alef/nest-media-query
                child-query
                parent-query))))))

  (testing "parent no constraint and type, child not constraint and type"
    (testing "parent type and child type are equal"
      (let [child-query [:css.media/query
                         [:css.media.query/constraint "not"]
                         [:css.media.query/type "child-type"]
                         [:css.media.query/conjunction
                          [:css.media.query/expression
                           [:css.media.query/feature "child-feature"]
                           [:css.media.query/value "child-value"]]]]
            parent-query [:css.media/query
                          [:css/noop]
                          [:css.media.query/type "parent-type"]
                          [:css.media.query/conjunction
                           [:css.media.query/expression
                            [:css.media.query/feature "parent-feature"]
                            [:css.media.query/value "parent-value"]]]]
            result-query [:css/noop]]
        (is (= result-query
               (garden.normalize.alef/nest-media-query
                child-query
                parent-query)))))

    (testing "parent type and child type are not equal"
      (let [child-query [:css.media/query
                         [:css.media.query/constraint "not"]
                         [:css.media.query/type "child-type"]
                         [:css.media.query/conjunction
                          [:css.media.query/expression
                           [:css.media.query/feature "child-feature"]
                           [:css.media.query/value "child-value"]]]]
            parent-query [:css.media/query
                          [:css/noop]
                          [:css.media.query/type "parent-type"]
                          [:css.media.query/conjunction
                           [:css.media.query/expression
                            [:css.media.query/feature "parent-feature"]
                            [:css.media.query/value "parent-value"]]]]
            result-query [:css/noop]]
        (is (= result-query
               (garden.normalize.alef/nest-media-query
                child-query
                parent-query))))))

  (testing "parent not constraint and type, child not constraint and type"
    (testing "parent type and child type are equal"
      (let [child-query [:css.media/query
                         [:css.media.query/constraint "not"]
                         [:css.media.query/type "type"]
                         [:css.media.query/conjunction
                          [:css.media.query/expression
                           [:css.media.query/feature "child-feature"]
                           [:css.media.query/value "child-value"]]]]
            parent-query [:css.media/query
                          [:css.media.query/constraint "not"]
                          [:css.media.query/type "type"]
                          [:css.media.query/conjunction
                           [:css.media.query/expression
                            [:css.media.query/feature "parent-feature"]
                            [:css.media.query/value "parent-value"]]]]
            result-query [:css.media/query
                          [:css.media.query/constraint "not"]
                          [:css.media.query/type "type"]
                          [:css.media.query/conjunction
                           [:css.media.query/expression
                            [:css.media.query/feature "parent-feature"]
                            [:css.media.query/value "parent-value"]]
                           [:css.media.query/expression
                            [:css.media.query/feature "child-feature"]
                            [:css.media.query/value "child-value"]]]]]
        (is (= result-query
               (garden.normalize.alef/nest-media-query
                child-query
                parent-query)))))

    (testing "parent type and child type are not equal"
      (let [child-query [:css.media/query
                         [:css.media.query/constraint "not"]
                         [:css.media.query/type "child-type"]
                         [:css.media.query/conjunction
                          [:css.media.query/expression
                           [:css.media.query/feature "child-feature"]
                           [:css.media.query/value "child-value"]]]]
            parent-query [:css.media/query
                          [:css.media.query/constraint "not"]
                          [:css.media.query/type "parent-type"]
                          [:css.media.query/conjunction
                           [:css.media.query/expression
                            [:css.media.query/feature "parent-feature"]
                            [:css.media.query/value "parent-value"]]]]
            result-query [:css/noop]]
        (is (= result-query
               (garden.normalize.alef/nest-media-query
                child-query
                parent-query)))))))

(deftest nest-media-query-list-test
  (let [child-query-list [:css.media/query-list
                          [:css.media/query
                           [:css/noop]
                           [:css/noop]
                           [:css.media.query/conjunction
                            [:css.media.query/expression
                             [:css.media.query/feature "child-feature-1"]]]]
                          [:css.media/query
                           [:css/noop]
                           [:css/noop]
                           [:css.media.query/conjunction
                            [:css.media.query/expression
                             [:css.media.query/feature "child-feature-2"]]]]]
        parent-query-list [:css.media/query-list
                           [:css.media/query
                            [:css/noop]
                            [:css/noop]
                            [:css.media.query/conjunction
                             [:css.media.query/expression
                              [:css.media.query/feature "parent-feature-1"]]]]
                           [:css.media/query
                            [:css/noop]
                            [:css/noop]
                            [:css.media.query/conjunction
                             [:css.media.query/expression
                              [:css.media.query/feature "parent-feature-2"]]]]]
        result [:css.media/query-list
                [:css.media/query
                 [:css/noop]
                 [:css/noop]
                 [:css.media.query/conjunction
                  [:css.media.query/expression
                   [:css.media.query/feature "parent-feature-1"]]
                  [:css.media.query/expression
                   [:css.media.query/feature "child-feature-1"]]]]
                [:css.media/query
                 [:css/noop]
                 [:css/noop]
                 [:css.media.query/conjunction
                  [:css.media.query/expression
                   [:css.media.query/feature "parent-feature-2"]]
                  [:css.media.query/expression
                   [:css.media.query/feature "child-feature-1"]]]]
                [:css.media/query
                 [:css/noop]
                 [:css/noop]
                 [:css.media.query/conjunction
                  [:css.media.query/expression
                   [:css.media.query/feature "parent-feature-1"]]
                  [:css.media.query/expression
                   [:css.media.query/feature "child-feature-2"]]]]
                [:css.media/query
                 [:css/noop]
                 [:css/noop]
                 [:css.media.query/conjunction
                  [:css.media.query/expression
                   [:css.media.query/feature "parent-feature-2"]]
                  [:css.media.query/expression
                   [:css.media.query/feature "child-feature-2"]]]]]]
    (is (= result
           (garden.normalize.alef/nest-media-query-list
            child-query-list
            parent-query-list)))))

(deftest normalize-fragment-test
  (testing "a :css/fragment node's children are spliced into it's parent"
    (let [node [:css/stylesheet
                [:css/fragment
                 [:css/rule
                  [:css.selector/simple "x"]
                  [:css/block]]
                 [:css/fragment
                  [:css/rule
                   [:css.selector/simple "y"]
                   [:css/block]]]]
                [:css/noop]
                [:css/rule
                 [:css.selector/simple "z"]
                 [:css/block]]]
          expected [:css/stylesheet
                    [:css/rule
                     [:css.selector/simple "x"]
                     [:css/block]]
                    [:css/rule
                     [:css.selector/simple "y"]
                     [:css/block]]
                    [:css/rule
                     [:css.selector/simple "z"]
                     [:css/block]]]
          actual (garden.normalize.alef/normalize node)]
      (is (= expected
             actual)))))

(deftest normalize-charset-test
  (testing ":css/charset nodes are organized to the first level of the tree"
    (let [node [:css/stylesheet
                [:css/noop]
                [:css/charset "a-charset"]
                [:css/rule
                 [:css.selector/simple "x"]
                 [:css/block]
                 [:css/charset "b-charset"]]]
          expected [:css/stylesheet
                    [:css/charset "a-charset"]
                    [:css/charset "b-charset"]
                    [:css/rule
                     [:css.selector/simple "x"]
                     [:css/block]]]
          actual (garden.normalize.alef/normalize node)]
      (is (= expected
             actual)))))

(deftest normalize-import-test
  (testing ":css/import nodes are organized to the first level of the tree before :css/charset nodes"
    (let [node [:css/stylesheet
                [:css/noop]
                [:css/charset "a-charset"]
                [:css/import "a-import" [:css/noop]]
                [:css/rule
                 [:css.selector/simple "x"]
                 [:css/block]
                 [:css/charset "b-charset"]
                 [:css/import "b-import" [:css/noop]]]]
          expected [:css/stylesheet
                    [:css/charset "a-charset"]
                    [:css/charset "b-charset"]
                    [:css/import "a-import" [:css/noop]]
                    [:css/import "b-import" [:css/noop]]
                    [:css/rule
                     [:css.selector/simple "x"]
                     [:css/block]]]
          actual (garden.normalize.alef/normalize node)]
      (is (= expected
             actual))))

  (testing ":css/import respects media query context"
    (let [node [:css/stylesheet
                [:css/import "a-import"
                 [:css.media/query-list
                  [:css.media/query
                   [:css/noop]
                   [:css/noop]
                   [:css.media.query/conjunction
                    [:css.media.query/expression
                     [:css.media.query/feature "child-feature"]
                     [:css.media.query/value "child-value"]]]]]]]
          expected node
          actual (garden.normalize.alef/normalize node)]
      (is (= expected
             actual)))

    (let [node [:css/stylesheet
                [:css.media/rule
                 [:css.media/query-list
                  [:css.media/query
                   [:css/noop]
                   [:css/noop]
                   [:css.media.query/conjunction
                    [:css.media.query/expression
                     [:css.media.query/feature "parent-feature"]
                     [:css.media.query/value "parent-value"]]]]]
                 [:css/import "a-import"
                  [:css.media/query-list
                   [:css.media/query
                    [:css/noop]
                    [:css/noop]
                    [:css.media.query/conjunction
                     [:css.media.query/expression
                      [:css.media.query/feature "child-feature"]
                      [:css.media.query/value "child-value"]]]]]]]]
          actual (garden.normalize.alef/normalize node)
          expected [:css/stylesheet
                    [:css/import "a-import"
                     [:css.media/query-list
                      [:css.media/query
                       [:css/noop]
                       [:css/noop]
                       [:css.media.query/conjunction
                        [:css.media.query/expression
                         [:css.media.query/feature "parent-feature"]
                         [:css.media.query/value "parent-value"]]
                        [:css.media.query/expression
                         [:css.media.query/feature "child-feature"]
                         [:css.media.query/value "child-value"]]]]]]
                    [:css.media/rule
                     [:css.media/query-list
                      [:css.media/query
                       [:css/noop]
                       [:css/noop]
                       [:css.media.query/conjunction
                        [:css.media.query/expression
                         [:css.media.query/feature "parent-feature"]
                         [:css.media.query/value "parent-value"]]]]]]]]
      (is (= expected
             actual)))))

(deftest normalize-rule-test
  (testing "nested rules are unnested"
    (let [node [:css/stylesheet
                [:css/rule
                 [:css.selector/simple "x"]
                 [:css/block]
                 [:css/rule
                  [:css.selector/simple "y"]
                  [:css/block]]]]
          actual (garden.normalize.alef/normalize node)
          expected [:css/stylesheet
                    [:css/rule
                     [:css.selector/simple "x"]
                     [:css/block]]
                    [:css/rule
                     [:css.selector/compound
                      [:css.selector/simple "x"]
                      [:css.selector/simple "y"]]
                     [:css/block]]]]
      (is (= expected
             actual))))

  (testing "nested rules within media queries are unnested"
    (let [node [:css/stylesheet
                [:css.media/rule
                 [:css.media/query-list
                  [:css.media/query
                   [:css/noop]
                   [:css/noop]
                   [:css/noop]]]
                 [:css/rule
                  [:css.selector/simple "x"]
                  [:css/block]
                  [:css/rule
                   [:css.selector/simple "y"]
                   [:css/block]]]]]
          result [:css/stylesheet
                  [:css.media/rule
                   [:css.media/query-list
                    [:css.media/query
                     [:css/noop]
                     [:css/noop]
                     [:css/noop]]]
                   [:css/rule
                    [:css.selector/simple "x"]
                    [:css/block]]
                   [:css/rule
                    [:css.selector/compound
                     [:css.selector/simple "x"]
                     [:css.selector/simple "y"]]
                    [:css/block]]]]]
      (is (= result
             (garden.normalize.alef/normalize node))))))

(deftest normalize-media-rule-test
  (testing "nested media rules are unnested"
    (let [node [:css/stylesheet
                [:css.media/rule
                 [:css.media/query-list
                  [:css.media/query
                   [:css/noop]
                   [:css/noop]
                   [:css.media.query/conjunction
                    [:css.media.query/feature "parent-feature"]]]]
                 [:css.media/rule
                  [:css.media/query-list
                   [:css.media/query
                    [:css/noop]
                    [:css/noop]
                    [:css.media.query/conjunction
                     [:css.media.query/feature "child-feature"]]]]]]]
          expected [:css/stylesheet
                    [:css.media/rule
                     [:css.media/query-list
                      [:css.media/query
                       [:css/noop]
                       [:css/noop]
                       [:css.media.query/conjunction
                        [:css.media.query/feature "parent-feature"]]]]]
                    [:css.media/rule
                     [:css.media/query-list
                      [:css.media/query
                       [:css/noop]
                       [:css/noop]
                       [:css.media.query/conjunction
                        [:css.media.query/feature "parent-feature"]
                        [:css.media.query/feature "child-feature"]]]]]]
          actual (garden.normalize.alef/normalize node)]
      (is (= expected
             actual))))

  (testing "nested rules are unnested"
    (let [node [:css/stylesheet
                [:css.media/rule
                 [:css.media/query-list
                  [:css.media/query
                   [:css/noop]
                   [:css.media.query/type "screen"]
                   [:css/noop]]]
                 [:css/rule
                  [:css.selector/simple "x"]
                  [:css/block]
                  [:css/rule
                   [:css.selector/simple "y"]
                   [:css/block]]]]]
          result [:css/stylesheet
                  [:css.media/rule
                   [:css.media/query-list
                    [:css.media/query
                     [:css/noop]
                     [:css.media.query/type "screen"]
                     [:css/noop]]]
                   [:css/rule
                    [:css.selector/simple "x"]
                    [:css/block]]
                   [:css/rule
                    [:css.selector/compound
                     [:css.selector/simple "x"]
                     [:css.selector/simple "y"]]
                    [:css/block]]]]]
      (is (= result
             (garden.normalize.alef/normalize node))))))
