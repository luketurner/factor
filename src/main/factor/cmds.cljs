(ns factor.cmds
  "Defines all the commands used in Factor. Commands are user-facing actions
   that can be invoked with the command palette, or with their own global hotkeys.
   Depending on the context of the open view, etc. not all commands are available.
   Menu/toolbar buttons also invoke commands.

   Commands are actually signals -- reactions -- that can reference other signals (e.g. DB subscriptions.) So,
   the command's name, disabld status, event, etc. can change dynamically.
   
   The `cmd` helper function automatically derefs the command, so you don't need to remember this
   implementation detail in your views. But if you want un-derefed command, use `cmd-atom`.
   
   Reference the Command schema in the factor.schema namespace for what fields are supported."
  (:require [com.rpl.specter :as s]))

(def all-cmds-raw
  {:open-command-palette {:id :open-command-palette
                          :name "Open command palette"
                          :global-hotkey "/"
                          :params [{:name "Command Palette"
                                    :choice-sub [:command-choices]}]
                          :ev [:cmd-invoke]}

   :new-factory {:id :new-factory
                 :name "New factory"
                 :icon :plus
                 :params [{:name "Name"}]
                 :ev [:new-factory]}

   :open-factory {:id :open-factory
                  :name "Open factory"
                  :disabled-sub [:open-factory-disabled?]
                  :params [{:name "Factory"
                            :choice-sub [:open-factory-choices]}]
                  :ev [:open-factory]}

   :delete-factory {:id :delete-factory
                    :name "Delete factory"
                    :disabled-sub [:delete-factory-disabled?]
                    :params [{:name "Factory"
                              :choice-sub [:delete-factory-choices]}]
                    :ev [:delete-factory]}

   :delete-open-factory {:id :delete-open-factory
                         :name "Delete open factory"
                         :icon :delete
                         :disabled-sub [:delete-open-factory-disabled?]
                         :ev [:delete-open-factory]}

   :undo {:id :undo
          :name "Undo"
          :disabled-sub [:undo-disabled?]
          :icon :undo
          :ev [:undo]}

   :redo {:id :redo
          :name "Redo"
          :icon :redo
          :disabled-sub [:redo-disabled?]
          :ev [:redo]}

   :undo-multi {:id :undo-multi
                :name "Undo multiple"
                :disabled-sub [:undo-disabled?]
                :params [{:name "Undo target"
                          :choice-sub [:undo-choices]}]
                :ev [:undo]}

   :redo-multi {:id :redo-multi
                :name "Redo multiple"
                :disabled-sub [:redo-disabled?]
                :params [{:name "Redo target"
                          :choice-sub [:redo-choices]}]
                :ev [:redo]}

   :open-item-editor {:id :open-item-editor
                      :name "Open item editor"
                      :ev [:open-item-editor]}

   :open-recipe-editor {:id :open-recipe-editor
                        :name "Open recipe editor"
                        :ev [:open-recipe-editor]}

   :open-machine-editor {:id :open-machine-editor
                         :name "Open machine editor"
                         :ev [:open-machine-editor]}

   :open-import-export {:id :open-import-export
                        :name "Import/export world"
                        :ev [:open-import-export]}

   :open-help {:id :open-help
               :name "Open help"
               :ev [:open-help]}

   :delete-world {:id :delete-world
                  :name "Delete world"
                  :ev [:delete-world]}

   :new-item {:id :new-item
              :name "New item"
              :icon :plus
              :ev [:new-item]}

   :new-recipe {:id :new-recipe
                :name "New recipe"
                :icon :plus
                :ev [:new-recipe]}

   :new-machine {:id :new-machine
                 :name "New machine"
                 :icon :plus
                 :ev [:new-machine]}

   :delete-selected-items {:id :delete-selected-items
                           :name "Delete selected item(s)"
                           :icon :delete
                           :disabled-sub [:delete-selected-items-disabled?]
                           :ev [:delete-selected-items]}

   :delete-selected-recipes {:id :delete-selected-recipes
                             :name "Delete selected recipe(s)"
                             :icon :delete
                             :disabled-sub [:delete-selected-recipes-disabled?]
                             :ev [:delete-selected-recipes]}

   :delete-selected-machines {:id :delete-selected-recipes
                              :name "Delete selected machine(s)"
                              :icon :delete
                              :disabled-sub [:delete-selected-machines-disabled?]
                              :ev [:delete-selected-machines]}

   :change-item-rate-unit {:id :change-item-rate-unit
                           :name "Item rate unit"
                           :params [{:name "Unit"
                                     :choice-sub [:item-rate-unit-choices]}]
                           :ev [:change-item-rate-unit]}

   :change-energy-unit {:id :change-energy-unit
                        :name "Energy unit"
                        :params [{:name "Unit"
                                  :choice-sub [:energy-unit-choices]}]
                        :ev [:change-energy-unit]}

   :change-power-unit {:id :change-power-unit
                       :name "Power unit"
                       :params [{:name "Unit"
                                 :choice-sub [:power-unit-choices]}]
                       :ev [:change-power-unit]}

   :toggle-filter-view {:id :toggle-filter-view
                        :name "Toggle filter view"
                        :disabled-sub [:toggle-filter-view-disabled?]
                        :ev [:toggle-filter-view]}

   :toggle-debug-view {:id :toggle-debug-view
                       :name "Toggle debug view"
                       :disabled-sub [:toggle-debug-view-disabled?]
                       :ev [:toggle-debug-view]}})

(defn cmd [kw] (get all-cmds-raw kw))

(defn all-cmds [] (vals all-cmds-raw))
(defn all-cmd-ids [] (keys all-cmds-raw))