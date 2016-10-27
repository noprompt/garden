(ns garden.repl
  "Method definitions for `print-method` with Garden types."
  (:require [garden.compiler :as compiler]
            [garden.util :as util]
            [garden.types]
            [garden.color]
            [garden.selectors :as selectors])
  (:import (garden.types CSSFunction
                         CSSAtRule)
           (garden.color Hsl
                         Hsla
                         Rgb
                         Rgba)
           (garden.selectors CSSSelector)))

#_
(defmethod print-method CSSUnit [css-unit writer]
  (.write writer (compiler/render-css css-unit)))

#_
(defmethod print-method CSSFunction [css-function writer]
  (.write writer (compiler/render-css css-function)))

#_
(defmethod print-method Hsl [color writer]
  (.write writer (compiler/render-css color)))

#_
(defmethod print-method Hsla [color writer]
  (.write writer (compiler/render-css color)))

#_
(defmethod print-method Rgb [color writer]
  (.write writer (compiler/render-css color)))

#_
(defmethod print-method Rgba [color writer]
  (.write writer (compiler/render-css color)))

#_
(defmethod print-method CSSAtRule [css-at-rule writer]
  (let [f (if (or (util/at-keyframes? css-at-rule)
                  (util/at-media? css-at-rule))
            compiler/compile-css
            compiler/render-css)]
    (.write writer (f css-at-rule))))

#_
(defmethod print-method CSSSelector [css-selector writer]
  (.write writer (selectors/css-selector css-selector)))
