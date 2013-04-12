(ns garden.stylesheet-test
  (:require [clojure.test :refer :all]
            [garden.stylesheet :refer :all]))

(deftest css-functions
  (testing "url"
    (is (= (str (url "http://foo.css"))
           "url(http://foo.css)")))

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
    (is (= (at-import "http://foo.css")
           "@import \"http://foo.css\";"))
    (is (= (at-import (url "http://foo.css"))
           "@import url(http://foo.css);"))
    (is (= (at-import "http://foo.css" :screen)
           "@import \"http://foo.css\" screen;"))
    (is (= (at-import "http://foo.css" :screen {:orientation :landscape})
           "@import \"http://foo.css\" screen,(orientation:landscape);"))))

