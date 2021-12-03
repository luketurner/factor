(ns factor.styles
  (:require [garden.selectors :as s]))

(def css-rules
  [[:* {:box-sizing "border-box"}]
   [:.app-container {:width "100vw" :height "100vh" :display :flex :flex-flow "column nowrap"}]
  ;;  [:.data-table :.data-table-editor {:margin "1rem"}]
   [:main {:height "calc(100% - 150px)" :display :flex :flex-flow "column nowrap" :overflow :auto}]
   [:.vertical-split {:height "100%"}]
   [(s/> :.vertical-split :*) {:height "50% !important" :overflow :auto}]
   [:.full-screen {:width "100%" :height "100%"}]
   [:.card-stack {:display :flex :flex-flow "column wrap" :align-content :start :height "100%" :width "100%"}]
   [:.w-2 {:width "2rem"}]
   [:.w-4 {:width "4rem"}]
   [:.w-6 {:width "6rem"}]
   [:.w-8 {:width "8rem"}]
   [:.w-10 {:width "10rem"}]
   [:.w-12 {:width "12rem"}]
   [:.w-14 {:width "14rem"}]
   [:.w-16 {:width "16rem"}]
   [:.w-18 {:width "18rem"}]
   [:.w-20 {:width "20rem"}]
   [:.m-1 {:margin "1rem"}]
   [:.bp3-non-ideal-state {:height "max-content"}]
   [:.pgraph-pane {:display "flex" :flex-flow "row nowrap" :height "100%"}]
   [:.pgraph-pane-left {:padding "1rem" :width "18rem" :display "flex" :flex-flow "column nowrap" :align-items "stretch"}]
   [(s/> :.pgraph-pane-left :.bp3-form-group) {:margin-bottom "2rem"}]
[:.pgraph-pane-right {:padding "1rem" :flex-grow 2}]])