(ns factor.world
  (:require [re-frame.core :refer [reg-cofx inject-cofx reg-event-fx reg-global-interceptor ->interceptor reg-fx subscribe reg-sub dispatch reg-event-db]]
            [clojure.edn :as edn]))

(def empty-world {:items {} :machines {} :recipes {} :factories {}})

(defn world->str [world] (pr-str world))
(defn str->world [s] (edn/read-string s))

(reg-sub :world-data (fn [db _] (get db :world)))

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



(defn world-page []
  (let [item-count (or @(subscribe [:item-count]) "No")
        machine-count (or @(subscribe [:machine-count]) "No")
        recipe-count (or @(subscribe [:recipe-count]) "No")
        world-data (world->str @(subscribe [:world-data]))
        update-world-data #(dispatch [:world-import (str->world (.-value (.-target %)))])
        wipe-world #(dispatch [:world-reset])]
    [:div
     [:h2 "world"]
     [:dl
      [:dt "Statistics"]
      [:dd
       [:ul
        [:li (str item-count " items")]
        [:li (str machine-count " machines")]
        [:li (str recipe-count " recipes")]]]
      [:dt "Import"]
      [:dd
       [:p "Paste world data: " [:input {:type "text" :on-change update-world-data}]]]
      [:dt "Export"]
      [:dd
       [:p "Copy world data: " [:input {:type "text" :read-only true :value world-data}]]]
      [:dt "Wipe World Data"]
      [:dd
       [:button {:on-click wipe-world} "DELETE WORLD PERMANENTLY"]]]]))
