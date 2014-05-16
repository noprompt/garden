(ns garden.media
  "Utility functions for working with media queries.")

;; See: http://www.w3.org/TR/css3-mediaqueries/#media1
(def media-features
  #{:all
    :aspect-ratio :min-aspect-ratio :max-aspect-ratio
    :braille
    :color :min-color :max-color
    :color-index :min-color-index :max-color-index
    :device-height :min-device-height :max-device-height
    :device-width :min-device-width :max-device-width
    :embossed
    :grid
    :handheld
    :height :min-height :max-height
    :monochrome :min-monochrome :max-monochrome
    :orientation
    :print
    :projection
    :resolution :min-resolution :max-resolution
    :scan
    :screen
    :speech
    :tty
    :tv
    :width :min-width :max-width})
