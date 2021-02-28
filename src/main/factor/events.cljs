(ns factor.events
  (:require [re-frame.core :refer [reg-global-interceptor inject-cofx enrich reg-event-db get-effect reg-event-fx ->interceptor get-coeffect assoc-effect]]
            [factor.factory :refer [recalc-factory factory factory-has-reference? update-foreign-keys]]
            [factor.util :refer [new-uuid dispatch-after add-fx]]
            [factor.machine :refer [machine]]
            [factor.item :refer [item]]
            [factor.recipe :refer [recipe-has-reference? recipe]]
            [factor.world :refer [empty-world]]
            [medley.core :refer [filter-vals]]))

(defn reg-all-events []
  (reg-event-db :create-factory (fn [db] (assoc-in db [:world :factories (new-uuid)] (factory))))
  (reg-event-db :update-factory
                [(enrich recalc-factory)]
                (fn [db [_ id factory]] (assoc-in db [:world :factories id] factory)))
  (reg-event-db :delete-factory (fn [db [_ id]] (update-in db [:world :factories] dissoc id)))

  (reg-event-fx
   :update-factories-with-fk
   (fn [{:keys [db]} [_ type type-id]]
     (let [world (:world db)
           to-update (filter-vals #(factory-has-reference? % type type-id) (:factories world))]
       {:fx (apply concat
                   (for [[factory-id _] to-update]
                     (let [factory (get-in world [:factories factory-id])]
                       [[:dispatch [:update-factory factory-id (update-foreign-keys factory type world)]]
                        [:toast (str "Updating factory " factory-id " due to " (name type) " change.")]])))})))

  (reg-event-db :create-item (fn [db] (assoc-in db [:world :items (new-uuid)] (item))))
  (reg-event-db :update-item (fn [db [_ id v]] (assoc-in db [:world :items id] v)))
  (reg-event-db :delete-item
                [(dispatch-after (fn [[_ id]] [:update-recipes-with-fk :item id]))
                 (dispatch-after (fn [[_ id]] [:update-factories-with-fk :item id]))]
                (fn [db [_ id]] (update-in db [:world :items] dissoc id)))
  (reg-event-db :create-machine (fn [db] (assoc-in db [:world :machines (new-uuid)] (machine))))
  (reg-event-db :update-machine
                (fn [db [_ id v]] (assoc-in db [:world :machines id] v)))
  (reg-event-db :delete-machine
                [(dispatch-after (fn [[_ id]] [:update-recipes-with-fk :item id]))
                 (dispatch-after (fn [[_ id]] [:update-factories-with-fk :item id]))]
                (fn [db [_ id]] (update-in db [:world :machines] dissoc id)))

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

  (reg-event-db :world-import (fn [db [_ w]] (assoc db :world w)))
  (reg-event-db :world-reset (fn [db] (assoc db :world {})))

  (reg-event-fx
   :load-world
   [(inject-cofx :localstorage :world)]
   (fn [{{world :world} :localstorage db :db :as foo}]
     {:db (assoc db :world (if (not-empty world) world empty-world))}))

  (reg-event-fx
   :save-world
   (fn [_ [_ world]]
     {:localstorage {:world world}}))

  (reg-global-interceptor
   (->interceptor
    :id :world-saver
    :after (fn [{{{old-world :world} :db} :coeffects
                 {{new-world :world} :db} :effects :as context}]
             (if (and new-world (not= new-world old-world))
               (add-fx context [:dispatch [:save-world new-world]])
               context)))))

