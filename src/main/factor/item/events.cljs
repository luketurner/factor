(ns factor.item.events
  (:require [re-frame.core :refer [reg-event-db]]
            [factor.factory :refer [update-factories]]
            [factor.item :refer [item]]))

(reg-event-db :create-item (fn [db] (assoc-in db [:world :items (random-uuid)] (item))))
(reg-event-db :update-item (fn [db [_ id v]] (assoc-in db [:world :items id] v)))
(reg-event-db :delete-item
              [(update-factories {:with :item})]
              (fn [db [_ id]] (update-in db [:world :items] dissoc id)))