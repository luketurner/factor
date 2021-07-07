(ns factor.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :refer [as-element]]
            [factor.styles :as styles]
            [factor.world :as w]
            [factor.pgraph :as pgraph]
            [garden.core :refer [css]]
            [factor.util :refer [new-uuid]]
            [factor.components :as c]))

(defn nav-link [page icon text]
  (let [selected-page @(subscribe [:ui [:selected-page]])]
    [c/button {:class :bp3-minimal
               :on-click #(dispatch [:ui [:selected-page] page])
               :icon icon
               :text text
               :disabled (= selected-page page)}]))


(defn create-and-select-factory []
  (let [factory (w/new-factory)]
    (dispatch [:update-factory factory])
    (dispatch [:open-factory (:id factory)])))

(defn delete-and-unselect-factory [id]
  (let [factory-ids     @(subscribe [:factory-ids])
        other-factory-id (some #(when (not= % id) %) factory-ids)]
    (dispatch [:delete-factory id])
    (dispatch [:open-factory other-factory-id])))

(defn factory-page-bar []
  (let [selected       @(subscribe [:open-factory])
        select-factory #(dispatch [:open-factory %])]
    [c/navbar
     [c/navbar-group-left
      [c/navbar-heading "Factories"]
      [c/navbar-divider]
      [c/suggest :factory selected select-factory]
      [c/button {:class :bp3-minimal :on-click create-and-select-factory :icon :plus :text "Add factory"}]
      [c/navbar-divider]
      [c/button {:on-click #(delete-and-unselect-factory selected) :intent :danger :text "Delete factory"}]]]))

(defn get-item-name [id] (:name @(subscribe [:item id])))
(defn get-machine-name [id] (:name @(subscribe [:machine id])))
(defn get-recipe-name [id] (:name @(subscribe [:recipe id])))

(defn factory-page []
  (if-let [factory-id @(subscribe [:open-factory])]
    (let [factory @(subscribe [:factory factory-id])
          pg @(subscribe [:factory-pgraph factory-id])
          update-factory #(dispatch [:update-factory %])]
      [:div.card-stack
       [c/card-lg
        [c/form-group {:label "ID"}
         [c/input {:value (:id factory) :disabled true}]]
        [c/form-group {:label "Name"}
         [c/input {:value (:name factory) :on-change #(update-factory (assoc factory :name %))}]]]
       [c/card-lg [c/form-group {:label "Desired Outputs"}
                   [c/quantity-set-input :item (:desired-output factory) #(update-factory (assoc factory :desired-output %))]]]
       [c/card-lg [c/form-group {:label "Outputs"}
                   (into [:ul] (for [[x n] (:output (pgraph/get-node pg :root))] [:li n "x " (get-item-name x)]))]]
       [c/card-lg [c/form-group {:label "Inputs"}
                   (into [:ul] (for [[x n] (:input (pgraph/get-node pg :root))] [:li n "x " (get-item-name x)]))]]
      ;;  [c/card-lg [c/form-group {:label "Machines"}
      ;;              (into [:ul] (for [[x n] (:machines pgraph)] [:li n "x " (get-machine-name x)]))]]
      ;;  [c/card-lg [c/form-group {:label "Recipes"}
      ;;              (into [:ul] (for [[x n] (:recipes pgraph)] [:li n "x " (get-recipe-name x)]))]]
       [c/card-lg [c/form-group {:label "Content"}
                   [c/textarea {:value (pr-str factory) :read-only true :style {:width "100%" :height "150px"}}]]]
       [c/card-lg [c/form-group {:label "Production Graph"}
                   [c/textarea {:value (pr-str (dissoc pg :world)) :read-only true :style {:width "100%" :height "150px"}}]]]])
    [c/non-ideal-state {:title "No factories"
                        :description "Create a factory to get started."
                        :action (as-element [c/button {:text "Create Factory"
                                                       :intent :success
                                                       :on-click create-and-select-factory}])}]))

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
  (let [update-item #(dispatch [:update-item (:data %)])
        update-selection #(dispatch [:ui [:item-page :selected] %])]
    [c/grid {:row-data items
           :on-grid-ready #(update-selection [])
           :on-row-value-changed update-item
           :on-selection-changed #(-> % (get-selected-ids) (update-selection))
           :column-defs [{:checkboxSelection true}
                         {:field :id}
                         {:field :name :editable true}]}]))

(defn item-page-bar []
  (let [create-item    #(dispatch [:update-item (w/new-item)])
        selected-items @(subscribe [:ui [:item-page :selected]])
        delete-items   #(do 
                          (dispatch [:delete-items selected-items])
                          (dispatch [:ui [:item-page :selected] []]))
        num-selected   (count selected-items)]
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
  (let [update-machine #(dispatch [:update-machine (:data %)])
        update-selection #(dispatch [:ui [:machine-page :selected] %])]
    [c/grid {:row-data machines
             :on-grid-ready #(update-selection [])
             :on-row-value-changed #(update-machine %)
             :on-selection-changed #(-> % (get-selected-ids) (update-selection))
             :column-defs [{:checkboxSelection true}
                           {:field :id}
                           {:field :name :editable true}
                           {:field :power :editable true}
                           {:field :speed :editable true}]}]))

(defn machine-page-bar []
  (let [create-machine #(dispatch [:update-machine (w/new-machine)])
        selected-machines @(subscribe [:ui [:machine-page :selected]])
        delete-machines   #(do
                             (dispatch [:delete-machines selected-machines])
                             (dispatch [:ui [:machine-page :selected] []]))
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
        update-recipe #(dispatch [:update-recipe (:data %)])
        update-selection #(dispatch [:ui [:recipe-page :selected] %])]
    [c/grid {:row-data all-recipes
             :on-grid-ready #(update-selection [])
             :on-row-value-changed update-recipe
             :on-selection-changed #(-> % (get-selected-ids) (update-selection))
             :column-defs [{:checkboxSelection true}
                           {:field :id}
                           {:field :name}]}]))

(defn recipe-page-bar []
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


(defn name-editor [thing on-change]
  [c/form-group {:label "Name"}
   [c/input {:value (:name thing)
             :on-change #(->> % (assoc thing :name) (on-change))}]])

(defn input-editor [thing on-change]
  [c/form-group {:label "Inputs"}
   [c/quantity-set-input :item (:input thing) #(-> thing (assoc :input %) (on-change))]])

(defn output-editor [thing on-change]
  [c/form-group {:label "Outputs"}
   [c/quantity-set-input :item (:output thing) #(-> thing (assoc :output %) (on-change))]])

(defn machine-list-editor [thing on-change]
  [c/form-group {:label "Machines"}
   [c/list-input :machine (:machines thing) #(-> thing (assoc :machines (set %)) (on-change))]])

(defn recipe-editor [id]
  (let [recipe @(subscribe [:recipe id])
        update-recipe #(dispatch [:update-recipe %])]
    [:div.card-stack
     [c/card-lg [name-editor recipe update-recipe]]
     [c/card-lg [input-editor recipe update-recipe]]
     [c/card-lg [output-editor recipe update-recipe]]
     [c/card-lg [machine-list-editor recipe update-recipe]]]))

(defn recipe-page-editor []
  (let [selected-recipes   @(subscribe [:ui [:recipe-page :selected]])]
    [:div.data-table-editor
     (case (count selected-recipes)
       0 [:div "No recipes selected."]
       1 [recipe-editor (first selected-recipes)]
       [:div "Select a single recipe to edit."])]))

(defn recipe-page []
  [:div.vertical-split
   [recipe-grid]
   [recipe-page-editor]])

(defn settings-page-bar [] [c/navbar])

(defn settings-page []
  [:div
   [:p "WIP"]
   [c/button {:on-click #(dispatch [:world-reset w/empty-world]) :text "RESET WORLD"}]])

(defn main-content []
  (let [[x] @(subscribe [:ui [:selected-page]])]
    [:main
     (case x
      :home [home-page]
      :factories [factory-page]
      :items [item-page]
      :machines [machine-page]
      :recipes [recipe-page]
      :settings [settings-page])]))

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
    [c/navbar-divider]
    [nav-link [:settings] :settings "Settings"]
]
   ])

(defn secondary-navbar []
  (let [[x] @(subscribe [:ui [:selected-page]])]
    (case x
      :home [c/navbar]
      :factories [factory-page-bar]
      :items [item-page-bar]
      :machines [machine-page-bar]
      :recipes [recipe-page-bar]
      :settings [settings-page-bar])))

(defn footer []
  [c/navbar 
   [c/navbar-group-left "Copyright 2021 Luke Turner"]
   [c/navbar-group-right
    [c/anchor-button {:class :bp3-minimal
                      :href "https://git.sr.ht/~luketurner/factor"
                      :text "sourcehut"}]
    [c/anchor-button {:class :bp3-minimal
                      :href "https://github.com/luketurner/factor"
                      :text "github"}]]])

(defn app []
  (let []
    [:<> 
     [:style (apply css styles/css-rules)]
     [:div.app-container
      [primary-navbar]
      [secondary-navbar]
      [main-content]
      [footer]]]))