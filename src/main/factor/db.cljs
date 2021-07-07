(ns factor.db
  (:require [re-frame.core :refer [->interceptor dispatch-sync]]
            [malli.core :refer [validate explain]]
            [medley.core :refer [dissoc-in]]
            [factor.util :refer [add-fx]]
            [factor.world :as w]))

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
               [:name :string]]]]
            [:machines
             [:map-of :string
              [:map {:closed true}
               [:id :string]
               [:name :string]
               [:power number?]
               [:speed number?]]]]
            [:recipes
             [:map-of :string
              [:map {:closed true}
               [:id :string]
               [:name :string]
               [:input [:map-of :string number?]]
               [:output [:map-of :string number?]]
               [:machines [:set :string]]]]]]]
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
