(ns factor.recipe.components
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as string]
            [factor.item.components :refer [item-rate-editor-list item-rate-list]]
            [factor.machine.components :refer [machine-list machine-list-editor]]
            [factor.widgets :refer [button collapsible-section deletable-section]]))

(defn recipe-viewer [recipe-id times]
  (let [{:keys [input output machines]} @(subscribe [:recipe recipe-id])
        output-item-names (for [[o _] output] (:name @(subscribe [:item o])))
        display-name (if (not-empty output)
                       (str "Recipe:" (string/join ", " output-item-names))
                       "New Recipe")]
    [:details [:summary (str (when times (str times "x ")) display-name)]
     [:dl
      [:dt "Inputs"]
      [:dd [item-rate-list input]]
      [:dt "Outputs"]
      [:dd [item-rate-list output]]
      [:dt "Machines"]
      [:dd [machine-list machines]]]]))


(defn recipe-viewer-list [recipe-map]
  (into [:ul] (for [[r t] recipe-map] [recipe-viewer r t])))

(defn recipe-editor [recipe-id times]
  (let [{:keys [input output machines] :as recipe} @(subscribe [:recipe recipe-id])
        output-item-names (for [[o _] output] (:name @(subscribe [:item o])))
        display-name (if (not-empty output)
                       (str "Recipe:" (string/join ", " output-item-names))
                       "New Recipe")
        summary (str (when times (str times "x ")) display-name)
        upd #(dispatch [:update-recipe recipe-id %])
        del #(dispatch [:delete-recipe recipe-id])]
    [deletable-section {:on-delete del}
     [collapsible-section {:summary summary}
     [:dl
      [:dt "Inputs"]
      [:dd [item-rate-editor-list input #(upd (assoc recipe :input %))]]
      [:dt "Outputs"]
      [:dd [item-rate-editor-list output #(upd (assoc recipe :output %))]]
      [:dt "Machines"]
      [:dd [machine-list-editor machines #(upd (assoc recipe :machines %))]]]]]))

(defn recipe-editor-list [recipe-map]
  (into [:ul] (for [[r t] recipe-map] [recipe-editor r t])))


