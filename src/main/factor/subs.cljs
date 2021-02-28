(ns factor.subs
  (:require [re-frame.core :refer [reg-sub]]))

(defn reg-all []
  (reg-sub :world-data (fn [db _] (get db :world)))
  (reg-sub :factory (fn [db [_ id]] (get-in db [:world :factories id])))
  (reg-sub :factory-ids (fn [db] (-> db (get-in [:world :factories]) (keys))))
  (reg-sub :item (fn [db [_ id]] (get-in db [:world :items id])))
  (reg-sub :item-ids (fn [db] (-> db (get-in [:world :items]) (keys))))
  (reg-sub :item-count (fn [db] (-> db (get-in [:world :items]) (count))))
  (reg-sub :item-names (fn [db] (->> (get-in db [:world :items])
                                     (map (fn [[k v]] [(:name v) k]))
                                     (into {}))))
  (reg-sub :machine (fn [db [_ id]] (get-in db [:world :machines id])))
  (reg-sub :machine-ids (fn [db] (-> db (get-in [:world :machines]) (keys))))
  (reg-sub :machine-count (fn [db] (-> db (get-in [:world :machines]) (count))))
  (reg-sub :machine-names (fn [db] (->> (get-in db [:world :machines])
                                        (map (fn [[k v]] [(:name v) k]))
                                        (into {}))))
  (reg-sub :recipe (fn [db [_ id]] (get-in db [:world :recipes id])))
  (reg-sub :recipe-ids (fn [db] (-> db (get-in [:world :recipes]) (keys))))
  (reg-sub :recipe-count (fn [db] (-> db (get-in [:world :recipes]) (count))))
  (reg-sub :recipe-is-expanded (fn [db [_ id]] (get-in db [:ui :recipes :expanded id] false)))
  (reg-sub :selected-page (fn [db _] (get-in db [:ui :selected-page]))))

