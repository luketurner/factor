(ns factor.factory.events
  (:require [re-frame.core :refer [reg-event-db]]
            [factor.factory :refer [update-factories factory]]))

(reg-event-db :create-factory (fn [db] (assoc-in db [:world :factories (random-uuid)] (factory))))
(reg-event-db :update-factory
              [(update-factories {:with :factory})]
              (fn [db [_ id factory]] (assoc-in db [:world :factories id] factory)))
(reg-event-db :delete-factory (fn [db [_ id]] (update-in db [:world :factories] dissoc id)))

