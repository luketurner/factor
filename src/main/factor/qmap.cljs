(ns factor.qmap
  "Defines functions for working with \"qmaps\" (short for \"quantity maps\").
   
   A qmap is a map where the keys can be anything, and the values are positive numbers.
   Qmaps are used throughout the Factor codebase to represent quantified sets of items,
   e.g. the required inputs and outputs for recipes."
  (:refer-clojure :exclude [+ - *])
  (:require [medley.core :refer [map-vals filter-vals]]
            [com.rpl.specter :as s]
            [clojure.set :as set]
            [clojure.string :as string]))

(defn trim
  "Trims any zero or null quantities from the qmap."
  [qm]
  (filter-vals pos? qm))

(defn +
  "Returns a qmap representing the sum of the quantities in the provided qmaps."
  [& qms]
  (apply merge-with clojure.core/+ qms))

(defn -
  "Returns a qmap representing the difference of the qmap QM and one or more qmaps QMS.
   If a quantity ends up negative, the quantity is removed from the map."
  [qm & qms]
  (-> qm
      (+ (map-vals clojure.core/- (apply + qms)))
      (trim)))

(defn *
  "Multiplies all the values in the qmap by number N. (If ratio is zero or negative, all values will be removed from the qmap.)"
  [qm n]
  (trim (map-vals (partial clojure.core/* n) qm)))

(defn intersects?
  "Returns true if any of the keys in A are also present in B."
  [a b]
  (boolean (not-empty (set/intersection (set (keys a)) (set (keys b))))))

(defn intersection
  "Accepts two or more qmaps and returns their intersection.
   The intersection is defined similarly to that of a set --
   it only contains keys if the keys are present in both qmaps.
   For each key, the value will be set to the minimum of all the values of that key."
  ([a b]
   (trim (into {} (s/select [(s/submap (keys b)) s/ALL (s/view (fn [[k v]] [k (min v (b k))]))] a)))))

(defn satisfying-ratio
  "Given two qmaps GOAL and BASE, returns an integer N such that for every key in
   both GOAL and BASE, quantity(GOAL) <= N * quantity(BASE)"
  [goal base]
  (js/Math.ceil (apply max (for [[k v] base]
                             (when (contains? goal k)
                               (/ (goal k) v))))))

(defn qmap->str
  [qm name-fn delimiter]
  (string/join delimiter (map (fn [[x n]] (str n "x " (name-fn x))) qm)))