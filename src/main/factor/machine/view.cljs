(ns factor.machine.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [factor.machine.components :refer [machine-editor]]))

(defn machine-page []
  (let [machines @(subscribe [:machine-ids])]
    [:div
     [:h2 "machines"]
     (if (not-empty machines)
       (into [:div] (for [id machines] [machine-editor id]))
       [:p "You don't have any machines."])
     [:button {:on-click #(dispatch [:create-machine])} "Add machine"]]))