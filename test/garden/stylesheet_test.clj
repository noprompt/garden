(ns garden.stylesheet-test
  (:require [clojure.test :refer :all]
            [garden.stylesheet :refer :all]))

(deftest css-functions
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
           "toggle(italic,bold)")))

  (testing "at-import"
    (is (= (at-import "http://example.com/foo.css")
           "@import \"http://example.com/foo.css\";"))
    (is (= (at-import (url "http://example.com/foo.css"))
           "@import url(http://example.com/foo.css);"))
    (is (= (at-import "http://example.com/foo.css" :screen)
           "@import \"http://example.com/foo.css\" screen;"))
    (is (= (at-import "http://example.com/foo.css" :screen {:orientation :landscape})
           "@import \"http://example.com/foo.css\" screen,(orientation:landscape);"))))

