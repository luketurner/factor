(ns factor.item.components
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [factor.widgets :refer [dropdown input-rate input-text list-editor list-editor-validated]]))

(defn item-picker [value on-change focused?]
  [dropdown @(subscribe [:item-names]) value "Select item..." on-change focused?])

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

(defn item-rate-list [rates]
  (if (not-empty rates)
    (into [:ul]
          (for [[item num] rates] [:li (str num " " (:name @(subscribe [:item item])))]))
    [:p "No items."]))

(defn item-rate-editor [[itm r] on-change focused?]
  [:div
   [input-rate r #(on-change [itm %] [itm r])]
   [item-picker itm #(on-change [% r] [itm r]) focused?]])

(defn item-rate-editor-list [rates on-change]
  (let [unsaved-rates (reagent/atom [])]
    (println "unsaved-rates" @unsaved-rates)
    (fn [rates on-change]
      [list-editor-validated {:data rates
                              :unsaved-data @unsaved-rates
                              :row-fn (fn [rate]
                                        [item-rate-editor rate
                                         (fn [[nk nv] [ok _]]
                                           (when (or (not-empty nk)
                                                     (not-empty ok))
                                             (on-change (-> rates
                                                            (dissoc ok)
                                                            (assoc nk nv)))))
                                         (= rate (last rates))])
                              :empty-message [:p "No items."]
                              :add-fn #(swap! unsaved-rates conj [(inc (get (last @unsaved-rates) 0)) ["" 0]])
                              :del-fn #(on-change (dissoc rates (% 0)))
                              :unsaved-row-fn (fn [[k [item rate]]]
                                                [item-rate-editor [item rate]
                                                 (fn [[item rate]]
                                                   ; TODO -- dispatch :create-item event
                                                   ; when data is valid
                                                   (swap!
                                                    unsaved-rates
                                                    #(into [] (for [[k' _ :as old] %]
                                                       (if (= k k')
                                                         [k [item rate]]
                                                         old)))))
                                                 (= rate (last rates))])
                              :unsaved-del-fn (fn [[k _]] (swap! unsaved-rates (filter #(not= k (% 0)))))}])))

(defn item-editor [id focused?]
  (let [item @(subscribe [:item id])
        update-name #(dispatch [:update-item id (assoc item :name %)])]
    [input-text (:name item) update-name focused?]))

(defn item-editor-list [items]
  [list-editor {:data items
                :row-fn (fn [id] [item-editor id (= id (last items))])
                :empty-message [:p "No items."]
                :add-fn #(dispatch [:create-item])
                :del-fn #(dispatch [:delete-item %])}])