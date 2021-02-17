(ns factor.util
  (:require [re-frame.core :refer [get-coeffect get-effect ->interceptor assoc-effect]]))

(defn new-uuid []
  (str (random-uuid)))

(defn dispatch-after [ev-fn]
  (->interceptor
   :after (fn [context]
            (let [ev (get-coeffect context :event)
                  existing-fx (get-effect context :fx [])
                  update-fx [:dispatch (ev-fn ev)]]
              (assoc-effect context :fx (conj existing-fx update-fx))))))