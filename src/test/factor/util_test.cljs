(ns factor.util-test
  (:require [factor.util :as util]
            [cljs.test :refer [deftest is]]))

(deftest generated-uuid-is-string
  (is (string? (util/new-uuid)) "new-uuid should return a string"))