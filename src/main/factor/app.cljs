(ns factor.app
  (:require [reagent.dom :as dom]
            [medley.core :refer [dissoc-in]]
            [re-frame.core :refer [dispatch-sync reg-global-interceptor ->interceptor reg-event-fx reg-event-db reg-sub subscribe dispatch reg-fx]]
            [malli.core :as malli]
            [factor.factory.view :refer [factory-page]]
            [factor.machine.view :refer [machine-page]]
            [factor.recipe.view :refer [recipe-page]]
            [factor.item.view :refer [item-page]]
            [factor.world.view :refer [world-page]]
            [factor.localstorage]
            [factor.subs :refer [reg-all-subs]]
            [factor.events :refer [reg-all-events]]
            [factor.styles :refer [app-styles]]
            [factor.util :refer [add-fx]]))




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
               [:name :string]
               [:power number?]
               [:speed number?]]]]
            [:recipes
             [:map-of :string
              [:map {:closed true}
               [:input [:map-of :string number?]]
               [:output [:map-of :string number?]]
               [:machines [:set :string]]]]]]]
   [:ui [:map]]])

(defn ->db-validator [schema]
  (->interceptor
   :id :db-validator
   :after (fn [{{db :db} :effects :as ctx}]
            (let [invalid? (and db (not (malli/validate schema db)))]
              (cond-> ctx
                invalid? (dissoc-in [:effects :db])
                invalid? (add-fx [:toast (str "validate failed: " (pr-str (malli/explain schema db)))]))))))

(defn ->world-saver []
  (->interceptor
   :id :world-saver
   :after (fn [{{{old-world :world} :db} :coeffects
                {{new-world :world} :db} :effects :as context}]
            (if (and new-world (not= new-world old-world))
              (add-fx context [:dispatch [:save-world new-world]])
              context))))

(defn reg-all []
  (reg-all-subs)
  (reg-all-events)
  (reg-global-interceptor (->db-validator db-schema))
  (reg-global-interceptor (->world-saver)))

(defn render []
  (dom/render [app] (js/document.getElementById "app")))

(defn init-db []
  (dispatch-sync [:initialize-db])
  (dispatch-sync [:load-world]))

(defn init
  "Init function called on page load."
  []
  (reg-all)
  (init-db)
  (render))

(defn after-load
  "Triggered for every hot-reload when running development builds."
  []
  (println "reloading app...")
  (reg-all)
  (render))


