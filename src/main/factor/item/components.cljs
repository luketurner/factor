(ns factor.item.components
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [factor.util :refer [filtered-update]]
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
  (let [unsaved-rates (reagent/atom [])
        dissoc-unsaved-rate! (fn [[k _]]
                               (swap! unsaved-rates
                                      (fn [xs] (into [] (filter #(not= k (% 0)) xs)))))
        unsaved-rate-valid? (fn [[item rate]]
                              (and (not-empty item)
                                   (pos? rate)))
        save-rate! (fn [[item rate]]
                     (on-change (merge-with + rates {item rate})))
        update-unsaved-rate! (fn [[k rate-tuple :as kvp]]
                               (swap!
                                unsaved-rates
                                (fn [xs]
                                  (into [] (filtered-update xs
                                                            #(= (% 0) k)
                                                            #(identity kvp))))))
        update-or-save-unsaved-rate! (fn [[_ rate-tuple :as kvp]]
                                      (if (unsaved-rate-valid? rate-tuple)
                                        (do
                                          (save-rate! rate-tuple)
                                          (dissoc-unsaved-rate! kvp))
                                        (update-unsaved-rate! kvp)))
        add-unsaved-rate! (fn [] (swap!
                                  unsaved-rates
                                  #(into [] (conj % [(inc (get (last @unsaved-rates) 0)) ["" 0]]))))]
    (fn [rates on-change]
      [list-editor-validated {:data rates
                              :unsaved-data @unsaved-rates
                              :row-fn (fn [item-rate]
                                        [item-rate-editor item-rate
                                         (fn [[nk nv] [ok _]]
                                           (when (or (not-empty nk)
                                                     (not-empty ok))
                                             (on-change (-> rates
                                                            (dissoc ok)
                                                            (assoc nk nv)))))])
                              :empty-message [:p "No items."]
                              :add-fn add-unsaved-rate!
                              :del-fn #(on-change (dissoc rates (% 0)))
                              :unsaved-row-fn (fn [[ix item-rate]]
                                                [item-rate-editor item-rate
                                                 #(update-or-save-unsaved-rate! [ix %])])
                              :unsaved-del-fn dissoc-unsaved-rate!}])))

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