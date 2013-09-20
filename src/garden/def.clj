(ns garden.def
  (:import (garden.types CSSFunction
                         CSSKeyframes)))

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
      ;; => #'user/a

      (a {:text-decoration \"none\"})
      ;; => [:a {:text-decoration \"none\"}]

  Ex.
      (defrule sub-headings :h4 :h5 :h6)
      ;; => #'user/sub-headings

      (sub-headings {:font-weight \"normal\"})
      ;; => [:h4 :h5 :h6 {:font-weight \"normal\"}]"
  [name & selectors]
  (let [rfn (if (seq selectors)
              (apply rule selectors)
              (rule (keyword name)))
        name (vary-meta name assoc :arglists '(list '[& children]))]
    `(def ~name ~rfn)))


(defn cssfn [fn-name]
  (fn [& args]
    (CSSFunction. fn-name args)))

(defmacro ^{:arglists '([name] [name docstring? & fn-tail])}
  defcssfn
  "Define a function for creating custom CSS functions. The generated
  function will automatically create an instance of
  `garden.types.CSSFunction` of which the `:args` field will be set
  to whatever the return value of the original function is. The
  `:function` field will be set to `(str name)`.

  If only the `name` argument is provided the returned function will
  accept any number of arguments.

  Ex.
      (def cssfn url)
      ;; => #'user/url

      (url \"http://fonts.googleapis.com/css?family=Lato\")
      ;; => #garden.types.CSSFunction{:function \"url\", :args \"http://fonts.googleapis.com/css?family=Lato\"}

      (css (url \"http://fonts.googleapis.com/css?family=Lato\"))
      ;; => url(http://fonts.googleapis.com/css?family=Lato) 

  Ex.
      (defcssfn attr
        ([name] name)
        ([name type-or-unit]
           [[name type-or-unit]])
        ([name type-or-unit fallback]
           [name [type-or-unit fallback]]))
      ;; => #'user/attr

      (attr :vertical :length)
      ;; => #garden.types.CSSFunction{:function \"url\", :args [:vertical :length]}

      (css (attr :vertical :length))
      ;; => \"attr(vertical length)\"

      (attr :end-of-quote :string :inherit) 
      ;; => #garden.types.CSSFunction{:function \"url\", :args [:end-of-quote [:string :inherit]]}

      (css (attr :end-of-quote :string :inherit))
      ;; => \"attr(end-of-quote string, inherit)\""
  ([name]
     (let [name (vary-meta name assoc :arglists '(list '[& args]))]
       `(def ~name (cssfn (str '~name)))))
  ([name & fn-tail]
     (let [docstring? (when (string? (first fn-tail))
                        (first fn-tail))
           fn-tail (if docstring?
                     (rest fn-tail)
                     fn-tail)
           arglists (if (every? list? fn-tail)
                      (map first fn-tail)
                      (list (first fn-tail)))
           name (vary-meta name assoc :arglists `'~arglists)
           name (if docstring?
                  (vary-meta name assoc :doc docstring?)
                  name)]
       `(def ~name
          (fn [& args#]
            (CSSFunction. (str '~name) (apply (fn ~@fn-tail) args#)))))))

(defmacro defkeyframes
  "Define a CSS @keyframes animation.

  Ex. 
      (defkeyframes my-animation
        [:from
         {:background \"red\"}]

        [:to
         {:background \"yellow\"}])

      (css {:vendors [\"webkit\"]}
        my-animation ;; Include the animation in the stylesheet.
        [:div
         ^:prefix ;; Use vendor prefixing (optional).
         {:animation [[my-animation \"5s\"]]}])"
  [name & frames]
  `(def ~name
     (CSSKeyframes. (str '~name) (list ~@frames))))
