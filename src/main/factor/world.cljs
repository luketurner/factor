(ns factor.world
  (:require [re-frame.core :refer [subscribe reg-sub dispatch reg-event-db]]
            [clojure.edn :as edn]))

(reg-event-db :world-import (fn [db [_ w]] (assoc db :world w)))
(reg-event-db :world-reset (fn [db] (assoc db :world {})))
(reg-sub :world-data (fn [db _] (get db :world)))

(defn world->str [world] (pr-str world))
(defn str->world [s] (edn/read-string s))

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
