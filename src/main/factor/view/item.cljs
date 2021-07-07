(ns factor.view.item
  (:require [factor.components :as c]
            [re-frame.core :refer [dispatch subscribe]]
            [factor.world :as w]))

(defn get-selected-ids [ev]
  (-> ev
      (:api)
      (.getSelectedRows)
      (js->clj)
      (->> (map #(get % "id")))
      (vec)))

(defn selected-text [num] (str "(" num " items selected)"))

(defn item-grid [items]
  (let [update-item #(dispatch [:update-item (:data %)])
        update-selection #(dispatch [:ui [:item-page :selected] %])]
    [c/grid {:row-data items
             :on-grid-ready #(update-selection [])
             :on-row-value-changed update-item
             :on-selection-changed #(-> % (get-selected-ids) (update-selection))
             :column-defs [{:checkboxSelection true}
                           {:field :id}
                           {:field :name :editable true}]}]))

(defn navbar []
  (let [create-item    #(dispatch [:update-item (w/new-item)])
        selected-items @(subscribe [:ui [:item-page :selected]])
        delete-items   #(do
                          (dispatch [:delete-items selected-items])
                          (dispatch [:ui [:item-page :selected] []]))
        num-selected   (count selected-items)]
    [c/navbar
     [c/navbar-group-left
      [c/navbar-heading "Item List"]
      [c/button {:class :bp3-minimal :on-click create-item :icon :plus :title "Add item"}]
      [c/navbar-divider]
      (when (< 0 num-selected)
        [:<>
         [:div (selected-text num-selected)]
         [c/button {:class :bp3-minimal
                    :on-click delete-items
                    :icon :minus :text "Delete"
                    :disabled (= num-selected 0)}]])]]))

(defn page []
  (let [all-items @(subscribe [:item-seq])]
    [item-grid all-items]))
