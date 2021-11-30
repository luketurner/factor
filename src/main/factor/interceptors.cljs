(ns factor.interceptors
  (:require [re-frame.core :refer [->interceptor get-effect get-coeffect]]
            [factor.util :refer [add-fx]]
            [day8.re-frame.undo :as undo]))

(defn ->world-saver []
  (->interceptor
   :id :world-saver
   :after (fn [ctx]
            (let [{old-world :world} (get-coeffect ctx :db)
                  {new-world :world} (get-effect   ctx :db)]
              (if (and new-world (not= new-world old-world))
                (add-fx ctx [:dispatch [:world-save new-world]])
                ctx)))))

(defn ->config-saver []
  (->interceptor
   :id :config-saver
   :after (fn [ctx]
            (let [{old-config :config} (get-coeffect ctx :db)
                  {new-config :config} (get-effect   ctx :db)]
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