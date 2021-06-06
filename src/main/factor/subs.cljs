(ns factor.subs
  (:require [re-frame.core :refer [reg-sub]]
            [factor.world :as world]))

(defn reg-all []
  (reg-sub :world-data (fn [db _] (get db :world)))

  (reg-sub :factory (fn [db [_ id]] (get-in db [:world :factories id])))
  (reg-sub :factory-calc (fn [db [_ id]] (let [world (get db :world)
                                               factory (get-in world [:factories id])]
                                           (world/satisfy-factory world factory))))
  (reg-sub :factory-ids (fn [db] (-> db (get-in [:world :factories]) (keys))))
  (reg-sub :factory-names (fn [db] (->> (get-in db [:world :factories])
                                     (map (fn [[k v]] [(:name v) k]))
                                     (into {}))))
  
  (reg-sub :item (fn [db [_ id]] (get-in db [:world :items id])))
  (reg-sub :item-ids (fn [db] (-> db (get-in [:world :items]) (keys))))
  (reg-sub :item-count (fn [db] (-> db (get-in [:world :items]) (count))))
  (reg-sub :item-names (fn [db] (->> (get-in db [:world :items])
                                     (map (fn [[k v]] [(:name v) k]))
                                     (into {}))))
  (reg-sub :item-seq (fn [db] (map (fn [[id v]] (assoc v :id id)) (get-in db [:world :items]))))

  (reg-sub :machine (fn [db [_ id]] (get-in db [:world :machines id])))
  (reg-sub :machine-ids (fn [db] (-> db (get-in [:world :machines]) (keys))))
  (reg-sub :machine-count (fn [db] (-> db (get-in [:world :machines]) (count))))
  (reg-sub :machine-names (fn [db] (->> (get-in db [:world :machines])
                                        (map (fn [[k v]] [(:name v) k]))
                                        (into {}))))
  (reg-sub :machine-seq (fn [db] (map (fn [[id v]] (assoc v :id id)) (get-in db [:world :machines])))))

  (reg-sub :recipe (fn [db [_ id]] (get-in db [:world :recipes id])))
  (reg-sub :recipe-ids (fn [db] (-> db (get-in [:world :recipes]) (keys))))
  (reg-sub :recipe-count (fn [db] (-> db (get-in [:world :recipes]) (count))))
  (reg-sub :recipe-names (fn [db] (->> (get-in db [:world :recipes])
                                     (map (fn [[k v]] [(:name v) k]))
                                     (into {}))))
  (reg-sub :recipe-is-expanded (fn [db [_ id]] (get-in db [:ui :recipes :expanded id] false)))
  (reg-sub :recipe-seq (fn [db] (map (fn [[id v]] (assoc v :id id)) (get-in db [:world :recipes]))))

  (reg-sub :selected-page (fn [db _] (get-in db [:ui :selected-page])))
  (reg-sub :current-selection (fn [db [_ type]]
                                (let [[t s] (get-in db [:ui :selection])]
                                  (if (= type t) s [])))))

