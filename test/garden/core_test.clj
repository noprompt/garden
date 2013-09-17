(ns garden.core-test
  (:use clojure.test
        garden.core))

(defn output [f]
  (slurp (format "test/garden/output/%s.css" f)))

(defn expanded [s]
  (output (str "expanded-" (name s))))

(def css-pretty-print
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

    (testing "pretty-print output"
      (is (= (css-pretty-print s1) (expanded :s1)))
      (is (= (css-pretty-print s1') (expanded :s1)))
      (is (= (css-pretty-print s1'') (expanded :s1)))
      (is (= (css-pretty-print s2) (expanded :s2)))
      (is (= (css-pretty-print s3) (expanded :s3)))
      (is (= (css-pretty-print s4) (expanded :s4))))))
