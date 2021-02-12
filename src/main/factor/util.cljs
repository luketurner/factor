(ns factor.util
  (:require ["uuid" :as uuid]))

(defn new-uuid [] (uuid/v4))
