(ns garden.util
  "Utility functions used by Garden."
  (:require [clojure.string :as str]))

(defprotocol ToString
  (^String to-str [this] "Convert a value into a string."))

(extend-protocol ToString
  clojure.lang.Keyword
  (to-str [this] (name this))

  Object
  (to-str [this] (str this))

  nil (to-str [this] ""))

(defn ^String as-str
  "Convert a variable number of values into strings."
  [& args]
  (apply str (map to-str args)))

(defn ^Boolean natural?
  "True if n is a natural number."
  [n]
  (and (integer? n) (pos? n)))

(defn between?
  "True if n is a number between a and b."
  [n a b]
  (let [bottom (min a b)
        top (max a b)]
    (and (>= n bottom) (<= n top))))

(defn ^String wrap-quotes
  "Wrap a string with double quotes."
  [s]
  (str \" s \"))

(defn space-join
  "Return a space separated list of values. Subsequences are joined with
   commas."
  [xs]
  (str/join " " (map to-str xs)))

(defn comma-join
  "Return a comma separated list of values. Subsequences are joined with
   spaces."
  [xs]
  (let [ys (for [x xs] (if (sequential? x) (space-join x) (to-str x)))]
    (str/join ", " ys)))

(defn without-meta
  "Return obj with meta removed."
  [obj]
  (with-meta obj nil))

(defn record?
  "Return true if obj is an instance of clojure.lang.IRecord."
  [obj]
  (instance? clojure.lang.IRecord obj))

(defn hash-map?
  "Return true if obj is a map but not a record."
  [obj]
  (and (map? obj) (not (record? obj))))

(defn clip
  "Return a number such that n is no less than a and no more than b."
  [a b n]
  (let [[a b] (if (<= a b) [a b] [b a])] 
    (max a (min b n))))

(defn average
  "Return the average of two or more numbers."
  [n m & more]
  (/ (apply + n m more) (+ 2.0 (count more))))

(defn into!
  "The same as `into` but for transient vectors."
  [coll xs]
  (loop [coll coll xs xs]
    (if-let [x (first xs)]
      (recur (conj! coll x) (next xs))
      coll)))

(defn prefix
  "Attach a CSS style prefix to s."
  [p s]
  (let [p (to-str p)]
    (if (= \- (last p))
      (str p s)
      (str p \- s))))

(defn vendor-prefix
  "Attach a CSS vendor prefix to s."
  [p s]
  (let [p (to-str p)]
    (if (= \- (first p))
      (prefix p s) 
      (prefix (str \- p) s))))
