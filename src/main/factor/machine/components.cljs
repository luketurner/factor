(ns factor.machine.components
  (:require [re-frame.core :refer [subscribe dispatch]]
            [factor.widgets :refer [input-rate deletable-section collapsible-section dropdown dropdown-submitted button input-text list-editor]]))

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
        set-name #(dispatch [:update-machine machine-id (assoc machine :name %)])
        set-power #(dispatch [:update-machine machine-id (assoc machine :power %)])
        set-speed #(dispatch [:update-machine machine-id (assoc machine :speed %)])]
    [deletable-section {:on-delete [:delete-machine machine-id]}
     [collapsible-section {:summary (:name machine)}
     [:dl
      [:dt "Name"]
      [:dd [input-text (:name machine) set-name focused?]]
      [:dt "Energy cost"]
      [:dd [input-rate (:power machine) set-power]]
      [:dt "Production speed"]
      [:dd [input-rate (:speed machine) set-speed]]]]]))