(ns factor.widgets
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch]]
            ["react-hotkeys" :as rhk :refer [HotKeys]]
            [factor.util :refer [new-uuid]]))

(rhk/configure #js {"ignoreTags" #js []})

(defn hotkeys
  "Wrapper for react-hotkeys. Instead of specifying keymaps and handlers separately,
   the keymap is a dict of form { keys [:handler-name handler-fn] }.
   The dict's key is a string or vector of strings."
  [keymap & children]
  (let [handlers (clj->js (into {} (vals keymap)))
        keymap (clj->js (into {} (for [[ks [k _]] keymap] [k ks])))]
    (into [:> HotKeys {:key-map keymap :handlers handlers}] children)))

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
       [:button {:on-click #(on-submit @value)} "+"]])))

(defn list-editor [{:keys [data row-fn add-fn del-fn empty-message]}]
  [:div
   [hotkeys {"enter" [(new-uuid) add-fn]}
   (if (not-empty data)
     (->> data
          (map (fn [v] [hotkeys {"alt+backspace" [(new-uuid) #(del-fn v)]}
                         (row-fn v)
                         [:button {:on-click #(del-fn v)} "-"]]))
          (into [:div]))
     empty-message)]
  [:button {:on-click add-fn} "Add"]])

(defn list-editor-validated [{:keys [data row-fn add-fn del-fn unsaved-data unsaved-row-fn unsaved-del-fn empty-message]}]
  (let [row (fn [v] [hotkeys {"alt+backspace" [(new-uuid) #(del-fn v)]}
                     (row-fn v)
                     [:button {:on-click #(del-fn v)} "-"]])
        unsaved-row (fn [v] [hotkeys {"alt+backspace" [(new-uuid) #(unsaved-del-fn v)]}
                             (unsaved-row-fn v)
                             [:button {:on-click #(unsaved-del-fn v)} "-"]])]
    [:div
     [hotkeys {"enter" [(new-uuid) add-fn]}
      (if (and (empty? data) (empty? unsaved-data)) empty-message
          (into [:div] (concat (map row data)
                               (map unsaved-row unsaved-data))))]
     [:button {:on-click add-fn} "Add"]]))

;; (defn list-editor-stateful [{:keys [data row-fn add-fn del-fn empty-message]}]
;;   (let [unsaved-data (reagent/atom [])
;;         list-row (fn [v] [hotkeys {"alt+backspace" [(new-uuid) #(del-fn v)]}
;;                           (row-fn v)
;;                           [:button {:on-click #(del-fn v)} "-"]])
;;         del-un (fn [k] (swap! unsaved-data (filter #(not= k (% 0)))))
;;         unsaved-list-row (fn [v] [hotkeys {"alt+backspace" [(new-uuid) #(del-un v)]}
;;                                   (row-fn v)
;;                                   [:button {:on-click #(del-un v)} "-"]])]
;;    [:div
;;     [hotkeys {"enter" [(new-uuid) add-fn]}
;;      (if (not-empty data)
;;        (->> data
;;             (map list-row)
;;             (into [:div]))
;;        empty-message)]
;;     [:button {:on-click add-fn} "Add"]]))