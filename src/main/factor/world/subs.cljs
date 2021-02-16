(ns factor.world.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :world-data (fn [db _] (get db :world)))
