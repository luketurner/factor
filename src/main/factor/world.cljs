(ns factor.world
  (:require [clojure.edn :as edn]
            [factor.util :refer [new-uuid dissoc-in]]
            [medley.core :refer [map-vals]]))

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
  (-> factory
      (dissoc-in [:machines] machine-id)))

(defn factory-without-recipe [factory recipe-id world]
  (-> factory
      (dissoc-in [:recipes] recipe-id)))

(defn factory-without-item [factory item-id world]
  (-> factory
      (dissoc-in [:desired-output] item-id)))

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
                  :name "Unnamed item"
                  :created-at (.now js/Date)} opts)))

(defn new-machine 
  ([] (new-machine {}))
  ([opts] (merge {:id (new-uuid)
                  :name "Unnamed machine"
                  :power 0
                  :speed 1
                  :created-at (.now js/Date)} opts)))

(defn new-recipe
  ([] (new-recipe {}))
  ([opts] (merge {:id (new-uuid)
                  :name "Unnamed recipe"
                  :input {}
                  :output {}
                  :catalysts {}
                  :machines #{}
                  :duration 1
                  :created-at (.now js/Date)} opts)))

(defn world->str [world] (pr-str world))
(defn str->world [s] (edn/read-string s))
