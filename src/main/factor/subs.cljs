(ns factor.subs
  "Registers all of Factor's re-frame subscriptions when the (reg-all) function is called.
   reg-all should be called before rendering the app, and called again after hot-reloading."
  (:require [re-frame.core :refer [reg-sub reg-sub-raw subscribe]]
            [reagent.ratom :refer [reaction]]
            [factor.pgraph :as pgraph]
            [factor.util :refer [clj->json clj->edn ipairs]]
            [com.rpl.specter :as s :refer [select select-any transform setval]]
            [factor.navs :as nav]
            [factor.schema :refer [json-encode World edn-encode]]
            [factor.cmds :as cmds]))


(defn reg-all []

  ;; Extractors

  (reg-sub :world  (fn [db _] (s/select-any nav/WORLD  db)))
  (reg-sub :ui     (fn [db _] (s/select-any nav/UI     db)))
  (reg-sub :config (fn [db _] (s/select-any nav/CONFIG db)))

  ;; Materialized views

  (reg-sub :selected-objects  :<- [:ui] (fn [ui] (select     [nav/SELECTED-OBJECTS]  ui)))
  (reg-sub :page-route        :<- [:ui] (fn [ui] (select-any [nav/PAGE-ROUTE]     ui)))
  (reg-sub :open-factory-id   :<- [:page-route]      (fn [[x1 x2]] (when (= x1 :factory) x2)))
  (reg-sub :open-factory?     :<- [:open-factory-id] (fn [v] (some? v)))

  (reg-sub :app-menu :<- [:ui] (fn [ui] (select-any [nav/APP-MENU] ui)))

  (reg-sub :omnibar-state :<- [:ui] (fn [ui] (select-any [nav/OMNIBAR-STATE] ui)))
  (reg-sub :omnibar-mode  :<- [:omnibar-state] (fn [v] (select-any [nav/MODE] v)))
  (reg-sub :cmd-invocation :<- [:omnibar-state] (fn [v] (select-any [nav/INVOCATION] v)))

  (reg-sub :units :<- [:config] (fn [config]      (select-any [nav/UNIT] config)))
  (reg-sub :item-rate-unit :<- [:units] (fn [config] (select-any :item-rate config)))
  (reg-sub :energy-unit    :<- [:units] (fn [config] (select-any :energy    config)))
  (reg-sub :power-unit     :<- [:units] (fn [config] (select-any :power     config)))
  (reg-sub :unit  :<- [:units]  (fn [units [_ u]] (select-any u          units)))

  (reg-sub :factories :<- [:world] (fn [w] (select-any nav/FACTORIES-MAP w)))
  (reg-sub :items     :<- [:world] (fn [w] (select-any nav/ITEMS-MAP     w)))
  (reg-sub :machines  :<- [:world] (fn [w] (select-any nav/MACHINES-MAP  w)))
  (reg-sub :recipes   :<- [:world] (fn [w] (select-any nav/RECIPES-MAP   w)))

  (reg-sub :any-factories? :<- [:factories] (fn [xs] (not-empty xs)))
  (reg-sub :any-items?     :<- [:items]     (fn [xs] (not-empty xs)))
  (reg-sub :any-recipes?   :<- [:recipes]   (fn [xs] (not-empty xs)))
  (reg-sub :any-machines?  :<- [:machines]  (fn [xs] (not-empty xs)))

  (reg-sub :factory :<- [:factories] (fn [xs [_ id]] (select-any id xs)))
  (reg-sub :machine :<- [:machines]  (fn [xs [_ id]] (select-any id xs)))
  (reg-sub :item    :<- [:items]     (fn [xs [_ id]] (select-any id xs)))
  (reg-sub :recipe  :<- [:recipes]   (fn [xs [_ id]] (select-any id xs)))

  (reg-sub :world-as-json :<- [:world] (fn [w] (->> w (json-encode World) (clj->json))))
  (reg-sub :world-as-edn  :<- [:world] (fn [w] (->> w (edn-encode  World) (clj->edn))))

  (reg-sub :factory-exists?        (fn [[_ id]] (subscribe [:factory id])) (fn [x] (some? x)))
  (reg-sub :factory-name           (fn [[_ id]] (subscribe [:factory id])) (fn [x] (select-any nav/NAME x)))
  (reg-sub :factory-desired-output (fn [[_ id]] (subscribe [:factory id])) (fn [x] (select-any nav/DESIRED-OUTPUT-QM x)))
  (reg-sub :factory-filters        (fn [[_ id]] (subscribe [:factory id])) (fn [x] (select-any nav/FILTER x)))

  (reg-sub :factory-filter         (fn [[_ id _]] (subscribe [:factory-filters id])) (fn [x [_ _ k]] (select-any k x)))

  (reg-sub :recipe-input     (fn [[_ id]] (subscribe [:recipe id])) (fn [x] (select-any nav/INPUT-QM            x)))
  (reg-sub :recipe-output    (fn [[_ id]] (subscribe [:recipe id])) (fn [x] (select-any nav/OUTPUT-QM           x)))
  (reg-sub :recipe-catalysts (fn [[_ id]] (subscribe [:recipe id])) (fn [x] (select-any nav/CATALYSTS-QM        x)))
  (reg-sub :recipe-machines  (fn [[_ id]] (subscribe [:recipe id])) (fn [x] (select-any nav/RECIPE-MACHINE-LIST x)))
  (reg-sub :recipe-duration  (fn [[_ id]] (subscribe [:recipe id])) (fn [x] (select-any nav/DURATION            x)))

  (reg-sub :machine-seq :<- [:machines]  (fn [m] (select s/MAP-VALS m)))
  (reg-sub :factory-seq :<- [:factories] (fn [m] (select s/MAP-VALS m)))
  (reg-sub :recipe-seq  :<- [:recipes]   (fn [m] (select s/MAP-VALS m)))
  (reg-sub :item-seq    :<- [:items]     (fn [m] (select s/MAP-VALS m)))

  (reg-sub :machine-ids :<- [:machines]  (fn [m] (select s/MAP-KEYS m)))
  (reg-sub :factory-ids :<- [:factories] (fn [m] (select s/MAP-KEYS m)))
  (reg-sub :recipe-ids  :<- [:recipes]   (fn [m] (select s/MAP-KEYS m)))
  (reg-sub :item-ids    :<- [:items]     (fn [m] (select s/MAP-KEYS m)))

  (reg-sub :factory-id-set :<- [:factory-ids] (fn [xs] (into #{} xs)))
  (reg-sub :machine-id-set :<- [:machine-ids] (fn [xs] (into #{} xs)))
  (reg-sub :recipe-id-set  :<- [:recipe-ids]  (fn [xs] (into #{} xs)))
  (reg-sub :item-id-set    :<- [:item-ids]    (fn [xs] (into #{} xs)))

  (reg-sub :item-ids->names    :<- [:items]     (fn [m] (transform [s/MAP-VALS] #(select-any nav/NAME %) m)))
  (reg-sub :recipe-ids->names  :<- [:recipes]   (fn [m] (transform [s/MAP-VALS] #(select-any nav/NAME %) m)))
  (reg-sub :machine-ids->names :<- [:machines]  (fn [m] (transform [s/MAP-VALS] #(select-any nav/NAME %) m)))
  (reg-sub :factory-ids->names :<- [:factories] (fn [m] (transform [s/MAP-VALS] #(select-any nav/NAME %) m)))

  (reg-sub :item-count    :<- [:item-seq]    (fn [m] (count m)))
  (reg-sub :recipe-count  :<- [:recipe-seq]  (fn [m] (count m)))
  (reg-sub :factory-count :<- [:factory-seq] (fn [m] (count m)))
  (reg-sub :machine-count :<- [:machine-seq] (fn [m] (count m)))

  (reg-sub :recipe-index        :<- [:recipe-seq]   (fn [xs] (pgraph/recipe-index xs)))
  (reg-sub :recipe-input-index  :<- [:recipe-index] (fn [m] (select-any :input m)))
  (reg-sub :recipe-output-index :<- [:recipe-index] (fn [m] (select-any :output m)))

  ;; COMMAND SUBS

  (reg-sub-raw :command-choices
               (fn []
                 (reaction
                  (map (fn [{:keys [id disabled-sub] nm :name}]
                         {:key (name id) :name nm :value id :disabled (and disabled-sub @(subscribe disabled-sub))})
                       (cmds/all-cmds)))))

  (reg-sub :open-factory-disabled?   :<- [:any-factories?] not)
  (reg-sub :delete-factory-disabled? :<- [:any-factories?] not)

  (reg-sub :undo-disabled? :<- [:undos?] not)
  (reg-sub :redo-disabled? :<- [:redos?] not)
  (reg-sub :delete-selected-items-disabled? :<- [:selected-objects] not-empty)
  (reg-sub :delete-selected-recipes-disabled? :<- [:selected-objects] not-empty)
  (reg-sub :delete-selected-machines-disabled? :<- [:selected-objects] not-empty)

  (reg-sub :undo-choices :<- [:undo-explanations] #(map (fn [[s ix]] {:key (str ix) :name s :value ix}) (ipairs %)))
  (reg-sub :redo-choices :<- [:redo-explanations] #(map (fn [[s ix]] {:key (str ix) :name s :value ix}) (ipairs %)))

  (reg-sub :delete-open-factory-disabled? :<- [:open-factory?] not)
  (reg-sub :toggle-filter-view-disabled? :<- [:open-factory?] not)
  (reg-sub :toggle-debug-view-disabled? :<- [:open-factory?] not)

  (reg-sub :factory-choices :<- [:factory-ids->names] (fn [m] (map (fn [[id nm]] {:key id :name nm :value id}) m)))

  (reg-sub :open-factory-choices
           :<- [:factory-choices]
           :<- [:open-factory-id]
           (fn [[choices id]] (setval [(s/filterer :key (s/pred= id)) s/ALL :disabled] true choices)))
  
  (reg-sub :delete-factory-choices :<- [:factory-choices] identity)
  (reg-sub :item-rate-unit-choices :<- [:item-rate-unit]
           (fn [x]
             [{:key :items/sec :name "items/sec" :value "items/sec" :disabled (= x "items/sec")}
              {:key :items/min :name "items/min" :value "items/min" :disabled (= x "items/min")}]))
  
  (reg-sub :power-unit-choices  :<- [:power-unit]  (fn [x] [{:key :W :name "W" :value "W" :disabled (= x "W")}]))
  (reg-sub :energy-unit-choices :<- [:energy-unit] (fn [x] [{:key :J :name "J" :value "J" :disabled (= x "J")}]))

  ;; PGRAPH SUBS

  ; These subs exclude fields that don't matter for pgraph processing.
  ; This way, if e.g. a factory name changes, the whole pgraph doesn't recalculate.
  (reg-sub :factory-for-pgraph (fn [[_ id]] (subscribe [:factory id])) (fn [x] (setval [(s/multi-path nav/NAME nav/ID)] s/NONE x)))
  (reg-sub :machine-for-pgraph (fn [[_ id]] (subscribe [:machine id])) (fn [x] (setval nav/NAME s/NONE x)))
  (reg-sub :recipe-for-pgraph  (fn [[_ id]] (subscribe [:recipe id]))  (fn [x] (setval nav/NAME s/NONE x)))


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

