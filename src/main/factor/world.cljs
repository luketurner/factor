(ns factor.world
  (:require [clojure.edn :as edn]
            [factor.util :refer [new-uuid add-fx]]
            [re-frame.core :refer [->interceptor dispatch-sync]]))

(def empty-world {:items {} :machines {} :recipes {} :factories {}})

(defn world->str [world] (pr-str world))
(defn str->world [s] (edn/read-string s))

(defn update-world [db & rest]
  (apply (partial update db :world) rest))

(defn factories [world] (get world :factories))
(defn machines [world] (get world :machines))
(defn recipes [world] (get world :recipes))
(defn items [world] (get world :items))

(defn factory-ids [world] (-> world (factories) (keys)))
(defn machine-ids [world] (-> world (machines) (keys)))
(defn recipe-ids [world] (-> world (recipes) (keys)))
(defn item-ids [world] (-> world (items) (keys)))

(defn with-factory [world id factory] (update world :factories assoc (or id (new-uuid)) factory))
(defn with-machine [world id machine] (update world :machines assoc (or id (new-uuid)) machine))
(defn with-item [world id item] (update world :items assoc (or id (new-uuid)) item))
(defn with-recipe [world id recipe] (update world :recipes assoc (or id (new-uuid)) recipe))

(defn without-factory [world id] (update world :factories dissoc id))
(defn without-machine [world id] (update world :machines dissoc id))
(defn without-item [world id] (update world :items dissoc id))
(defn without-recipe [world id] (update world :recipes dissoc id))

(defn ->saver []
  (->interceptor
   :id :world-saver
   :after (fn [{{{old-world :world} :db} :coeffects
                {{new-world :world} :db} :effects :as context}]
            (if (and new-world (not= new-world old-world))
              (add-fx context [:dispatch [:save-world new-world]])
              context))))
