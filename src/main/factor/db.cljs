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
               [:machines [:map-of :string number?]]
               [:hard-denied-machines [:set :string]]
               [:soft-denied-machines [:set :string]]
               [:hard-denied-recipes [:set :string]]
               [:soft-denied-recipes [:set :string]]
               [:hard-denied-items [:set :string]]
               [:soft-denied-items [:set :string]]
               [:hard-allowed-machines [:set :string]]
               [:soft-allowed-machines [:set :string]]
               [:hard-allowed-recipes [:set :string]]
               [:soft-allowed-recipes [:set :string]]
               [:hard-allowed-items [:set :string]]
               [:soft-allowed-items [:set :string]]]]]
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
               [:machines [:vector :string]]
               [:duration number?]
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

(defn add-duration-to-recipes
  "All recipes without a :duration key will have the key initialized to 1."
  [db]
  (s/setval [(s/keypath :world :recipes) s/MAP-VALS (s/keypath :duration) (s/pred nil?)] 1 db))

(defn switch-recipe-machines-to-vector
  "All recipes with a :machines that is a set will have the value converted to a vector."
  [db]
  (s/transform [(s/keypath :world :recipes) s/MAP-VALS (s/keypath :machines) (s/pred set?)] vec db))

(defn add-allow-deny-lists-to-factories
  "Adds the hard/soft allow/deny lists for items, machines, and recipes to factory maps where they aren't
   already set."
  [db]
  (->> db
       (s/multi-transform [(s/keypath :world :factories)
                           s/MAP-VALS
                           (s/multi-path
                            [(s/keypath :hard-denied-machines) (s/pred nil?) (s/terminal-val #{})]
                            [(s/keypath :soft-denied-machines) (s/pred nil?) (s/terminal-val #{})]
                            [(s/keypath :hard-denied-recipes) (s/pred nil?) (s/terminal-val #{})]
                            [(s/keypath :soft-denied-recipes) (s/pred nil?) (s/terminal-val #{})]
                            [(s/keypath :hard-denied-items) (s/pred nil?) (s/terminal-val #{})]
                            [(s/keypath :soft-denied-items) (s/pred nil?) (s/terminal-val #{})]
                            [(s/keypath :hard-allowed-machines) (s/pred nil?) (s/terminal-val #{})]
                            [(s/keypath :soft-allowed-machines) (s/pred nil?) (s/terminal-val #{})]
                            [(s/keypath :hard-allowed-recipes) (s/pred nil?) (s/terminal-val #{})]
                            [(s/keypath :soft-allowed-recipes) (s/pred nil?) (s/terminal-val #{})]
                            [(s/keypath :hard-allowed-items) (s/pred nil?) (s/terminal-val #{})]
                            [(s/keypath :soft-allowed-items) (s/pred nil?) (s/terminal-val #{})])])))

(defn config-set-default-units
  "Sets default values for units that were added in settings. (Without this migration, the units are empty
   until the user goes into settings and configures them.)"
  [db]
  (->> db
       (s/multi-transform [(s/keypath :config :unit)
                           (s/multi-path
                            [(s/keypath :item-rate) (s/pred nil?) (s/terminal-val "items/sec")]
                            [(s/keypath :power) (s/pred nil?) (s/terminal-val "W")]
                            [(s/keypath :energy) (s/pred nil?) (s/terminal-val "J")])])))

(defn migrate-database
  "A function that accepts user-provided database contents, which might have been generated
   from an earlier version of Factor (e.g. when importing a world or loading from local storage),
   and returns an updated database that's been \"migrated\" to the latest Factor schema.
   
   All migrations performed in this function are idempotent. See the docstring for each migration function
   for details about why that particular migration is required.
   
   For convenience, consider using the `->migrate-database` interceptor to easily add migrations to an existing event."
  [db]
  (->> db
       (add-catalysts-map-to-recipes)
       (add-duration-to-recipes)
       (switch-recipe-machines-to-vector)
       (add-allow-deny-lists-to-factories)
       (config-set-default-units)))

(defn ->migrate-database
  "An interceptor that runs the `migrate-database` function on the :db effect after the event executes.
   
   Note, if the event also has a validation interceptor, this should run before that
   (meaning it should be AFTER that in the interceptor list)."
  []
  (->interceptor
   :id :migrate-database
   :after (fn [ctx]
            (->> (get-effect ctx :db)
                 (migrate-database)
                 (assoc-effect ctx :db)))))

(defn migrate-config
  "A function that accepts user-provided database contents, which might have been generated
   from an earlier version of Factor (e.g. when loading from local storage),
   and returns an updated database where the :config key is updated to the latest version of Factor's schema.
   
   (This function only updates the :config key -- it's intended for use with config-loading events specifically.)"
  [db]
  (->> db
       (config-set-default-units)))

(defn ->migrate-config
  "An interceptor that runs the `migrate-config` function on the :db effect after the event executes.
   
   Note, if the event also has a validation interceptor, this should run before that
   (meaning it should be AFTER that in the interceptor list)."
  []
  (->interceptor
   :id :migrate-config
   :after (fn [ctx]
            (->> (get-effect ctx :db)
                 (migrate-config)
                 (assoc-effect ctx :db)))))