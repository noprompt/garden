(ns garden.compiler_test
  (:require [clojure.test :refer :all]
            [garden.compiler :refer :all]))

(deftest test-with-output-style
  (let [declaration {:foo "bar"}
        declaration' {:foo {:bar {:baz "quux"}}}
        rule [:foo {:bar "baz"} {:quux "grault"}]
        rule' [:foo {:bar "baz"} [:quux {:corge "grault"}]]
        rule'' [:foo :bar {:bar "baz"} [:quux :corge {:corge "grault"}]]
        stylesheet [[:foo {:bar "baz"}] [:quux {:corge "grault"}]]
        media-expr (sorted-map :bar true :baz false :quux "grault")]
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
        (is (= (make-stylesheet stylesheet)
               "foo{bar:baz}quux{corge:grault}"))
        (is (= (make-media-expression media-expr)
               "bar and not baz and (quux:grault)"))
        (is (= (make-media-query media-expr [rule])
               "@media bar and not baz and (quux:grault){foo{bar:baz;quux:grault}}"))))
    (testing "with output :compact"
      (with-output-style :compact
        (is (= (render-css declaration)
               "foo: bar"))
        (is (= (render-css declaration')
               "foo-bar-baz: quux"))
        (is (= (render-css rule)
               "foo { bar: baz; quux: grault; }"))
        (is (= (make-stylesheet stylesheet)
               "foo { bar: baz; }\nquux { corge: grault; }"))
        (is (= (make-media-expression media-expr)
               "bar and not baz and (quux: grault)"))
        (is (= (make-media-expression media-expr)
               "bar and not baz and (quux: grault)"))
        (is (= (make-media-query media-expr stylesheet)
               "@media bar and not baz and (quux: grault) {\n  foo { bar: baz; }\n  quux { corge: grault; }\n}"))))
    (testing "with output :expanded"
      (with-output-style :expanded
        (is (= (render-css declaration)
               "  foo: bar"))
        (is (= (render-css declaration')
               "  foo-bar-baz: quux"))
        (is (= (render-css rule)
               "foo {\n  bar: baz;\n  quux: grault;\n}"))
        (is (= (make-stylesheet stylesheet)
               "foo {\n  bar: baz;\n}\n\nquux {\n  corge: grault;\n}"))
        (is (= (make-media-expression media-expr)
               "bar and not baz and (quux: grault)"))
        (is (= (make-media-query media-expr stylesheet)
               "@media bar and not baz and (quux: grault) {\n\n    foo {\n      bar: baz;\n    }\n    \n    quux {\n      corge: grault;\n    }\n\n}"))))
    (testing "with output :invalid"
      (with-output-style :invalid
        (is (= (render-css declaration)
               "foo:bar"))
        (is (= (render-css declaration')
               "foo-bar-baz:quux"))
        (is (= (render-css rule)
               "foo{bar:baz;quux:grault}"))
        (is (= (make-stylesheet stylesheet)
               "foo{bar:baz}quux{corge:grault}"))
        (is (= (make-media-expression media-expr)
               "bar and not baz and (quux:grault)"))
        (is (= (make-media-query media-expr [rule])
               "@media bar and not baz and (quux:grault){foo{bar:baz;quux:grault}}"))))))

(run-tests)
