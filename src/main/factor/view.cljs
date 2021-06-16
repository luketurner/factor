(ns factor.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :refer [as-element]]
            [factor.styles :as styles]
            [factor.components :as c]))

(defn name-editor [thing on-change]
  [c/form-group {:label "Name"}
   [c/input {:value (:name thing)
             :on-change #(->> % (assoc thing :name) (on-change))}]])

(defn input-editor [thing on-change]
  [c/form-group {:label "Inputs"}
   [c/quantity-set-input :item (:input thing) #(-> thing (assoc :input %) (on-change))]])

(defn recipe-editor [id]
  (let [recipe @(subscribe [:recipe id])
        update-recipe #(dispatch [:update-recipe id %])]
    [:form
     [c/card-lg
      [name-editor recipe update-recipe]]
     [c/card-lg [input-editor recipe update-recipe]]]))

(defn nav-link [page icon text]
  (let [selected-page @(subscribe [:selected-page])]
    [c/button {:class :bp3-minimal
               :on-click #(dispatch [:select-page page])
               :icon icon
               :text text
               :disabled (= selected-page page)}]))


(defn factory-page-bar []
  (let [{:keys [selected]} @(subscribe [:ui [:factory-page]])
        select-factory     #(dispatch [:ui [:factory-page :selected] %])
        create-factory     #(dispatch [:create-factory])
        delete-factory     #(dispatch [:delete-factory selected])]
    [c/navbar
     [c/navbar-group-left
      [c/navbar-heading "Factories"]
      [c/navbar-divider]
      [c/suggest :factory selected select-factory]
      [c/button {:class :bp3-minimal :on-click create-factory :icon :plus :text "Add factory"}]
      [c/navbar-divider]
      [c/button {:on-click delete-factory :intent :danger :text "Delete factory"}]]]))


(defn factory-page []
  (if-let [factory-id @(subscribe [:ui [:factory-page :selected]])]
    (let [factory @(subscribe [:factory factory-id])]
      [c/card-lg [:p "open factory: " (:name factory) " (" factory-id ")"]])
    [c/non-ideal-state {:title "No factories"
                        :description "Create a factory to get started."
                        :action (as-element [c/button {:text "Create Factory" :intent :success}])}]))

(defn home-page []
  [:div
   [:h2 "overview"]
   [:p "Welcome! Factor is a tool that helps with planning factories in video games (e.g. Factorio.)"]
   [:p "Create a new factory using the " [:strong "factories"] " option in the sidebar, and specify what items the factory should output."]
   [:p "Assuming you've also entered all your "
    [:strong "items"] ", "
    [:strong "recipes"] ", and "
    [:strong "machines"] ", Factor will calculate what inputs and what number of machines your factory will require."]
   [:p "If you have a lot of items/machines/recipes/etc. to input, these keyboard shortcuts might come in handy:"]
   [:table
    [:tbody
     [:tr [:td "ENTER"] [:td "Add new entry"]]
     [:tr [:td "ALT+BKSP"] [:td "Delete current entry"]]]]
   [:p "This project is a work-in-progress and not all features work yet. Be warned!"]])

(defn get-selected-ids [ev]
  (-> ev
      (:api)
      (.getSelectedRows)
      (js->clj)
      (->> (map #(get % "id")))
      (vec)))

(defn selected-text [num] (str "(" num " items selected)"))

(defn item-grid [items]
  (let [update-item #(dispatch [:update-item (get-in % [:data :id]) {:name (get-in % [:data :name])}])
        update-selection #(dispatch [:update-selection [:item %]])]
    [c/grid {:row-data items
           :on-grid-ready #(update-selection [])
           :on-row-value-changed update-item
           :on-selection-changed #(-> % (get-selected-ids) (update-selection))
           :column-defs [{:checkboxSelection true}
                         {:field :id}
                         {:field :name :editable true}]}]))

(defn item-page-bar []
  (let [create-item #(dispatch [:create-item])
        selected-items @(subscribe [:current-selection :item])
        delete-items #(dispatch [:delete-items selected-items])
        num-selected (count selected-items)]
    [c/navbar
     [c/navbar-group-left
      [c/navbar-heading "Item List"]
      [c/button {:class :bp3-minimal :on-click create-item :icon :plus :title "Add item"}]
      [c/navbar-divider]
      (when (< 0 num-selected)
        [:<>
         [:div (selected-text num-selected)]
         [c/button {:class :bp3-minimal
                  :on-click delete-items
                  :icon :minus :text "Delete"
                  :disabled (= num-selected 0)}]])]]))

(defn item-page []
  (let [all-items @(subscribe [:item-seq])]
     [item-grid all-items]))

(defn machine-grid [machines]
  (let [update-machine #(dispatch [:update-machine (get-in % [:data :id]) (dissoc (:data %) :id)])
        update-selection #(dispatch [:ui [:recipe-page :selected] %])]
    [c/grid {:row-data machines
             :on-grid-ready #(update-selection [])
             :on-row-value-changed update-machine
             :on-selection-changed #(-> % (get-selected-ids) (update-selection))
             :column-defs [{:checkboxSelection true}
                           {:field :id}
                           {:field :name :editable true}
                           {:field :power :editable true}
                           {:field :speed :editable true}]}]))

(defn machine-page-bar []
  (let [create-machine #(dispatch [:create-machine])
        selected-machines @(subscribe [:current-selection :machine])
        delete-machines #(dispatch [:delete-machines selected-machines])
        num-selected (count selected-machines)]
    [c/navbar
     [c/navbar-group-left
      [c/navbar-heading "Machine List"]
      [c/button {:class :bp3-minimal :on-click create-machine :icon :plus :title "Add machine"}]
      [c/navbar-divider]
      (when (< 0 num-selected)
        [:<>
         [:div (selected-text num-selected)]
         [c/button {:class :bp3-minimal
                  :on-click delete-machines
                  :icon :minus :text "Delete"
                  :disabled (= num-selected 0)}]])]]))

(defn machine-page []
  (let [all-machines @(subscribe [:machine-seq])]
    [machine-grid all-machines]))

(defn recipe-grid []
  (let [all-recipes @(subscribe [:recipe-seq])
        update-recipe #(dispatch [:update-recipe (get-in % [:data :id]) (dissoc (:data %) :id)])
        update-selection #(dispatch [:ui [:recipe-page :selected] %])]
    [c/grid {:row-data all-recipes
             :on-grid-ready #(update-selection [])
             :on-row-value-changed update-recipe
             :on-selection-changed #(-> % (get-selected-ids) (update-selection))
             :column-defs [{:checkboxSelection true}
                           {:field :id}
                           {:field :name}]}]))

(defn recipe-page-bar []
  (let [{:keys [selected]} @(subscribe [:ui [:recipe-page]])
        num-selected        (count selected)
        create-recipe      #(dispatch [:create-recipe])
        delete-recipes     #(dispatch [:delete-recipes selected])]
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

(defn recipe-page-editor []
  (let [{:keys [selected]} @(subscribe [:ui [:recipe-page]])]
    (println "selected" selected)
    [:div.data-table-editor
     (case (count selected)
       0 [:div "No recipes selected."]
       1 [recipe-editor (first selected)]
       [:div "Select a single recipe to edit."])]))

(defn recipe-page []
  [:div.vertical-split
   [recipe-grid]
   [recipe-page-editor]])

(defn main-content []
  (let [[x] @(subscribe [:selected-page])]
    [:main
     (case x
      :home [home-page]
      :factories [factory-page]
      :items [item-page]
      :machines [machine-page]
      :recipes [recipe-page])]))

(defn primary-navbar []
  [c/navbar
   [c/navbar-group-left
    [c/navbar-heading [:strong "factor."]]
    [nav-link [:home] :home "Home"]
    [c/navbar-divider]
    [nav-link [:factories] :office "Factories"]
    [nav-link [:items] :cube "Items"]
    [nav-link [:machines] :oil-field "Machines"]
    [nav-link [:recipes] :data-lineage "Recipes"]
    [c/navbar-divider]]
   [c/navbar-group-right
    [c/anchor-button {:class :bp3-minimal
                         :href "https://github.com/luketurner/factor"
                         :text "Github"}]
    [c/anchor-button {:class :bp3-minimal
                         :href "https://git.sr.ht/~luketurner/factor"
                         :text "sr.ht"}]]])

(defn secondary-navbar []
  (let [[x] @(subscribe [:selected-page])]
    (case x
      :home nil
      :factories [factory-page-bar]
      :items [item-page-bar]
      :machines [machine-page-bar]
      :recipes [recipe-page-bar])))

(defn footer []
  [c/navbar [c/navbar-group-left "Copyright 2021 Luke Turner"]])

(defn app []
  (let []
    [:<> 
     [:style (apply garden.core/css factor.styles/css-rules)]
     [:div.app-container
      [primary-navbar]
      [secondary-navbar]
      [main-content]
      [footer]]]))