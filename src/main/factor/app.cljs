(ns factor.app
  "Entry point for the application. Call `init` function to start."
  (:require [reagent.dom :as dom]
            [re-frame.core :refer [reg-global-interceptor dispatch-sync]]
            [factor.fx :as fx]
            [factor.subs :as subs]
            [factor.events :as events]
            [factor.interceptors :refer [->world-validator ->world-saver ->config-saver]]
            [factor.view.app :refer [app]]))

(goog-define DEV false)

(when DEV (println "factor development mode enabled!"))

(defn reg-all []
  (subs/reg-all)
  (events/reg-all)
  (fx/reg-all)
  (when DEV (reg-global-interceptor (->world-validator)))
  (reg-global-interceptor (->world-saver))
  (reg-global-interceptor (->config-saver)))

(defn render []
  (dom/render [app] (js/document.getElementById "app")))

(defn init
  "Init function called on page load."
  []
  (reg-all)
  (dispatch-sync [:initialize-db])
  (dispatch-sync [:world-load])
  (dispatch-sync [:config-load])
  (render))

(defn after-load
  "Triggered for every hot-reload when running development builds."
  []
  (println "reloading app...")
  (reg-all)
  (render))


