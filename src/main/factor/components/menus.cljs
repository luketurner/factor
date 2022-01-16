(ns factor.components.menus
  (:require [factor.components.macros :refer-macros [defcomponent]]
            [reagent.core :as reagent :refer [with-let]]
            [re-frame.core :refer [dispatch subscribe]]
            [factor.components.wrappers :refer [popover hotkeys-target button menu menu-divider menu-item]]
            [factor.components.cmd :refer [cmd-menu-item]]))

(defcomponent hotkey-label
  "This component accepts a char prop (which should be a single character) and a string.
   It returns a Hiccup form representing the same string, but with the first instance of the char
   underlined (technically, it has the .hotkey class applied to it). 
   Used for indicating hotkeys in label text."
  [{:keys [char]} [string-content]]
  (if-not key string-content
          (let [[_ head v tail] (re-find (re-pattern (str "(?i)^(.*?)(" char ")(.*?)$")) string-content)]
            [:<> head [:span.hotkey v] tail])))

(defcomponent app-menu
  "Component for an application menu (e.g. File, Edit, etc.) Stores open-menu state in the `app-db`,
   so only one menu can be opened at once, menus can be invoked with global hotkeys, etc.

   The UI design for the button will probably look best in a navbar.
   
   Accepts props:
   
   :key a unique keyword identifying this menu
   :name human-readable menu name (title case)
   :icon Icon to display alongside menu (optional)
   :hotkey Single-character string to be used as the menu's global hotkey.
           e.g. if \"f\", menu can be toggled with alt+f
           will be underlined in name if present (a la Windows menus)"
  [{:keys [key hotkey name icon]} c]
  (with-let [set-menu-open #(dispatch [:set-app-menu (if % [key] [])])]
    (let [[p1] @(subscribe [:app-menu])
          is-open (= p1 key)
          toggle-open (reagent/partial set-menu-open (not is-open))]
      [popover {:is-open is-open
                :on-interaction set-menu-open
                :position "bottom-left"
                :minimal true
                :interaction-kind "click"}
       [hotkeys-target {:hotkeys (if-not hotkey []
                                         [{:combo (str "alt+" hotkey)
                                           :label (str "Open " name " menu")
                                           :onKeyDown toggle-open
                                           :preventDefault true
                                           :stopPropagation true
                                           :global true}])}
        [button {:on-click toggle-open
                 :icon icon
                 :minimal true}
         [hotkey-label {:char hotkey} name]]]
       (into [menu] c)])))


(defn factory-menu []
  [app-menu {:name "Factory" :key :factory :hotkey "f"}
   [cmd-menu-item {:cmd :new-factory}]
   [cmd-menu-item {:cmd :open-factory}]
   [cmd-menu-item {:cmd :delete-factory}]
   [menu-divider]
   [cmd-menu-item {:cmd :toggle-filter-view}]
   [cmd-menu-item {:cmd :toggle-debug-view}]])

(defn edit-menu []
  [app-menu {:name "Edit" :key :edit :hotkey "e"}
   [cmd-menu-item {:cmd :undo}]
   [cmd-menu-item {:cmd :undo-multi}]
   [cmd-menu-item {:cmd :redo}]
   [cmd-menu-item {:cmd :redo-multi}]])

(defn world-menu []
  [app-menu {:name "World" :key :world :hotkey "w"}
   [cmd-menu-item {:cmd :open-item-editor}]
   [cmd-menu-item {:cmd :open-machine-editor}]
   [cmd-menu-item {:cmd :open-recipe-editor}]
   [cmd-menu-item {:cmd :open-import-export}]
   [cmd-menu-item {:cmd :delete-world}]])

(defn settings-menu []
  [app-menu {:name "Settings" :key :settings :hotkey "s"}
   [menu-divider {:title "Units"}]
   [cmd-menu-item {:cmd :change-item-rate-unit}]
   [cmd-menu-item {:cmd :change-energy-unit}]
   [cmd-menu-item {:cmd :change-power-unit}]])

(defn help-menu []
  [app-menu {:name "Help" :key :help :hotkey "h"}
   [cmd-menu-item {:cmd :open-help}]
   [menu-divider {:title "External Links"}]
   [menu-item {:href "https://github.com/luketurner/factor" :target :_blank :text "Github" :icon :git-repo}]
   [menu-item {:href "https://git.sr.ht/~luketurner/factor" :target :_blank :text "Sourcehut" :icon :git-repo}]])

(defn menus []
  [:<>
   [factory-menu]
   [edit-menu]
   [world-menu]
   [settings-menu]
   [help-menu]])