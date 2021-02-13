(ns factor.widgets
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch]]
            ["react-hotkeys" :as rhk :refer [HotKeys]]))

(rhk/configure #js {"ignoreTags" #js []})

(defn hotkeys
  "Wrapper for react-hotkeys. Instead of specifying keymaps and handlers separately,
   the keymap is a dict of form { keys event-to-dispatch }.
   The dict's key is a string or vector of strings, and the value is a vector that will
   be dispatched as a re-frame event.)"
  [keymap & children]
  (let [handler-for (fn [ev] (fn [e]
                               (.stopPropagation e)
                               (dispatch ev)))
        handlers (clj->js (into {} (for [[_ ev] keymap] 
                                     [(str ev) (handler-for ev)])))
        keymap (clj->js (into {} (for [[keys ev] keymap]
                                   [(str ev) keys])))]
    (into [:> HotKeys {:key-map keymap :handlers handlers}] children)))

(defn input-rate [value on-change]
  [:input.rate-picker {:type "number"
                       :value (or value 0)
                       :min 0
                       :on-change #(-> % (.-target) (.-value) (on-change))}])

(defn dropdown [options value placeholder on-change]
  (into [:select.dropdown {:value (or value "") :on-change #(let [v (.-value (.-target %))]
                                                              (when (not-empty v) (on-change v)))}
         [:option {:value ""} placeholder]]
        (for [[name value] options] [:option {:value value} name])))

(defn dropdown-submitted [options placeholder on-submit]
  (let [value (reagent/atom nil)]
    (fn [options placeholder on-submit]
      [:div
       [dropdown options @value placeholder #(reset! value %)]
       [:button {:on-click #(on-submit @value)} "+"]])))

