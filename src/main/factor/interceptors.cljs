(ns factor.interceptors
  (:require [re-frame.core :refer [->interceptor get-effect assoc-effect]]
            [malli.core :refer [validate explain]]
            [factor.util :refer [add-fx]]
            [factor.schema :refer [World]]
            [day8.re-frame.undo :as undo]))

(defn ->world-saver []
  (->interceptor
   :id :world-saver
   :after (fn [{{{old-world :world} :db} :coeffects
                {{new-world :world} :db} :effects :as context}]
            (if (and new-world (not= new-world old-world))
              (add-fx context [:dispatch [:world-save new-world]])
              context))))

(defn ->config-saver []
  (->interceptor
   :id :config-saver
   :after (fn [{{{old-config :config} :db} :coeffects
                {{new-config :config} :db} :effects :as context}]
            (if (and new-config (not= new-config old-config))
              (add-fx context [:dispatch [:config-save new-config]])
              context))))

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