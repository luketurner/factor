(ns factor.view.recipe
  (:require [factor.components :as c]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :refer [with-let]]
            [factor.util :refer [callback-factory-factory]]
            [clojure.string :as string]))

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
             update-selection #(dispatch [:ui [:recipe-page :selected] %])
             on-grid-ready #(update-selection [])
             on-selection-changed #(-> % (get-selected-ids) (update-selection))]
    (let [all-recipes @(subscribe [:recipe-seq])
          item-rate-unit @(subscribe [:unit :item-rate])
          [_ rate-denominator] (string/split item-rate-unit #"/" 2)]
      [c/grid {:row-data all-recipes
               :on-grid-ready on-grid-ready
               :on-row-value-changed update-recipe
               :on-selection-changed on-selection-changed
               :column-defs [{:checkboxSelection true :sortable false}
                             {:field :id}
                             {:field :name :editable true}
                             {:field :duration :editable true :type :numericColumn
                              :headerName (str "Duration (" rate-denominator ")")
                              :valueParser c/grid-value-parser-for-floats}
                             {:field :created-at
                              :headerName "Created"}]}])))

(defn navbar []
  (with-let [create-recipe  #(dispatch [:create-recipe])
             delete-recipes #(do
                               (dispatch [:delete-recipes %])
                               (dispatch [:ui [:recipe-page :selected] []]))
             delete-recipes-factory (callback-factory-factory delete-recipes)]
    (let [selected-recipes   @(subscribe [:ui [:recipe-page :selected]])
          num-selected        (count selected-recipes)]
      [c/navbar
       [c/navbar-group-left
        [c/navbar-heading "Recipe List"]
        [c/undo-redo]
        [c/button {:class :bp3-minimal :on-click create-recipe :icon :plus :title "Add recipe"}]
        [c/navbar-divider]
        (when (< 0 num-selected)
          [:<>
           [:div (str "(" num-selected " recipes selected)")]
           [c/button {:class :bp3-minimal
                      :on-click (delete-recipes-factory selected-recipes)
                      :icon :minus :text "Delete"
                      :disabled (= num-selected 0)}]])]])))

(defn input-editor
  [id]
  (with-let [update-cb (fn [id v] (dispatch [:update-recipe-input id v]))
             update-cb-factory (callback-factory-factory update-cb)]
    [c/quantity-set-input :item @(subscribe [:recipe-input id]) (update-cb-factory id)]))

(defn output-editor
  [id]
  (with-let [update-cb (fn [id v] (dispatch [:update-recipe-output id v]))
             update-cb-factory (callback-factory-factory update-cb)]
    [c/quantity-set-input :item @(subscribe [:recipe-output id]) (update-cb-factory id)]))

(defn catalysts-editor [id]
  (with-let [update-cb (fn [id v] (dispatch [:update-recipe-catalysts id v]))
             update-cb-factory (callback-factory-factory update-cb)]
    [c/quantity-set-input :item @(subscribe [:recipe-catalysts id]) (update-cb-factory id)]))

(defn machine-list-editor
  [id]
  (with-let [update-cb (fn [id v] (dispatch [:update-recipe-machines id v]))
             update-cb-factory (callback-factory-factory update-cb)]
    [c/list-input :machine @(subscribe [:recipe-machines id]) (update-cb-factory id)]))

(defn duration-editor
  [id]
  (with-let [update-cb (fn [id v] (dispatch [:update-recipe-duration id v]))
             update-cb-factory (callback-factory-factory update-cb)]
    [c/numeric-input @(subscribe [:recipe-duration id]) (update-cb-factory id)]))

(defn duration-editor-label
  [c]
  [c/form-group {:label (str "Duration (" (last (string/split @(subscribe [:unit :item-rate]) #"/" 2)) ")")}
   c])

(defn recipe-editor [id]
  [:div.card-stack
   [c/card-lg [c/form-group {:label "Inputs (per crafting operation)"} [input-editor id]]]
   [c/card-lg [c/form-group {:label "Outputs (per crafting operation)"} [output-editor id]]]
   [c/card-lg [c/form-group {:label "Catalysts (per machine)"} [catalysts-editor id]]]
   [c/card-lg [c/form-group {:label "Machines"} [machine-list-editor id]]]
   [c/card-lg [duration-editor-label
               [duration-editor id]]]])

(defn recipe-page-editor []
  (let [selected-recipes @(subscribe [:ui [:recipe-page :selected]])]
    [:div.data-table-editor
     (case (count selected-recipes)
       0 [c/non-ideal-state {:icon :data-lineage
                             :title "No recipe selected!"
                             :description "Select a single recipe to edit it."}]
       1 [recipe-editor (first selected-recipes)]
       [c/non-ideal-state {:icon :data-lineage
                           :title "Multiple recipes selected!"
                           :description "Select a single recipe to edit it."}])]))

(defn page []
  [:div.vertical-split
   [recipe-grid]
   [recipe-page-editor]])
