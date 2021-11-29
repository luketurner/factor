(ns factor.navs
  "Defines custom Specter navigators that can be used when writing subs/events/etc."
  (:require [com.rpl.specter :as s :refer [path]]
            [malli.core :as m]
            [factor.schema :as sc]))

; general/utility navigators

; throws an error if the navigated value is changed during a tranfsormation
; and the new value doesn't match the given schema.
; Has no effect on selects.
(s/defnav validate [schema]
 (select* [_ x next-fn] (next-fn x))
 (transform* [_ x next-fn]
             (let [next-x (next-fn x)]
               (if (or (= next-x x)
                       (m/validate schema next-x))
                 next-x
                 (throw (pr-str (m/explain schema next-x)))))))

; navigators on DB

(def WORLD (path :world))
(def CONFIG (path :config))
(def UI (path :ui))

(def VALID-CONFIG (path CONFIG (validate sc/Config)))
(def VALID-WORLD (path WORLD (validate sc/World)))

; shorthand

(def W (path WORLD))

; navigators on WORLD

(def FACTORIES (path :factories))
(def MACHINES (path :machines))
(def RECIPES (path :recipes))
(def ITEMS (path :items))

(def MAP-FACTORIES (path FACTORIES s/MAP-VALS))
(def MAP-RECIPES (path RECIPES s/MAP-VALS))
(def MAP-ITEMS (path ITEMS s/MAP-VALS))
(def MAP-MACHINES (path MACHINES s/MAP-VALS))

(def MAP-FACTORY-IDS (path FACTORIES s/MAP-KEYS))
(def MAP-RECIPE-IDS (path RECIPES s/MAP-KEYS))
(def MAP-ITEM-IDS (path ITEMS s/MAP-KEYS))
(def MAP-MACHINE-IDS (path MACHINES s/MAP-KEYS))

(defn map-factories [ids] (path FACTORIES (s/submap ids) s/MAP-VALS))
(defn map-recipes [ids] (path RECIPES (s/submap ids) s/MAP-VALS))
(defn map-items [ids] (path ITEMS (s/submap ids) s/MAP-VALS))
(defn map-machines [ids] (path MACHINES (s/submap ids) s/MAP-VALS))

(defn valid-factory [id] (path FACTORIES id (validate sc/Factory)))
(defn valid-recipe [id] (path RECIPES id (validate sc/Recipe)))
(defn valid-machine [id] (path MACHINES id (validate sc/Machine)))
(defn valid-item [id] (path ITEMS id (validate sc/Item)))

; navigators on factory/recipe/etc.

(def NAME (path :name))
(def DESIRED-OUTPUT (path :desired-output))
(def FILTERS (path :filter))
(def INPUT (path :input))
(def OUTPUT (path :output))
(def CATALYSTS (path :catalysts))
(def DURATION (path :duration))
(def POWER (path :power))
(def SPEED (path :speed))
(def RECIPE-MACHINES (path :machines))

(def FILTERED-RECIPES (path FILTERS
                            (s/multi-path
                             [:hard-allowed-recipes]
                             [:soft-allowed-recipes]
                             [:hard-denied-recipes]
                             [:soft-denied-recipes])
                            s/ALL))
(def FILTERED-ITEMS (path FILTERS
                          (s/multi-path
                           [:hard-allowed-items]
                           [:soft-allowed-items]
                           [:hard-denied-items]
                           [:soft-denied-items])
                          s/ALL))
(def FILTERED-MACHINES (path FILTERS
                             (s/multi-path
                              [:hard-allowed-machines]
                              [:soft-allowed-machines]
                              [:hard-denied-machines]
                              [:soft-denied-machines])
                             s/ALL))

(def FACTORY->ITEMS
  (path
   (s/multi-path
    [DESIRED-OUTPUT s/MAP-KEYS]
    [FILTERED-ITEMS])))

(def FACTORY->RECIPES
  (path [FILTERED-RECIPES]))

(def FACTORY->MACHINES
  (path [FILTERED-MACHINES]))

(def RECIPE->ITEMS
  (path
   (s/multi-path
    [INPUT s/MAP-KEYS]
    [OUTPUT s/MAP-KEYS]
    [CATALYSTS s/MAP-KEYS])))

(def RECIPE->MACHINES
  (path [RECIPE-MACHINES s/ALL]))

; navigators on config

(def OPEN-FACTORY (path :open-factory))
(def UNIT (path :unit))

; navigators on ui

(def SELECTED-PAGE (path :selected-page))
(def SELECTED-OBJECTS (path :selected-objects))