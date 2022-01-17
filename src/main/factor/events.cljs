(ns factor.events
  "Defines all the events used by Factor."
  (:require [re-frame.core :refer [inject-cofx reg-event-db reg-event-fx path]]
            [day8.re-frame.undo :refer [undoable]]
            [com.rpl.specter :as s]
            [factor.navs :as nav]
            [factor.schema :refer [make edn-decode edn-encode json-decode Config Factory Item Recipe Machine World AppDb]]
            [factor.util :refer [json->clj edn->clj new-uuid route->url]]
            [factor.interceptors :refer [->purge-redos ->purge-undos ->fragment-updater ->focuser]]
            [cljs.core.match :refer [match]]
            [factor.cmds :as cmds]))

(defn reg-all []

  ;; General events

  (reg-event-db :initialize-db (fn [] (make AppDb nil)))

  ;; Command events

  (reg-event-db :open-command-palette (fn [db] (s/multi-transform [nav/VALID-UI nav/OMNIBAR-STATE
                                                                   (s/multi-path [nav/MODE (s/terminal-val :command-palette)]
                                                                                 [nav/QUERY (s/terminal-val "")])]
                                                                  db)))

  (reg-event-db :new-factory [(undoable "New factory")
                              (->fragment-updater)]
                (fn [db [_ name]]
                  (let [id (new-uuid)
                        factory (make Factory {:created-at (.now js/Date)
                                               :id id
                                               :name name})]
                    (s/multi-transform
                     [(s/multi-path
                       [nav/VALID-UI nav/PAGE-ROUTE (s/terminal-val [:factory id])]
                       [nav/WORLD (nav/valid-factory id) (s/terminal-val factory)])]
                     db))))

  (reg-event-db :open-factory
                [(->fragment-updater)]
                (fn [db [_ id]]
                  (s/setval [nav/VALID-UI
                             nav/PAGE-ROUTE]
                            [:factory id] db)))

  (reg-event-db :open-item-editor [(->fragment-updater)] (fn [db] (s/setval [nav/VALID-UI nav/PAGE-ROUTE] [:items] db)))
  (reg-event-db :open-recipe-editor [(->fragment-updater)] (fn [db] (s/setval [nav/VALID-UI nav/PAGE-ROUTE] [:recipes] db)))
  (reg-event-db :open-machine-editor [(->fragment-updater)] (fn [db] (s/setval [nav/VALID-UI nav/PAGE-ROUTE] [:machines] db)))
  (reg-event-db :open-import-export [(->fragment-updater)] (fn [db] (s/setval [nav/VALID-UI nav/PAGE-ROUTE] [:settings] db)))
  (reg-event-db :open-help [(->fragment-updater)] (fn [db] (s/setval [nav/VALID-UI nav/PAGE-ROUTE] [:help] db)))

  (reg-event-db :delete-world [(undoable "Delete world")] (fn [db] (s/setval [nav/WORLD] (make World nil) db)))

  (reg-event-db :delete-factory [(undoable "Delete factory")
                                 (->fragment-updater)]
                (fn [db [_ id]]
                  (s/multi-transform
                   (s/multi-path
                    [nav/VALID-WORLD (nav/factories [id]) (s/terminal-val s/NONE)]
                      ; the below path benefit from some explaining. Basically, if
                      ; the open factory was deleted, we want to open another,
                      ; not-deleted factory if possible. The (collect-one) navigator
                      ; collects the ID of another factory to open. The (s/terminal identity)
                      ; line will then set the value of the navigated path to that collected value.
                      ; Because multi-path transforms are run in order, the deleted
                      ; factories have already been removed from FACTORIES-MAP when this runs.
                    [(s/collect-one nav/WORLD nav/FACTORIES-MAP s/FIRST s/FIRST)
                     nav/VALID-UI nav/PAGE-ROUTE
                     (s/if-path (s/collected? [v] (some? v))
                                [(s/selected? [s/FIRST (s/pred= :factory)])
                                 (s/nthpath 1)
                                 (s/pred= id)
                                 (s/terminal identity)]
                                (s/terminal-val [:home]))])
                   db)))

  (reg-event-db :delete-open-factory [(undoable "Delete open factory")
                                      (->fragment-updater)]
                (fn [db]
                  (let [id (s/select-any [nav/UI
                                          nav/PAGE-ROUTE
                                          (s/selected? [s/FIRST (s/pred= :factory)])
                                          (s/nthpath 1)]
                                     db)]
                    (s/multi-transform
                     (s/multi-path
                      [nav/VALID-WORLD (nav/factories [id]) (s/terminal-val s/NONE)]
                      ; the below path benefit from some explaining. Basically, if
                      ; the open factory was deleted, we want to open another,
                      ; not-deleted factory if possible. The (collect-one) navigator
                      ; collects the ID of another factory to open. The (s/terminal identity)
                      ; line will then set the value of the navigated path to that collected value.
                      ; Because multi-path transforms are run in order, the deleted
                      ; factories have already been removed from FACTORIES-MAP when this runs.
                      [(s/collect-one nav/WORLD nav/FACTORIES-MAP s/FIRST s/FIRST)
                       nav/VALID-UI nav/PAGE-ROUTE
                       (s/if-path (s/collected? [v] (some? v))
                                  [(s/selected? [s/FIRST (s/pred= :factory)])
                                   (s/nthpath 1)
                                   (s/pred= id)
                                   (s/terminal identity)]
                                  (s/terminal-val [:home]))])
                     db))))


  (reg-event-db :new-item [(undoable "New item")]
                (fn [db [_ item]]
                  (let [id (new-uuid)
                        item (->> item
                                  (merge {:created-at (.now js/Date) :id id})
                                  (make Item))]
                    (s/setval [nav/WORLD (nav/valid-item id)] item db))))

  (reg-event-db :new-recipe [(undoable "New recipe")]
                (fn [db [_ recipe]]
                  (let [id (new-uuid)
                        recipe (->> recipe
                                    (merge {:created-at (.now js/Date) :id id})
                                    (make Recipe))]
                    (s/setval [nav/WORLD (nav/valid-recipe id)] recipe db))))

  (reg-event-db :new-machine [(undoable "New machine")]
                (fn [db [_ machine]]
                  (let [id (new-uuid)
                        machine (->> machine
                                     (merge {:created-at (.now js/Date) :id id})
                                     (make Machine))]
                    (s/setval [nav/WORLD (nav/valid-machine id)] machine db))))

  (reg-event-db :delete-selected-recipes [(undoable "Delete recipe(s)")]
                (fn [db]
                  (let [xs (s/select [nav/UI nav/SELECTED-OBJECTS (s/view set)] db)]
                    (s/setval
                     (s/multi-path
                      [nav/VALID-UI nav/SELECTED-OBJECTS]
                      [nav/WORLD (s/multi-path
                                  [nav/FACTORIES nav/FACTORY->RECIPES xs]
                                  (nav/recipes xs))])
                     s/NONE
                     db))))

  (reg-event-db :delete-selected-machines [(undoable "Delete machine(s)")]
                (fn [db]
                  (let [xs (s/select [nav/UI nav/SELECTED-OBJECTS (s/view set)] db)]
                    (s/setval
                     (s/multi-path
                      [nav/VALID-UI nav/SELECTED-OBJECTS]
                      [nav/WORLD (s/multi-path
                                  [nav/FACTORIES nav/FACTORY->MACHINES xs]
                                  [nav/RECIPES nav/RECIPE->MACHINES xs]
                                  (nav/machines xs))])
                     s/NONE
                     db))))

  (reg-event-db :delete-selected-items [(undoable "Delete item(s)")]
                (fn [db]
                  (let [xs (s/select [nav/UI nav/SELECTED-OBJECTS (s/view set)] db)]
                    (s/setval
                     (s/multi-path
                      [nav/VALID-UI nav/SELECTED-OBJECTS]
                      [nav/WORLD (s/multi-path
                                  [nav/FACTORIES nav/FACTORY->ITEMS xs]
                                  [nav/RECIPES nav/RECIPE->ITEMS xs]
                                  (nav/items xs))])
                     s/NONE
                     db))))

  (reg-event-db :change-item-rate-unit (fn [db [_ v]] (s/setval [nav/VALID-CONFIG nav/UNIT :item-rate] v db)))
  (reg-event-db :change-power-unit (fn [db [_ v]] (s/setval [nav/VALID-CONFIG nav/UNIT :power] v db)))
  (reg-event-db :change-energy-unit (fn [db [_ v]] (s/setval [nav/VALID-CONFIG nav/UNIT :energy] v db)))

  (reg-event-db :toggle-filter-view [(->fragment-updater)]
                (fn [db] (s/transform [nav/VALID-UI
                                       nav/PAGE-ROUTE
                                       (s/selected? s/FIRST (s/pred= :factory))
                                       (s/nthpath 2)] #(if (= % :filters) s/NONE :filters) db)))

  (reg-event-db :toggle-debug-view [(->fragment-updater)]
                (fn [db] (s/transform [nav/VALID-UI
                                       nav/PAGE-ROUTE]
                                      #(match %
                                         [:factory x :debug] [:factory x]
                                         [:factory x] [:factory x :debug]
                                         [:factory x _] [:factory x :debug]
                                         x                   x)
                                      db)))

  ;; World-mutating events
  ;; Undoable

  (reg-event-db :update-item    [(undoable "Item changed")]
                (fn [db [_ {:keys [id] :as x}]]
                  (s/setval [nav/WORLD (nav/valid-item id)] x db)))

  (reg-event-db :update-machine [(undoable "Machine changed")]
                (fn [db [_ {:keys [id] :as x}]]
                  (s/setval [nav/WORLD (nav/valid-machine id)] x db)))

  (reg-event-db :update-recipe  [(undoable "Recipe changed")]
                (fn [db [_ {:keys [id] :as x}]]
                  (s/setval [nav/WORLD (nav/valid-recipe id)] x db)))

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


  ;; Localstorage persistence events
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
  
  ;; Starts a command invocation. If the command has parameters, the omnibar is opened to prompt the user
  ;; for arguments. If there are no parameters, the command is invoked (dispatched) immediately.
  (reg-event-fx :cmd-invoke
                (fn [{:keys [db]} [_ cmd-id]]
                  (let [cmd (cmds/cmd cmd-id)]
                    (if (empty? (:params cmd))
                      {:fx [:dispatch (:ev cmd)]}
                      {:db (s/setval [nav/VALID-UI nav/OMNIBAR-STATE]
                                     {:mode :cmd-invocation
                                      :query ""
                                      :invocation {:cmd cmd-id
                                                   :params []}} db)}))))

  ;; Collects an argument's value for a currently executing command invocation.
  ;; If there are no more arguments to be collected, the command is invoked (dispatched) with all collected arguments.
  (reg-event-fx :cmd-invoke-collect
                (fn [{:keys [db]} [_ v]]
                    (let [invocation (s/select-any [nav/UI nav/OMNIBAR-STATE nav/INVOCATION] db)
                          params (s/select-any [nav/PARAM-LIST] invocation)
                          cmd (cmds/cmd (s/select-any [nav/CMD] invocation))
                          cmd-ev (into (:ev cmd) (conj params v))
                          more-params? (not= (inc (count params)) (count (:params cmd)))]
                      (if more-params?
                        ;; more params -- collect value and keep omnibar open
                        {:db (s/multi-transform
                              [nav/VALID-UI nav/OMNIBAR-STATE
                               (s/multi-path
                                [nav/QUERY (s/terminal-val "")]
                                [nav/INVOCATION nav/PARAM-LIST s/END (s/terminal-val [v])])]
                              db)}
                        ;; no more params -- reset omnibar and dispatch the event
                        {:db (s/setval [nav/VALID-UI nav/OMNIBAR-STATE]
                                       {:mode :closed}
                                       db)
                         :fx [[:dispatch cmd-ev]]}))))

  (reg-event-db :close-omnibar (fn [db] (s/setval [nav/VALID-UI nav/OMNIBAR-STATE] {:mode :closed} db)))

  (reg-event-db :set-app-menu (fn [db [_ v]] (s/setval [nav/VALID-UI nav/APP-MENU] v db)))

  (reg-event-db :select-objects
                (fn [db [_ xs]] (s/setval [nav/VALID-UI nav/SELECTED-OBJECT-LIST] xs db))))

