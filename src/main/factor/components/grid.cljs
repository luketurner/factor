(ns factor.components.grid
  (:require [factor.components.macros :refer-macros [defcomponent]]
            [factor.components.wrappers :refer [ag-grid]]
            [factor.util :refer [try-fn]]
            [medley.core :refer [map-keys]]))

(defn grid-cb->clj
  "Accepts an ag-grid style event (as would be passed to a grid callback) and returns a Clojureified version.
   Note the :api part of the event is not Clojurified because it's mostly methods."
  [ev]
  {:api (.-api ev)
   :data (-> ev (.-data) (js->clj) (->> (map-keys keyword)))})

(defn grid-cb
  "Accepts a callback function `cb` and returns a function suitable for use as an ag-grid event handler."
  [cb]
  (fn [ev]
    (->> ev (grid-cb->clj) (try-fn cb))))

(defn grid-value-parser-for-floats
  [ev]
  (js/parseFloat (.-newValue ev)))

(defcomponent grid
  "Wrapper around ag-grid's Grid component. Sets a variety of default props for DRY-ness. All defaults can be
   overriden with `props`. Note that usually you want to specify at least the :on-row-value-changed prop. The
   `children` can contain column definitions, but it's usually simpler to use the :column-defs option in `props`."
  [props children]
  (let [default-props  {:row-selection :multiple
                        :enter-moves-down true
                        :enter-moves-down-after-edit true
                        :edit-type "fullRow"
                        :default-col-def {:sortable true}}
        override-props {:on-row-value-changed (grid-cb (:on-row-value-changed props))
                        :on-grid-ready        (grid-cb (:on-grid-ready        props))
                        :on-selection-changed (grid-cb (:on-selection-changed props))}
        grid-props     (merge default-props props override-props)]
    [:div.ag-theme-alpine.full-screen
     (into [ag-grid grid-props] children)]))