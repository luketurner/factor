(ns factor.widgets
  (:require [reagent.core :as reagent]))

(defn rate-picker [value on-change]
  [:input.rate-picker {:type "number"
                       :placeholder "--"
                       :value value
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
