(ns factor.view.settings
  (:require [factor.components :as c]
            [re-frame.core :refer [dispatch subscribe]]
            [factor.world :as w]))

(defn navbar [] [c/navbar])

(defn page []
  [:div
   [:p "WIP"]
   [c/button {:on-click #(dispatch [:world-reset w/empty-world]) :text "RESET WORLD"}]])
