(ns garden.def)

(defn rule
  "Create a rule function for the given selector. The returned
   function accepts any number of arguments which represent the
   selector's children.

   ex. (let [text-field (rule \"[type=\"text\"])]
         (text-field {:border [\"1px\" :solid \"black\"]}))
       => [\"[type=\"text\"] {:boder [\"1px\" :solid \"black\"]}]"
  [selector & more]
  (fn [& children]
    (into (vec (cons selector more)) children)))

(defmacro defrule
  "Define a function for creating rules.

   ex. (defrule sub-headings :h4 :h5 :h6)
       => #'user/sub-headings
       (sub-headings {:font-weight \"normal\"})
       => [:h4 :h5 :h6 {:font-weight \"normal\"}]"
  [name & selectors]
  (let [r (if (seq selectors)
            (apply rule selectors)
            (rule (keyword name)))]
    `(def ~name (partial ~r))))
