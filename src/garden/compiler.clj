(ns garden.compiler
  (:refer-clojure :exclude [newline])
  (:require [clojure.string :as string]
            [clojure.math.combinatorics :refer [cartesian-product]]
            [garden.util :refer :all]
            [garden.units :refer [unit?]]))

(declare render-css)

;;;; Output style and formatting

(def output-style
  {:expanded {:comma ", "
              :colon ": "
              :semicolon ";\n"
              :left-brace " {\n"
              :right-brace ";\n}"
              :rule-separator "\n\n"
              :newline "\n"
              :indent 2}
   :compact {:comma ", "
             :colon ": "
             :semicolon "; "
             :left-brace " { "
             :right-brace "; }"
             :rule-separator "\n"
             :newline "\n"
             :indent 0}
   :compressed {:comma ","
                :colon ":"
                :semicolon ";"
                :left-brace "{"
                :right-brace "}"
                :rule-separator ""
                :newline ""
                :indent 0}})

(def ^:dynamic *output-style* :compressed)

(defn- output [k]
  (fn [] (-> output-style *output-style* k)))

(def ^:private comma (output :comma))
(def ^:private colon (output :colon))
(def ^:private semicolon (output :semicolon))
(def ^:private left-brace (output :left-brace))
(def ^:private right-brace (output :right-brace))
(def ^:private rule-separator (output :rule-separator))
(def ^:private newline (output :newline))
(def ^:private indent-level (output :indent))

(defn- ^String indent
  "Return an indent string."
  ([]
   (indent (indent-level)))
  ([n]
   (reduce str (take n (repeat \space)))))

(defmacro with-output-style
  "Sets the output style for rendering CSS strings. Defaults to compressed."
  [style & body]
  (let [style (if (contains? output-style (keyword style))
                (keyword style)
                :compressed)]
    `(binding [*output-style* ~style]
       ~@body)))

(declare comma-join space-join)

(defn- comma-join
  "Return a comma separated list of values. Subsequences are joined with
   spaces."
  [xs]
  (let [ys (map #(if (sequential? %) (space-join %) (to-str %)) xs)]
    (string/join (comma) ys)))

(defn- space-join
  "Return a space separated list of values. Subsequences are joined with
   commas."
  [xs]
  (let [ys (map #(if (sequential? %) (comma-join %) (to-str %)) xs)]
    (string/join \space ys)))

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
                 (not (unit? value))) ; Don't expand units.
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

(defn make-rule
  "Make a CSS rule."
  [[selector & declarations]]
  (str (to-str selector)
       (left-brace)
       (string/join (semicolon) (map render-css declarations))
       (right-brace)))

(defn make-stylesheet
  "Make a CSS stylesheet from a vector of rules."
  [rules]
  (->> (filter vector? rules)
       (map render-css)
       (string/join (rule-separator))))

;;;; Media query generation.

(def media-output-style
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

(defn make-media-query
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

(defprotocol CSSRenderer
  (render-css [this]
    "Convert a Clojure data type in to a string of CSS."))

(defn- render-declaration
  "Render a declaration map as a CSS declaration."
  [declaration]
  (->> (expand-declaration declaration)
       (map make-declaration)
       (string/join (semicolon))))

(defn- render-rule
  "Render a rule vector as a CSS rule."
  ([rule]
   (render-rule rule []))
  ([rule context]
   (let [selector (take-while (complement coll?) rule)
         context (if (seq context)
                   (map flatten (cartesian-product context selector))
                   (into context selector))
         selector (comma-join context)
         declarations (filter map? rule)
         subselectors (filter vector? rule)
         rendered-rule (when (seq declarations)
                         (make-rule `[~selector ~@declarations]))]
      (if (seq subselectors)
        (->> (map #(render-rule %1 context) subselectors)
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
  garden.units.Unit
  (render-css [{:keys [magnitude unit]}]
    (str (render-css magnitude) (to-str unit)))
  Object
  (render-css [this]
    (str this))
  nil
  (render-css [this]
    ""))

(defn compile-css [& rules]
  (render-css rules))
