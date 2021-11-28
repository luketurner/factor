(ns factor.world
  (:require [clojure.edn :as edn]
            [factor.util :refer [new-uuid dissoc-in]]
            [medley.core :refer [map-vals]]
            [factor.schema :as sc]
            [com.rpl.specter :as s]))

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

(defn machine-for-factory-recipe 
  "Picks which machine should be used to craft given `recipe`. Respects the machine
   constraints assigned by `factory`, if any. Returns nil if there are no matching machines."
  [factory recipe]
  (s/select-first
   [(s/keypath :machines)] ; Add filtering logic here when implemented in factory
   recipe))

(defn recipe-index
  [recipes]
  (reduce
   (fn [m x]
     (-> m
         (update :output (partial merge-with concat) (map-vals #(identity #{(:id x)}) (:output x)))
         (update :input  (partial merge-with concat) (map-vals #(identity #{(:id x)}) (:input  x)))))
   {:input {} :output {}}
   recipes))