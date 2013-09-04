(ns user
  (:refer-clojure :exclude [+ - * /])
  (:require [garden.core :refer [css]]
            [garden.util :as util]
            [garden.units :as units :refer [px]]
            [garden.color :as color]
            [garden.def :refer [defrule defcssfn]]
            [garden.stylesheet :as stylesheet]
            ;; Comment this line before running tests to prevent
            ;; warning messages. 
            ;;[garden.arithmetic :refer [+ - * /]]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint pp]]
            [clojure.repl :refer [source doc]]))

(defn debug [x]
  (pprint x)
  x)
