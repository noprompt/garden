(ns garden.core-test
  (:use clojure.test
        garden.core))

(defn output [f]
  (slurp (format "test/garden/output/%s.css" f)))

(defn compressed [s]
  (output (str "compressed-" (name s))))

(defn expanded [s]
  (output (str "expanded-" (name s))))

(def css-compressed
  (partial css {:pretty-print? false}))

(def css-expanded
  (partial css {:pretty-print? true}))

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
        s4 (vary-meta (list s1 s3) assoc :media {:screen true})]

    (testing "compressed output"
      (is (= (css-compressed s1) (compressed :s1)))
      (is (= (css-compressed s1') (compressed :s1)))
      (is (= (css-compressed s1'') (compressed :s1)))
      (is (= (css-compressed s2) (compressed :s2)))
      (is (= (css-compressed s3) (compressed :s3)))
      (is (= (css-compressed s4) (compressed :s4))))

    (testing "exapnded output"
      (is (= (css-expanded s1) (expanded :s1)))
      (is (= (css-expanded s1') (expanded :s1)))
      (is (= (css-expanded s1'') (expanded :s1)))
      (is (= (css-expanded s2) (expanded :s2)))
      (is (= (css-expanded s3) (expanded :s3)))
      (is (= (css-expanded s4) (expanded :s4))))))
