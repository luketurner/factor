(ns factor.subs
  (:require [re-frame.core :refer [reg-sub reg-sub-raw subscribe]]
            [reagent.ratom :refer [reaction]]
            [medley.core :refer [map-vals]]
            [factor.world :as world]
            [factor.pgraph :as pgraph]))

(defn reg-all []

  ;; Extractors

  ; only use this if you really need the whole world (e.g. exporting/saving)
  (reg-sub :world-data (fn [db _] (get db :world)))

  (reg-sub :factory (fn [db [_ id]] (get-in db [:world :factories id])))
  (reg-sub :item (fn [db [_ id]] (get-in db [:world :items id])))
  (reg-sub :machine (fn [db [_ id]] (get-in db [:world :machines id])))
  (reg-sub :recipe (fn [db [_ id]] (get-in db [:world :recipes id])))

  (reg-sub :factories (fn [db] (get-in db [:world :factories])))
  (reg-sub :items (fn [db] (get-in db [:world :items])))
  (reg-sub :machines (fn [db] (get-in db [:world :machines])))
  (reg-sub :recipes (fn [db] (get-in db [:world :recipes])))

  (reg-sub :ui (fn [db [_ path]] (get-in db (into [:ui] path))))

  (reg-sub :config (fn [db] (get db :config)))

  ;; Materialized views

  (reg-sub :open-factory
           (fn [] [(subscribe [:config]) (subscribe [:factory-id-set])])
           (fn [[{:keys [open-factory]} ids]] (when (contains? ids open-factory) open-factory)))
  (reg-sub :unit         :<- [:config] (fn [c [_ k]] (get-in c [:unit k])))

  (reg-sub :factory-name (fn [[_ id]] (subscribe [:factory id])) (fn [x] (get x :name)))
  (reg-sub :factory-desired-output (fn [[_ id]] (subscribe [:factory id])) (fn [x] (get x :desired-output)))

  (reg-sub :recipe-input (fn [[_ id]] (subscribe [:recipe id])) (fn [x] (get x :input)))
  (reg-sub :recipe-output (fn [[_ id]] (subscribe [:recipe id])) (fn [x] (get x :output)))
  (reg-sub :recipe-catalysts (fn [[_ id]] (subscribe [:recipe id])) (fn [x] (get x :catalysts)))
  (reg-sub :recipe-machines (fn [[_ id]] (subscribe [:recipe id])) (fn [x] (get x :machines)))
  (reg-sub :recipe-duration (fn [[_ id]] (subscribe [:recipe id])) (fn [x] (get x :duration)))


  (reg-sub :machine-seq :<- [:machines] (fn [m] (into [] (vals m))))
  (reg-sub :factory-seq :<- [:factories] (fn [m] (into [] (vals m))))
  (reg-sub :recipe-seq :<- [:recipes] (fn [m] (into [] (vals m))))
  (reg-sub :item-seq :<- [:items] (fn [m] (into [] (vals m))))

  (reg-sub :machine-ids :<- [:machines] (fn [m] (into [] (keys m))))
  (reg-sub :factory-ids :<- [:factories] (fn [m] (into [] (keys m))))
  (reg-sub :recipe-ids :<- [:recipes] (fn [m] (into [] (keys m))))
  (reg-sub :item-ids :<- [:items] (fn [m] (into [] (keys m))))

  (reg-sub :factory-id-set :<- [:factory-ids] (fn [xs] (into #{} xs)))

  (reg-sub :item-ids->names :<- [:items] (fn [m] (map-vals :name m)))
  (reg-sub :recipe-ids->names :<- [:recipes] (fn [m] (map-vals :name m)))
  (reg-sub :machine-ids->names :<- [:machines] (fn [m] (map-vals :name m)))
  (reg-sub :factory-ids->names :<- [:factories] (fn [m] (map-vals :name m)))

  (reg-sub :item-count :<- [:item-seq] (fn [m] (count m)))
  (reg-sub :recipe-count :<- [:recipe-seq] (fn [m] (count m)))
  (reg-sub :factory-count :<- [:factory-seq] (fn [m] (count m)))
  (reg-sub :machine-count :<- [:machine-seq] (fn [m] (count m)))

  (reg-sub :recipe-index :<- [:recipe-seq] (fn [xs] (world/recipe-index xs)))
  (reg-sub :recipe-input-index :<- [:recipe-index] (fn [m] (get m :input)))
  (reg-sub :recipe-output-index :<- [:recipe-index] (fn [m] (get m :output)))

  ; These subs exclude fields that don't matter for pgraph processing.
  ; This way, if e.g. a factory name changes, the whole pgraph doesn't recalculate.
  (reg-sub :factory-for-pgraph (fn [[_ id]] (subscribe [:factory id])) (fn [x] (dissoc x :name :id)))
  (reg-sub :machine-for-pgraph (fn [[_ id]] (subscribe [:machine id])) (fn [x] (dissoc x :name)))
  (reg-sub :recipe-for-pgraph (fn [[_ id]] (subscribe [:recipe id])) (fn [x] (dissoc x :name)))

  ; The :recipes-with-output sub accepts an item ID as a parameter, uses the recipe-index to
  ; look up all recipes that provide that item as output, and returns a set of recipe objects (NOT ids)
  ; Will re-run whenever the recipe index changes or any of the recipe objects change.
  (reg-sub-raw
   :recipes-with-output
   (fn [_ [_ id]]
     (reaction
      (let [recipe-output-index @(subscribe [:recipe-output-index])
            recipe-ids (get recipe-output-index id #{})
            recipes (map (fn [r] @(subscribe [:recipe-for-pgraph r])) recipe-ids)]
        recipes))))

  ; Calculates a pgraph for the given factory. Uses reg-sub-raw because it only needs to re-run in specific cases:
  ; 1. A machine or recipe referenced by a node/candidate in the pgraph was updated.
  ; 2. Any recipes were added/removed from the recipe index nodes that were checked when the pgraph was built.
  ; By limiting our subscriptions to only those required machines/recipes/etc. we can avoid having to recalculate
  ; the expensive pgraph when unused recipes/etc. are changed.
  (reg-sub-raw :factory-pgraph
               (fn [_ [_ factory-id]]
                 (reaction
                  (let [factory @(subscribe [:factory-for-pgraph factory-id])
                        get-recipes-with-output (fn [item] @(subscribe [:recipes-with-output item]))
                        get-machine (fn [id] @(subscribe [:machine-for-pgraph id]))]
                    (pgraph/pgraph {:get-recipes-with-output get-recipes-with-output
                                    :get-machine get-machine
                                    :filter (:filter factory)
                                    :desired-output (:desired-output factory)}))))))

