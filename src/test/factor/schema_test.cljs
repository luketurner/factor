(ns factor.schema-test
  (:require [factor.schema :as schema]
            [cljs.test :refer [deftest is]]))

(deftest can-make-app-db
  (is (some? (schema/make schema/AppDb nil)) "make AppDb should return a value"))