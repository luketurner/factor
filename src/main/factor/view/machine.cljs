(ns factor.view.machine
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


(defn machine-grid [machines]
  (let [update-machine #(dispatch [:update-machine (:data %)])
        update-selection #(dispatch [:ui [:machine-page :selected] %])]
    [c/grid {:row-data machines
             :on-grid-ready #(update-selection [])
             :on-row-value-changed #(update-machine %)
             :on-selection-changed #(-> % (get-selected-ids) (update-selection))
             :column-defs [{:checkboxSelection true}
                           {:field :id}
                           {:field :name :editable true}
                           {:field :power :editable true}
                           {:field :speed :editable true}]}]))

(defn navbar []
  (let [create-machine #(dispatch [:update-machine (w/new-machine)])
        selected-machines @(subscribe [:ui [:machine-page :selected]])
        delete-machines   #(do
                             (dispatch [:delete-machines selected-machines])
                             (dispatch [:ui [:machine-page :selected] []]))
        num-selected (count selected-machines)]
    [c/navbar
     [c/navbar-group-left
      [c/navbar-heading "Machine List"]
      [c/button {:class :bp3-minimal :on-click create-machine :icon :plus :title "Add machine"}]
      [c/navbar-divider]
      (when (< 0 num-selected)
        [:<>
         [:div (selected-text num-selected)]
         [c/button {:class :bp3-minimal
                    :on-click delete-machines
                    :icon :minus :text "Delete"
                    :disabled (= num-selected 0)}]])]]))

(defn page []
  (let [all-machines @(subscribe [:machine-seq])]
    [machine-grid all-machines]))