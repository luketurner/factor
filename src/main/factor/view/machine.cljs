(ns factor.view.machine
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :refer [with-let]]
            [clojure.string :as string]
            [factor.components.cmd :refer [cmd-btn]]
            [factor.components.wrappers :refer [navbar-divider]]
            [factor.components.grid :refer [grid-value-parser-for-floats grid]]))

(defn get-selected-ids [ev]
  (-> ev
      (:api)
      (.getSelectedRows)
      (js->clj)
      (->> (map #(get % "id")))
      (vec)))

(defn selected-text [num] (str "(" num " items selected)"))


(defn machine-grid [machines]
  (with-let [update-machine #(dispatch [:update-machine (:data %)])
             update-selection #(dispatch [:select-objects %])
             on-grid-ready #(update-selection [])
             on-selection-changed #(-> % (get-selected-ids) (update-selection))]
    (let [power-unit @(subscribe [:unit :power])
          item-rate-unit @(subscribe [:unit :item-rate])
          [_ rate-denominator] (string/split item-rate-unit #"/" 2)]
      [grid {:row-data machines
             :on-grid-ready on-grid-ready
             :on-row-value-changed update-machine
             :on-selection-changed on-selection-changed
             :column-defs [{:checkboxSelection true :sortable false}
                           {:field :id}
                           {:field :name :editable true}
                           {:field :power :editable true :type :numericColumn
                            :headerName (str "Power (" power-unit ")")
                            :valueParser grid-value-parser-for-floats}
                           {:field :speed :editable true :type :numericColumn
                            :headerName (str "Speed (" rate-denominator ")")
                            :valueParser grid-value-parser-for-floats}
                           {:field :created-at
                            :headerName "Created"}]}])))

(defn tools []
  (let [selected-machines @(subscribe [:selected-objects])
        num-selected (count selected-machines)]
    [:<>
     [cmd-btn {:cmd :new-machine :intent :success :minimal true}]
     (when (< 0 num-selected)
       [:<>
        [navbar-divider]
        [:div (selected-text num-selected)]
        [cmd-btn {:cmd :delete-selected-machines :intent :danger :minimal true}]])]))

(defn page []
  (let [all-machines @(subscribe [:machine-seq])]
    [machine-grid all-machines]))