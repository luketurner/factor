(ns factor.components.menus
  (:require [factor.components :as c]))

(defn factory-menu []
  [c/app-menu {:name "Factory" :key :factory :hotkey "f"}
   [c/menu-item-cmd {:cmd :new-factory}]
   [c/menu-item-cmd {:cmd :open-factory}]
   [c/menu-item-cmd {:cmd :delete-factory}]
   [c/menu-divider]
   [c/menu-item-cmd {:cmd :toggle-filter-view}]
   [c/menu-item-cmd {:cmd :toggle-debug-view}]])

(defn edit-menu []
  [c/app-menu {:name "Edit" :key :edit :hotkey "e"}
   [c/menu-item-cmd {:cmd :undo}]
   [c/menu-item-cmd {:cmd :undo-multi}]
   [c/menu-item-cmd {:cmd :redo}]
   [c/menu-item-cmd {:cmd :redo-multi}]])

(defn world-menu []
  [c/app-menu {:name "World" :key :world :hotkey "w"}
   [c/menu-item-cmd {:cmd :open-item-editor}]
   [c/menu-item-cmd {:cmd :open-machine-editor}]
   [c/menu-item-cmd {:cmd :open-recipe-editor}]
   [c/menu-item-cmd {:cmd :open-import-export}]
   [c/menu-item-cmd {:cmd :delete-world}]])

(defn settings-menu []
  [c/app-menu {:name "Settings" :key :settings :hotkey "s"}
   [c/menu-divider {:title "Units"}]
   [c/menu-item-cmd {:cmd :change-item-rate-unit}]
   [c/menu-item-cmd {:cmd :change-energy-unit}]
   [c/menu-item-cmd {:cmd :change-power-unit}]])

(defn help-menu []
  [c/app-menu {:name "Help" :key :help :hotkey "h"}
   [c/menu-item-cmd {:cmd :open-help}]
   [c/menu-divider {:title "External Links"}]
   [c/menu-item {:href "https://github.com/luketurner/factor" :target :_blank :text "Github" :icon :git-repo}]
   [c/menu-item {:href "https://git.sr.ht/~luketurner/factor" :target :_blank :text "Sourcehut" :icon :git-repo}]])

(defn menus []
  [:<>
   [factory-menu]
   [edit-menu]
   [world-menu]
   [settings-menu]
   [help-menu]])