(ns garden.stylesheet-test
  (:use clojure.test
        garden.stylesheet
        garden.stylesheet.functions
        garden.stylesheet.functions.filters))

(deftest properties-test
  (testing "font-family"
    (is (= (font-family "Liberation Mono")
           {:font-family ['("\"Liberation Mono\"")]})
        (= (font-family "Liberation Mono" "Consolas" "Menlo" :mono-space)
           {:font-family ['("\"Liberation Mono\"" "Consolas" "Menlo" :mono-space)]}))))

(deftest directives-test
   (testing "at-import"
     (is (= (at-import "http://example.com/foo.css")
            "@import \"http://example.com/foo.css\";"))
     (is (= (at-import (url "http://example.com/foo.css"))
            "@import url(http://example.com/foo.css);"))
     (is (= (at-import "http://example.com/foo.css" :screen)
            "@import \"http://example.com/foo.css\" screen;"))
     (is (= (at-import "http://example.com/foo.css" :screen {:orientation :landscape})
            "@import \"http://example.com/foo.css\" screen,(orientation:landscape);"))))

(deftest functions
  (testing "url"
    (is (= (str (url "http://example.com/foo.css"))
           "url(http://example.com/foo.css)")))

  (testing "attr"
    (is (= (str (attr :size))
           "attr(size)"))
    (is (= (str (attr :size :px))
           "attr(size px)"))
    (is (= (str (attr :size :px :auto))
           "attr(size px,auto)")))

  (testing "toggle"
    (is (= (str (toggle :italic))
           "toggle(italic)"))
    (is (= (str (toggle :italic :bold))
           "toggle(italic,bold)"))))

(deftest filter-functions-test
   (testing "grayscale"
     (is (= (str (grayscale "10%")) "grayscale(10%)"))
     (is (thrown? IllegalArgumentException (grayscale "10px"))))

   (testing "sepia"
     (is (= (str (sepia "10%")) "sepia(10%)"))
     (is (thrown? IllegalArgumentException (sepia "10px"))))

   (testing "saturate"
     (is (= (str (saturate "10%")) "saturate(10%)"))
     (is (thrown? IllegalArgumentException (saturate "10px"))))

   (testing "invert"
     (is (= (str (invert "10%")) "invert(10%)"))
     (is (thrown? IllegalArgumentException (invert "10px"))))

   (testing "opacity"
     (is (= (str (opacity "10%")) "opacity(10%)"))
     (is (thrown? IllegalArgumentException (opacity "10px"))))

   (testing "brightness"
     (is (= (str (brightness "10%")) "brightness(10%)"))
     (is (thrown? IllegalArgumentException (brightness "10px"))))

   (testing "contrast"
     (is (= (str (contrast "10%")) "contrast(10%)"))
     (is (thrown? IllegalArgumentException (contrast "10px"))))

   (testing "blur"
     (is (= (str (blur "10px")) "blur(10px)"))
     (is (thrown? IllegalArgumentException (blur "10%"))))

   (testing "hue-rotate"
     (is (= (str (hue-rotate "10deg")) "hue-rotate(10deg)"))
     (is (thrown? IllegalArgumentException (hue-rotate "10%")))))

