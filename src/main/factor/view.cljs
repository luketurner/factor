(ns factor.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :refer [adapt-react-class reactify-component create-class]]
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
            ["ag-grid-react" :refer [AgGridColumn AgGridReact]]
            [medley.core :refer [map-keys]]))

(defn button [props & children] (into [(c b/Button) props] children))

(defn control-group [props & children]
  (into [(c b/ControlGroup) props] children))

(defn form-group [props & children]
  (into [(c b/FormGroup) props] children))

(defn suggest [type value on-change]
  (let [values @(subscribe [(keyword (str (name type) "-names"))])]
    [(c bs/Suggest) {:items values
                     :selected-item value
                     :on-item-select on-change}]))

(defn numeric-input [value on-change opts]
  [(c b/NumericInput) (merge {:value value
                              :on-value-change on-change}
                             opts)])

(defn suggest-numeric [type [x n] on-change]
  [control-group
   [numeric-input n #(on-change [x %]) {:min 0}]
   [suggest type  x #(on-change [% n])]])

(defn suggest-numeric-deletable [type kvp on-change on-delete]
  [control-group
   [suggest-numeric type kvp on-change]
   [button {:icon :delete :on-click on-delete}]])

(defn input [{:keys [on-change] :as props}]
  (let [override-props {:on-change #(-> % (.-target) (.-value) (on-change))}
        props (merge props override-props)]
    [(c b/InputGroup) props]))

(defn recipe-editor [id]
  (let [{:keys [name]} @(subscribe [:recipe id])
        on-change #(dispatch [:update-recipe id])]
    [:form
     [form-group {:label "Name"}
      [input {:value name}]]]))

;; (defn collapsible [props & children]
;;   (reagent.core/with-let [open? (reagent.core/atom false)]
;;     [:<>
;;      [:div  (:label props)]
;;      (into [(c b/Collapse) {:is-open @open?}] children)]))

(defn collapsible [label & children]
  (into [:details [:summary label]] children))

(defn card [p & children]
  (into [(c b/Card) p] children))

(defn recipe-editor-collapsible [id]
  (let [{:keys [name]} @(subscribe [:recipe id])]
    [card
     [collapsible
     (str "Recipe " (subs id 0 3))
     [recipe-editor id]]]))

(defn recipe-list []
  (let [recipes @(subscribe [:recipe-ids])]
    (into [:div {:styles {:max-width "30rem"}}] (for [r recipes] [recipe-editor-collapsible r]))))


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

(defn grid-cb->clj [ev]
  {:api (.-api ev)
   :data (-> ev (.-data) (js->clj) (->> (map-keys keyword)))})

(defn try-fn [f & args] (when (fn? f) (apply f args)))

(defn grid-cb [cb]
  (fn [ev]
    (->> ev (grid-cb->clj) (try-fn cb))))

(defn grid [props & children]
  (let [
        default-props  {:row-selection :multiple
                        :enter-moves-down true
                        :enter-moves-down-after-edit true
                        :edit-type "fullRow"
                        :default-col-def {:editable true}}
        override-props {:on-row-value-changed (grid-cb (:on-row-value-changed props))
                        :on-grid-ready        (grid-cb (:on-grid-ready        props))
                        :on-selection-changed (grid-cb (:on-selection-changed props))}
        grid-props     (merge default-props props override-props)]
   [:div.ag-theme-alpine {:style {:width "100%" :height "100%"}}
    (into [(c AgGridReact) grid-props] children)]))

(defn get-selected-ids [ev]
  (-> ev
      (:api)
      (.getSelectedRows)
      (js->clj)
      (->> (map #(get % :id)))
      (vec)))

(defn navbar [& children] (into [(c b/Navbar)] children))
(defn navbar-heading [& children] (into [(c b/Navbar.Heading)] children))
(defn navbar-divider [] [(c b/Navbar.Divider)])
(defn navbar-group-left [& children] (into [(c b/Navbar.Group) {:align :left}] children))
(defn navbar-group-right [& children] (into [(c b/Navbar.Group) {:align :right}] children))

(defn selected-text [num] (str "(" num " items selected)"))

(defn item-grid [items]
  (let [update-item #(dispatch [:update-item (get-in % [:data :id]) {:name (get-in % [:data :name])}])
        update-selection #(dispatch [:update-selection [:item %]])]
    [grid {:row-data items
           :on-grid-ready #(update-selection [])
           :on-row-value-changed update-item
           :on-selection-changed #(-> % (get-selected-ids) (update-selection))
           :column-defs [{:checkboxSelection true}
                         {:field :id :editable false}
                         {:field :name}]}]))

(defn item-page-bar []
  (let [create-item #(dispatch [:create-item])
        selected-items @(subscribe [:current-selection :item])
        delete-items #(dispatch [:delete-items selected-items])
        num-selected (count selected-items)]
    [navbar
     [navbar-group-left
      [navbar-heading "Item List"]
      [button {:class :bp3-minimal :on-click create-item :icon :plus :title "Add item"}]
      [navbar-divider]
      (when (< 0 num-selected)
        [:<>
         [:div (selected-text num-selected)]
         [button {:class :bp3-minimal
                  :on-click delete-items
                  :icon :minus :text "Delete"
                  :disabled (= num-selected 0)}]])]]))

(defn item-page [item]
  (let [all-items @(subscribe [:item-seq])]
    [:<>
     [item-page-bar]
     [item-grid all-items]]))

(defn machine-grid [machines]
  (let [update-machine #(dispatch [:update-machine (get-in % [:data :id]) (dissoc (:data %) :id)])
        update-selection #(dispatch [:update-selection [:machine %]])]
    [grid {:row-data machines
           :on-grid-ready #(update-selection [])
           :on-row-value-changed update-machine
           :on-selection-changed #(-> % (get-selected-ids) (update-selection))
           :column-defs [{:checkboxSelection true}
                         {:field :id :editable false}
                         {:field :name}
                         {:field :power}
                         {:field :speed}]}]))

(defn machine-page-bar []
  (let [create-machine #(dispatch [:create-machine])
        selected-machines @(subscribe [:current-selection :machine])
        delete-machines #(dispatch [:delete-machines selected-machines])
        num-selected (count selected-machines)]
    [navbar
     [navbar-group-left
      [navbar-heading "Machine List"]
      [button {:class :bp3-minimal :on-click create-machine :icon :plus :title "Add machine"}]
      [navbar-divider]
      (when (< 0 num-selected)
        [:<>
         [:div (selected-text num-selected)]
         [button {:class :bp3-minimal
                  :on-click delete-machines
                  :icon :minus :text "Delete"
                  :disabled (= num-selected 0)}]])]]))

(defn machine-page []
  (let [all-machines @(subscribe [:machine-seq])]
    [:<>
     [machine-page-bar]
     [machine-grid all-machines]]))

;; (defn rows-for-recipe [{:keys [id name input output machines]}]
;;   (map vector (repeat id) (repeat name) input output machines))

;; (defn rows-for-recipes [recipes]
;;   (reduce (fn [rs r] (concat rs (rows-for-recipe r))) recipes))

(defn recipe-io-editor [{:keys [value on-change]}]
  [grid {:row-data value
         :on-row-value-changed #(-> % (:data) (on-change))
         :column-defs [{:field 0}
                       {:field 1}]}])

(defn new-recipe-cell-renderer [key]
  (reactify-component
   (fn [{:keys [value]}]
     (let [data  (into [] (js->clj value))]
     (if (not-empty data)
       [grid {:default-col-def {:editable false}
              :row-data data
              :column-defs [{:field :0}
                            {:field :1}]}])))))

(defn new-recipe-cell-editor [key]
  (reactify-component
   (fn [{:keys [value stop-editing]}]
     [:textarea {:on-change stop-editing} value])))

(defn recipe-grid [recipes]
  (let [update-recipe #(dispatch [:update-recipe (get-in % [:data :id]) (dissoc (:data %) :id)])
        update-selection #(dispatch [:update-selection [:recipe %]])]
    [grid {:row-data recipes
           :on-grid-ready #(update-selection [])
           :on-row-value-changed update-recipe
           :on-selection-changed #(-> % (get-selected-ids) (update-selection))
           :column-defs [{:checkboxSelection true}
                         {:field :id :editable false}
                         {:field :name}
                         {:field :input :cellRendererFramework (new-recipe-cell-renderer :input)}]}]))

(defn recipe-page-bar []
  (let [create-recipe #(dispatch [:create-recipe])
        selected-recipes @(subscribe [:current-selection :recipe])
        delete-recipes #(dispatch [:delete-recipes selected-recipes])
        num-selected (count selected-recipes)]
    [navbar
     [navbar-group-left
      [navbar-heading "Recipe List"]
      [button {:class :bp3-minimal :on-click create-recipe :icon :plus :title "Add recipe"}]
      [navbar-divider]
      (when (< 0 num-selected)
        [:<>
         [:div (str "(" num-selected " recipes selected)")]
         [button {:class :bp3-minimal
                        :on-click delete-recipes
                        :icon :minus :text "Delete"
                        :disabled (= num-selected 0)}]])]]))

(defn recipe-page []
  (let [all-recipes @(subscribe [:recipe-seq])]
    [:<>
     [recipe-page-bar]
     [recipe-list]]))

(defn recipe-page2 []
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


;; (defn editor-for [v cb]
;;   (cond
;;     (map? v) [map-editor v cb]
;;     (seq? v) [list-editor v cb]
;;     (string? v) [string-editor v cb]))

;; (defn list-editor [v cb]
;;   (into [] (for [i v] [editor-for i cb])))

;; (defn string-editor [s cb] [:input {:type :text :value s}])

;; (defn map-editor [m cb]
;;   (into [] (for [[k v] m] [form-group {:label k} [editor-for v cb]])))



(defn app []
  (let []
    [:div.app-container {:style {:width "100vw"
                                 :height "100vh"
                                 :display :flex
                                 :flex-flow "column nowrap"}}
     [primary-navbar]
     [selected-page]
     [:footer "Copyright 2021 Luke Turner"]]))