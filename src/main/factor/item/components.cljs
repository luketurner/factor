(ns factor.item.components
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [factor.util :refer [filtered-update]]
            [factor.widgets :refer [deletable-row dropdown input-rate input-text list-editor list-editor-validated]]))

(defn item-picker [value on-change focused?]
  [dropdown @(subscribe [:item-names]) value "Select item..." on-change focused?])

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
    (fn [rates on-change]
      (let [dissoc-unsaved-rate! (fn [[k _]]
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
        [list-editor-validated {:data rates
                                :unsaved-data @unsaved-rates
                                :row-fn (fn [item-rate]
                                          [deletable-row {:on-delete #(on-change (dissoc rates (item-rate 0)))}
                                           [item-rate-editor item-rate
                                            (fn [[nk nv] [ok _]]
                                              (when (or (not-empty nk)
                                                        (not-empty ok))
                                                (on-change (-> rates
                                                               (dissoc ok)
                                                               (assoc nk nv)))))]])
                                :empty-message [:div "No items."]
                                :add-fn add-unsaved-rate!
                                :unsaved-row-fn (fn [[ix item-rate]]
                                                  [deletable-row {:on-delete dissoc-unsaved-rate!}
                                                   [item-rate-editor item-rate
                                                   #(update-or-save-unsaved-rate! [ix %])]])}]))))

(defn item-editor [id focused?]
  (let [item @(subscribe [:item id])
        update-name #(dispatch [:update-item id (assoc item :name %)])]
    [input-text (:name item) update-name focused?]))

(defn item-editor-list [items]
  [list-editor {:data items
                :row-fn (fn [id] [deletable-row {:on-delete [:delete-item id]}
                                  [item-editor id (= id (last items))]])
                :empty-message [:p "No items."]
                :add-fn #(dispatch [:create-item])}])