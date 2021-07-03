(ns factor.world
  (:require [clojure.edn :as edn]
            [factor.util :refer [new-uuid dissoc-in]]
            [medley.core :refer [filter-vals map-vals]]))

(def empty-world {:items {} :machines {} :recipes {} :factories {}})

(defn update-item [world item]
  (-> world
      (assoc-in [:items (:id item)] item)))

(defn update-machine [world machine]
  (-> world
      (assoc-in [:machines (:id machine)] machine)))

(defn update-recipe [world recipe]
  (-> world
      (assoc-in [:recipes (:id recipe)] recipe)))



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
       (:recipes)
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


(defn update-factory [world factory]
  (-> world
      (assoc-in [:factories (:id factory)] factory)))


(defn recipe-without-item [recipe item-id]
  (-> recipe
      (dissoc-in [:input] item-id)
      (dissoc-in [:output] item-id)))

(defn recipe-without-machine [recipe machine-id]
  (-> recipe
      (dissoc-in [:machines] machine-id)))

(defn factory-without-machine [factory machine-id world]
  (let [updated-factory (-> factory
                            (dissoc-in [:machines] machine-id))]
    (if (= factory updated-factory) factory (satisfy-factory world factory))))

(defn factory-without-recipe [factory recipe-id world]
  (let [updated-factory (-> factory
                            (dissoc-in [:recipes] recipe-id))]
    (if (= factory updated-factory) factory (satisfy-factory world factory))))

(defn factory-without-item [factory item-id world]
  (let [updated-factory (-> factory
                            (dissoc-in [:desired-output] item-id))]
    (if (= factory updated-factory) factory (satisfy-factory world factory))))




(defn remove-factory-by-id [world id]
  (-> world
      (dissoc-in [:factories] id)))

(defn remove-item-by-id [world id]
  (-> world
      (update :recipes   (partial map-vals #(recipe-without-item  % id)))
      (update :factories (partial map-vals #(factory-without-item % id world)))
      (dissoc-in [:items] id)))

(defn remove-machine-by-id [world id]
  (-> world
      (update :recipes   (partial map-vals #(recipe-without-machine  % id)))
      (update :factories (partial map-vals #(factory-without-machine % id world)))
      (dissoc-in [:machines] id)))

(defn remove-recipe-by-id [world id]
  (-> world
      (update :factories (partial map-vals #(factory-without-recipe % id world)))
      (dissoc-in [:recipes] id)))



(defn new-factory
  ([] (new-factory {}))
  ([opts] (merge {:id (new-uuid)
                  :name "Unnamed factory"
                  :desired-output {}
                  :input {}
                  :output {}
                  :machines {}
                  :recipes {}} opts)))

(defn new-item
  ([] (new-item {}))
  ([opts] (merge {:id (new-uuid)
                  :name "Unnamed item"} opts)))


(defn new-machine 
  ([] (new-machine {}))
  ([opts] (merge {:id (new-uuid)
                  :name "Unnamed machine"
                  :power 0
                  :speed 1} opts)))

(defn new-recipe
  ([] (new-recipe {}))
  ([opts] (merge {:id (new-uuid)
                  :name "Unnamed recipe"
                  :input {}
                  :output {}
                  :machines #{}} opts)))


(defn world->str [world] (pr-str world))
(defn str->world [s] (edn/read-string s))
