(ns factor.app
  (:require [reagent.dom :refer [render]]
            [garden.core :refer [css]]
            [garden.selectors :as s]
            [re-frame.core :refer [reg-event-db reg-sub subscribe dispatch]]
            [factor.factory :refer [factory-page]]
            [factor.machine :refer [machine-page]]
            [factor.recipe :refer [recipe-page]]
            [factor.item :refer [item-page]]
            [factor.world :refer [world-page]]))




(reg-event-db :select-page (fn [db [_ page]] (assoc-in db [:ui :selected-page] page)))
(reg-sub :selected-page (fn [db _] (get-in db [:ui :selected-page])))

(reg-event-db :initialize-db (fn [] {:ui {:selected-page :home}
                                     :factories {}
                                     :world {:items {}
                                             :recipes {}
                                             :machines {}}}))

(def app-styles
  (css [:body {:font "20px VT323" :margin 0}]
       [:.app-container {:min-height "100vh" :max-width "1024px" :margin "auto" :display "flex" :flex-flow "row wrap" :align-items "start"}]
       [:footer {:width "100%" :align-self "end" :text-align "center"}]
       [:h2 {:font-size "2rem" :margin "2rem 0"}
        [:button {:font-size "1rem"}]]
       [:nav {:margin-right "2rem" :font-size "1.25rem"}
        [:p {:margin "0.5rem 0" :text-align "right"}]
        [:a {:color "inherit" :text-decoration "inherit"}]]
       [:h2 {:border-bottom "1px solid black"}]
       [:.factory-name {:font-size "inherit"}]
       [:button {:margin-left "1rem" :font "inherit" :border "none" :background "none" :cursor "pointer" :text-transform "uppercase"}]
       [:button:hover {:color "white" :background-color "black"}]
       [:button::before {:content "\"[\""}]
       [:button::after {:content "\"]\""}]
       [:dd {:margin-left "4rem" :margin-bottom "1rem"}]
       [:ul {:padding "0"}]
       [:li {:list-style "none"}]
       [:.rate-picker {:width "5em"}]
       [:.dropdown {:width "11em"}]
       [:input :select {:font "inherit" :border "none"}]))

(defn nav-link [page]
  (let [selected-page @(subscribe [:selected-page])]
    [:a {:href (str "#" (name page)) :on-click #(dispatch [:select-page page])}
     (when (= selected-page page) ":") (name page)]))

(defn home-page []
  [:p "Welcome home!"])

(defn help-page []
  [:p "Get your help here. (Once the page is finished.)"])

(defn app []
  (let [selected-page @(subscribe [:selected-page])]
    [:div.app-container
     [:style app-styles]
     [:nav
      [:h1 [:a {:href "#" :on-click #(dispatch [:select-page :home])} "factor."]]
      [:p [nav-link :factories]]
      [:p [nav-link :items]]
      [:p [nav-link :recipes]]
      [:p [nav-link :machines]]
      [:p [nav-link :world]]
      [:p [:br] [nav-link :help]]
      [:p [:a {:href "https://git.sr.ht/~luketurner/factor"} "view source"]]]
     [:main (case selected-page
              :home [home-page]
              :factories [factory-page]
              :items [item-page]
              :recipes [recipe-page]
              :machines [machine-page]
              :world [world-page]
              :help [help-page]
              [:p "Loading..."])]
     [:footer "Copyright 2020 Luke Turner"]]))


(defn init []
  (dispatch [:initialize-db])
  (render [app] (js/document.getElementById "app")))
