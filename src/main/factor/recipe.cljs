(ns factor.recipe
  (:require [clojure.string :as string]
            [re-frame.core :refer [subscribe dispatch reg-event-db reg-sub]]
            [factor.util :refer [new-uuid]]
            [factor.machine :refer [machine-list-editor machine-list]]
            [factor.item :refer [item-rate-list-editor item-rate-list]]))

(defn recipe [] {:input {}
                 :output {}
                 :machines #{}})

(reg-event-db :create-recipe 
              (fn [db [_ opt]]
                (let [id (new-uuid)]
                  (cond-> db
                    true (assoc-in [:world :recipes id] (recipe))
                    (= opt :expanded) (assoc-in [:ui :recipes :expanded id] true)))))
(reg-event-db :update-recipe (fn [db [_ id v]] (assoc-in db [:world :recipes id] v)))
(reg-event-db :delete-recipe (fn [db [_ id]] (update-in db [:world :recipes] dissoc id)))
(reg-sub :recipe (fn [db [_ id]] (get-in db [:world :recipes id])))
(reg-sub :recipe-ids (fn [db] (-> db (get-in [:world :recipes]) (keys))))
(reg-sub :recipe-count (fn [db] (-> db (get-in [:world :recipes]) (count))))

(reg-event-db :toggle-recipe-expanded (fn [db [_ id]] (update-in db [:ui :recipes :expanded id] not)))
(reg-sub :recipe-is-expanded (fn [db [_ id]] (get-in db [:ui :recipes :expanded id] false)))



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
        upd #(dispatch [:update-recipe recipe-id %])
        is-expanded @(subscribe [:recipe-is-expanded recipe-id])
        toggle-expanded #(dispatch [:toggle-recipe-expanded recipe-id])]
    [:details {:open is-expanded} 
     [:summary {:on-click toggle-expanded}
      (str (when times (str times "x ")) display-name)
      [:button {:on-click #(dispatch [:delete-recipe recipe-id])} "-"]]
     [:dl
      [:dt "Inputs"]
      [:dd [item-rate-list-editor input #(upd (assoc recipe :input %))]]
      [:dt "Outputs"]
      [:dd [item-rate-list-editor output #(upd (assoc recipe :output %))]]
      [:dt "Machines"]
      [:dd [machine-list-editor machines #(upd (assoc recipe :machines %))]]]]))

(defn recipe-editor-list [recipe-map]
  (into [:ul] (for [[r t] recipe-map] [recipe-editor r t])))


(defn recipe-page []
  (let [recipes @(subscribe [:recipe-ids])
        create-recipe #(dispatch [:create-recipe :expanded])]
    [:div
     [:h2 "recipes"]
     (if (not-empty recipes)
       (into [:div] (for [id recipes] [recipe-editor id]))
       [:p "You don't have any recipes."])
     [:button {:on-click create-recipe} "Add recipe"]]))