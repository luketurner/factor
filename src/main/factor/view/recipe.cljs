(ns factor.view.recipe
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :refer [with-let]]
            [factor.util :refer [callback-factory-factory]]
            [clojure.string :as string]
            [factor.components.wrappers :refer [non-ideal-state card-lg form-group numeric-input navbar-divider]]
            [factor.components.inputs :refer [list-input quantity-set-input]]
            [factor.components.cmd :refer [cmd-btn]]
            [factor.components.grid :refer [grid-value-parser-for-floats grid]]))

(defn get-selected-ids [ev]
  (-> ev
      (:api)
      (.getSelectedRows)
      (js->clj)
      (->> (map #(get % "id")))
      (vec)))

(defn selected-text [num] (str "(" num " items selected)"))

(defn recipe-grid []
  (with-let [update-recipe #(dispatch [:update-recipe (:data %)])
             update-selection #(dispatch [:select-objects %])
             on-grid-ready #(update-selection [])
             on-selection-changed #(-> % (get-selected-ids) (update-selection))]
    (let [all-recipes @(subscribe [:recipe-seq])
          item-rate-unit @(subscribe [:unit :item-rate])
          [_ rate-denominator] (string/split item-rate-unit #"/" 2)]
      [grid {:row-data all-recipes
             :on-grid-ready on-grid-ready
             :on-row-value-changed update-recipe
             :on-selection-changed on-selection-changed
             :column-defs [{:checkboxSelection true :sortable false}
                           {:field :id}
                           {:field :name :editable true}
                           {:field :duration :editable true :type :numericColumn
                            :headerName (str "Duration (" rate-denominator ")")
                            :valueParser grid-value-parser-for-floats}
                           {:field :created-at
                            :headerName "Created"}]}])))

(defn tools []
  (let [selected-recipes   @(subscribe [:selected-objects])
        num-selected        (count selected-recipes)]
    [:<>
     [cmd-btn {:cmd :new-recipe :intent :success :minimal true}]
     (when (< 0 num-selected)
       [:<>
        [navbar-divider]
        [:div (str "(" num-selected " recipes selected)")]
        [cmd-btn {:cmd :delete-selected-recipes :intent :danger :minimal true}]])]))

(defn input-editor
  [id]
  (with-let [update-cb (fn [id v] (dispatch [:update-recipe-input id v]))
             update-cb-factory (callback-factory-factory update-cb)]
    [quantity-set-input {:type :item
                         :value @(subscribe [:recipe-input id])
                         :on-change (update-cb-factory id)}]))

(defn output-editor
  [id]
  (with-let [update-cb (fn [id v] (dispatch [:update-recipe-output id v]))
             update-cb-factory (callback-factory-factory update-cb)]
    [quantity-set-input {:type :item
                         :value @(subscribe [:recipe-output id])
                         :on-change (update-cb-factory id)}]))

(defn catalysts-editor [id]
  (with-let [update-cb (fn [id v] (dispatch [:update-recipe-catalysts id v]))
             update-cb-factory (callback-factory-factory update-cb)]
    [quantity-set-input {:type :item
                         :value @(subscribe [:recipe-catalysts id])
                         :on-change (update-cb-factory id)}]))

(defn machine-list-editor
  [id]
  (with-let [update-cb (fn [id v] (dispatch [:update-recipe-machines id v]))
             update-cb-factory (callback-factory-factory update-cb)]
    [list-input {:type :machine
                 :value @(subscribe [:recipe-machines id])
                 :on-change (update-cb-factory id)}]))

(defn duration-editor
  [id]
  (with-let [update-cb (fn [id v] (dispatch [:update-recipe-duration id v]))
             update-cb-factory (callback-factory-factory update-cb)]
    [numeric-input {:value @(subscribe [:recipe-duration id])
                    :on-change (update-cb-factory id)}]))

(defn duration-editor-label
  [c]
  [form-group {:label (str "Duration (" (last (string/split @(subscribe [:unit :item-rate]) #"/" 2)) ")")}
   c])

(defn recipe-editor [id]
  [:div.card-stack
   [card-lg [form-group {:label "Inputs (per crafting operation)"} [input-editor id]]]
   [card-lg [form-group {:label "Outputs (per crafting operation)"} [output-editor id]]]
   [card-lg [form-group {:label "Catalysts (per machine)"} [catalysts-editor id]]]
   [card-lg [form-group {:label "Machines"} [machine-list-editor id]]]
   [card-lg [duration-editor-label
               [duration-editor id]]]])

(defn recipe-page-editor []
  (let [selected-recipes @(subscribe [:selected-objects])]
    [:div.data-table-editor
     (case (count selected-recipes)
       0 [non-ideal-state {:icon :data-lineage
                           :title "No recipe selected!"
                           :description "Select a single recipe to edit it."}]
       1 [recipe-editor (first selected-recipes)]
       [non-ideal-state {:icon :data-lineage
                           :title "Multiple recipes selected!"
                           :description "Select a single recipe to edit it."}])]))

(defn page []
  [:div.vertical-split
   [recipe-grid]
   [recipe-page-editor]])
