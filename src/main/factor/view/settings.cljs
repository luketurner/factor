(ns factor.view.settings
  (:require [factor.components :as c]
            [re-frame.core :refer [dispatch subscribe]]
            [factor.world :as w]
            [reagent.core :as reagent]
            [factor.presets.factorio :as factorio]))

(defn navbar [] [c/navbar])

(defn world-loader
  []
  (reagent/with-let [v (reagent/atom "")]
    [:<>
     ;; TODO -- update textarea to extract value from on-change within component
     ;; instead of having to do the .-target .-value jazz everywhere
     [c/textarea {:on-change #(->> %
                                   (.-target)
                                   (.-value)
                                   (reset! v))}]
     [c/alerting-button
      {:text "Import"}
      {:on-confirm #(dispatch [:world-reset (w/str->world @v)])
       :confirm-button-text "Confirm Import"
       :intent :danger}
      [:p "Importing another world will completely replace your existing world, including all items, recipes, machines, and factories! You cannot undo this action!"]
      [:p "(If you haven't done so already, I recommend exporting your existing world as a backup before importing another one.)"]]]))

(defn preset-load-button
  [label preset-world]
  [c/alerting-button
   {:text label}
   {:on-confirm #(dispatch [:world-reset preset-world])
    :confirm-button-text "Confirm Load Preset"
    :intent :danger}
   [:p "Loading a preset will completely replace your existing world, including all items, recipes, machines, and factories! You cannot undo this action!"]
   [:p "(If you haven't done so already, I recommend exporting your existing world as a backup before loading a preset.)"]])

(defn page []
  [:div.card-stack
   [c/card-lg
    [c/form-group {:label "Item rate unit (default: items/sec)"}
     [c/select-enum #{"items/sec" "items/min"} (or @(subscribe [:unit :item-rate]) "items/sec") #(dispatch [:set-unit :item-rate %])]]
    [c/form-group {:label "Power unit (default: W)"}
     [c/select-enum #{"W"} (or @(subscribe [:unit :power]) "W") #(dispatch [:set-unit :power %])]]
    [c/form-group {:label "Energy unit (default: J)"}
     [c/select-enum #{"J"} (or @(subscribe [:unit :energy]) "J") #(dispatch [:set-unit :energy %])]]]
   [c/card-lg
    [c/form-group {:label "Export (Copy World Data)"}
     [c/textarea {:value (w/world->str @(subscribe [:world-data])) :read-only true}]]]
   [c/card-lg
    [c/form-group {:label "Import (Paste World Data)"}
     [world-loader]]]
   [c/card-lg
    [c/form-group {:label "Delete All Data"}
     [c/alerting-button
      {:text "RESET WORLD"
       :intent :danger}
      {:on-confirm #(dispatch [:world-reset w/empty-world])
       :intent :danger
       :confirm-button-text "DELETE MY 🤬 WORLD!!"}
      [:p "This will delete your ENTIRE world, including all items, recipes, machines, and factories! You cannot undo this action!"]
      [:p "(If you haven't done so already, I recommend exporting your existing world as a backup before resetting it.)"]]]]
   [c/card-lg
    [c/form-group {:label "Import Preset"}
     [preset-load-button "Factorio (WIP)" factorio/world]]]])
