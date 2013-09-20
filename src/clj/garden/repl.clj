(ns garden.repl
  "Method definitions for `print-method` with Garden types."
  (:require [garden.compiler :as compiler])
  (:import (garden.types CSSUnit
                         CSSFunction
                         CSSImport
                         CSSKeyframes
                         CSSMediaQuery)))

(defmethod print-method CSSUnit [css-unit writer]
  (.write writer (compiler/render-css css-unit)))

(defmethod print-method CSSFunction [css-function writer]
  (.write writer (compiler/render-css css-function)))

(defmethod print-method CSSImport [css-import writer]
  (.write writer (compiler/render-css css-import)))

(defmethod print-method CSSKeyframes [css-keyframes writer]
  (.write writer (compiler/compile-css css-keyframes)))

(defmethod print-method CSSMediaQuery [css-media-query writer]
  (.write writer (compiler/compile-css css-media-query)))

;; Show colors as hexadecimal values (ie. #000000) in the REPL.
(defmethod print-method CSSColor [color writer]
  (.write writer (str color)))