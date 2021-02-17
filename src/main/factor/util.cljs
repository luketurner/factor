(ns factor.util)

(defn new-uuid []
  (str (random-uuid)))