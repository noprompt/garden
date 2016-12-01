(ns garden.compiler
  "Functions for compiling Clojure data structures to CSS."
  (:require
   [clojure.string :as string]
   [garden.ast]
   [garden.normalize]
   [garden.parse]
   [garden.selectors :as selectors]
   [garden.util :as util]))


;; =====================================================================
;; Compiler environments

(def default-env
  {:comma ","
   :colon ":"
   :curly-brace-left "{"
   :curly-brace-right "}"
   :minus "-"
   :nesting-level 0
   :obelus "/"
   ;; Location to save a stylesheet after compiling.
   :output-to nil
   :plus "+"
   ;; A sequence of files to prepend to the output file.
   :preamble []
   ;; A set of property names to automatically prefix with `:vendors`.
   :prefix-properties #{}
   ;; When set to `true` the compiled stylesheet will be "pretty
   ;; printed." This would be equivalent to setting
   ;; `{:ouput-style => :expanded}` in Sass. When set to `false`
   ;; the compiled stylesheet will be compressed.
   :pretty-print? false
   :round-brace-left "("
   :round-brace-right ")"
   :selector-context nil
   :semicolon ";"
   :separator ""
   :tab "  "
   :times "*"
   ;; A list of vendor prefixes to prepend to things like
   ;; `@keyframes`, properties, etc.
   :vendors []})

(def pretty-env
  (merge default-env
         {:colon ": "
          :comma ", "
          :curly-brace-left " {\n"
          :curly-brace-right "}\n"
          :minus " - "
          :nesting-level 0
          :obelus " / "
          :plus " + "
          :pretty-print? true
          :round-brace-left "("
          :round-brace-right ")"
          :selector-context nil
          :semicolon ";\n"
          :separator "\n"
          :tab "  "
          :times " * "}))


;; =====================================================================
;; Environment helpers

(defn indent
  "Return an indent string with respect to the environment `env`."
  [env]
  (if (:pretty-print? env)
    (let [{:keys [tab nesting-level]} env]
      (apply str (repeat nesting-level tab)))
    ""))

(defn inc-nesting-level
  "Incement the nesting level of `env` by one."
  [env]
  (update env :nesting-level inc))

(defn- vendors
  "Return the set of browser vendors specified in `env`."
  [env]
  (:vendors env))

(defn- prefixed-properties
  "Return the list of prefixed properties specified in `env`."
  [env]
  (:prefix-properties env))

(defn- prefix-property?
  [env property]
  (contains? (prefixed-properties env) property))


;; =====================================================================
;; Node compilation

(defn tag
  "Used to retrieve the tag from tagged union value (e.g. a vector
  such that the first element is a keyword)."
  {:private true}
  [[tag :as tagged-data]]
  {:pre [(vector? tagged-data)]}
  tag)

(defmulti
  ^{:arglists '([node env])}
  compile-node
  "Compile a CSS AST node."
  (fn [node env]
    (tag node))
  :default ::unknown-node)

(defn compile-nodes [nodes env]
  (map compile-node nodes (repeat env)))

(defmethod compile-node :css/comma-separated-list
  [[_ & nodes] env]
  (string/join (:comma env)
               (compile-nodes nodes env)))

(defmethod compile-node :css/declaration
  [[_ property-node value-node :as node] env]
  (let [compiled-property (compile-node property-node env)
        compiled-value (compile-node value-node env)]
    (if (prefix-property? env compiled-property)
      (string/join 
       (for [vendor (vendors env)]
         (str
          (indent env)
          "-" (name vendor) "-" compiled-property
          (:colon env)
          compiled-value
          (:semicolon env))))
      (str
       (indent env)
       compiled-property
       (:colon env)
       compiled-value
       (:semicolon env)))))

(defmethod compile-node :css/fragment
  [[_ & children] env]
  (string/join (:separator env) (compile-nodes children env)))

(defmethod compile-node :css.declaration/block
  [[_ & declaration-nodes] env]
  (let [env* (inc-nesting-level env)]
    (str
     (:curly-brace-left env)
     (string/join (compile-nodes declaration-nodes env*))
     (indent env)
     (:curly-brace-right env))))

(defmethod compile-node :css.declaration/property
  [[_ property] _]
  (name property))

(defmethod compile-node :css.declaration/value
  [[_ value-node] env]
  (compile-node value-node env))

(defmethod compile-node :css/function
  [[_ identifier arguments] env]
  (str (compile-node identifier env)
       (:round-brace-left env)
       (compile-node arguments env)
       (:round-brace-right env)))

(defmethod compile-node :css/identifier
  [[_ identifier] _]
  (name identifier))

(defmethod compile-node :css/import
  [[_ url media-query-list] env]
  (str
   (string/trim 
    (str "@import " (pr-str url) " " (compile-node media-query-list env)))
   ";"))

(defmethod compile-node :css/noop
  [_ _]
  "")

(defmethod compile-node :css/number
  [[_ number] _]
  (str number))

(defmethod compile-node :css/percentage
  [[_ number] _]
  (str number "%"))

(defmethod compile-node :css/raw
  [[_ x] _]
  x)

(defmethod compile-node :css/rule
  [[_ selector-node declaration-block] env]
  (str 
   (indent env)
   (compile-node selector-node env)
   (compile-node declaration-block env)))

(defmethod compile-node :css/selector
  [[_ selector-node] env]
  (compile-node selector-node env))

(defmethod compile-node :css.selector/simple
  [[_ identifier] env]
  (name identifier))

(defmethod compile-node :css.selector/compound
  [[_ & selector-nodes] env]
  (string/join " " (compile-nodes selector-nodes env)))

(defmethod compile-node :css.selector/complex
  [[_ & selector-nodes] env]
  (string/join (:comma env)
               (compile-nodes selector-nodes env)))

(defmethod compile-node :css/space-separated-list
  [[_ & nodes] env]
  (string/join " " (compile-nodes nodes env)))

(defmethod compile-node :css/stylesheet
  [[_ & nodes] env]
  (string/join (:separator env) (compile-nodes nodes env)))

(defmethod compile-node :css/unit
  [[_ magnitude-node unit-node] env]
  (str (compile-node magnitude-node env)
       (compile-node unit-node env)))

;; ---------------------------------------------------------------------
;; calc compilation

(defmethod compile-node :css/calc
  [[_ value-node] env]
  (str "calc"
       (:round-brace-left env)
       (compile-node value-node env)
       (:round-brace-right env)))

(defmethod compile-node :css.calc/difference
  [[_ value-node-1 value-node-2] env]
  (str (:round-brace-left env)
       (compile-node value-node-1 env)
       (:minus env)
       (compile-node value-node-2 env)
       (:round-brace-right env)))

(defmethod compile-node :css.calc/product
  [[_ value-node-1 value-node-2] env]
  (str (:round-brace-left env)
       (compile-node value-node-1 env)
       (:times env)
       (compile-node value-node-2 env)
       (:round-brace-right env)))

(defmethod compile-node :css.calc/quotient
  [[_ value-node-1 value-node-2] env]
  (str (:round-brace-left env)
       (compile-node value-node-1 env)
       (:obelus env)
       (compile-node value-node-2 env)
       (:round-brace-right env)))

(defmethod compile-node :css.calc/sum
  [[_ value-node-1 value-node-2] env]
  (str (:round-brace-left env)
       (compile-node value-node-1 env)
       (:plus env)
       (compile-node value-node-2 env)
       (:round-brace-right env)))

;; ---------------------------------------------------------------------
;; Media query compilation

(defmethod compile-node :css.media.query/conjunction
  [[_ & child-nodes] env]
  (string/join " and " (compile-nodes child-nodes env)))

(defmethod compile-node :css.media.query/constraint
  [[_ constraint] _]
  (str (name constraint) " "))

(defmethod compile-node :css.media.query/expression
  [[_ & [feature value]] env]
  (str 
   (:round-brace-left env)
   (if value
     (str (compile-node feature env)
          (:colon env)
          (compile-node value env))
     (compile-node feature env))
   (:round-brace-right env)))

(defmethod compile-node :css.media.query/feature
  [[_ feature] env]
  (name feature))

(defmethod compile-node :css.media.query/type
  [[_ type] _]
  (name type))

(defmethod compile-node :css.media.query/value
  [[_ value] env]
  (str value))

(defmethod compile-node :css.media/query
  [[_ constraint type conjunction] env]
  (let [query-head  (str (compile-node constraint env)
                         (compile-node type env))
        query-tail (compile-node conjunction env)]
    (case [(empty? query-head) (empty? query-tail)]
      [true true]
      ""

      [true false]
      query-tail

      [false true]
      query-head

      [false false]
      (str query-head " and " query-tail))))

(defmethod compile-node :css.media/query-list
  [[_ & query-nodes] env]
  (string/join (:comma env) (compile-nodes query-nodes env)))

(defmethod compile-node :css.media/rule
  [[_ query-list-node & child-nodes] env]
  (let [new-env (inc-nesting-level env)]
    (str "@media "
         (compile-node query-list-node env)
         (:curly-brace-left env)
         (string/join (:separator env)
                      (compile-nodes child-nodes new-env))
         (:curly-brace-right env))))

;; ---------------------------------------------------------------------
;; Keyframes compilation

(defmethod compile-node :css/keyframes
  [[_ name-node block-node] env]
  (string/join
   (:separator env)
   (cons
    (str "@keyframes "
         (compile-node name-node env)
         (compile-node block-node env))
    (for [vendor (:vendors env)]
      (str "@-" (name vendor) "-keyframes "
           (compile-node name-node env)
           (compile-node block-node env))))))

(defmethod compile-node :css.keyframes/name
  [[_ name] env]
  name)

(defmethod compile-node :css.keyframes/block
  [[_ & child-nodes] env]
  (let [new-env (inc-nesting-level env)]
    (str
     (:curly-brace-left env)
     (string/join (:separator env)
                  (compile-nodes child-nodes new-env))
     (indent env)
     (:curly-brace-right env))))

(defmethod compile-node :css.selector/keyframe
  [[_ selector-node] env]
  (compile-node selector-node env))

(defmethod compile-node :css.keyframes/rule
  [[_ selector-node declaration-block-node] env]
  (str (indent env)
       (compile-node selector-node env)
       (compile-node declaration-block-node env)))


;; =====================================================================
;; Main API

(defn compile-style
  "Convert a sequence of maps into CSS for use with the HTML style
   attribute."
  [maps]
  {:pre [(every? map? maps)]}
  (transduce
   (comp
    ;; A sequence of :css/declaration-block nodes.
    (map garden.parse/parse)
    (mapcat
     (fn [node]
       (garden.normalize/flatten-node node garden.normalize/empty-context)))
    ;; An alternating sequence of :css.declaration/property and
    ;; :css.declaration/value nodes.
    (mapcat garden.ast/children)
    (map (fn [node] (compile-node node default-env))))
   str
   ""
   maps))

(defn compile-vdom-style
  "Convert a sequence of maps into a map for use with the a virtual
  DOM `style` attribute."
  [maps]
  {:pre [(every? map? maps)]}
  (transduce
   (comp 
    ;; A sequence of :css/declaration-block nodes.
    (map garden.parse/parse)
    ;; A sequence of :css/declaration nodes.
    (mapcat garden.ast/children)
    ;; A sequence of :css.declaration/property and
    ;; :css.declaration/value pairs.
    (map garden.ast/children)
    ;; A sequence of compiled property and value pairs.
    (map (fn [property-and-value-nodes]
           (vec (compile-nodes property-and-value-nodes default-env)))))
   conj
   {}
   maps))

(defn- do-compile [options xs]
  (let [env (if (:pretty-print? options)
              (merge pretty-env options)
              (merge default-env options))]
    (-> xs
        (garden.parse/parse)
        (garden.normalize/normalize)
        (compile-node env))))

(defn- do-preamble
  "Prefix stylesheet with files in preamble. Not available in
  ClojureScript."
  [{:keys [preamble]} stylesheet]
  #?(:clj
     (string/join "\n" (conj (mapv slurp preamble) stylesheet)))
  #?(:cljs
     stylesheet))

(defn- do-output-to
  "Write contents of stylesheet to disk."
  [{:keys [output-to]} stylesheet]
  #?(:clj
     (when output-to
       (spit output-to stylesheet)
       (println "Wrote:" output-to)))
  stylesheet)

(defn compile-css
  "Compile `x` to CSS."
  [options x]
  (->> (do-compile options x)
       (do-preamble options)
       (do-output-to options)))
