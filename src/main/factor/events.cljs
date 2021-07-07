(ns factor.events
  (:require [re-frame.core :refer [inject-cofx reg-event-db reg-event-fx path]]
            [factor.world :as w]))


(defn reg-all []

  ;; General events

  (reg-event-db :initialize-db (fn [] {:ui {:selected-page [:home]}
                                       :config {}
                                       :world w/empty-world}))

  ;; World-mutating events

  (reg-event-db :update-factory (path :world) (fn [world [_ factory]] (w/update-factory world factory)))
  (reg-event-db :update-item    (path :world) (fn [world [_ item]]    (w/update-item    world item)))
  (reg-event-db :update-machine (path :world) (fn [world [_ machine]] (w/update-machine world machine)))
  (reg-event-db :update-recipe  (path :world) (fn [world [_ recipe]]  (w/update-recipe  world recipe)))

  (reg-event-db :delete-factory (path :world) (fn [world [_ factory]] (w/remove-factory-by-id world factory)))
  (reg-event-db :delete-recipe  (path :world) (fn [world [_ recipe]]  (w/remove-recipe-by-id  world recipe)))
  (reg-event-db :delete-machine (path :world) (fn [world [_ machine]] (w/remove-machine-by-id world machine)))
  (reg-event-db :delete-item    (path :world) (fn [world [_ item]]    (w/remove-item-by-id    world item)))

  (reg-event-db :world-reset (fn [db [_ w]] (assoc db :world w)))

  ;; World persistence events

  (reg-event-fx
   :world-load
   [(inject-cofx :localstorage :world)]
   (fn [{{world :world} :localstorage db :db} [_ default-world]]
     {:db (assoc db :world (if (not-empty world) world default-world))}))

  (reg-event-fx
   :world-save
   (fn [_ [_ world]]
     {:localstorage {:world world}}))

  ;; Config-mutating events

  (reg-event-db :open-factory (fn [db [_ id]] (assoc-in db [:config :open-factory] id)))

  ;; Config persistence events

  (reg-event-fx
   :config-load
   [(inject-cofx :localstorage :config)]
   (fn [{{config :config} :localstorage db :db} [_ default-config]]
     {:db (assoc db :config (if (not-empty config) config default-config))}))

  (reg-event-fx :config-save (fn [_ [_ config]] {:localstorage {:config config}}))

  ;; Helper events (triggers other events)

  (reg-event-fx :delete-items    (fn [_ [_ ids]] {:fx (map #(identity [:dispatch [:delete-item %]])    ids)}))
  (reg-event-fx :delete-machines (fn [_ [_ ids]] {:fx (map #(identity [:dispatch [:delete-machine %]]) ids)}))
  (reg-event-fx :delete-recipes  (fn [_ [_ ids]] {:fx (map #(identity [:dispatch [:delete-recipe %]])  ids)}))

  ;; UI mutation event

  (reg-event-db :ui (fn [db [_ path val]] (assoc-in db (into [:ui] path) val))))

