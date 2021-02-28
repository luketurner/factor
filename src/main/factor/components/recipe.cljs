(ns factor.components.recipe
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as string]
            [factor.components.item :as item]
            [factor.components.machine :as machine]
            [factor.components.widgets :as w]))

(defn viewer [recipe-id times]
  (let [{:keys [input output machines]} @(subscribe [:recipe recipe-id])
        output-item-names (for [[o _] output] (:name @(subscribe [:item o])))
        display-name (if (not-empty output)
                       (str "Recipe:" (string/join ", " output-item-names))
                       "New Recipe")]
    [:details [:summary (str (when times (str times "x ")) display-name)]
     [:dl
      [:dt "Inputs"]
      [:dd [item/rate-list input]]
      [:dt "Outputs"]
      [:dd [item/rate-list output]]
      [:dt "Machines"]
      [:dd [machine/list machines]]]]))


(defn viewer-list [recipe-map]
  (into [:ul] (for [[r t] recipe-map] [viewer r t])))

(defn editor [recipe-id times]
  (let [{:keys [input output machines] :as recipe} @(subscribe [:recipe recipe-id])
        output-item-names (for [[o _] output] (:name @(subscribe [:item o])))
        display-name (if (not-empty output)
                       (str "Recipe:" (string/join ", " output-item-names))
                       "New Recipe")
        summary (str (when times (str times "x ")) display-name)
        upd #(dispatch [:update-recipe recipe-id %])
        del #(dispatch [:delete-recipe recipe-id])]
    [w/deletable-section {:on-delete del}
     [w/collapsible-section {:summary [:<> summary [w/button {:on-click del} "-"]]}
      [:dl
       [:dt "Inputs"]
       [:dd [item/rate-editor-list input #(upd (assoc recipe :input %))]]
       [:dt "Outputs"]
       [:dd [item/rate-editor-list output #(upd (assoc recipe :output %))]]
       [:dt "Machines"]
       [:dd [machine/list-editor machines #(upd (assoc recipe :machines %))]]]]]))

(defn editor-list [recipe-map]
  (into [:ul] (for [[r t] recipe-map] [editor r t])))


