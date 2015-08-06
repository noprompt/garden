(ns garden.compiler
  "Functions for compiling Clojure data structures to CSS."
  (:require
   [clojure.string :as string]
   #?(:clj  [garden.color :as color]
      :cljs [garden.color :as color :refer [CSSColor]])
   [garden.compression :as compression]
   [garden.selectors :as selectors]
   [garden.units :as units]
   [garden.util :as util]
   #?(:cljs
      [garden.types :refer [CSSUnit CSSFunction CSSAtRule]]))
  #?(:cljs
     (:require-macros
      [garden.compiler :refer [with-media-query-context with-selector-context]]))
  #?(:clj
     (:import (garden.types CSSUnit CSSFunction CSSAtRule)
              (garden.color CSSColor))))

;; ---------------------------------------------------------------------
;; Compiler flags

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
   ;; A sequence of files to prepend to the output file.
   :preamble []
   ;; Location to save a stylesheet after compiling.
   :output-to nil
   ;; A list of vendor prefixes to prepend to things like
   ;; `@keyframes`, properties within declarations containing the
   ;; `^:prefix` meta data, and properties defined in `:auto-prefix`.
   :vendors []
   ;; A set of properties to automatically prefix with `:vendors`.
   :auto-prefix #{}
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

;; ---------------------------------------------------------------------
;; Utilities

(defmacro with-selector-context
  [selector-context & body]
  `(binding [*selector-context* ~selector-context]
     (do ~@body)))

(defmacro with-media-query-context
  [selector-context & body]
  `(binding [*media-query-context* ~selector-context]
     (do ~@body)))

(defn- vendors
  "Return the current list of browser vendors specified in `*flags*`."
  []
  (seq (:vendors *flags*)))

(defn- auto-prefixed-properties
  "Return the current list of auto-prefixed properties specified in `*flags*`."
  []
  (set (map name (:auto-prefix *flags*))))

(defn- auto-prefix?
  [property]
  (contains? (auto-prefixed-properties) property))

(defn- top-level-expression? [x]
  (or (util/rule? x)
      (util/at-import? x)
      (util/at-media? x)
      (util/at-keyframes? x)))

(defn- divide-vec
  "Return a vector of [(filter pred coll) (remove pred coll)]."
  [pred coll]
  ((juxt filter remove) pred coll))

#?(:clj
   (defn- save-stylesheet
     "Save a stylesheet to disk."
     [path stylesheet]
     (spit path stylesheet)))

;; =====================================================================
;; Expansion

;; The expansion process ensures that before a stylesheet is rendered
;; it is in a format that can be easily digested. That is, it produces
;; a new data structure which is a list of only one level.

;; This intermediate process between input and compilation separates
;; concerns between parsing data structures and compiling them to CSS.

;; All data types that implement `IExpandable` should produce a list.

(defprotocol IExpandable
  (expand [this]
    "Return a list containing the expanded form of `this`."))

;; ---------------------------------------------------------------------
;; List expansion

(defn- expand-seqs
  "Like flatten but only affects seqs."
  [coll]
  (mapcat
   (fn [x]
     (if (seq? x)
       (expand-seqs x)
       (list x)))
   coll))

;; ---------------------------------------------------------------------
;; Declaration expansion

(defn expand-declaration-1
  [d]
  (let [prefix #(util/as-str %1 "-" %2)]
    (reduce
     (fn [m [k v]]
       (if (util/hash-map? v)
         (reduce
          (fn [m1 [k1 v1]]
            (assoc m1 (prefix k k1) v1))
          m
          (expand-declaration-1 v))
         (assoc m (util/to-str k) v)))
     {}
     d)))

(defn- expand-declaration
  [d]
  (when (seq d)
    (with-meta (expand-declaration-1 d) (meta d))))

;; ---------------------------------------------------------------------
;; Rule expansion

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
      (concat (butlast parent)
              (-> (last parent)
                  (util/as-str reference)
                  (list))))
    selector))

(defn- expand-selector [selector parent]
  (let [selector (map selectors/css-selector selector)
        selector (if (seq parent)
                   (->> (util/cartesian-product parent selector)
                        (map flatten))
                   (map list selector))]
    (map expand-selector-reference selector)))

(defn- expand-rule
  [rule]
  (let [[selector children] (split-with selectors/selector? rule)
        selector (expand-selector selector *selector-context*)
        children (expand children)
        [declarations xs] (divide-vec util/declaration? children)
        ys (with-selector-context
             (if (seq selector)
               selector
               *selector-context*)
             (doall (mapcat expand xs)))]
    (->> (mapcat expand declarations)
         (conj [selector])
         (conj ys))))

;; ---------------------------------------------------------------------
;; At-rule expansion

(defmulti ^:private expand-at-rule :identifier)

(defmethod expand-at-rule :default
  [at-rule]
  (list at-rule))

;; @keyframes expansion

(defmethod expand-at-rule :keyframes
  [{:keys [value]}]
  (let [{:keys [identifier frames]} value]
    (->> {:identifier (util/to-str identifier)
          :frames (mapcat expand frames)}
         (CSSAtRule. :keyframes)
         (list))))

;; @media expansion

(defn- expand-media-query-expression [expression]
  (if-let [f (->> [:media-expressions :nesting-behavior]
                  (get-in *flags*)
                  (media-expression-behavior))]
    (f expression *media-query-context*)
    expression))

(defmethod expand-at-rule :media
  [{:keys [value]}]
  (let [{:keys [media-queries rules]} value 
        media-queries (expand-media-query-expression media-queries)
        xs (with-media-query-context media-queries             (doall (mapcat expand (expand rules))))
        ;; Though media-queries may be nested, they may not be nested
        ;; at compile time. Here we make sure this is the case.  
        [subqueries rules] (divide-vec util/at-media? xs)]
    (cons
     (CSSAtRule. :media {:media-queries media-queries
                         :rules rules})
     subqueries)))

;; ---------------------------------------------------------------------
;; Stylesheet expansion

(defn- expand-stylesheet [xs]
  (->> (expand xs)
       (map expand)
       (apply concat)))

(extend-protocol IExpandable

  #?(:clj clojure.lang.ISeq
     :cljs IndexedSeq)
  (expand [this] (expand-seqs this))

  #?(:cljs LazySeq)
  #?(:cljs (expand [this] (expand-seqs this)))

  #?(:cljs RSeq)
  #?(:cljs(expand [this] (expand-seqs this)))

  #?(:cljs NodeSeq)
  #?(:cljs (expand [this] (expand-seqs this)))

  #?(:cljs ArrayNodeSeq)
  #?(:cljs (expand [this] (expand-seqs this)))

  #?(:cljs Cons)
  #?(:cljs (
            expand [this] (expand-seqs this)))

  #?(:cljs ChunkedCons)
  #?(:cljs (expand [this] (expand-seqs this)))

  #?(:cljs ChunkedSeq)
  (expand [this] (expand-seqs this))

  #?(:cljs PersistentArrayMapSeq)
  #?(:cljs (expand [this] (expand-seqs this)))

  #?(:cljs List)
  #?(:cljs (expand [this] (expand-seqs this)))

  #?(:clj  clojure.lang.IPersistentVector
     :cljs PersistentVector)
  (expand [this] (expand-rule this))

  #?(:cljs Subvec)
  #?(:cljs (expand [this] (expand-rule this)))

  #?(:cljs BlackNode)
  #?(:cljs (expand [this] (expand-rule this)))

  #?(:cljs RedNode)
  #?(:cljs (expand [this] (expand-rule this)))

  #?(:clj clojure.lang.IPersistentMap
     :cljs PersistentArrayMap)
  (expand [this] (list (expand-declaration this)))

  #?(:cljs PersistentHashMap)
  #?(:cljs (expand [this] (list (expand-declaration this))))

  #?(:cljs PersistentTreeMap)
  #?(:cljs (expand [this] (list (expand-declaration this))))

  #?(:clj Object
     :cljs default)
  (expand [this] (list this))

  CSSFunction
  (expand [this] (list this))

  CSSAtRule
  (expand [this] (expand-at-rule this))

  CSSColor
  (expand [this] (list this))

  nil
  (expand [this] nil))

;; ---------------------------------------------------------------------
;; Rendering

(defprotocol CSSRenderer
  (render-css [this]
    "Convert a Clojure data type in to a string of CSS."))

;; ---------------------------------------------------------------------
;; Punctuation

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
  indent-loc-re
  #?(:clj
     #"(?m)(?=[\sA-z#.}-]+)^")
  #?(:cljs
     (js/RegExp. "(?=[ A-Za-z#.}-]+)^" "gm")))

(defn- indent-str [s]
  #?(:clj
     (string/replace s indent-loc-re indent))
  #?(:cljs
     (.replace s indent-loc-re indent)))

;; ---------------------------------------------------------------------
;; Declaration rendering

(defn- render-value
  "Render the value portion of a declaration."
  [x]
  (if (util/at-keyframes? x)
    (util/to-str (get-in x [:value :identifier]))
    (render-css x)))

(defn- render-property-and-value
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

(defn- add-blocks
  "For each block in `declaration`, add sequence of blocks
   returned from calling `f` on the block."
  [f declaration]
  (mapcat #(cons % (f %)) declaration))

(defn- prefixed-blocks
  "Sequence of blocks with their properties prefixed by
   each vendor in `vendors`."
  [vendors [p v]]
  (for [vendor vendors]
    [(util/vendor-prefix vendor (name p)) v]))

(defn- prefix-all-properties
  "Add prefixes to all blocks in `declaration` using
   vendor prefixes in `vendors`."
  [vendors declaration]
  (add-blocks (partial prefixed-blocks vendors) declaration))

(defn- prefix-auto-properties
  "Add prefixes to all blocks in `declaration` when property
   is in the `:auto-prefix` set."
  [vendors declaration]
  (add-blocks
   (fn [block]
     (let [[p _] block]
       (when (auto-prefix? (name p))
         (prefixed-blocks vendors block))))
   declaration))

(defn- prefix-declaration
  "Prefix properties within a `declaration` if `{:prefix true}` is
   set in its meta, or if a property is in the `:auto-prefix` set."
  [declaration]
  (let [vendors (or (:vendors (meta declaration)) (vendors))
        prefix-fn (if (:prefix (meta declaration))
                    prefix-all-properties
                    prefix-auto-properties)]
    (prefix-fn vendors declaration)))

(defn- render-declaration
  [declaration]
  (->> (prefix-declaration declaration)
       (map render-property-and-value)
       (string/join "\n")))

;; ---------------------------------------------------------------------
;; Rule rendering

(defn- render-selector
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

;; ---------------------------------------------------------------------
;; Media query rendering

(defn- render-media-expr-part
  "Render the individual components of a media expression."
  [[k v]]
  (let [[sk sv] (map render-value [k v])]
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

;; ---------------------------------------------------------------------
;; Garden type rendering

(defn- render-unit
  "Render a CSSUnit."
  [css-unit]
  (let [{:keys [magnitude unit]} css-unit
        magnitude #?(:cljs magnitude)
        #?(:clj (if (ratio? magnitude)
                  (float magnitude)
                  magnitude))]
    (str magnitude (name unit))))

(defn- render-function
  "Render a CSS function."
  [css-function]
  (let [{:keys [function args]} css-function
        args (if (sequential? args)
               (comma-separated-list args)
               (util/to-str args))]
    (util/format "%s(%s)" (util/to-str function) args)))

(defn ^:private render-color [c]
  (if-let [a (:alpha c)]
    (let [{:keys [hue saturation lightness]} (color/as-hsl c)
          [s l] (map units/percent [saturation lightness])]
      (util/format "hsla(%s)" (comma-separated-list [hue s l a])))
    (color/as-hex c)))

;; ---------------------------------------------------------------------
;; At-rule rendering

(defmulti ^:private render-at-rule
  "Render a CSS at-rule"
  :identifier)

(defmethod render-at-rule :default [_] nil)

;; @import

(defmethod render-at-rule :import
  [{:keys [value]}]
  (let [{:keys [url media-queries]} value 
        url (if (string? url)
              (util/wrap-quotes url)
              (render-css url))
        queries (when media-queries
                  (render-media-expr media-queries))]
    (str "@import "
         (if queries (str url " " queries) url)
         semicolon)))

;; @keyframes

(defmethod render-at-rule :keyframes
  [{:keys [value]}]
  (let [{:keys [identifier frames]} value]
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

;; @media

(defmethod render-at-rule :media
  [{:keys [value]}]
  (let [{:keys [media-queries rules]} value]
    (when (seq rules)
      (str "@media "
           (render-media-expr media-queries)
           l-brace-1
           (-> (map render-css rules)
               (rule-join)
               (indent-str)) 
           r-brace-1))))


;; ---------------------------------------------------------------------
;; CSSRenderer implementation

(extend-protocol CSSRenderer
  #?(:clj clojure.lang.ISeq
     :cljs IndexedSeq)
  (render-css [this] (map render-css this))

  #?(:cljs LazySeq)
  #?(:cljs (render-css [this] (map render-css this)))

  #?(:cljs RSeq)
  #?(:cljs (render-css [this] (map render-css this)))

  #?(:cljs NodeSeq)
  #?(:cljs (render-css [this] (map render-css this)))

  #?(:cljs ArrayNodeSeq)
  #?(:cljs (render-css [this] (map render-css this)))

  #?(:cljs Cons)
  #?(:cljs (render-css [this] (map render-css this)))

  #?(:cljs ChunkedCons)
  #?(:cljs (render-css [this] (map render-css this)))

  #?(:cljs ChunkedSeq)
  #?(:cljs (render-css [this] (map render-css this)))

  #?(:cljs PersistentArrayMapSeq)
  #?(:cljs (render-css [this] (map render-css this)))

  #?(:cljs List)
  #?(:cljs (render-css [this] (map render-css this)))

  #?(:clj clojure.lang.IPersistentVector
     :cljs PersistentVector)
  (render-css [this] (render-rule this))

  #?(:cljs Subvec)
  #?(:cljs (render-css [this] (render-rule this)))

  #?(:cljs BlackNode)
  #?(:cljs (render-css [this] (render-rule this)))

  #?(:cljs RedNode)
  #?(:cljs (render-css [this] (render-rule this)))

  #?(:clj clojure.lang.IPersistentMap
     :cljs PersistentArrayMap)
  (render-css [this] (render-declaration this))

  #?(:cljs PersistentHashMap)
  #?(:cljs (render-css [this] (render-declaration this)))

  #?(:cljs PersistentTreeMap)
  #?(:cljs (render-css [this] (render-declaration this)))

  #?(:clj clojure.lang.Ratio)
  #?(:clj (render-css [this] (str (float this))))

  #?(:cljs number)
  #?(:cljs (render-css [this] (str this)))

  #?(:clj clojure.lang.Keyword
     :cljs Keyword)
  (render-css [this] (name this))

  CSSUnit
  (render-css [this] (render-unit this))

  CSSFunction
  (render-css [this] (render-function this))

  CSSAtRule
  (render-css [this] (render-at-rule this))

  #?(:clj CSSColor
     :cljs color/CSSColor)
  (render-css [this] (render-color this))

  #?(:clj Object
     :cljs default)
  (render-css [this] (str this))

  nil
  (render-css [this] ""))


;; ---------------------------------------------------------------------
;; Compilation

(defn compile-style
  "Convert a sequence of maps into CSS for use with the HTML style
   attribute."
  [ms]
  (->> (filter util/declaration? ms)
       (reduce merge)
       (expand)
       (render-css)
       (first)))

(defn- do-compile
  "Return a string of CSS."
  [flags rules]
  (binding [*flags* flags]
    (->> (expand-stylesheet rules)
         (filter top-level-expression?) 
         (map render-css)
         (remove nil?)
         (rule-join))))

(defn- do-preamble
  "Prefix stylesheet with files in preamble. Not available in
  ClojureScript."
  [{:keys [preamble]} stylesheet]
  #?(:clj
     (string/join "\n" (conj (mapv slurp preamble) stylesheet)))
  #?(:cljs
     stylesheet))

(defn- do-compression
  "Compress CSS if the pretty-print(?) flag is true."
  [{:keys [pretty-print? pretty-print]} stylesheet]
  ;; Also accept pretty-print like CLJS.
  (if (or pretty-print? pretty-print) 
    stylesheet
    (compression/compress-stylesheet stylesheet)))

(defn- do-output-to
  "Write contents of stylesheet to disk."
  [{:keys [output-to]} stylesheet]
  #?(:clj
     (when output-to
       (save-stylesheet output-to stylesheet)
       (println "Wrote:" output-to)))
  stylesheet)

(defn compile-css
  "Convert any number of Clojure data structures to CSS."
  [flags & rules]
  (let [[flags rules] (if (and (util/hash-map? flags)
                               (some (set (keys flags)) (keys *flags*)))
                        [(merge *flags* flags) rules]
                        [*flags* (cons flags rules)])]
    (->> (do-compile flags rules)
         (do-preamble flags)
         (do-compression flags)
         (do-output-to flags))))
