(ns garden.def
  (:require [clojure.spec :as spec]
            [garden.core]
            [garden.keyframes]
            [garden.parse]
            [garden.stylesheet]
            [garden.util :as util]))

(defmacro defstyles
  "Convenience macro equivalent to `(def name (list styles*))`."
  [name & styles]
  `(def ~name (list ~@styles)))

(defmacro defstylesheet
  "Convenience macro equivalent to `(def name (css opts? styles*))`."
  [name & styles]
  `(def ~name
     (garden.core/css ~@styles)))

(defmacro defrule
  "Define a function for creating rules. If only the `name` argument is
  provided the rule generating function will default to using it as the
  primary selector.

  Ex.
      (defrule a)
      ;; => #'user/a

      (a {:text-decoration \"none\"})
      ;; => [#{:a} {:text-decoration \"none\"}]

  Ex.
      (defrule sub-headings :h4 :h5 :h6)
      ;; => #'user/sub-headings

      (sub-headings {:font-weight \"normal\"})
      ;; => [#{:h4 :h5 :h6} {:font-weight \"normal\"}]"
  [sym & selectors]
  (let [selector (if (seq selectors)
                   `(set '~selectors)
                   #{(keyword sym)})
        rule [selector]
        [_ sym spec] (macroexpand `(defn ~sym [~'& ~'children]
                                     (into ~rule ~'children)))]
    `(def ~sym ~spec)))

(defmacro
  ^{:arglists '([name] [name docstring? & fn-tail])}
  defcssfn
  "Define a function for creating custom CSS functions. The generated
  function will automatically create an instance of
  `garden.stylesheet.Function` of which the `:args` field will be set
  to whatever the return value of the original function is. The
  `:function` field will be set to `(str name)`.

  If only the `name` argument is provided the returned function will
  accept any number of arguments.

  Ex.
      (def cssfn url)
      ;; => #'user/url

      (url \"http://fonts.googleapis.com/css?family=Lato\")
      ;; => #garden.stylesheet.Function{:function \"url\", :args \"http://fonts.googleapis.com/css?family=Lato\"}

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
      ;; => #garden.stylesheet.Function{:name \"url\", :args [:vertical :length]}

      (css (attr :vertical :length))
      ;; => \"attr(vertical length)\"

      (attr :end-of-quote :string :inherit) 
      ;; => #garden.stylesheet.Function{:function \"url\", :args [:end-of-quote [:string :inherit]]}

      (css (attr :end-of-quote :string :inherit))
      ;; => \"attr(end-of-quote string, inherit)\""
  ([sym]
   (let [[_ sym fn-tail] (macroexpand
                          `(defn ~sym [& args#]
                             (garden.stylesheet.Function.
                              ~(str sym)
                              (vec args#))))]
     `(def ~sym ~fn-tail)))
  ([sym & fn-tail]
   (let [[_ sym [_ & fn-spec]] (macroexpand `(defn ~sym ~@fn-tail))
         cssfn-name (str sym)]
     `(def ~sym
        (fn [& args#]
          (let [result# (apply (fn ~@fn-spec) args#)]
            (if (vector? result#)
              (garden.stylesheet.Function. ~cssfn-name result#)
              (throw
               (ex-info ~(format "The CSS function `%s` must return vector"
                                 (symbol (name (ns-name *ns*)) (name sym)))
                        (spec/explain-data vector? result#))))))))))

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
  [sym & frames]
  `(def ~sym
     (garden.keyframes/rule ~(name sym) ~@frames)))


