(ns factor.util
  (:require [re-frame.core :refer [get-coeffect get-effect ->interceptor assoc-effect]]))

(defn new-uuid []
  (str (random-uuid)))

(defn add-fx [context fx]
  (assoc-effect context :fx (conj (get-effect context :fx []) fx)))

(defn dispatch-after [ev-fn]
  (->interceptor
   :after (fn [context]
            (let [ev (get-coeffect context :event)]
              (add-fx context [:dispatch (ev-fn ev)])))))

(defn filtered-update [xs filter-fn update-fn]
  (for [x xs] (if (filter-fn x) (update-fn x) x)))