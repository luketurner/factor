(ns factor.view.factory
  (:require [factor.components :as c]
            [factor.pgraph :as pgraph]
            [re-frame.core :refer [dispatch subscribe dispatch-sync]]
            [reagent.core :refer [as-element with-let]]
            [factor.qmap :as qmap]
            [factor.util :refer [callback-factory-factory]]))

(defn delete-open-factory-button
  []
  (with-let [delete-factory #(dispatch [:delete-factories [%]])
             delete-cb-factory (callback-factory-factory delete-factory)]
    (let [selected-factory @(subscribe [:open-factory])]
      [c/alerting-button
       {:text "Delete"
        :class :bp3-minimal
        :icon :delete
        :intent :danger}
       {:on-confirm (delete-cb-factory selected-factory)
        :intent :danger
        :confirm-button-text "Delete It!"}
       [:p "This will permanently delete this factory! (Your items/machines/recipes will stick around.)"]])))

(defn select-open-factory-input
  []
  (with-let [select-factory #(dispatch [:open-factory %])]
    [c/suggest {:type :factory
                :value @(subscribe [:open-factory])
                :on-item-select select-factory}]))

(defn create-factory-button
 []
 (with-let [create-factory #(dispatch [:create-factory])]
   [c/button {:class :bp3-minimal
              :intent :success
              :on-click create-factory
              :icon :plus
              :text "New"}]))

(defn navbar []
  [c/navbar
   [c/navbar-group-left
    [c/navbar-heading "Factories"]
    [select-open-factory-input]
    [delete-open-factory-button]
    [create-factory-button]
    [c/navbar-divider]
    [c/undo-redo]
    [c/navbar-divider]]])


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

(defn pgraph-tree-inner
  [id]
  (with-let [node-states (reagent.core/atom {})
             on-expand #(swap! node-states assoc-in [(.-id %) :expanded] true)
             on-collapse #(swap! node-states assoc-in [(.-id %) :expanded] false)]
    (let [pg @(subscribe [:factory-pgraph id])]
      [c/tree {:contents (clj->js [(pgraph-tree-node pg @node-states #{} nil (:id (pgraph/desired-output-node pg)))])
               :on-node-expand on-expand
               :on-node-collapse on-collapse}])))

(defn pgraph-tree
  [id]
  (if (empty? @(subscribe [:factory-desired-output id]))
    [:p "Add desired output(s) to view production graph."]
    [pgraph-tree-inner id]))

(defn node-list-inner
  [id]
  (let [pg @(subscribe [:factory-pgraph id])]
    (into [:ul] (for [node (pgraph/all-nodes pg) :when (and (not= (:id node) :excess)
                                                            (not= (:id node) :missing)
                                                            (not= (:id node) (:id (pgraph/desired-output-node pg))))]
                  [:li (str (get node :num-machines) "x " (:name @(subscribe [:recipe (get-in node [:recipe :id])])))]))))

(defn node-list
  [id]
  (if (empty? @(subscribe [:factory-desired-output id]))
    [:p "Add desired output(s) to view production stages."]
    [node-list-inner id]))

(defn catalyst-list
  [id]
  (if-let [catalysts (pgraph/all-catalysts @(subscribe [:factory-pgraph id]))]
    (into [:ul] (for [[k v] catalysts] [:li (str v "x " (get-item-name k))]))))

(defn factory-name-editor
  [id]
  (with-let [dispatch-update (fn [id n] (dispatch-sync [:update-factory-name id n]))
             updater (callback-factory-factory dispatch-update)]
    (let [name @(subscribe [:factory-name id])]
      [c/input {:value name :on-change (updater id)}])))

(defn factory-desired-output-editor
  [id]
  (with-let [dispatch-update (fn [id n] (dispatch-sync [:update-factory-desired-output id n]))
             updater (callback-factory-factory dispatch-update)]
    (let [value @(subscribe [:factory-desired-output id])]
      [c/quantity-set-input {:type :item
                             :value value
                             :on-change (updater id)}])))

(defn factory-id-editor
  [id]
  [c/input {:value id :disabled true}])

(defn item-list-entry
  [id n]
  [:li {:key id} n "x " (get-item-name id)])

(defn item-list-for-qmap
  [m]
  (into [:ul] (for [[x n] m] [item-list-entry x n])))

(defn factory-raw-data
  [id]
  [c/textarea {:value (pr-str @(subscribe [:factory id]))
               :read-only true
               :style {:width "100%" :height "150px"}}])

(defn pgraph-raw-data
  [id]
  [c/textarea {:value (pr-str @(subscribe [:factory-pgraph id]))
               :read-only true
               :style {:width "100%" :height "150px"}}])

(defn pgraph-dot-data
  [id]
  [c/textarea {:value (pgraph/pg->dot @(subscribe [:factory-pgraph id]))
               :read-only true
               :style {:width "100%" :height "150px"}}])

(defn factory-excess-outputs
  [id]
  [item-list-for-qmap (pgraph/excess-output @(subscribe [:factory-pgraph id]))])

(defn factory-missing-inputs
  [id]
  [item-list-for-qmap (pgraph/missing-input @(subscribe [:factory-pgraph id]))])

(defn no-factories
  []
  (with-let [create-factory #(dispatch [:create-factory])
             action (as-element [c/button {:text "Create Factory"
                                           :intent :success
                                           :on-click create-factory}])]
    [c/non-ideal-state {:icon :office
                        :title "No factories!"
                        :description "Create a factory to get started."
                        :action action}]))

(defn page []
  (if-let [id @(subscribe [:open-factory])]
    (let [item-rate-unit @(subscribe [:unit :item-rate])]
      [:div.card-stack
       [c/card-lg
        [c/form-group {:label "ID"} [factory-id-editor id]]
        [c/form-group {:label "Name"} [factory-name-editor id]]]
       [c/card-lg [c/form-group {:label (str "Desired Outputs (" item-rate-unit ")")} [factory-desired-output-editor id]]]
       [c/card-lg [c/form-group {:label (str "Excess Outputs (" item-rate-unit ")")} [factory-excess-outputs id]]]
       [c/card-lg [c/form-group {:label (str "Needed Inputs (" item-rate-unit ")")} [factory-missing-inputs id]]]
       [c/card-lg [c/form-group {:label (str "Catalysts (" item-rate-unit ")")} [catalyst-list id]]]
       [c/card-lg [c/form-group {:label (str "Production Graph (" item-rate-unit ")")} [pgraph-tree id]]]
       [c/card-lg [c/form-group {:label (str "Production Stages (" item-rate-unit ")")} [node-list id]]]
       [c/card-lg
        [c/form-group {:label "Dot-Formatted Production Graph (WARNING: Data is not sanitized. Don't use with untrusted worlds!)"} [pgraph-dot-data id]]
        [c/form-group {:label "Raw Data - Production Graph"} [pgraph-raw-data id]]
        [c/form-group {:label "Raw Data - Factory Object"} [factory-raw-data id]]]])
    [no-factories]))