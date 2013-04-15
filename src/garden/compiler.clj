(ns garden.compiler
  (:refer-clojure :exclude [newline])
  (:require [clojure.string :as string]
            [clojure.math.combinatorics :refer [cartesian-product]]
            [garden.util :refer :all]
            [garden.types])
  (:import garden.types.CSSFunction
           garden.types.CSSUnit))

(defprotocol CSSRenderer
  (render-css [this]
    "Convert a Clojure data type in to a string of CSS."))

(defn- ^String indent
  "Return an indent string."
  ([]
   (indent (indent-level)))
  ([n]
   (reduce str (take n (repeat \space)))))

;;;; Declaration, rule, and stylesheet generation.

(defn- expand-declaration
  "Expands nested properties."
  [declaration]
  (reduce
    (fn [m [prop value]]
      (let [prop (to-str prop)
            prefix (fn [[k v]]
                     {(str prop \- (to-str k)) v})]
        (if (and (map? value)
                 (not (instance? clojure.lang.IRecord value))) 
          (expand-declaration (into m (map prefix value)))
          (assoc m prop value))))
    {}
    declaration))

(defn- make-declaration
  "Make a CSS declaration."
  [[prop v]]
  (str (indent) (to-str prop) (colon)
       (if (sequential? v)
         (space-join v)
         (to-str v))))

(defn- make-rule
  "Make a CSS rule."
  [[selector & declarations]]
  (str (to-str selector)
       (left-brace)
       (string/join (semicolon) (map render-css declarations))
       (right-brace)))

(defn- make-stylesheet
  "Make a CSS stylesheet from a vector of rules."
  [rules]
  (->> (filter vector? rules)
       (map render-css)
       (string/join (rule-separator))))

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
                   (true? v) (to-str k)
                   (false? v) (str "not " (to-str k))
                   :else (if (and v (seq (to-str v)))
                           (str "(" (to-str k) (colon) (to-str v) ")")
                           (str "(" (to-str k) ")"))))]
     (string/join " and " query)))
  ([expr & more]
   (comma-join (map make-media-expression (cons expr more)))))

(defn- make-media-query
  "Make a CSS media query from one or more maps and a sequence of rules."
  [expr rules]
  (let [expr (if (sequential? expr)
               (apply make-media-expression expr)
               (make-media-expression expr))
        mos (partial get-in media-output-style)
        ;; Media queries are a sepcial case and require a few minor adjustments
        ;; to their output.
        l-brace (mos [*output-style* :left-brace] (left-brace))
        r-brace (mos [*output-style* :right-brace] (right-brace))
        rules  (if (= *output-style* :compressed)
                 (make-stylesheet rules)
                 (let [ind (indent (+ 2 (indent-level)))]
                   (string/replace (make-stylesheet rules) #"(?m:^)" ind)))]
    (str "@media " expr l-brace rules (rule-separator) r-brace)))

(defn- render-declaration
  "Render a declaration map as a CSS declaration."
  [declaration]
  (->> (expand-declaration declaration)
       (map make-declaration)
       (string/join (semicolon))))

(defn- extract-attachment [selector]
  (when-let [attachment (re-find #"^&.+" (to-str (last selector)))]
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
                       (list (as-str (last parent) attachment))))
             sel))
         new-context)))

(defn- render-rule
  "Render a rule vector as a CSS rule."
  ([rule]
   (render-rule rule []))
  ([rule context]
   (let [selector (take-while (complement coll?) rule)
         new-context (expand-selector selector context)
         selector (comma-join new-context)
         declarations (filter map? rule)
         subselectors (filter vector? rule)
         rendered-rule (when (seq declarations)
                         (make-rule `[~selector ~@declarations]))]
      (if (seq subselectors)
        (->> (map #(render-rule %1 new-context) subselectors)
             (cons rendered-rule)
             (string/join (rule-separator)))
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
    (string/join (newline) (map render-css this)))
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
