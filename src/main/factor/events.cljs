(ns factor.events
  "Defines all the events used by Factor."
  (:require [re-frame.core :refer [inject-cofx reg-event-db reg-event-fx path ->interceptor]]
            [factor.world :as w]
            [factor.db :as db]
            [day8.re-frame.undo :as undo :refer [undoable]]))


(defn reg-all []

  ;; Custom interceptors that provide functions similar to [:purge-redos] event.
  (defn ->purge-undos []
    (->interceptor
     :id :purge-undos
     :after (fn [ctx]
              (when (undo/undos?) (undo/clear-undos!))
              ctx)))

  (defn ->purge-redos []
    (->interceptor
     :id :purge-redos
     :after (fn [ctx]
              (when (undo/redos?) (undo/clear-redos!))
              ctx)))

  ;; General events

  (reg-event-db :initialize-db (fn [] {:ui {:selected-page :home}
                                       :config {:unit {:item-rate "items/sec"
                                                       :power "W"
                                                       :energy "J"}}
                                       :world w/empty-world}))

  ;; World-mutating events
  ;; Undoable

  (reg-event-db :update-factory [(undoable) (path :world)] (fn [world [_ factory]] (w/update-factory world factory)))
  (reg-event-db :update-item    [(undoable) (path :world)] (fn [world [_ item]]    (w/update-item    world item)))
  (reg-event-db :update-machine [(undoable) (path :world)] (fn [world [_ machine]] (w/update-machine world machine)))
  (reg-event-db :update-recipe  [(undoable) (path :world)] (fn [world [_ recipe]]  (w/update-recipe  world recipe)))

  (reg-event-db :delete-factories [(undoable) (path :world)] (fn [world [_ xs]] (reduce w/remove-factory-by-id world xs)))
  (reg-event-db :delete-recipes   [(undoable) (path :world)] (fn [world [_ xs]] (reduce w/remove-recipe-by-id  world xs)))
  (reg-event-db :delete-machines  [(undoable) (path :world)] (fn [world [_ xs]] (reduce w/remove-machine-by-id world xs)))
  (reg-event-db :delete-items     [(undoable) (path :world)] (fn [world [_ xs]] (reduce w/remove-item-by-id    world xs)))

  (reg-event-db :world-reset
                [(undoable)
                 (db/->world-validator)
                 (db/->migrate-database)]
                (fn [db [_ w]] (assoc db :world w)))

  (reg-event-db :update-factory-name [(undoable) (path :world)] (fn [world [_ id name]] (assoc-in world [:factories id :name] name)))
  (reg-event-db :update-factory-desired-output [(undoable) (path :world)] (fn [world [_ id qm]] (assoc-in world [:factories id :desired-output] qm)))

  (reg-event-db :update-recipe-input [(undoable) (path :world)] (fn [world [_ id qm]] (assoc-in world [:recipes id :input] qm)))
  (reg-event-db :update-recipe-output [(undoable) (path :world)] (fn [world [_ id qm]] (assoc-in world [:recipes id :output] qm)))
  (reg-event-db :update-recipe-catalysts [(undoable) (path :world)] (fn [world [_ id qm]] (assoc-in world [:recipes id :catalysts] qm)))
  (reg-event-db :update-recipe-machines [(undoable) (path :world)] (fn [world [_ id xs]] (assoc-in world [:recipes id :machines] xs)))
  (reg-event-db :update-recipe-duration [(undoable) (path :world)] (fn [world [_ id n]] (assoc-in world [:recipes id :duration] n)))


  ;; World persistence events
  ;; Not undoable

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
  ;; Not undoable (for now?)

  (reg-event-db :open-factory (fn [db [_ id]] (assoc-in db [:config :open-factory] id)))
  (reg-event-db :set-unit (fn [db [_ k v]] (assoc-in db [:config :unit k] v)))

  ;; Config persistence events
  ;; Not undoable

  (reg-event-fx
   :config-load
   [(inject-cofx :localstorage :config)
    (db/->migrate-config)]
   (fn [{{config :config} :localstorage db :db} [_ default-config]]
     {:db (assoc db :config (if (not-empty config) config default-config))}))

  (reg-event-fx :config-save (fn [_ [_ config]] {:localstorage {:config config}}))

  ;; UI mutation events
  ;; Not undoable

  ; Note: This event purges the undo/redo history when it runs so the user can't undo/redo things from the old page
  (reg-event-db :select-page [(->purge-undos) (->purge-redos)] (fn [db [_ page]] (assoc-in db [:ui :selected-page] page)))

  (reg-event-db :ui (fn [db [_ path val]] (assoc-in db (into [:ui] path) val))))

