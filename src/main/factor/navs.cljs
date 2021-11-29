(ns factor.navs
  "Defines custom Specter navigators that can be used when writing subs/events/etc.
   
   Navigator follow specific naming conventions:
   
   - Should be a noun indicating the thing being navigated to.
   - If singular (e.g. CONFIG), the navigator should navigate to a single value
     - Usually, if terminating a path with a singular navigator, you should use select-any instead of select
   - If plural (e.g. FACTORIES), the navigator should navigate to multiple (0, 1, or many) values
     - Such navigators DO NOT need an ALL after them to unpack the values
   - If navigating TO a data structure like a vec/map/etc that is added to the end of the navigator
     - e.g. FACTORY-MAP navigates to a map of factories, MACHINE-LIST navigates to a list of machines.
     - Other prefixes: QM for a quantitymap
   
   Navigators with a singular name (e.g. CONFIG) navigate to a singular value.
   Navigators with a plural name (e.g. FACTORIES) navigate to multiple values."
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

(def CONFIG (path :config))
(def WORLD  (path :world))
(def UI     (path :ui))

(def VALID-CONFIG (path CONFIG (validate sc/Config)))
(def VALID-WORLD  (path WORLD (validate sc/World)))
(def VALID-UI     (path UI (validate sc/Ui)))

; shorthand

(def W (path WORLD))

; navigators on WORLD

(def FACTORIES-MAP (path :factories))
(def MACHINES-MAP  (path :machines))
(def RECIPES-MAP   (path :recipes))
(def ITEMS-MAP     (path :items))

(def FACTORIES (path FACTORIES-MAP s/MAP-VALS))
(def RECIPES   (path RECIPES-MAP s/MAP-VALS))
(def ITEMS     (path ITEMS-MAP s/MAP-VALS))
(def MACHINES  (path MACHINES-MAP s/MAP-VALS))

(def FACTORY-IDS (path FACTORIES-MAP s/MAP-KEYS))
(def RECIPE-IDS  (path RECIPES-MAP s/MAP-KEYS))
(def ITEM-IDS    (path ITEMS-MAP s/MAP-KEYS))
(def MACHINE-IDS (path MACHINES-MAP s/MAP-KEYS))

(defn factories [ids] (path FACTORIES-MAP (s/submap ids) s/MAP-VALS))
(defn recipes [ids]   (path RECIPES-MAP   (s/submap ids) s/MAP-VALS))
(defn items [ids]     (path ITEMS-MAP     (s/submap ids) s/MAP-VALS))
(defn machines [ids]  (path MACHINES-MAP  (s/submap ids) s/MAP-VALS))

(defn valid-factory [id] (path FACTORIES-MAP id (validate sc/Factory)))
(defn valid-recipe [id]  (path RECIPES-MAP   id (validate sc/Recipe)))
(defn valid-machine [id] (path MACHINES-MAP  id (validate sc/Machine)))
(defn valid-item [id]    (path ITEMS-MAP     id (validate sc/Item)))

; navigators on factory/recipe/etc.

(def ID                   (path :id))
(def NAME                 (path :name))
(def DESIRED-OUTPUT-QM    (path :desired-output))
(def FILTER               (path :filter))
(def INPUT-QM             (path :input))
(def OUTPUT-QM            (path :output))
(def CATALYSTS-QM         (path :catalysts))
(def DURATION             (path :duration))
(def POWER                (path :power))
(def SPEED                (path :speed))
(def RECIPE-MACHINE-LIST  (path :machines))

(def DESIRED-OUTPUT-ITEMS (path DESIRED-OUTPUT-QM   s/MAP-KEYS))
(def INPUT-ITEMS          (path INPUT-QM            s/MAP-KEYS))
(def OUTPUT-ITEMS         (path OUTPUT-QM           s/MAP-KEYS))
(def CATALYSTS-ITEMS      (path CATALYSTS-QM        s/MAP-KEYS))
(def RECIPE-MACHINES      (path RECIPE-MACHINE-LIST s/ALL))

(def FILTERED-RECIPES  (path (s/multi-path :hard-allowed-recipes  :soft-allowed-recipes  :hard-denied-recipes  :soft-denied-recipes)  s/ALL))
(def FILTERED-ITEMS    (path (s/multi-path :hard-allowed-items    :soft-allowed-items    :hard-denied-items    :soft-denied-items)    s/ALL))
(def FILTERED-MACHINES (path (s/multi-path :hard-allowed-machines :soft-allowed-machines :hard-denied-machines :soft-denied-machines) s/ALL))

(def FACTORY->ITEMS    (path (s/multi-path DESIRED-OUTPUT-ITEMS [FILTER FILTERED-ITEMS])))
(def FACTORY->RECIPES  (path FILTER FILTERED-RECIPES))
(def FACTORY->MACHINES (path FILTER FILTERED-MACHINES))
(def RECIPE->ITEMS     (path (s/multi-path INPUT-ITEMS OUTPUT-ITEMS CATALYSTS-ITEMS)))
(def RECIPE->MACHINES  (path RECIPE-MACHINES))

; navigators on config

(def OPEN-FACTORY (path :open-factory))
(def UNIT         (path :unit))

; navigators on ui

(def SELECTED-PAGE        (path :selected-page))
(def SELECTED-OBJECT-LIST (path :selected-objects))
(def SELECTED-OBJECTS     (path SELECTED-OBJECT-LIST s/ALL))