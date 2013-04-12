(ns garden.util
  "Utility functions used by Garden.")

(defprotocol ToString
  (^String to-str [this] "Convert a value into a string."))

(extend-protocol ToString
  clojure.lang.Keyword
  (to-str [this]
    (name this))
  Object
  (to-str [this]
    (str this))
  nil
  (to-str [_]
    ""))

(defn ^String as-str
  [& args]
  (apply str (map to-str args)))

(defn ^Boolean natural?
  "True if n is a natural number."
  [n]
  (and (integer? n) (pos? n)))

(defn ^String wrap-quotes
  "Wrap a string with double quotes."
  [s]
  (str \" s \"))