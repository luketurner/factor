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

(defn tools []
  (let [selected-items @(subscribe [:selected-objects])
        num-selected   (count selected-items)]
    [:<>
     [c/cmd-btn {:cmd :new-item :intent :success :minimal true}]
     (when (< 0 num-selected)
       [:<>
        [c/navbar-divider]
        [:div (selected-text num-selected)]
        [c/cmd-btn {:cmd :delete-selected-items :minimal :true :intent :danger}]])]))

(defn page []
  (let [all-items @(subscribe [:item-seq])]
    [item-grid all-items]))
