(ns factor.view.recipe
  (:require [factor.components :as c]
            [re-frame.core :refer [dispatch subscribe]]
            [factor.world :as w]))

(defn get-selected-ids [ev]
  (-> ev
      (:api)
      (.getSelectedRows)
      (js->clj)
      (->> (map #(get % "id")))
      (vec)))

(defn selected-text [num] (str "(" num " items selected)"))

(defn recipe-grid []
  (let [all-recipes @(subscribe [:recipe-seq])
        update-recipe #(dispatch [:update-recipe (:data %)])
        update-selection #(dispatch [:ui [:recipe-page :selected] %])]
    [c/grid {:row-data all-recipes
             :on-grid-ready #(update-selection [])
             :on-row-value-changed update-recipe
             :on-selection-changed #(-> % (get-selected-ids) (update-selection))
             :column-defs [{:checkboxSelection true :sortable false}
                           {:field :id}
                           {:field :name :editable true}
                           {:field :created-at
                            :headerName "Created"}]}]))

(defn navbar []
  (let [selected-recipes   @(subscribe [:ui [:recipe-page :selected]])
        num-selected        (count selected-recipes)
        create-recipe      #(dispatch [:update-recipe (w/new-recipe)])
        delete-recipes   #(do
                            (dispatch [:delete-recipes selected-recipes])
                            (dispatch [:ui [:recipe-page :selected] []]))]
    [c/navbar
     [c/navbar-group-left
      [c/navbar-heading "Recipe List"]
      [c/button {:class :bp3-minimal :on-click create-recipe :icon :plus :title "Add recipe"}]
      [c/navbar-divider]
      (when (< 0 num-selected)
        [:<>
         [:div (str "(" num-selected " recipes selected)")]
         [c/button {:class :bp3-minimal
                    :on-click delete-recipes
                    :icon :minus :text "Delete"
                    :disabled (= num-selected 0)}]])]]))

(defn input-editor [thing on-change]
  (let [item-rate-unit @(subscribe [:unit :item-rate])]
    [c/form-group {:label (str "Inputs (" item-rate-unit ")")}
     [c/quantity-set-input :item (:input thing) #(-> thing (assoc :input %) (on-change))]]))

(defn output-editor [thing on-change]
  (let [item-rate-unit @(subscribe [:unit :item-rate])]
    [c/form-group {:label (str "Outputs (" item-rate-unit ")")}
     [c/quantity-set-input :item (:output thing) #(-> thing (assoc :output %) (on-change))]]))

(defn catalysts-editor [thing on-change]
  (let [item-rate-unit @(subscribe [:unit :item-rate])]
    [c/form-group {:label (str "One-time catalysts (" item-rate-unit ")")}
     [c/quantity-set-input :item (:catalysts thing) #(-> thing (assoc :catalysts %) (on-change))]]))

(defn machine-list-editor [thing on-change]
  [c/form-group {:label "Machines"}
   [c/list-input :machine (:machines thing) #(-> thing (assoc :machines (set %)) (on-change))]])

(defn recipe-editor [id]
  (let [recipe @(subscribe [:recipe id])
        update-recipe #(dispatch [:update-recipe %])]
    [:div.card-stack
     [c/card-lg [input-editor recipe update-recipe]]
     [c/card-lg [output-editor recipe update-recipe]]
     [c/card-lg [catalysts-editor recipe update-recipe]]
     [c/card-lg [machine-list-editor recipe update-recipe]]]))

(defn recipe-page-editor []
  (let [selected-recipes   @(subscribe [:ui [:recipe-page :selected]])]
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
