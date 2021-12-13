(ns factor.view.app
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [as-element]]
            [factor.styles :as styles]
            [garden.core :refer [css]]
            [factor.components :as c]
            [factor.components.menus :as menus]
            [factor.view.settings :as settings]
            [factor.view.item :as item]
            [factor.view.recipe :as recipe]
            [factor.view.machine :as machine]
            [factor.view.factory :as factory]
            [factor.view.home :as home]
            [factor.view.notfound :as notfound]
            [factor.schema :as schema :refer [json-encode]]
            [cljs.core.match :refer [match]]
            [factor.view.help :as help]))

(defn view-specific-tools []
  (match @(subscribe [:page-route])
    [:items] [item/tools]
    [:machines] [machine/tools]
    [:recipes] [recipe/tools]
    :else [:<>]))

(defn primary-navbar []
  [c/navbar
   [c/navbar-group-left
    [c/navbar-heading [c/anchor-button {:class "bp3-minimal" :on-click (reagent/partial dispatch [:update-route [:home]])} [:strong "factor."]]]
    [c/navbar-divider]
    [menus/menus]
    [c/navbar-divider]
    [c/undo-redo]
    [c/navbar-divider]
    [view-specific-tools]]])


(defn main-content []
  [:main
   (match @(subscribe [:page-route])
     [:home] [home/page]
     [:factory _] [factory/page]
     [:factory _ _] [factory/page]
     [:items] [item/page]
     [:machines] [machine/page]
     [:recipes] [recipe/page]
     [:settings] [settings/page]
     [:help] [help/page]
     [:notfound] [notfound/page])])

(defn route-breadcrumbs []
  (let [route (json-encode schema/PageRoute @(subscribe [:page-route]))]
    [c/breadcrumbs {:items (for [r route] {:text r})}]))

(defn footer []
  [c/navbar
   [c/navbar-group-left
    [route-breadcrumbs]]
   [c/navbar-group-right
    [c/anchor-button {:class :bp3-minimal
                      :href "https://git.sr.ht/~luketurner/factor"
                      :text "sourcehut"}]
    [c/anchor-button {:class :bp3-minimal
                      :href "https://github.com/luketurner/factor"
                      :text "github"}]
    [:span {:style {:margin-left "0.5rem"}} "Copyright 2021 Luke Turner"]]])

(defn app []
  [:<>
   [:style (apply css styles/css-rules)]
   [c/hotkeys-provider
    [c/global-omnibar]
    [c/global-hotkeys]
    [:div.app-container
     [primary-navbar]
     [main-content]
     [footer]]]])