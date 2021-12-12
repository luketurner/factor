(ns factor.schema
  (:require [malli.core :as m]
            [malli.transform :as mt]
            [cljs.core.match :refer [match]]))

;; schemas

(def Id :string)

(def Name :string)

(def NumericDate [number? {:default 0}])

(def Quantity [number? {:default 0}])
(def Ratio [number? {:default 1}])

(def Qmap [:map-of Id Quantity])

(def Filter
  [:map {:closed true}
   [:hard-denied-machines [:set Id]]
   [:soft-denied-machines [:set Id]]
   [:hard-denied-recipes [:set Id]]
   [:soft-denied-recipes [:set Id]]
   [:hard-denied-items [:set Id]]
   [:soft-denied-items [:set Id]]
   [:hard-allowed-machines [:set Id]]
   [:soft-allowed-machines [:set Id]]
   [:hard-allowed-recipes [:set Id]]
   [:soft-allowed-recipes [:set Id]]
   [:hard-allowed-items [:set Id]]
   [:soft-allowed-items [:set Id]]])


(def Factory
  [:map {:closed true}
   [:id Id]
   [:name {:default "Unnamed Factory"} Name]
   [:desired-output Qmap]
   [:output Qmap]
   [:input Qmap]
   [:recipes Qmap]
   [:machines Qmap]
   [:filter Filter]
   [:created-at NumericDate]])

(def Item [:map {:closed true}
           [:id Id]
           [:name {:default "Unnamed Item"} Name]
           [:created-at number?]])

(def Machine [:map {:closed true}
              [:id Id]
              [:name {:default "Unnamed Machine"} Name]
              [:power Quantity]
              [:speed Ratio]
              [:created-at NumericDate]])

(def Recipe [:map {:closed true}
             [:id Id]
             [:name {:default "Unnamed Recipe"} Name]
             [:input Qmap]
             [:output Qmap]
             [:catalysts Qmap]
             [:machines [:vector Id]]
             [:duration Ratio]
             [:created-at NumericDate]])

(def Factories [:map-of Id Factory])
(def Items [:map-of Id Item])
(def Machines [:map-of Id Machine])
(def Recipes [:map-of Id Recipe])

(def World
  [:map {:closed true}
   [:factories Factories]
   [:items Items]
   [:machines Machines]
   [:recipes Recipes]])

(def Config
  [:map {:closed true}
   [:unit [:map {:closed true}
           [:item-rate {:default "items/sec"} [:enum "items/sec" "items/min"]]
           [:energy    {:default "J"}         [:enum "J"]]
           [:power     {:default "W"}         [:enum "W"]]]]])

(def PageRoute
  [:or {:default [:home]
        :decode/json #(match %
                       ([(:or "home" "items" "recipes" "machines" "settings")] :seq) [(keyword (first %))]
                       (["factory" id] :seq)   [:factory id]
                       (["factory" id x] :seq) [:factory id (keyword x)]
                       :else [:notfound])}
   [:tuple [:enum :notfound :home :items :machines :recipes :settings]]
   [:tuple [:= :factory] Id]
   [:tuple [:= :factory] Id [:enum :debug :filters]]])

(def Ui 
 [:map {:closed true}
  [:selected-objects [:vector Id]]
  [:page-route PageRoute]
  [:omnibar-state
   [:map {:closed true}
    [:mode {:default :closed}
     [:enum :closed :command-palette
      :open-factory :create-factory :delete-factory]]
    [:query :string]]]])

(def AppDb
  [:map {:closed true}
   [:world World]
   [:config Config]
   [:ui Ui]])

;; transformers

(def default-value-tranformer
  (mt/transformer
   (mt/default-value-transformer
     {:defaults {:map-of (constantly {})
                 :map (constantly {})
                 :set (constantly #{})
                 :vector (constantly [])
                 :number (constantly 0)
                 :string (constantly "")}})
   (mt/strip-extra-keys-transformer)))

(def json-transformer
  (mt/transformer
   (mt/key-transformer {:encode name :decode keyword})
   mt/json-transformer
   default-value-tranformer))

(def edn-transformer
  default-value-tranformer)

;; helper functions

(defn json-encode [schema v] (m/encode schema v json-transformer))
(defn json-decode [schema v] (m/decode schema v json-transformer))
(defn edn-encode  [schema v] (m/encode schema v edn-transformer))
(defn edn-decode  [schema v] (m/decode schema v edn-transformer))
(defn make [schema v] (m/encode schema v default-value-tranformer))
