(ns factor.events
  "Defines all the events used by Factor."
  (:require [re-frame.core :refer [inject-cofx reg-event-db reg-event-fx path]]
            [day8.re-frame.undo :refer [undoable]]
            [com.rpl.specter :as s]
            [factor.navs :as nav]
            [factor.schema :refer [make edn-decode edn-encode json-decode Config Factory Item Recipe Machine World AppDb]]
            [factor.util :refer [json->clj edn->clj new-uuid route->url]]
            [factor.interceptors :refer [->purge-redos ->purge-undos ->fragment-updater ->focuser]]))

(defn reg-all []

  ;; General events

  (reg-event-db :initialize-db (fn [] (make AppDb nil)))

  ;; World-mutating events
  ;; Undoable


  (reg-event-db :create-factory [(undoable "New factory")
                                 (->fragment-updater)]
                (fn [db [_ factory]]
                  (let [id (new-uuid)
                        factory (->> factory
                                     (merge {:created-at (.now js/Date) :id id})
                                     (make Factory))]
                    (s/multi-transform
                     [(s/multi-path
                       [nav/VALID-UI nav/PAGE-ROUTE (s/terminal-val [:factory id])]
                       [nav/WORLD (nav/valid-factory id) (s/terminal-val factory)])]
                     db))))

  (reg-event-db :create-item [(undoable "New item")]
                (fn [db [_ item]]
                  (let [id (new-uuid)
                        item (->> item
                                  (merge {:created-at (.now js/Date) :id id})
                                  (make Item))]
                    (s/setval [nav/WORLD (nav/valid-item id)] item db))))

  (reg-event-db :create-recipe [(undoable "New recipe")]
                (fn [db [_ recipe]]
                  (let [id (new-uuid)
                        recipe (->> recipe
                                    (merge {:created-at (.now js/Date) :id id})
                                    (make Recipe))]
                    (s/setval [nav/WORLD (nav/valid-recipe id)] recipe db))))

  (reg-event-db :create-machine [(undoable "New machine")]
                (fn [db [_ machine]]
                  (let [id (new-uuid)
                        machine (->> machine
                                     (merge {:created-at (.now js/Date) :id id})
                                     (make Machine))]
                    (s/setval [nav/WORLD (nav/valid-machine id)] machine db))))

  (reg-event-db :update-factory [(undoable "Factory changed")]
                (fn [db [_ {:keys [id] :as x}]]
                  (s/setval [nav/WORLD (nav/valid-factory id)] x db)))

  (reg-event-db :update-item    [(undoable "Item changed")]
                (fn [db [_ {:keys [id] :as x}]]
                  (s/setval [nav/WORLD (nav/valid-item id)] x db)))

  (reg-event-db :update-machine [(undoable "Machine changed")]
                (fn [db [_ {:keys [id] :as x}]]
                  (s/setval [nav/WORLD (nav/valid-machine id)] x db)))

  (reg-event-db :update-recipe  [(undoable "Recipe changed")]
                (fn [db [_ {:keys [id] :as x}]]
                  (s/setval [nav/WORLD (nav/valid-recipe id)] x db)))

  (reg-event-db :delete-factories [(undoable "Delete factory")
                                   (->fragment-updater)]
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
                       nav/VALID-UI nav/PAGE-ROUTE (s/selected? [s/FIRST (s/pred= :factory)]) (s/nthpath 1) xs (s/terminal identity)])
                     w))))

  (reg-event-db :delete-recipes [(undoable "Delete recipes")]
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

  (reg-event-db :delete-machines [(undoable "Delete machines")]
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

  (reg-event-db :delete-items [(undoable "Delete items")]
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
                [(undoable "Reset world")]
                (fn [db [_ w]] (assoc db :world (make World w))))

  (reg-event-db :load-world-from-json
                [(undoable "Load world")]
                (fn [db [_ x]] (->> x
                                    (json->clj)
                                    (json-decode World)
                                    #(s/setval [nav/VALID-WORLD] % db))))

  (reg-event-db :load-world-from-edn
                [(undoable "Load world")]
                (fn [db [_ x]] (->> x
                                    (edn->clj)
                                    (edn-decode World)
                                    #(s/setval [nav/VALID-WORLD] % db))))

  (reg-event-db :update-factory-name [(undoable "Change factory name")]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-factory id) nav/NAME] v db)))

  (reg-event-db :update-factory-desired-output [(undoable "Change factory desired output")]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-factory id) nav/DESIRED-OUTPUT-QM] v db)))

  (reg-event-db :update-factory-filter [(undoable "Change factory filter")]
                (fn [db [_ id k v]]
                  (s/setval [nav/WORLD (nav/valid-factory id) nav/FILTER k] v db)))

  (reg-event-db :update-recipe-input [(undoable "Change recipe input")]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-recipe id) nav/INPUT-QM] v db)))

  (reg-event-db :update-recipe-output [(undoable "Change recipe output")]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-recipe id) nav/OUTPUT-QM] v db)))

  (reg-event-db :update-recipe-catalysts [(undoable "Change recipe catalysts")]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-recipe id) nav/CATALYSTS-QM] v db)))

  (reg-event-db :update-recipe-machines [(undoable "Change recipe machines")]
                (fn [db [_ id v]]
                  (s/setval [nav/WORLD (nav/valid-recipe id) nav/RECIPE-MACHINE-LIST] v db)))

  (reg-event-db :update-recipe-duration [(undoable "Change recipe duration")]
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

  (reg-event-db :update-route
                [(->fragment-updater)]
                (fn [db [_ route]] (s/multi-transform [nav/VALID-UI (s/multi-path [nav/PAGE-ROUTE (s/terminal-val route)]
                                                                                  [nav/SELECTED-OBJECT-LIST (s/terminal-val [])])] db)))
  
  (reg-event-db :focus
                [(->focuser)]
                (fn [db [_ el-id]] (s/setval [nav/VALID-UI nav/FOCUSED] el-id db)))

  (reg-event-db :update-omnibar-query (fn [db [_ v]] (s/setval [nav/VALID-UI nav/OMNIBAR-QUERY] v db)))
  (reg-event-db :open-command-palette (fn [db] (s/multi-transform [nav/VALID-UI nav/OMNIBAR-STATE
                                                                   (s/multi-path [nav/MODE (s/terminal-val :command-palette)]
                                                                                 [nav/QUERY (s/terminal-val "")])]
                                                                  db)))
  (reg-event-db :close-omnibar (fn [db] (s/setval [nav/VALID-UI nav/OMNIBAR-MODE] :closed db)))

  (reg-event-db :set-app-menu (fn [db [_ v]] (s/setval [nav/VALID-UI nav/APP-MENU] v db)))


  (reg-event-db :select-objects
                (fn [db [_ xs]] (s/setval [nav/VALID-UI nav/SELECTED-OBJECT-LIST] xs db)))

  ;; Command events
  ;; Sometimes undoable

  (reg-event-db :omnibar-open-factory
                (fn [db] (s/multi-transform
                          [nav/VALID-UI nav/OMNIBAR-STATE
                           (s/multi-path
                            [nav/MODE (s/terminal-val :open-factory)]
                            [nav/QUERY (s/terminal-val "")])]
                          db)))

  (reg-event-db :omnibar-create-factory
                (fn [db] (s/multi-transform
                          [nav/VALID-UI nav/OMNIBAR-STATE
                           (s/multi-path
                            [nav/MODE (s/terminal-val :create-factory)]
                            [nav/QUERY (s/terminal-val "")])]
                          db)))

  (reg-event-db :omnibar-delete-factory
                (fn [db] (s/multi-transform
                          [nav/VALID-UI nav/OMNIBAR-STATE
                           (s/multi-path
                            [nav/MODE (s/terminal-val :delete-factory)]
                            [nav/QUERY (s/terminal-val "")])]
                          db))))

