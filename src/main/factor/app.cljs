(ns factor.app
  "Entry point for the application. Call `init` function to start."
  (:require [reagent.dom :as dom]
            [re-frame.core :refer [reg-global-interceptor dispatch-sync dispatch]]
            [factor.fx :as fx]
            [factor.subs :as subs]
            [factor.events :as events]
            [factor.interceptors :refer [->world-saver ->config-saver]]
            [factor.util :refer [url->route]]
            [factor.view.app :refer [app]]))

(goog-define DEV false)

(when DEV (println "factor development mode enabled!"))

(defn reg-all []
  (subs/reg-all)
  (events/reg-all)
  (fx/reg-all)
  (reg-global-interceptor (->world-saver))
  (reg-global-interceptor (->config-saver)))

(defn render []
  (dom/render [app] (js/document.getElementById "app")))

(defn sync-page-route []
  (.addEventListener js/window "hashchange" (fn [x] (dispatch [:update-route (url->route (.-newURL x))])))
  (dispatch-sync [:update-route (url->route (.-href js/location))]))

(defn sync-page-focus []
  (.addEventListener
   (js/document.getElementById "app")
   "focus"
   (fn [x] (let [id (.-id (.-target x))]
             (when-not (empty? id)
               (dispatch-sync [:focus id])))) true))

(defn init
  "Init function called on page load."
  []
  (reg-all)
  (dispatch-sync [:initialize-db])
  (dispatch-sync [:world-load])
  (dispatch-sync [:config-load])
  (sync-page-route)
  (sync-page-focus)
  (render))

(defn after-load
  "Triggered for every hot-reload when running development builds."
  []
  (println "reloading app...")
  (reg-all)
  (render))


