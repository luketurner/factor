(ns factor.factory
  (:require [re-frame.core :refer [reg-event-db reg-sub dispatch subscribe]]
            [factor.util :refer [new-uuid]]
            [factor.item :refer [item-rate-list item-rate-list-editor]]))

(defn factory []
  {:name "Unnamed Factory"
   :input {}
   :output {}})

(reg-event-db :create-factory (fn [db] (assoc-in db [:world :factories (new-uuid)] (factory))))
(reg-event-db :update-factory (fn [db [_ id factory]] (assoc-in db [:world :factories id] factory)))
(reg-event-db :delete-factory (fn [db [_ id]] (update-in db [:world :factories] dissoc id)))
(reg-sub :factory (fn [db [_ id]] (get-in db [:world :factories id])))
(reg-sub :factory-ids (fn [db] (-> db (get-in [:world :factories]) (keys))))


(defn factory-editor [factory-id]
  (let [{:keys [name input output desired-output] :as factory} @(subscribe [:factory factory-id])
        upd #(dispatch [:update-factory factory-id %])]
    [:div
     [:h2
      [:input {:class "factory-name" :type "text" :value name :on-change #(upd (assoc factory :name (.-value (.-target %))))}]
      [:button {:on-click #(dispatch [:delete-factory factory-id])} "Delete"]]
     [:dl
      [:dt "Desired Outputs"]
      [:dd [item-rate-list-editor desired-output #(upd (assoc factory :desired-output %))]]
      [:dt "Outputs"]
      [:dd [item-rate-list output]]
      [:dt "Inputs"]
      [:dd [item-rate-list input]]
      [:dt "Recipes"]
      [:dd "No recipes."]
      [:dd]]]))


(defn factory-page []
  (let [factories @(subscribe [:factory-ids])]
    [:div
     (if (not-empty factories)
       (into [:div] (for [fact-id factories] [factory-editor fact-id]))
       [:div [:h2 "factories"] [:p "You don't have any factories."]])
     [:button {:on-click #(dispatch [:create-factory])} "Add factory"]]))