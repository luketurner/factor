(ns factor.view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :refer [as-element]]
            [factor.styles :as styles]
            [factor.world :as w]
            [factor.pgraph :as pgraph]
            [garden.core :refer [css]]
            [factor.util :refer [new-uuid]]
            [factor.components :as c]
            [factor.view.settings :as settings]
            [factor.view.item :as item]
            [factor.view.recipe :as recipe]
            [factor.view.machine :as machine]
            [factor.view.factory :as factory]
            [factor.view.home :as home]))

(defn primary-navbar []
  [c/navbar
   [c/navbar-group-left
    [c/navbar-heading [:strong "factor."]]
    [c/nav-link :home :home "Home"]
    [c/navbar-divider]
    [c/nav-link :factories :office "Factories"]
    [c/nav-link :items :cube "Items"]
    [c/nav-link :machines :oil-field "Machines"]
    [c/nav-link :recipes :data-lineage "Recipes"]
    [c/navbar-divider]
    [c/nav-link :settings :settings "Settings"]]])

(defn secondary-navbar []
  (let [x @(subscribe [:ui [:selected-page]])]
    (case x
      :home [home/navbar]
      :factories [factory/navbar]
      :items [item/navbar]
      :machines [machine/navbar]
      :recipes [recipe/navbar]
      :settings [settings/navbar])))

(defn main-content []
  (let [x @(subscribe [:ui [:selected-page]])]
    [:main
     (case x
       :home [home/page]
       :factories [factory/page]
       :items [item/page]
       :machines [machine/page]
       :recipes [recipe/page]
       :settings [settings/page])]))

(defn footer []
  [c/navbar 
   [c/navbar-group-left "Copyright 2021 Luke Turner"]
   [c/navbar-group-right
    [c/anchor-button {:class :bp3-minimal
                      :href "https://git.sr.ht/~luketurner/factor"
                      :text "sourcehut"}]
    [c/anchor-button {:class :bp3-minimal
                      :href "https://github.com/luketurner/factor"
                      :text "github"}]]])

(defn app []
  [:<>
   [:style (apply css styles/css-rules)]
   [:div.app-container
    [primary-navbar]
    [secondary-navbar]
    [main-content]
    [footer]]])