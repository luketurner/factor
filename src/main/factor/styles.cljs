(ns factor.styles
  (:require [garden.core :refer [css]]))

(def app-styles
  (css [:html {:font "20px VT323"}]
       [:body {:margin 0}]
       [:* {:box-sizing "border-box"}]
       [:table {:border-spacing "0.5rem"}]
       [:.app-container {:min-height "100vh" :max-width "1024px" :margin "auto" :display "flex" :flex-flow "column nowrap"}]
       [:.main-container {:flex-grow "1" :display "flex" :flex-flow "row nowrap" :align-items "start"}]
       [:footer {:width "100%" :text-align "center" :margin "1rem 0"}]
       [:h1 {:font-size "1.5rem" :margin "1.5rem 0"}]
       [:h2 {:font-size "1.5rem" :margin "1.5rem 0" :font-weight "normal"}
        [:button {:font-size "1rem"}]]
       [:nav {:margin "0 2rem"}
        [:p {:margin "0.5rem 0" :text-align "right"}]
        [:a {:color "inherit" :text-decoration "inherit"}]]
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
       [:.right {:float "right"}]))
