(ns factor.db
  (:require [re-frame.core :refer [->interceptor dispatch-sync]]
            [malli.core :refer [validate explain]]
            [medley.core :refer [dissoc-in]]
            [factor.util :refer [add-fx]]))

(defn init []
  (dispatch-sync [:initialize-db])
  (dispatch-sync [:load-world]))

(def schema
  [:map {:closed true}
   [:world [:map {:closed true}
            [:factories
             [:map-of :string
              [:map {:closed true}
               [:name :string]
               [:desired-output [:map-of :string number?]]
               [:output [:map-of :string number?]]
               [:input [:map-of :string number?]]
               [:recipes [:map-of :string number?]]
               [:machines [:map-of :string number?]]]]]
            [:items
             [:map-of :string
              [:map {:closed true}
               [:name :string]]]]
            [:machines
             [:map-of :string
              [:map {:closed true}
               [:name :string]
               [:power number?]
               [:speed number?]]]]
            [:recipes
             [:map-of :string
              [:map {:closed true}
               [:name :string]
               [:input [:map-of :string number?]]
               [:output [:map-of :string number?]]
               [:machines [:set :string]]]]]]]
   [:config [:map]]
   [:ui [:map]]])

(defn ->validator [schema]
  (->interceptor
   :id :db-validator
   :after (fn [{{db :db} :effects :as ctx}]
            (let [invalid? (and db (not (validate schema db)))]
              (cond-> ctx
                invalid? (dissoc-in [:effects :db])
                invalid? (add-fx [:toast (str "validate failed: " (pr-str (explain schema db)))]))))))

