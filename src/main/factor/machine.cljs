(ns factor.machine
  (:require [re-frame.core :refer [subscribe dispatch reg-event-db reg-sub]]
            [factor.util :refer [new-uuid]]
            [factor.widgets :refer [dropdown dropdown-submitted]]))

(defn machine [] {:name "Unnamed machine"})

(reg-event-db :create-machine (fn [db] (assoc-in db [:world :machines (new-uuid)] (machine))))
(reg-event-db :update-machine (fn [db [_ id v]] (assoc-in db [:world :machines id] v)))
(reg-event-db :delete-machine (fn [db [_ id]] (update-in db [:world :machines] dissoc id)))
(reg-sub :machine (fn [db [_ id]] (get-in db [:world :machines id])))
(reg-sub :machine-ids (fn [db] (-> db (get-in [:world :machines]) (keys))))
(reg-sub :machine-count (fn [db] (-> db (get-in [:world :machines]) (count))))
(reg-sub :machine-names (fn [db] (->> (get-in db [:world :machines])
                                      (map (fn [[k v]] [(:name v) k]))
                                      (into {}))))

(defn machine-list [machines]
  (if (not-empty machines)
    (into [:ul]
          (for [[machine num] machines] [:li (str num " " (:name @(subscribe [:machine machine])))]))
    [:p "No machines."]))

(defn machine-picker [value on-change]
  [dropdown @(subscribe [:machine-names]) value "Select machine..." on-change])

(defn machine-picker-submitted [on-submit]
  [dropdown-submitted @(subscribe [:machine-names]) "Select machine..." on-submit])

(defn machine-list-editor [machines on-change]
  (into
   [:div]
   (concat
    (for [machine-id machines]
      [:div
       [machine-picker machine-id #(on-change (-> machines (disj machine-id) (conj %)))]
       [:button {:on-click #(on-change (disj machines machine-id))} "-"]])
    [[machine-picker-submitted (fn [m] (on-change (conj machines m)))]])))

(defn machine-editor [machine-id]
  (let [machine @(subscribe [:machine machine-id])
        update-name #(dispatch [:update-machine machine-id (assoc machine :name (.-value (.-target %)))])
        delete-machine #(dispatch [:delete-machine machine-id])]
    [:div
     [:input {:type "text" :value (:name machine) :on-change update-name}]
     [:button {:on-click delete-machine} "-"]]))

(defn machine-page []
  (let [machines @(subscribe [:machine-ids])]
    [:div
     [:h2 "machines"]
     (if (not-empty machines)
       (into [:p] (for [id machines] [machine-editor id]))
       [:p "You don't have any machines."])
     [:button {:on-click #(dispatch [:create-machine])} "Add machine"]]))