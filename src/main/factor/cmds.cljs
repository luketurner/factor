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
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.ratom :refer [reaction]]
            [factor.util :refer [ipairs]]))

(def all-cmds
  {:open-command-palette (reaction {:name "Open command palette"
                                    :global-hotkey "/"
                                    :ev [:open-command-palette]})

   :new-factory (reaction {:name "New factory"
                           :icon :plus
                           :params [{:name "Name"}]
                           :ev [:new-factory]})

   :open-factory (reaction {:name "Open factory"
                            :disabled (not @(subscribe [:any-factories?]))
                            :params [{:name "Factory"
                                      :choices (for [[id name] @(subscribe [:factory-ids->names])]
                                                 {:key id
                                                  :name name
                                                  :value id})}]
                            :ev [:open-factory]})

   :delete-factory (reaction {:name "Delete factory"
                              :disabled (not @(subscribe [:any-factories?]))
                              :params [{:name "Factory"
                                        :choices (for [[id name] @(subscribe [:factory-ids->names])]
                                                   {:key id
                                                    :name name
                                                    :value id})}]
                              :ev [:delete-factory]})

   :delete-open-factory (reaction {:name "Delete open factory"
                                   :icon :delete
                                   :disabled (not @(subscribe [:open-factory?]))
                                   :ev [:delete-open-factory]})

   :undo (reaction {:name "Undo"
                    :disabled (not @(subscribe [:undos?]))
                    :icon :undo
                    :ev [:undo]})

   :redo (reaction {:name "Redo"
                    :icon :redo
                    :disabled (not @(subscribe [:redos?]))
                    :ev [:redo]})

   :undo-multi (reaction {:name "Undo multiple"
                          :disabled (not @(subscribe [:undos?]))
                          :params [{:name "Undo target"
                                    :choices (for [[ex ix] (rest (ipairs @(subscribe [:undo-explanations])))]
                                               {:name ex
                                                :key ix
                                                :value ix})}]
                          :ev [:undo]})

   :redo-multi (reaction {:name "Redo multiple"
                          :disabled (not @(subscribe [:redos?]))
                          :params [{:name "Redo target"
                                    :choices (for [[ex ix] (rest (ipairs @(subscribe [:redo-explanations])))]
                                               {:name ex
                                                :key ix
                                                :value ix})}]
                          :ev [:redo]})

   :open-item-editor (reaction {:name "Open item editor"
                                :ev [:open-item-editor]})

   :open-recipe-editor (reaction {:name "Open recipe editor"
                                  :ev [:open-recipe-editor]})

   :open-machine-editor (reaction {:name "Open machine editor"
                                   :ev [:open-machine-editor]})

   :open-import-export (reaction {:name "Import/export world"
                                  :ev [:open-import-export]})
   
   :open-help (reaction {:name "Open help"
                         :ev [:open-help]})

   :delete-world (reaction {:name "Delete world"
                            :ev [:delete-world]})

   :new-item (reaction {:name "New item"
                        :icon :plus
                        :ev [:new-item]})

   :new-recipe (reaction {:name "New recipe"
                          :icon :plus
                          :ev [:new-recipe]})

   :new-machine (reaction {:name "New machine"
                           :icon :plus
                           :ev [:new-machine]})

   :delete-selected-items (reaction (let [objects @(subscribe [:selected-objects])]
                                      {:name "Delete selected item(s)"
                                       :icon :delete
                                       :disabled (empty? objects)
                                       :ev [:delete-selected-items]}))

   :delete-selected-recipes (reaction (let [objects @(subscribe [:selected-objects])]
                                        {:name "Delete selected recipe(s)"
                                         :icon :delete
                                         :disabled (empty? objects)
                                         :ev [:delete-selected-recipes]}))

   :delete-selected-machines (reaction (let [objects @(subscribe [:selected-objects])]
                                         {:name "Delete selected machine(s)"
                                          :icon :delete
                                          :disabled (empty? objects)
                                          :ev [:delete-selected-machines]}))

   :change-item-rate-unit (reaction (let [cur @(subscribe [:unit :item-rate])
                                          choice (fn [v] {:key v :name v :value v :disabled (= v cur)})]
                                      {:name "Item rate unit"
                                       :params [{:name "Unit" :choices [(choice "items/sec") (choice "items/min")]}]
                                       :ev [:change-item-rate-unit]}))

   :change-energy-unit (reaction (let [cur @(subscribe [:unit :energy])
                                       choice (fn [v] {:key v :name v :value v :disabled (= v cur)})]
                                   {:name "Energy unit"
                                    :params [{:name "Unit" :choices [(choice "J")]}]
                                    :ev [:change-energy-unit]}))

   :change-power-unit (reaction (let [cur @(subscribe [:unit :power])
                                      choice (fn [v] {:key v :name v :value v :disabled (= v cur)})]
                                  {:name "Power unit"
                                   :params [{:name "Unit" :choices [(choice "W")]}]
                                   :ev [:change-power-unit]}))

   :toggle-filter-view (reaction (let [[p1] @(subscribe [:page-route])]
                                   {:name "Toggle filter view"
                                    :disabled (not= p1 :factory)
                                    :ev [:toggle-filter-view]}))

   :toggle-debug-view (reaction (let [[p1] @(subscribe [:page-route])]
                                  {:name "Toggle debug view"
                                   :disabled (not= p1 :factory)
                                   :ev [:toggle-debug-view]}))})

(defn cmd-atom [kw] (get all-cmds kw))
(defn cmd [kw] (when-let [v (cmd-atom kw)] @v))

(defn dispatch-cmd
  [{:keys [ev params]} & args]
  (dispatch (into ev (take (count params) args))))