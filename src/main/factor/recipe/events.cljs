(ns factor.recipe.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [factor.recipe :refer [recipe recipe-has-reference? update-foreign-keys]]
            [medley.core :refer [filter-vals]]
            [factor.util :refer [new-uuid dispatch-after]]))

(reg-event-db :create-recipe
              (fn [db [_ opt]]
                (let [id (new-uuid)]
                  (cond-> db
                    true (assoc-in [:world :recipes id] (recipe))
                    (= opt :expanded) (assoc-in [:ui :recipes :expanded id] true)))))
(reg-event-db
 :update-recipe
 [(dispatch-after (fn [[_ id]] [:update-factories-with-fk :item id]))]
 (fn [db [_ id v]] (assoc-in db [:world :recipes id] v)))
(reg-event-db :delete-recipe (fn [db [_ id]] (update-in db [:world :recipes] dissoc id)))
(reg-event-db :toggle-recipe-expanded (fn [db [_ id]] (update-in db [:ui :recipes :expanded id] not)))

(reg-event-fx
 :update-recipes-with-fk
 (fn [{:keys [db]} [_ type type-id]]
   (let [world (:world db)
         to-update (filter-vals #(recipe-has-reference? % type type-id) (:recipes world))]
     {:fx (apply concat
                 (for [[recipe-id _] to-update]
                   (let [recipe (get-in world [:recipes recipe-id])]
                     [[:dispatch [:update-recipe recipe-id (update-foreign-keys recipe type world)]]
                      [:toast (str "Updating recipe " recipe-id " due to " (name type) " change.")]])))})))