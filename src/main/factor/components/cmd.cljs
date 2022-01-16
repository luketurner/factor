(ns factor.components.cmd
  "Components relating to invoking commands."
  (:require [factor.components.macros :refer-macros [defcomponent]]
            [factor.components.wrappers :as w]
            [factor.components.inputs :refer [omnibar input-omnibar]]
            [com.rpl.specter :as s]
            [reagent.core :refer [with-let] :as reagent]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [factor.cmds :as cmds]
            [clojure.string :as string]
            [medley.core :refer [index-by]]))

(defcomponent cmd-btn
  [{:keys [minimal intent class cmd]} _]
  (let [{:keys [ev name icon disabled-sub]} (cmds/cmd cmd)
        disabled (and disabled-sub @(subscribe disabled-sub))]
    [w/button {:on-click (reagent/partial dispatch ev)
               :text name
               :intent intent
               :icon icon
               :disabled disabled
               :class (if minimal "bp3-minimal" class)}]))

(defcomponent cmd-invocation-omnibar
  "Omnibar mode for collecting user input during command invocations."
  [_ _]
  (with-let [on-item-select (fn [choice] (dispatch-sync [:cmd-invoke-collect (:value choice)]))
             on-submit (fn [value] (dispatch-sync [:cmd-invoke-collect value]))
             item-predicate (fn [q {:keys [name] :as v} _ exact-match?]
                              (if exact-match? (= q name)
                                  (string/includes? (string/lower-case name) (string/lower-case q))))
             item-renderer (fn [{:keys [key name]} {:keys [on-click is-disabled is-active]}]
                             [w/menu-item {:key key
                                           :text name
                                           :on-click on-click
                                           :disabled is-disabled
                                           :intent (when is-active "primary")}])]
    (let [{cmd-id :cmd collected-params :params} @(subscribe [:cmd-invocation])
          {:keys [name icon choice-sub]} (get-in (cmds/cmd cmd-id) [:params (count collected-params)])]
      (if choice-sub
        (let [choices @(subscribe choice-sub)
              keys (s/select [s/ALL :key] choices)
              key->choice (index-by :key choices)]
          [omnibar {:items keys
                    :item-getter key->choice
                    :on-item-select on-item-select
                    :item-predicate item-predicate
                    :item-renderer item-renderer
                    :placeholder name
                    :left-icon icon}])
        [input-omnibar {:on-submit on-submit
                        :placeholder name
                        :left-icon icon}]))))

(defcomponent cmd-menu-item
  [{:keys [cmd]} _]
  (let [{:keys [name disabled-sub ev] [{:keys [choice-sub]}] :params} (cmds/cmd cmd)
        choices (and choice-sub @(subscribe choice-sub))]
    (into [w/menu-item {:text name
                        :key cmd
                        :disabled (and disabled-sub @(subscribe disabled-sub))
                        :on-click (reagent/partial dispatch ev)}
           (when (not-empty choices)
             (for [{:keys [name key value disabled]} choices]
               [w/menu-item {:text name
                             :key key
                             :disabled disabled
                             :on-click (reagent/partial dispatch (conj ev value))}]))])))

(defcomponent cmd-global-hotkeys
  "Include once, somewhere in the React tree, to capture application-wide global hotkeys for commands."
  [p c]
  (with-let [cmd->hk (fn [cmd] {:combo (:global-hotkey cmd)
                                :label (:name cmd)
                                :global true
                                :onKeyDown #(dispatch [:cmd-invoke (:id cmd)])
                                :stopPropagation true
                                :preventDefault true})
             hks (s/select [s/ALL (s/pred :global-hotkey) (s/view cmd->hk)] (cmds/all-cmds))]
    ; note -- HotkeysTarget2 API requires children to be specified even if all hks are global. Hence the dummy child [:<>].
    [w/hotkeys-target {:hotkeys (or hks [])} [:<>]]))