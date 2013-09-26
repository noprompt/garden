(ns garden.repl
  "Method definitions for `print-method` with Garden types."
  (:require [garden.compiler :as compiler]
            [garden.util :as util]
            [garden.types]
            [garden.color])
  (:import (garden.types CSSUnit
                         CSSFunction
                         CSSAtRule)
           (garden.color CSSColor)))

(defmethod print-method CSSUnit [css-unit writer]
  (.write writer (compiler/render-css css-unit)))

(defmethod print-method CSSFunction [css-function writer]
  (.write writer (compiler/render-css css-function)))

;; Show colors as hexadecimal values (ie. #000000) in the REPL.
(defmethod print-method CSSColor [color writer]
  (.write writer (str color)))

(defmethod print-method CSSAtRule [css-at-rule writer]
  (let [f (if (or (util/at-keyframes? css-at-rule)
                  (util/at-media? css-at-rule))
            compiler/compile-css
            compiler/render-css)]
    (.write writer (f css-at-rule))))
