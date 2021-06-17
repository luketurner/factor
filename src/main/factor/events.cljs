(ns factor.events
  (:require [re-frame.core :refer [inject-cofx enrich reg-event-db reg-event-fx]]
            [factor.world :as w]
            [medley.core :refer [filter-vals]]
            [factor.util :refer [dispatch-after dissoc-in]]))

(defn reg-all []

  (reg-event-db :initialize-db (fn [] {:ui {:selected-page [:home]}
                                       :config {}
                                       :world {:items {}
                                               :factories {}
                                               :recipes {}
                                               :machines {}}}))

  (reg-event-db :update-factory (fn [db [_ id x]] (assoc-in db [:world :factories id] x)))
  (reg-event-db :update-item (fn [db [_ id x]] (assoc-in db [:world :items id] x)))
  (reg-event-db :update-recipe
                ;; [(dispatch-after (fn [[_ id]] [:update-factories-with-fk :item id]))]
                (fn [db [_ id x]] (assoc-in db [:world :recipes id] x)))
  (reg-event-db :update-machine
                ;; [(dispatch-after (fn [[_ id]] [:update-factories-with-fk :machine id]))
                ;;  (dispatch-after (fn [[_ id]] [:update-recipes-with-fk :machine id]))]
                (fn [db [_ id x]] (assoc-in db [:world :machines id] x)))
  
  (reg-event-db :open-factory (fn [db [_ id]] (assoc-in db [:config :open-factory] id)))

  (reg-event-db :delete-factory
                (fn [db [_ id]] (dissoc-in db [:world :factories] id)))
  (reg-event-db :delete-recipe
                ;; [(dispatch-after (fn [[_ id]] [:update-factories-with-fk :recipe id]))
                (fn [db [_ id]] (dissoc-in db [:world :recipes] id)))
  (reg-event-db :delete-item
                [(dispatch-after (fn [[_ id]] [:update-recipes-with-fk :item id]))]
                ;;  (dispatch-after (fn [[_ id]] [:update-factories-with-fk :item id]))
                (fn [db [_ id]] (dissoc-in db [:world :items] id)))
  (reg-event-db :delete-machine
                [(dispatch-after (fn [[_ id]] [:update-recipes-with-fk :machine id]))]
                ;;  (dispatch-after (fn [[_ id]] [:update-factories-with-fk :machine id]))
                (fn [db [_ id]] (dissoc-in db [:world :machines] id)))

  (reg-event-fx :delete-items (fn [_ [_ ids]] {:fx (map #(identity [:dispatch [:delete-item %]]) ids)}))
  (reg-event-fx :delete-machines (fn [_ [_ ids]] {:fx (map #(identity [:dispatch [:delete-machine %]]) ids)}))
  (reg-event-fx :delete-recipes (fn [_ [_ ids]] {:fx (map #(identity [:dispatch [:delete-recipe %]]) ids)}))

  (reg-event-fx
   :update-factories-with-fk
   (fn [{:keys [db]} [_ type type-id]]
     (let [world (:world db)
           to-update (filter-vals #(w/factory-has-reference? % type type-id) (:factories world))]
       {:fx (apply concat
                   (for [[factory-id _] to-update]
                     (let [factory (get-in world [:factories factory-id])]
                       [[:dispatch [:update-factory factory-id (w/update-factory-foreign-keys factory type world)]]
                        [:toast (str "Updating factory " factory-id " due to " (name type) " change.")]])))})))

  (reg-event-fx
   :update-recipes-with-fk
   (fn [{:keys [db]} [_ type type-id]]
     (let [world (:world db)
           to-update (filter-vals #(w/recipe-has-reference? % type type-id) (:recipes world))]
       {:fx (apply concat
                   (for [[recipe-id _] to-update]
                     (let [recipe (get-in world [:recipes recipe-id])]
                       [[:dispatch [:update-recipe recipe-id (w/update-recipe-foreign-keys recipe type world)]]
                        [:toast (str "Updating recipe " recipe-id " due to " (name type) " change.")]])))})))

  (reg-event-db :world-import (fn [db [_ w]] (assoc db :world w)))
  (reg-event-db :world-reset (fn [db] (assoc db :world {})))

  (reg-event-fx
   :load-world
   [(inject-cofx :localstorage :world)]
   (fn [{{world :world} :localstorage db :db}]
     {:db (assoc db :world (if (not-empty world) world w/empty-world))}))

  (reg-event-fx
   :save-world
   (fn [_ [_ world]]
     {:localstorage {:world world}}))

  (reg-event-db :ui (fn [db [_ path val]] (assoc-in db (into [:ui] path) val))))

