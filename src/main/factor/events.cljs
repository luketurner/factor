(ns factor.events
  (:require [re-frame.core :refer [inject-cofx reg-event-db reg-event-fx path]]
            [factor.world :as w]
            [factor.db :as db]))


(defn reg-all []

  ;; General events

  (reg-event-db :initialize-db (fn [] {:ui {:selected-page [:home]}
                                       :config {:unit {:item-rate "items/sec"
                                                       :power "W"
                                                       :energy "J"}}
                                       :world w/empty-world}))

  ;; World-mutating events

  (reg-event-db :update-factory (path :world) (fn [world [_ factory]] (w/update-factory world factory)))
  (reg-event-db :update-item    (path :world) (fn [world [_ item]]    (w/update-item    world item)))
  (reg-event-db :update-machine (path :world) (fn [world [_ machine]] (w/update-machine world machine)))
  (reg-event-db :update-recipe  (path :world) (fn [world [_ recipe]]  (w/update-recipe  world recipe)))

  (reg-event-db :delete-factories [(path :world)] (fn [world [_ xs]] (reduce w/remove-factory-by-id world xs)))
  (reg-event-db :delete-recipes   [(path :world)] (fn [world [_ xs]] (reduce w/remove-recipe-by-id  world xs)))
  (reg-event-db :delete-machines  [(path :world)] (fn [world [_ xs]] (reduce w/remove-machine-by-id world xs)))
  (reg-event-db :delete-items     [(path :world)] (fn [world [_ xs]] (reduce w/remove-item-by-id    world xs)))

  (reg-event-db :world-reset
                [(db/->world-validator)
                 (db/->migrate-database)]
                (fn [db [_ w]] (assoc db :world w)))


  (reg-event-db :update-recipe-input [(path :world)] (fn [world [_ id qm]] (assoc-in world [:recipes id :input] qm)))
  (reg-event-db :update-recipe-output [(path :world)] (fn [world [_ id qm]] (assoc-in world [:recipes id :output] qm)))
  (reg-event-db :update-recipe-catalysts [(path :world)] (fn [world [_ id qm]] (assoc-in world [:recipes id :catalysts] qm)))
  (reg-event-db :update-recipe-machines [(path :world)] (fn [world [_ id xs]] (assoc-in world [:recipes id :machines] xs)))
  (reg-event-db :update-recipe-duration [(path :world)] (fn [world [_ id n]] (assoc-in world [:recipes id :duration] n)))


  ;; World persistence events

  (reg-event-fx
   :world-load
   [(inject-cofx :localstorage :world)
    (db/->world-validator)
    (db/->migrate-database)]
   (fn [{{world :world} :localstorage db :db} [_ default-world]]
     {:db (assoc db :world (if (not-empty world) world default-world))}))

  (reg-event-fx
   :world-save
   (fn [_ [_ world]]
     {:localstorage {:world world}}))

  ;; Config-mutating events

  (reg-event-db :open-factory (fn [db [_ id]] (assoc-in db [:config :open-factory] id)))
  (reg-event-db :set-unit (fn [db [_ k v]] (assoc-in db [:config :unit k] v)))

  ;; Config persistence events

  (reg-event-fx
   :config-load
   [(inject-cofx :localstorage :config)
    (db/->migrate-config)]
   (fn [{{config :config} :localstorage db :db} [_ default-config]]
     {:db (assoc db :config (if (not-empty config) config default-config))}))

  (reg-event-fx :config-save (fn [_ [_ config]] {:localstorage {:config config}}))

  ;; UI mutation event

  (reg-event-db :ui (fn [db [_ path val]] (assoc-in db (into [:ui] path) val))))

