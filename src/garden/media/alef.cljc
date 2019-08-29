(ns garden.media.alef
  "Utility functions for working with media queries."
  (:require [clojure.core :as clj]
            [clojure.spec :as spec]
            [garden.parse.alef]))

(def media-types
  #{:all
    :aural
    :braille
    :embossed
    :handheld
    :print
    :projection
    :screen
    :speech
    :tty
    :tv})

;; See: http://www.w3.org/TR/css3-mediaqueries/#media1
(def media-features
  #{:aspect-ratio
    :color
    :color-index
    :device-height
    :device-width
    :grid
    :height
    :max-aspect-ratio
    :max-color
    :max-color-index
    :max-device-height
    :max-device-width
    :max-height
    :max-monochrome
    :max-resolution
    :max-width
    :min-aspect-ratio
    :min-color
    :min-color-index
    :min-device-height
    :min-device-width
    :min-height
    :min-monochrome
    :min-resolution
    :min-width
    :monochrome
    :orientation
    :resolution
    :scan
    :width})

(defn media-type?
  [x]
  (contains? media-types x))

(spec/def ::constraint
  #{:not :only})

(spec/def ::type
  media-types)

(spec/def ::features
  (spec/map-of keyword?
               (complement (some-fn coll? seq?)))) 

(spec/def ::query
  (spec/keys :opt-un [::constraint ::type ::features]))

(spec/def ::query-list
  (spec/coll-of ::query))


;; ---------------------------------------------------------------------
;; MediaQuery

(defrecord MediaQuery [constraint type features])

(defn media-query?
  "true if `x` is an instance of `MediaQuery`, false otherwise."
  [x]
  (instance? MediaQuery x))

(defn valid-media-query?
  "true if `x` is an instance of `MediaQuery` and , false otherwise."
  [x])


;; ---------------------------------------------------------------------
;; MediaRule

(defrecord MediaRule [query-list rules])

(defn media-rule?
  "true if `x` is an instance of `MediaRule`, false otherwise."
  [x]
  (instance? MediaRule x))

;; ---------------------------------------------------------------------
;; Media type construction

(spec/def ::query
  (spec/cat :constraint (spec/? #{:not :only})
            :type (spec/? media-types)
            :features (spec/? map?)))

(defn parse-query-arguments
  "Parses arguments passed to `query` using `clojure.spec/conform`
  providing defaults."
  {:private true}
  [v]
  (spec/conform ::query v))

(defn
  query
  {:arglists '([media-type]
               [feature-map]
               [constraint media-type]
               [constraint feature-map]
               [media-type feature-map]
               [constraint media-type feature-map])}
  ([x]
   {:pre [(or (media-type? x)
              (map? x))]}
   (let [m (parse-query-arguments [x])]
     (map->MediaQuery m)))
  ([x y]
   {:pre [(or (and (#{:not :only} x)
                   (media-type? y))
              (and (#{:not :only} x)
                   (map? y))
              (and (media-type? x)
                   (map? y)))]}
   (let [m (parse-query-arguments [x y])]
     (map->MediaQuery m)))
  ([x y z]
   {:pre [(#{:not :only} x)
          (media-type? y)
          (map? z)]}
   (let [m (parse-query-arguments [x y z])]
     (map->MediaQuery m))))

(defn rule
  [media-query-or-queries & body]
  {:pre [(or (media-query? media-query-or-queries)
             (and (sequential? media-query-or-queries)
                  (every? media-query? media-query-or-queries)))]}
  (let [media-queries (cond-> media-query-or-queries
                        (clj/not (sequential? media-query-or-queries))
                        vector)]
    (map->MediaRule
     {:media media-queries
      :rules (vec body)})))

;; ---------------------------------------------------------------------
;; IParse implementation

(defn parse-media-constraint
  {:private true}
  [media-query]
  (if-let [constraint (:constraint media-query)]
    [:css.media.query/constraint
     (name constraint)]
    [:css/noop]))

(defn parse-media-type
  {:private true}
  [media-query]
  (if-let [type (:type media-query)]
    [:css.media.query/type (name type)]
    [:css/noop]))

(defn parse-media-features
  {:private true}
  [media-query]
  (if-let [media-features (seq (:features media-query))]
    (let [expression-nodes
          (for [[feature value] media-features]
            (if (some? value)
              [:css.media.query/expression
               [:css.media.query/feature feature]
               [:css.media.query/value value]]
              [:css.media.query/expression
               [:css.media.query/feature feature]]))]
      (into [:css.media.query/conjunction]
            expression-nodes))
    [:css.media.query/conjunction]))

(extend-protocol garden.parse.alef/IParse
  MediaQuery
  (-parse [media-query]
    (let [constraint-node (parse-media-constraint media-query)
          media-type-node (parse-media-type media-query)
          conjunction-node (parse-media-features media-query)]
      [:css.media/query
       constraint-node
       media-type-node
       conjunction-node]))
  
  MediaRule
  (-parse [media-rule]
    (into
     [:css.media/rule
      (into
       [:css.media/query-list]
       (map garden.parse.alef/parse (:media media-rule)))]
     (map garden.parse.alef/parse (:rules media-rule)))))
