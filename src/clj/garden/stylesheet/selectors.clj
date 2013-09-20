(ns garden.stylesheet.selectors
  "Functions for defining styles for HTML elements."
  (:refer-clojure :exclude [time var map])
  (:require [garden.def :refer [defrule]]))

(doseq [t '[a abbr acronym address area article aside audio
            b base bdi bdo big blockquote body br button
            canvas caption cite code col colgroup command
            datalist dd del details dfn div dl dt
            em embed
            fieldset figcaption figure footer form
            h1 h2 h3 h4 h5 h6 header hr
            i iframe img input ins
            kbd
            label legend li
            main map mark menu meter
            nav noscript
            object
            ol optgroup option output
            p pre progress
            q
            rp rt ruby
            s samp section select small span strong sub summary sup
            table tbody td textarea tfoot th thead time tr
            u ul
            var video
            wbr]]
  (let [doc (str "Define styles for the " t " selector" ".")]
    (eval `(defrule ~(with-meta t {:doc doc})
             ~(keyword t)))))
