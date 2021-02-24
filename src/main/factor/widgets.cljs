(ns factor.widgets
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch]]
            ["react-hotkeys" :as rhk :refer [HotKeys]]
            [factor.util :refer [new-uuid]]))

(rhk/configure #js {"ignoreTags" #js []})

(defn button [{:keys [on-click auto-focus]} & children]
  (into
   [:button {:on-click #(if (fn? on-click) (on-click) (dispatch on-click))
             :auto-focus auto-focus}]
   children))

(defn hotkeys
  "Wrapper for react-hotkeys. Instead of specifying keymaps and handlers separately,
   the keymap is a dict of form { keys [:handler-name handler-fn] }.
   The dict's key is a string or vector of strings."
  [keymap & children]
  (let [handlers (clj->js (into {} (vals keymap)))
        keymap (clj->js (into {} (for [[ks [k _]] keymap] [k ks])))]
    (into [:> HotKeys {:key-map keymap :handlers handlers}] children)))

(defn deletable-row [{:keys [on-delete]} & children]
  [hotkeys {"alt+backspace" [(new-uuid) on-delete]}
   (into
    [:div.row]
    (conj (vec children) [button {:on-click on-delete} "-"]))])

(defn addable-rows [{:keys [on-create auto-focus]} & children]
  [:<>
   [hotkeys {"enter" [(new-uuid) on-create]}
    (into [:div.rows] children)]
   [button {:on-click on-create :auto-focus auto-focus} "Add"]])

(defn input-text [value on-change focused?]
  [:input {:type "text"
           :value value
           :on-change #(-> % (.-target) (.-value) (on-change))
           :auto-focus focused?}])

(defn input-rate [value on-change]
  [:input.rate-picker {:type "number"
                       :value (or value 0)
                       :min 0
                       :on-change #(-> % (.-target) (.-value) (on-change))}])

(defn dropdown [options value placeholder on-change focused?]
  (into [:select.dropdown {:value (or value "") 
                           :on-change #(let [v (.-value (.-target %))]
                                         (when (not-empty v) (on-change v)))
                           :auto-focus focused?}
         [:option {:value ""} placeholder]]
        (for [[name value] options] [:option {:value value} name])))

(defn dropdown-submitted [options placeholder on-submit]
  (let [value (reagent/atom nil)]
    (fn [options placeholder on-submit]
      [:div
       [dropdown options @value placeholder #(reset! value %)]
       [button {:on-click #(on-submit @value)} "+"]])))


(defn list-editor [{:keys [data row-fn add-fn del-fn empty-message]}]
  [addable-rows {:on-create add-fn :auto-focus (empty? data)}
   (if (not-empty data)
     (->> data
          (map (fn [v] [deletable-row {:on-delete #(del-fn v)} (row-fn v)]))
          (into [:div]))
     empty-message)])


(defn list-editor-validated [{:keys [data row-fn add-fn del-fn unsaved-data unsaved-row-fn unsaved-del-fn empty-message]}]
  (let [row (fn [v] [deletable-row {:on-delete #(del-fn v)}
                     (row-fn v)])
        unsaved-row (fn [v] [deletable-row {:on-delete #(unsaved-del-fn v)}
                             (unsaved-row-fn v)])]
    [addable-rows {:on-create add-fn}
     (if (and (empty? data) (empty? unsaved-data)) empty-message
         (into [:div] (concat (map row data)
                              (map unsaved-row unsaved-data))))]))
