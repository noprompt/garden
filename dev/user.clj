(ns user
  (:refer-clojure :exclude [+ - * /])
  (:require [garden.core :refer [css]]
            [garden.util :as u]
            [garden.units :as un]
            [garden.color :as c]
            [garden.def :refer [defrule]]
            ;; Comment this line before running tests to prevent
            ;; warning messages. 
            ;;[garden.arithmetic :refer [+ - * /]]
            [clojure.string :as s]
            [clojure.pprint :refer [pprint pp]]
            [clojure.repl :refer [source doc]]))

(defn debug [x]
  (clojure.pprint/pprint x)
  x)
