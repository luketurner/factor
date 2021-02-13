(ns factor.factory
  (:require [re-frame.core :refer [reg-event-db reg-sub dispatch subscribe]]
            [factor.util :refer [new-uuid]]
            [factor.item :refer [item-rate-list item-rate-list-editor]]))

(defn factory []
  {:name "Unnamed Factory"
   :input {}
   :output {}})

(reg-event-db :create-factory (fn [db] (assoc-in db [:factories (new-uuid)] (factory))))
(reg-event-db :update-factory (fn [db [_ id factory]] (assoc-in db [:factories id] factory)))
(reg-event-db :delete-factory (fn [db [_ id]] (update-in db [:factories] dissoc id)))
(reg-sub :factory (fn [db [_ id]] (get-in db [:factories id])))
(reg-sub :factory-ids (fn [db] (-> db (get-in [:factories]) (keys))))


(defn factory-editor [factory-id]
  (let [{:keys [name input output] :as factory} @(subscribe [:factory factory-id])
        upd #(dispatch [:update-factory factory-id %])]
    [:div
     [:h2
      [:input {:class "factory-name" :type "text" :value name :on-change #(upd (assoc factory :name (.-value (.-target %))))}]
      [:button {:on-click #(dispatch [:delete-factory factory-id])} "Delete"]]
     [:dl
      [:dt "Outputs"]
      [:dd [item-rate-list-editor output #(upd (assoc factory :output %))]]
      [:dt "Inputs"]
      [:dd [item-rate-list input]]
      [:dt "Recipes"]
      [:dd
       [:details [:summary "Smelt steel" [:button "Avoid"]]
        [:dl
         [:dt "Inputs"]
         [:dd [:ul
               [:li "2 Iron ore"]
               [:li "1 Coal ore"]]]
         [:dt "Outputs"]
         [:dd [:ul
               [:li "2 Steel ore"]]]
         [:dt "Machines"]
         [:dd [:ul
               [:li "Smelter"]]]]]]
      [:dt "Consumption Tree"]
      [:dd "..."]]]))


(defn factory-page []
  (let [factories @(subscribe [:factory-ids])]
    [:div
     (if (not-empty factories)
       (into [:div] (for [fact-id factories] [factory-editor fact-id]))
       [:h2 "factories" [:p "You don't have any factories."]])
     [:button {:on-click #(dispatch [:create-factory])} "Add factory"]]))