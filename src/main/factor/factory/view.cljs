(ns factor.factory.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [factor.recipe.components :refer [recipe-editor-list]]
            [factor.item.components :refer [item-rate-list item-rate-editor-list]]
            [factor.machine.components :refer [machine-list]]))

(defn factory-editor [factory-id]
  (let [{:keys [name input output desired-output recipes machines] :as factory} @(subscribe [:factory factory-id])
        upd #(dispatch [:update-factory factory-id %])]
    [:div
     [:h2
      [:input {:class "factory-name" :type "text" :value name :on-change #(upd (assoc factory :name (.-value (.-target %))))}]
      [:button {:on-click #(dispatch [:delete-factory factory-id])} "Delete"]]
     [:dl
      [:dt "Desired Outputs"]
      [:dd [item-rate-editor-list desired-output #(upd (assoc factory :desired-output %))]]
      [:dt "Outputs"]
      [:dd [item-rate-list output]]
      [:dt "Inputs"]
      [:dd [item-rate-list input]]
      [:dt "Recipes"]
      [:dd [recipe-editor-list recipes]]
      [:dt "Machines"]
      [:dd [machine-list machines]]
      [:dd]]]))

(defn factory-page []
  (let [factories @(subscribe [:factory-ids])]
    [:div
     (if (not-empty factories)
       (into [:div] (for [fact-id factories] [factory-editor fact-id]))
       [:div [:h2 "factories"] [:p "You don't have any factories."]])
     [:button {:on-click #(dispatch [:create-factory])} "Add factory"]]))