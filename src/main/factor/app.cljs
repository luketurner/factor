(ns factor.app
  (:require [reagent.dom :refer [render]]))

(defn app []
  [:h1 "Factor"])

(defn init []
  (render [app] (js/document.getElementById "app")))
