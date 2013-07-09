(ns garden.def)

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
       => [\"[type=\"text\"] {:boder [\"1px\" :solid \"black\"]}]"
  [selector & more]
  (if-not (or (keyword? selector)
              (string? selector)
              (symbol? selector))
    (throw (IllegalArgumentException.
            "Selector must be either a keyword, string, or symbol."))
    (fn [& children]
      (into (apply vector selector more) children))))

(defmacro defrule
  "Define a function for creating rules. If only the `name` argument is
   provided the rule generating function will default to using it as the
   primary selector.

   Ex.
       (defrule a)
       => #'user/a
       (a {:text-decoration \"none\"})
       => [:a {:text-decoration \"none\"}]

   Ex.
       (defrule sub-headings :h4 :h5 :h6)
       => #'user/sub-headings
       (sub-headings {:font-weight \"normal\"})
       => [:h4 :h5 :h6 {:font-weight \"normal\"}]"
  [name & selectors]
  (let [rfn (if (seq selectors)
              (apply rule selectors)
              (rule (keyword name)))
        name (vary-meta name assoc :arglists '(list '[& children]))]
    `(def ~name ~rfn)))
