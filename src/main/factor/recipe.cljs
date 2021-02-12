(ns factor.recipe
  (:require [clojure.string :as string]
            [re-frame.core :refer [subscribe dispatch reg-event-db reg-sub]]
            [factor.util :refer [new-uuid]]
            [factor.machine :refer [machine-list-editor machine-list]]
            [factor.item :refer [item-rate-list-editor item-rate-list]]))

(defn recipe [] {:input {}
                 :output {}
                 :machines #{}})

(reg-event-db :create-recipe (fn [db] (assoc-in db [:world :recipes (new-uuid)] (recipe))))
(reg-event-db :update-recipe (fn [db [_ id v]] (assoc-in db [:world :recipes id] v)))
(reg-event-db :delete-recipe (fn [db [_ id]] (update-in db [:world :recipes] dissoc id)))
(reg-sub :recipe (fn [db [_ id]] (get-in db [:world :recipes id])))
(reg-sub :recipe-ids (fn [db] (-> db (get-in [:world :recipes]) (keys))))

(defn recipe-viewer [recipe-id]
  (let [{:keys [input output machines]} @(subscribe [:recipe recipe-id])]
    [:details [:summary (first (keys output))]
     [:dl
      [:dt "Inputs"]
      [:dd [item-rate-list input]]
      [:dt "Outputs"]
      [:dd [item-rate-list output]]
      [:dt "Machines"]
      [:dd [machine-list machines]]]]))

(defn recipe-editor [recipe-id]
  (let [{:keys [input output machines] :as recipe} @(subscribe [:recipe recipe-id])
        output-item-names (for [[o _] output] (:name @(subscribe [:item o])))
        display-name (if (not-empty output)
                       (str "Recipe:" (string/join ", " output-item-names))
                       "New Recipe")
        upd #(dispatch [:update-recipe recipe-id %])]
    [:details [:summary display-name]
     [:dl
      [:dt "Inputs"]
      [:dd [item-rate-list-editor input #(upd (assoc recipe :input %))]]
      [:dt "Outputs"]
      [:dd [item-rate-list-editor output #(upd (assoc recipe :output %))]]
      [:dt "Machines"]
      [:dd [machine-list-editor machines #(upd (assoc recipe :machines %))]]]]))

(defn recipe-page []
  (let [recipes @(subscribe [:recipe-ids])]
    [:div
     (if (not-empty recipes)
       (into [:div] (for [id recipes] [recipe-editor id]))
       [:p "You don't have any recipes."])
     [:button {:on-click #(dispatch [:create-recipe])} "Add recipe"]]))