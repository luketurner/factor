(ns factor.app
  (:require [reagent.dom :refer [render]]
            [reagent.core :as reagent]
            [garden.core :refer [css]]
            [garden.selectors :as gs]
            ["uuid" :as uuid]
            [clojure.string :as string]
            [re-frame.core :refer [reg-event-db reg-sub subscribe dispatch]]
            [factor.widgets :refer [rate-picker dropdown dropdown-submitted]]))

(defn nextid [] (uuid/v4))


(defn factory []
  {:name "Unnamed Factory"
   :input {}
   :output {}})

(defn item [] {:name "Unnamed item"})

(defn machine [] {:name "Unnamed machine"})

(defn recipe [] {:input {}
                 :output {}
                 :machines #{}})

(reg-event-db :create-item (fn [db] (assoc-in db [:world :items (nextid)] (item))))
(reg-event-db :update-item (fn [db [_ id v]] (assoc-in db [:world :items id] v)))
(reg-event-db :delete-item (fn [db [_ id]] (update-in db [:world :items] dissoc id)))
(reg-sub :item (fn [db [_ id]] (get-in db [:world :items id])))
(reg-sub :item-ids (fn [db] (-> db (get-in [:world :items]) (keys))))
(reg-sub :item-names (fn [db] (->> (get-in db [:world :items])
                                   (map (fn [[k v]] [(:name v) k]))
                                   (into {}))))

(reg-event-db :create-machine (fn [db] (assoc-in db [:world :machines (nextid)] (machine))))
(reg-event-db :update-machine (fn [db [_ id v]] (assoc-in db [:world :machines id] v)))
(reg-event-db :delete-machine (fn [db [_ id]] (update-in db [:world :machines] dissoc id)))
(reg-sub :machine (fn [db [_ id]] (get-in db [:world :machines id])))
(reg-sub :machine-ids (fn [db] (-> db (get-in [:world :machines]) (keys))))
(reg-sub :machine-names (fn [db] (->> (get-in db [:world :machines])
                                   (map (fn [[k v]] [(:name v) k]))
                                   (into {}))))


(reg-event-db :create-recipe (fn [db] (assoc-in db [:world :recipes (nextid)] (recipe))))
(reg-event-db :update-recipe (fn [db [_ id v]] (assoc-in db [:world :recipes id] v)))
(reg-event-db :delete-recipe (fn [db [_ id]] (update-in db [:world :recipes] dissoc id)))
(reg-sub :recipe (fn [db [_ id]] (get-in db [:world :recipes id])))
(reg-sub :recipe-ids (fn [db] (-> db (get-in [:world :recipes]) (keys))))


(reg-event-db :create-factory (fn [db] (assoc-in db [:factories (nextid)] (factory))))
(reg-event-db :update-factory (fn [db [_ id factory]] (assoc-in db [:factories id] factory)))
(reg-event-db :delete-factory (fn [db [_ id]] (update-in db [:factories] dissoc id)))
(reg-sub :factory (fn [db [_ id]] (get-in db [:factories id])))
(reg-sub :factory-ids (fn [db] (-> db (get-in [:factories]) (keys))))

(reg-event-db :select-page (fn [db [_ page]] (assoc-in db [:ui :selected-page] page)))
(reg-sub :selected-page (fn [db _] (get-in db [:ui :selected-page])))

(reg-event-db :initialize-db (fn [] {:ui {:selected-page :factories}
                                     :factories {}
                                     :world {:items {}
                                             :recipes {}
                                             :machines {}}}))


(defn item-picker [value on-change]
  [dropdown @(subscribe [:item-names]) value "Select item..." on-change])

(defn item-rate-creator [on-create]
  (let [rate (reagent/atom nil)
        item (reagent/atom nil)]
    (fn [on-create]
      [:div
       [rate-picker @rate #(reset! rate %)]
       [item-picker @item #(reset! item %)]
       [:button {:on-click #(on-create @item @rate)} "+"]])))

(defn item-rate-list-editor [items on-change]
  (into
   [:div]
   (concat
    (for [[item rate] items]
      [:div
       [rate-picker rate #(on-change (assoc items item %))]
       [item-picker item #(on-change (-> items (dissoc item) (assoc % rate)))]
       [:button {:on-click #(on-change (dissoc items item))} "-"]])
    [[item-rate-creator (fn [i r] (on-change (assoc items i r)))]])))

(defn item-rate-list [items]
  (if (not-empty items)
    (into [:ul]
        (for [[item num] items] [:li (str num " " (:name @(subscribe [:item item])))]))
    [:p "No items."]))

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

(defn recipe-viewer [recipe-id]
  (let [{:keys [input output machines]} @(subscribe [:recipe recipe-id])]
    [:details [:summary (first (keys output))]
     [:dl
      [:dt "Inputs"]
      [:dd [item-rate-list input]]
      [:dt "Outputs"]
      [:dd [item-rate-list output]]
      [:dt "Machines"]
      [:dd [machine-list machines]]]]))

(defn factory-editor [factory-id]
  (let [{:keys [name input output] :as factory} @(subscribe [:factory factory-id])
        upd #(dispatch [:update-factory factory-id %])]
    [:div
     [:h2
      [:input {:class "factory-name" :type "text" :value name :on-change #(upd (assoc factory :name (.-value (.-target %))))}]
      [:button {:on-click #(dispatch [:delete-factory factory-id])} "Delete"]]
     [:dl
      [:dt "Outputs"]
      [:dd [item-rate-list-editor output #(upd (assoc factory :output %))]]
      [:dt "Inputs"]
      [:dd [item-rate-list input]]
      [:dt "Recipes"]
      [:dd
       [:details [:summary "Smelt steel" [:button "Avoid"]]
        [:dl
         [:dt "Inputs"]
         [:dd [:ul
               [:li "2 Iron ore"]
               [:li "1 Coal ore"]]]
         [:dt "Outputs"]
         [:dd [:ul
               [:li "2 Steel ore"]]]
         [:dt "Machines"]
         [:dd [:ul
               [:li "Smelter"]]]]]]
      [:dt "Consumption Tree"]
      [:dd "..."]]]))


(defn factory-view []
  (let [factories @(subscribe [:factory-ids])]
    [:div
     (if (not-empty factories)
       (into [:div] (for [fact-id factories] [factory-editor fact-id]))
       [:p "You don't have any factories."])
     [:button {:on-click #(dispatch [:create-factory])} "Add factory"]]))


(defn item-editor [item-id]
  (let [item @(subscribe [:item item-id])
        update-name #(dispatch [:update-item item-id (assoc item :name (.-value (.-target %)))])
        delete-item #(dispatch [:delete-item item-id])]
      [:div
       [:input {:type "text" :value (:name item) :on-change update-name}]
       [:button {:on-click delete-item} "-"]]))

(defn item-view []
  (let [items @(subscribe [:item-ids])]
    [:div
     (if (not-empty items)
       (into [:div] (for [id items] [item-editor id]))
       [:p "You don't have any items."])
     [:button {:on-click #(dispatch [:create-item])} "Add item"]]))

(defn machine-editor [machine-id]
  (let [machine @(subscribe [:machine machine-id])
        update-name #(dispatch [:update-machine machine-id (assoc machine :name (.-value (.-target %)))])
        delete-machine #(dispatch [:delete-machine machine-id])]
    [:div
     [:input {:type "text" :value (:name machine) :on-change update-name}]
     [:button {:on-click delete-machine} "-"]]))

(defn machine-view []
  (let [machines @(subscribe [:machine-ids])]
    [:div
     (if (not-empty machines)
       (into [:div] (for [id machines] [machine-editor id]))
       [:p "You don't have any machines."])
     [:button {:on-click #(dispatch [:create-machine])} "Add machine"]]))

(defn recipe-editor [recipe-id]
  (let [{:keys [input output machines] :as recipe} @(subscribe [:recipe recipe-id])
        output-item-names (for [[o _] output] (:name @(subscribe [:item o])))
        display-name (if (not-empty output)
                       (str "Recipe:" (string/join ", " output-item-names))
                       "New Recipe")
        upd #(dispatch [:update-recipe recipe-id %])]
    [:details [:summary display-name]
     [:dl
      [:dt "Inputs"]
      [:dd [item-rate-list-editor input #(upd (assoc recipe :input %))]]
      [:dt "Outputs"]
      [:dd [item-rate-list-editor output #(upd (assoc recipe :output %))]]
      [:dt "Machines"]
      [:dd [machine-list-editor machines #(upd (assoc recipe :machines %))]]]]))

(defn recipe-view []
  (let [recipes @(subscribe [:recipe-ids])]
    [:div
     (if (not-empty recipes)
       (into [:div] (for [id recipes] [recipe-editor id]))
       [:p "You don't have any recipes."])
     [:button {:on-click #(dispatch [:create-recipe])} "Add recipe"]]))



(def app-styles
  (css [:#app {:max-width "786px" :margin "auto" }]
       [:h1 {:text-align "center"}]
       [:.navbar {:text-align "center"}]
       [:h2 {:border-bottom "1px solid black"}]
       [:.factory-name {:font-size "inherit"}]
       [:button {:margin-left "1rem"}]
       [:dd {:margin-left "4rem" :margin-bottom "1rem"}]
       [:ul {:padding "0"}]
       [:li {:list-style "none"}]
       [:.rate-picker {:width "5em"}]
       [:.dropdown {:width "11em"}]))




(defn app []
  [:div
   [:style app-styles]
   [:h1 "Factor"]
   [:div.navbar
    [:button {:on-click #(dispatch [:select-page :factories])} "Factories"]
    [:button {:on-click #(dispatch [:select-page :items])} "Items"]
    [:button {:on-click #(dispatch [:select-page :recipes])} "Recipes"]
    [:button {:on-click #(dispatch [:select-page :machines])} "Machines"]]
   (case @(subscribe [:selected-page])
     :factories [factory-view]
     :items [item-view]
     :recipes [recipe-view]
     :machines [machine-view]
     [:p "Loading..."])])


(defn init []
  (dispatch [:initialize-db])
  (render [app] (js/document.getElementById "app")))
