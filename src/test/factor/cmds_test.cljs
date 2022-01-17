(ns factor.cmds-test
  (:require [factor.cmds :as cmds]
            [factor.schema :as schema]
            [factor.subs :as subs]
            [factor.events :as events]
            [cljs.test :refer [deftest is]]
            [malli.core :refer [validate explain]]
            [malli.error :refer [humanize]]
            [re-frame.registrar :refer [get-handler]]))

(deftest all-cmds-are-valid
  (doseq [cmd (cmds/all-cmds)]
    (is (validate schema/Command cmd) (humanize (explain schema/Command cmd)))))

; Note -- relies on re-frame's internal get-handler function
; not guaranteed to work across re-frame versions
(defn assert-handler [kind id]
  (when id
    (is (some? (get-handler kind id)) (str (name kind) " " id " should be defined"))))

(defn assert-sub [id] (assert-handler :sub id))
(defn assert-ev  [id] (assert-handler :event id))

(deftest all-cmd-subs-are-defined
  (subs/reg-all)
  (events/reg-all)
  (doseq [{:keys [disabled-sub params ev]} (cmds/all-cmds)]
    (assert-sub (first disabled-sub))
    (assert-ev (first ev))
    (doseq [{:keys [choice-sub]} params]
      (assert-sub (first choice-sub)))))