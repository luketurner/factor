(ns factor.view.item
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :refer [with-let]]
            [factor.components.cmd :refer [cmd-btn]]
            [factor.components.grid :refer [grid]]
            [factor.components.wrappers :refer [navbar-divider]]))

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
    [grid {:row-data items
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
     [cmd-btn {:cmd :new-item :intent :success :minimal true}]
     (when (< 0 num-selected)
       [:<>
        [navbar-divider]
        [:div (selected-text num-selected)]
        [cmd-btn {:cmd :delete-selected-items :minimal :true :intent :danger}]])]))

(defn page []
  (let [all-items @(subscribe [:item-seq])]
    [item-grid all-items]))
