(ns factor.view.settings
  (:require [factor.components :as c]
            [re-frame.core :refer [dispatch subscribe]]
            [factor.world :as w]
            [reagent.core :as reagent]))

(defn navbar [] [c/navbar])

(defn world-loader
  []
  (reagent/with-let [v (reagent/atom "")]
    [:<> 
     ;; TODO -- update textarea to extract value from on-change within component
     ;; instead of having to do the .-target .-value jazz everywhere
     [c/textarea {:on-change #(->> %
                                  (.-target)
                                  (.-value)
                                  (reset! v))}]
     [c/button {:text "Import"
                :intent :warning
                :on-click #(dispatch [:world-reset (w/str->world @v)])}]]))

(defn page []
  [:div.card-stack
   [c/card-lg
    [c/form-group {:label "Export (Copy World Data)"}
     [c/textarea {:value (w/world->str @(subscribe [:world-data])) :read-only true}]]]
   [c/card-lg
    [c/form-group {:label "Import (Paste World Data)"}
     [world-loader]]]
   [c/card-lg
    [c/form-group {:label "Delete All Data"}
     [c/button {:on-click #(dispatch [:world-reset w/empty-world])
                :text "RESET WORLD"
                :intent :danger}]]]])
