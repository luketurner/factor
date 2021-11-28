(ns factor.events
  "Defines all the events used by Factor."
  (:require [re-frame.core :refer [inject-cofx reg-event-db reg-event-fx path ->interceptor]]
            [factor.db :as db]
            [day8.re-frame.undo :as undo :refer [undoable]]
            [com.rpl.specter :as s]
            [factor.schema :refer [make edn-decode edn-encode json-decode Config Factory Item Recipe Machine World AppDb]]
            [factor.util :refer [json->clj edn->clj new-uuid]]))

(def TERM-NONE (s/path (s/terminal-val s/NONE)))

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

  (reg-event-db :initialize-db (fn [] (make AppDb nil)))

  ;; World-mutating events
  ;; Undoable


  (reg-event-db :create-factory [(undoable)]
                (fn [db [_ factory]]
                  (let [id (new-uuid)]
                    (-> db
                        (assoc-in [:config :open-factory] id)
                        (assoc-in [:world :factories id]
                                  (->> factory
                                       (merge {:created-at (.now js/Date) :id id})
                                       (make Factory)))))))

  (reg-event-db :create-item [(undoable) (path :world :items)]
                (fn [items [_ item]]
                  (let [id (new-uuid)]
                    (->> item
                         (merge {:created-at (.now js/Date) :id id})
                         (make Item)
                         (assoc items id)))))

  (reg-event-db :create-recipe [(undoable) (path :world :recipes)]
                (fn [recipes [_ recipe]]
                  (let [id (new-uuid)]
                    (->> recipe
                         (merge {:created-at (.now js/Date) :id id})
                         (make Recipe)
                         (assoc recipes id)))))

  (reg-event-db :create-machine [(undoable) (path :world :machines)]
                (fn [machines [_ machine]]
                  (let [id (new-uuid)]
                    (->> machine
                         (merge {:created-at (.now js/Date) :id id})
                         (make Machine)
                         (assoc machines id)))))

  (reg-event-db :update-factory [(undoable) (path :world :factories)]
                (fn [xs [_ {:keys [id] :as x}]] (assoc xs id x)))
  
  (reg-event-db :update-item    [(undoable) (path :world :items)]
                (fn [xs [_ {:keys [id] :as x}]] (assoc xs id x)))
  
  (reg-event-db :update-machine [(undoable) (path :world :macines)]
                (fn [xs [_ {:keys [id] :as x}]] (assoc xs id x)))
  
  (reg-event-db :update-recipe  [(undoable) (path :world :recipes)]
                (fn [xs [_ {:keys [id] :as x}]] (assoc xs id x)))

  (reg-event-db :delete-factories [(undoable) (path :world)]
                (fn [w [_ xs]]
                  (s/setval [:factories s/ALL (s/selected? s/FIRST xs)]
                            s/NONE
                            w)))

  (reg-event-db :delete-recipes [(undoable) (path :world)]
                (fn [w [_ xs]]
                  (let [xs (set xs)
                        WITHOUT-XS (s/path s/ALL xs TERM-NONE)
                        WITHOUT-XS-KEY (s/path s/ALL (s/selected? s/FIRST xs) TERM-NONE)]
                    (s/multi-transform
                     [(s/multi-path
                       [:factories s/MAP-VALS :filter
                        (s/multi-path
                         [:hard-allowed-recipes WITHOUT-XS]
                         [:soft-allowed-recipes WITHOUT-XS]
                         [:hard-denied-recipes WITHOUT-XS]
                         [:soft-denied-recipes WITHOUT-XS])]
                       [:recipes WITHOUT-XS-KEY])]
                     w))))
  
  (reg-event-db :delete-machines  [(undoable) (path :world)]
                (fn [w [_ xs]]
                  (let [xs (set xs)
                        WITHOUT-XS (s/path s/ALL xs TERM-NONE)
                        WITHOUT-XS-KEY (s/path s/ALL (s/selected? s/FIRST xs) TERM-NONE)]
                    (s/multi-transform
                     [(s/multi-path
                       [:factories s/MAP-VALS :filter
                        (s/multi-path
                         [:hard-allowed-machines WITHOUT-XS]
                         [:soft-allowed-machines WITHOUT-XS]
                         [:hard-denied-machines WITHOUT-XS]
                         [:soft-denied-machines WITHOUT-XS])]
                       [:recipes s/MAP-VALS :machines WITHOUT-XS]
                       [:machines WITHOUT-XS-KEY])]
                     w))))
  
  (reg-event-db :delete-items [(undoable) (path :world)]
                (fn [w [_ xs]]
                  (let [xs (set xs)
                        WITHOUT-XS (s/path s/ALL xs TERM-NONE)
                        WITHOUT-XS-KEY (s/path s/ALL (s/selected? s/FIRST xs) TERM-NONE)]
                    (s/multi-transform
                     [(s/multi-path
                       [:factories s/MAP-VALS
                        (s/multi-path
                         [:desired-output WITHOUT-XS]
                         [:filter
                          (s/multi-path
                           [:hard-allowed-items WITHOUT-XS]
                           [:soft-allowed-items WITHOUT-XS]
                           [:hard-denied-items WITHOUT-XS]
                           [:soft-denied-items WITHOUT-XS])])]
                       [:recipes s/MAP-VALS
                        (s/multi-path
                         [:input WITHOUT-XS-KEY]
                         [:output WITHOUT-XS-KEY]
                         [:catalysts WITHOUT-XS-KEY])]
                       [:items WITHOUT-XS-KEY])]
                     w))))

  (reg-event-db :world-reset
                [(undoable)
                 (db/->world-validator)
                 (db/->migrate-database)]
                (fn [db [_ w]] (assoc db :world (make World w))))

  (reg-event-db :load-world-from-json
                [(undoable)
                 (db/->world-validator)
                 (db/->migrate-database)]
                (fn [db [_ x]] (->> x
                                    (json->clj)
                                    (json-decode World)
                                    (assoc db :world))))

  (reg-event-db :load-world-from-edn
                [(undoable)
                 (db/->world-validator)
                 (db/->migrate-database)]
                (fn [db [_ x]] (->> x
                                    (edn->clj)
                                    (edn-decode World)
                                    (assoc db :world))))

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
   (fn [{{world :world} :localstorage db :db}]
     {:db (assoc db :world (edn-decode World world))}))

  (reg-event-fx
   :world-save
   (fn [_ [_ world]]
     {:localstorage {:world (edn-encode World world)}}))

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
   (fn [{{config :config} :localstorage db :db}]
     {:db (assoc db :config (edn-decode Config config))}))

  (reg-event-fx :config-save (fn [_ [_ config]] {:localstorage {:config (edn-encode Config config)}}))

  ;; UI mutation events
  ;; Not undoable

  ; Note: This event purges the undo/redo history when it runs so the user can't undo/redo things from the old page
  (reg-event-db :select-page [(->purge-undos) (->purge-redos)] (fn [db [_ page]] (assoc-in db [:ui :selected-page] page)))

  (reg-event-db :ui (fn [db [_ path val]] (assoc-in db (into [:ui] path) val))))

