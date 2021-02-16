(ns factor.factory.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :factory (fn [db [_ id]] (get-in db [:world :factories id])))
(reg-sub :factory-ids (fn [db] (-> db (get-in [:world :factories]) (keys))))
