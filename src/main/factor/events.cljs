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
                    (s/setval [nav/WORLD (nav/valid-item id)] item db))))

  (reg-event-db :create-recipe [(undoable)]
                (fn [db [_ recipe]]
                  (let [id (new-uuid)
                        recipe (->> recipe
                                    (merge {:created-at (.now js/Date) :id id})
                                    (make Recipe))]
                    (s/setval [nav/WORLD (nav/valid-recipe id)] recipe db))))

  (reg-event-db :create-machine [(undoable)]
                (fn [db [_ machine]]
                  (let [id (new-uuid)
                        machine (->> machine
                                     (merge {:created-at (.now js/Date) :id id})
                                     (make Machine))]
                    (s/setval [nav/WORLD (nav/valid-machine id)] machine db))))

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
                  (let [xs (set xs)]
                    (s/multi-transform
                     (s/multi-path
                      [nav/VALID-WORLD (nav/factories xs) (s/terminal-val s/NONE)]
                      ; the below path benefit from some explaining. Basically, if
                      ; the open factory was deleted, we want to open another,
                      ; not-deleted factory if possible. The (collect-one) navigator
                      ; collects the ID of another factory to open. The (s/terminal identity)
                      ; line will then set the value of the navigated path to that collected value.
                      ; Because multi-path transforms are run in order, the deleted
                      ; factories have already been removed from FACTORIES-MAP when this runs.
                      [(s/collect-one nav/WORLD nav/FACTORIES-MAP s/FIRST s/FIRST)
                       nav/VALID-CONFIG nav/OPEN-FACTORY xs (s/terminal identity)])
                     w))))

  (reg-event-db :delete-recipes [(undoable)]
                (fn [w [_ xs]]
                  (let [xs (set xs)]
                    (s/setval
                     (s/multi-path
                      [nav/VALID-UI nav/SELECTED-OBJECTS xs]
                      [nav/WORLD (s/multi-path
                                  [nav/FACTORIES nav/FACTORY->RECIPES xs]
                                  (nav/recipes xs))])
                     s/NONE
                     w))))

  (reg-event-db :delete-machines [(undoable)]
                (fn [w [_ xs]]
                  (let [xs (set xs)]
                    (s/setval
                     (s/multi-path
                      [nav/VALID-UI nav/SELECTED-OBJECTS xs]
                      [nav/WORLD (s/multi-path
                                  [nav/FACTORIES nav/FACTORY->MACHINES xs]
                                  [nav/RECIPES nav/RECIPE->MACHINES xs]
                                  (nav/machines xs))])
                     s/NONE
                     w))))

  (reg-event-db :delete-items [(undoable)]
                (fn [w [_ xs]]
                  (let [xs (set xs)]
                    (s/setval
                     (s/multi-path
                      [nav/VALID-UI nav/SELECTED-OBJECTS xs]
                      [nav/WORLD (s/multi-path
                                  [nav/FACTORIES nav/FACTORY->ITEMS xs]
                                  [nav/RECIPES nav/RECIPE->ITEMS xs]
                                  (nav/items xs))])
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
                (s/setval [nav/WORLD (nav/valid-factory id) nav/NAME] v db)))
  
  (reg-event-db :update-factory-desired-output [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-factory id) nav/DESIRED-OUTPUT-QM] v db)))

  (reg-event-db :update-recipe-input [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-recipe id) nav/INPUT-QM] v db)))
  
  (reg-event-db :update-recipe-output [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-recipe id) nav/OUTPUT-QM] v db)))
  
  (reg-event-db :update-recipe-catalysts [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-recipe id) nav/CATALYSTS-QM] v db)))
  
  (reg-event-db :update-recipe-machines [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-recipe id) nav/RECIPE-MACHINE-LIST] v db)))
  
  (reg-event-db :update-recipe-duration [(undoable)]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-recipe id) nav/DURATION] v db)))


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
                (fn [db [_ page]] (s/multi-transform [nav/VALID-UI (s/multi-path [nav/SELECTED-PAGE (s/terminal-val page)]
                                                                           [nav/SELECTED-OBJECT-LIST (s/terminal-val [])])] db)))
  
  (reg-event-db :select-objects
                (fn [db [_ xs]] (s/setval [nav/VALID-UI nav/SELECTED-OBJECT-LIST] xs db))))

