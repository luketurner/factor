(ns factor.machine.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :machine (fn [db [_ id]] (get-in db [:world :machines id])))
(reg-sub :machine-ids (fn [db] (-> db (get-in [:world :machines]) (keys))))
(reg-sub :machine-count (fn [db] (-> db (get-in [:world :machines]) (count))))
(reg-sub :machine-names (fn [db] (->> (get-in db [:world :machines])
                                      (map (fn [[k v]] [(:name v) k]))
                                      (into {}))))
