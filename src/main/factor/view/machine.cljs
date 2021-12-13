(ns factor.view.machine
  (:require [factor.components :as c]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :refer [with-let]]
            [clojure.string :as string]
            [factor.util :refer [callback-factory-factory]]))

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
      [c/grid {:row-data machines
               :on-grid-ready on-grid-ready
               :on-row-value-changed update-machine
               :on-selection-changed on-selection-changed
               :column-defs [{:checkboxSelection true :sortable false}
                             {:field :id}
                             {:field :name :editable true}
                             {:field :power :editable true :type :numericColumn
                              :headerName (str "Power (" power-unit ")")
                              :valueParser c/grid-value-parser-for-floats}
                             {:field :speed :editable true :type :numericColumn
                              :headerName (str "Speed (" rate-denominator ")")
                              :valueParser c/grid-value-parser-for-floats}
                             {:field :created-at
                              :headerName "Created"}]}])))

(defn tools []
  (with-let [create-machine #(dispatch [:create-machine])
             delete-machines   #(dispatch [:delete-machines %])
             delete-machines-factory (callback-factory-factory delete-machines)]
    (let [selected-machines @(subscribe [:selected-objects])
          num-selected (count selected-machines)]
      [:<>
       [c/button {:class :bp3-minimal :on-click create-machine :icon :plus :text "New machine" :intent :success}]
       (when (< 0 num-selected)
         [:<>
          [c/navbar-divider]
          [:div (selected-text num-selected)]
          [c/button {:class :bp3-minimal
                     :on-click (delete-machines-factory selected-machines)
                     :icon :minus :text "Delete"
                     :disabled (= num-selected 0)}]])])))

(defn page []
  (let [all-machines @(subscribe [:machine-seq])]
    [machine-grid all-machines]))