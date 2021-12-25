(ns factor.cmds
  "Defines all the commands used in Factor. Commands are user-facing actions
   that can be invoked with the command palette, or with their own global hotkeys.
   Depending on the context of the open view, etc. not all commands are available.
   Menu/toolbar buttons also invoke commands.

   Commands are actually signals -- reactions -- that can reference other signals (e.g. DB subscriptions.) So,
   the command's name, disabld status, event, etc. can change dynamically.
   
   The `cmd` helper function automatically derefs the command, so you don't need to remember this
   implementation detail in your views. But if you want un-derefed command, use `cmd-atom`
   
   Field reference:
   :name -- Command name (used for button text, searching, etc.)
   :ev -- vector to dispatch when command is clicked
   :icon -- icon to display alongside command name
   :disabled -- whether command should be invokeable
   :global-hotkey -- the global hotkey to invoke the command"
  (:require [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer [reaction]]))

(def all-cmds
  {:open-command-palette (reaction {:name "Open command palette"
                                    :global-hotkey "/"
                                    :ev [:open-command-palette]})

   :new-factory (reaction {:name "New factory"
                           :icon :plus
                           :ev [:omnibar-create-factory]})

   :open-factory (reaction {:name "Open factory..."
                            :ev [:omnibar-open-factory]})

   :delete-factory (reaction {:name "Delete factory..."
                              :ev [:omnibar-delete-factory]})

   :delete-open-factory (reaction {:name "Delete open factory"
                                   :icon :delete
                                   :ev [:delete-factories [(second @(subscribe [:page-route]))]]})

   :edit-filters (reaction {:name "Edit filters"
                            :ev [:edit-filters]})

   :undo (reaction {:name "Undo"
                    :disabled (not @(subscribe [:undos?]))
                    :icon :undo
                    :ev [:undo]})

   :redo (reaction {:name "Redo"
                    :icon :redo
                    :disabled (not @(subscribe [:redos?]))
                    :ev [:redo]})

   :undo-multi (reaction {:name "Undo multiple..."
                          :ev [:undo-multi]})

   :redo-multi (reaction {:name "Redo multiple..."
                          :ev [:redo-multi]})

   :open-debug-view (reaction {:name "Open debug view"
                               :ev [:open-debug-view]})

   :open-item-editor (reaction {:name "Open item editor"
                                :ev [:update-route [:items]]})

   :open-recipe-editor (reaction {:name "Open recipe editor"
                                  :ev [:update-route [:recipes]]})

   :open-machine-editor (reaction {:name "Open machine editor"
                                   :ev [:update-route [:machines]]})

   :open-factory-editor (reaction {:name "Open factory editor"
                                   :ev [:update-route [:factories]]})

   :import-world (reaction {:name "Import world"
                            :ev [:import-world]})

   :export-world (reaction {:name "Export world"
                            :ev [:export-world]})

   :load-preset-world (reaction {:name "Load preset world"
                                 :ev [:load-preset-world]})

   :reset-world (reaction {:name "Delete world"
                           :ev [:delete-world]})

   :new-item (reaction {:name "New item"
                        :icon :plus
                        :ev [:create-item]})

   :new-recipe (reaction {:name "New recipe"
                          :icon :plus
                          :ev [:create-recipe]})

   :new-machine (reaction {:name "New machine"
                           :icon :plus
                           :ev [:create-machine]})

   :delete-selected-items (reaction (let [objects @(subscribe [:selected-objects])]
                                      {:name "Delete selected item(s)"
                                       :icon :delete
                                       :disabled (empty? objects)
                                       :ev [:delete-items objects]}))

   :delete-selected-recipes (reaction (let [objects @(subscribe [:selected-objects])]
                                        {:name "Delete selected recipe(s)"
                                         :icon :delete
                                         :disabled (empty? objects)
                                         :ev [:delete-recipes objects]}))

   :delete-selected-machines (reaction (let [objects @(subscribe [:selected-objects])]
                                         {:name "Delete selected machine(s)"
                                          :icon :delete
                                          :disabled (empty? objects)
                                          :ev [:delete-machines objects]}))

   :change-item-unit (reaction {:name "Change unit: Item rate"
                                :ev [:change-item-rate]})})

(defn cmd-atom [kw] (get all-cmds kw))
(defn cmd [kw] (when-let [v (cmd-atom kw)] @v))