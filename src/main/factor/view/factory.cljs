(ns factor.view.factory
  (:require [factor.pgraph :as pgraph]
            [factor.navs :as nav]
            [re-frame.core :refer [dispatch subscribe dispatch-sync]]
            [reagent.core :refer [as-element with-let]]
            [factor.qmap :as qmap]
            [factor.util :refer [callback-factory-factory]]
            [factor.components.cmd :refer [cmd-btn]]
            [factor.components.wrappers :refer [navbar-divider tree non-ideal-state textarea icon callout divider form-group card-lg button]]
            [factor.components.inputs :refer [quantity-set-input input set-input]]))

(defn open-factory-id
  []
  (second @(subscribe [:page-route])))

(defn tools []
  [:<>
   [cmd-btn {:cmd :new-factory :minimal true :intent :success}]
   [navbar-divider]
   [cmd-btn {:cmd :delete-open-factory :minimal true :intent :danger}]])


(defn get-item-name [id] (:name @(subscribe [:item id])))

(defn pgraph-tree-node
  [pg node-states seen-nodes parent-id node-id]
  (let [seen? (seen-nodes node-id)
        {:keys [recipe] :as node} (pgraph/get-node pg node-id)
        tree-node-id (str node-id "-" parent-id)
        {:keys [expanded selected disabled] :or {expanded true}} (get node-states tree-node-id)
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
      [tree {:contents (clj->js [(pgraph-tree-node pg @node-states #{} nil (:id (pgraph/desired-output-node pg)))])
               :on-node-expand on-expand
               :on-node-collapse on-collapse}])))

(defn pgraph-tree
  []
  (let [id (open-factory-id)]
   (if (empty? @(subscribe [:factory-desired-output id]))
    [non-ideal-state {:icon :arrow-left
                        :title "Factory is empty"
                        :description "Add your desired output item(s) in the sidebar to the left."}]
    [pgraph-tree-inner id])))

(defn node-list-inner
  [id]
  (let [pg @(subscribe [:factory-pgraph id])]
    (into [:ul] (for [node (pgraph/all-nodes pg) :when (and (not= (:id node) :excess)
                                                            (not= (:id node) :missing)
                                                            (not= (:id node) (:id (pgraph/desired-output-node pg))))]
                  [:li (str (get node :num-machines) "x " (:name @(subscribe [:recipe (get-in node [:recipe :id])])))]))))

(defn node-list
  []
  (let [id (open-factory-id)]
    (if (empty? @(subscribe [:factory-desired-output id]))
      [:ul [:li "None"]]
      [node-list-inner id])))

(defn catalyst-list
  []
  (let [catalysts (pgraph/all-catalysts @(subscribe [:factory-pgraph (open-factory-id)]))]
    (if (not-empty catalysts)
      (into [:ul] (for [[k v] catalysts] [:li (str v "x " (get-item-name k))]))
      [:ul [:li "None"]])))

(defn factory-name-editor
  []
  (with-let [dispatch-update (fn [id n] (dispatch-sync [:update-factory-name id n]))
             updater (callback-factory-factory dispatch-update)]
    (let [id (open-factory-id)
          name @(subscribe [:factory-name id])]
      [input {:value name :on-change (updater id)}])))

(defn factory-desired-output-editor
  []
  (with-let [dispatch-update (fn [id n] (dispatch-sync [:update-factory-desired-output id n]))
             updater (callback-factory-factory dispatch-update)]
    (let [id (open-factory-id)
          value @(subscribe [:factory-desired-output id])]
      [quantity-set-input {:type :item
                             :value value
                             :on-change (updater id)}])))

(defn factory-id-editor
  []
  [input {:value (open-factory-id) :disabled true}])

(defn item-list-entry
  [id n]
  [:li {:key id} n "x " (get-item-name id)])

(defn item-list-for-qmap
  [m]
  (if (not-empty m)
    (into [:ul] (for [[x n] m] [item-list-entry x n]))
    [:ul [:li "None"]]))

(defn factory-raw-data
  []
  [textarea {:value (pr-str @(subscribe [:factory (open-factory-id)]))
             :read-only true
             :style {:width "100%" :height "150px"}}])

(defn pgraph-raw-data
  []
  [textarea {:value (pr-str @(subscribe [:factory-pgraph (open-factory-id)]))
             :read-only true
             :style {:width "100%" :height "150px"}}])

(defn pgraph-dot-data
  []
  [textarea {:value (pgraph/pg->dot @(subscribe [:factory-pgraph (open-factory-id)]))
             :read-only true
             :style {:width "100%" :height "150px"}}])

(defn factory-excess-outputs
  []
  [item-list-for-qmap (pgraph/excess-output @(subscribe [:factory-pgraph (open-factory-id)]))])

(defn factory-missing-inputs
  []
  [item-list-for-qmap (pgraph/missing-input @(subscribe [:factory-pgraph (open-factory-id)]))])

(defn pgraph-pane []
  (let [item-rate-unit @(subscribe [:unit :item-rate])]
    [:div.pgraph-pane
     [:div.pgraph-pane-left
      [form-group {:label "Factory Name"} [factory-name-editor]]
      [form-group {:label (str "Desired Outputs (" item-rate-unit ")")} [factory-desired-output-editor]]
      [form-group {:label (str "Excess Outputs (" item-rate-unit ")")} [factory-excess-outputs]]
      [form-group {:label (str "Required Inputs (" item-rate-unit ")")} [factory-missing-inputs]]
      [form-group {:label (str "Required Catalysts (" item-rate-unit ")")} [catalyst-list]]
      [form-group {:label (str "Crafting Stages")} [node-list]]]
     [divider]
     [:div.pgraph-pane-right
      [callout {:title "Production Graph" :style {:margin-bottom "1rem"}}
       [icon {:icon :office :color "#5c7080"}] " Desired Output :: "
       [icon {:icon :data-lineage :color "#5c7080"}] " Crafting Stage :: "
       [icon {:icon :cube :color "#5c7080"}] " Required Input :: "
       [icon {:icon :lab-test :color "#5c7080"}] " Required Catalyst"]
      [pgraph-tree]]]))

(defn filter-editor [type filter-key]
  (with-let [update-fn (fn [id k v] (dispatch [:update-factory-filter id k v]))
             update-factory (callback-factory-factory update-fn)]
    (let [[_ id] @(subscribe [:page-route])
          filter-set @(subscribe [:factory-filter id filter-key])]
      [set-input {:type type
                  :value filter-set
                  :on-change (update-factory id filter-key)}])))

(defn filter-pane []
  [:div.filter-pane
   [callout {:title "Item/Machine/Recipe Filters" :style {:margin "1rem 1rem 0 1rem" :max-width "1024px"}}
    [:p
     "Use the following lists to exclude items/machines/recipes from being used when generating Production Graphs for this factory. "
     "Deny-lists exclude the items/machines/recipes on the list, and allow-lists (if set) exclude the items/machines/recipes NOT on the list. "
     "The " [:em "soft"] " versions can be used to express a preference to use items/machines/recipes over others, if possible."]
    [:p "For example, if you don't have some recipes researched yet, add them to Denied Recipes to avoid using them."]
    [:p "Or, if you would prefer to use Machine A over Machines B and C whenever possible, "
     "add Machine A to Allowed Machines (Soft)."]
    [:p "These settings are specific to this factory."]]
   [:div.filter-row
    [form-group {:label "Allowed Items" :class "filter-editor"} [filter-editor :item nav/HARD-ALLOWED-ITEMS]]
    [form-group {:label "Allowed Items (Soft)" :class "filter-editor"} [filter-editor :item nav/SOFT-ALLOWED-ITEMS]]
    [form-group {:label "Denied Items" :class "filter-editor"} [filter-editor :item nav/HARD-DENIED-ITEMS]]
    [form-group {:label "Denied Items (Soft)" :class "filter-editor"} [filter-editor :item nav/SOFT-DENIED-ITEMS]]]
   [:div.filter-row
    [form-group {:label "Allowed Machines" :class "filter-editor"} [filter-editor :machine nav/HARD-ALLOWED-MACHINES]]
    [form-group {:label "Allowed Machines (Soft)" :class "filter-editor"} [filter-editor :machine nav/SOFT-ALLOWED-MACHINES]]
    [form-group {:label "Denied Machines" :class "filter-editor"} [filter-editor :machine nav/HARD-DENIED-MACHINES]]
    [form-group {:label "Denied Machines (Soft)" :class "filter-editor"} [filter-editor :machine nav/SOFT-DENIED-MACHINES]]]
   [:div.filter-row
    [form-group {:label "Allowed Recipes" :class "filter-editor"} [filter-editor :recipe nav/HARD-ALLOWED-RECIPES]]
    [form-group {:label "Allowed Recipes (Soft)" :class "filter-editor"} [filter-editor :recipe nav/SOFT-ALLOWED-RECIPES]]
    [form-group {:label "Denied Recipes" :class "filter-editor"} [filter-editor :recipe nav/HARD-DENIED-RECIPES]]
    [form-group {:label "Denied Recipes (Soft)" :class "filter-editor"} [filter-editor :recipe nav/SOFT-DENIED-RECIPES]]]])

(defn debug-pane []
 [:div.card-stack
  [card-lg
   [form-group {:label "ID"} [factory-id-editor]]
   [form-group {:label "Dot-Formatted Production Graph (WARNING: Data is not sanitized. Don't use with untrusted worlds!)"} [pgraph-dot-data]]
   [form-group {:label "Raw Data - Production Graph"} [pgraph-raw-data]]
   [form-group {:label "Raw Data - Factory Object"} [factory-raw-data]]]])

(defn invalid-factory
  []
  (with-let [go-home #(dispatch [:update-route [:home]])
             action (as-element [button {:text "Go Home"
                                         :intent :success
                                         :on-click go-home}])]
    [non-ideal-state {:icon :office
                        :title "Unknown factory"
                        :description "Oops! This factory doesn't exist!"
                        :action action}]))

(defn page []
 (let [[_ id subpage] @(subscribe [:page-route])]
  (if-not (and id @(subscribe [:factory-exists? id])) [invalid-factory]
   (case subpage
     :filters [filter-pane]
     :debug [debug-pane]
     [pgraph-pane]))))