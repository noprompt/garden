(ns garden.util
  "Utility functions used by Garden."
  (:require
   [clojure.string :as str]
   [garden.types :as t]
   #?@(:cljs
       [[goog.string]
        [goog.string.format]]))
  #?(:clj
     (:refer-clojure :exclude [format]))
  #?(:clj
     (:import garden.types.CSSAtRule)))

;; ---------------------------------------------------------------------
;; String utilities

#?(:cljs
   (defn format
     "Formats a string using goog.string.format."
     [fmt & args]
     (apply goog.string/format fmt args)))

;; To avoid the pain of #?cljs :refer.
#?(:clj
   (def format #'clojure.core/format))

(defprotocol ToString
  (^String to-str [this] "Convert a value into a string."))

(extend-protocol ToString
  #?(:clj clojure.lang.Keyword)
  #?(:cljs Keyword)
  (to-str [this] (name this))

  #?(:clj Object)
  #?(:cljs default)
  (to-str [this] (str this))

  nil (to-str [this] ""))

(defn ^String as-str
  "Convert a variable number of values into strings."
  [& args]
  (apply str (map to-str args)))

(defn string->int
  "Convert a string to an integer with optional base."
  [s & [radix]]
  (let [radix (or radix 10)]
    #?(:clj
       (Integer/parseInt ^String s ^Long radix))
    #?(:cljs
       (js/parseInt s radix))))

(defn int->string
  "Convert an integer to a string with optional base."
  [i & [radix]]
  (let [radix (or radix 10)]
    #?(:clj
       (Integer/toString ^Long i ^Long radix))
    #?(:cljs
       (.toString i radix))))

(defn space-join
  "Return a space separated list of values."
  [xs]
  (str/join " " (map to-str xs)))

(defn comma-join
  "Return a comma separated list of values. Subsequences are joined with
   spaces."
  [xs]
  (let [ys (for [x xs]
             (if (sequential? x)
               (space-join x)
               (to-str x)))]
    (str/join ", " ys)))

(defn wrap-quotes
  "Wrap a string with double quotes."
  [s]
  (str \" s \"))

;; ---------------------------------------------------------------------
;; Predicates

(defn hash-map?
  "True if `(map? x)` and `x` does not satisfy `clojure.lang.IRecord`."
  [x]
  (and (map? x) (not (record? x))))

(def
  ^{:doc "Alias to `vector?`."}
  rule? vector?)

(def
  ^{:doc "Alias to `hash-map?`."}
  declaration? hash-map?)

(defn at-rule?
  [x]
  (instance? #?(:clj CSSAtRule) #?(:cljs t/CSSAtRule) x))

(defn at-media?
  "True if `x` is a CSS `@media` rule."
  [x]
  (and (at-rule? x) (= (:identifier x) :media)))

(defn at-keyframes?
  "True if `x` is a CSS `@keyframes` rule."
  [x]
  (and (at-rule? x) (= (:identifier x) :keyframes)))

(defn at-import?
  "True if `x` is a CSS `@import` rule."
  [x]
  (and (at-rule? x) (= (:identifier x) :import)))

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

;; ---------------------------------------------------------------------
;; Math utilities

(defn natural?
  "True if n is a natural number."
  [n]
  (and (integer? n) (pos? n)))

(defn between?
  "True if n is a number between a and b."
  [n a b]
  (let [bottom (min a b)
        top (max a b)]
    (and (>= n bottom) (<= n top))))

(defn clip
  "Return a number such that n is no less than a and no more than b."
  [a b n]
  (let [[a b] (if (<= a b) [a b] [b a])] 
    (max a (min b n))))

(defn average
  "Return the average of two or more numbers."
  [n m & more]
  (/ (apply + n m more) (+ 2.0 (count more))))

;; Taken from clojure.math.combinatorics.
(defn cartesian-product
  "All the ways to take one item from each sequence."
  [& seqs]
  (let [v-original-seqs (vec seqs)
	step
	(fn step [v-seqs]
	  (let [increment
		(fn [v-seqs]
		  (loop [i (dec (count v-seqs)), v-seqs v-seqs]
		    (if (= i -1) nil
			(if-let [rst (next (v-seqs i))]
			  (assoc v-seqs i rst)
			  (recur (dec i) (assoc v-seqs i (v-original-seqs i)))))))]
	    (when v-seqs
              (cons (map first v-seqs)
                    (lazy-seq (step (increment v-seqs)))))))]
    (when (every? seq seqs)
      (lazy-seq (step v-original-seqs)))))
