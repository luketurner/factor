(ns factor.view.home
  (:require [reagent.core :refer [as-element]]
            [factor.components.inputs :refer [nav-link]]
            [factor.components.wrappers :refer [non-ideal-state]]
            [factor.components.cmd :refer [cmd-btn]]))

(defn page []
  [non-ideal-state
   {:icon :join-table
    :title "Factor: A ticket out of Spreadsheet Hell?"
    :description (as-element [:<>
                              [:p "Ever made a " [:strong "spreadsheet"] " to calculate optimal rates and ratios for your builds
                                   in games like Factorio or Satisfactory? Did it get annoyingly complicated?"]
                              [:p [:strong "Factor"] " is a Web app designed to replace that workflow!"]
                              [:h4 "Quickstart"]
                              [:p
                               "Because Factor is game-agnostic, first you need to configure the items, machines, and recipes available in your particular game/modpack/etc. using the " [:strong [:code "World"]] " menu."]
                              [:p
                               "Then, do " [:strong [:code "Factory -> New factory"]] " and add the items you want to produce to your factory's desired outputs. Factor will calculate a "
                               "\"production graph\" for the factory, telling you all the recipes, inputs and outputs, machines, etc. that it requires:"]
                              [:p
                               "Everything's stored in your browser's local storage. Use the " [:strong [:code "World -> Import/export world"]] " command page to import, export, or reset your Factor data."]
                              [:p "Also, you can use the command palette (shortcut " [:strong [:code "/"]] ") to launch any command with the keyboard!"]
                              [:p "For more information, visit the Help page (WIP): " [cmd-btn {:cmd :open-help :minimal true}]]])
    ;; :action (as-element [c/button {:intent :success :text "Use Demo Data"}])
    }])