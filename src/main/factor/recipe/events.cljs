(ns factor.recipe.events
  (:require [re-frame.core :refer [reg-event-db]]
            [factor.recipe :refer [recipe]]))

(reg-event-db :create-recipe
              (fn [db [_ opt]]
                (let [id (random-uuid)]
                  (cond-> db
                    true (assoc-in [:world :recipes id] (recipe))
                    (= opt :expanded) (assoc-in [:ui :recipes :expanded id] true)))))
(reg-event-db :update-recipe (fn [db [_ id v]] (assoc-in db [:world :recipes id] v)))
(reg-event-db :delete-recipe (fn [db [_ id]] (update-in db [:world :recipes] dissoc id)))
(reg-event-db :toggle-recipe-expanded (fn [db [_ id]] (update-in db [:ui :recipes :expanded id] not)))
