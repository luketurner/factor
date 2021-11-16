(ns factor.view.factory
  (:require [factor.components :as c]
            [factor.pgraph :as pgraph]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :refer [as-element]]
            [clojure.string :as string]
            [factor.qmap :as qmap]
            [factor.world :as w]))

(defn create-and-select-factory []
  (let [factory (w/new-factory)]
    (dispatch [:update-factory factory])
    (dispatch [:open-factory (:id factory)])))

(defn delete-and-unselect-factory [id]
  (let [factory-ids     @(subscribe [:factory-ids])
        other-factory-id (some #(when (not= % id) %) factory-ids)]
    (dispatch [:delete-factory id])
    (dispatch [:open-factory other-factory-id])))

(defn navbar []
  (let [selected       @(subscribe [:open-factory])
        select-factory #(dispatch [:open-factory %])]
    [c/navbar
     [c/navbar-group-left
      [c/navbar-heading "Factories"]
      [c/navbar-divider]
      [c/suggest :factory selected select-factory]
      
      [c/alerting-button
       {:text "Delete"
        :class :bp3-minimal
        :icon :delete
        :intent :danger}
       {:on-confirm #(delete-and-unselect-factory selected)
        :intent :danger
        :confirm-button-text "Delete It!"}
       [:p "This will permanently delete this factory! (Your items/machines/recipes will stick around.)"]]
      [c/button {:class :bp3-minimal :intent :success :on-click create-and-select-factory :icon :plus :text "New"}]]]))


(defn get-item-name [id] (:name @(subscribe [:item id])))
(defn get-machine-name [id] (:name @(subscribe [:machine id])))
(defn get-recipe-name [id] (:name @(subscribe [:recipe id])))

(defn pgraph-tree-node
  [pg node-states seen-nodes parent-id node-id]
  (let [seen? (seen-nodes node-id)
        {:keys [recipe] :as node} (pgraph/get-node pg node-id)
        tree-node-id (str node-id "-" parent-id)
        {:keys [expanded selected disabled]} (get node-states tree-node-id)
        seen-nodes (conj seen-nodes node-id)
        child-node-for-edge (fn [[l _ _]]
                              (pgraph-tree-node pg node-states seen-nodes node-id l))
        child-nodes (when-not seen? (map child-node-for-edge (pgraph/input-edges pg node-id)))
        child-nodes (if (not-empty (:catalysts node))
                      (conj child-nodes {:id (str tree-node-id "-catalysts")
                                         :label (str "Catalysts: " (qmap/qmap->str (:catalysts node) get-item-name ", "))
                                         :icon :lab-test})
                      child-nodes)]
    {:id tree-node-id
     :label (qmap/qmap->str (case node-id
                              :missing (pgraph/missing-input-for pg parent-id)
                              :excess (pgraph/excess-output-for pg parent-id)
                              (if (= (:id node) (:id (pgraph/desired-output-node pg)))
                                (:input node)
                                (:output node)))
                            get-item-name
                            ", ")
     :isExpanded expanded
     :isSelected selected
     :disabled disabled
     :hasCaret (not-empty child-nodes)
     :icon (case node-id
             :missing :cube
             :excess :cube
             (if (= (:id node) (:id (pgraph/desired-output-node pg)))
               :office
               :data-lineage))
     :childNodes child-nodes}))

(defn pgraph-tree
  [pg]
  (reagent.core/with-let [node-states (reagent.core/atom {})]
    [c/tree {:contents (clj->js [(pgraph-tree-node pg @node-states #{} nil (:id (pgraph/desired-output-node pg)))])
             :on-node-expand #(swap! node-states assoc-in [(.-id %) :expanded] true)
             :on-node-collapse #(swap! node-states assoc-in [(.-id %) :expanded] false)}]))

(defn node-list
  [pg]
  (into [:ul] (for [node (pgraph/all-nodes pg) :when (and (not= (:id node) :excess)
                                                          (not= (:id node) :missing)
                                                          (not= (:id node) (:id (pgraph/desired-output-node pg))))]
                [:li (str (get node :num-machines) "x " (:name @(subscribe [:recipe (get-in node [:recipe :id])])))])))

(defn catalyst-list
  [pg]
  (if-let [catalysts (pgraph/all-catalysts pg)]
    (into [:ul] (for [[k v] catalysts] [:li (str v "x " (get-item-name k))]))))

(defn page []
  (if-let [factory-id @(subscribe [:open-factory])]
    (let [factory @(subscribe [:factory factory-id])
          pg @(subscribe [:factory-pgraph factory-id])
          update-factory #(dispatch [:update-factory %])
          item-rate-unit @(subscribe [:unit :item-rate])]
      [:div.card-stack
       [c/card-lg
        [c/form-group {:label "ID"}
         [c/input {:value (:id factory) :disabled true}]]
        [c/form-group {:label "Name"}
         [c/input {:value (:name factory) :on-change #(update-factory (assoc factory :name %))}]]]
       [c/card-lg [c/form-group {:label (str "Desired Outputs (" item-rate-unit ")")}
                   [c/quantity-set-input :item (:desired-output factory) #(update-factory (assoc factory :desired-output %))]]]
       [c/card-lg [c/form-group {:label (str "Excess Outputs (" item-rate-unit ")")}
                   (into [:ul] (for [[x n] (pgraph/excess-output pg)] [:li n "x " (get-item-name x)]))]]
       [c/card-lg [c/form-group {:label (str "Needed Inputs (" item-rate-unit ")")}
                   (into [:ul] (for [[x n] (pgraph/missing-input pg)] [:li n "x " (get-item-name x)]))]]
       [c/card-lg [c/form-group {:label (str "Catalysts (" item-rate-unit ")")}
                   [catalyst-list pg]]]
      ;;  [c/card-lg [c/form-group {:label "Machines"}
      ;;              (into [:ul] (for [[x n] (:machines pgraph)] [:li n "x " (get-machine-name x)]))]]
      ;;  [c/card-lg [c/form-group {:label "Recipes"}
      ;;              (into [:ul] (for [[x n] (:recipes pgraph)] [:li n "x " (get-recipe-name x)]))]]
       [c/card-lg [c/form-group {:label (str "Production Graph (" item-rate-unit ")")}
                   (if (empty? (:desired-output factory))
                     [:p "Add desired output(s) to view production graph."]
                     [pgraph-tree pg])]]
       [c/card-lg [c/form-group {:label (str "Production Stages (" item-rate-unit ")")}
                   (if (empty? (:desired-output factory))
                     [:p "Add desired output(s) to view production stages."]
                     [node-list pg])]]
       [c/card-lg
        [c/form-group {:label "Dot-Formatted Production Graph (WARNING: Data is not sanitized. Don't use with untrusted worlds!)"}
         [c/textarea {:value (pgraph/pg->dot pg)
                      :read-only true
                      :style {:width "100%" :height "150px"}}]]
        [c/form-group {:label "Raw Data - Production Graph"}
         [c/textarea {:value (pr-str (dissoc pg :world))
                      :read-only true
                      :style {:width "100%" :height "150px"}}]]
        [c/form-group {:label "Raw Data - Factory Object"}
         [c/textarea {:value (pr-str factory)
                      :read-only true
                      :style {:width "100%" :height "150px"}}]]]])
    [c/non-ideal-state {:icon :office
                        :title "No factories!"
                        :description "Create a factory to get started."
                        :action (as-element [c/button {:text "Create Factory"
                                                       :intent :success
                                                       :on-click create-and-select-factory}])}]))