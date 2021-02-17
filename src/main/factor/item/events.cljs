(ns factor.item.events
  (:require [re-frame.core :refer [reg-event-db]]
            [factor.item :refer [item]]
            [factor.util :refer [new-uuid dispatch-after]]))

(reg-event-db :create-item (fn [db] (assoc-in db [:world :items (new-uuid)] (item))))
(reg-event-db :update-item (fn [db [_ id v]] (assoc-in db [:world :items id] v)))
(reg-event-db :delete-item
              [(dispatch-after (fn [[_ id]] [:update-recipes-with-fk :item id]))
               (dispatch-after (fn [[_ id]] [:update-factories-with-fk :item id]))]
              (fn [db [_ id]] (update-in db [:world :items] dissoc id)))