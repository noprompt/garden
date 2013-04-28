(ns garden.core-test
  (:use clojure.test
        garden.core)
  (:require [garden.util :refer [with-output-style]]))

(defn output [f]
  (slurp (format "test/garden/output/%s.css" f)))

(defn compressed [s]
  (output (str "compressed-" (name s))))

(defn compact [s]
  (output (str "compact-" (name s))))

(defn expanded [s]
  (output (str "expanded-" (name s))))

(deftest test-css
  (let [s1 [:h1 {:font-weight "bold"}]
        s1' [:h1 {:font {:weight "bold"}}]
        s1'' (for [i (range 1 2)]
               [(str "h" i) {:font-weight "bold"}])
        s2 [:h1 {:font-weight "bold"}
            [:a {:text-decoration "none"}]]
        s3 [:a
            {:font-weight "normal"}
            [:&:hover {:font-weight "bold"}]]
        s4 (vary-meta (list s1 s3) assoc :screen true)]

    (testing "compressed output"
      (with-output-style :compressed
        (is (= (css s1) (compressed :s1)))
        (is (= (css s1') (compressed :s1)))
        (is (= (css s1'') (compressed :s1)))
        (is (= (css s2) (compressed :s2)))
        (is (= (css s3) (compressed :s3)))
        (is (= (css s4) (compressed :s4)))))

    (testing "compact output"
      (with-output-style :compact
        (is (= (css s1) (compact :s1)))
        (is (= (css s1') (compact :s1)))
        (is (= (css s1'') (compact :s1)))
        (is (= (css s2) (compact :s2)))
        (is (= (css s3) (compact :s3)))
        (is (= (css s4) (compact :s4)))))

    (testing "exapnded output"
      (with-output-style :expanded
        (is (= (css s1) (expanded :s1)))
        (is (= (css s1') (expanded :s1)))
        (is (= (css s1'') (expanded :s1)))
        (is (= (css s2) (expanded :s2)))
        (is (= (css s3) (expanded :s3)))
        (is (= (css s4) (expanded :s4)))))))
