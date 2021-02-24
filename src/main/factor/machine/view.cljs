(ns factor.machine.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [factor.machine.components :refer [machine-editor]]
            [factor.widgets :refer [list-editor]]))

(defn machine-page []
  (let [machines @(subscribe [:machine-ids])]
    [:<>
     [:h2 "machines"]
     [list-editor {:data machines
                   :row-fn (fn [id] [machine-editor id (= id (last machines))])
                   :empty-message [:p "You don't have any machines."]
                   :add-fn #(dispatch [:create-machine])
                   :del-fn #(dispatch [:delete-machine %])}]]))
