(ns garden.parse
  "Parser for Garden syntax."
  (:require
   [clojure.spec :as spec]
   [garden.ast])
  #?@(:clj
      [(:require
        [garden.color]
        [garden.units])
       (:import
        (garden.color Hsl Hsla Rgb Rgba)
        (garden.units Unit))]
      :cljs
      [(:require
        [garden.color :refer [Hsl Hsla Rgb Rgba]])
       (:require
        [garden.units :refer [Unit]])]))

(defprotocol IParse
  (-parse [this]))

(defn iparse?
  "true if `x` satisfies `IParse`, false otherwise."
  [x]
  (satisfies? IParse x))

(defn parse
  "Parse a value into a CSS AST."
  [x]
  {:post [(garden.ast/ast? %)]}
  (-parse x))

(defn tag
  "Used to retrieve the tag from tagged union values produced by
  `clojure.spec/conform` (e.g. the first element of the vector)."
  {:private true}
  [[tag :as tagged-data]]
  {:pre [(vector? tagged-data)]}
  tag)

(defn potential-identifier?
  "Predicate for determining whether or not `x` *could* potentially be
  rendered as CSS identifier. This is potential because the value of
  either a string or the result of converting a keyword or symbol to a
  string, may turn out to be invalid for the purposes of rendering."
  {:private true}
  [x]
  (or (keyword? x)
      (symbol? x)))

(spec/def ::iparse iparse?)

(spec/def ::raw-value string?)

(spec/def ::declaration-property
  (spec/or ::declaration-property keyword?
           ::declaration-property string?
           ::declaration-property symbol?))

(spec/def ::space-separated-list
  (spec/coll-of
   (spec/and (fn [x]
               (or (record? x)
                   (not (or (coll? x)
                            (sequential? x)))))
             (spec/or ::raw-value ::raw-value
                      ::numeric-value number?
                      ::identifier potential-identifier?
                      ::iparse ::iparse))
   :kind vector?))

(spec/def ::comma-separated-list
  (spec/coll-of
   (spec/or ::raw-value ::raw-value
            ::numeric-value number?
            ::identifier potential-identifier?
            ::space-separated-list ::space-separated-list
            ::iparse ::iparse)
   :kind vector?))

(spec/def ::multi-value
  (spec/coll-of
   (spec/or ::identifier potential-identifier?
            ::numeric-value number?
            ::comma-separated-list ::comma-separated-list
            ::declaration-block ::declaration-block
            ::iparse ::iparse
            ::raw-value ::raw-value)
   :kind set?))

(spec/def ::declaration-value
  (spec/or ::declaration-value
           (spec/or ::identifier potential-identifier?
                    ::numeric-value number?
                    ::comma-separated-list ::comma-separated-list
                    ::multi-value ::multi-value
                    ::declaration-block ::declaration-block
                    ::iparse ::iparse
                    ::raw-value ::raw-value)))

(spec/def ::declaration
  (spec/or ::declaration
           (spec/tuple ::declaration-property
                       ::declaration-value)))

(spec/def ::declaration-block
  (spec/and (fn [x]
              (and (map? x)
                   (not (record? x))))
            (spec/* ::declaration)))

(spec/def ::simple-selector
  (fn [x]
    (or (keyword? x)
        (symbol? x))))

(spec/def ::compound-selector
  (spec/coll-of
   (spec/or ::simple-selector ::simple-selector)
   :kind vector?))

(spec/def ::complex-selector
  (spec/coll-of
   (spec/or ::simple-selector ::simple-selector
            ::compound-selector ::compound-selector)
   :kind set?))

(spec/def ::selector
  (spec/or ::simple-selector ::simple-selector
           ::compound-selector ::compound-selector
           ::complex-selector ::complex-selector))

(spec/def ::vector-rule-child
  (spec/or ::declaration-block ::declaration-block
           ::vector-rule ::vector-rule
           ::iparse ::iparse))

(spec/def ::vector-rule
  (spec/cat :selector ::selector
            :children (spec/* ::vector-rule-child))) 

;; ---------------------------------------------------------------------
;; Conformed data processing

(defmulti
  ^{:arglists '([[tag x]])
    :private true}
  process-tagged-parse-data
  #'tag
  :default ::unknown-tag)

(defmethod process-tagged-parse-data ::raw-value
  [[_ x]]
  [:css/raw x])

(defmethod process-tagged-parse-data ::identifier
  [[_ x]]
  (if-let [ns (namespace x)]
    [:css/identifier (str ns "-" (name x))]
    [:css/identifier (name x)]))

(defmethod process-tagged-parse-data ::numeric-value
  [[_ n]]
  [:css/number n])

(defmethod process-tagged-parse-data ::iparse
  [[_ x]]
  (-parse x))

;; ---------------------------------------------------------------------
;; Selector processing

(defmethod process-tagged-parse-data ::simple-selector
  [[_ selector]]
  (let [selector-name (name selector)
        selector-namespace (namespace selector)
        parent-reference? (= (first selector-name) \&)]
    (case [parent-reference? (some? selector-namespace)]
      [true true]
      [:css.selector/parent-reference
       (str selector-namespace "|" (subs selector-name 1))]

      [true false]
      [:css.selector/parent-reference (subs selector-name 1)]

      [false true]
      [:css.selector/simple
       (str selector-namespace "|" selector-name)]

      [false false]
      [:css.selector/simple selector-name])))

(defmethod process-tagged-parse-data ::complex-selector
  [[_ parse-data]]
  (into
   [:css.selector/complex]
   (map process-tagged-parse-data parse-data)))

(defmethod process-tagged-parse-data ::compound-selector
  [[_ parse-data]]
  (into
   [:css.selector/compound]
   (map (fn [[_ selector]]
          (process-tagged-parse-data [::simple-selector selector]))
        parse-data)))

;; ---------------------------------------------------------------------
;; Declaration processing

(defmethod process-tagged-parse-data ::declaration-block
  [[_ parse-data]]
  (into
   [:css.declaration/block]
   (map process-tagged-parse-data parse-data)))

(defmethod process-tagged-parse-data ::declaration
  [[_ parse-data]]
  (let [[property-data value-data] parse-data
        [_ value-node] value-data]
    (if (= ::multi-value
           (tag value-node))
      (let [[_ value-nodes] value-node]
        (into
         [:css/fragment]
         (for [value-node value-nodes]
           (process-tagged-parse-data
            [::declaration
             [property-data [::declaration-value value-node]]]))))
      (into
       [:css/declaration]
       (map process-tagged-parse-data parse-data)))))

(defmethod process-tagged-parse-data ::declaration-property
  [[_ property]]
  [:css.declaration/property
   (if (keyword? property)
     (if-let [ns (namespace property)]
       (str ns "-" (name property))
       (name property))
     property)])

(defmethod process-tagged-parse-data ::declaration-value
  [[_ parse-data]]
  [:css.declaration/value
   (process-tagged-parse-data parse-data)])

(defmethod process-tagged-parse-data ::comma-separated-list
  [[_ parse-data]]
  (into
   [:css/comma-separated-list]
   (map process-tagged-parse-data parse-data)))

(defmethod process-tagged-parse-data ::space-separated-list
  [[_ parse-data]]
  (into
   [:css/space-separated-list]
   (map process-tagged-parse-data parse-data)))


;; ---------------------------------------------------------------------
;; Rule processing

(defn organize-processed-vector-children
  "Organize proccessed child nodes of a parsed `::vector-rule` into a
  map of declaration and non-declaration nodes. `:css/fragment` nodes
  are recursively elminated and their children included in the
  organization."
  {:private true}
  [processed-children]
  (loop [queue processed-children
         state {:declarations []
                :non-declaration-nodes []}]
    (if (seq queue)
      (let [node (first queue)]
        (case (tag node)
          :css.declaration/block
          (let [queue* (rest queue)
                state* (update state :declarations into (rest node))]
            (recur queue* state*))

          :css/fragment
          (let [queue* (concat (rest node) (rest queue))]
            (recur queue* state))

          ;; else
          (let [queue* (rest queue)
                state* (update state :non-declaration-nodes conj node)]
            (recur queue* state*))))
      state)))

(defmethod process-tagged-parse-data ::vector-rule
  [[_ parse-data]]
  (let [{:keys [selector children]} parse-data
        selector-node (process-tagged-parse-data selector)
        processed-children (map process-tagged-parse-data children)
        organized-children (organize-processed-vector-children processed-children)
        {:keys [declarations non-declaration-nodes]} organized-children
        declaration-block-node (into [:css.declaration/block] declarations)]
    (into
     [:css/rule selector-node declaration-block-node]
     non-declaration-nodes)))

;; ---------------------------------------------------------------------
;; IParse implementation

(defn parse-comma-separated-list
  [v]
  (let [parse-data (spec/conform ::comma-separated-list v)]
    (process-tagged-parse-data [::comma-separated-list parse-data])))

(defn parse-hash-map
  [m]
  (let [parse-data (spec/conform ::declaration-block m)
        node (process-tagged-parse-data [::declaration-block parse-data])]
    (with-meta node (meta m))))

(defn parse-seq
  [s]
  (into [:css/fragment] (map parse s)))

(defn parse-vector
  [v]
  (let [parse-data (spec/conform ::vector-rule v)]
    (process-tagged-parse-data [::vector-rule parse-data])))

#?(:clj
   (extend-protocol IParse
     clojure.lang.ArraySeq
     (-parse [s]
       (parse-seq s))

     clojure.lang.LazySeq
     (-parse [s]
       (parse-seq s))

     clojure.lang.PersistentArrayMap
     (-parse [m]
       (parse-hash-map m))

     clojure.lang.PersistentHashMap
     (-parse [m]
       (parse-hash-map m))

     clojure.lang.PersistentList
     (-parse [s]
       (parse-seq s))

     clojure.lang.PersistentVector
     (-parse [v]
       (parse-vector v))

     clojure.lang.PersistentVector$ChunkedSeq
     (-parse [v]
       (parse-seq v))

     Number
     (-parse [l]
       [:css/number l]))

   :cljs
   (extend-protocol IParse
     number
     (-parse [n]
       [:css/number n])

     LazySeq
     (-parse [s]
       (parse-seq s))

     PersistentArrayMap
     (-parse [m]
       (parse-hash-map m))

     PersistentHashMap
     (-parse [m]
       (parse-hash-map m))

     PersistentList
     (-parse [s]
       (parse-seq s))

     PersistentVector
     (-parse [v]
       (parse-vector v))))

(extend-protocol IParse
  Hsl
  (-parse [c]
    [:css/function
     [:css/identifier "hsl"]
     [:css/comma-separated-list
      [:css/number (garden.color/hue c)]
      [:css/percentage (garden.color/saturation c)]
      [:css/percentage (garden.color/lightness c)]]])

  
  Hsla
  (-parse [c]
    [:css/function
     [:css/identifier "hsla"]
     [:css/comma-separated-list
      [:css/number (garden.color/hue c)]
      [:css/percentage (garden.color/saturation c)]
      [:css/percentage (garden.color/lightness c)]
      [:css/number (garden.color/alpha c)]]])

  Rgb
  (-parse [c]
    [:css/function
     [:css/identifier "rgb"]
     [:css/comma-separated-list
      [:css/number (garden.color/red c)]
      [:css/number (garden.color/green c)]
      [:css/number (garden.color/blue c)]]])

  Rgba
  (-parse [c]
    [:css/function
     [:css/identifier "rgba"]
     [:css/comma-separated-list
      [:css/number (garden.color/red c)]
      [:css/number (garden.color/green c)]
      [:css/number (garden.color/blue c)]
      [:css/number (garden.color/alpha c)]]])

  Unit
  (-parse [u]
    [:css/unit
     [:css/number (garden.units/magnitude u)]
     [:css/identifier (name (garden.units/measurement u))]]))
