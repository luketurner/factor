(ns factor.components.menus
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [with-let]]
            [factor.components :as c]
            [factor.util :refer [ipairs]]))


(defn open-factory-id
  []
  (second @(subscribe [:page-route])))

(defn first-route-piece
  []
  (first @(subscribe [:page-route])))

(defn open-factory-menu-item
  []
  (with-let [open-factory #(dispatch [:update-route [:factory %]])]
    (let [ids->names @(subscribe [:factory-ids->names])
          current-id (open-factory-id)]
      (into [c/menu-item {:text "Open factory..."}]
            (for [[id name] ids->names]
              [c/menu-item {:text name
                            :label (subs id 0 3)
                            :key id
                            :disabled (= id current-id)
                            :on-click (reagent/partial open-factory id)}])))))

(defn delete-factory-menu-item
  []
  (with-let [delete-factory #(dispatch [:delete-factories [%]])]
    (let [ids->names @(subscribe [:factory-ids->names])]
      (into [c/menu-item {:text "Delete factory..."}]
            (for [[id name] ids->names]
              [c/menu-item {:text name :label (subs id 0 3) :key id
                            :on-click (reagent/partial delete-factory id)}])))))

(defn create-factory-menu-item
  []
  (with-let [create-factory #(dispatch [:create-factory])]
    [c/menu-item {:text "New factory"
                  :on-click create-factory}]))

(defn open-filter-view-menu-item
  []
  (with-let [open-filter-view #(dispatch [:update-route [:factory % :filters]])]
    (let [id (open-factory-id)]
      [c/menu-item {:text "Open factory filters..."
                    :disabled (nil? id)
                    :on-click (reagent/partial open-filter-view id)}])))

(defn open-debug-view-menu-item
  []
  (with-let [open-debug-view #(dispatch [:update-route [:factory % :debug]])]
    (let [id (open-factory-id)]
      [c/menu-item {:text "Open debug view..."
                    :disabled (nil? id)
                    :on-click (reagent/partial open-debug-view id)}])))

(defn undo-menu-item
  []
  [c/menu-item {:text "Undo"
                :disabled (not @(subscribe [:undos?]))
                :on-click (reagent/partial dispatch [:undo])}])

(defn redo-menu-item
  []
  [c/menu-item {:text "Redo"
                :disabled (not @(subscribe [:redos?]))
                :on-click (reagent/partial dispatch [:redo])}])

(defn undo-list-menu-item
  []
  (with-let [undo #(dispatch [:undo %])]
    (let [undos @(subscribe [:undo-explanations])]
      (into [c/menu-item {:text "Undo..." :disabled (empty? undos)}]
            (for [[text ix] (ipairs undos)]
              [c/menu-item {:text text :key ix :label (inc ix)
                            :on-click (reagent/partial undo (inc ix))}])))))

(defn redo-list-menu-item
  []
  (with-let [redo #(dispatch [:redo %])]
    (let [redos @(subscribe [:redo-explanations])]
      (into [c/menu-item {:text "Redo..." :disabled (empty? redos)}]
            (for [[text ix] (ipairs redos)]
              [c/menu-item {:text text :key ix :label (inc ix)
                            :on-click (reagent/partial redo (inc ix))}])))))

(defn item-editor-menu-item
  []
  [c/menu-item {:text "Open item editor..."
                :disabled (= :items (first-route-piece))
                :on-click (reagent/partial dispatch [:update-route [:items]])}])

(defn recipe-editor-menu-item
  []
  [c/menu-item {:text "Open recipe editor..."
                :disabled (= :recipes (first-route-piece))
                :on-click (reagent/partial dispatch [:update-route [:recipes]])}])

(defn machine-editor-menu-item
  []
  [c/menu-item {:text "Open machine editor..."
                :disabled (= :machines (first-route-piece))
                :on-click (reagent/partial dispatch [:update-route [:machines]])}])

(defn import-export-world-menu-item
  []
  [c/menu-item {:text "Import/export world..."
                :disabled (= :settings (first-route-piece))
                :on-click (reagent/partial dispatch [:update-route [:settings]])}])


(defn item-rate-menu-item
  []
  (with-let [update-unit #(dispatch [:set-unit :item-rate %])]
    (let [current-unit @(subscribe [:unit :item-rate])]
      (into [c/menu-item {:text "Item rate"}]
            (for [unit ["items/sec" "items/min"]]
              [c/menu-item {:text unit
                            :key unit
                            :disabled (= unit current-unit)
                            :on-click (reagent/partial update-unit unit)}])))))

(defn energy-menu-item
  []
  (with-let [update-unit #(dispatch [:set-unit :energy %])]
    (let [current-unit @(subscribe [:unit :energy])]
      (into [c/menu-item {:text "Energy"}]
            (for [unit ["J"]]
              [c/menu-item {:text unit
                            :key unit
                            :disabled (= unit current-unit)
                            :on-click (reagent/partial update-unit unit)}])))))

(defn power-menu-item
  []
  (with-let [update-unit #(dispatch [:set-unit :power %])]
    (let [current-unit @(subscribe [:unit :power])]
      (into [c/menu-item {:text "Power"}]
            (for [unit ["W"]]
              [c/menu-item {:text unit
                            :key unit
                            :disabled (= unit current-unit)
                            :on-click (reagent/partial update-unit unit)}])))))

(defn delete-world-menu-item
  []
  [c/menu-item {:text "Reset world"
                :on-click (reagent/partial dispatch [:world-reset])}])

(defn factory-menu []
  [c/popup-menu {:label "Factory..." :desc "Open factory menu" :hotkey "f"}
   [create-factory-menu-item]
   [open-factory-menu-item]
   [delete-factory-menu-item]
   [c/menu-divider]
   [open-filter-view-menu-item]
   [open-debug-view-menu-item]])

(defn edit-menu []
  [c/popup-menu {:label "Edit..." :desc "Open edit menu" :hotkey "e"}
   [undo-menu-item]
   [undo-list-menu-item]
   [redo-menu-item]
   [redo-list-menu-item]])

(defn world-menu []
  [c/popup-menu {:label "World..." :desc "Open world menu" :hotkey "w"}
   [item-editor-menu-item]
   [machine-editor-menu-item]
   [recipe-editor-menu-item]
   [import-export-world-menu-item]
   [delete-world-menu-item]])

(defn settings-menu []
  [c/popup-menu {:label "Settings..." :desc "Open settings menu" :hotkey "s"}
   [c/menu-divider {:title "Units"}]
   [item-rate-menu-item]
   [energy-menu-item]
   [power-menu-item]])

(defn help-menu-item
  []
  [c/menu-item {:text "Open help..."
                :icon :help
                :disabled (= :help (first-route-piece))
                :on-click (reagent/partial dispatch [:update-route [:help]])}])

(defn help-menu []
  [c/popup-menu {:label "Help..." :desc "Open help menu" :hotkey "h"}
   [help-menu-item]
   [c/menu-divider {:title "External Links"}]
   [c/menu-item {:href "https://github.com/luketurner/factor" :target :_blank :text "Github" :icon :git-repo}]
   [c/menu-item {:href "https://git.sr.ht/~luketurner/factor" :target :_blank :text "Sourcehut" :icon :git-repo}]])

(defn menus []
  [:<>
   [factory-menu]
   [edit-menu]
   [world-menu]
   [settings-menu]
   [help-menu]])