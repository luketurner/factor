(ns factor.view.home
  (:require [reagent.core :refer [as-element]]
            [factor.components.inputs :refer [nav-link]]
            [factor.components.wrappers :refer [non-ideal-state]]))

(defn page []
  [non-ideal-state
   {:icon :join-table
    :title "Factor: A ticket out of Spreadsheet Hell?"
    :description (as-element [:<>
                              [:p "Ever made a " [:strong "spreadsheet"] " to calculate optimal rates and ratios for your builds
                                   in games like Factorio or Satisfactory? Did it get annoyingly complicated?"]
                              [:p [:strong "Factor"] " is a Web app designed to replace that workflow!"]
                              [:p
                               "Because Factor is game-agnostic, first you need to configure the items, machines, and recipes available in your particular game/modpack/etc.:"]
                              [:p
                               [nav-link [:items] :cube "Items"]
                               [nav-link [:machines] :oil-field "Machines"]
                               [nav-link [:recipes] :data-lineage "Recipes"]]
                              [:p
                               "Then, just create a factory and specify what items it should produce. Factor will calculate a "
                               "\"production graph\" for the factory, telling you all the recipes, inputs and outputs, machines, etc. that it requires:"]
                              [:p [nav-link [:factories] :office "Factories"]]
                              [:p
                               "Everything's stored in your browser's local storage. Use the Settings page to import, export, or reset your Factor data, or to change Factor-wide settings."]
                              [:p [nav-link [:settings] :settings "Settings"]]])
    ;; :action (as-element [c/button {:intent :success :text "Use Demo Data"}])
    }])