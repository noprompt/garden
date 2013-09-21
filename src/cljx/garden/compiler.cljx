(ns garden.compiler
  (:require [clojure.string :as string]
            [garden.util :as util :refer (#+cljs format to-str as-str)]
            [garden.types])
  #+cljs (:require-macros [garden.compiler :refer [with-media-query-context with-selector-context]])
  (:import #+cljs cljs.core.ISeq
           #+clj (java.io StringReader
                    StringWriter)
           #+clj (com.yahoo.platform.yui.compressor CssCompressor)
           garden.types.CSSFunction
           garden.types.CSSImport
           garden.types.CSSKeyframes
           garden.types.CSSUnit
           garden.types.CSSMediaQuery))

;;;; ## Compiler flags

(def
  ^{:dynamic true
    :private true
    :doc "The current compiler flags."}
  *flags*
  {;; When set to `true` the compiled stylesheet will be "pretty
   ;; printed." This would be equivalent to setting
   ;; `{:ouput-style => :expanded}` in Sass. When set to `false`
   ;; the compiled stylesheet will be compressed with the YUI
   ;; compressor.
   :pretty-print? true
   ;; A list of vendor prefixes to append automatically to things like
   ;; `@keyframes` and declarations containing the `^:prefix` meta data.
   :vendors []
   ;; `@media-query` specific configuration.
   :media-expressions {;; May either be `:merge` or `:default`. When
                       ;; set to `:merge` nested media queries will
                       ;; have their expressions merged with their
                       ;; parent's.
                       :nesting-behavior :default}})

(def
  ^{:private true
    :doc "Retun a function to call when rendering a media expression.
  The returned function accepts two arguments: the media
  expression being evaluated and the current media expression context.
  Both arguments are maps. This is used to provide semantics for nested
  media queries."}
  media-expression-behavior
  {:merge (fn [expr context] (merge context expr))
   :default (fn [expr _] expr)})

(def
  ^{:dynamic true
    :private true
    :doc "The current parent selector context."}
  *selector-context* nil)
 
(def
  ^{:dynamic true
    :private true
    :doc "The current media query context."}
  *media-query-context* nil)

;;;; ## Utilities

#+clj
(defmacro with-selector-context
  [selector-context & body]
  `(binding [*selector-context* ~selector-context]
     (do ~@body)))
 
 #+clj
(defmacro with-media-query-context
  [selector-context & body]
  `(binding [*media-query-context* ~selector-context]
     (do ~@body)))

(defn vendors
  "Return the current list of browser vendors specified in `*flags*`."
  []
  (seq  (:vendors *flags*)))

#+clj
(defn- save-stylesheet
  "Save a stylesheet to disk."
  [path stylesheet]
  (spit path stylesheet))

#+clj
(defn- compress-stylesheet
  "Compress a stylesheet with the YUI CSSCompressor."
  [stylesheet]
  (let [reader (StringReader. ^String stylesheet)
        writer (StringWriter.)]
    (doto (CssCompressor. reader)
      (.compress writer -1))
    (str writer)))

(defn- divide
  "Return a vector of [(filter pred coll) (remove pred coll)]."
  [pred coll]
  ((juxt filter remove) pred coll))

(defn- top-level-expression? [x]
  (or (util/rule? x)
      (util/import? x)
      (util/media-query? x)
      (util/keyframes? x)))
 
;; ## Expansion

;; The expansion process ensures that before a stylesheet is rendered
;; it is in a format that can be easily digested. That is, it produces
;; a new data structure which is a list of only one level.

;; This intermediate process between input and compilation separates
;; concerns between parsing data structure and compiling them to CSS.

;; All data types that implement `IExpandable` should produce a list.

(defprotocol IExpandable
  (expand [this]
    "Return a list containing the expaned form of `this`."))

;; ### List expansion

(defn expand-seqs
  "Like flatten but only affects seqs."
  [coll]
  (mapcat
   (fn [x]
     (if (seq? x)
       (expand-seqs x)
       (list x)))
   coll))
 
;; ### Declaration expansion

(defn expand-declaration
  [declaration]
  (when (seq declaration)
    (let [m (meta declaration)
          f (fn [m [prop val]]
              (letfn [(prefix  [[k v]]
                        (hash-map (util/as-str prop "-" k) v))]
                (if (util/hash-map? val)
                  (->> (map prefix val) (into m) expand-declaration)
                  (assoc m (util/to-str prop) val))))]
      (-> (reduce f {} declaration)
          (with-meta m)))))

;; ### Rule expansion

(def
  ^{:private true
    :doc "Matches a single \"&\" or \"&\" follow by one or more 
  non-whitespace characters."}
  parent-selector-re
  #"^&(?:\S+)?$")

(defn- extract-reference
  "Extract the selector portion of a parent selector reference."
  [selector]
  (when-let [reference (->> (last selector)
                            (util/to-str)
                            (re-find parent-selector-re))]
    (apply str (rest reference))))

(defn- expand-selector-reference
  [selector]
  (if-let [reference (extract-reference selector)]
    (let [parent (butlast selector)]
      (-> (last parent)
          (util/as-str reference)
          (cons (butlast parent))))
    selector))

(defn- expand-selector [selector parent]
  (let [selector (if (seq parent)
                   (->> (util/cartesian-product parent selector)
                        (map flatten))
                   (map list selector))]
    (map expand-selector-reference selector)))
 
(defn- expand-rule
  [rule]
  (let [[selector children] (split-with (complement coll?) rule)
        selector (expand-selector selector *selector-context*)
        children (expand children)
        [declarations xs] (divide util/declaration? children)
        ys (with-selector-context
             (if (seq selector)
               selector
               *selector-context*)
             (mapcat expand xs))]
    (->> (mapcat expand declarations)
         (conj [selector])
         (conj ys))))

;; ### Media query expansion

(defn- expand-media-query-expression [expression]
  (if-let [f (->> [:media-expressions :nesting-behavior]
                  (get-in *flags*)
                  (media-expression-behavior))]
    (f expression *media-query-context*)
    expression))

(defn- expand-media-query [media-query]
  (let [{:keys [expression children]} media-query
        expression (expand-media-query-expression expression)
        xs (with-media-query-context expression
             (mapcat expand (expand children)))
        ;; Though media-queries may be nested, they may not be nested
        ;; at compile time. Here we make sure this is the case.  
        [subqueries ys] (divide util/media-query? xs)]
    (cons (CSSMediaQuery. expression ys) subqueries)))

(defn- expand-keyframes [keyframes]
  (let [{:keys [identifier frames]} keyframes
        frames (mapcat expand frames)]
    (list (CSSKeyframes. (util/to-str identifier) frames))))
 
;; ### Stylesheet expansion
 
(defn- expand-stylesheet [xs]
  (->> (expand xs)
       (map expand)
       (apply concat)))
 
(extend-protocol IExpandable
  #+clj clojure.lang.ISeq
  #+cljs IndexedSeq
  (expand [this] (expand-seqs this))
 
  #+clj clojure.lang.IPersistentVector
  #+cljs PersistentVector
  (expand [this] (expand-rule this))
 
  #+clj clojure.lang.IPersistentMap
  #+cljs PersistentHashMap
  (expand [this] (list (expand-declaration this)))

  CSSImport
  (expand [this] (list this))

  CSSFunction
  (expand [this] (list this))

  CSSMediaQuery
  (expand [this] (expand-media-query this))

  CSSKeyframes
  (expand [this] (expand-keyframes this))
 
  #+clj Object
  #+cljs object
  (expand [this] (list this))
 
  nil
  (expand [this] nil))

;; ## Rendering

(defprotocol CSSRenderer
  (render-css [this]
    "Convert a Clojure data type in to a string of CSS."))

;; ### Punctuation

(def ^:private comma ", ")
(def ^:private colon ": ")
(def ^:private semicolon ";")
(def ^:private l-brace " {\n")
(def ^:private r-brace "\n}")
(def ^:private l-brace-1 " {\n\n")
(def ^:private r-brace-1 "\n\n}")
(def ^:private rule-sep "\n\n")
(def ^:private indent "  ")

(defn- space-separated-list
  "Return a space separated list of values."
  ([xs]
     (space-separated-list render-css xs))
  ([f xs]
     (string/join " " (map f xs))))

(defn- comma-separated-list
  "Return a comma separated list of values. Subsequences are joined with
   spaces."
  ([xs]
     (comma-separated-list render-css xs))
  ([f xs]
     (let [ys (for [x xs]
                (if (sequential? x)
                  (space-separated-list f x)
                  (f x)))]
       (string/join comma ys))))

(defn- rule-join [xs]
  (string/join rule-sep xs))

(def
  ^{:private true
    :doc "Match the start of a line if the characters immediately
  after it are spaces or used in a CSS id (#), class (.), or tag name."}
  indent-location
  #"(?m)(?=[ A-Za-z#.}-]+)^")

(defn- indent-str [s]
  (string/replace s indent-location indent))

;; ### Declaration rendering

(defn render-value
  "Render the value portion of a declaration."
  [x]
  (if (util/keyframes? x)
    (util/to-str (:identifier x))
    (render-css x)))

(defn render-property-and-value
  [[prop val]]
  (if (set? val)
    (->> (interleave (repeat prop) val)
         (partition 2)
         (map render-property-and-value)
         (string/join "\n"))
    (let [val (if (sequential? val)
                (comma-separated-list render-value val)
                (render-value val))]
      (util/as-str prop colon val semicolon))))

(defn prefix-declaration
  "If `(:vendors *flags*)` is bound and `declaration` has the meta 
  `{:prefix true}` automatically create vendor prefixed properties."
  [declaration]
  (if-not (and (vendors) (:prefix (meta declaration)))
    declaration
    (mapcat 
     (fn [prop val]
       (-> (mapv
            (fn [vendor p v]
              [(util/vendor-prefix vendor p) v])
            (vendors)
            (repeat prop)
            (repeat val))
           (conj [prop val])))
     (keys declaration)
     (vals declaration))))

(defn render-declaration
  [declaration]
  (->> (prefix-declaration declaration)
       (map render-property-and-value)
       (string/join "\n")))

;; ### Rule rendering

(defn render-selector
  [selector]
  (comma-separated-list selector))

(defn- render-rule
  "Convert a vector to a CSS rule string. The vector is expected to be
  fully expanded."
  [[selector declarations :as rule]]
  (when (and (seq rule) (every? seq rule))
    (str (render-selector selector)
         l-brace
         (->> (map render-css declarations)
              (string/join "\n")
              (indent-str))
         r-brace)))

;; ### Media query rendering

(defn- render-media-expr-part
  "Render the individual components of a media expression."
  [[k v]]
  (let [[sk sv] (map util/to-str [k v])]
    (cond
     (true? v) sk
     (false? v) (str "not " sk)
     (= "only" sv) (str "only " sk)
     :else (if (and v (seq sv))
             (str "(" sk colon sv ")")
             (str "(" sk ")")))))

(defn- render-media-expr
  "Make a media query expession from one or more maps. Keys are not
  validated but values have the following semantics:
  
    `true`  as in `{:screen true}`  == \"screen\"
    `false` as in `{:screen false}` == \"not screen\"
    `:only` as in `{:screen :only}  == \"only screen\""
  [expr]
  (if (sequential? expr)
    (->> (map render-media-expr expr)
         (comma-separated-list))
    (->> (map render-media-expr-part expr)
         (string/join " and "))))

;; ### Garden type rendering

(defn- render-media-query
  "Render a media query. media-query is expected to be fully expanded."
  [media-query]
  (let [{:keys [expression children]} media-query]
    (when (seq children)
      (str "@media "
           (render-media-expr expression)
           l-brace-1
           (-> (map render-css children)
               (rule-join)
               (indent-str)) 
           r-brace-1))))

(defn- render-unit
  "Render a CSSUnit."
  [css-unit]
  (let [{:keys [magnitude unit]} css-unit
        magnitude #+cljs magnitude
            #+clj (if (ratio? magnitude)
                    (float magnitude)
                    magnitude)]
    (str (if (zero? magnitude) 0 magnitude)
         (when-not (zero? magnitude) (name unit)))))

(defn- render-import
  "Render a CSS @import directive."
  [css-import]
  (let [{:keys [url media-expr]} css-import
        url (if (string? url)
              (util/wrap-quotes url)
              (render-css url))
        exprs (when media-expr
                (render-media-expr media-expr))]
    (str "@import "
         (if exprs (str url " " exprs) url)
         semicolon)))

(defn- render-function
  "Render a CSS function."
  [css-function]
  (let [{:keys [function args]} css-function
        args (if (sequential? args)
               (comma-separated-list args)
               (util/to-str args))]
    (format "%s(%s)" (util/to-str function) args)))

(defn- render-keyframes
  "Render a CSS @keyframes block."
  [css-keyframes]
  (let [{:keys [identifier frames]} css-keyframes]
    (when (seq frames)
      (let [body (str (util/to-str identifier)
                      l-brace-1
                      (->> (map render-css frames)
                           (rule-join)
                           (indent-str))
                      r-brace-1)
            prefix (fn [vendor]
                     (str "@" (util/vendor-prefix vendor "keyframes ")))]
        (->> (map prefix (vendors))
             (cons "@keyframes ")
             (map #(str % body))
             (rule-join))))))

;; ### CSSRenderer implementation

(extend-protocol CSSRenderer
  #+clj clojure.lang.ISeq
  #+cljs IndexedSeq
  (render-css [this] (map render-css this))
  
  #+clj clojure.lang.IPersistentVector
  #+cljs PersistentVector
  (render-css [this] (render-rule this))

  #+clj clojure.lang.IPersistentMap
  #+cljs PersistentHashMap
  (render-css [this] (render-declaration this))

  #+clj clojure.lang.Ratio
  #+clj (render-css [this] (str (float this)))

  #+cljs number
  (render-css [this] (str this))

  #+clj clojure.lang.Keyword
  #+cljs Keyword
  (render-css [this] (name this))

  CSSUnit
  (render-css [this] (render-unit this))

  CSSFunction
  (render-css [this] (render-function this))

  CSSImport
  (render-css [this] (render-import this))

  CSSKeyframes
  (render-css [this] (render-keyframes this))

  CSSMediaQuery
  (render-css [this] (render-media-query this))

  #+clj Object
  #+cljs object
  (render-css [this] (str this))

  nil
  (render-css [this] ""))

(defn compile-style
  "Convert a sequence of maps into css for use with the HTML style
   attribute."
  [ms]
  (->> (filter util/declaration? ms)
       (reduce merge)
       (expand)
       (render-css)))

(defn- compile-stylesheet
  [flags rules]
  (binding [*flags* (merge *flags* flags)]
    (->> (expand-stylesheet rules)
         ;; Declarations may not appear at the top level.
         (filter top-level-expression?) 
         (map render-css)
         (remove nil?)
         (rule-join))))

(defn compile-css
  "Convert any number of Clojure data structures to CSS."
  [flags & rules]
  (let [[flags rules] (if (and (util/hash-map? flags)
                               (some (set (keys flags)) (keys *flags*)))
                        [flags rules]
                        [*flags* (cons flags rules)])
        output-to (:output-to flags)
        stylesheet #+cljs (compile-stylesheet flags rules)
             #+clj (let [stylesheet (compile-stylesheet flags rules)]
                     (if (:pretty-print? flags)
                       stylesheet
                       (compress-stylesheet stylesheet)))]
    #+clj
    (if output-to
      (do
        (save-stylesheet output-to stylesheet)
        stylesheet)
      stylesheet)
    #+cljs stylesheet))
