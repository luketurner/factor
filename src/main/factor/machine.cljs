(ns factor.machine
  (:require [medley.core :refer [filter-vals map-vals dissoc-in]]))

(defn machine [] {:name ""})

(defn factory-references-machine? [factory machine-id]
  (some #(contains? (% factory) machine-id) [:machines]))

(defn factories-with-machine [factories machine-id]
  (filter-vals factories #(factory-references-machine? % machine-id)))

(defn update-factories-for-machine [factories machine-id]
  (map-vals (factories-with-machine factories machine-id)
            #(-> %
                 (dissoc-in [:machines machine-id]))))



