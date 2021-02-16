(ns factor.world
  (:require [clojure.edn :as edn]))

(def empty-world {:items {} :machines {} :recipes {} :factories {}})

(defn world->str [world] (pr-str world))
(defn str->world [s] (edn/read-string s))
