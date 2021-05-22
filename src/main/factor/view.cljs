(ns factor.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [factor.styles :as styles]
            [factor.components.factory :as factory]
            [factor.components.item :as item]
            [factor.components.recipe :as recipe]
            [factor.components.machine :as machine]
            [factor.world :refer [str->world world->str]]
            [factor.components.widgets :as w]
            [ulti.core :refer [themed-stylesheet]]))

(defn nav-link [object-type]
  (let [sub-nav @(subscribe [:sub-nav])]
    [:a {:href "#" :on-click #(dispatch [:toggle-sub-nav object-type])}
     (when (= object-type sub-nav) ":") (name object-type)]))

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

(defn item-page []
  (let [items @(subscribe [:item-ids])]
    [:div
     [:h2 "items"]
     [w/list-editor {:data items
                   :row-fn (fn [id] [w/deletable-row {:on-delete [:delete-item id]}
                                     [item/editor id (= id (last items))]])
                   :empty-message [:p "You don't have any items."]
                   :add-fn #(dispatch [:create-item])}]]))

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

(defn world-page []
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

(defn app []
  (pr ulti.core/theme-schema)
  (let [sub-nav @(subscribe [:sub-nav])
        selected-object @(subscribe [:selected-object])]
    [:div.app-container
     [:style styles/app]
     [themed-stylesheet]
     [:div.main-container
      [:nav
       [:h1 [:a {:href "#" :on-click #(dispatch [:select-object nil])} "factor."]]
       [:p [nav-link :factories]]
       [:p [nav-link :items]]
       [:p [nav-link :recipes]]
       [:p [nav-link :machines]]
      ;;  [:p [nav-link :world]]
       [:p.spacer]
      ;;  [:p [nav-link :help]]
       [:p [:a {:href "https://git.sr.ht/~luketurner/factor"} "view source"]]]
      [:div.sub-nav
       (case sub-nav
         :factories [factory/sub-nav]
         :items [item/sub-nav]
         :recipes [recipe/sub-nav]
         :machines [machine/sub-nav]
         nil)]
      [:main (case (:object-type selected-object)
               :factory [factory/editor (:object-id selected-object)]
               :item [item/editor (:object-id selected-object)]
               :recipe [recipe/editor (:object-id selected-object)]
               :machine [machine/editor (:object-id selected-object)]
               [home-page])]]
     [:footer "Copyright 2020 Luke Turner"]]))