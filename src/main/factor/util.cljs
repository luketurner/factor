(ns factor.util
  (:require [re-frame.core :refer [get-coeffect get-effect ->interceptor assoc-effect]]
            [reagent.core :refer [adapt-react-class]]
            [clojure.string :as string]
            [factor.schema :as schema :refer [json-decode]]
            [com.rpl.specter :as s]
            [clojure.edn :as edn]))

(defn new-uuid
  "Returns a string containing a random UUID."
  []
  (str (random-uuid)))

(defn add-fx
  "Appends the effect vec to the end of the :fx effect for the given context.
   If there is no :fx effect, it will be added automatically.e"
  [context fx]
  (assoc-effect context :fx (conj (get-effect context :fx []) fx)))

(defn dispatch-after
  "Interceptor that dispatches another event after the intercepted event is finished.
   Accepts a function that will be called with the intercepted event and is expected to
   return a vec for the event to be dispatched."
  [ev-fn]
  (->interceptor
   :after (fn [context]
            (let [ev (get-coeffect context :event)]
              (add-fx context [:dispatch (ev-fn ev)])))))

(defn cl
  "Shorthand for reagent.core/adapt-react-class"
  [x]
  (adapt-react-class x))

(defn dissoc-in
  "Like dissoc-in from medley, but does not remove empty collections.
   For example:
   
   (factor.util/dissoc-in {:foo {:bar 1}} [:foo :bar])
     => {:foo {}}
   (medley.core/dissoc-in {:foo {:bar 1}} [:foo :bar])
     => {}
   "
  [x path k]
  (update-in x path #(dissoc % k)))

(defn delete-index
  "Returns a new vector like v but with the value at ix removed."
  [v ix]
  (into [] (concat (subvec v 0 ix)
                   (subvec v (inc ix)))))

(defn move-index-behind
  "Returns a new vector like v but with the value at ix moved back one (decremented)."
  [v ix]
  (let [ix2 (max (dec ix) 0)]
    (-> v
        (assoc ix2 (nth v ix))
        (assoc ix (nth v ix2)))))

(defn move-index-ahead
  "Returns a new vector like v but with the value at ix moved forward one (incremented)."
  [v ix]
  (let [ix2 (min (inc ix) (dec (count v)))]
    (-> v
        (assoc ix2 (nth v ix))
        (assoc ix (nth v ix2)))))

(defn ipairs
  "Given coll, returns a seq of pairs (x, ix) where x is the value of (get coll ix)"
  [coll]
  (map list coll (range)))

(defn try-fn
  "Returns `(apply f args)` if `f` is a function, otherwise returns `nil`."
  [f & args] (when (fn? f) (apply f args)))

(defn pick-max
  "Returns the max of value you get when calling (f x) for each x in xs."
  [f xs]
  (apply max-key f xs))


(defn is-denied?
  "returns true if `deny` contains `v`,
   or if `allow` is non-empty and does not contain `v`"
  [v deny allow]
  (boolean (or (contains? deny v)
               (and (not-empty allow)
                    (not (contains? allow v))))))

(defn callback-factory-factory
  "returns a function which will always return the `same-callback` every time 
   it is called. 
   `same-callback` is what actually calls your `callback` and, when it does,
   it supplies any necessary args, including those supplied at wrapper creation
   time and any supplied by the browser (a DOM event object?) at call time.
   
   (from https://github.com/day8/re-frame/blob/f2d9fadb257d5249f5bbe4a1d7ca850b0847db2b/docs/on-stable-dom-handlers.md)"
  [the-real-callback]
  (let [*args1        (atom nil)
        same-callback (fn [& args2]
                        (apply the-real-callback (concat @*args1 args2)))]
    (fn callback-factory
      [& args1]
      (reset! *args1 args1)
      same-callback)))

(defn clj->json [x] (-> x (clj->js) (js/JSON.stringify)))
(defn json->clj [x] (-> x (js/JSON.parse) (js->clj)))
(defn edn->clj [x] (edn/read-string x))
(defn clj->edn [x] (pr-str x))

(defn as-url [s] (new js/URL s))
(defn fragment [url] (.-hash url))

(defn url->route
  [url]
  (-> url
      (as-url)
      (fragment)
      (subs 1)
      (string/split "/")
      (->> (filter not-empty))
      (->> (json-decode schema/PageRoute))))

(defn route->url
  [route]
  (str "/" (string/join "/" (s/transform [(s/filterer keyword?) s/ALL] name route))))