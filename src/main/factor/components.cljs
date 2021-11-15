(ns factor.components
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [factor.util :refer [c]]
            ["@blueprintjs/core" :as b]
            ["@blueprintjs/select" :as bs]
            ["ag-grid-react" :refer [AgGridReact]]
            [clojure.string :as string]
            [factor.util :refer [ipairs try-fn delete-index move-index-ahead move-index-behind]]
            [com.rpl.specter :as s]
            [medley.core :refer [map-keys]]))

;; WRAPPER COMPONENTS
;; These components are simple wrappers around HTML/Blueprint elements.

(defn alert [p & children] (into [(c b/Alert) p] children))
(defn anchor-button [props & children] (into [(c b/AnchorButton) props] children))
(defn button [props & children] (into [(c b/Button) props] children))
(defn card [p & children] (into [(c b/Card) p] children))
(defn card-lg [& children] (into [(c b/Card) {:class-name "w-20 m-1"}] children))
(defn card-md [& children] (into [(c b/Card) {:class-name "w-16 m-1"}] children))
(defn card-sm [& children] (into [(c b/Card) {:class-name "w-12 m-1"}] children))
(defn collapsible [label & children] (into [:details [:summary label]] children))
(defn control-group [props & children] (into [(c b/ControlGroup) props] children))
(defn form-group [props & children] (into [(c b/FormGroup) props] children))
(defn icon [p] [(c b/Icon) p])
(defn input-group [props & children] (into [(c b/InputGroup) props] children))
(defn menu-item [p & children] (into [(c b/MenuItem) p] children))
(defn navbar [& children] (into [(c b/Navbar)] children))
(defn navbar-divider [] [(c b/Navbar.Divider)])
(defn navbar-group-left [& children] (into [(c b/Navbar.Group) {:align :left}] children))
(defn navbar-group-right [& children] (into [(c b/Navbar.Group) {:align :right}] children))
(defn navbar-heading [& children] (into [(c b/Navbar.Heading)] children))
(defn non-ideal-state [p & children] (into [(c b/NonIdealState) p] children))
(defn textarea [props] [(c b/TextArea) props])
(defn tree [p] [(c b/Tree) p])
(defn tree-node [p & children] (into [(c b/TreeNode) p] children))

;; OTHER COMPONENTS
;; Non-wrapper components follow. Each should have a docstring explaining its purpose.

(defn select-button
  "A button intended to be used as the display element for a Select dropdown."
  [text]
  [button {:text text :right-icon :double-caret-vertical}])

(defn select-enum
  "A wrapper around Blueprint's Select component, designed for selecting one of an enumerated set of possible values."
  [possible-values initial-value on-change]
  (reagent/with-let [query (reagent/atom "")]
    [(c bs/Select) {:items possible-values
                    :query @query
                    :active-item initial-value
                    :on-query-change #(reset! query %)
                    :on-item-select on-change
                    :item-renderer (fn [item opts] 
                                     (let [on-click (.-handleClick opts)
                                           active? (.-active (.-modifiers opts))]
                                       (reagent/as-element [menu-item {:key item
                                                                       :on-click on-click
                                                                       :text item
                                                                       :intent (when active? "primary")}])))}
     [select-button initial-value]]))

(defn suggest
  "A wrapper around Blueprint's Suggest component, designed for selecting one of the object types from the World.
   (e.g. if `type` is `:item`, it will suggest items.)"
  [type value on-change]
  (let [type-name (name type)
        ids->names @(subscribe [(keyword (str type-name "-ids->names"))])]
    [(c bs/Suggest) {:items (if (not-empty ids->names) (keys ids->names) [])
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

(defn numeric-input
  "A numeric input with relatively narrow width, intended to fit integers less than 1000 compactly.
   Any provided `opts` will be passed through to the underlying NumericInput component."
  [value on-change opts]
  [(c b/NumericInput) (merge {:value value
                              :async-control true
                              :on-value-change on-change
                              :style {:width "2.5rem"}}
                             opts)])

(defn suggest-numeric
  "A control group used to input a numeric quantity combined with a reference to an object like an item or a machine.
   Used, for example, to select the number of each type of item that's required as input for a recipe."
  [type [x n] on-change]
  [control-group
   [numeric-input n #(on-change [x %]) {:min 0}]
   [suggest type  x #(on-change [% n])]])

(defn suggest-numeric-deletable
  "Like suggest-numeric, but includes a delete button on the right of the control. The `on-delete` callback is called when
   the delete button is pressed."
  [type kvp on-change on-delete]
  [control-group
   [suggest-numeric type kvp on-change]
   [button {:icon :delete :intent :danger :on-click on-delete}]])

(defn suggest-numeric-addable
  "Like suggest-numeric, but includes an add (+) button on the right of the control. The `on-add` function is called when
   the add button is pressed."
  [type kvp on-change on-add]
  [control-group
   [suggest-numeric type kvp on-change]
   [button {:icon :plus :intent :success :on-click #(on-add kvp)}]])

(defn quantity-set-input
  "A complex control for configuring the contents of a qmap. Each entry in the qmap is rendered as a line that's editable
   and deletable. There's also a line for adding new entries to the qmap. The `on-change` function is called with the full updated
   qmap whenever any updates are performed."
  [type quantity-set on-change]
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

(defn list-input
  "A complex control for editing a list of objects (items, machines, etc.) Each entry in the list is rendered as a line that's editable
   and deletable. There's also a line for adding new entries to the list. Lines can be moved up and down. Duplicates are allowed.
   The `on-change` function is called with the full updated list whenever any updates are performed."
  [type value on-change]
  (reagent/with-let [new-val (reagent/atom nil)]
    (let [update-value-at-index (fn [ix v] (on-change (assoc value ix v)))
          delete-value-at-index (fn [ix] (on-change (delete-index value ix)))
          move-value-at-index-up (fn [ix] (on-change (move-index-behind value ix)))
          move-value-at-index-down (fn [ix] (on-change (move-index-ahead value ix)))
          update-new-value #(reset! new-val %)
          add-new-value (fn [x]
                          (on-change (conj value x))
                          (update-new-value nil))]
      (conj
       (into [:<>] (for [[v i] (ipairs value)]
                     [control-group
                      [button {:icon :chevron-up :on-click #(move-value-at-index-up i) :disabled (= i 0)}]
                      [button {:icon :chevron-down :on-click #(move-value-at-index-down i) :disabled (= i (dec (count value)))}]
                      [suggest type v #(update-value-at-index i %)]
                      [button {:icon :delete :on-click #(delete-value-at-index i)}]]))
       [control-group
        [suggest type @new-val update-new-value]
        [button {:icon :plus :on-click #(add-new-value @new-val)}]]))))

(defn input
  "A wrapper for Blueprint's InputGroup component. All props are passed through to the underlying InputGroup.
   The only exception is the on-change prop, which is wrapped such that the value is already extracted from
   the InputGroup's React.SyntheticEvent before being passed to the `on-change` function."
  [{:keys [on-change] :as props}]
  (let [default-props {:async-control true}
        override-props {:on-change #(-> % (.-target) (.-value) (on-change))}
        props (merge default-props props override-props)]
    [(c b/InputGroup) props]))

(defn grid-cb->clj
  "Accepts an ag-grid style event (as would be passed to a grid callback) and returns a Clojureified version.
   Note the :api part of the event is not Clojurified because it's mostly methods."
  [ev]
  {:api (.-api ev)
   :data (-> ev (.-data) (js->clj) (->> (map-keys keyword)))})

(defn grid-cb
  "Accepts a callback function `cb` and returns a function suitable for use as an ag-grid event handler."
  [cb]
  (fn [ev]
    (->> ev (grid-cb->clj) (try-fn cb))))

(defn grid-value-parser-for-floats
  [ev]
  (js/parseFloat (.-newValue ev)))

(defn grid
  "Wrapper around ag-grid's Grid component. Sets a variety of default props for DRY-ness. All defaults can be
   overriden with `props`. Note that usually you want to specify at least the :on-row-value-changed prop. The
   `children` can contain column definitions, but it's usually simpler to use the :column-defs option in `props`."
  [props & children]
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

(defn nav-link
  "A minimally styled button that, when clicked, will change the currently selected page."
  [page icon text]
  (let [selected-page @(subscribe [:ui [:selected-page]])]
    [button {:class :bp3-minimal
             :on-click #(dispatch [:ui [:selected-page] page])
             :icon icon
             :text text
             :disabled (= selected-page page)}]))

(defn alerting-button
  "A button that will pop up a confirmation dialog when clicked. Allows specifying two sets of props:
   `button-props` are applied to the button, and `alert-props` to the popup alert.
   If the user confirms the alert, the :on-confirm key in `alert-props` will be called."
  [button-props alert-props & children]
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