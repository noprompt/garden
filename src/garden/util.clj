(ns garden.util
  "Utility functions used by Garden."
  (:refer-clojure :exclude [newline])
  (:require [clojure.string :as string]))

(defprotocol ToString
  (^String to-str [this] "Convert a value into a string."))

(extend-protocol ToString
  clojure.lang.Keyword
  (to-str [this]
    (name this))
  Object
  (to-str [this]
    (str this))
  nil
  (to-str [_]
    ""))

(defn ^String as-str
  "Convert a variable number of values into strings."
  [& args]
  (apply str (map to-str args)))

(defn ^Boolean natural?
  "True if n is a natural number."
  [n]
  (and (integer? n) (pos? n)))

(defn ^String wrap-quotes
  "Wrap a string with double quotes."
  [s]
  (str \" s \"))

;;;; Output style and formatting

(def ^{:private true
       :doc "Map associating output-style options to characters used when
             rendering CSS."}
  output-style
  {:expanded {:comma ", "
              :colon ": "
              :semicolon ";\n"
              :left-brace " {\n"
              :right-brace ";\n}"
              :rule-separator "\n\n"
              :newline "\n"
              :indent 2}
   :compact {:comma ", "
             :colon ": "
             :semicolon "; "
             :left-brace " { "
             :right-brace "; }"
             :rule-separator "\n"
             :newline "\n"
             :indent 0}
   :compressed {:comma ","
                :colon ":"
                :semicolon ";"
                :left-brace "{"
                :right-brace "}"
                :rule-separator ""
                :newline ""
                :indent 0}})

(def ^{:dynamic true
       :doc "The stylesheet output style."}
  *output-style* :compressed)

(defn- output [k]
  (fn [] (-> output-style *output-style* k)))

(def comma (output :comma))
(def colon (output :colon))
(def semicolon (output :semicolon))
(def left-brace (output :left-brace))
(def right-brace (output :right-brace))
(def rule-separator (output :rule-separator))
(def newline (output :newline))
(def indent-level (output :indent))

(defmacro with-output-style
  "Set the output style for rendering CSS strings. The value of style may be
   either :expanded, :compact, or :compressed. Defaults to compressed."
  [style & body]
  (let [style (if (contains? output-style (keyword style))
                (keyword style)
                :compressed)]
    `(binding [*output-style* ~style]
       ~@body)))

(declare comma-join space-join)

(defn comma-join
  "Return a comma separated list of values. Subsequences are joined with
   spaces."
  [xs]
  (let [ys (map #(if (sequential? %) (space-join %) (to-str %)) xs)]
    (string/join (comma) ys)))

(defn space-join
  "Return a space separated list of values. Subsequences are joined with
   commas."
  [xs]
  (let [ys (map #(if (sequential? %) (comma-join %) (to-str %)) xs)]
    (string/join \space ys)))
