(ns garden.compiler-test
  (:require
   #?(:cljs [cljs.test :as t :refer-macros [is are deftest testing]]
      :clj [clojure.test :as t :refer [is are deftest testing]])
   #?(:clj [garden.types :as types]
      :cljs [garden.types :as types :refer [CSSFunction CSSUnit]])
   [garden.color :as color]
   [garden.compiler :refer [compile-css expand render-css]]
   [garden.stylesheet :refer (at-import at-media at-keyframes at-supports at-page)])
  #?(:clj
     (:import garden.types.CSSFunction
              garden.types.CSSUnit)))

;; Helpers

(defn render [x]
  (first (render-css (expand x))))

(defn compile-helper [x & {:as flags}]
  (-> (merge flags {:pretty-print? false})
      (compile-css x)))

(def test-vendors ["moz" "webkit"])

;; Tests
(deftest render-css-test
  (testing "maps"
    (is (= "a: 1;" (render {:a 1})))
    (is (= "a: b;" (render {:a "b"})))
    (is (= "a: b;" (render {:a :b})))
    (is (= "a-b-c: 1;" (render {:a {:b {:c 1}}})))
    (is (= "a: 1;\na: 2;\na: 3;" (render {:a (sorted-set 1 2 3)})))
    (is (= "a: 1, 2, 3;" (render {:a [1 2 3]})))
    (is (= "a: 1 2, 3, 4 5;" (render {:a [[1 2] 3 [4 5]]})))

    (testing "ordering"
      (is (= "a: 1;\nb: 2;\nc: 3;\nd: 4;\ne: 5;\nf: 6;\ng: 7;\nh: 8;"
             (render {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7 :h 8})))
      (is (not= "a: 1;\nb: 2;\nc: 3;\nd: 4;\ne: 5;\nf: 6;\ng: 7;\nh: 8;\ni: 9;"
                (render {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7 :h 8 :i 9})))
      (is (not= "a: 1;\nb: 2;\nc: 3;\nd: 4;\ne: 5;\nf: 6;\ng: 7;\nh: 8;\ni: 9;\nj: 10;"
                (render (array-map :a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7 :h 8 :i 9 :j 10))))
      (is (= "a: 1;\nb: 2;\nc: 3;\nd: 4;\ne: 5;\nf: 6;\ng: 7;\nh: 8;\ni: 9;\nj: 10;\nk: 11;"
             (render (sorted-map :a 1 :k 11 :b 2 :j 10 :c 3 :i 9 :d 4 :h 8 :e 5 :g 7 :f 6))))
      (is (= "k: 11;\nj: 10;\ni: 9;\nh: 8;\ng: 7;\nf: 6;\ne: 5;\nd: 4;\nc: 3;\nb: 2;\na: 1;"
             (render (sorted-map-by #(compare %2 %1) :a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7 :h 8 :i 9 :j 10 :k 11))))))

  (testing "vectors"
    (is (= "a{x:1;y:2}" (compile-helper [:a {:x 1} {:y 2}])))
    (is (= "a{x:1}a b{y:2}" (compile-helper [:a {:x 1} [:b {:y 2}]])))
    (is (= "a,b{x:1}a c,a d,b c,b d{y:2}" (compile-helper [:a :b {:x 1} [:c :d {:y 2}]])))
    (is (= "a,b{x:1}a c,a d,b c,b d{y:2}" (compile-helper [:a :b '({:x 1}) '([:c :d {:y 2}])])))
    (is (= "a{x:1}b{y:2}" (compile-helper [[:a {:x 1}] [:b {:y 2}]])))
    (is (= "a b{y:1}" (compile-helper [:a [{:x 1} [:b {:y 1}]]])))
    (is (= "a{x:1}a b{y:1}" (compile-helper [:a {:x 1} [[:b {:y 1}]]]))))

  (testing "colors"
    (is (= "hsla(30, 40%, 50%, 0.5)" (render (color/hsla 30 40 50 0.5))))
    ;; there was a bug which incorrectly changed "0%" to "0", see https://github.com/noprompt/garden/issues/120
    (is (= "hsla(0, 0%, 0%, 0.0)" (render (color/hsla 0 0 0 0.0))))
    (is (= "a{color:hsla(0,0%,0%,0.0)}" (compile-helper [:a {:color (color/hsla 0 0 0 0.0)}])))))

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
    (is (= "a:hover{x:y}"
           (compile-helper [:a [:&:hover {:x :y}]])))
    (is (= "a{x:y}"
           (compile-helper [:a [:& {:x :y}]])))
    (is (= "a:b{x:y}"
           (compile-helper [:a [:&:b {:x :y}]])))
    (is (= "a:b,a:c{x:y}"
           (compile-helper [:a [:&:b :&:c {:x :y}]])))
    (is (= "@media(max-width:1em){a:hover{x:y}}"
           (compile-helper [:a
                            (at-media {:max-width "1em"}
                                      [:&:hover {:x :y}])])))
    (is (= "@supports(display:grid){a:hover{x:y}}"
           (compile-helper [:a
                            (at-supports {:display :grid}
                                         [:&:hover {:x :y}])])))
    (is (= "@media screen{a{f:bar}}@media print{a{g:foo}}"
           (compile-helper (at-media {:screen true}
                                     [:a {:f "bar"}
                                      (at-media {:print true}
                                                [:& {:g "foo"}])]))))))

(deftest css-function-test
  (testing "CSSFunction"
    (is (= "url(background.jpg)"
           (render (CSSFunction. :url "background.jpg"))))
    (is (= "daughter(alice, bob)"
           (render (CSSFunction. :daughter [:alice :bob]))))
    (is (= "x(y(1), z(2))"
           (render (CSSFunction. :x [(CSSFunction. :y 1) (CSSFunction. :z 2)]))))))

(deftest at-rule-test
  (testing "@import"
    (let [url "http://example.com/foo.css"]
      (is (= "@import \"http://example.com/foo.css\";"
             (render (at-import url))))
      (is (= "@import \"http://example.com/foo.css\" screen;"
             (render (at-import url {:screen true}))))))

  (testing "@keyframes"
    (let [kfs (at-keyframes :id
                            [:from {:x 0}]
                            [:to {:x 1}])]
      (is (= "@keyframes id{from{x:0}to{x:1}}"
             (compile-helper kfs)))
      (is (= "a{d:id}"
             (compile-helper [:a {:d kfs}]))))
    ;; there was a bug which incorrectly changed "0%" to "0", see https://github.com/noprompt/garden/issues/120
    (is (= "@keyframes id{0%{x:0}100%{x:1}}"
           (compile-helper (at-keyframes :id
                                         [:0% {:x 0}]
                                         [:100% {:x 1}]))))
    (is (= "@keyframes id{0%{x:0}100%{x:1}}"
           (compile-helper (at-keyframes :id
                                         ["0%" {:x 0}]
                                         ["100%" {:x 1}]))))
    (is (= ".foo{color:red}@keyframes id{0%{x:0}100%{x:1}}"
           (compile-helper [:.foo
                            {:color "red"}
                            (at-keyframes :id
                                          ["0%" {:x 0}]
                                          ["100%" {:x 1}])]))))
  (testing "@page"
    (is (= "@page{size:A4;margin:10px}"
           (compile-helper (at-page nil {:size "A4"
                                         :margin "10px"}))))
    (is (= "@page{size:A3;@bottom-right-corner{content:'Page ' counter(page)}}"
           (compile-helper (at-page nil
                                    {:size "A3"}
                                    ["@bottom-right-corner" {:content "'Page ' counter(page)"}]))))
    (is (= "@page cover{size:A3;@bottom-right-corner{content:'Page ' counter(page)}}"
           (compile-helper (at-page :cover
                                    {:size "A3"}
                                    ["@bottom-right-corner" {:content "'Page ' counter(page)"}]))))))

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

  (testing ":media-expressions :nesting-behavior"
    (let [compiled (compile-css
                    {:media-expressions {:nesting-behavior :merge}
                     :pretty-print? false}
                    (at-media {:screen true}
                              [:a {:x 1}]
                              (at-media {:print true}
                                        [:b {:y 1}])))]
      (is (re-find #"@media screen\{a\{x:1\}\}" compiled))
      (is (re-find #"@media (?:screen and print|print and screen)\{b\{y:1\}\}" compiled)))))
