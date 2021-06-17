(ns factor.styles
  (:require [garden.core :refer [css]]
            [garden.units :refer [px percent rem]]
            [garden.selectors :as s]
            [garden.stylesheet :refer [at-media]]))

(def css-rules
  [[:* {:box-sizing "border-box"}]
   [:.app-container {:width "100vw" :height "100vh" :display :flex :flex-flow "column nowrap"}]
  ;;  [:.data-table :.data-table-editor {:margin "1rem"}]
   [:main {:height "calc(100% - 150px)" :display :flex :flex-flow "column nowrap"}]
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
   [:.m-1 {:margin "1rem"}]])