(ns factor.app
  (:require [reagent.dom :refer [render]]
            [re-frame.core :refer [reg-global-interceptor ->interceptor reg-event-fx reg-event-db reg-sub subscribe dispatch reg-fx]]
            [malli.core :as malli]
            [factor.factory.view :refer [factory-page]]
            [factor.factory.events]
            [factor.factory.subs]
            [factor.machine.view :refer [machine-page]]
            [factor.machine.events]
            [factor.machine.subs]
            [factor.recipe.view :refer [recipe-page]]
            [factor.recipe.events]
            [factor.recipe.subs]
            [factor.item.view :refer [item-page]]
            [factor.item.events]
            [factor.item.subs]
            [factor.world.view :refer [world-page]]
            [factor.world.events]
            [factor.world.subs]
            [factor.localstorage]
            [factor.styles :refer [app-styles]]))




(reg-event-db :select-page (fn [db [_ page]] (assoc-in db [:ui :selected-page] page)))
(reg-sub :selected-page (fn [db _] (get-in db [:ui :selected-page])))

(reg-event-db :initialize-db (fn [] {:ui {:selected-page :home}
                                     :world {:items {}
                                             :factories {}
                                             :recipes {}
                                             :machines {}}}))

; TODO -- for now, toast just sends to console
(reg-fx :toast (fn [data] (println data)))


(defn nav-link [page]
  (let [selected-page @(subscribe [:selected-page])]
    [:a {:href (str "#" (name page)) :on-click #(dispatch [:select-page page])}
     (when (= selected-page page) ":") (name page)]))

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

(defn app []
  (let [selected-page @(subscribe [:selected-page])]
    [:div.app-container
     [:style app-styles]
     [:div.main-container
      [:nav
       [:h1 [:a {:href "#" :on-click #(dispatch [:select-page :home])} "factor."]]
       [:p [nav-link :factories]]
       [:p [nav-link :items]]
       [:p [nav-link :recipes]]
       [:p [nav-link :machines]]
       [:p [nav-link :world]]
       [:p.spacer]
       [:p [nav-link :help]]
       [:p [:a {:href "https://git.sr.ht/~luketurner/factor"} "view source"]]]
      [:main (case selected-page
               :home [home-page]
               :factories [factory-page]
               :items [item-page]
               :recipes [recipe-page]
               :machines [machine-page]
               :world [world-page]
               :help [help-page]
               [:p "Loading..."])]]
     [:footer "Copyright 2020 Luke Turner"]]))

(def db-schema
  [:map {:closed true}
   [:world [:map {:closed true}
            [:factories
             [:map-of :string
              [:map {:closed true}
               [:name :string]
               [:desired-output [:map-of :string number?]]
               [:output [:map-of :string number?]]
               [:input [:map-of :string number?]]
               [:recipes [:map-of :string number?]]
               [:machines [:map-of :string number?]]]]]
            [:items
             [:map-of :string
              [:map {:closed true}
               [:name :string]]]]
            [:machines
             [:map-of :string
              [:map {:closed true}
               [:names :string]
               [:power number?]
               [:speed number?]]]]
            [:recipes
             [:map-of :string
              [:map {:closed true}
               [:input [:map-of :string number?]]
               [:output [:map-of :string number?]]
               [:machines [:set :string]]]]]]]
   [:ui [:map]]])

(defn validate-db [schema]
  (->interceptor
   :id :db-validator
   :after (fn [{{db :db} :effects :as ctx}]
             (if (and db (not (malli/validate schema db)))
               (throw (ex-info (str "validate failed:" (:errors (malli/explain schema db))) {}))
               ctx))))


(defn init []
  (dispatch [:initialize-db])
  (dispatch [:load-world])
  (reg-global-interceptor (validate-db db-schema))
  (render [app] (js/document.getElementById "app")))
