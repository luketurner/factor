(ns factor.interceptors
  (:require [re-frame.core :refer [->interceptor get-effect get-coeffect]]
            [factor.util :refer [add-fx]]
            [com.rpl.specter :refer [select-any]]
            [factor.navs :as nav]
            [day8.re-frame.undo :as undo]))

(defn ->world-saver []
  (->interceptor
   :id :world-saver
   :after (fn [ctx]
            (let [old-world (select-any nav/WORLD (get-coeffect ctx :db))
                  new-world (select-any nav/WORLD (get-effect   ctx :db))]
              (if (and new-world (not= new-world old-world))
                (add-fx ctx [:dispatch [:world-save new-world]])
                ctx)))))

(defn ->config-saver []
  (->interceptor
   :id :config-saver
   :after (fn [ctx]
            (let [old-config (select-any nav/CONFIG (get-coeffect ctx :db))
                  new-config (select-any nav/CONFIG (get-effect   ctx :db))]
              (if (and new-config (not= new-config old-config))
                (add-fx ctx [:dispatch [:config-save new-config]])
                ctx)))))

;; Custom interceptors that provide functions similar to [:purge-redos] event.
(defn ->purge-undos []
  (->interceptor
   :id :purge-undos
   :after (fn [ctx]
            (when (undo/undos?) (undo/clear-undos!))
            ctx)))

(defn ->purge-redos []
  (->interceptor
   :id :purge-redos
   :after (fn [ctx]
            (when (undo/redos?) (undo/clear-redos!))
            ctx)))