(ns factor.app
  (:require [reagent.dom :as dom]
            [re-frame.core :refer [reg-global-interceptor]]
            [factor.fx :as fx]
            [factor.subs :as subs]
            [factor.events :as events]
            [factor.world :as world]
            [factor.db :as db]
            [factor.view :as view]))

(goog-define DEV false)

(when DEV (println "factor development mode enabled!"))

(defn reg-all []
  (subs/reg-all)
  (events/reg-all)
  (fx/reg-all)
  (when DEV (reg-global-interceptor (db/->world-validator)))
  (reg-global-interceptor (db/->world-saver))
  (reg-global-interceptor (db/->config-saver)))

(defn render []
  (dom/render [view/app] (js/document.getElementById "app")))

(defn init
  "Init function called on page load."
  []
  (reg-all)
  (db/init)
  (render))

(defn after-load
  "Triggered for every hot-reload when running development builds."
  []
  (println "reloading app...")
  (reg-all)
  (render))


