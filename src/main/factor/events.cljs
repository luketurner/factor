(ns factor.events
  (:require [re-frame.core :refer [inject-cofx enrich reg-event-db reg-event-fx]]
            [factor.factory :refer [recalc-factory factory factory-has-reference? update-foreign-keys]]
            [factor.util :refer [dispatch-after]]
            [factor.machine :refer [machine]]
            [factor.item :refer [item]]
            [factor.recipe :refer [recipe-has-reference? recipe]]
            [factor.world :refer [empty-world] :as world]
            [medley.core :refer [filter-vals]]))

(defn reg-all []

  (reg-event-db :initialize-db (fn [] {:ui {:selected-page :home}
                                       :world {:items {}
                                               :factories {}
                                               :recipes {}
                                               :machines {}}}))

  (reg-event-db :create-factory #(world/update-world % world/with-factory nil (factory)))
  (reg-event-db :create-item #(world/update-world % world/with-item nil (item)))
  (reg-event-db :create-recipe #(world/update-world % world/with-recipe nil (recipe)))
  (reg-event-db :create-machine #(world/update-world % world/with-machine nil (machine)))

  (reg-event-db :update-factory
                [(enrich recalc-factory)]
                (fn [db [_ id x]] (world/update-world db world/with-factory id x)))
  (reg-event-db :update-item
                (fn [db [_ id x]] (world/update-world db world/with-item id x)))
  (reg-event-db :update-recipe
                [(dispatch-after (fn [[_ id]] [:update-factories-with-fk :item id]))]
                (fn [db [_ id x]] (world/update-world db world/with-recipe id x)))
  (reg-event-db :update-machine
                [(dispatch-after (fn [[_ id]] [:update-factories-with-fk :machine id]))
                 (dispatch-after (fn [[_ id]] [:update-recipes-with-fk :machine id]))]
                (fn [db [_ id x]] (world/update-world db world/with-machine id x)))

  (reg-event-db :delete-factory (fn [db [_ id]] (world/update-world db world/without-factory id)))
  (reg-event-db :delete-recipe
                [(dispatch-after (fn [[_ id]] [:update-factories-with-fk :recipe id]))]
                (fn [db [_ id]] (world/update-world db world/without-recipe id)))
  (reg-event-db :delete-item
                [(dispatch-after (fn [[_ id]] [:update-recipes-with-fk :item id]))
                 (dispatch-after (fn [[_ id]] [:update-factories-with-fk :item id]))]
                (fn [db [_ id]] (world/update-world db world/without-item id)))
  (reg-event-db :delete-machine
                [(dispatch-after (fn [[_ id]] [:update-recipes-with-fk :machine id]))
                 (dispatch-after (fn [[_ id]] [:update-factories-with-fk :machine id]))]
                (fn [db [_ id]] (world/update-world db world/without-machine id)))

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

  (reg-event-db :select-page (fn [db [_ page]] (assoc-in db [:ui :selected-page] page))))

