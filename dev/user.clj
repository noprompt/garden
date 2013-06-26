(ns user
  (:refer-clojure :exclude [+ - * /])
  (:require [garden.core :refer [css]]
            [garden.util :as u]
            [garden.units :as un]
            [garden.color :as c]
            ;; Comment this line before running tests to prevent
            ;; warning messages. 
            ;;[garden.arithemetic :refer [+ - * /]]
            [clojure.string :as s]
            [clojure.pprint :refer [pprint pp]]))
