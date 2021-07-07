(ns factor.view.home
  (:require [factor.components :as c]
            [reagent.core :refer [as-element]]))

(defn navbar [] [c/navbar])

(defn page []
  [c/non-ideal-state
   {:icon :home
    :title "Welcome"
    :description (as-element [:<>
                              [:p "Ever made a spreadsheet to calculate optimal rates and ratios for your builds
                                   in games like Factorio? "]
                              [:p [:strong "factor."] " is a Web app designed to replace that workflow."]
                              [:p
                               "Because Factor is game-agnostic, first you need to configure the "
                               [:strong [c/icon {:icon :cube}] " Items"] ", "
                               [:strong [c/icon {:icon :oil-field}] " Machines"] ", and "
                               [:strong [c/icon {:icon :data-lineage}] " Recipes"]
                               " available in your particular game/modpack/etc."]
                              [:p
                               "Then, create a " [:strong [c/icon {:icon :office}] " Factory"] " and specify its desired output. Factor will calculate a "
                               "\"production graph\" for the factory, and tell you all the recipes, inputs and outputs, machines, etc. that it requires."]
                              [:p
                               "Everything's stored in your browser's local storage. Use the "
                               [:strong [c/icon {:icon :settings}] " Settings"]
                               " page to import, export, or reset your Factor data, or to change Factor-wide settings."]])
    :action (as-element [c/button {:intent :success :text "Use Demo Data"}])}])