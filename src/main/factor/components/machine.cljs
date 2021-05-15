(ns factor.components.machine
  (:refer-clojure :exclude [list])
  (:require [re-frame.core :refer [subscribe dispatch]]
            [factor.components.widgets :as w]))

(defn list [machines]
  (if (not-empty machines)
    (into [:ul]
          (for [[machine num] machines] [:li (str num " " (:name @(subscribe [:machine machine])))]))
    [:p "No machines."]))

(defn picker [value on-change]
  [w/dropdown @(subscribe [:machine-names]) value "Select machine..." on-change])

(defn picker-submitted [on-submit]
  [w/dropdown-submitted @(subscribe [:machine-names]) "Select machine..." on-submit])

(defn list-editor [machines on-change]
  (into
   [:div]
   (concat
    (for [machine-id machines]
      [:div
       [picker machine-id #(on-change (-> machines (disj machine-id) (conj %)))]
       [w/button {:on-click #(on-change (disj machines machine-id))} "-"]])
    [[picker-submitted (fn [m] (on-change (conj machines m)))]])))

(defn editor [machine-id focused?]
  (let [machine @(subscribe [:machine machine-id])
        set-name #(dispatch [:update-machine machine-id (assoc machine :name %)])
        set-power #(dispatch [:update-machine machine-id (assoc machine :power %)])
        set-speed #(dispatch [:update-machine machine-id (assoc machine :speed %)])]
    [w/deletable-section {:on-delete [:delete-machine machine-id]}
     [w/collapsible-section {:summary [:<> (:name machine) [w/button {:on-click [:delete-machine machine-id]} "-"]]}
      [:dl
       [:dt "Name"]
       [:dd [w/input-text (:name machine) set-name focused?]]
       [:dt "Energy cost"]
       [:dd [w/input-rate (:power machine) set-power]]
       [:dt "Production speed"]
       [:dd [w/input-rate (:speed machine) set-speed]]]]]))

(defn sub-nav []
  [:p "Machines!"])