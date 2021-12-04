(ns factor.components
  "Defines reusable components for use in views. Includes wrappers for Blueprint components."
  (:require-macros [factor.components :refer [defcomponent defwrapper]])
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [with-let]]
            ["@blueprintjs/core" :as b]
            ["@blueprintjs/select" :as bs]
            ["ag-grid-react" :refer [AgGridReact]]
            [clojure.string :as string]
            [factor.util :refer [cl ipairs try-fn delete-index move-index-ahead move-index-behind callback-factory-factory]]
            [medley.core :refer [map-keys]]))

;; WRAPPER COMPONENTS
;; These components are simple wrappers around HTML/Blueprint elements.

(defwrapper alert           (cl b/Alert))
(defwrapper anchor-button   (cl b/AnchorButton))
(defwrapper button          (cl b/Button))
(defwrapper card            (cl b/Card))
(defwrapper control-group   (cl b/ControlGroup))
(defwrapper divider         (cl b/Divider))
(defwrapper form-group      (cl b/FormGroup))
(defwrapper icon            (cl b/Icon))
(defwrapper input-group     (cl b/InputGroup))
(defwrapper callout         (cl b/Callout))
(defwrapper menu-item       (cl b/MenuItem))
(defwrapper navbar          (cl b/Navbar))
(defwrapper navbar-divider  (cl b/Navbar.Divider))
(defwrapper navbar-group    (cl b/Navbar.Group))
(defwrapper navbar-heading  (cl b/Navbar.Heading))
(defwrapper non-ideal-state (cl b/NonIdealState))
(defwrapper textarea        (cl b/TextArea))
(defwrapper tree            (cl b/Tree))
(defwrapper tree-node       (cl b/TreeNode))
  
; these wrappers are further encapsulated by other components
; and usually shouldn't be used directly
(defwrapper numeric-input-raw (cl b/NumericInput))
(defwrapper grid-raw          (cl AgGridReact))
(defwrapper select-raw        (cl bs/Select))

;; SEMI-WRAPPERS
;; These are wrappers that adjust the default props without adding their own rendering/logic/etc.

(defwrapper card-lg            card         {:class-name "w-20 m-1"})
(defwrapper card-md            card         {:class-name "w-16 m-1"})
(defwrapper card-sm            card         {:class-name "w-12 m-1"})
(defwrapper navbar-group-left  navbar-group {:align :left})
(defwrapper navbar-group-right navbar-group {:align :right})

;; OTHER COMPONENTS
;; Non-wrapper components follow. Each should have a docstring explaining its purpose.

(defcomponent select-enum
  "A wrapper around Blueprint's Select component, designed for selecting one of an enumerated set of possible values."
  [{:keys [items on-item-select initial-value]} _]
  (with-let [query (reagent/atom "")
             reset-query #(reset! query %)
             item-renderer (fn [item opts]
                             (let [on-click (.-handleClick opts)
                                   active? (.-active (.-modifiers opts))]
                               (reagent/as-element [menu-item {:key item
                                                               :on-click on-click
                                                               :text item
                                                               :intent (when active? "primary")}])))]
    [select-raw {:items items
                 :query @query
                 :active-item initial-value
                 :on-query-change reset-query
                 :on-item-select on-item-select
                 :item-renderer item-renderer}
     [button {:text initial-value :right-icon :double-caret-vertical}]]))

(defcomponent suggest
  "A wrapper around Blueprint's Suggest component, designed for selecting one of the object types from the World.
   (e.g. if `type` is `:item`, it will suggest items.)"
  [{:keys [type value on-item-select]} _]
  (with-let [render-input-value (fn [ids->names id]
                                  (or (ids->names id) ""))
             render-item (fn [ids->names id opts]
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
             display-item? (fn [ids->names qs id]
                             (string/includes? (string/lower-case (ids->names id))
                                               (string/lower-case qs)))
             item-predicate (callback-factory-factory display-item?)
             item-renderer (callback-factory-factory render-item)
             input-value-renderer (callback-factory-factory render-input-value)
             no-results (reagent/as-element [menu-item {:disabled true :text "No matching results."}])]
    (let [type-name (name type)
          ids->names @(subscribe [(keyword (str type-name "-ids->names"))])]
      [(cl bs/Suggest) {:items (if (not-empty ids->names) (keys ids->names) [])
                        :selected-item value
                        :on-item-select on-item-select
                        :no-results no-results
                        :input-value-renderer (input-value-renderer ids->names)
                        :item-renderer (item-renderer ids->names)
                        :item-predicate (item-predicate ids->names)}])))

(defcomponent numeric-input
  "A numeric input with relatively narrow width, intended to fit integers less than 1000 compactly.
   Any provided `opts` will be passed through to the underlying NumericInput component."
  [{:keys [value on-change min]} _]
  [numeric-input-raw {:value value
                      :async-control true
                      :on-value-change on-change
                      :style {:width "2.5rem"}
                      :min min}])

(defcomponent suggest-numeric
  "A control group used to input a numeric quantity combined with a reference to an object like an item or a machine.
   Used, for example, to select the number of each type of item that's required as input for a recipe."
  [{:keys [type on-change] [x n :as value] :value} _]
  (with-let [change-fst (fn [[_ b] cb x] (cb [x b]))
             change-snd (fn [[a _] cb x] (cb [a x]))
             change-fst-factory (callback-factory-factory change-fst)
             change-snd-factory (callback-factory-factory change-snd)]
    [control-group
     [numeric-input {:value n :on-change (change-snd-factory value on-change) :min 0}]
     [suggest {:type type 
               :value x
               :on-item-select (change-fst-factory value on-change)}]]))

(defcomponent suggest-numeric-deletable
  "Like suggest-numeric, but includes a delete button on the right of the control. The `on-delete` callback is called when
   the delete button is pressed."
  [{:keys [type value on-change on-delete]} _]
  [control-group
   [suggest-numeric {:type type :value value :on-change on-change}]
   [button {:icon :delete :intent :danger :on-click on-delete}]])

(defcomponent suggest-numeric-addable
  "Like suggest-numeric, but includes an add (+) button on the right of the control. The `on-add` function is called when
   the add button is pressed."
  [{:keys [type value on-change on-add]} _]
  (with-let [on-add-cb (fn [cb v] (cb v))
             on-add-factory (callback-factory-factory on-add-cb)]
    [control-group
     [suggest-numeric {:type type :value value :on-change on-change}]
     [button {:icon :plus :intent :success :on-click (on-add-factory on-add value)}]]))

(defcomponent quantity-set-input-line
  [{:keys [type qs on-change] [id _ :as value] :value} _]
  (with-let [update-cb (fn [qs cb oid [id num]] (-> qs (dissoc oid) (assoc id num) (cb)))
             update-cb-factory (callback-factory-factory update-cb)
             delete-cb (fn [qs cb id] (-> qs (dissoc id) (cb)))
             delete-cb-factory (callback-factory-factory delete-cb)]
    [suggest-numeric-deletable {:type type
                                :value value
                                :on-change (update-cb-factory qs on-change id)
                                :on-delete (delete-cb-factory qs on-change id)}]))

(defcomponent quantity-set-input-add-line
  [{:keys [type on-add]} _]
  (with-let [new-quantity (reagent/atom [])
             update-new-quantity #(reset! new-quantity %)
             add-cb (fn [cb v]
                      (cb v)
                      (update-new-quantity []))
             add-cb-factory (callback-factory-factory add-cb)]
    [suggest-numeric-addable {:type type
                              :value @new-quantity
                              :on-change update-new-quantity
                              :on-add (add-cb-factory on-add)}]))

(defcomponent quantity-set-input
  "A complex control for configuring the contents of a qmap. Each entry in the qmap is rendered as a line that's editable
   and deletable. There's also a line for adding new entries to the qmap. The `on-change` function is called with the full updated
   qmap whenever any updates are performed."
  [{:keys [type value on-change]} _]
  (with-let [add-cb (fn [qs cb [k v]] (cb (assoc qs k v)))
             add-cb-factory (callback-factory-factory add-cb)]
    (conj
     (into [:<>] (for [kvp value] [quantity-set-input-line {:type type
                                                            :value kvp
                                                            :on-change on-change
                                                            :qs value}]))
     [quantity-set-input-add-line {:type type
                                   :on-add (add-cb-factory value on-change)}])))

(defcomponent list-input-line
  [{:keys [type xs on-change x ix]} _]
  (with-let [update-cb (fn [xs cb ix x] (cb (assoc xs ix x)))
             update-cb-factory (callback-factory-factory update-cb)
             delete-cb (fn [xs cb ix] (cb (delete-index xs ix)))
             delete-cb-factory (callback-factory-factory delete-cb)
             move-up-cb (fn [xs cb ix] (cb (move-index-behind xs ix)))
             move-up-cb-factory (callback-factory-factory move-up-cb)
             move-down-cb (fn [xs cb ix] (cb (move-index-ahead xs ix)))
             move-down-cb-factory (callback-factory-factory move-down-cb)]
    [control-group
     [button {:icon :chevron-up :on-click (move-up-cb-factory xs on-change ix) :disabled (= ix 0)}]
     [button {:icon :chevron-down :on-click (move-down-cb-factory xs on-change ix) :disabled (= ix (dec (count xs)))}]
     [suggest {:type type :value x :on-item-select (update-cb-factory xs on-change ix)}]
     [button {:icon :delete :on-click (delete-cb-factory xs on-change ix)}]]))

(defcomponent list-input-add-line
  [{:keys [type on-add]} _]
  (with-let [new-val (reagent/atom nil)
             update-new-val #(reset! new-val %)
             add-cb (fn [cb v]
                      (cb v)
                      (update-new-val nil))
             add-cb-factory (callback-factory-factory add-cb)]
    [control-group
     [suggest {:type type :value @new-val :on-item-select update-new-val}]
     [button {:icon :plus :on-click (add-cb-factory on-add @new-val)}]]))

(defcomponent list-input
  "A complex control for editing a list of objects (items, machines, etc.) Each entry in the list is rendered as a line that's editable
   and deletable. There's also a line for adding new entries to the list. Lines can be moved up and down. Duplicates are allowed.
   The `on-change` function is called with the full updated list whenever any updates are performed."
  [{:keys [type value on-change]} _]
  (with-let [add-cb (fn [value cb x] (cb (conj value x)))
             add-cb-factory (callback-factory-factory add-cb)]
    (conj
     (into [:<>] (for [[x ix] (ipairs value)]
                   [list-input-line {:type type :xs value :on-change on-change :x x :ix ix}]))
     [list-input-add-line {:type type :on-add (add-cb-factory value on-change)}])))

(defcomponent set-input-add-line
  [{:keys [type on-add]} _]
  (with-let [new-val (reagent/atom nil)
             update-new-val #(reset! new-val %)
             add-cb (fn [cb v]
                      (cb v)
                      (update-new-val nil))
             add-cb-factory (callback-factory-factory add-cb)]
    [control-group
     [suggest {:type type :value @new-val :on-item-select update-new-val}]
     [button {:icon :plus :on-click (add-cb-factory on-add @new-val)}]]))

(defcomponent set-input-line
  [{:keys [type xs on-change x]} _]
  (with-let [update-cb (fn [xs cb ox x] (cb (conj (disj xs ox) x)))
             update-cb-factory (callback-factory-factory update-cb)
             delete-cb (fn [xs cb x] (cb (disj xs x)))
             delete-cb-factory (callback-factory-factory delete-cb)]
    [control-group
     [suggest {:type type :value x :on-item-select (update-cb-factory xs on-change x)}]
     [button {:icon :delete :on-click (delete-cb-factory xs on-change x)}]]))

(defcomponent set-input
  "A complex control for editing a set of objects (items, machines, etc.) Each entry in the set is rendered as a line that's editable
   and deletable. There's also a line for adding new entries to the set. Duplicates are not allowed.
   The `on-change` function is called with the full updated list whenever any updates are performed."
  [{:keys [type value on-change]} _]
  (with-let [add-cb (fn [value cb x] (when-not (contains? value x) (cb (conj value x))))
             add-cb-factory (callback-factory-factory add-cb)]
    (conj
     (into [:<>] (for [[x ix] (ipairs value)]
                   [set-input-line {:type type :xs value :on-change on-change :x x :ix ix}]))
     [set-input-add-line {:type type :on-add (add-cb-factory value on-change)}])))

(defcomponent input
  "A wrapper for Blueprint's InputGroup component. All props are passed through to the underlying InputGroup.
   The only exception is the on-change prop, which is wrapped such that the value is already extracted from
   the InputGroup's React.SyntheticEvent before being passed to the `on-change` function."
  [{:keys [on-change] :as props} _]
  (with-let [change-cb (fn [cb ev] (-> ev (.-target) (.-value) (cb)))
             change-cb-factory (callback-factory-factory change-cb)]
    (let [default-props {:async-control true}
          override-props {:on-change (change-cb-factory on-change)}
          props (merge default-props props override-props)]
      [(cl b/InputGroup) props])))

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

(defcomponent grid
  "Wrapper around ag-grid's Grid component. Sets a variety of default props for DRY-ness. All defaults can be
   overriden with `props`. Note that usually you want to specify at least the :on-row-value-changed prop. The
   `children` can contain column definitions, but it's usually simpler to use the :column-defs option in `props`."
  [props children]
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
     (into [grid-raw grid-props] children)]))

(defn nav-link
  "A minimally styled button that, when clicked, will change the currently selected page."
  [page icon text]
  (with-let [on-click #(dispatch [:select-page %])
             on-click-factory (callback-factory-factory on-click)]
    (let [selected-page @(subscribe [:selected-page])]
      [button {:class :bp3-minimal
               :on-click (on-click-factory page)
               :icon icon
               :text text
               :disabled (= selected-page page)}])))

(defn alerting-button
  "A button that will pop up a confirmation dialog when clicked. Allows specifying two sets of props:
   `button-props` are applied to the button, and `alert-props` to the popup alert.
   If the user confirms the alert, the :on-confirm key in `alert-props` will be called."
  [button-props alert-props & children]
  (with-let [open? (reagent/atom false)
             open #(reset! open? true)
             close #(reset! open? false)]
    [:<>
     [button (assoc button-props :on-click open)]
     (into [alert (merge {:is-open @open?
                          :can-escape-key-cancel true
                          :can-outside-click-cancel true
                          :confirm-button-text "Confirm"
                          :cancel-button-text "Cancel"
                          :on-close close} alert-props)]
           children)]))

(defn undo-redo
  "A control group containing undo/redo buttons. The buttons are wired to the global undo/redo stack."
  []
  (with-let [undo #(dispatch [:undo])
             redo #(dispatch [:redo])]
    [control-group
     [button {:class :bp3-minimal :disabled (not @(subscribe [:undos?])) :on-click undo :icon :undo :title "Undo"}]
     [button {:class :bp3-minimal :disabled (not @(subscribe [:redos?])) :on-click redo :icon :redo :title "Redo"}]]))
