(ns garden.compiler_test
  (:require #+clj [clojure.test :refer :all]
            #+cljs [cemerick.cljs.test :as t]
            [garden.compiler :refer [compile-css expand render-css]]
            [garden.stylesheet :refer (at-import at-media at-keyframes)]
            [garden.types])
  #+cljs (:require-macros [cemerick.cljs.test :refer [deftest is testing]])
  (:import garden.types.CSSFunction))

;; Helpers

(defn render= [x y]
  (= (-> x expand render-css first) y))

(defn compile= [x y & {:as flags}]
   (= (->> x (compile-css (merge flags {:pretty-print? false}))) y))

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
                 "a: 1 2, 3, 4 5;")))
  
  (testing "vectors"
    (is (compile= [:a {:x 1} {:y 2}]
                  "a{x:1;y:2}"))
    #+clj
    (is (compile= [:a {:x 1} [:b {:y 2}]]
                  "a{x:1}a b{y:2}"))
    #+clj (is (compile= [:a :b {:x 1} [:c :d {:y 2}]]
                  "a,b{x:1}a c,a d,b c,b d{y:2}"))
    #+clj (is (compile= [:a :b '({:x 1}) '([:c :d {:y 2}])]
                  "a,b{x:1}a c,a d,b c,b d{y:2}"))
    (is (compile= [[:a {:x 1}] [:b {:y 2}]]
                  "a{x:1}b{y:2}"))
    #+clj (is (compile= [:a [{:x 1} [:b {:y 1}]]]
                  "a b{y:1}"))
    #+clj (is (compile= [:a {:x 1} [[:b {:y 1}]]]
                  "a{x:1}a b{y:1}"))
    (is (compile= [:a ^:prefix {:b 1}]
                  "a{-moz-b:1;-webkit-b:1;b:1}"
                  :vendors test-vendors))))

(deftest at-media-test
    (is (compile= (at-media {:screen true} [:h1 {:a :b}])
                  "@media screen{h1{a:b}}"))

    (is (compile= (list (at-media {:screen true}
                          [:h1 {:a :b}])
                        [:h2 {:c :d}])
                  "@media screen{h1{a:b}}h2{c:d}"))

    #+clj
    (is (compile= (list [:a {:a "b"}
                         (at-media {:screen true}
                           [:&:hover {:c "d"}])])
                  "a{a:b}@media screen{a:hover{c:d}}"))

    (is (compile= (at-media {:toast true}
                    [:h1 {:a "b"}])
                  "@media toast{h1{a:b}}"))

    (is (compile= (at-media {:bacon :only}
                    [:h1 {:a "b"}])
                  "@media only bacon{h1{a:b}}"))

    (is (compile= (at-media {:sad false}
                    [:h1 {:a "b"}])
                  "@media not sad{h1{a:b}}"))

    (is (compile= (at-media {:happy true :sad false}
                    [:h1 {:a "b"}])
                  "@media happy and not sad{h1{a:b}}"))

    (is (compile= (at-media {:-vendor-prefix-x "2"}
                    [:h1 {:a "b"}])
                  "@media(-vendor-prefix-x:2){h1{a:b}}")))

#+clj
(deftest parent-selector-test
  (testing "parent selector references"
    (is (compile= [:a [:&:hover {:x :y}]]
                  "a:hover{x:y}"))

    (is (compile= [:a [:& {:x :y}]]
                  "a{x:y}"))

    (is (compile= [:a
                   (at-media {:max-width "1em"}
                     [:&:hover {:x :y}])]
                  "@media(max-width:1em){a:hover{x:y}}"))

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
    (let [compiled (compile-css {:vendors test-vendors} [:a ^:prefix {:a 1 :b 1}])]
      (is (re-find #"-moz-a:1;-webkit-a:1;a:1" compiled))
      (is (re-find #"-moz-b:1;-webkit-b:1;b:1" compiled)))

    (let [compiled (compile-css {:vendors test-vendors}
                                (at-keyframes "fade"
                                  [:from {:foo "bar"}]
                                  [:to {:foo "baz"}]))]
      (is (re-find #"@-moz-keyframes" compiled))
      (is (re-find #"@-webkit-keyframes" compiled))
      (is (re-find #"@keyframes" compiled))))

  (testing ":media-expressions :nesting-behavior"
    (let [compiled (compile-css {:media-expressions {:nesting-behavior :merge}}
                                (at-media {:screen true}
                                  [:a {:x 1}]
                                  (at-media {:print true}
                                    [:b {:y 1}])))]
      (is (re-find #"@media screen\{a\{x:1\}\}" compiled))
      #+clj (is (re-find #"@media (?:screen and print|print and screen)\{b\{y:1\}\}" compiled)))))
