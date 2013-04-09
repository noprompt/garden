(ns garden.media
  (:require [garden.util :refer :all]
            [garden.units :as unit]
            ))


;; See: http://www.w3.org/TR/css3-mediaqueries/#media1
;; Feature, accepted value type or values, accept min/max prefixes
(def media-features
  {:width [unit/length? true]
   :height [unit/length? true]
   :device-width [unit/length? true]
   :device-height [unit/length? true]
   :orientation [#{:portrait :landscape} false]
   :aspect-ratio [ratio? true]
   :device-aspect-ratio [ratio? true]
   :color [natural? true]
   :color-index [natural? true]
   :monochrome [natural? true]
   :resolution [unit/resolution? true]
   :scan [#{:progressive :interlace} false]
   :grid [natural? false]})

(def media-feature-names
  (letfn [(prefix-keyword [k p]
            (keyword (as-str  p \-  k)))
          (extract-names [coll [feature [_ prefix?]]]
            (if prefix?
              (->> (map (partial prefix-keyword feature) [:min :max])
                   (cons feature)
                   (into coll))
              (conj coll feature)))]
    (reduce extract-names #{} media-features)))

(def media-feature-types
  #{:all :braille :embossed :handheld :print :projection :screen :speech :tty :tv})
