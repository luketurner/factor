(ns factor.fx
  (:require [re-frame.core :refer [reg-cofx reg-fx]]
            [clojure.edn :as edn]))

;;
;; Notes on :localstorage effect and coeffect
;;   To get data /out/ of localstorage, request the key you want with
;;   (inject-cofx :localstorage :my-key)
;;   Then, your reg-event-fx handlers will get a coeffect like:
;;   {:localstorage {:my-key "data-loaded-from-localstorage"}}
;;   
;;   To get data /in/ to localstorage, the event handler can add an effect
;;   of the same form -- e.g.
;;   {:localstorage {:my-key "data-loaded-from-localstorage"}}
;;   will save the string into localstorage with the :my-key key.
;;   
;;   Note that keys are expected to be keywords, and are sent through (name)
;;   before being sent to localstorage. Values are transparently serialized
;;   to/from EDN.
;; 
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

  ; TODO -- for now, toast just sends to console
  (reg-fx :toast (fn [data] (println data))))
