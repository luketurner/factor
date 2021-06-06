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

(defn recipe [] {:name ""
                 :input {}
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

(defn unsatisfied-output [{:keys [output desired-output]}]
  (->> output (merge-with - desired-output) (filter-vals pos?)))

(defn satisfy-desired-machines [world factory]
  factory)

(defn recipe-satisfies? [recipe id-type id]
  (-> recipe (get id-type) (contains? id)))

(defn satisfying-recipe [world id-type id]
  (->> world
       (recipes)
       (some #(when (recipe-satisfies? (second %) id-type id) %))))

(defn satisfying-recipe-for-set [world id-type ids]
  (some #(satisfying-recipe world id-type %) ids))

(defn satisfying-ratio [goal base]
  (apply max (for [[k v] base]
               (when (contains? goal k)
                 (/ (goal k) v)))))

(defn satisfy-desired-output [world factory]
  (loop [satisfied-factory factory
         unsatisfied (unsatisfied-output factory)]
    (if (empty? unsatisfied)
      satisfied-factory
      (if-let [[next-recipe-id, next-recipe] (->> unsatisfied (map first) (satisfying-recipe-for-set world :output))]
        (let [times (->> next-recipe (:output) (satisfying-ratio unsatisfied))
              updated-factory (apply-recipe factory next-recipe-id next-recipe times)]
          (recur
           updated-factory
           (unsatisfied-output updated-factory)))
        satisfied-factory))))

(defn satisfy-desired-input [world factory]
  factory)

(defn satisfy-factory [world factory]
  (->> factory
       (satisfy-desired-machines world)
       (satisfy-desired-input world)
       (satisfy-desired-output world)))