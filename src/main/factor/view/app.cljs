(ns factor.view.app
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [as-element]]
            [factor.styles :as styles]
            [garden.core :refer [css]]
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
            [factor.view.help :as help]
            [factor.components.cmd :refer [cmd-invocation-omnibar cmd-btn cmd-global-hotkeys]]
            [factor.components.wrappers :refer [navbar navbar-group-left navbar-heading anchor-button navbar-divider control-group breadcrumbs navbar-group-right hotkeys-provider]]))

(defn global-omnibar
  []
  (let [mode @(subscribe [:omnibar-mode])]
    (case mode
      :cmd-invocation [cmd-invocation-omnibar]
      :closed nil)))

(defn view-specific-tools []
  (match @(subscribe [:page-route])
    [:factory _] [factory/tools]
    [:factory _ _] [factory/tools]
    [:items] [item/tools]
    [:machines] [machine/tools]
    [:recipes] [recipe/tools]
    :else [:<>]))

(defn primary-navbar []
  [navbar
   [navbar-group-left
    [navbar-heading [anchor-button {:class "bp3-minimal" :on-click (reagent/partial dispatch [:update-route [:home]])} [:strong "factor."]]]
    [navbar-divider]
    [menus/menus]
    [navbar-divider]
    [control-group
     [cmd-btn {:minimal true :cmd :undo}]
     [cmd-btn {:minimal true :cmd :redo}]]
    [navbar-divider]
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
    [breadcrumbs {:items (for [r route] {:text r})}]))

(defn footer []
  [navbar
   [navbar-group-left
    [route-breadcrumbs]]
   [navbar-group-right
    [anchor-button {:class :bp3-minimal
                      :href "https://git.sr.ht/~luketurner/factor"
                      :text "sourcehut"}]
    [anchor-button {:class :bp3-minimal
                      :href "https://github.com/luketurner/factor"
                      :text "github"}]
    [:span {:style {:margin-left "0.5rem"}} "Copyright 2021 Luke Turner"]]])

(defn app []
  [:<>
   [:style (apply css styles/css-rules)]
   [hotkeys-provider
    [global-omnibar]
    [cmd-global-hotkeys]
    [:div.app-container
     [primary-navbar]
     [main-content]
     [footer]]]])