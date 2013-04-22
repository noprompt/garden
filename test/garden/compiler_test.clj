(ns garden.compiler_test
  (:require [clojure.test :refer :all]
            [garden.compiler :refer :all]
            [garden.util :refer [with-output-style]]))

(deftest test-with-output-style
  (let [declaration {:foo "bar"}
        declaration' {:foo {:bar {:baz "quux"}}}
        rule [:foo {:bar "baz"} {:quux "grault"}]
        rule' [:foo {:bar "baz"} [:quux {:corge "grault"}]]
        rule'' [:foo :bar {:bar "baz"} [:quux :corge {:corge "grault"}]]
        rule''' [:foo :bar '({:bar "baz"}) '([:quux :corge {:corge "grault"}])]
        stylesheet [[:foo {:bar "baz"}] [:quux {:corge "grault"}]]
        stylesheet' (with-meta [:h1 {:foo "bar"}] {:screen true})]
    (testing "with output :compressed"
      (with-output-style :compressed
        (is (= (render-css declaration)
               "foo:bar"))
        (is (= (render-css declaration')
               "foo-bar-baz:quux"))
        (is (= (render-css rule)
               "foo{bar:baz;quux:grault}"))
        (is (= (render-css rule')
               "foo{bar:baz}foo quux{corge:grault}"))
        (is (= (render-css rule'')
               "foo,bar{bar:baz}foo quux,foo corge,bar quux,bar corge{corge:grault}"))
        (is (= (render-css rule''')
               "foo,bar{bar:baz}foo quux,foo corge,bar quux,bar corge{corge:grault}"))
        (is (= (render-css stylesheet)
               "foo{bar:baz}quux{corge:grault}"))))
    (testing "with output :compact"
      (with-output-style :compact
        (is (= (render-css declaration)
               "foo: bar"))
        (is (= (render-css declaration')
               "foo-bar-baz: quux"))
        (is (= (render-css rule)
               "foo { bar: baz; quux: grault; }"))))
    (testing "with output :expanded"
      (with-output-style :expanded
        (is (= (render-css declaration)
               "  foo: bar"))
        (is (= (render-css declaration')
               "  foo-bar-baz: quux"))
        (is (= (render-css rule)
               "foo {\n  bar: baz;\n  quux: grault;\n}"))
        (is (= (render-css stylesheet)
               "foo {\n  bar: baz;\n}\n\nquux {\n  corge: grault;\n}"))))
    (testing "with output :invalid"
      (with-output-style :invalid
        (is (= (render-css declaration)
               "foo:bar"))
        (is (= (render-css declaration')
               "foo-bar-baz:quux"))
        (is (= (render-css rule)
               "foo{bar:baz;quux:grault}"))
        (is (= (render-css stylesheet)
               "foo{bar:baz}quux{corge:grault}"))))
    (testing "media queries"
      (is (= (compile-css ^:screen [:h1 {:a "b"}])
             "@media screen{h1{a:b}}"))
      (is (= (compile-css ^:screen [[:h1 {:c "d"}] [:h2 {:e "f"}]]
                          [:h1 {:a "b"}])
             "h1{a:b}@media screen{h1{c:d}h2{e:f}}"))
      (is (= (compile-css [:h1 {:a "b"} ^:screen [:&:hover {:c "d"}]])
             "h1{a:b}@media screen{h1:hover{c:d}}"))
      (is (= (compile-css ^:toast [:h1 {:a "b"}])
             "h1{a:b}")))))

