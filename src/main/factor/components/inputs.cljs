(ns factor.components.inputs
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as reagent :refer [with-let as-element]]
            [factor.components.macros :refer-macros [defcomponent]]
            [clojure.string :as string]
            [factor.util :refer [cl ipairs delete-index move-index-ahead move-index-behind callback-factory-factory]]
            [factor.components.wrappers :as w]
            ["@blueprintjs/core" :as b]
            ["@blueprintjs/select" :as bs]))


(defcomponent omnibar
  "General omnibar component. Manages open/closed and query state using the app-db.
   
   Note: This component is used for implementing new omnibar modes. To actually render an omnibar in the app,
   use the global-omnibar component somewhere in the React tree.
   
   Supported props:
   
   :mode -- the omnibar will be open when this mode is selected in the app-db omnibar state. e.g. :command-palette
   :items -- list of items to choose from
   :on-item-select -- function to be called when the user selects an item
   :item-predicate -- function called with 4 params: (query, item, index, exact?).
                      called for each item, return true if the item matches the query.
   :item-renderer -- function called with 2 params: (item, opts) where opts has keys:
                       `:on-click`, `:is-active`, `:is-disabled`, `:is-match`
                     Expected to return a Hiccup form that will automatically be passed to as-element.
   :item-getter -- (optional) Function called with the item that returns a different representation
                   of the item for use in other item-related callbacks. For example, if items are the IDs of
                   factories, this function could look up the actual factory for the item, to avoid duplicate logic
                   in the other callbacks.
   
   Visual props (all optional):
   
   :placeholder -- the text that's displayed before the user types anything
   :left-icon -- icon to display at left of prompt"
  [{:keys [mode items on-item-select item-predicate item-renderer item-getter placeholder left-icon]
    :or {item-getter identity}} _]
  (with-let [close-omnibar #(dispatch [:close-omnibar])
             update-query #(dispatch-sync [:update-omnibar-query %])
             on-item-select-impl (fn [on-item-select item-getter item]
                                   (close-omnibar)
                                   (on-item-select (item-getter item)))
             item-renderer-impl (fn [item-renderer item-getter item opts]
                                  (-> item
                                      (item-getter)
                                      (item-renderer {:on-click (.-handleClick opts)
                                                      :is-active (.-active (.-modifiers opts))
                                                      :is-disabled (.-disabled (.-modifiers opts))
                                                      :is-match (.-matchesPredicate (.-modifiers opts))})
                                      (as-element)))
             item-predicate-impl (fn [item-predicate item-getter q item ix exact?]
                                   (item-predicate q (item-getter item) ix exact?))]
    (let [{:keys [query] current-mode :mode} @(subscribe [:omnibar-state])]
      [w/omnibar {:is-open true
                  :on-close close-omnibar
                  :items items
                  :query query
                  :on-query-change update-query
                  :on-item-select (reagent/partial on-item-select-impl on-item-select item-getter)
                  :item-predicate (reagent/partial item-predicate-impl item-predicate item-getter)
                  :item-renderer (reagent/partial item-renderer-impl item-renderer item-getter)
                  :input-props {:placeholder placeholder
                                :left-icon left-icon}}])))

(defcomponent input-omnibar
  "Omnibar component specifically for cases where there are no items, the omnibar is being used as a glorified input.
   In this case there is no on-item-select, but an on-submit that gets called when the user presses Enter.
   
   Note: This component is used for implementing new omnibar modes. To actually render an omnibar in the app,
   use the global-omnibar component somewhere in the React tree.
   
   Supported props:
   
   :mode -- the omnibar will be open when this mode is selected in the app-db omnibar state. e.g. :command-palette
   :on-submit -- function to be called when the user presses Enter
   
   Visual props (all optional):
   
   :placeholder -- the text that's displayed before the user types anything
   :left-icon -- icon to display at left of prompt"
  [{:keys [mode on-submit placeholder left-icon]} _]
  (with-let [close-omnibar #(dispatch [:close-omnibar])
             update-query #(dispatch-sync [:update-omnibar-query %])
             on-item-select (fn [on-submit q]
                              (close-omnibar)
                              (on-submit q))]
    (let [{:keys [query] current-mode :mode} @(subscribe [:omnibar-state])]
      [w/omnibar {:is-open true
                  :on-close close-omnibar
                  :items []
                  :query query
                  :on-query-change update-query
                  :on-item-select (reagent/partial on-item-select on-submit)
                  :create-new-item-from-query identity
                  :input-props {:placeholder placeholder
                                :left-icon left-icon}}])))


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
                                [w/menu-item {:key id
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
             no-results (reagent/as-element [w/menu-item {:disabled true :text "No matching results."}])]
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
  [w/numeric-input {:value value
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
    [w/control-group
     [numeric-input {:value n :on-change (change-snd-factory value on-change) :min 0}]
     [suggest {:type type
               :value x
               :on-item-select (change-fst-factory value on-change)}]]))

(defcomponent suggest-numeric-deletable
  "Like suggest-numeric, but includes a delete button on the right of the control. The `on-delete` callback is called when
   the delete button is pressed."
  [{:keys [type value on-change on-delete]} _]
  [w/control-group
   [suggest-numeric {:type type :value value :on-change on-change}]
   [w/button {:icon :delete :intent :danger :on-click on-delete}]])

(defcomponent suggest-numeric-addable
  "Like suggest-numeric, but includes an add (+) button on the right of the control. The `on-add` function is called when
   the add button is pressed."
  [{:keys [type value on-change on-add]} _]
  (with-let [on-add-cb (fn [cb v] (cb v))
             on-add-factory (callback-factory-factory on-add-cb)]
    [w/control-group
     [suggest-numeric {:type type :value value :on-change on-change}]
     [w/button {:icon :plus :intent :success :on-click (on-add-factory on-add value)}]]))

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
    [w/control-group
     [w/button {:icon :chevron-up :on-click (move-up-cb-factory xs on-change ix) :disabled (= ix 0)}]
     [w/button {:icon :chevron-down :on-click (move-down-cb-factory xs on-change ix) :disabled (= ix (dec (count xs)))}]
     [suggest {:type type :value x :on-item-select (update-cb-factory xs on-change ix)}]
     [w/button {:icon :delete :on-click (delete-cb-factory xs on-change ix)}]]))

(defcomponent list-input-add-line
  [{:keys [type on-add]} _]
  (with-let [new-val (reagent/atom nil)
             update-new-val #(reset! new-val %)
             add-cb (fn [cb v]
                      (cb v)
                      (update-new-val nil))
             add-cb-factory (callback-factory-factory add-cb)]
    [w/control-group
     [suggest {:type type :value @new-val :on-item-select update-new-val}]
     [w/button {:icon :plus :on-click (add-cb-factory on-add @new-val)}]]))

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
    [w/control-group
     [suggest {:type type :value @new-val :on-item-select update-new-val}]
     [w/button {:icon :plus :on-click (add-cb-factory on-add @new-val)}]]))

(defcomponent set-input-line
  [{:keys [type xs on-change x]} _]
  (with-let [update-cb (fn [xs cb ox x] (cb (conj (disj xs ox) x)))
             update-cb-factory (callback-factory-factory update-cb)
             delete-cb (fn [xs cb x] (cb (disj xs x)))
             delete-cb-factory (callback-factory-factory delete-cb)]
    [w/control-group
     [suggest {:type type :value x :on-item-select (update-cb-factory xs on-change x)}]
     [w/button {:icon :delete :on-click (delete-cb-factory xs on-change x)}]]))

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

(defn nav-link
  "A minimally styled button that, when clicked, will change the currently selected page."
  [page icon text]
  (with-let [on-click #(dispatch [:update-route [%]])
             on-click-factory (callback-factory-factory on-click)]
    (let [route @(subscribe [:page-route])]
      [w/button {:class :bp3-minimal
                 :on-click (on-click-factory page)
                 :icon icon
                 :text text
                 :disabled (= route [page])}])))

(defn alerting-button
  "A button that will pop up a confirmation dialog when clicked. Allows specifying two sets of props:
   `button-props` are applied to the button, and `alert-props` to the popup alert.
   If the user confirms the alert, the :on-confirm key in `alert-props` will be called."
  [button-props alert-props & children]
  (with-let [open? (reagent/atom false)
             open #(reset! open? true)
             close #(reset! open? false)]
    [:<>
     [w/button (assoc button-props :on-click open)]
     (into [w/alert (merge {:is-open @open?
                            :can-escape-key-cancel true
                            :can-outside-click-cancel true
                            :confirm-button-text "Confirm"
                            :cancel-button-text "Cancel"
                            :on-close close} alert-props)]
           children)]))
