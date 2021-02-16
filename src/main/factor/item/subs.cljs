(ns factor.item.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :item (fn [db [_ id]] (get-in db [:world :items id])))
(reg-sub :item-ids (fn [db] (-> db (get-in [:world :items]) (keys))))
(reg-sub :item-count (fn [db] (-> db (get-in [:world :items]) (count))))
(reg-sub :item-names (fn [db] (->> (get-in db [:world :items])
                                   (map (fn [[k v]] [(:name v) k]))
                                   (into {}))))