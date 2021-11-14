(ns factor.factory
  (:require [factor.util :refer [is-denied? pick-max]]))

(defn machine-id-hard-denied?
  [{{:keys [hard-denied-machines hard-allowed-machines]} :factory} m]
  (is-denied? m hard-denied-machines hard-allowed-machines))

(defn machine-id-soft-denied?
  [{{:keys [soft-denied-machines soft-allowed-machines]} :factory} m]
  (is-denied? m soft-denied-machines soft-allowed-machines))

(defn item-id-hard-denied?
  [{{:keys [hard-denied-items hard-allowed-items]} :factory} i]
  (is-denied? i hard-denied-items hard-allowed-items))

(defn item-id-soft-denied?
  [{{:keys [soft-denied-items soft-allowed-items]} :factory} i]
  (is-denied? i soft-denied-items soft-allowed-items))

(defn recipe-id-hard-denied?
  [{{:keys [hard-denied-recipes hard-allowed-recipes]} :factory} r]
  (is-denied? r hard-denied-recipes hard-allowed-recipes))

(defn recipe-id-soft-denied?
  [{{:keys [soft-denied-recipes soft-allowed-recipes]} :factory} r]
  (is-denied? r soft-denied-recipes soft-allowed-recipes))

(defn recipe-hard-denied?
  [pg r]
  (let [item-id-hard-denied? (partial item-id-hard-denied? pg)
        machine-id-hard-denied? (partial machine-id-hard-denied? pg)]
    (or (recipe-id-hard-denied? pg (:id r))
        (empty? (filter #(not (machine-id-hard-denied? %)) (:machines r)))
        (some item-id-hard-denied? (keys (:input r)))
        (some item-id-hard-denied? (keys (:output r)))
        (some item-id-hard-denied? (keys (:catalysts r))))))

(defn recipe-soft-denied?
  [pg r]
  (let [item-id-soft-denied? (partial item-id-soft-denied? pg)
        machine-id-soft-denied? (partial machine-id-soft-denied? pg)]
    (or (recipe-id-soft-denied? pg (:id r))
        (empty? (filter #(not (machine-id-soft-denied? %)) (:machines r)))
        (some item-id-soft-denied? (keys (:input r)))
        (some item-id-soft-denied? (keys (:output r)))
        (some item-id-soft-denied? (keys (:catalysts r))))))

(defn preferred-machine
  "Given a recipe, returns an ID for one of the machines the recipe supports. Respects factory allow/deny lists. Picks the highest-speed machine."
  [pg recipe]
  (let [{:keys [factory]} pg
        {:keys [soft-denied not-denied]} (->> (:machines recipe)
                                              (filter #(not (machine-id-hard-denied? factory %)))
                                              (group-by #(if (machine-id-soft-denied? pg %)
                                                           :soft-denied
                                                           :not-denied)))]
    (->> (if (seq not-denied) not-denied soft-denied)
         (pick-max :speed))))