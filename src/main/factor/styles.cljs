(ns factor.styles
  (:require [garden.core :refer [css]]
            [garden.units :refer [px percent rem]]
            [garden.stylesheet :refer [at-media]]))

(def app
  (css [:html {:font "20px VT323"}]
       [:body {:margin 0}]
       [:* {:box-sizing "border-box"}]
       [:table {:border-spacing "0.5rem"}]
       [:.app-container {:min-height "100vh" :max-width "1024px" :margin "auto" :display "flex" :flex-flow "column nowrap"}]
       [:.main-container {:margin "0 1rem" :flex-grow "1" :display "flex" :flex-flow "row nowrap" :align-items "start"}]
       [:footer {:width "100%" :text-align "center" :margin "1rem 0"}]
       [:h1 {:font-size "1.5rem" :margin "1.5rem 0"}]
       [:h2 {:font-size "1.5rem" :margin "1.5rem 0" :font-weight "normal"}
        [:button {:font-size "1rem"}]]
       [:nav :.sub-nav {:margin-right "1rem" :display "flex" :flex-flow "column"}
        [:p {:margin "0.5rem 0" :text-align "right"}]
        [:a {:color "inherit" :text-decoration "inherit"}]
        [:.spacer {:height "1rem"}]]
       [:.factory-name {:font-size "inherit"}]
       [:button {:margin-left "1rem" :font "inherit" :border "none" :background "none" :cursor "pointer" :text-transform "uppercase"}]
       [:button:hover {:color "white" :background-color "black"}]
       [:button::before {:content "\"[\""}]
       [:button::after {:content "\"]\""}]
       [:dd {:margin-left "4rem" :margin-bottom "1rem"}]
       [:ul {:padding "0"}]
       [:li {:list-style "none"}]
       [:.rate-picker {:width "3em"}]
       [:.dropdown {:width "11em"}]
       [:input :select {:font "inherit" :border "none"}]
       [:.row {:display "flex" :flex-flow "row nowrap"}]
       [:.scrollable {:overflow-y :auto}]
       (at-media {:max-width (px 1024)}
                 [:.app-container {:margin "0 1rem"}]
                 [:.main-container {:flex-flow "column nowrap"}]
                 [:nav {:flex-flow "row wrap" :align-items "space-between"}
                  [:h1 {:width (percent 100) :margin 0 :text-align "center"}]
                  [:p {:margin "0 1rem"}]
                  [:.spacer {:display "none"}]])))
