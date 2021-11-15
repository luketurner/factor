(ns factor.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [medley.core :refer [map-vals]]
            [factor.world :as world]
            [factor.pgraph :as pgraph]))

(defn reg-all []

  ;; Extractors

  ; only use this if you really need the whole world (e.g. exporting/saving)
  (reg-sub :world-data (fn [db _] (get db :world)))

  (reg-sub :factory (fn [db [_ id]] (get-in db [:world :factories id])))
  (reg-sub :item (fn [db [_ id]] (get-in db [:world :items id])))
  (reg-sub :machine (fn [db [_ id]] (get-in db [:world :machines id])))
  (reg-sub :recipe (fn [db [_ id]] (get-in db [:world :recipes id])))

  (reg-sub :factories (fn [db] (get-in db [:world :factories])))
  (reg-sub :items (fn [db] (get-in db [:world :items])))
  (reg-sub :machines (fn [db] (get-in db [:world :machines])))
  (reg-sub :recipes (fn [db] (get-in db [:world :recipes])))

  (reg-sub :ui (fn [db [_ path]] (get-in db (into [:ui] path))))

  (reg-sub :open-factory (fn [db _] (get-in db [:config :open-factory])))
  (reg-sub :unit (fn [db [_ k]] (get-in db [:config :unit k])))

  ;; Materialized views

  (reg-sub :machine-seq :<- [:machines] (fn [m] (into [] (vals m))))
  (reg-sub :factory-seq :<- [:factories] (fn [m] (into [] (vals m))))
  (reg-sub :recipe-seq :<- [:recipes] (fn [m] (into [] (vals m))))
  (reg-sub :item-seq :<- [:items] (fn [m] (into [] (vals m))))

  (reg-sub :machine-ids :<- [:machine-seq] (fn [m] (into [] (keys m))))
  (reg-sub :factory-ids :<- [:factory-seq] (fn [m] (into [] (keys m))))
  (reg-sub :recipe-ids :<- [:recipe-seq] (fn [m] (into [] (keys m))))
  (reg-sub :item-ids :<- [:item-seq] (fn [m] (into [] (keys m))))

  (reg-sub :item-ids->names :<- [:items] (fn [m] (map-vals :name m)))
  (reg-sub :recipe-ids->names :<- [:recipes] (fn [m] (map-vals :name m)))
  (reg-sub :machine-ids->names :<- [:machines] (fn [m] (map-vals :name m)))
  (reg-sub :factory-ids->names :<- [:factories] (fn [m] (map-vals :name m)))

  (reg-sub :item-count :<- [:item-seq] (fn [m] (count m)))
  (reg-sub :recipe-count :<- [:recipe-seq] (fn [m] (count m)))
  (reg-sub :factory-count :<- [:factory-seq] (fn [m] (count m)))
  (reg-sub :machine-count :<- [:machine-seq] (fn [m] (count m)))

  ; These subs exclude fields that don't matter for pgraph processing.
  ; This way, if e.g. a factory name changes, the whole pgraph doesn't recalculate.
  (reg-sub :factory-for-pgraph (fn [[_ id]] (subscribe [:factory id])) (fn [f] (dissoc f :name :id)))
  (reg-sub :recipes-for-pgraph :<- [:recipes] (fn [m] (map-vals #(dissoc % :name) m)))
  (reg-sub :machines-for-pgraph :<- [:machines] (fn [m] (map-vals #(dissoc % :name) m)))


  ;; :hard-denied-items (:hard-denied-items factory)
  ;; :soft-denied-items (:hard-denied-items factory)
  ;; :hard-denied-machines (:hard-denied-machines factory)
  ;; :soft-denied-machines (:hard-denied-machines factory)
  ;; :hard-denied-recipes (:hard-denied-recipes factory)
  ;; :soft-denied-recipes (:hard-denied-recipes factory)
  ;; :hard-allowed-items (:hard-denied-items factory)
  ;; :soft-allowed-items (:hard-denied-items factory)
  ;; :hard-allowed-machines (:hard-denied-machines factory)
  ;; :soft-allowed-machines (:hard-denied-machines factory)
  ;; :hard-allowed-recipes (:hard-denied-recipes factory)
  ;; :soft-allowed-recipes (:hard-denied-recipes factory)

  (reg-sub :factory-pgraph
           (fn [[_ id]] {:recipes (subscribe [:recipes-for-pgraph])
                         :machines (subscribe [:machines-for-pgraph])
                         :factory (subscribe [:factory-for-pgraph id])})
           (fn [{:keys [recipes machines factory]}]
             (pgraph/pgraph {:recipes recipes
                             :machines machines
                             :filter (:filter factory)
                             :desired-output (:desired-output factory)}))))

