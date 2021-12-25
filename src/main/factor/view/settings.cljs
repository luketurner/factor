(ns factor.view.settings
  (:require [factor.components :as c]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as reagent :refer [with-let]]))

(defn world-loader
  []
  (with-let [v (reagent/atom "")
             on-change #(->> %
                             (.-target)
                             (.-value)
                             (reset! v))
             on-confirm #(dispatch [:load-world-from-edn @v])]
    [:<>
     ;; TODO -- update textarea to extract value from on-change within component
     ;; instead of having to do the .-target .-value jazz everywhere
     [c/textarea {:on-change on-change}]
     [c/alerting-button
      {:text "Import"}
      {:on-confirm on-confirm
       :confirm-button-text "Confirm Import"
       :intent :danger}
      [:p "Importing another world will completely replace your existing world, including all items, recipes, machines, and factories! You cannot undo this action!"]
      [:p "(If you haven't done so already, I recommend exporting your existing world as a backup before importing another one.)"]]]))

(defn world-exporter
  []
  [c/textarea {:value @(subscribe [:world-as-edn]) :read-only true}])

(defn page []
  [:div.card-stack
   [c/card-lg [c/form-group {:label "Export (Copy World Data)"} [world-exporter]]]
   [c/card-lg [c/form-group {:label "Import (Paste World Data)"} [world-loader]]]])
