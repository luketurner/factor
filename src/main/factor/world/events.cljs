(ns factor.world.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx reg-global-interceptor ->interceptor dispatch]]
            [factor.world :refer [empty-world]]))

(reg-event-db :world-import (fn [db [_ w]] (assoc db :world w)))
(reg-event-db :world-reset (fn [db] (assoc db :world {})))

(reg-event-fx
 :load-world
 [(inject-cofx :localstorage :world)]
 (fn [{{world :world} :localstorage db :db :as foo}]
   {:db (assoc db :world (if (not-empty world) world empty-world))}))

(reg-event-fx
 :save-world
 (fn [_ [_ world]]
   {:localstorage {:world world}}))

(reg-global-interceptor
 (->interceptor
  :id :world-saver
  :after (fn [{{{old-world :world} :db} :coeffects
               {{new-world :world} :db} :effects :as context}]
           (when (and new-world (not= new-world old-world))
             (dispatch [:save-world new-world]))
           context)))