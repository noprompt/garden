(ns garden.compiler
  (:require [clojure.string :as string]
            [clojure.math.combinatorics :refer [cartesian-product]]
            [garden.util :as u]
            [garden.types])
  (:import garden.types.CSSFunction
           garden.types.CSSUnit))

(defprotocol CSSRenderer
  (render-css [this]
    "Convert a Clojure data type in to a string of CSS."))

(defn- ^String indent
  "Return an indent string."
  ([]
   (indent (u/indent-level)))
  ([n]
   (reduce str (take n (repeat \space)))))

;;;; Declaration, rule, and stylesheet generation.

(defn- expand-declaration
  "Expands nested properties."
  [declaration]
  (reduce
    (fn [m [prop value]]
      (let [prop (u/to-str prop)
            prefix (fn [[k v]]
                     {(str prop \- (u/to-str k)) v})]
        (if (and (map? value)
                 (not (instance? clojure.lang.IRecord value)))
          (expand-declaration (into m (map prefix value)))
          (assoc m prop value))))
    {}
    declaration))

(defn- make-declaration
  "Make a CSS declaration."
  [[prop v]]
  (str (indent) (u/to-str prop) (u/colon)
       (if (sequential? v)
         (u/space-join v)
         (u/to-str v))))

(defn- make-rule
  "Make a CSS rule."
  [[selector & declarations]]
  (str (u/to-str selector)
       (u/left-brace)
       (string/join (u/semicolon) (map render-css declarations))
       (u/right-brace)))

(defn- make-stylesheet
  "Make a CSS stylesheet from a vector of rules."
  [rules]
  (->> (filter vector? rules)
       (map render-css)
       (string/join (u/rule-separator))))

;;;; Media query generation.

(def
  ^{:private true
    :doc "Map for associng output-style to characters used in rendering a CSS
          media query."}
  media-output-style
  {:expanded {:left-brace " {\n\n"
              :right-brace "}"}
   :compact {:left-brace " {\n"
             :right-brace "}"}
   :compressed {:left-brace "{"
                :right-brace "}"}})

(defn make-media-expression
  "Make a media query expession from one or more maps."
  ([expr]
   (let [query (for [[k v] expr]
                 (cond
                   (true? v) (u/to-str k)
                   (false? v) (str "not " (u/to-str k))
                   :else (if (and v (seq (u/to-str v)))
                           (str "(" (u/to-str k) (u/colon) (u/to-str v) ")")
                           (str "(" (u/to-str k) ")"))))]
     (string/join " and " query)))
  ([expr & more]
   (u/comma-join (map make-media-expression (cons expr more)))))

(defn- make-media-query
  "Make a CSS media query from one or more maps and a sequence of rules."
  [expr rules]
  (let [expr (if (sequential? expr)
               (apply make-media-expression expr)
               (make-media-expression expr))
        mos (partial get-in media-output-style)
        ;; Media queries are a sepcial case and require a few minor adjustments
        ;; to their output.
        l-brace (mos [u/*output-style* :left-brace] (u/left-brace))
        r-brace (mos [u/*output-style* :right-brace] (u/right-brace))
        rules  (if (= u/*output-style* :compressed)
                 (make-stylesheet rules)
                 (let [ind (indent (+ 2 (u/indent-level)))]
                   (string/replace (make-stylesheet rules) #"(?m:^)" ind)))]
    (str "@media " expr l-brace rules (u/rule-separator) r-brace)))

(defn- render-declaration
  "Render a declaration map as a CSS declaration."
  [declaration]
  (->> (expand-declaration declaration)
       (map make-declaration)
       (string/join (u/semicolon))))

(defn- extract-attachment [selector]
  (when-let [attachment (re-find #"^&.+" (u/to-str (last selector)))]
    (apply str (rest attachment))))

(defn- expand-selector [selector context]
  "Expands a selector within the context and returns a new selector."
  (let [new-context (if (seq context)
                      (map flatten (cartesian-product context selector))
                      (map vector selector))]
    (map (fn [sel]
           (if-let [attachment (extract-attachment sel)]
             (let [parent (butlast sel)]
               (concat (butlast parent)
                       (list (u/as-str (last parent) attachment))))
             sel))
         new-context)))

(defn- divide-rule [rule]
  "Divide a rule in to triple of selector, declarations, and subrules."
  (let [[selector children] (split-with (complement coll?) rule)]
    (loop [children children
           new-rule [selector [] []]]
      (if-let [child (first children)]
        (cond
         (map? child)
         (recur (next children) (update-in new-rule [1] conj child))
         (vector? child)
         (recur (next children) (update-in new-rule [2] conj child))
         (list? child)
         (recur (apply concat child (rest children)) new-rule)
         :else
         (recur (next children) new-rule))
        new-rule))))

(defn- render-rule
  "Render a rule vector as a CSS rule."
  ([rule]
     (render-rule rule []))
  ([rule context]
     (let [[selector declarations subrules] (divide-rule rule)
           new-context (expand-selector selector context)
           rendered-selector (u/comma-join new-context)
           rendered-rule (when (seq declarations)
                           (make-rule `[~rendered-selector ~@declarations]))]
       (if (seq subrules)
         (->> (map #(render-rule %1 new-context) subrules)
              (cons rendered-rule)
              (string/join (u/rule-separator)))
         rendered-rule))))

(extend-protocol CSSRenderer
  clojure.lang.IPersistentVector
  (render-css [this]
    (render-rule this))
  clojure.lang.IPersistentMap
  (render-css [this]
    (render-declaration this))
  clojure.lang.ISeq
  (render-css [this]
    (string/join (u/newline) (map render-css this)))
  clojure.lang.Ratio
  (render-css [this]
    (str (float this)))
  garden.types.CSSUnit
  (render-css [this]
    (str this))
  garden.types.CSSFunction
  (render-css [this]
    (str this))
  Object
  (render-css [this]
    (str this))
  nil
  (render-css [this]
    ""))

(defn compile-css
  "Convert any number of Clojure data structures to CSS."
  [& rules]
  (render-css rules))

