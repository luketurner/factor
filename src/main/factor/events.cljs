(ns factor.events
  (:require [re-frame.core :refer [inject-cofx enrich reg-event-db reg-event-fx]]
            [factor.world :as w]
            [medley.core :refer [filter-vals]]
            [factor.util :refer [dispatch-after]]))

(defn reg-all []

  (reg-event-db :initialize-db (fn [] {:ui {:selected-page [:home]}
                                       :world {:items {}
                                               :factories {}
                                               :recipes {}
                                               :machines {}}}))

  ;; (reg-event-db :select-object (fn [db [_ object-type object-id]]
  ;;                                (assoc-in db [:ui :selected-object]
  ;;                                          {:object-id object-id :object-type object-type})))

  ;; (reg-event-db :toggle-sub-nav (fn [db [_ object-type]]
  ;;                                (update-in db [:ui :sub-nav] #(if (= % object-type) nil object-type))))

  (reg-event-db :create-factory #(w/update-world % w/with-factory nil (w/factory)))
  (reg-event-db :create-item #(w/update-world % w/with-item nil (w/item)))
  (reg-event-db :create-recipe #(w/update-world % w/with-recipe nil (w/recipe)))
  (reg-event-db :create-machine #(w/update-world % w/with-machine nil (w/machine)))

  (reg-event-db :update-factory
                (fn [db [_ id x]] (w/update-world db w/with-factory id x)))
  (reg-event-db :update-item
                (fn [db [_ id x]] (w/update-world db w/with-item id x)))
  (reg-event-db :update-recipe
                [(dispatch-after (fn [[_ id]] [:update-factories-with-fk :item id]))]
                (fn [db [_ id x]] (w/update-world db w/with-recipe id x)))
  (reg-event-db :update-machine
                [(dispatch-after (fn [[_ id]] [:update-factories-with-fk :machine id]))
                 (dispatch-after (fn [[_ id]] [:update-recipes-with-fk :machine id]))]
                (fn [db [_ id x]] (w/update-world db w/with-machine id x)))

  (reg-event-db :delete-factory 
                [(dispatch-after (fn [[_ id]] [:cleanup-selection :factory id]))]
                (fn [db [_ id]] (w/update-world db w/without-factory id)))
  (reg-event-db :delete-recipe
                [(dispatch-after (fn [[_ id]] [:update-factories-with-fk :recipe id]))
                 (dispatch-after (fn [[_ id]] [:cleanup-selection :recipe id]))]
                (fn [db [_ id]] (w/update-world db w/without-recipe id)))
  (reg-event-db :delete-item
                [(dispatch-after (fn [[_ id]] [:update-recipes-with-fk :item id]))
                 (dispatch-after (fn [[_ id]] [:update-factories-with-fk :item id]))
                 (dispatch-after (fn [[_ id]] [:cleanup-selection :item id]))]
                (fn [db [_ id]] (w/update-world db w/without-item id)))
  (reg-event-db :delete-machine
                [(dispatch-after (fn [[_ id]] [:update-recipes-with-fk :machine id]))
                 (dispatch-after (fn [[_ id]] [:update-factories-with-fk :machine id]))
                 (dispatch-after (fn [[_ id]] [:cleanup-selection :machine id]))]
                (fn [db [_ id]] (w/update-world db w/without-machine id)))
  
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

  (reg-event-db :select-page (fn [db [_ page]] (assoc-in db [:ui :selected-page] page)))
  (reg-event-db :update-selection (fn [db [_ s]] (assoc-in db [:ui :selection] s)))
  (reg-event-db :cleanup-selection (fn [db [_ type id]]
                                     (update-in db [:ui :selection]
                                                (fn [[t s]]
                                                  [t (if (= type t)
                                                       (remove #(= % id) s)
                                                       s)]))))
  
  (reg-event-db :ui (fn [db [_ path val]] (assoc-in db (into [:ui] path) val)))

  )

