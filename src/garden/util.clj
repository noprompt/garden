(ns garden.util
  "Utility functions used by Garden."
  (:refer-clojure :exclude [newline])
  (:require [clojure.string :as string]
            [garden.color :as color]))

(defprotocol ToString
  (^String to-str [this] "Convert a value into a string."))

(extend-protocol ToString
  clojure.lang.Keyword
  (to-str [this]
    (name this))
  java.awt.Color
  (to-str [this]
    (color/rgb->hex
     (color/rgb (.getRed this) (.getGreen this) (.getBlue this))))
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

(defn between?
  "True if n is a number between a and b."
  [n a b]
  (let [bottom (min a b)
        top (max a b)]
    (and (>= n bottom) (<= n top))))

(defn ^String wrap-quotes
  "Wrap a string with double quotes."
  [s]
  (str \" s \"))

;;;; Output style and formatting

(def ^{:doc "Map associating output-style options to characters used when
             rendering CSS."}
  output-style
  {:expanded {:comma ", "
              :colon ": "
              :semicolon ";\n"
              :left-brace " {\n"
              :right-brace ";\n}"
              :media-left-brace " {\n\n"
              :media-right-brace "\n\n}"
              :rule-separator "\n\n"
              :newline "\n"
              :indent 2}
   :compact {:comma ", "
             :colon ": "
             :semicolon "; "
             :left-brace " { "
             :right-brace "; }"
             :media-left-brace " {\n"
             :media-right-brace "\n}"
             :rule-separator "\n"
             :newline "\n"
             :indent 0}
   :compressed {:comma ","
                :colon ":"
                :semicolon ";"
                :left-brace "{"
                :right-brace "}"
                :media-left-brace "{"
                :media-right-brace "}"
                :rule-separator ""
                :newline ""
                :indent 0}})

(def ^{:dynamic true
       :doc "The stylesheet output style."}
  *output-style* :compressed)

(letfn [(output [k]
          #(get-in output-style [*output-style* k]))]
  (def comma (output :comma))
  (def colon (output :colon))
  (def semicolon (output :semicolon))
  (def left-brace (output :left-brace))
  (def right-brace (output :right-brace))
  (def rule-separator (output :rule-separator))
  (def newline (output :newline))
  (def indent-level (output :indent))
  (def media-left-brace (output :media-left-brace))
  (def media-right-brace (output :media-right-brace)))

(defmacro with-output-style
  "Set the output style for rendering CSS strings. The value of style may be
   either :expanded, :compact, or :compressed. Defaults to compressed."
  [style & body]
  (let [k (keyword style)
        style (if (contains? output-style k) k :compressed)]
    `(binding [*output-style* ~style]
       ~@body)))

(declare comma-join space-join)

(defn comma-join
  "Return a comma separated list of values. Subsequences are joined with
   spaces."
  [xs]
  (let [ys (for [x xs] (if (sequential? x) (space-join x) (to-str x)))]
    (string/join (comma) ys)))

(defn space-join
  "Return a space separated list of values. Subsequences are joined with
   commas."
  [xs]
  (let [ys (for [x xs] (if (sequential? x) (comma-join x) (to-str x)))]
    (string/join \space ys)))

(defn without-meta
  "Return obj with meta removed."
  [obj]
  (with-meta obj nil))

(defn record?
  "Return true if obj is an instance of clojure.lang.IRecord."
  [obj]
  (instance? clojure.lang.IRecord obj))
