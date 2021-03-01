(ns factor.world
  (:require [clojure.edn :as edn]
            [factor.util :refer [new-uuid add-fx]]
            [re-frame.core :refer [->interceptor dispatch-sync]]
            [medley.core :refer [filter-vals map-vals dissoc-in filter-keys]]))

(def empty-world {:items {} :machines {} :recipes {} :factories {}})

(defn factory []
  {:name "Unnamed Factory"
   :desired-output {}
   :input {}
   :output {}
   :machines {}
   :recipes {}})

(defn machine [] {:name ""
                  :power 0
                  :speed 1})

(defn recipe [] {:input {}
                 :output {}
                 :machines #{}})

(defn item [] {:name ""})

(defn world->str [world] (pr-str world))
(defn str->world [s] (edn/read-string s))

(defn update-world [db & rest]
  (apply (partial update db :world) rest))

(defn factories [world] (get world :factories))
(defn machines [world] (get world :machines))
(defn recipes [world] (get world :recipes))
(defn items [world] (get world :items))

(defn factory-ids [world] (-> world (factories) (keys)))
(defn machine-ids [world] (-> world (machines) (keys)))
(defn recipe-ids [world] (-> world (recipes) (keys)))
(defn item-ids [world] (-> world (items) (keys)))

(defn with-factory [world id factory] (update world :factories assoc (or id (new-uuid)) factory))
(defn with-machine [world id machine] (update world :machines assoc (or id (new-uuid)) machine))
(defn with-item [world id item] (update world :items assoc (or id (new-uuid)) item))
(defn with-recipe [world id recipe] (update world :recipes assoc (or id (new-uuid)) recipe))

(defn without-factory [world id] (update world :factories dissoc id))
(defn without-machine [world id] (update world :machines dissoc id))
(defn without-item [world id] (update world :items dissoc id))
(defn without-recipe [world id] (update world :recipes dissoc id))

(defn factory-has-reference?
  [factory id-type id]
  (let [relevant-dicts (case id-type
                         :item [:input :output :desired-output]
                         :recipe [:recipes]
                         :machine [:machines])]
    (some #(contains? (% factory) id) relevant-dicts)))

(defn update-factory-foreign-keys
  "Updates all foreign keys of the specified type in the factory object."
  [factory fk-type {:keys [items recipes machines]}]
  (cond-> factory
    (#{:all :item} fk-type)
    (-> (update :input #(filter-keys items %))
        (update :output #(filter-keys items %))
        (update :desired-output #(filter-keys items %)))
    (#{:all :recipe} fk-type) (update :recipes #(filter-keys recipes %))
    (#{:all :machine} fk-type) (update :machines #(filter-keys machines %))))

(defn recipe-has-reference?
  [recipe id-type id]
  (let [relevant-dicts (case id-type
                         :item [:input :output]
                         :machine [:machines])]
    (some #(contains? (% recipe) id) relevant-dicts)))

(defn update-recipe-foreign-keys
  "Updates all foreign keys of the specified type in the recipe object."
  [recipe fk-type {:keys [items machines]}]
  (cond-> recipe
    (#{:all :item} fk-type)
    (-> (update :input #(filter-keys items %))
        (update :output #(filter-keys items %)))
    (#{:all :machine} fk-type) (update :recipes #(filter-keys machines %))))

(defn ->saver []
  (->interceptor
   :id :world-saver
   :after (fn [{{{old-world :world} :db} :coeffects
                {{new-world :world} :db} :effects :as context}]
            (if (and new-world (not= new-world old-world))
              (add-fx context [:dispatch [:save-world new-world]])
              context))))

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

(defn satisfy-factory [world factory]
  (satisfy-desired-outputs {:desired-output (:desired-output factory)
                             :name (:name factory)} 
                           world))
