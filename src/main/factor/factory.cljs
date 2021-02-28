(ns factor.factory
  (:require [medley.core :refer [filter-vals map-vals dissoc-in filter-keys]]))

(defn factory []
  {:name "Unnamed Factory"
   :desired-output {}
   :input {}
   :output {}
   :machines {}
   :recipes {}})

(defn factory-references-item? [factory item-id]
  (some #(contains? (% factory) item-id) [:input :output :desired-output]))

(defn factories-with-item [factories item-id]
  (filter-vals #(factory-references-item? % item-id) factories))

(defn update-factories-for-item [factories item-id]
  (map-vals #(-> %
                 (dissoc-in [:input item-id])
                 (dissoc-in [:output item-id])
                 (dissoc-in [:desired-output item-id]))
            (factories-with-item factories item-id)))

(defn recipe-for-item [recipes item-id]
  (some (fn [[k {:keys [output]}]]
          (when (contains? output item-id) k)) recipes))


(defn apply-recipe [factory recipe-id recipe times]
  (let [input-change (map-vals #(* % times) (:input recipe))
        output-change (map-vals #(* % times) (:output recipe))
        machine-id (first (:machines recipe))]
   (cond-> factory
       input-change (update :input #(merge-with + % input-change))
       output-change (update :input #(merge-with + % (map-vals - output-change)))
       output-change (update :input #(filter-vals pos? %))
       output-change (update :output #(merge-with + % output-change))
       machine-id (update-in [:machines machine-id] + times)
       true (update-in [:recipes recipe-id] + times))))

(defn satisfy-desired-outputs [initial-factory {:keys [recipes]}]
  (letfn [(get-recipe-for [items] (some #(recipe-for-item recipes (% 0)) items))
          (satisfy [{:keys [output desired-output] :as factory}]
                   (let [output-gap (->> output
                                         (merge-with - desired-output)
                                         (filter-vals pos?))]
                     (if (empty? output-gap) factory
                         (let [next-recipe-id (get-recipe-for output-gap)
                               next-recipe (recipes next-recipe-id)
                               times (apply max (for [[k v] (:output next-recipe)]
                                                  (when (contains? output-gap k)
                                                    (/ (output-gap k) v))))
                               updated-factory (apply-recipe factory
                                                             next-recipe-id
                                                             next-recipe
                                                             times)]
                           (if (= updated-factory factory) factory
                             (satisfy updated-factory))))))]
    (satisfy initial-factory)))

(defn recalc-factory [db [_ factory-id]]
  (update-in db [:world :factories factory-id]
             #(satisfy-desired-outputs {:desired-output (:desired-output %)} (:world db))))

(defn factory-has-reference?
  [factory id-type id]
  (let [relevant-dicts (case id-type
                         :item [:input :output :desired-output]
                         :recipe [:recipes]
                         :machine [:machines])]
    (some #(contains? (% factory) id) relevant-dicts)))

(defn update-foreign-keys
  "Updates all foreign keys of the specified type in the factory object."
  [factory fk-type {:keys [items recipes machines]}]
  (cond-> factory
    (#{:all :item} fk-type)
    (-> (update :input #(filter-keys items %))
        (update :output #(filter-keys items %))
        (update :desired-output #(filter-keys items %)))
    (#{:all :recipe} fk-type) (update :recipes #(filter-keys recipes %))
    (#{:all :machine} fk-type) (update :machines #(filter-keys machines %))))

