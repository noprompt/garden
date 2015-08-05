(ns garden.compression
  "Stylesheet compression utilities."
  #?(:clj
     (:import (java.io StringReader StringWriter)
               (com.yahoo.platform.yui.compressor CssCompressor))))

;; ---------------------------------------------------------------------
;; Clojure

;; Clojure stylesheet compression leverages the YUI Compressor as it
;; provides a performant and excellent solution to CSS compression.

#?(:clj
   (defn compress-stylesheet
     "Compress a stylesheet with the YUI CSSCompressor. Set
  line-break-position to -1 for no line breaks, 0 for a line break
  after each rule, and n > 0 for a line break after at most n
  columns. Defaults to no -1"
     ([stylesheet]
      (compress-stylesheet stylesheet -1))
     ([^String stylesheet line-break-position]
      (with-open [reader (StringReader. stylesheet)
                  writer (StringWriter.)]
        (doto (CssCompressor. reader)
          (.compress writer line-break-position))
        (str writer)))))

;; ---------------------------------------------------------------------
;; ClojureScript

;; ClojureScript stylesheet compression uses a simple tokenizer and
;; loop/recur to construct a new string of minified CSS.

#?(:cljs
   (defn- token-fn
     "Return a function which when given a string will return a map
  containing the chunk of text matched by re, it's size, and tag."
     [[tag re]]
     (fn [s]
       (when-let [chunk (re-find re s)]
         {:tag tag
          :chunk chunk
          :size (count chunk)}))))

#?(:cljs
   (defn- tokenizer
     "Given an arbitrary number of [tag regex] pairs, return a function
  which when given a string s will return the first matching token of s.
  Token precedence is determined by the order of the pairs. The first
  and last pairs have the highest and lowest precedence respectively."
     [& tags+regexes]
     (let [fs (map token-fn tags+regexes)]
       (fn [s]
         (some #(% s) fs)))))

#?(:cljs
   (def
     ^{:private true
       :doc "Tokenizer used during stylesheet compression."}
     stylesheet-tokenizer
     (tokenizer
      ;; String literals
      [:string #"^\"(?:\\.|[^\"])*\""]
      ;; Delimiters
      [:r-brace #"^\s*\{\s*"]
      [:l-brace #"^;?\s*}"]
      [:r-paren #"^\s*\(\s*"]
      [:l-paren #"^\s*\)"]
      [:comma #"^,\s*"]
      [:colon #"^:\s*"]
      [:semicolon #"^;"]
      ;; White space
      [:space+ #"^ +"]
      [:white-space+ #"^\s+"]
      ;; Everything else
      [:any #"^."])))

#?(:cljs
   (defn compress-stylesheet
     "Compress a string of CSS using a basic compressor."
     [stylesheet]
     (loop [s1 stylesheet s2 ""]
       (if-let [{:keys [tag chunk size]} (stylesheet-tokenizer s1)]
         (recur (subs s1 size)
                (str s2 (case tag
                          :string chunk
                          :r-brace "{"
                          :l-brace "}"
                          :r-paren "("
                          :l-paren ")"
                          :comma ","
                          :semi-comma ";"
                          :colon ":"
                          :space+ " "
                          :white-space+ ""
                          chunk)))
         s2))))
