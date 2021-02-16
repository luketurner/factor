(ns factor.machine.events
  (:require [re-frame.core :refer [reg-event-db]]
            [factor.machine :refer [machine]]))

(reg-event-db :create-machine (fn [db] (assoc-in db [:world :machines (random-uuid)] (machine))))
(reg-event-db :update-machine (fn [db [_ id v]] (assoc-in db [:world :machines id] v)))
(reg-event-db :delete-machine (fn [db [_ id]] (update-in db [:world :machines] dissoc id)))
