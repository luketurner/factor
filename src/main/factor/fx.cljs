(ns factor.fx
  "Registers all effects and coeffects.
   
   Coeffects:
     :localstorage
   Effects:
     :localstorage
     :toast
   
   ## Local Storage
   
   To get data out of localstorage, use the :localstorage coeffect.
   You can request the key you want with:

   (inject-cofx :localstorage :my-key)

   Then, your reg-event-fx handlers will get a coeffect like:

   {:localstorage {:my-key \"data-loaded-from-localstorage\"}}

   To put data into localstorage, use the :localstorage effect.
   Your reg-event-fx handler can add an effect like:

   {:localstorage {:my-key \"data-to-save\"}}

   Note that localstorage keys are expected to keywords, and are sent through
   `name` before being sent to localstorage. Values are transparently serialized
   to/from EDN.
   
   ## Toast
   
   For now, toast just logs to console."
  (:require [re-frame.core :refer [reg-cofx reg-fx]]
            [clojure.edn :as edn]))

(defn reg-all []
  (reg-cofx
   :localstorage
   (fn [cofx key]
     (assoc-in cofx [:localstorage key]
               (edn/read-string (js/window.localStorage.getItem (name key))))))

  (reg-fx
   :localstorage
   (fn [key-values]
     (doseq [[key value] key-values]
       (js/window.localStorage.setItem (name key) (pr-str value)))))

  (reg-fx :toast (fn [data] (println data))))
