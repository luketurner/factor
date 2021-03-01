(ns factor.components.factory
  (:require [re-frame.core :refer [dispatch subscribe]]
            [factor.components.widgets :as w]
            [factor.components.item :as item]
            [factor.components.recipe :as recipe]
            [factor.components.machine :as machine]))

(defn editor [factory-id]
  (let [{:keys [name desired-output] :as factory} @(subscribe [:factory factory-id])
        {:keys [input output recipes machines]} @(subscribe [:factory-calc factory-id])
        upd #(dispatch [:update-factory factory-id %])]
    [:div
     [:h2
      [:input {:class "factory-name" :type "text" :value name :on-change #(upd (assoc factory :name (.-value (.-target %))))}]
      [w/button {:on-click #(dispatch [:delete-factory factory-id])} "Delete"]]
     [:dl
      [:dt "Desired Outputs"]
      [:dd [item/rate-editor-list desired-output #(upd (assoc factory :desired-output %))]]
      [:dt "Outputs"]
      [:dd [item/rate-list output]]
      [:dt "Inputs"]
      [:dd [item/rate-list input]]
      [:dt "Recipes"]
      [:dd [recipe/editor-list recipes]]
      [:dt "Machines"]
      [:dd [machine/list machines]]
      [:dd]]]))