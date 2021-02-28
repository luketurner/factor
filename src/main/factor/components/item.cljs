(ns factor.components.item
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [factor.util :refer [filtered-update]]
            [factor.components.widgets :as w]))

(defn picker [value on-change focused?]
  [w/dropdown @(subscribe [:item-names]) value "Select item..." on-change focused?])

(defn rate-list [rates]
  (if (not-empty rates)
    (into [:ul]
          (for [[item num] rates] [:li (str num " " (:name @(subscribe [:item item])))]))
    [:p "No items."]))

(defn rate-editor [[itm r] on-change focused?]
  [:div
   [w/input-rate r #(on-change [itm %] [itm r])]
   [picker itm #(on-change [% r] [itm r]) focused?]])

(defn rate-editor-list [rates on-change]
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
        [w/list-editor-validated {:data rates
                                :unsaved-data @unsaved-rates
                                :row-fn (fn [item-rate]
                                          [w/deletable-row {:on-delete #(on-change (dissoc rates (item-rate 0)))}
                                           [rate-editor item-rate
                                            (fn [[nk nv] [ok _]]
                                              (when (or (not-empty nk)
                                                        (not-empty ok))
                                                (on-change (-> rates
                                                               (dissoc ok)
                                                               (assoc nk nv)))))]])
                                :empty-message [:div "No items."]
                                :add-fn add-unsaved-rate!
                                :unsaved-row-fn (fn [[ix item-rate]]
                                                  [w/deletable-row {:on-delete dissoc-unsaved-rate!}
                                                   [rate-editor item-rate
                                                   #(update-or-save-unsaved-rate! [ix %])]])}]))))

(defn editor [id focused?]
  (let [item @(subscribe [:item id])
        update-name #(dispatch [:update-item id (assoc item :name %)])]
    [w/input-text (:name item) update-name focused?]))

(defn editor-list [items]
  [w/list-editor {:data items
                :row-fn (fn [id] [w/deletable-row {:on-delete [:delete-item id]}
                                  [editor id (= id (last items))]])
                :empty-message [:p "No items."]
                :add-fn #(dispatch [:create-item])}])