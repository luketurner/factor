(ns factor.components.factory
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as reagent]
            [factor.components.widgets :as w]
            [factor.components.item :as item]
            [factor.components.recipe :as recipe]
            [factor.components.machine :as machine]
            [clojure.string :as string]))

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
      [:dd [machine/list machines]]]]))

(defn factory-list [{:keys [search]}]
  (let [factory-names @(subscribe [:factory-names])]
    (into [:ul]
          (for [[name id] factory-names :when (or (empty? search) (string/includes? name search))]
            [:li 
             [:a {:href "#" :on-click #(dispatch [:select-object :factory id])} name]]))))

(defn sub-nav []
  (let [search (reagent/atom "")]
   (fn []
     [:<>
      [:h2 " "]
      [:input {:type "text" :value @search :on-change #(->> % (.-target) (.-value) (reset! search))}]
      [:div.scrollable
       [factory-list {:search @search}]]])))