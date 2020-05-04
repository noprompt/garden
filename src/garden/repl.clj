(ns garden.repl
  "Method definitions for `print-method` with Garden types."
  (:require [garden.compiler :as compiler]
            [garden.util :as util]
            [garden.types]
            [garden.color]
            [garden.selectors :as selectors])
  (:import (garden.types CSSUnit
                         CSSFunction
                         CSSAtRule)
           (garden.color CSSColor)
           (garden.selectors CSSSelector)
           (java.io Writer)))

(defmethod print-method CSSUnit [css-unit writer]
  (.write ^Writer writer ^String (compiler/render-css css-unit)))

(defmethod print-method CSSFunction [css-function writer]
  (.write ^Writer writer ^String (compiler/render-css css-function)))

(defmethod print-method CSSColor [color writer]
  (.write ^Writer writer ^String (compiler/render-css color)))

(defmethod print-method CSSAtRule [css-at-rule writer]
  (let [f (if (or (util/at-keyframes? css-at-rule)
                  (util/at-media? css-at-rule))
            compiler/compile-css
            compiler/render-css)]
    (.write ^Writer writer ^String (f css-at-rule))))

(defmethod print-method CSSSelector [css-selector writer]
  (.write ^Writer writer ^String (selectors/css-selector css-selector)))
