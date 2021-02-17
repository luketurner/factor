(ns factor.item.events
  (:require [re-frame.core :refer [reg-event-db]]
            [factor.recipe.events :refer [update-recipe-fks]]
            [factor.item :refer [item]]
            [factor.factory.events :refer [update-factory-fks]]
            [factor.util :refer [new-uuid]]))

(reg-event-db :create-item (fn [db] (assoc-in db [:world :items (new-uuid)] (item))))
(reg-event-db :update-item (fn [db [_ id v]] (assoc-in db [:world :items id] v)))
(reg-event-db :delete-item
              [(update-recipe-fks {:with :item})
               (update-factory-fks {:with :item})]
              (fn [db [_ id]] (update-in db [:world :items] dissoc id)))