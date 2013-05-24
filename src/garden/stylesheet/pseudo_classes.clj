(ns garden.stylesheet.pseudo-classes
  (:refer-clojure :exclude [empty])
  (:require [garden.util :as util]
            [garden.def :refer [rule defrule]]))

(doseq [c '[active focus visited hover link
            enabled disabled checked
            first-child last-child only-child
            first-of-type last-of-type
            empty root
            required optional valid invalid
            target]]
  (eval `(defrule ~c ~(str "&:" c))))

(defrule ^{:doc "CSS3 ::before psuedo selector"}
  before "&::before")

(defrule ^{:doc "CSS3/CSS2 :before psuedo selector."}
  before' "&::before" "&:before")

(defrule ^{:doc "CSS3 ::after psuedo selector"}
  after "&::after")

(defrule ^{:doc "CSS3/CSS2 :after psuedo selector."}
  after' "&::after" "&:after")

(defrule ^{:doc "CSS3 ::first-line psuedo selector"}
  first-line "&::first-line")

(defrule ^{:doc "CSS3/CSS2 :first-line psuedo selector."}
  first-line' "&::first-line" "&:first-line")

(defrule ^{:doc "CSS3 ::first-letter psuedo selector"}
  first-letter "&::first-letter")

(defrule ^{:doc "CSS3/CSS2 :first-letter psuedo selector."}
  first-letter' "&::first-letter" "&:first-letter")

(defrule selection "&::selection" "&::-moz-selection")

(defn lang
  [l & children]
  [(format "&:lang(%s)" (util/to-str l)) children])

(defn nth-child
  [n & children]
  [(format "&:nth-child(%s)" (util/to-str n)) children])

(defn nth-of-type
  [n & children]
  [(format "&:nth-of-type(%s)" (util/to-str n)) children])
