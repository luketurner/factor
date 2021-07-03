(ns factor.subs
  (:require [re-frame.core :refer [reg-sub]]
            [factor.world :as world]))

(defn reg-all []
  (reg-sub :world-data (fn [db _] (get db :world)))

  (reg-sub :factory (fn [db [_ id]] (get-in db [:world :factories id])))
  (reg-sub :factory-satisfied (fn [db [_ id]] (let [world (get db :world)
                                                    factory (get-in world [:factories id])]
                                                (world/satisfy-factory world factory))))
  (reg-sub :factory-ids (fn [db] (-> db (get-in [:world :factories]) (keys) (->> (into [])))))
  (reg-sub :factory-names (fn [db] (->> (get-in db [:world :factories])
                                     (map (fn [[k v]] [(:name v) k]))
                                     (into {}))))
  
  (reg-sub :item (fn [db [_ id]] (get-in db [:world :items id])))
  (reg-sub :item-ids (fn [db] (-> db (get-in [:world :items]) (keys) (->> (into [])))))
  (reg-sub :item-count (fn [db] (-> db (get-in [:world :items]) (count))))
  (reg-sub :item-names (fn [db] (->> (get-in db [:world :items])
                                     (map (fn [[k v]] [(:name v) k]))
                                     (into {}))))
  (reg-sub :item-seq (fn [db] (into [] (map (fn [[id v]] (assoc v :id id)) (get-in db [:world :items])))))

  (reg-sub :machine (fn [db [_ id]] (get-in db [:world :machines id])))
  (reg-sub :machine-ids (fn [db] (-> db (get-in [:world :machines]) (keys) (->> (into [])))))
  (reg-sub :machine-count (fn [db] (-> db (get-in [:world :machines]) (count))))
  (reg-sub :machine-names (fn [db] (->> (get-in db [:world :machines])
                                        (map (fn [[k v]] [(:name v) k]))
                                        (into {}))))
  (reg-sub :machine-seq (fn [db] (into [] (map (fn [[id v]] (assoc v :id id)) (get-in db [:world :machines])))))

  (reg-sub :recipe (fn [db [_ id]] (get-in db [:world :recipes id])))
  (reg-sub :recipe-ids (fn [db] (-> db (get-in [:world :recipes]) (keys) (->> (into [])))))
  (reg-sub :recipe-count (fn [db] (-> db (get-in [:world :recipes]) (count))))
  (reg-sub :recipe-names (fn [db] (->> (get-in db [:world :recipes])
                                     (map (fn [[k v]] [(:name v) k]))
                                     (into {}))))
  (reg-sub :recipe-seq (fn [db] (into [] (map (fn [[id v]] (assoc v :id id)) (get-in db [:world :recipes])))))

  (reg-sub :ui (fn [db [_ path]] (get-in db (into [:ui] path))))

  (reg-sub :open-factory (fn [db _] (get-in db [:config :open-factory]))))

