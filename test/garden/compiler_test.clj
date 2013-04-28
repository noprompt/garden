(ns garden.compiler_test
  (:require [clojure.test :refer :all]
            [garden.compiler :refer :all]))

(deftest test-with-output-style
  (testing "with output :compressed"
    (is (= (render-css {:foo "bar"})
           "foo:bar"))
    (is (= (render-css {:foo {:bar {:baz "quux"}}})
           "foo-bar-baz:quux"))
    (is (= (render-css [:foo {:bar "baz"} {:quux "grault"}])
           "foo{bar:baz;quux:grault}"))
    (is (= (render-css [:foo {:bar "baz"} [:quux {:corge "grault"}]])
           "foo{bar:baz}foo quux{corge:grault}"))
    (is (= (render-css [:foo :bar {:bar "baz"} [:quux :corge {:corge "grault"}]])
           "foo,bar{bar:baz}foo quux,foo corge,bar quux,bar corge{corge:grault}"))
    (is (= (render-css [:foo :bar '({:bar "baz"}) '([:quux :corge {:corge "grault"}])])
           "foo,bar{bar:baz}foo quux,foo corge,bar quux,bar corge{corge:grault}"))
    (is (= (render-css [[:foo {:bar "baz"}] [:quux {:corge "grault"}]])
           "foo{bar:baz}quux{corge:grault}")))
  
  (testing "media queries"
    (is (= (compile-css ^:screen [:h1 {:a "b"}])
           "@media screen{h1{a:b}}"))
    (is (= (compile-css ^:screen [[:h1 {:c "d"}] [:h2 {:e "f"}]]
                        [:h1 {:a "b"}])
           "h1{a:b}@media screen{h1{c:d}h2{e:f}}"))
    (is (= (compile-css [:h1 {:a "b"} ^:screen [:&:hover {:c "d"}]])
           "h1{a:b}@media screen{h1:hover{c:d}}"))
    (is (= (compile-css ^:toast [:h1 {:a "b"}])
           "h1{a:b}"))))

