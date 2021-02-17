(ns factor.recipe
  (:require [re-frame.core :refer [enrich]]
            [medley.core :refer [filter-keys map-kv-vals]]))

(defn recipe [] {:input {}
                 :output {}
                 :machines #{}})

;; (defn factory-references-recipe? [factory recipe-id]
;;   (some #(contains? (% factory) recipe-id) [:recipes]))

;; (defn factories-with-recipe [factories recipe-id]
;;   (filter-vals factories #(factory-references-recipe? % recipe-id)))

;; (defn update-factories-for-recipe [factories recipe-id]
;;   (map-vals (factories-with-recipe factories recipe-id)
;;             #(-> %
;;                  (dissoc-in [:recipes recipe-id]))))
;;                  
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



