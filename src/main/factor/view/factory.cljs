(ns factor.view.factory
  (:require [factor.components :as c]
            [factor.pgraph :as pgraph]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :refer [as-element]]
            [clojure.string :as string]
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
      [c/button {:class :bp3-minimal :on-click create-and-select-factory :icon :plus :text "Add factory"}]
      [c/navbar-divider]
      [c/button {:on-click #(delete-and-unselect-factory selected) :intent :danger :text "Delete factory"}]]]))


(defn get-item-name [id] (:name @(subscribe [:item id])))
(defn get-machine-name [id] (:name @(subscribe [:machine id])))
(defn get-recipe-name [id] (:name @(subscribe [:recipe id])))

(defn qmap->str
  [qm]
  (string/join ", " (map (fn [[x n]] (str n "x " (get-item-name x))) qm)))

(defn pgraph-tree-node
  [pg node-states seen-nodes parent-id node-id]
  (let [seen? (seen-nodes node-id)
        {:keys [recipe] :as node} (pgraph/get-node pg node-id)
        tree-node-id (str node-id "-" parent-id)
        {:keys [expanded selected disabled]} (get node-states tree-node-id)
        seen-nodes (conj seen-nodes node-id)
        child-node-for-edge (fn [[l _ _]]
                              (pgraph-tree-node pg node-states seen-nodes node-id l))
        child-nodes (when-not seen? (map child-node-for-edge (pgraph/input-edges pg node-id)))]
    {:id tree-node-id
     :label (qmap->str (case node-id
              :start (:output node)
              :end (:input node)
              (:output node)))
    ;;  :secondaryLabel (case node-id
    ;;           :start "Required Input"
    ;;           :end "Factory Output"
    ;;           (:name recipe))
     :isExpanded expanded
     :isSelected selected
     :disabled disabled
     :hasCaret (not-empty child-nodes)
     :icon (case node-id
             :start :cube
             :end :office
             :data-lineage)
     :childNodes child-nodes}))

(defn pgraph-tree
  [pg]
  (reagent.core/with-let [node-states (reagent.core/atom {})]
    (if (pgraph/is-empty? pg) [:p "Add a desired output to view production graph."]
     [c/tree {:contents (clj->js [(pgraph-tree-node pg @node-states #{} nil :end)])
             :on-node-expand #(swap! node-states assoc-in [(.-id %) :expanded] true)
             :on-node-collapse #(swap! node-states assoc-in [(.-id %) :expanded] false)}])))

(defn page []
  (if-let [factory-id @(subscribe [:open-factory])]
    (let [factory @(subscribe [:factory factory-id])
          pg @(subscribe [:factory-pgraph factory-id])
          update-factory #(dispatch [:update-factory %])]
      ;; (println "nodes" (tree-node-for-pgraph-node pg :root))
      [:div.card-stack
       [c/card-lg
        [c/form-group {:label "ID"}
         [c/input {:value (:id factory) :disabled true}]]
        [c/form-group {:label "Name"}
         [c/input {:value (:name factory) :on-change #(update-factory (assoc factory :name %))}]]]
       [c/card-lg [c/form-group {:label "Desired Outputs"}
                   [c/quantity-set-input :item (:desired-output factory) #(update-factory (assoc factory :desired-output %))]]]
       [c/card-lg [c/form-group {:label "Outputs"}
                   (into [:ul] (for [[x n] (:input (pgraph/get-node pg :end))] [:li n "x " (get-item-name x)]))]]
       [c/card-lg [c/form-group {:label "Inputs"}
                   (into [:ul] (for [[x n] (:output (pgraph/get-node pg :start))] [:li n "x " (get-item-name x)]))]]
      ;;  [c/card-lg [c/form-group {:label "Machines"}
      ;;              (into [:ul] (for [[x n] (:machines pgraph)] [:li n "x " (get-machine-name x)]))]]
      ;;  [c/card-lg [c/form-group {:label "Recipes"}
      ;;              (into [:ul] (for [[x n] (:recipes pgraph)] [:li n "x " (get-recipe-name x)]))]]
       [c/card-lg [c/form-group {:label "Production Graph"}
                   [pgraph-tree pg]]]
       [c/card-lg
        [c/form-group {:label "Production Graph (raw)"}
         [c/textarea {:value (pr-str (dissoc pg :world))
                      :read-only true
                      :style {:width "100%" :height "150px"}}]]
        [c/form-group {:label "Factory Data (raw)"}
         [c/textarea {:value (pr-str factory)
                      :read-only true
                      :style {:width "100%" :height "150px"}}]]]])
    [c/non-ideal-state {:icon :office
                        :title "No factories!"
                        :description "Create a factory to get started."
                        :action (as-element [c/button {:text "Create Factory"
                                                       :intent :success
                                                       :on-click create-and-select-factory}])}]))