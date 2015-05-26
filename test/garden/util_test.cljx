(ns garden.util-test
  (:refer-clojure :exclude [complement])
  (:require
   #+clj [clojure.test :refer :all]
   #+cljs [cemerick.cljs.test :as t]
   [garden.util :as util])
  #+cljs
  (:require-macros
   [cemerick.cljs.test :refer [deftest is testing]]))

(deftest font-family-test
  (is (= (util/font-family "Liberation Mono" 'Consolas :monospace)
         "\"Liberation Mono\", Consolas, monospace")))
