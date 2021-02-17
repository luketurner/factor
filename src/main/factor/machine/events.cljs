(ns factor.machine.events
  (:require [re-frame.core :refer [reg-event-db]]
            [factor.machine :refer [machine]]
            [factor.util :refer [new-uuid dispatch-after]]))

(reg-event-db :create-machine (fn [db] (assoc-in db [:world :machines (new-uuid)] (machine))))
(reg-event-db :update-machine 
              (fn [db [_ id v]] (assoc-in db [:world :machines id] v)))
(reg-event-db :delete-machine
              [(dispatch-after (fn [[_ id]] [:update-recipes-with-fk :item id]))
               (dispatch-after (fn [[_ id]] [:update-factories-with-fk :item id]))]
              (fn [db [_ id]] (update-in db [:world :machines] dissoc id)))
