(ns factor.filter
  (:require [factor.util :refer [is-denied?]]))

(defn machine-id-hard-denied?
  [{:keys [hard-denied-machines hard-allowed-machines]} m]
  (is-denied? m hard-denied-machines hard-allowed-machines))

(defn machine-id-soft-denied?
  [{:keys [soft-denied-machines soft-allowed-machines]} m]
  (is-denied? m soft-denied-machines soft-allowed-machines))

(defn item-id-hard-denied?
  [{:keys [hard-denied-items hard-allowed-items]} i]
  (is-denied? i hard-denied-items hard-allowed-items))

(defn item-id-soft-denied?
  [{:keys [soft-denied-items soft-allowed-items]} i]
  (is-denied? i soft-denied-items soft-allowed-items))

(defn recipe-id-hard-denied?
  [{:keys [hard-denied-recipes hard-allowed-recipes]} r]
  (is-denied? r hard-denied-recipes hard-allowed-recipes))

(defn recipe-id-soft-denied?
  [{:keys [soft-denied-recipes soft-allowed-recipes]} r]
  (is-denied? r soft-denied-recipes soft-allowed-recipes))

(defn recipe-hard-denied?
  [f r]
  (let [item-id-hard-denied? (partial item-id-hard-denied? f)
        machine-id-hard-denied? (partial machine-id-hard-denied? f)]
    (or (recipe-id-hard-denied? f (:id r))
        (empty? (filter #(not (machine-id-hard-denied? %)) (:machines r)))
        (some item-id-hard-denied? (keys (:input r)))
        (some item-id-hard-denied? (keys (:output r)))
        (some item-id-hard-denied? (keys (:catalysts r))))))

(defn recipe-soft-denied?
  [f r]
  (let [item-id-soft-denied? (partial item-id-soft-denied? f)
        machine-id-soft-denied? (partial machine-id-soft-denied? f)]
    (or (recipe-id-soft-denied? f (:id r))
        (empty? (filter #(not (machine-id-soft-denied? %)) (:machines r)))
        (some item-id-soft-denied? (keys (:input r)))
        (some item-id-soft-denied? (keys (:output r)))
        (some item-id-soft-denied? (keys (:catalysts r))))))

