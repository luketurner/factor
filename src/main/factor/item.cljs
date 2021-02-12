(ns factor.item
  (:require [re-frame.core :refer [subscribe dispatch reg-event-db reg-sub]]
            [reagent.core :as reagent]
            [factor.util :refer [new-uuid]]
            [factor.widgets :refer [dropdown input-rate]]))

(defn item [] {:name "Unnamed item"})

(reg-event-db :create-item (fn [db] (assoc-in db [:world :items (new-uuid)] (item))))
(reg-event-db :update-item (fn [db [_ id v]] (assoc-in db [:world :items id] v)))
(reg-event-db :delete-item (fn [db [_ id]] (update-in db [:world :items] dissoc id)))

(reg-sub :item (fn [db [_ id]] (get-in db [:world :items id])))
(reg-sub :item-ids (fn [db] (-> db (get-in [:world :items]) (keys))))
(reg-sub :item-names (fn [db] (->> (get-in db [:world :items])
                                   (map (fn [[k v]] [(:name v) k]))
                                   (into {}))))

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


(defn item-editor [item-id]
  (let [item @(subscribe [:item item-id])
        update-name #(dispatch [:update-item item-id (assoc item :name (.-value (.-target %)))])
        delete-item #(dispatch [:delete-item item-id])]
    [:div
     [:input {:type "text" :value (:name item) :on-change update-name}]
     [:button {:on-click delete-item} "-"]]))

(defn item-page []
  (let [items @(subscribe [:item-ids])]
    [:div
     (if (not-empty items)
       (into [:div] (for [id items] [item-editor id]))
       [:p "You don't have any items."])
     [:button {:on-click #(dispatch [:create-item])} "Add item"]]))
