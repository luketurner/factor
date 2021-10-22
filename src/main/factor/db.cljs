(ns factor.db
  (:require [re-frame.core :refer [->interceptor dispatch-sync get-effect assoc-effect]]
            [malli.core :refer [validate explain]]
            [medley.core :refer [dissoc-in]]
            [factor.util :refer [add-fx]]
            [factor.world :as w]
            [com.rpl.specter :as s]))

(defn init []
  (dispatch-sync [:initialize-db])
  (dispatch-sync [:world-load w/empty-world])
  (dispatch-sync [:config-load {}]))

(def schema
  [:map {:closed true}
   [:world [:map {:closed true}
            [:factories
             [:map-of :string
              [:map {:closed true}
               [:id :string]
               [:name :string]
               [:desired-output [:map-of :string number?]]
               [:output [:map-of :string number?]]
               [:input [:map-of :string number?]]
               [:recipes [:map-of :string number?]]
               [:machines [:map-of :string number?]]]]]
            [:items
             [:map-of :string
              [:map {:closed true}
               [:id :string]
               [:name :string]
               [:created-at number?]]]]
            [:machines
             [:map-of :string
              [:map {:closed true}
               [:id :string]
               [:name :string]
               [:power number?]
               [:speed number?]
               [:created-at number?]]]]
            [:recipes
             [:map-of :string
              [:map {:closed true}
               [:id :string]
               [:name :string]
               [:input [:map-of :string number?]]
               [:output [:map-of :string number?]]
               [:catalysts [:map-of :string number?]]
               [:machines [:set :string]]
               [:created-at number?]]]]]]
   [:config [:map]]
   [:ui [:map]]])

(defn ->world-validator [schema]
  (->interceptor
   :id :db-validator
   :after (fn [{{db :db} :effects :as ctx}]
            (let [invalid? (and db (not (validate schema db)))]
              (cond-> ctx
                invalid? (dissoc-in [:effects :db])
                invalid? (add-fx [:toast (str "validate failed: " (pr-str (explain schema db)))]))))))

(defn ->world-saver []
  (->interceptor
   :id :world-saver
   :after (fn [{{{old-world :world} :db} :coeffects
                {{new-world :world} :db} :effects :as context}]
            (if (and new-world (not= new-world old-world))
              (add-fx context [:dispatch [:world-save new-world]])
              context))))

(defn ->config-saver []
  (->interceptor
   :id :config-saver
   :after (fn [{{{old-config :config} :db} :coeffects
                {{new-config :config} :db} :effects :as context}]
            (if (and new-config (not= new-config old-config))
              (add-fx context [:dispatch [:config-save new-config]])
              context))))

(defn add-catalysts-map-to-recipes
  "All recipes without a :catalysts key will have the key initialized to {}.
   This is needed because :catalysts was added as a required key, and
   users may have previously created recipes without this key."
  [db]
  (s/setval [(s/keypath :world :recipes) s/MAP-VALS (s/keypath :catalysts) (s/pred nil?)] {} db))

(defn migrate-database
  "A function that accepts user-provided database contents, which might have been generated
   from an earlier version of Factor (e.g. when importing a world or loading from local storage),
   and returns an updated database that's been \"migrated\" to the latest Factor schema.
   
   All migrations performed in this function are idempotent. See the docstring for each migration function
   for details about why that particular migration is required.
   
   For convenience, consider using the `->migrate-database` interceptor to easily add migrations to an existing event."
  [db]
  (->> db
       (add-catalysts-map-to-recipes)))

(defn ->migrate-database
  "An interceptor that runs the `migrate-database` function on the :db effect after the event executes.
   
   Note, if the event also has a validation interceptor, this should run before that."
  []
  (->interceptor
   :id :migrate-database
   :after (fn [ctx]
            (->> (get-effect ctx :db)
                 (migrate-database)
                 (assoc-effect ctx :db)))))