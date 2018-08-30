(ns garden.compiler-test
  (:require
   #?(:cljs [cljs.test :as t :refer-macros [is are deftest testing]]
      :clj  [clojure.test :as t :refer [is are deftest testing]])
   #?(:clj  [garden.types :as types]
      :cljs [garden.types :as types :refer [CSSFunction CSSUnit]])
   [garden.color :as color]
   [garden.compiler :refer [compile-css expand render-css]]
   [garden.stylesheet :refer (at-import at-media at-keyframes at-supports)])
  #?(:clj
     (:import garden.types.CSSFunction
              garden.types.CSSUnit)))

;; Helpers

(defn render= [x y]
  (= (first (render-css (expand x)))
     y))

(defn compile= [x y & {:as flags}]
  (-> (merge flags {:pretty-print? false})
      (compile-css x)
      (= y)))

(def test-vendors ["moz" "webkit"])

;; Tests
(deftest render-css-test
  (testing "maps"
    (is (render= {:a 1}
                 "a: 1;"))
    (is (render= {:a "b"}
                 "a: b;"))
    (is (render= {:a :b}
                 "a: b;"))
    (is (render= {:a {:b {:c 1}}}
                 "a-b-c: 1;"))
    (is (render= {:a (sorted-set 1 2 3)}
                 "a: 1;\na: 2;\na: 3;"))
    (is (render= {:a [1 2 3]}
                 "a: 1, 2, 3;"))
    (is (render= {:a [[1 2] 3 [4 5]]}
                 "a: 1 2, 3, 4 5;"))

    (testing "ordering"
      (is (render= {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7 :h 8}
                   "a: 1;\nb: 2;\nc: 3;\nd: 4;\ne: 5;\nf: 6;\ng: 7;\nh: 8;"))
      (is (not (render= {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7 :h 8 :i 9}
                        "a: 1;\nb: 2;\nc: 3;\nd: 4;\ne: 5;\nf: 6;\ng: 7;\nh: 8;\ni: 9;")))
      (is (not (render= (array-map :a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7 :h 8 :i 9 :j 10)
                        "a: 1;\nb: 2;\nc: 3;\nd: 4;\ne: 5;\nf: 6;\ng: 7;\nh: 8;\ni: 9;\nj: 10;")))
      (is (render= (sorted-map :a 1 :k 11 :b 2 :j 10 :c 3 :i 9 :d 4 :h 8 :e 5 :g 7 :f 6)
                   "a: 1;\nb: 2;\nc: 3;\nd: 4;\ne: 5;\nf: 6;\ng: 7;\nh: 8;\ni: 9;\nj: 10;\nk: 11;"))
      (is (render= (sorted-map-by #(compare %2 %1) :a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7 :h 8 :i 9 :j 10 :k 11)
                   "k: 11;\nj: 10;\ni: 9;\nh: 8;\ng: 7;\nf: 6;\ne: 5;\nd: 4;\nc: 3;\nb: 2;\na: 1;" ))))

  (testing "vectors"
    (is (compile= [:a {:x 1} {:y 2}]
                  "a{x:1;y:2}"))
    (is (compile= [:a {:x 1} [:b {:y 2}]]
                  "a{x:1}a b{y:2}"))
    (is (compile= [:a :b {:x 1} [:c :d {:y 2}]]
                  "a,b{x:1}a c,a d,b c,b d{y:2}"))
    (is (compile= [:a :b '({:x 1}) '([:c :d {:y 2}])]
                  "a,b{x:1}a c,a d,b c,b d{y:2}"))
    (is (compile= [[:a {:x 1}] [:b {:y 2}]]
                  "a{x:1}b{y:2}"))
    (is (compile= [:a [{:x 1} [:b {:y 1}]]]
                  "a b{y:1}"))
    (is (compile= [:a {:x 1} [[:b {:y 1}]]]
                  "a{x:1}a b{y:1}")))

  (testing "colors"
    (is (render= (color/hsla 30 40 50 0.5)
                 "hsla(30, 40%, 50%, 0.5)"))))

(deftest at-media-test
  (let [flags {:pretty-print? false}]
    (are [x y] (= (compile-css flags x) y)
      (at-media {:screen true} [:h1 {:a :b}])
      "@media screen{h1{a:b}}"

      (list (at-media {:screen true}
                      [:h1 {:a :b}])
            [:h2 {:c :d}])
      "@media screen{h1{a:b}}h2{c:d}"

      (list [:a {:a "b"}
             (at-media {:screen true}
                       [:&:hover {:c "d"}])])
      "a{a:b}@media screen{a:hover{c:d}}"

      (at-media {:toast true}
                [:h1 {:a "b"}])
      "@media toast{h1{a:b}}"

      (at-media {:bacon :only}
                [:h1 {:a "b"}])
      "@media only bacon{h1{a:b}}"

      (at-media {:sad false}
                [:h1 {:a "b"}])
      "@media not sad{h1{a:b}}"

      (at-media {:-vendor-prefix-x "2"}
                [:h1 {:a "b"}])
      "@media(-vendor-prefix-x:2){h1{a:b}}"

      (at-media {:min-width (CSSUnit. :em 1)}
                [:h1 {:a "b"}])
      "@media(min-width:1em){h1{a:b}}")

    (let [re #"@media (?:happy and not sad|not sad and happy)"
          compiled (compile-css
                    {:pretty-print? false}
                    (at-media {:happy true :sad false}
                              [:h1 {:a "b"}]))]
      (is (re-find re compiled)))))

(deftest at-supports-test
  (let [flags {:pretty-print? false}]
    (are [x y] (= (compile-css flags x) y)
               (at-supports {:display :grid} [:h1 {:a :b}])
               "@supports(display:grid){h1{a:b}}"

               (list (at-supports {:display :grid}
                               [:h1 {:a :b}])
                     [:h2 {:c :d}])
               "@supports(display:grid){h1{a:b}}h2{c:d}"

               (list [:a {:a "b"}
                      (at-supports {:display :grid}
                                   [:&:hover {:c "d"}])])
               "a{a:b}@supports(display:grid){a:hover{c:d}}"

               (at-supports {:-vendor-prefix-x "2"}
                            [:h1 {:a "b"}])
               "@supports(-vendor-prefix-x:2){h1{a:b}}"

               (at-supports {:min-width (CSSUnit. :em 1)}
                            [:h1 {:a "b"}])
               "@supports(min-width:1em){h1{a:b}}")

    (let [re #"@supports(?:\(-vendor-prefix-x:2\) and \(display:grid\)|\(display:grid\) and \(-vendor-prefix-x:2\))"
          compiled (compile-css
                     {:pretty-print? false}
                     (at-supports {:-vendor-prefix-x "2" :display :grid}
                                  [:h1 {:a "b"}]))]
      (is (re-find re compiled)))))

(deftest parent-selector-test
  (testing "parent selector references"
    (is (compile= [:a [:&:hover {:x :y}]]
                  "a:hover{x:y}"))

    (is (compile= [:a [:& {:x :y}]]
                  "a{x:y}"))

    (is (compile= [:a [:&:b {:x :y}]]
                  "a:b{x:y}"))

    (is (compile= [:a [:&:b :&:c {:x :y}]]
                  "a:b,a:c{x:y}"))

    (is (compile= [:a
                   (at-media {:max-width "1em"}
                             [:&:hover {:x :y}])]
                  "@media(max-width:1em){a:hover{x:y}}"))

    (is (compile= [:a
                   (at-supports {:display :grid}
                                [:&:hover {:x :y}])]
                  "@supports(display:grid){a:hover{x:y}}"))

    (is (compile= (at-media {:screen true}
                            [:a {:f "bar"}
                             (at-media {:print true}
                                       [:& {:g "foo"}])])
                  "@media screen{a{f:bar}}@media print{a{g:foo}}"))))

(deftest css-function-test
  (testing "CSSFunction"
    (is (render= (CSSFunction. :url "background.jpg")
                 "url(background.jpg)"))

    (is (render= (CSSFunction. :daughter [:alice :bob])
                 "daughter(alice, bob)"))

    (is (render= (CSSFunction. :x [(CSSFunction. :y 1) (CSSFunction. :z 2)])
                 "x(y(1), z(2))"))))

(deftest at-rule-test
  (testing "@import"
    (let [url "http://example.com/foo.css"]
      (is (render= (at-import url)
                   "@import \"http://example.com/foo.css\";"))
      (is (render= (at-import url {:screen true})
                   "@import \"http://example.com/foo.css\" screen;"))))

  (testing "@keyframes"
    (let [kfs (at-keyframes :id
                            [:from {:x 0}]
                            [:to {:x 1}])]
      (is (compile= kfs
                    "@keyframes id{from{x:0}to{x:1}}"))
      (is (compile= [:a {:d kfs}]
                    "a{d:id}")))))

(deftest flag-tests
  (testing ":vendors"
    (let [compiled (compile-css
                    {:vendors test-vendors :pretty-print? false}
                    [:a ^:prefix {:a 1 :b 1}])]

      (are [re] (re-find re compiled)
        #"-moz-a:1"
        #"-webkit-a:1"
        #"a:1"
        #"-moz-b:1"
        #"-webkit-b:1"
        #"b:1"))

    (let [compiled (compile-css
                    {:vendors test-vendors :pretty-print? false}
                    (at-keyframes "fade"
                                  [:from {:foo "bar"}]
                                  [:to {:foo "baz"}]))]
      (is (re-find #"@-moz-keyframes" compiled))
      (is (re-find #"@-webkit-keyframes" compiled))
      (is (re-find #"@keyframes" compiled))))

  (testing ":auto-prefix"
    (let [compiled (compile-css
                    {:auto-prefix #{:a "b"}
                     :vendors test-vendors
                     :pretty-print? false}
                    [:a {:a 1 :b 1 :c 1}])]

      (are [re] (re-find re compiled)
        #"-moz-a:1"
        #"-webkit-a:1"
        #"a:1"
        #"-moz-b:1"
        #"-webkit-b:1"
        #"b:1"
        #"c:1")

      (is (not (re-find #"-moz-c:1" compiled)))
      (is (not (re-find #"-webkit-c:1" compiled)))))

  (testing ":query-expressions :nesting-behavior"
    (let [compiled (compile-css
                     {:query-expressions {:nesting-behavior :merge}
                      :pretty-print?     false}
                     (at-media {:screen true}
                              [:a {:x 1}]
                              (at-media {:print true}
                                        [:b {:y 1}])))]
      (is (re-find #"@media screen\{a\{x:1\}\}" compiled))
      (is (re-find #"@media (?:screen and print|print and screen)\{b\{y:1\}\}" compiled)))))
