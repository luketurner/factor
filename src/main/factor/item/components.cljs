(ns factor.item.components
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [factor.widgets :refer [dropdown input-rate hotkeys]]))

(defn item-picker [value on-change]
  [dropdown @(subscribe [:item-names]) value "Select item..." on-change])

(defn item-rate-creator [on-create]
  (let [rate (reagent/atom nil)
        item (reagent/atom nil)]
    (fn [on-create]
      [:div
       [input-rate @rate #(reset! rate %)]
       [item-picker @item #(reset! item %)]
       [:button {:on-click #(on-create @item @rate)} "+"]])))

(defn item-rate-list-editor [items on-change]
  (into
   [:div]
   (concat
    (for [[item rate] items]
      [:div
       [input-rate rate #(on-change (assoc items item %))]
       [item-picker item #(on-change (-> items (dissoc item) (assoc % rate)))]
       [:button {:on-click #(on-change (dissoc items item))} "-"]])
    [[item-rate-creator (fn [i r] (on-change (assoc items i r)))]])))

(defn item-rate-list [items]
  (if (not-empty items)
    (into [:ul]
          (for [[item num] items] [:li (str num " " (:name @(subscribe [:item item])))]))
    [:p "No items."]))


(defn item-editor [id focused?]
  (let [item @(subscribe [:item id])
        update-name #(dispatch [:update-item id (assoc item :name (.-value (.-target %)))])
        delete-item #(dispatch [:delete-item id])]
    [hotkeys {"del" [:delete-item id]}
     [:input {:type "text" :value (:name item) :on-change update-name :auto-focus focused?}]
     [:button {:on-click delete-item} "-"]]))
