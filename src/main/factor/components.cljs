(ns factor.components
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [factor.util :refer [c]]
            ["@blueprintjs/core" :as b]
            ["@blueprintjs/select" :as bs]
            ["ag-grid-react" :refer [AgGridReact]]
            [clojure.string :as string]
            [factor.util :refer [without]]
            [medley.core :refer [map-keys filter-keys]]))

(defn icon [p]
  [(c b/Icon) p])

(defn non-ideal-state [p & children]
  (into [(c b/NonIdealState) p] children))

(defn button [props & children] (into [(c b/Button) props] children))
(defn anchor-button [props & children] (into [(c b/AnchorButton) props] children))

(defn control-group [props & children]
  (into [(c b/ControlGroup) props] children))

(defn form-group [props & children]
  (into [(c b/FormGroup) props] children))

(defn textarea [props]
  [(c b/TextArea) props])

(defn menu-item [p & children] (into [(c b/MenuItem) p] children))

(defn suggest [type value on-change]
  (let [type-name (name type)
        ids->names @(subscribe [(keyword (str type-name "-names"))])]
    [(c bs/Suggest) {:items (keys ids->names)
                     :selected-item value
                     :on-item-select on-change
                     :no-results (reagent/as-element [menu-item {:disabled true :text "No matching results."}])
                     :input-value-renderer (fn [id]
                                             (or (ids->names id) ""))
                     :item-renderer (fn [id opts]
                                      (let [on-click (.-handleClick opts)
                                            active? (.-active (.-modifiers opts))
                                            disabled? (.-disabled (.-modifiers opts))
                                            matches-predicate? (.-matchesPredicate (.-modifiers opts))]
                                        (when matches-predicate?
                                          (reagent/as-element
                                           [menu-item {:key id
                                                       :on-click on-click
                                                       :text (ids->names id)
                                                       :label (subs id 0 3)
                                                       :disabled disabled?
                                                       :intent (when active? "primary")}]))))
                     :item-predicate (fn [qs id]
                                       (string/includes? (string/lower-case (ids->names id))
                                                         (string/lower-case qs)))}]))

(defn suggest-item [value on-change]
  (let [ids->names @(subscribe [:item-names])]
    [(c bs/Suggest) {:items (keys ids->names)
                     :selected-item value
                     :on-item-select on-change
                     :no-results (reagent/as-element [menu-item {:disabled true :text "No matching results."}])
                     :input-value-renderer (fn [id]
                                             (or (ids->names id) ""))
                     :item-renderer (fn [id opts]
                                      (let [on-click (.-handleClick opts)
                                            active? (.-active (.-modifiers opts))
                                            disabled? (.-disabled (.-modifiers opts))
                                            matches-predicate? (.-matchesPredicate (.-modifiers opts))]
                                        (when matches-predicate?
                                          (reagent/as-element
                                           [menu-item {:key id
                                                       :on-click on-click
                                                       :text (ids->names id)
                                                       :label (subs id 0 3)
                                                       :disabled disabled?
                                                       :intent (when active? "primary")}]))))
                     :item-predicate (fn [qs id]
                                       (string/includes? (string/lower-case (ids->names id))
                                                         (string/lower-case qs)))}]))

(defn numeric-input [value on-change opts]
  [(c b/NumericInput) (merge {:value value
                              :on-value-change on-change
                              :style {:width "2.5rem"}}
                             opts)])

(defn suggest-numeric [type [x n] on-change]
  [control-group
   [numeric-input n #(on-change [x %]) {:min 0}]
   [suggest type  x #(on-change [% n])]])

(defn suggest-numeric-deletable [type kvp on-change on-delete]
  [control-group
   [suggest-numeric type kvp on-change]
   [button {:icon :delete :intent :danger :on-click on-delete}]])

(defn suggest-numeric-addable [type kvp on-change on-add]
  [control-group
   [suggest-numeric type kvp on-change]
   [button {:icon :plus :intent :success :on-click #(on-add kvp)}]])

(defn quantity-set-input [type quantity-set on-change]
  (reagent/with-let [new-quantity (reagent/atom [])]
    (let [update-quantity (fn [[id num] oid] (-> quantity-set (dissoc oid) (assoc id num) (on-change)))
          delete-quantity (fn [id]           (-> quantity-set (dissoc id) (on-change)))
          update-new-quantity #(reset! new-quantity %)
          add-new-quantity (fn [x]
                             (update-quantity x nil)
                             (update-new-quantity []))]
      (conj
       (into [:<>] (for [[id num] quantity-set]
                     [suggest-numeric-deletable type [id num] #(update-quantity % id) #(delete-quantity id)]))
       [suggest-numeric-addable type @new-quantity update-new-quantity add-new-quantity]))))

(defn suggest-item-numeric [[x n] on-change]
  [control-group
   [numeric-input n #(on-change [x %]) {:min 0}]
   [suggest-item  x #(on-change [% n])]])

(defn suggest-item-numeric-deletable [kvp on-change on-delete]
  [control-group
   [suggest-item-numeric kvp on-change]
   [button {:icon :delete :intent :danger :on-click on-delete}]])

(defn suggest-item-numeric-addable [kvp on-change on-add]
  [control-group
   [suggest-item-numeric kvp on-change]
   [button {:icon :plus :intent :success :on-click #(on-add kvp)}]])

(defn quantity-set-input-item [quantity-set on-change]
  (reagent/with-let [new-quantity (reagent/atom [])]
    (let [update-quantity (fn [[id num] oid] (-> quantity-set (dissoc oid) (assoc id num) (on-change)))
          delete-quantity (fn [id]           (-> quantity-set (dissoc id) (on-change)))
          update-new-quantity #(reset! new-quantity %)
          add-new-quantity (fn [x]
                             (update-quantity x nil)
                             (update-new-quantity []))]
      (conj
       (into [:<>] (for [[id num] quantity-set]
                     [suggest-item-numeric-deletable [id num] #(update-quantity % id) #(delete-quantity id)]))
       [suggest-item-numeric-addable @new-quantity update-new-quantity add-new-quantity]))))

(defn list-input [type value on-change]
  (reagent/with-let [new-val (reagent/atom nil)]
    (let [update-value (fn [v ov] (-> value (without ov) (conj v) (on-change)))
          delete-value (fn [v]    (-> value (without v) (on-change)))
          update-new-value #(reset! new-val %)
          add-new-value (fn [x]
                             (update-value x nil)
                             (update-new-value nil))]
      (conj
       (into [:<>] (for [v value]
                     [control-group
                      [suggest type v #(update-value % v)]
                      [button {:icon :delete :on-click #(delete-value v)}]]))
       [control-group
        [suggest type @new-val update-new-value]
        [button {:icon :plus :on-click #(add-new-value @new-val)}]]))))

(defn input [{:keys [on-change] :as props}]
  (let [override-props {:on-change #(-> % (.-target) (.-value) (on-change))}
        props (merge props override-props)]
    [(c b/InputGroup) props]))

(defn collapsible [label & children]
  (into [:details [:summary label]] children))

(defn card [p & children]
  (into [(c b/Card) p] children))

(defn card-sm [& children]
  (into [(c b/Card) {:class-name "w-12 m-1"}] children))

(defn card-md [& children]
  (into [(c b/Card) {:class-name "w-16 m-1"}] children))

(defn card-lg [& children]
  (into [(c b/Card) {:class-name "w-20 m-1"}] children))

(defn grid-cb->clj [ev]
  {:api (.-api ev)
   :data (-> ev (.-data) (js->clj) (->> (map-keys keyword)))})

(defn try-fn [f & args] (when (fn? f) (apply f args)))

(defn grid-cb [cb]
  (fn [ev]
    (->> ev (grid-cb->clj) (try-fn cb))))


(defn grid [props & children]
  (let [default-props  {:row-selection :multiple
                        :enter-moves-down true
                        :enter-moves-down-after-edit true
                        :edit-type "fullRow"
                        :default-col-def {:sortable true}}
        override-props {:on-row-value-changed (grid-cb (:on-row-value-changed props))
                        :on-grid-ready        (grid-cb (:on-grid-ready        props))
                        :on-selection-changed (grid-cb (:on-selection-changed props))}
        grid-props     (merge default-props props override-props)]
    [:div.ag-theme-alpine.full-screen
     (into [(c AgGridReact) grid-props] children)]))
 
(defn navbar [& children] (into [(c b/Navbar)] children))
(defn navbar-heading [& children] (into [(c b/Navbar.Heading)] children))
(defn navbar-divider [] [(c b/Navbar.Divider)])
(defn navbar-group-left [& children] (into [(c b/Navbar.Group) {:align :left}] children))
(defn navbar-group-right [& children] (into [(c b/Navbar.Group) {:align :right}] children))

(defn nav-link [page icon text]
  (let [selected-page @(subscribe [:ui [:selected-page]])]
    [button {:class :bp3-minimal
             :on-click #(dispatch [:ui [:selected-page] page])
             :icon icon
             :text text
             :disabled (= selected-page page)}]))

(defn tree [p]
  [(c b/Tree) p])

(defn tree-node [p & children]
  (into [(c b/TreeNode) p] children))

(defn alert [p & children]
  (into [(c b/Alert) p] children))

(defn alerting-button [button-props alert-props & children]
  (reagent/with-let [open? (reagent/atom false)]
    [:<>
     [button (assoc button-props :on-click #(reset! open? true))]
     (into [alert (merge {:is-open @open?
                          :can-escape-key-cancel true
                          :can-outside-click-cancel true
                          :confirm-button-text "Confirm"
                          :cancel-button-text "Cancel"
                          :on-close #(reset! open? false)} alert-props)]
           children)]))