(ns factor.pgraph-test
  (:require [factor.schema :as sc]
            [cljs.test :refer [deftest is]]
            [factor.pgraph :as pgraph]))

(def test-world
  (sc/make sc/World {:factories {"testfactory" {:id "testfactory"
                                                :desired-output {"iron-ingot" 10}}}
                     :items {"iron-ore"  {:id "iron-ore" :name "iron-ore"}
                             "iron-ingot"  {:id "iron-ingot" :name "iron-ingot"}
                             "coal"  {:id "coal" :name "coal"}
                             "copper-ore"  {:id "copper-ore" :name "copper-ore"}
                             "copper-wire"  {:id "copper-wire" :name "copper-wire"}
                             "iron-plate"  {:id "iron-plate" :name "iron-plate"}
                             "steel-ingot"  {:id "steel-ingot" :name "steel-ingot"}
                             "steel-beam"  {:id "steel-beam" :name "steel-beam"}
                             "cable"  {:id "cable" :name "cable"}
                             "slag"  {:id "slag" :name "slag"}
                             "water"  {:id "water" :name "water"}
                             "sulfuric-acid"  {:id "sulfuric-acid" :name "sulfuric-acid"}
                             "iron-ore-slurry"  {:id "iron-ore-slurry" :name "iron-ore-slurry"}
                             "purified-iron-ore"  {:id "purified-iron-ore" :name "purified-iron-ore"}}
                     :machines {"smelter"  {:id "smelter" :name "smelter" :power 1 :speed 1}
                                "assembler"  {:id "assembler" :name "assembler" :power 1 :speed 1}
                                "pump"  {:id "pump" :name "pump" :power 1 :speed 1}}
                     :recipes {"iron-ingot" {:id "iron-ingot"
                                             :name "iron-ingot"
                                             :input {"iron-ore" 1}
                                             :output {"iron-ingot" 1}
                                             :machines #{"smelter"}}
                               "steel-ingot" {:id "steel-ingot"
                                              :name "steel-ingot"
                                              :input {"iron-ore" 1 "coal" 2}
                                              :output {"steel-ingot" 1 "slag" 1}
                                              :machines #{"smelter"}}
                               "iron-plate" {:id "iron-plate"
                                             :name "iron-plate"
                                             :input {"iron-ingot" 2}
                                             :output {"iron-plate" 1}
                                             :machines #{"assembler"}}
                               "steel-beam" {:id "steel-beam"
                                             :name "steel-beam"
                                             :input {"steel-ingot" 3}
                                             :output {"steel-beam" 2}
                                             :machines #{"assembler"}}
                               "copper-wire" {:id "copper-wire"
                                              :name "copper-wire"
                                              :input {"copper-ore" 1}
                                              :output {"copper-wire" 4}
                                              :machines #{"assembler"}}
                               "concrete" {:id "concrete"
                                           :name "concrete"
                                           :input {"slag" 1 "water" 10}
                                           :output {"concrete" 1}
                                           :machines #{"assembler"}}
                               "water" {:id "water"
                                        :name "water"
                                        :input {}
                                        :output {"water" 10}
                                        :machines #{"pump"}}
                               "cable" {:id "cable"
                                        :name "cable"
                                        :input {"copper-wire" 2}
                                        :output {"cable" 1}
                                        :machines #{"assembler"}}
                               "iron-ore-slurry" {:id "iron-ore-slurry"
                                                  :name "iron-ore-slurry"
                                                  :input {"iron-ore" 2
                                                          "sulfuric-acid" 10}
                                                  :output {"iron-ore-slurry" 10}
                                                  :machines #{"assembler"}}
                               "iron-slurry-refining" {:id "iron-slurry-refining"
                                                       :name "iron-slurry-refining"
                                                       :input {"iron-ore-slurry" 10}
                                                       :output {"purified-iron-ore" 3
                                                                "sulfuric-acid" 10}
                                                       :machines #{"assembler"}}}}))

(defn build-test-pg
  [desired-output filter]
  (let [{:keys [machines recipes]} test-world
        recipe-index (pgraph/recipe-index (vals recipes))]
   (pgraph/pgraph {:get-recipes-with-output #(map recipes (get-in recipe-index [:output %]))
                   :get-machine #(get machines %)
                   :desired-output desired-output
                   :filter filter})))

(deftest pgraph-for-factory-should-satisfy-desired-output
  (let [pg (build-test-pg {"iron-ingot" 123} {})]
    (is (= (pgraph/missing-input pg) {"iron-ore" 123})
        "should be missing iron ore")
    (is (empty? (pgraph/excess-output pg))
        "should have no excess output")
    (is (= (count (pgraph/all-nodes pg)) 4)
        "should have 4 nodes (missing, excess, desired, ingot crafting)")))

(deftest pgraph-for-factory-should-satisfy-desired-output-iteratively
  (let [pg (build-test-pg {"iron-plate" 123} {})]
    (is (= (pgraph/missing-input pg) {"iron-ore" 246})
        "should be missing iron ore")
    (is (empty? (pgraph/excess-output pg))
        "should have no excess output")
    (is (= (count (pgraph/all-nodes pg)) 5)
        "should have 5 nodes (missing, excess, desired, plate crafting, ingot crafting)")))

(deftest pgraph-for-factory-should-handle-partial-satisfaction
  (let [pg (build-test-pg {"cable" 1} {})]
    (is (= (pgraph/missing-input pg) {"copper-ore" 1})
        "should be missing copper ore")
    (is (= (pgraph/excess-output pg) {"copper-wire" 2})
        "should produce excess copper wire")
    (is (= (count (pgraph/all-nodes pg)) 5)
        "should have 5 nodes (missing, excess, desired, wire crafting, cable crafting)")))

(deftest pgraph-for-factory-should-reuse-existing-output-where-possible
  (let [pg (build-test-pg {"concrete" 123 "steel-ingot" 123} {})]
    (is (= (pgraph/missing-input pg) {"iron-ore" 123 "coal" 246})
        "should be missing iron ore and coal")
    (is (empty? (pgraph/excess-output pg))
        "should have no excess outputs")
    (is (= (count (pgraph/all-nodes pg)) 6)
        "should have 6 nodes (missing, excess, desired, concrete crafting, iron ingot crafting, steel ingot crafting)")))

(deftest pgraph-for-factory-should-support-circular-recipes
  (let [pg (build-test-pg {"purified-iron-ore" 3} {})]
    (is (= (pgraph/missing-input pg) {"iron-ore" 2})
        "should be missing iron ore")
    (is (empty? (pgraph/excess-output pg))
        "should have no excess outputs")
    (is (= (count (pgraph/all-nodes pg)) 5)
        "should have 5 nodes (missing, excess, desired, slurry crafting, purify crafting)")))