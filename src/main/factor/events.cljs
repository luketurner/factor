(ns factor.events
  "Defines all the events used by Factor."
  (:require [re-frame.core :refer [inject-cofx reg-event-db reg-event-fx path]]
            [day8.re-frame.undo :refer [undoable]]
            [com.rpl.specter :as s]
            [factor.navs :as nav]
            [factor.schema :refer [make edn-decode edn-encode json-decode Config Factory Item Recipe Machine World AppDb]]
            [factor.util :refer [json->clj edn->clj new-uuid]]
            [factor.interceptors :refer [->purge-redos ->purge-undos]]))

(defn reg-all []

  ;; General events

  (reg-event-db :initialize-db (fn [] (make AppDb nil)))

  ;; World-mutating events
  ;; Undoable


  (reg-event-db :create-factory [(undoable)]
                (fn [db [_ factory]]
                  (let [id (new-uuid)
                        factory (->> factory
                                     (merge {:created-at (.now js/Date) :id id})
                                     (make Factory))]
                    (s/multi-transform
                     [(s/multi-path
                       [nav/CONFIG nav/OPEN-FACTORY (s/terminal-val id)]
                       [nav/WORLD (nav/valid-factory id) (s/terminal-val factory)])]
                     db))))

  (reg-event-db :create-item [(undoable)]
                (fn [db [_ item]]
                  (let [id (new-uuid)
                        item (->> item
                                  (merge {:created-at (.now js/Date) :id id})
                                  (make Item))]

                    (s/setval [nav/W (nav/valid-item id)] item db))))

  (reg-event-db :create-recipe [(undoable)]
                (fn [db [_ recipe]]
                  (let [id (new-uuid)
                        recipe (->> recipe
                                    (merge {:created-at (.now js/Date) :id id})
                                    (make Recipe))]
                    (s/setval [nav/W (nav/valid-recipe id)] recipe db))))

  (reg-event-db :create-machine [(undoable)]
                (fn [db [_ machine]]
                  (let [id (new-uuid)
                        machine (->> machine
                                     (merge {:created-at (.now js/Date) :id id})
                                     (make Machine))]
                    (s/setval [nav/W (nav/valid-machine id)] machine db))))

  (reg-event-db :update-factory [(undoable)]
                (fn [db [_ {:keys [id] :as x}]]
                  (s/setval [nav/WORLD (nav/valid-factory id)] x db)))

  (reg-event-db :update-item    [(undoable)]
                (fn [db [_ {:keys [id] :as x}]]
                  (s/setval [nav/WORLD (nav/valid-item id)] x db)))

  (reg-event-db :update-machine [(undoable)]
                (fn [db [_ {:keys [id] :as x}]]
                  (s/setval [nav/WORLD (nav/valid-machine id)] x db)))

  (reg-event-db :update-recipe  [(undoable)]
                (fn [db [_ {:keys [id] :as x}]]
                  (s/setval [nav/WORLD (nav/valid-recipe id)] x db)))

  (reg-event-db :delete-factories [(undoable)]
                (fn [w [_ xs]]
                  (s/setval (s/multi-path
                             [nav/UI nav/SELECTED-OBJECTS s/ALL xs]
                             [nav/WORLD (nav/map-factories xs)]) s/NONE w)))

  (reg-event-db :delete-recipes [(undoable)]
                (fn [w [_ xs]]
                  (let [xs (set xs)]
                    (s/setval
                     (s/multi-path
                      [nav/UI nav/SELECTED-OBJECTS s/ALL xs]
                      [nav/WORLD (s/multi-path
                                  [nav/MAP-FACTORIES nav/FACTORY->RECIPES xs]
                                  (nav/map-recipes xs))])
                     s/NONE
                     w))))

  (reg-event-db :delete-machines [(undoable)]
                (fn [w [_ xs]]
                  (let [xs (set xs)]
                    (s/setval
                     (s/multi-path
                      [nav/UI nav/SELECTED-OBJECTS s/ALL xs]
                      [nav/WORLD (s/multi-path
                                  [nav/MAP-FACTORIES nav/FACTORY->MACHINES xs]
                                  [nav/MAP-RECIPES nav/RECIPE->MACHINES xs]
                                  (nav/map-machines xs))])
                     s/NONE
                     w))))

  (reg-event-db :delete-items [(undoable)]
                (fn [w [_ xs]]
                  (let [xs (set xs)]
                    (s/setval
                     (s/multi-path
                      [nav/UI nav/SELECTED-OBJECTS s/ALL xs]
                      [nav/WORLD (s/multi-path
                                  [nav/MAP-FACTORIES nav/FACTORY->ITEMS xs]
                                  [nav/MAP-RECIPES nav/RECIPE->ITEMS xs]
                                  (nav/map-items xs))])
                     s/NONE
                     w))))

  (reg-event-db :world-reset
                [(undoable)]
                (fn [db [_ w]] (assoc db :world (make World w))))

  (reg-event-db :load-world-from-json
                [(undoable)]
                (fn [db [_ x]] (->> x
                                    (json->clj)
                                    (json-decode World)
                                    #(s/setval [nav/VALID-WORLD] % db))))

  (reg-event-db :load-world-from-edn
                [(undoable)]
                (fn [db [_ x]] (->> x
                                    (edn->clj)
                                    (edn-decode World)
                                    #(s/setval [nav/VALID-WORLD] % db))))

  (reg-event-db :update-factory-name [(undoable)]
              (fn [db [_ id v]]
                (s/setval [nav/W (nav/valid-factory id) nav/NAME] v db)))
  
  (reg-event-db :update-factory-desired-output [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/W (nav/valid-factory id) nav/DESIRED-OUTPUT] v db)))

  (reg-event-db :update-recipe-input [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/W (nav/valid-recipe id) nav/INPUT] v db)))
  
  (reg-event-db :update-recipe-output [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/W (nav/valid-recipe id) nav/OUTPUT] v db)))
  
  (reg-event-db :update-recipe-catalysts [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/W (nav/valid-recipe id) nav/CATALYSTS] v db)))
  
  (reg-event-db :update-recipe-machines [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/W (nav/valid-recipe id) nav/MACHINES] v db)))
  
  (reg-event-db :update-recipe-duration [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/W (nav/valid-recipe id) nav/DURATION] v db)))


  ;; World persistence events
  ;; Not undoable

  (reg-event-fx
   :world-load
   [(inject-cofx :localstorage :world)]
   (fn [{{world :world} :localstorage db :db}]
     {:db (s/setval [nav/VALID-WORLD] (edn-decode World world) db)}))

  (reg-event-fx
   :world-save
   (fn [_ [_ world]]
     {:localstorage {:world (edn-encode World world)}}))

  ;; Config-mutating events
  ;; Not undoable (for now?)

  (reg-event-db :open-factory (fn [db [_ id]] (s/setval [nav/VALID-CONFIG nav/OPEN-FACTORY] id db)))
  (reg-event-db :set-unit (fn [db [_ k v]] (s/setval [nav/VALID-CONFIG nav/UNIT k] v db)))

  ;; Config persistence events
  ;; Not undoable

  (reg-event-fx
   :config-load
   [(inject-cofx :localstorage :config)]
   (fn [{{config :config} :localstorage db :db}]
     {:db (s/setval [nav/VALID-CONFIG] (edn-decode Config config) db)}))

  (reg-event-fx :config-save (fn [_ [_ config]] {:localstorage {:config (edn-encode Config config)}}))

  ;; UI mutation events
  ;; Not undoable

  ; Note: This event purges the undo/redo history when it runs so the user can't undo/redo things from the old page
  (reg-event-db :select-page [(->purge-undos) (->purge-redos)]
                (fn [db [_ page]] (s/multi-transform [nav/UI (s/multi-path [nav/SELECTED-PAGE (s/terminal-val page)]
                                                                           [nav/SELECTED-OBJECTS (s/terminal-val [])])] db)))
  
  (reg-event-db :select-objects
                (fn [db [_ xs]] (s/setval [nav/UI nav/SELECTED-OBJECTS] xs db))))

