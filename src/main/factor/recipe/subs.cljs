(ns factor.recipe.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :recipe (fn [db [_ id]] (get-in db [:world :recipes id])))
(reg-sub :recipe-ids (fn [db] (-> db (get-in [:world :recipes]) (keys))))
(reg-sub :recipe-count (fn [db] (-> db (get-in [:world :recipes]) (count))))

(reg-sub :recipe-is-expanded (fn [db [_ id]] (get-in db [:ui :recipes :expanded id] false)))
