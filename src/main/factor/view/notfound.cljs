(ns factor.view.notfound
  (:require [reagent.core :refer [with-let as-element]]
            [re-frame.core :refer [dispatch]]
            [factor.components :as c]))

(defn navbar [] [c/navbar])

(defn page
  []
  (with-let [go-home #(dispatch [:update-route [:home]])
             action (as-element [c/button {:text "Go Home"
                                           :intent :success
                                           :on-click go-home}])]
    [c/non-ideal-state {:icon :question-mark
                        :title "Page Not Found"
                        :description "Oops! This page doesn't exist!"
                        :action action}]))