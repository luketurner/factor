(ns factor.util
  (:require [re-frame.core :refer [get-coeffect get-effect ->interceptor assoc-effect]]
            [reagent.core :refer [adapt-react-class]]))

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

(defn c [x] (adapt-react-class x))

(defn dissoc-in [x path k]
  (update-in x path #(dissoc % k)))

(defn without [x vs]
  (remove #(= % vs) x))

(defn delete-index [v ix]
  (into [] (concat (subvec v 0 ix)
                   (subvec v (inc ix)))))

(defn move-index-behind [v ix]
  (let [ix2 (max (dec ix) 0)]
    (-> v
        (assoc ix2 (nth v ix))
        (assoc ix (nth v ix2)))))

(defn move-index-ahead [v ix]
  (let [ix2 (min (inc ix) (dec (count v)))]
    (-> v
        (assoc ix2 (nth v ix))
        (assoc ix (nth v ix2)))))

(defn ipairs [coll] (map list coll (range)))

(defn try-fn
  "Returns `(apply f args)` if `f` is a function, otherwise returns `nil`."
  [f & args] (when (fn? f) (apply f args)))

(defn pick-max
  "Returns the max of value you get when calling (f x) for each x in xs."
  [f xs]
  (apply max-key f xs))