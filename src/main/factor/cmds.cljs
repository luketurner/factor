(ns factor.cmds
  "Defines all the commands used in Factor. Commands are user-facing actions
   that can be invoked with the command palette, or with their own global hotkeys.
   Depending on the context of the open view, etc. not all commands are available.
   Menu/toolbar buttons also invoke commands.
   
   When a command is invoked, a corresponding event is dispatched.")

(def all-cmds
  {:open-command-palette {:name "Open command palette"
                          :global-hotkey "/"
                          :ev [:open-command-palette]}
   :new-factory {:name "New factory"
                 :hotkey "f"
                 :ev [:create-factory-command]}
   :open-factory {:name "Open factory"
                  :hotkey "o"
                  :ev [:omnibar-open-factory]}
   :delete-factory {:name "Delete factory"
                    :hotkey "d"
                    :ev [:delete-factory]}
   :edit-filters {:name "Edit filters"
                  :hotkey "F"
                  :ev [:edit-filters]}
   :undo-one {:name "Undo last action"
              :hotkey "u"
              :ev [:undo]}
   :redo-one {:name "Redo last action"
              :hotkey "r"
              :ev [:redo]}
   :undo-multi {:name "Undo..."
                :hotkey "U"
                :ev [:undo-multi]}
   :redo-multi {:name "Redo..."
                :hotkey "U"
                :ev [:redo-multi]}
   :open-debug-view {:name "Open debug view"
                     :hotkey "z"
                     :ev [:open-debug-view]}
   :open-item-editor {:name "Open item editor"
                     :hotkey "1"
                     :ev [:open-item-editor]}
   :open-recipe-editor {:name "Open recipe editor"
                     :hotkey "2"
                     :ev [:open-recipe-editor]}
   :open-machine-editor {:name "Open machine editor"
                     :hotkey "3"
                     :ev [:open-machine-editor]}
   :import-world {:name "Import world"
                     :hotkey "z"
                     :ev [:import-world]}
   :export-world {:name "Export world"
                     :hotkey "z"
                     :ev [:export-world]}
   :load-preset-world {:name "Load preset world"
                     :hotkey "z"
                     :ev [:load-preset-world]}
   :reset-world {:name "Delete world"
                     :hotkey "z"
                     :ev [:delete-world]}
   :change-item-unit {:name "Change unit: Item rate"
                      :ev [:change-item-rate]}})