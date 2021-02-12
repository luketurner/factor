(ns factor.app
  (:require [reagent.dom :refer [render]]
            [garden.core :refer [css]]
            [re-frame.core :refer [reg-event-db reg-sub subscribe dispatch]]
            [factor.factory :refer [factory-page]]
            [factor.machine :refer [machine-page]]
            [factor.recipe :refer [recipe-page]]
            [factor.item :refer [item-page]]))




(reg-event-db :select-page (fn [db [_ page]] (assoc-in db [:ui :selected-page] page)))
(reg-sub :selected-page (fn [db _] (get-in db [:ui :selected-page])))

(reg-event-db :initialize-db (fn [] {:ui {:selected-page :factories}
                                     :factories {}
                                     :world {:items {}
                                             :recipes {}
                                             :machines {}}}))

(def app-styles
  (css [:#app {:max-width "786px" :margin "auto" }]
       [:h1 {:text-align "center"}]
       [:.navbar {:text-align "center"}]
       [:h2 {:border-bottom "1px solid black"}]
       [:.factory-name {:font-size "inherit"}]
       [:button {:margin-left "1rem"}]
       [:dd {:margin-left "4rem" :margin-bottom "1rem"}]
       [:ul {:padding "0"}]
       [:li {:list-style "none"}]
       [:.rate-picker {:width "5em"}]
       [:.dropdown {:width "11em"}]))

(defn app []
  [:div
   [:style app-styles]
   [:h1 "Factor"]
   [:div.navbar
    [:button {:on-click #(dispatch [:select-page :factories])} "Factories"]
    [:button {:on-click #(dispatch [:select-page :items])} "Items"]
    [:button {:on-click #(dispatch [:select-page :recipes])} "Recipes"]
    [:button {:on-click #(dispatch [:select-page :machines])} "Machines"]]
   (case @(subscribe [:selected-page])
     :factories [factory-page]
     :items [item-page]
     :recipes [recipe-page]
     :machines [machine-page]
     [:p "Loading..."])])


(defn init []
  (dispatch [:initialize-db])
  (render [app] (js/document.getElementById "app")))
