(ns garden.core-test
  (:use clojure.test
        garden.core))

(defn output [f]
  (slurp (format "test/garden/output/%s.css" f)))

(defn compressed [s]
  (output (str "compressed-" (name s))))

(defn compact [s]
  (output (str "compact-" (name s))))

(defn expanded [s]
  (output (str "expanded-" (name s))))

(def css-compressed (partial css {:output-style :compressed}))
(def css-compact (partial css {:output-style :compact}))
(def css-expanded (partial css {:output-style :expanded}))

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
        s4 (vary-meta (list s1 s3) assoc :media true :screen true)]

    (testing "compressed output"
      (is (= (css-compressed s1) (compressed :s1)))
      (is (= (css-compressed s1') (compressed :s1)))
      (is (= (css-compressed s1'') (compressed :s1)))
      (is (= (css-compressed s2) (compressed :s2)))
      (is (= (css-compressed s3) (compressed :s3)))
      (is (= (css-compressed s4) (compressed :s4))))

    (testing "compact output"
      (is (= (css-compact s1) (compact :s1)))
      (is (= (css-compact s1') (compact :s1)))
      (is (= (css-compact s1'') (compact :s1)))
      (is (= (css-compact s2) (compact :s2)))
      (is (= (css-compact s3) (compact :s3)))
      (is (= (css-compact s4) (compact :s4))))

    (testing "exapnded output"
      (is (= (css-expanded s1) (expanded :s1)))
      (is (= (css-expanded s1') (expanded :s1)))
      (is (= (css-expanded s1'') (expanded :s1)))
      (is (= (css-expanded s2) (expanded :s2)))
      (is (= (css-expanded s3) (expanded :s3)))
      (is (= (css-expanded s4) (expanded :s4))))))
