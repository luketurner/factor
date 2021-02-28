(ns factor.item.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [factor.item.components :refer [item-editor]]
            [factor.widgets :refer [list-editor deletable-row]]))


(defn item-page []
  (let [items @(subscribe [:item-ids])]
    [:div
     [:h2 "items"]
     [list-editor {:data items
                   :row-fn (fn [id] [deletable-row {:on-delete [:delete-item id]}
                                     [item-editor id (= id (last items))]])
                   :empty-message [:p "You don't have any items."]
                   :add-fn #(dispatch [:create-item])}]]))