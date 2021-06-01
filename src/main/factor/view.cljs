(ns factor.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :refer [adapt-react-class]]
            [factor.styles :as styles]
            [factor.components.factory :as factory]
            [factor.components.item :as item]
            [factor.components.recipe :as recipe]
            [factor.components.machine :as machine]
            [factor.world :refer [str->world world->str]]
            [factor.components.widgets :as w]
            [factor.util :refer [c]]
            ["@blueprintjs/core" :as b]
            ["@blueprintjs/select" :as bs]
            ["ag-grid-react" :refer [AgGridColumn AgGridReact]]))

(defn nav-link [page icon text]
  (let [selected-page @(subscribe [:selected-page])]
    [(c b/Button) {:class :bp3-minimal
                   :on-click #(dispatch [:select-page page])
                   :icon icon
                   :text text
                   :disabled (= selected-page page)}]))

(defn factory-page []
  (let [factories @(subscribe [:factory-ids])]
    [:div
     (if (not-empty factories)
       (into [:div] (for [fact-id factories] [factory/editor fact-id]))
       [:div [:h2 "factories"] [:p "You don't have any factories."]])
     [w/button {:on-click #(dispatch [:create-factory])} "Add factory"]]))

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

(defn help-page []
  [:div
   [:h2 "help"]
   [:p "Get your help here. (Once the page is finished.)"]])

(defn item-grid [items]
  (let [update-item #(dispatch [:update-item (get % "id") {:name (get % "name")}])
        update-selection #(dispatch [:update-selection [:item %]])]
    [:div.ag-theme-alpine {:style {:width "100%" :height "100%"}}
     [(c AgGridReact) {:row-data items
                       :row-selection :multiple
                       :enter-moves-down true
                       :enter-moves-down-after-edit true
                       :edit-type "fullRow"
                       :on-row-value-changed #(-> % (js->clj) (get "data") (update-item))
                       :on-selection-changed (fn [x] (-> x
                                                         (.-api)
                                                         (.getSelectedRows)
                                                         (js->clj)
                                                         (->> (map #(get % "id")))
                                                         (vec)
                                                         (update-selection)))}
      [(c AgGridColumn) {:checkboxSelection true}]
      [(c AgGridColumn) {:field :id}]
      [(c AgGridColumn) {:field :name
                         :editable true
                         :single-click-edit true}]]]))

(defn item-page-bar []
  (let [create-item #(dispatch [:create-item])
        selected-items @(subscribe [:current-selection :item])
        delete-items #(dispatch [:delete-items selected-items])
        num-selected (count selected-items)]
    [(c b/Navbar)
     [(c b/Navbar.Group) {:align :left}
      [(c b/Navbar.Heading) "Item List"]
      [(c b/Button) {:class :bp3-minimal :on-click create-item :icon :plus :title "Add item"}]
      [(c b/Navbar.Divider)]
      (when (< 0 num-selected)
        [:<>
         [:div (str "(" num-selected " items selected)")]
         [(c b/Button) {:class :bp3-minimal
                        :on-click delete-items
                        :icon :minus :text "Delete"
                        :disabled (= num-selected 0)}]])]]))

(defn item-page [item]
  (let [all-items @(subscribe [:item-seq])]
    [:<>
     [item-page-bar]
     [item-grid all-items]]))

(defn machine-page []
  (let [machines @(subscribe [:machine-ids])]
    [:<>
     [:h2 "machines"]
     [w/list-editor {:data machines
                     :row-fn (fn [id] [machine/editor id (= id (last machines))])
                     :empty-message [:p "You don't have any machines."]
                     :add-fn #(dispatch [:create-machine])
                     :del-fn #(dispatch [:delete-machine %])}]]))

(defn recipe-page []
  (let [recipes @(subscribe [:recipe-ids])]
    [:div
     [:h2 "recipes"]
     (if (not-empty recipes)
       (into [:div] (for [id recipes] [recipe/editor id]))
       [:p "You don't have any recipes."])
     [w/button {:on-click [:create-recipe :expanded]} "Add recipe"]]))

(defn settings-page []
  (let [item-count (or @(subscribe [:item-count]) "No")
        machine-count (or @(subscribe [:machine-count]) "No")
        recipe-count (or @(subscribe [:recipe-count]) "No")
        world-data (world->str @(subscribe [:world-data]))
        update-world-data #(dispatch [:world-import (str->world (.-value (.-target %)))])]
    [:div
     [:h2 "world"]
     [:dl
      [:dt "Statistics"]
      [:dd
       [:ul
        [:li (str item-count " items")]
        [:li (str machine-count " machines")]
        [:li (str recipe-count " recipes")]]]
      [:dt "Import"]
      [:dd
       [:p "Paste world data: " [:input {:type "text" :on-change update-world-data}]]]
      [:dt "Export"]
      [:dd
       [:p "Copy world data: " [:input {:type "text" :read-only true :value world-data}]]]
      [:dt "Wipe World Data"]
      [:dd
       [w/button {:on-click [:world-reset]} "DELETE WORLD PERMANENTLY"]]]]))

;; (defn megasearch []
;;   (let [factories @(subscribe [:factory-names])
;;         items @(subscribe [:item-names])
;;         machines @(subscribe [:machine-names])
;;         recipes @(subscribe [:recipe-names])
;;         navigate #(print "TODO" %)]
;;     [(c bs/Suggest)
;;      {:items (concat (keys factories) (keys items) (keys machines) (keys recipes))
;;       :item-renderer (fn [v] (:name v))
;;       :on-item-select navigate}]))

(defn selected-page []
  (let [[x] @(subscribe [:selected-page])]
    (case x
      :home [home-page]
      :help [help-page]
      :settings [settings-page]
      :factories [factory-page]
      :items [item-page]
      :machines [machine-page]
      :recipes [recipe-page])))

(defn primary-navbar []
  [(c b/Navbar)
   [(c b/Navbar.Group) {:align :left}
    [(c b/Navbar.Heading) [:strong "factor."]]
    [nav-link [:home] :home "Home"]
    [nav-link [:help] :help "Help"]
    [nav-link [:settings] :settings "Settings"]
    [(c b/Navbar.Divider)]
    [nav-link [:factories] :office "Factories"]
    [nav-link [:items] :cube "Items"]
    [nav-link [:machines] :oil-field "Machines"]
    [nav-link [:recipes] :data-lineage "Recipes"]
    [(c b/Navbar.Divider)]]
   [(c b/Navbar.Group) {:align :right}
    [(c b/AnchorButton) {:class :bp3-minimal
                         :href "https://github.com/luketurner/factor"
                         :text "Github"}]
    [(c b/AnchorButton) {:class :bp3-minimal
                         :href "https://git.sr.ht/~luketurner/factor"
                         :text "sr.ht"}]]])

(defn app []
  (let []
    [:div.app-container {:style {:width "100vw"
                                 :height "100vh"
                                 :display :flex
                                 :flex-flow "column nowrap"}}
     [primary-navbar]
     [selected-page]
     [:footer "Copyright 2021 Luke Turner"]]))