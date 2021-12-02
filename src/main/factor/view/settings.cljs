(ns factor.view.settings
  (:require [factor.components :as c]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as reagent :refer [with-let]]
            [factor.presets.factorio :as factorio]))

(defn navbar [] [c/navbar])

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

(defn preset-load-button
  []
  (with-let [on-confirm #(dispatch [:world-reset factorio/world])]
    [c/alerting-button
     {:text "Load Preset (WIP)"}
     {:on-confirm on-confirm
      :confirm-button-text "Confirm Load Preset"
      :intent :danger}
     [:p "Loading a preset will completely replace your existing world, including all items, recipes, machines, and factories! You cannot undo this action!"]
     [:p "(If you haven't done so already, I recommend exporting your existing world as a backup before loading a preset.)"]]))

(defn item-rate-unit-editor
  []
  (with-let [on-change #(dispatch [:set-unit :item-rate %])]
    [c/select-enum {:items #{"items/sec" "items/min"}
                    :initial-value (or @(subscribe [:unit :item-rate]) "items/sec")
                    :on-item-select on-change}]))

(defn power-unit-editor
  []
  (with-let [on-change #(dispatch [:set-unit :power %])]
    [c/select-enum {:items #{"W"}
                    :initial-value (or @(subscribe [:unit :power]) "W")
                    :on-item-select on-change}]))

(defn energy-unit-editor
  []
  (with-let [on-change #(dispatch [:set-unit :energy %])]
    [c/select-enum {:items #{"J"}
                    :initial-value (or @(subscribe [:unit :energy]) "J")
                    :on-item-select on-change}]))

(defn world-exporter
  []
  [c/textarea {:value @(subscribe [:world-as-edn]) :read-only true}])

(defn world-delete-button
  []
  (with-let [on-confirm #(dispatch [:world-reset])]
    [c/alerting-button
     {:text "RESET WORLD"
      :intent :danger}
     {:on-confirm on-confirm
      :intent :danger
      :confirm-button-text "DELETE MY 🤬 WORLD!!"}
     [:p "This will delete your ENTIRE world, including all items, recipes, machines, and factories! You cannot undo this action!"]
     [:p "(If you haven't done so already, I recommend exporting your existing world as a backup before resetting it.)"]]))

(defn page []
  [:div.card-stack
   [c/card-lg
    [c/form-group {:label "Item rate unit (default: items/sec)"} [item-rate-unit-editor]]
    [c/form-group {:label "Power unit (default: W)"} [power-unit-editor]]
    [c/form-group {:label "Energy unit (default: J)"} [energy-unit-editor]]]
   [c/card-lg [c/form-group {:label "Export (Copy World Data)"} [world-exporter]]]
   [c/card-lg [c/form-group {:label "Import (Paste World Data)"} [world-loader]]]
   [c/card-lg [c/form-group {:label "Delete All Data"} [world-delete-button]]]
   [c/card-lg [c/form-group {:label "Import Preset"} [preset-load-button]]]])
