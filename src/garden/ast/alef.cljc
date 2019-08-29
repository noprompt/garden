(ns garden.ast.alef)

(defn node?
  "True if `x` is an AST node, false otherwise."
  [x]
  (and (vector? x)
       (keyword? (first x))))

(defn tag
  "Used to retrieve the tag from tagged union value (e.g. a vector
  such that the first element is a keyword)."
  [node]
  {:pre [(node? node)]}
  (first node))

(defn children
  {:arglists '([node])}
  [node]
  {:pre [(node? node)]}
  (let [[_ & children] node]
    children))

(defn ast?
  "true if `x` is a valid CSS AST, false othwerise."
  [x]
  ;; Always true for now until a spec for an AST can be written.
  true)

(def noop
  [:css/noop])

(defn noop? [node]
  {:pre [(node? node)]}
  (= noop node))

(def top-level-node-tags
  #{:css/charset
    :css/import
    :css.media/rule
    :css/keyframes})

(defn top-level-node?
  [node]
  {:pre [(node? node)]}
  (contains? top-level-node-tags (tag node)))

(def selector-tags
  #{:css.selector/simple
    :css.selector/compound
    :css.selector/complex})

(defn selector?
  "True if `x` is a CSS selector node, false otherwise."
  [x]
  (and (node? x)
       (contains? selector-tags (tag x))))

(defn media-query-list?
  "True if `x` is a CSS media query list node, false otherwise."
  [x]
  (and (node? x)
       (= (tag x) :css.media/query-list)))

(defn charset?
  "True if `x` is a CSS charset node, false otherwise."
  [x]
  (and (node? x)
       (= (tag x) :css/charset)))

(defn import?
  "True if `x` is a CSS import node, false otherwise."
  [x]
  (and (node? x)
       (= (tag x) :css/import)))

(defn function?
  "True if `x` is a CSS import node, false otherwise."
  [x]
  (and (node? x)
       (= (tag x) :css/function)))
