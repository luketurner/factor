(ns factor.recipe.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [factor.recipe.components :refer [recipe-editor]]
            [factor.widgets :refer [button]]))

(defn recipe-page []
  (let [recipes @(subscribe [:recipe-ids])]
    [:div
     [:h2 "recipes"]
     (if (not-empty recipes)
       (into [:div] (for [id recipes] [recipe-editor id]))
       [:p "You don't have any recipes."])
     [button {:on-click [:create-recipe :expanded]} "Add recipe"]]))