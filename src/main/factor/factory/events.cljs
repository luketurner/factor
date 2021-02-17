(ns factor.factory.events
  (:require [re-frame.core :refer [enrich reg-event-db get-effect reg-event-fx ->interceptor get-coeffect assoc-effect]]
            [factor.factory :refer [recalc-factory factory factory-has-reference? update-foreign-keys]]
            [factor.util :refer [new-uuid]]
            [medley.core :refer [filter-vals]]))

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

(defn update-factory-fks [{:keys [with]}]
  (->interceptor
   :after (fn [context]
            (let [[_ id] (get-coeffect context :event)
                  existing-fx (get-effect context :fx [])
                  update-fx [:dispatch [:update-factories-with-fk with id]]]
              (assoc-effect context :fx (conj existing-fx update-fx))))))