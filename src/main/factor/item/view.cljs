(ns factor.item.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [factor.item.components :refer [item-editor]]
            [factor.widgets :refer [hotkeys]]))


(defn item-page []
  (let [items @(subscribe [:item-ids])]
    [:div
     [:h2 "items"]
     [hotkeys {"enter" [:create-item]}
      (if (not-empty items)
        (into [:div]
              (for [id items]
                [item-editor id]))
        [:p "You don't have any items."])]
     [:button {:on-click #(dispatch [:create-item])} "Add item"]]))