(ns factor.machine.events
  (:require [re-frame.core :refer [reg-event-db]]
            [factor.machine :refer [machine]]
            [factor.recipe.events :refer [update-recipe-fks]]
            [factor.util :refer [new-uuid]]))

(reg-event-db :create-machine (fn [db] (assoc-in db [:world :machines (new-uuid)] (machine))))
(reg-event-db :update-machine 
              [(update-recipe-fks {:with :machine})]
              (fn [db [_ id v]] (assoc-in db [:world :machines id] v)))
(reg-event-db :delete-machine
              [(update-recipe-fks {:with :machine})]
              (fn [db [_ id]] (update-in db [:world :machines] dissoc id)))
