(ns garden.stylesheet.pseudo-classes
  "Functions for defining styles for pseudo classes."
  (:refer-clojure :exclude [empty])
  (:require [garden.util :as u]
            [garden.def :refer [defrule]]))

(doseq [c '[active focus visited hover link
            enabled disabled checked
            first-child last-child only-child
            first-of-type last-of-type
            empty root
            required optional valid invalid
            target]]
  (let [doc (str "Define styles for the :" c " pseudo class.")]
    (eval `(defrule ~(with-meta c {:doc doc})
             ~(str "&:" c)))))

;; CSS3 introduced the new double colon syntax (ie. `::before`) to
;; distinguish the difference between pseudo-classes and
;; pseudo-elements. Browsers accept both notations so why not kill two
;; birds with one stone?

(doseq [c '[before after first-line first-letter]]
  (let [doc (str "Define styles for the CSS3 ::" c
                 " and CSS2 :" c" pseudo class.")]
    (eval `(defrule ~(with-meta c {:doc doc})
             ~(str "&::" c) ~(str "&:" c)))))

(defrule ^{:doc "Define styles for the ::selection and ::-moz-selection
                 pseudo class."}
  selection "&::selection" "&::-moz-selection")

(defn lang
  "Define styles for the :lang pseudo class.

   ex. (lang :fr {:quotes [\"«\" \"»\"]})
       => [\"&:lang(fr) {:quotes [\"«\" \"»\"]}]"
  [l & children]
  [(format "&:lang(%s)" (u/to-str l)) children])

(defn nth-child
  "Define styles for the :nth-child psuedo class.

   ex. (nth-child :even {:background \"green\"})
       => [\"&:nth-child(even)\" {:background \"green\"} "
  [n & children]
  [(format "&:nth-child(%s)" (u/to-str n)) children])

(defn nth-of-type
  "Define styles for the :nth-of-type psuedo class.

   ex. (nth-of-type \"2n+1\" {:font-weight \"bold\"})
       => [\"&:nth-of-type(2n+1)\" {:font-weight \"bold\"} "
  [n & children]
  [(format "&:nth-of-type(%s)" (u/to-str n)) children])
