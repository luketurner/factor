(ns factor.widgets
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch]]
            ["react-hotkeys" :as rhk :refer [GlobalHotKeys HotKeys]]))

(rhk/configure #js {"ignoreTags" #js []})

(defn hotkeys [keymap & children]
  (let [handlers (clj->js (into {} (for [[k _] keymap] [k #(dispatch [k])])))
        keymap (clj->js keymap)]
    (into [:> HotKeys {:key-map keymap :handlers handlers}] children)))

(defn global-hotkeys [keymap]
  (let [handlers (clj->js (into {} (for [[k _] keymap] [k #(dispatch [k])])))
        keymap (clj->js keymap)]
    [:> GlobalHotKeys {:key-map keymap :handlers handlers}]))

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

