(ns factor.factory
  (:require [re-frame.core :refer [enrich reg-event-db reg-sub dispatch subscribe]]
            [factor.util :refer [new-uuid]]
            [factor.item :refer [item-rate-list item-rate-list-editor]]
            [factor.recipe :refer [recipe-editor-list]]
            [factor.machine :refer [machine-list]]
            [medley.core :refer [filter-vals map-vals]]))

(defn factory []
  {:name "Unnamed Factory"
   :input {}
   :output {}})


(defn recipe-for-item [recipes item-id]
  (some (fn [[k {:keys [output]}]]
          (when (contains? output item-id) k)) recipes))


; TODO -- doesn't store info about machines
(defn apply-recipe [factory recipe-id recipe times]
  (let [input-change (map-vals #(* % times) (:input recipe))
        output-change (map-vals #(* % times) (:output recipe))
        machine-id (first (:machines recipe))]
   (cond-> factory
       input-change (update :input #(merge-with + % input-change))
       output-change (update :input #(merge-with + % (map-vals - output-change)))
       output-change (update :input #(filter-vals pos? %))
       output-change (update :output #(merge-with + % output-change))
       machine-id (update-in [:machines machine-id] + times)
       true (update-in [:recipes recipe-id] + times))))

(defn satisfy-desired-outputs [initial-factory {:keys [recipes]}]
  (letfn [(get-recipe-for [items] (some #(recipe-for-item recipes (% 0)) items))
          (satisfy [{:keys [output desired-output] :as factory}]
                   (let [output-gap (->> output
                                         (merge-with - desired-output)
                                         (filter-vals pos?))]
                     (if (empty? output-gap) factory
                         (let [next-recipe-id (get-recipe-for output-gap)
                               next-recipe (recipes next-recipe-id)
                               times (apply max (for [[k v] (:output next-recipe)]
                                                  (when (contains? output-gap k)
                                                    (/ (output-gap k) v))))
                               updated-factory (apply-recipe factory
                                                             next-recipe-id
                                                             next-recipe
                                                             times)]
                           (if (= updated-factory factory) factory
                             (satisfy updated-factory))))))]
    (satisfy initial-factory)))

(defn recalc-factory [db [_ factory-id]]
  (update-in db [:world :factories factory-id]
             #(satisfy-desired-outputs % (:world db))))

(reg-event-db :create-factory (fn [db] (assoc-in db [:world :factories (new-uuid)] (factory))))
(reg-event-db :update-factory
              [(enrich recalc-factory)]
              (fn [db [_ id factory]] (assoc-in db [:world :factories id] factory)))
(reg-event-db :delete-factory (fn [db [_ id]] (update-in db [:world :factories] dissoc id)))
(reg-sub :factory (fn [db [_ id]] (get-in db [:world :factories id])))
(reg-sub :factory-ids (fn [db] (-> db (get-in [:world :factories]) (keys))))


(defn factory-editor [factory-id]
  (let [{:keys [name input output desired-output recipes machines] :as factory} @(subscribe [:factory factory-id])
        upd #(dispatch [:update-factory factory-id %])]
    [:div
     [:h2
      [:input {:class "factory-name" :type "text" :value name :on-change #(upd (assoc factory :name (.-value (.-target %))))}]
      [:button {:on-click #(dispatch [:delete-factory factory-id])} "Delete"]]
     [:dl
      [:dt "Desired Outputs"]
      [:dd [item-rate-list-editor desired-output #(upd (assoc factory :desired-output %))]]
      [:dt "Outputs"]
      [:dd [item-rate-list output]]
      [:dt "Inputs"]
      [:dd [item-rate-list input]]
      [:dt "Recipes"]
      [:dd [recipe-editor-list recipes]]
      [:dt "Machines"]
      [:dd [machine-list machines]]
      [:dd]]]))


(defn factory-page []
  (let [factories @(subscribe [:factory-ids])]
    [:div
     (if (not-empty factories)
       (into [:div] (for [fact-id factories] [factory-editor fact-id]))
       [:div [:h2 "factories"] [:p "You don't have any factories."]])
     [:button {:on-click #(dispatch [:create-factory])} "Add factory"]]))