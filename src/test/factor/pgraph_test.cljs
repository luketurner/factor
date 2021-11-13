(ns factor.pgraph-test
  (:require [factor.world :as world]
            [cljs.test :refer [deftest is]]
            [factor.pgraph :as pgraph]))

(def test-world {:factories {"testfactory" (world/new-factory {:id "testfactory"
                                                            ;;    :hard-denied-machines #{"pump"}
                                                            ;;    :soft-denied-machines #{"smelter"}
                                                            ;;    :hard-denied-recipes #{"iron-ingot"}
                                                            ;;    :soft-denied-recipes #{"steel-ingot"}
                                                            ;;    :hard-denied-items #{"water"}
                                                            ;;    :soft-denied-items #{"water"}
                                                               :desired-output {"iron-ingot" 10}})}
                 :items {"iron-ore" (world/new-item {:id "iron-ore" :name "iron-ore"})
                         "iron-ingot" (world/new-item {:id "iron-ingot" :name "iron-ingot"})
                         "coal" (world/new-item {:id "coal" :name "coal"})
                         "copper-ore" (world/new-item {:id "copper-ore" :name "copper-ore"})
                         "copper-wire" (world/new-item {:id "copper-wire" :name "copper-wire"})
                         "iron-plate" (world/new-item {:id "iron-plate" :name "iron-plate"})
                         "steel-ingot" (world/new-item {:id "steel-ingot" :name "steel-ingot"})
                         "steel-beam" (world/new-item {:id "steel-beam" :name "steel-beam"})
                         "cable" (world/new-item {:id "cable" :name "cable"})
                         "slag" (world/new-item {:id "slag" :name "slag"})
                         "water" (world/new-item {:id "water" :name "water"})
                         "sulfuric-acid" (world/new-item {:id "sulfuric-acid" :name "sulfuric-acid"})
                         "iron-ore-slurry" (world/new-item {:id "iron-ore-slurry" :name "iron-ore-slurry"})
                         "purified-iron-ore" (world/new-item {:id "purified-iron-ore" :name "purified-iron-ore"})}
                 :machines {"smelter" (world/new-machine {:id "smelter" :name "smelter" :power 1 :speed 1})
                            "assembler" (world/new-machine {:id "assembler" :name "assembler" :power 1 :speed 1})
                            "pump" (world/new-machine {:id "pump" :name "pump" :power 1 :speed 1})}
                 :recipes {"iron-ingot" (world/new-recipe {:id "iron-ingot"
                                                           :name "iron-ingot"
                                                           :input {"iron-ore" 1}
                                                           :output {"iron-ingot" 1}
                                                           :machines #{"smelter"}})
                           "steel-ingot" (world/new-recipe {:id "steel-ingot"
                                                            :name "steel-ingot"
                                                            :input {"iron-ore" 1 "coal" 2}
                                                            :output {"steel-ingot" 1 "slag" 1}
                                                            :machines #{"smelter"}})
                           "iron-plate" (world/new-recipe {:id "iron-plate"
                                                           :name "iron-plate"
                                                           :input {"iron-ingot" 2}
                                                           :output {"iron-plate" 1}
                                                           :machines #{"assembler"}})
                           "steel-beam" (world/new-recipe {:id "steel-beam"
                                                           :name "steel-beam"
                                                           :input {"steel-ingot" 3}
                                                           :output {"steel-beam" 2}
                                                           :machines #{"assembler"}})
                           "copper-wire" (world/new-recipe {:id "copper-wire"
                                                            :name "copper-wire"
                                                            :input {"copper-ore" 1}
                                                            :output {"copper-wire" 4}
                                                            :machines #{"assembler"}})
                           "concrete" (world/new-recipe {:id "concrete"
                                                         :name "concrete"
                                                         :input {"slag" 1 "water" 10}
                                                         :output {"concrete" 1}
                                                         :machines #{"assembler"}})
                           "water" (world/new-recipe {:id "water"
                                                      :name "water"
                                                      :input {}
                                                      :output {"water" 10}
                                                      :machines #{"pump"}})
                           "cable" (world/new-recipe {:id "cable"
                                                      :name "cable"
                                                      :input {"copper-wire" 2}
                                                      :output {"cable" 1}
                                                      :machines #{"assembler"}})
                           "iron-ore-slurry" (world/new-recipe {:id "iron-ore-slurry"
                                                      :name "iron-ore-slurry"
                                                      :input {"iron-ore" 2
                                                              "sulfuric-acid" 10}
                                                      :output {"iron-ore-slurry" 10}
                                                      :machines #{"assembler"}})
                           "iron-slurry-refining" (world/new-recipe {:id "iron-slurry-refining"
                                                                     :name "iron-slurry-refining"
                                                                     :input {"iron-ore-slurry" 10}
                                                                     :output {"purified-iron-ore" 3
                                                                              "sulfuric-acid" 10}
                                                                     :machines #{"assembler"}})}})

(deftest pgraph-for-factory-should-satisfy-desired-output
  (let [w           test-world
        factory     (get-in w [:factories "testfactory"])
        factory     (assoc factory :desired-output {"iron-ingot" 123})
        pg          (pgraph/pgraph-for-factory w factory)]
    (is (= (pgraph/missing-input pg) {"iron-ore" 123})
        "should be missing iron ore")
    (is (empty? (pgraph/excess-output pg))
        "should have no excess output")
    (is (= (count (pgraph/all-nodes pg)) 4)
        "should have 4 nodes (missing, excess, desired, ingot crafting)")))

(deftest pgraph-for-factory-should-satisfy-desired-output-iteratively
  (let [w           test-world
        factory     (get-in w [:factories "testfactory"])
        factory     (assoc factory :desired-output {"iron-plate" 123})
        pg          (pgraph/pgraph-for-factory w factory)]
    (is (= (pgraph/missing-input pg) {"iron-ore" 246})
        "should be missing iron ore")
    (is (empty? (pgraph/excess-output pg))
        "should have no excess output")
    (is (= (count (pgraph/all-nodes pg)) 5)
        "should have 5 nodes (missing, excess, desired, plate crafting, ingot crafting)")))

(deftest pgraph-for-factory-should-handle-partial-satisfaction
  (let [w           test-world
        factory     (get-in w [:factories "testfactory"])
        factory     (assoc factory :desired-output {"cable" 1})
        pg          (pgraph/pgraph-for-factory w factory)]
    (is (= (pgraph/missing-input pg) {"copper-ore" 1})
        "should be missing copper ore")
    (is (= (pgraph/excess-output pg) {"copper-wire" 2})
        "should produce excess copper wire")
    (is (= (count (pgraph/all-nodes pg)) 5)
        "should have 5 nodes (missing, excess, desired, wire crafting, cable crafting)")))

(deftest pgraph-for-factory-should-reuse-existing-output-where-possible
  (let [w           test-world
        factory     (get-in w [:factories "testfactory"])
        factory     (assoc factory :desired-output {"concrete" 123 "steel-ingot" 123})
        pg          (pgraph/pgraph-for-factory w factory)]
    (is (= (pgraph/missing-input pg) {"iron-ore" 123 "coal" 246})
        "should be missing iron ore and coal")
    (is (empty? (pgraph/excess-output pg))
        "should have no excess outputs")
    (is (= (count (pgraph/all-nodes pg)) 6)
        "should have 6 nodes (missing, excess, desired, concrete crafting, iron ingot crafting, steel ingot crafting)")))

(deftest pgraph-for-factory-should-support-circular-recipes
  (let [w           test-world
        factory     (get-in w [:factories "testfactory"])
        factory     (assoc factory :desired-output {"purified-iron-ore" 3})
        pg          (pgraph/pgraph-for-factory w factory)]
    (is (= (pgraph/missing-input pg) {"iron-ore" 2})
        "should be missing iron ore")
    (is (empty? (pgraph/excess-output pg))
        "should have no excess outputs")
    (is (= (count (pgraph/all-nodes pg)) 5)
        "should have 5 nodes (missing, excess, desired, slurry crafting, purify crafting)")))