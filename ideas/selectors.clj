(ns selectors
  (:refer-clojure :exclude [compile]))

;; ---------------------------------------------------------------------
;; Utilities

;; String

(def string-literal-re
  #"\"(?:\\|[^\"])*\"|'(?:\\|[^'])*'")

(defn ^Boolean string-literal? [^String s]
  (boolean (re-matches string-literal-re s)))

(defn ^Boolean string-contains? [^String s1 ^String s2]
  (.contains s1 s2))

(defn ^String wrap-quotes [^String s]
  (str \" s \"))

;; Number

(defn whole? [n]
  (zero? (mod n 1)))

;; ---------------------------------------------------------------------
;; Protocols

(defprotocol IParse
  (parse [_]))

;; ---------------------------------------------------------------------
;; Data

(defn unit [type magnitude]
  {:op :unit
   :children [:magnitude :type]
   :magnitude magnitude
   :type type})

(defn number [n]
  {:pre [(number? n)]}
  {:op :number
   :form n})

(defn string [s]
  {:pre [(string? s)]}
  {:op :string
   :form s})

(defn identifier [i]
  {:op :identifier
   :form i})

(defn separated-list* [op coll]
  (reduce
   (fn [data [index x]]
     (-> data
         (update-in [:children] conj index)
         (assoc index x)))
   {:op op
    :children []}
   (map-indexed vector coll)))

(defn space-separated-list* [coll]
  (separated-list* :space-separated-list coll))

(defn space-separated-list [& more]
  (space-separated-list* more))

(defn comma-separated-list* [coll]
  (separated-list* :comma-separated-list coll))

(defn comma-separated-list [& more]
  (comma-separated-list* more))

(defn property [form]
  {:op :property
   :form form})

(defn declaration [property value]
  {:op :declaration
   :children [:property :value]
   :property property
   :value value})

(defn parse-seq-as-space-separated-list [v]
  (reify
    IParse
    (parse [_]
      (space-separated-list*
       (for [x v]
         (do
           (assert (not (coll? x)))
           (parse x)))))))

(defn parse-seq-as-comma-separated-list [v]
  (reify
    IParse
    (parse [_]
      (comma-separated-list*
       (for [x v]
         (parse (if (sequential? x)
                  (parse-seq-as-space-separated-list x)
                  x)))))))

;; ---------------------------------------------------------------------
;; Compile

(defmulti compile :op)

(defmethod compile :unit
  [{:keys [magnitude type]}]
  (cond
   (zero? magnitude)
   "0"
   (whole? magnitude)
   (str (int magnitude) (name type))
   :else
   (str (float magnitude) (name type))))

;; Primitives

(defmethod compile :number
  [{:keys [form]}]
  (str (if (whole? form)
         (int form)
         (float form))))

(defmethod compile :string
  [{:keys [^String form]}]
  form)

(defmethod compile :identifier
  [{:keys [form]}]
  (name form))

(defmethod compile :default
  [{:keys [op]}]
  (throw (ex-info (str "No method defined for op " op)
                  {:op op})))

;; ---------------------------------------------------------------------
;; Implementation

(extend-type java.lang.Number
  IParse
  (parse [this]
    (number this)))

(extend-type clojure.lang.Ratio
  IParse
  (parse [this]
    (number this)))

(extend-type java.lang.String
  IParse
  (parse [this]
    (string
     ;; Not good enough. Parse should fail on "\"" and "'".
     (if (string-literal? this)
       this
       (wrap-quotes this)))))

;; ---------------------------------------------------------------------
;; Scratch

;; Record?
(deftype CSSUnit [^String type ^Number magnitude]
  IParse
  (parse [_]
    (unit type magnitude)))


(parse
 (let [vs [(CSSUnit. :px 1)]]
   (parse-seq-as-comma-separated-list vs)))

;; ---------------------------------------------------------------------
;; Test
(require '[clojure.test :as t])
(require '[clojure.test.check :as tc])
(require '[clojure.test.check.clojure-test :refer [defspec]])
(require '[clojure.test.check.generators :as gen])
(require '[clojure.test.check.properties :as prop])

(def gen-float
  (gen/fmap float gen/ratio))

(def gen-whole
  (gen/such-that whole? (gen/fmap float gen/ratio) 1000))

(defspec prop-whole-numbers-compile-to-integers
  100
  (prop/for-all [v gen-whole]
    (let [compiled (compile (number v))]
      (re-matches #"-?\d+" compiled))))

(defspec prop-ratios-compile-to-floats
  100
  (prop/for-all [v (gen/such-that (complement whole?) gen/ratio)]
    (let [compiled (compile (parse v))]
      (re-matches #"-?\d+\.\d+" compiled))))

(defsepc prop-strings-compile-to-string-literals
  100
  (prop/for-all [v gen/string]
    (let [compiled (compile (parse v))]
      (or (= compiled v)
          (= compiled (wrap-quotes v))))))

;; Units

(def unit-types
  (gen/elements [:em :% :px :pt :cm :mm :in]))

(defn gen-unit [num-gen]
  (gen/fmap
   (fn [[unit magnitude]]
     (CSSUnit. unit magnitude))
   (gen/tuple unit-types num-gen)))

(defspec prop-units-with-zero-magnitude-compile-to-zero
  100
  (prop/for-all [v (gen-unit (gen/return 0))]
    (let [c (compile (parse v))]
      (= c "0"))))

(defspec prop-units-with-whole-number-magnitude-compile-to-whole-number-and-unit
  100
  (prop/for-all [v (gen-unit (gen/such-that (complement zero?) gen-whole))]
    (let [c (compile (parse v))]
      (re-matches #"-?\d+(?:[a-z]+|%)" c))))

(t/run-tests)

(compile (unit :px 1.0))

;; string
(assert (string-literal? "\"foo\""))
(assert (string-literal? "'foo'"))
