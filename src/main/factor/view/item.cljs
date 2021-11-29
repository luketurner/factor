(ns factor.view.item
  (:require [factor.components :as c]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :refer [with-let]]
            [factor.util :refer [callback-factory-factory]]))

(defn get-selected-ids [ev]
  (-> ev
      (:api)
      (.getSelectedRows)
      (js->clj)
      (->> (map #(get % "id")))
      (vec)))

(defn selected-text [num] (str "(" num " items selected)"))

(defn item-grid [items]
  (with-let [update-item #(dispatch [:update-item (:data %)])
             update-selection #(dispatch [:select-objects %])
             on-grid-ready #(update-selection [])
             on-selection-changed #(-> % (get-selected-ids) (update-selection))]
    [c/grid {:row-data items
             :on-grid-ready on-grid-ready
             :on-row-value-changed update-item
             :on-selection-changed on-selection-changed
             :column-defs [{:checkboxSelection true :sortable false}
                           {:field :id}
                           {:field :name :editable true}
                           {:field :created-at
                            :headerName "Created"}]}]))

(defn navbar []
  (with-let [create-item    #(dispatch [:create-item])
             delete-items   #(do
                               (dispatch [:delete-items %])
                               (dispatch [:select-objects []]))
             delete-items-factory (callback-factory-factory delete-items)]
    (let [selected-items @(subscribe [:selected-objects])
          num-selected   (count selected-items)]
      [c/navbar
       [c/navbar-group-left
        [c/navbar-heading "Item List"]
        [c/undo-redo]
        [c/button {:class :bp3-minimal :on-click create-item :icon :plus :title "Add item"}]
        [c/navbar-divider]
        (when (< 0 num-selected)
          [:<>
           [:div (selected-text num-selected)]
           [c/button {:class :bp3-minimal
                      :on-click (delete-items-factory selected-items)
                      :icon :minus :text "Delete"
                      :disabled (= num-selected 0)}]])]])))

(defn page []
  (let [all-items @(subscribe [:item-seq])]
    [item-grid all-items]))
