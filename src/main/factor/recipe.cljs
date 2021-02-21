(ns factor.recipe
  (:require [medley.core :refer [filter-keys]]))

(defn recipe [] {:input {}
                 :output {}
                 :machines #{}})
            
(defn recipe-has-reference?
  [recipe id-type id]
  (let [relevant-dicts (case id-type
                         :item [:input :output]
                         :machine [:machines])]
    (some #(contains? (% recipe) id) relevant-dicts)))

(defn update-foreign-keys
  "Updates all foreign keys of the specified type in the recipe object."
  [recipe fk-type {:keys [items machines]}]
  (cond-> recipe
    (#{:all :item} fk-type)
    (-> (update :input #(filter-keys items %))
        (update :output #(filter-keys items %)))
    (#{:all :machine} fk-type) (update :recipes #(filter-keys machines %))))



