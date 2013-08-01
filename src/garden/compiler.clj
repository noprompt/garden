(ns garden.compiler
  (:require [clojure.string :as s]
            [clojure.math.combinatorics :refer (cartesian-product)]
            [garden.util :as u :refer (to-str as-str)]
            garden.units
            garden.types)
  (:import garden.types.CSSFunction
           garden.types.CSSImport
           garden.units.CSSUnit))

(def ^:private punctuation
  {:expanded
   {:comma ", "
    :colon ": "
    :semicolon ";\n"
    :l-brace " {\n"
    :r-brace "}"
    :l-brace-1 " {\n\n"
    :r-brace-1 "\n\n}"
    :rule-sep "\n\n"
    :indent "  "}
   :compact
   {:comma ", "
    :colon ": "
    :semicolon "; "
    :l-brace " { "
    :r-brace "; }"
    :l-brace-1 " {\n"
    :r-brace-1 "\n}"
    :rule-sep "\n"
    :indent ""}
   :compressed
   {:comma ","
    :colon ":"
    :semicolon ";"
    :l-brace "{"
    :r-brace "}"
    :l-brace-1 "{"
    :r-brace-1 "}"
    :rule-sep ""
    :indent ""}})

(def ^{:private true
       :doc "Retun a function to call when rendering a media expression.
             The returned function accepts two arguments: the media
             expression being evaluated and the current media expression
             context. Both arguments are maps. This is used to provide
             semantics for nested media queries."}
  media-expression-behavior
  {:merge (fn [expr context] (merge context expr))
   :default (fn [expr _] expr)})

(def ^:dynamic ^:private *output-style* :compressed)

(def ^{:dynamic true
       :private true
       :doc "The current compiler flags."}
  *flags*
  {:output-style :expanded
   :media-expressions {:nesting-behavior :default}})

(def ^{:dynamic true
       :private true
       :doc "The current parent selector context."}
  *selector-context* nil)

(def ^{:dynamic true
       :private true
       :doc "The current media query context."}
  *media-context* nil)

(defmacro with-output-style [style & body]
  (let [style (if (punctuation style) style :compressed)]
    `(binding [*output-style* ~style] ~@body)))

(defmacro ^:private defpunctuation [name]
  (let [k (keyword name)]
    `(defn- ~name []
       (get-in punctuation [*output-style* ~k]))))

(defpunctuation comma)
(defpunctuation colon)
(defpunctuation semicolon)
(defpunctuation l-brace)
(defpunctuation r-brace)
(defpunctuation l-brace)
(defpunctuation r-brace-1)
(defpunctuation l-brace-1)
(defpunctuation rule-sep)
(defpunctuation new-line)
(defpunctuation indent)

;; Utilities

(defn- space-join
  "Return a space separated list of values."
  [xs]
  (s/join \space (map to-str xs)))

(defn- comma-join
  "Return a comma separated list of values. Subsequences are joined with
   spaces."
  [xs]
  (let [ys (for [x xs] (if (sequential? x) (space-join x) (to-str x)))]
    (s/join (comma) ys)))

(defn- rule-join [xs]
  (s/join (rule-sep) xs))

(defn- extract-media-query
  "Extract media query information from obj."
  [obj]
  (let [m (meta obj)]
    (when-let [q (and (:media m) (dissoc m :media))]
      (when (seq q) q))))

(defn- media-query? [x]
  (let [m (meta x)]
    (boolean (and (:media m) (seq (dissoc m :media))))))

;; Match the start of a line if the characters immediately after it
;; are spaces or used in a CSS id (#), class (.), or tag name.
(def ^:private indent-location #"(?m)(?=[ A-Za-z#.}-]+)^")

(defn- ^String indent-str [s]
  (if-not (= :compressed *output-style*)
    (s/replace s indent-location (indent))
    s))

;; Expansion

(defprotocol Expandable
  (expand [this]))

;; Declaration expansion

(defn- expand-declaration
  "Expands nested properties in declarations.

   Ex. (expand-declaration {:foo {:bar \"baz\"}})
   => {\"foo-bar\" \"baz\"}"
  [declaration]
  (reduce
    (fn [m [prop val]]
      (let [prefix (fn [[k v]] {(as-str prop "-" k) v})]
        (if (u/hash-map? val)
         (->> (map prefix val) (into m) expand-declaration)
         (assoc m (to-str prop) val))))
    {}
    declaration))

;; Rule expansion

(def ^:private parent-selector-re #"^&.+|^&$")

(defn ^String extract-reference
  ;; Extract the selector portion of a parent selector reference.
  [selector]
  (when-let [reference (->> (last selector)
                            to-str
                            (re-find parent-selector-re))]
    (apply str (rest reference))))

(defn- expand-selector-reference [selector]
  (if-let [reference (extract-reference selector)]
    (let [parent (butlast selector)]
      (-> (last parent)
          (as-str reference)
          (cons (butlast parent))))
    selector))

(defn- expand-selector
  ;; Expand a selector within the context of parent selector and
  ;; return a new selector.
  [selector parent]
  (let [new-selector
        (if (seq parent)
          (map flatten (cartesian-product parent selector))
          (map vector selector))]
    (map expand-selector-reference new-selector)))

(defn- expand-rule
  [rule]
  (let [[sel xs] (split-with (complement coll?) rule)
        sel (expand-selector sel *selector-context*)]
    (loop [xs xs, ds (transient []), children (transient [])]
      (if-let [x (first xs)]
        (cond
         (map? x)
         (recur (next xs) (conj! ds (expand x)) children)
         (or (vector? x) (media-query? x))
         (recur (next xs) ds (conj! children x))
         (sequential? x)
         (recur (concat x (next xs)) ds children)
         :else
         (recur (next xs) ds children))
        [[sel (persistent! ds)] (persistent! children)]))))

;; Stylesheet expansion

;; Convert a Garden stylesheet into a level vector of rules and media
;; queries. Doing this makes compiling the stylesheet significantly
;; easier since this process is divided into two steps: expansion and
;; compilation. Also, there is the added benefit of maintaining
;; stylesheet order. That is, the stylesheet elements will be compiled
;; in roughly the same order they appear in.
(defn- expand-stylesheet-1
  [xs state]
  (loop [xs xs state state]
    (if-let [x (first xs)]
      (cond
       (media-query? x)
       (let [q (extract-media-query x)
             ys (expand (list (u/without-meta x)))
             {rs false ms true} (group-by (comp map? first) ys)
             state (conj! state [q rs])
             state (u/into! state ms)]
         (recur (next xs) state))
       (vector? x)
       (let [[rule children] (expand x)
             state (conj! state rule)
             state (binding [*selector-context* (first rule)]
                     (expand-stylesheet-1 children state))]
         (recur (next xs) state))
       (sequential? x)
       (recur (concat x (next xs)) state)
       :else
       (recur (next xs) state))
      state)))

(defn- expand-stylesheet [xs]
  (persistent! (expand-stylesheet-1 xs (transient []))))

;; Expandable implementation

(extend-protocol Expandable
  clojure.lang.IPersistentVector
  (expand [this] (expand-rule this))

  clojure.lang.IPersistentMap
  (expand [this] (expand-declaration this))

  clojure.lang.ISeq
  (expand [this] (expand-stylesheet this)))

;;;; Rendering

;; Declaration rendering

(defprotocol CSSRenderer
  (render-css [this]
    "Convert a Clojure data type in to a string of CSS."))

(defn- render-property-and-value
  [[prop val]]
  (if (set? val)
    (->> (interleave (repeat prop) val)
         (partition 2)
         (map render-property-and-value)
         s/join)
    (let [val (if (sequential? val) (comma-join val) val)]
      (as-str prop (colon) val (semicolon)))))

(defn- ^String render-declaration
  [declaration]
  (s/join (map render-property-and-value declaration)))

;; Rule rendering

(defn- render-selector [selector]
  (comma-join selector))

(defn- ^String render-rule
  ;; Convert a vector to a CSS rule string.
  [[selector declarations :as rule]]
  (when (every? seq rule)
    (str (render-selector selector)
         (l-brace)
         (->> (map render-css declarations)
              (s/join)
              (indent-str))
         (r-brace))))

;; Media query rendering

(defn- ^String render-media-expr-part [[k v]]
  (let [[sk sv] (map to-str [k v])]
    (cond
     (true? v) sk
     (false? v) (str "not " sk)
     (= "only" sv) (str "only " sk)
     :else (if (and v (seq sv))
             (str "(" sk (colon) sv ")")
             (str "(" sk ")")))))

(defn- ^String render-media-expr
  ;; Make a media query expession from one or more maps. Keys are not
  ;; validated but values have the following semantics:
  ;;
  ;; `true`  as in `{:screen true}`  == "screen"
  ;; `false` as in `{:screen false}` == "not screen"
  ;; `:only` as in `{:screen :only}  == "only screen"
  [expr]
  (if (sequential? expr)
    (->> expr
         (map render-media-expr)
         comma-join)
    (->> expr
         (map render-media-expr-part)
         (s/join " and "))))

(defn render-media-query [expr rules]
  (when (seq rules)
    (str "@media "
         (render-media-expr expr)
         (l-brace-1)
         (indent-str rules)
         (r-brace-1))))

;; Garden type rendering

(defn- ^String render-import [css-import]
  (let [{:keys [url media-expr]} css-import
        url (if (string? url)
              (u/wrap-quotes url)
              (render-css url))
        exprs (when media-expr
                (render-media-expr media-expr))]
    (str "@import "
         (if exprs (str url " " exprs) url)
         (semicolon))))

(defn- ^String render-function [css-function]
  (let [{:keys [function args]} css-function
        args (if (sequential? args)
               (comma-join args)
               (to-str args))]
    (format "%s(%s)" (to-str function) args)))

;; CSSRenderer implementation

(extend-protocol CSSRenderer
  clojure.lang.IPersistentVector
  (render-css [this] (render-rule this))

  clojure.lang.IPersistentMap
  (render-css [this] (render-declaration this))

  clojure.lang.Ratio
  (render-css [this] (str (float this)))

  CSSUnit
  (render-css [this] (str this))

  CSSFunction
  (render-css [this] (render-function this))

  CSSImport
  (render-css [this] (render-import this))

  Object
  (render-css [this] (str this))

  nil
  (render-css [this] ""))

(defn ^String compile-css
  "Convert any number of Clojure data structures to CSS."
  [& rules]
  (loop [xs (expand rules) rendered []]
    (if-let [x (first xs)]
      (if (map? (first x))
        (let [[expr children] x
              mq (->> (map render-css children)
                      rule-join
                      (render-media-query expr))]
          (recur (next xs) (conj rendered mq)))
        (recur (next xs) (conj rendered (render-css x))))
      (rule-join (remove nil? rendered)))))
