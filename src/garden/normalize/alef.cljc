(ns garden.normalize.alef
  (:require
   [garden.ast.alef]
   [clojure.spec :as spec]
   [garden.util.alef]))

(spec/def ::parent-media-query-list
  (spec/or :noop garden.ast.alef/noop?
           :media-query-list garden.ast.alef/media-query-list?))

(spec/def ::parent-node
  garden.ast.alef/node?)

(spec/def ::parent-selector
  (spec/or :noop garden.ast.alef/noop?
           :selector garden.ast.alef/selector?))

(spec/def ::context
  (spec/keys :req-un [::parent-media-query-list
                      ::parent-property-node
                      ::parent-selector]))

(def empty-context
  {:parent-property-node [:css/noop]
   :parent-media-query-list [:css/noop]
   :parent-node-tag nil
   :parent-selector [:css/noop]})

;; ---------------------------------------------------------------------
;; Selector nesting

(defmulti
  ^{:arglists '([child-selector parent-selector])}
  nest-selector
  "Nest `child-selector` within `parent-selector`."
  (fn [child-selector parent-selector]
    [(garden.ast.alef/tag child-selector) (garden.ast.alef/tag parent-selector)]))

(defmethod nest-selector [:css.selector/simple :css/noop]
  [child-selector _]
  child-selector)

(defmethod nest-selector [:css.selector/simple :css.selector/simple]
  [child-selector parent-selector]
  [:css.selector/compound parent-selector child-selector])

(defmethod nest-selector [:css.selector/simple :css.selector/compound]
  [child-selector parent-selector]
  (conj parent-selector child-selector))

(defmethod nest-selector [:css.selector/simple :css.selector/complex]
  [child-selector parent-selector]
  (into [:css.selector/complex]
        (map nest-selector (repeat child-selector) (rest parent-selector))))

;; Compound selector nesting

(defmethod nest-selector [:css.selector/compound :css/noop]
  [child-selector _]
  child-selector)

(defmethod nest-selector [:css.selector/compound :css.selector/simple]
  [child-selector parent-selector]
  (into [:css.selector/compound parent-selector] (rest child-selector)))

(defmethod nest-selector [:css.selector/compound :css.selector/compound]
  [child-selector parent-selector]
  (into parent-selector (rest child-selector)))

(defmethod nest-selector [:css.selector/compound :css.selector/complex]
  [child-selector parent-selector]
  (into [:css.selector/complex]
        (map nest-selector (repeat child-selector) (rest parent-selector))))

;; Complex selector nesting

(defmethod nest-selector [:css.selector/complex :css/noop]
  [child-selector _]
  child-selector)

(defmethod nest-selector [:css.selector/complex :css.selector/simple]
  [child-selector parent-selector]
  (into [:css.selector/complex]
        (map nest-selector (rest child-selector) (repeat parent-selector))))

(defmethod nest-selector [:css.selector/complex :css.selector/compound]
  [child-selector parent-selector]
  (into [:css.selector/complex]
        (map nest-selector (rest child-selector) (repeat parent-selector))))

(defmethod nest-selector [:css.selector/complex :css.selector/complex]
  [child-selector parent-selector]
  (into [:css.selector/complex]
        (map (fn [[child-selector parent-selector]]
               (nest-selector child-selector parent-selector))
             (garden.util.alef/cartesian-product (rest child-selector)
                                            (rest parent-selector)))))

;; Complex selector nesting

(defmethod nest-selector [:css.selector/parent-reference :css/noop]
  [[_ reference] _]
  [:css.selector/simple reference])

(defmethod nest-selector [:css.selector/parent-reference :css.selector/simple]
  [[_ reference] [_ parent-selector]]
  [:css.selector/simple (str parent-selector reference)])

(defmethod nest-selector [:css.selector/parent-reference :css.selector/compound]
  [child-selector [_ & compound-children]]
  (-> [:css.selector/compound]
      (into (butlast compound-children))
      (conj (nest-selector child-selector
                           (last compound-children)))))

(defmethod nest-selector [:css.selector/parent-reference :css.selector/complex]
  [child-selector [_ & complex-children]]
  (into [:css.selector/complex]
        (map (fn [complex-child]
               (nest-selector child-selector complex-child))
             complex-children)))

;; ---------------------------------------------------------------------
;; Media query nesting

(defmulti
  ^{:arglists '([child-media-query parent-media-query])}
  nest-media-query
  (fn [[_ child-media-constraint child-media-type _]
       [_ parent-media-constraint parent-media-type _]]
    [[(garden.ast.alef/tag child-media-constraint) (garden.ast.alef/tag child-media-type)]
     [(garden.ast.alef/tag parent-media-constraint) (garden.ast.alef/tag parent-media-type)]]))

;; Neither parent or child has media constraints or types.
(defmethod nest-media-query [[:css/noop :css/noop]
                             [:css/noop :css/noop]]
  [child-media-query parent-media-query]
  (let [[_ _ _ [_ & child-expressions]] child-media-query
        [_ _ _ parent-conjunction] parent-media-query]
    [:css.media/query
     [:css/noop]
     [:css/noop]
     (into parent-conjunction child-expressions)]))

;; Neither parent or child has media constraints, child has media
;; type but parent does not.
(defmethod nest-media-query [[:css/noop :css.media.query/type]
                             [:css/noop :css/noop]]
  [child-media-query parent-media-query]
  (let [[_ _ child-media-type] child-media-query]
    (let [[_ _ _ [_ & child-expressions]] child-media-query
          [_ _ _ parent-conjunction] parent-media-query]
      [:css.media/query
       [:css/noop]
       child-media-type
       (into parent-conjunction child-expressions)])))

;; Neither parent or child has media constraints, child has no media
;; type but parent does.
(defmethod nest-media-query [[:css/noop :css/noop]
                             [:css/noop :css.media.query/type]]
  [child-media-query parent-media-query]
  (let [[_ _ parent-media-type] parent-media-query]
    (let [[_ _ _ [_ & child-expressions]] child-media-query
          [_ _ _ parent-conjunction] parent-media-query]
      [:css.media/query
       [:css/noop]
       parent-media-type
       (into parent-conjunction child-expressions)])))

;; Neither parent or child has media constraints, both parent and
;; child have media type.
(defmethod nest-media-query [[:css/noop :css.media.query/type]
                             [:css/noop :css.media.query/type]]
  [child-media-query parent-media-query]
  (let [[_ _ child-media-type] child-media-query
        [_ _ parent-media-type] parent-media-query]
    (if (= child-media-type
           parent-media-type)
      (let [[_ _ _ [_ & child-expressions]] child-media-query
            [_ _ _ parent-conjunction] parent-media-query]
        [:css.media/query
         [:css/noop]
         parent-media-type
         (into parent-conjunction child-expressions)])
      [:css/noop])))

;; Child has no constraint and no media type, parent has a constraint
;; and a media type.
(defmethod nest-media-query [[:css/noop :css/noop]
                             [:css.media.query/constraint :css.media.query/type]]
  [child-media-query parent-media-query]
  (let [[_ parent-constraint] parent-media-query]
    (case parent-constraint
      [:css.media.query/constraint "not"]
      parent-media-query

      [:css.media.query/constraint "only"]
      (let [[_ _ _ [_ & child-expressions]] child-media-query
            [_ _ parent-type parent-conjunction] parent-media-query]
        [:css.media/query
         parent-constraint
         parent-type
         (into parent-conjunction child-expressions)]))))

;; Child has no constraint but has a media type, parent has a
;; constraint and a media type.
(defmethod nest-media-query [[:css/noop :css.media.query/type]
                             [:css.media.query/constraint :css.media.query/type]]
  [child-media-query parent-media-query]
  (let [[_ _ child-type] child-media-query
        [_ parent-constraint parent-type] parent-media-query]
    (if (and (= child-type
                parent-type)
             (not= parent-constraint [:css.media.query/constraint "not"]))
      (let [[_ _ _ [_ & child-expressions]] child-media-query
            [_ _ _ parent-conjunction] parent-media-query]
        [:css.media/query
         parent-constraint
         parent-type
         (into parent-conjunction child-expressions)])
      [:css/noop])))

;; Child has constraint and a media type, parent has no constraint and
;; a media type.
(defmethod nest-media-query [[:css.media.query/constraint :css.media.query/type]
                             [:css/noop :css.media.query/type]]
  [child-media-query parent-media-query]
  (let [[_ child-constraint child-type] child-media-query
        [_ _ parent-type] parent-media-query]
    (cond
      (= child-constraint [:css.media/constraint "not"])
      [:css/noop]

      (= child-type parent-type)
      (let [[_ _ _ [_ & child-expressions]] child-media-query
            [_ _ _ parent-conjunction] parent-media-query]
        [:css.media/query
         child-constraint
         child-type
         (into parent-conjunction child-expressions)])

      :else
      [:css/noop])))

;; Both parent and child have constraint and type.
(defmethod nest-media-query [[:css.media.query/constraint :css.media.query/type]
                             [:css.media.query/constraint :css.media.query/type]]
  [child-media-query parent-media-query]
  (let [[_ child-constraint child-type] child-media-query
        [_ parent-constraint parent-type] parent-media-query]
    (if (and (= child-constraint
                parent-constraint)
             (= child-type
                parent-type))
      (let [[_ _ _ [_ & child-expressions]] child-media-query
            [_ _ _ parent-conjunction] parent-media-query]
        [:css.media/query
         parent-constraint
         parent-type
         (into parent-conjunction child-expressions)])
      [:css/noop])))

(defn nest-media-query-list [child-media-query-list parent-media-query-list]
  {:pre [(= (garden.ast.alef/tag child-media-query-list) 
            :css.media/query-list)
         (or (= (garden.ast.alef/tag parent-media-query-list)
                :css.media/query-list)
             (garden.ast.alef/noop? parent-media-query-list))]}
  (if (= parent-media-query-list [:css/noop])
    child-media-query-list
    (let [[_ & child-queries] child-media-query-list
          [_ & parent-queries] parent-media-query-list]
      (into [:css.media/query-list]
            (for [child-query child-queries
                  parent-query parent-queries]
              (nest-media-query child-query parent-query))))))

(defn normalize-selector
  "Normalizes `selector` by nesting it within the current context's
  parent selector."
  {:private true}
  [selector context]
  (let [parent-selector (:parent-selector context)]
    (if (identical? selector parent-selector)
      selector
      (nest-selector selector parent-selector))))

(defn normalize-media-query-list
  {:private true}
  [media-query-list context]
  (let [{:keys [parent-media-query-list]} context]
    (if (some? parent-media-query-list)
      (if (identical? media-query-list parent-media-query-list)
        media-query-list
        (nest-media-query-list media-query-list parent-media-query-list))
      media-query-list)))

(defmulti flatten-node
  (fn [node context]
    (spec/assert ::context context)
    (if (garden.ast.alef/node? node)
      (garden.ast.alef/tag node)
      ::not-node))
  :default ::pass)

(defn flatten-nodes [nodes context]
  (mapcat
   (fn [node]
     (flatten-node node context))
   nodes))

(defmethod flatten-node ::not-node
  [x _context]
  (list x))

(defmethod flatten-node ::pass
  [[node-tag & children] context]
  (let [context* (assoc context :parent-node-tag node-tag)]
    (list (into [node-tag] (flatten-nodes children context*)))))

(defn normalize-property
  {:private true}
  [property-node context]
  (let [{:keys [parent-property-node]} context]
    (if (garden.ast.alef/noop? parent-property-node)
      property-node
      (let [[_ parent-property] parent-property-node
            [_ property] property-node
            property* (str parent-property "-" property)]
        [:css.declaration/property property*]))))

(defmethod flatten-node :css/declaration
  [[_ property-node value-node :as node] context]
  (let [property-node* (normalize-property property-node context)
        [_ value-child-node] value-node]
    (case (garden.ast.alef/tag value-child-node)
      :css.declaration/block
      (let [context* (assoc context :parent-property-node property-node*)]
        (flatten-nodes (garden.ast.alef/children value-child-node) context*))

      ;; else
      (list
       ;; `with-meta` is a hangover form parsing declaration blocks
       ;; which have meta attached to them. When the meta data can
       ;; be ignored this should go away.
       (with-meta [:css/declaration property-node* value-node]
         (meta node))))))

(defmethod flatten-node :css/fragment
  [[_ & children] context]
  (flatten-nodes children context))

(defmethod flatten-node :css/keyframes
  [keyframes-node context]
  (let [{:keys [parent-node-tag]} context
        root-contexts #{nil
                        :css/stylesheet
                        :css.media/rule}]
    (if (contains? root-contexts parent-node-tag) 
      (list keyframes-node)
      (let [[_ [_ keyframes-name]] keyframes-node]
        (list [:css/identifier keyframes-name])))))

(defmethod flatten-node :css/import
  [import-node context]
  (let [[_ url media-query-list ] import-node
        {:keys [parent-media-query-list]} context]
    (list
     (case [(garden.ast.alef/noop? media-query-list) (garden.ast.alef/noop? parent-media-query-list)]
       [false false]
       [:css/import url (nest-media-query-list media-query-list parent-media-query-list)]

       [true false]
       [:css/import url parent-media-query-list]

       ;; else
       import-node))))

(defmethod flatten-node :css/rule
  [[tag selector declaration-block & children] context]
  (let [selector* (normalize-selector selector context)
        declaration-block* (flatten-node declaration-block context)
        context* (assoc context
                        :parent-selector selector*
                        :parent-node-tag tag)
        rule-node (into [tag selector*] declaration-block*)]
    (cons rule-node (flatten-nodes children context*))))

(defmethod flatten-node :css/stylesheet
  [[tag & children] context]
  (flatten-nodes children (assoc context :parent-node-tag tag)))

(defmethod flatten-node :css.media/rule
  [[tag media-query-list & children] context]
  (let [media-query-list* (normalize-media-query-list media-query-list context)
        context* (assoc context
                        :parent-media-query-list media-query-list*
                        :parent-node-tag tag)
        [_ query] media-query-list*]
    (if (= query [:css/noop])
      (list)
      (seq
       (reduce
        (fn [state node]
          (if (garden.ast.alef/top-level-node? node)
            (conj state node)
            (update state 0 conj node)))
        [;; Media query node.
         [tag media-query-list*]
         ;; Top level nodes.
         ,,,]
        (flatten-nodes children context*))))))


(defmulti insert-node
  {:arglists '([stylesheet node])
   :doc "Add `node` to the stylesheet `stylesheet`."
   :private true}
  (fn [stylesheet-node node]
    {:pre [(garden.ast.alef/node? stylesheet-node)]}
    (garden.ast.alef/tag node))
  :default ::default)

(defmethod insert-node ::default
  [stylesheet node]
  (conj stylesheet node))

(defmethod insert-node :css/noop
  [stylesheet _noop-node]
  stylesheet)

(defmethod insert-node :css/charset
  [stylesheet charset-node]
  (let [[_ & children] stylesheet
        [charset-nodes non-charset-nodes] (split-with garden.ast.alef/charset? children)]
    (into [:css/stylesheet] (concat charset-nodes (cons charset-node non-charset-nodes)))))        

(defmethod insert-node :css/import
  [stylesheet import-node]
  (let [[_ & children] stylesheet
        [head-nodes tail-nodes] (split-with
                                 (some-fn garden.ast.alef/charset?
                                          garden.ast.alef/import?)
                                 children)]
    (into [:css/stylesheet] (concat head-nodes (cons import-node tail-nodes)))))

(defn normalize
  [node]
  (reduce insert-node [:css/stylesheet] (flatten-node node empty-context)))
