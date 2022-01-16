(ns factor.view.notfound
  (:require [reagent.core :refer [with-let as-element]]
            [re-frame.core :refer [dispatch]]
            [factor.components.wrappers :refer [button non-ideal-state]]))

(defn page
  []
  (with-let [go-home #(dispatch [:update-route [:home]])
             action (as-element [button {:text "Go Home"
                                         :intent :success
                                         :on-click go-home}])]
    [non-ideal-state {:icon :question-mark
                      :title "Page Not Found"
                      :description "Oops! This page doesn't exist!"
                      :action action}]))