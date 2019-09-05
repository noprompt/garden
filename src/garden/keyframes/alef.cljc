(ns garden.keyframes.alef
  (:require [clojure.spec :as spec]
            [garden.ast.alef]
            [garden.parse.alef]))

(spec/def ::name keyword?)

(spec/def ::frame-selector
  (spec/or ::from-to-frame-selector #{:from :to}
           ::percentage-frame-selector number?))

(spec/def ::frames
  (spec/map-of ::frame-selector map?))

(spec/def ::keyframes-rule
  (spec/keys :req-un [::name ::frames]))

(spec/def ::rule-children
  (spec/*
   (spec/and
    vector?
    (spec/cat ::frame-selector ::frame-selector
              ::declarations (spec/* :garden.parse.alef/declaration-block)))))

(defrecord KeyframesRule [name frames])

(defn normalize-frame-map
  {:private true}
  [frame-map]
  (cond-> frame-map
    (:from frame-map)
    (assoc 0 (:from frame-map))

    (:to frame-map)
    (assoc 100 (:to frame-map))

    :always
    (dissoc :from :to)))

(defn rule [name & children]
  (spec/assert ::rule-children children)
  (let [frame-map (reduce
                   (fn [m [frame-selector & frame-children]]
                     (assoc m frame-selector (vec frame-children)))
                   {}
                   children)]
    (map->KeyframesRule
     {:name name
      :frames frame-map})))

(defn parse-keyframes-rule [keyframes-rule]
  (let [{:keys [frames name]} keyframes-rule
        normalized-frames (into (sorted-map)
                                (normalize-frame-map frames))]
    [:css/keyframes
     [:css.keyframes/name (clojure.core/name name)]
     (into [:css.keyframes/block]
           (for [[n declaration-maps] normalized-frames]
             [:css.keyframes/rule
              [:css.selector/keyframe
               [:css/percentage n]]
              (into
               [:css.declaration/block]
               (comp (map garden.parse.alef/parse)
                     (mapcat garden.ast.alef/children))
               declaration-maps)]))]))


(extend-protocol garden.parse.alef/IParse
  KeyframesRule
  (-parse [kr]
    (parse-keyframes-rule kr)))
