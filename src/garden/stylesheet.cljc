(ns garden.stylesheet
  "Utility functions for CSS properties, directives and functions."
  (:require [garden.util :as util]
            [garden.color :as color]
            [garden.media :as media]
            [garden.keyframes :as keyframes]
            [garden.parse]))

;;;; ## Stylesheet helpers

(defn rule
  "Create a rule function for the given selector. The `selector`
  argument must be valid selector (ie. a keyword, string, or symbol).
  Additional arguments may consist of extra selectors or
  declarations.

  The returned function accepts any number of arguments which represent
  the rule's children.

  Ex.
      (let [text-field (rule \"[type=\"text\"])]
       (text-field {:border [\"1px\" :solid \"black\"]}))
      ;; => [\"[type=\"text\"] {:boder [\"1px\" :solid \"black\"]}]"
  [selector & more]
  (if-not (or (keyword? selector)
              (string? selector)
              (symbol? selector))
    (throw (ex-info
            "Selector must be either a keyword, string, or symbol." {}))
    (fn [& children]
      (into (apply vector selector more) children))))

;; ---------------------------------------------------------------------
;; @-rules

(defrecord FontFaceRule [declarations]
  garden.parse/IParse
  (-parse [f]
    (into
     [:css/font-face]
     (map garden.parse/parse declarations))))

(defn at-font-face
  "Create a CSS @font-face rule."
  [& declarations]
  {:pre [(every? map? declarations)]}
  (map->FontFaceRule
   {:declarations declarations}))

(defrecord ImportRule [url media-query-list]
  garden.parse/IParse
  (-parse [i]
    [:css/import
     url
     (if-let [media-query-list (:media-query-list i)]
       (garden.parse/parse media-query-list)
       [:css/noop])]))

(defn at-import
  "Create a CSS @import rule."
  ([url]
   (map->ImportRule
    {:media-query-list nil
     :url url}))
  ([url media-query]
   (map->ImportRule
    {:media-query-list media-query
     :url url})))

(defn at-media
  "Create a CSS @media rule."
  [media-queries & rules]
  (apply media/rule media-queries rules))

(defn at-keyframes
  "Create a CSS @keyframes rule."
  [identifier & frames]
  (apply keyframes/rule identifier frames))

;; ---------------------------------------------------------------------
;; Functions

(defrecord Function [name args]
  garden.parse/IParse
  (-parse [f]
    [:css/function
     [:css/identifier (clojure.core/name (:name f))]
     (garden.parse/parse-comma-separated-list (:args f))]))

(defn cssfn [fn-name]
  (fn [& args]
    (Function. (name fn-name) (vec args))))

(defn rgb
  "Create a color from RGB values."
  [r g b]
  (color/rgb [r g b]))

(defn hsl
  "Create a color from HSL values."
  [h s l]
  (color/hsl [h s l]))

;; ---------------------------------------------------------------------
;; calc

(defrecord Calc [arg]
  garden.parse/IParse
  (-parse [c]
    [:css/calc (garden.parse/parse (:arg c))]))

(defrecord CalcDifference [arg-1 arg-2]
  garden.parse/IParse
  (-parse [c]
    [:css.calc/difference
     (garden.parse/parse (:arg-1 c))
     (garden.parse/parse (:arg-2 c))]))

(defrecord CalcProduct [arg-1 arg-2]
  garden.parse/IParse
  (-parse [c]
    [:css.calc/product
     (garden.parse/parse (:arg-1 c))
     (garden.parse/parse (:arg-2 c))]))

(defrecord CalcQuotient [arg-1 arg-2]
  garden.parse/IParse
  (-parse [c]
    [:css.calc/quotient
     (garden.parse/parse (:arg-1 c))
     (garden.parse/parse (:arg-2 c))]))

(defrecord CalcSum [arg-1 arg-2]
  garden.parse/IParse
  (-parse [c]
    [:css.calc/sum
     (garden.parse/parse (:arg-1 c))
     (garden.parse/parse (:arg-2 c))]))

#?(:clj
   (def ^{:private true
          :arglists '([sym])}
     calc-op->ctor
     {'* `map->CalcProduct
      '+ `map->CalcSum
      '- `map->CalcDifference
      '/ `map->CalcQuotient}))

#?(:clj
   (defn unroll-calc-op
     {:private true}
     [form]
     (let [[op & args] form]
       (if-let [ctor (calc-op->ctor op)]
         (let[[a b & rest] args]
           (reduce
            (fn [calc-sum arg]
              `(~ctor {:arg-1 ~calc-sum
                       :arg-2 ~arg}))
            `(~ctor {:arg-1 ~a
                     :arg-2 ~b})
            rest))
         form))))

#?(:clj
   (defn parse-calc
     {:private true}
     [form]
     (let [value (if (seq? form)
                   (clojure.walk/postwalk
                    (fn [form*]
                      (if (seq? form*)
                        (unroll-calc-op form*)
                        form*))
                    form)
                   form)]
       `(Calc. ~value))))

#?(:clj
   (defmacro calc [expr]
     (parse-calc expr)))
