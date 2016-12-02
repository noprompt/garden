(ns garden.compiler-test
  (:require
   [clojure.test :as t :include-macros true :refer [is are deftest testing]]
   [garden.color :as color]
   [garden.compiler]
   [garden.keyframes :as keyframes]
   [garden.media :as media]
   [garden.units :as units]
   #?(:clj [garden.stylesheet :as stylesheet]
      :cljs [garden.stylesheet :as stylesheet :include-macros true :refer [Function]]))
  #?(:clj
     (:import garden.stylesheet.Function)))

(defn compiles-to [x s]
  (let [expected s
        actual (garden.compiler/compile-css {} x)]
    (is (= expected
           actual))))

(deftest map-compilation-test
  (testing "maps"
    (compiles-to {:a 1}
                 "{a:1;}")

    (compiles-to {:a "b"}
                 "{a:b;}")

    (compiles-to {:a :b}
                 "{a:b;}")

    (compiles-to {:a {:b {:c 1}}}
                 "{a-b-c:1;}")

    (compiles-to {:a [1 2 3]}
                 "{a:1,2,3;}")

    (compiles-to {:a [[1 2] 3 [4 5]]}
                 "{a:1 2,3,4 5;}")

    (compiles-to #:a {:b :a/b}
                 "{a-b:a-b;}")

    (compiles-to {:a (sorted-set 1 2 3)}
                 "{a:1;a:2;a:3;}")))

(deftest vector-compilation-test
  (testing "vectors"
    (compiles-to [:a {:x 1} {:y 2}]
                 "a{x:1;y:2;}")

    (compiles-to [:a {:x 1} [:b {:y 2}]]
                 "a{x:1;}a b{y:2;}")

    (compiles-to [[:a :b]
                  {:x 1}
                  {:y 2}]
                 "a b{x:1;y:2;}")

    (compiles-to [[:a :b]
                  {:x 1}
                  [[:c :d]
                   {:y 2}]]
                 "a b{x:1;}a b c d{y:2;}")

    (compiles-to [(sorted-set :a :b)
                  {:x 1}
                  [(sorted-set :c :d)
                   {:y 2}]]
                 "a,b{x:1;}a c,b c,a d,b d{y:2;}")

    (compiles-to [(sorted-set :a :b)
                  '({:x 1})
                  (list [(sorted-set :c :d)
                         {:y 2}])]
                 "a,b{x:1;}a c,b c,a d,b d{y:2;}")

    (compiles-to (list [:a {:x 1}]
                       [:b {:y 2}])
                 "a{x:1;}b{y:2;}")

    (compiles-to [:a (list {:x 1}
                           [:b {:y 1}])]
                 "a{x:1;}a b{y:1;}")

    (compiles-to [:a {:x 1}
                  (list [:b {:y 1}])]
                 "a{x:1;}a b{y:1;}")))

(deftest color-compilation-test 
  (testing "colors"
    (compiles-to (color/hsl [30 40 50])
                 "hsl(30,40%,50%)")

    (compiles-to (color/hsla [30 40 50 0.5])
                 "hsla(30,40%,50%,0.5)")

    (compiles-to (color/rgb [30 40 50])
                 "rgb(30,40,50)")

    (compiles-to (color/rgba [30 40 50 0.5])
                 "rgba(30,40,50,0.5)")))

(testing "media"
  (compiles-to (media/rule (media/query :screen)
                 [:s {:a :b}])
               "@media screen{s{a:b;}}")

  (compiles-to (media/rule (media/query {:foo nil})
                 [:s {:a :b}])
               "@media (foo){s{a:b;}}")
  
  (compiles-to (media/rule (media/query :screen)
                 [:s1 {:a :b}]
                 (media/rule (media/query :screen)
                   [:s2 {:a :b}]))
               "@media screen{s1{a:b;}}@media screen{s2{a:b;}}")

  (compiles-to (media/rule (media/query :screen)
                 [:s1 {:a :b}]
                 (media/rule (media/query :print)
                   [:s2 {:a :b}]))
               "@media screen{s1{a:b;}}")

  (compiles-to (media/rule (media/query :not :screen)
                 [:s1 {:a :b}]
                 (media/rule (media/query :screen)
                   [:s2 {:a :b}]))
               "@media not screen{s1{a:b;}}")

  (compiles-to (media/rule (media/query :screen)
                 [:s1 {:a :b}]
                 (media/rule (media/query (sorted-map :a 1 :b 2))
                   [:s2 {:a :b}]))
               "@media screen{s1{a:b;}}@media screen and (a:1) and (b:2){s2{a:b;}}")

  (compiles-to [:s1
                (media/rule (media/query :screen)
                  [:s2 {:a :b}])]
               "s1{}@media screen{s1 s2{a:b;}}"))

(deftest parent-selector-test
  (testing "parent selector refrences"
    (compiles-to [:a [:&:hover {:x :y}]]
                 "a{}a:hover{x:y;}")

    (compiles-to [:a [:& {:x :y}]]
                 "a{}a{x:y;}")

    (compiles-to [:a [:&:b {:x :y}]]
                 "a{}a:b{x:y;}")

    (compiles-to [:a [#{:&:b :&:c} {:x :y}]]
                 "a{}a:b,a:c{x:y;}")
    
    (compiles-to [:a
                  (media/rule (media/query {:max-width "1em"})
                    [:&:hover {:x :y}])]
                 "a{}@media (max-width:1em){a:hover{x:y;}}")

    (compiles-to (media/rule (media/query :screen)
                   [:a {:f "bar"}
                    (media/rule (media/query {:max-with "1em"})
                      [:& {:g "foo"}])])
                 "@media screen{a{f:bar;}}@media screen and (max-with:1em){a{g:foo;}}")))

(deftest function-test
  (testing "Function"
    (compiles-to (Function. :url ["background.jpg"])
                 "url(background.jpg)")

    (compiles-to (Function. :daughter [:alice :bob])
                 "daughter(alice,bob)")

    (compiles-to (Function. :x [(Function. :y [1]) (Function. :z [2])])
                 "x(y(1),z(2))")))

(deftest at-import-test
  (testing "@import"
    (let [url "http://example.com/foo.css"]
      (compiles-to (stylesheet/at-import url)
                   "@import \"http://example.com/foo.css\";")

      (compiles-to (stylesheet/at-import url (media/query :screen)) 
                   "@import \"http://example.com/foo.css\" screen;"))))

(deftest at-rule-test 
  (testing "@keyframes"
    (compiles-to (stylesheet/at-keyframes :id
                   [:from {:x 0}]
                   [:to {:x 1}]) 
                 "@keyframes id{0%{x:0;}100%{x:1;}}")

    (compiles-to (stylesheet/at-keyframes :id
                   [:to {:x 1}]
                   [:from {:x 0}]
                   [50 {:y 0}]
                   [25 {:y 1}]) 
                 "@keyframes id{0%{x:0;}25%{y:1;}50%{y:0;}100%{x:1;}}")))

(deftest calc-test
  (testing "calc"
    (compiles-to (stylesheet/calc (+ 1 2))
                 "calc((1+2))")))

(deftest flag-tests
  (testing "^:prefix"
    (let [compiled (garden.compiler/compile-css
                    {:vendors #{"moz" "webkit"}
                     :pretty-print? false}
                    [:a ^:prefix {:a 1 :b 1}])]

      (are [re] (re-find re compiled)
        #"-moz-a:1"
        #"-webkit-a:1"
        #"a:1"
        #"-moz-b:1"
        #"-webkit-b:1"
        #"b:1")))

  (testing "@keyframes prefix"
    (let [vendors #{"moz" "webkit"}
          compiled (garden.compiler/compile-css
                    {:vendors vendors
                     :pretty-print? false}
                    (stylesheet/at-keyframes "fade"
                      [:from {:foo "bar"}]
                      [:to {:foo "baz"}]))]
      (is (re-find #"@-moz-keyframes" compiled))
      (is (re-find #"@-webkit-keyframes" compiled))
      (is (re-find #"@keyframes" compiled))))

  (testing ":prefix-properties"
    (let [compiled (garden.compiler/compile-css
                    {:prefix-properties #{"a" "b"}
                     :vendors #{:moz :webkit}}
                    [:x {:a 1 :b 1 :c 1}])]
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

  (testing ":prefix-functions"
    (let [fn-a (stylesheet/cssfn :a)
          fn-b (stylesheet/cssfn :b)
          fn-c (stylesheet/cssfn :c)
          compiled (garden.compiler/compile-css
                    {:prefix-functions #{"a" "b"}
                     :vendors #{:moz :webkit}}
                    {:x [(fn-a 1) (fn-b 1) (fn-c 1)]})]
      (are [re] (re-find re compiled)
        #"x:-moz-a\(1\),-moz-b\(1\),c\(1\)"
        #"x:-webkit-a\(1\),-webkit-b\(1\),c\(1\)"
        #"x:a\(1\),b\(1\),c\(1\)")

      (is (not (re-find #"-moz-c\(1\)" compiled)))
      (is (not (re-find #"-webkit-c\(1\)" compiled))))))
