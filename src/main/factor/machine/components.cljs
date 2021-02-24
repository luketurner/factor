(ns factor.machine.components
  (:require [re-frame.core :refer [subscribe dispatch]]
            [factor.widgets :refer [dropdown dropdown-submitted button input-text list-editor]]))

(defn machine-list [machines]
  (if (not-empty machines)
    (into [:ul]
          (for [[machine num] machines] [:li (str num " " (:name @(subscribe [:machine machine])))]))
    [:p "No machines."]))

(defn machine-picker [value on-change]
  [dropdown @(subscribe [:machine-names]) value "Select machine..." on-change])

(defn machine-picker-submitted [on-submit]
  [dropdown-submitted @(subscribe [:machine-names]) "Select machine..." on-submit])

(defn machine-list-editor [machines on-change]
  (into
   [:div]
   (concat
    (for [machine-id machines]
      [:div
       [machine-picker machine-id #(on-change (-> machines (disj machine-id) (conj %)))]
       [button {:on-click #(on-change (disj machines machine-id))} "-"]])
    [[machine-picker-submitted (fn [m] (on-change (conj machines m)))]])))

(defn machine-editor [machine-id focused?]
  (let [machine @(subscribe [:machine machine-id])
        update-name #(dispatch [:update-machine machine-id (assoc machine :name %)])]
    [input-text (:name machine) update-name focused?]))