(ns factor.pgraph-test
  (:require [factor.world :as world]
            [cljs.test :refer [deftest is]]
            [factor.pgraph :as pgraph]))

(def test-world {:factories {"testfactory" (world/new-factory {:id "testfactory"})}
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
                         "water" (world/new-item {:id "water" :name "water"})}
                 :machines {"smelter" (world/new-machine {:id "smelter" :name "smelter"})
                            "assembler" (world/new-machine {:id "assembler" :name "assembler"})
                            "pump" (world/new-machine {:id "pump" :name "pump"})}
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
                                                      :machines #{"assembler"}})}})

(deftest pgraph-try-satisfy-node-should-satisfy-root-node
  (let [w           test-world
        factory     (get-in w [:factories "testfactory"])
        factory     (assoc factory :desired-output {"iron-plate" 123})
        pg          (pgraph/empty-pgraph-for-factory w factory)
        [actual-pg _] (pgraph/try-satisfy-node pg :end)]
    (is (= (:edges actual-pg) {:start {1 {"iron-ingot" 246}}
                               1 {:end {"iron-plate" 123}}})
        "should have an edge from root to the node, and from the node to root")
    (is (= (get-in actual-pg [:nodes :start]) {:output {"iron-ingot" 246}})
        "should have updated the :start node's output")
    (is (= (get-in actual-pg [:nodes :end]) {:input {"iron-plate" 123}})
        "should have updated the :end node's input")
    (is (= (get-in actual-pg [:nodes 1]) {:id 1
                                          :recipe (get-in w [:recipes "iron-plate"])
                                          :recipe-ratio 123
                                          :input  {"iron-ingot" 246}
                                          :output {"iron-plate" 123}})
        "should have added a node that crafts iron plates")))

(deftest pgraph-try-satisfy-node-should-satisfy-second-node
  (let [w           test-world
        factory     (get-in w [:factories "testfactory"])
        factory     (assoc factory :desired-output {"iron-plate" 123})
        pg          (pgraph/empty-pgraph-for-factory w factory)
        [actual-pg _] (pgraph/try-satisfy-node pg :end)
        [actual-pg _] (pgraph/try-satisfy-node actual-pg 1)]
    (is (= (:edges actual-pg) {:start {2 {"iron-ore" 246}}
                               2      {1 {"iron-ingot" 246}}
                               1      {:end {"iron-plate" 123}}})
        "should have three edges")
    (is (= (get-in actual-pg [:nodes :start]) {:output {"iron-ore" 246}})
        "should have updated the :start node's output")
    (is (= (get-in actual-pg [:nodes :end]) {:input {"iron-plate" 123}})
        "should have updated the :end node's input")
    (is (= (get-in actual-pg [:nodes 1]) {:id 1
                                          :recipe (get-in w [:recipes "iron-plate"])
                                          :recipe-ratio 123
                                          :input  {"iron-ingot" 246}
                                          :output {"iron-plate" 123}})
        "should have added a node that crafts iron plates")
    (is (= (get-in actual-pg [:nodes 2]) {:id 2
                                          :recipe (get-in w [:recipes "iron-ingot"])
                                          :recipe-ratio 246
                                          :input  {"iron-ore" 246}
                                          :output {"iron-ingot" 246}})
        "should have added a node that crafts iron ingots")))

(deftest pgraph-try-satisfy-node-should-handle-partial-satisfaction
  (let [w           test-world
        factory     (get-in w [:factories "testfactory"])
        factory     (assoc factory :desired-output {"cable" 1})
        pg          (pgraph/empty-pgraph-for-factory w factory)
        [actual-pg _] (pgraph/try-satisfy-node pg :end)
        [actual-pg _] (pgraph/try-satisfy-node actual-pg 1)]
    (is (= (:edges actual-pg) {:start {2 {"copper-ore" 1}}
                               2 {1 {"copper-wire" 2}
                                  :end {"copper-wire" 2}}
                               1 {:end {"cable" 1}}})
        "should have three edges")
    (is (= (get-in actual-pg [:nodes :start]) {:output {"copper-ore" 1}})
        "should have updated the :start node")
    (is (= (get-in actual-pg [:nodes :end]) {:input {"copper-wire" 2
                                                     "cable" 1}})
        "should have updated the :end node")
    (is (= (get-in actual-pg [:nodes 1]) {:id 1
                                          :recipe (get-in w [:recipes "cable"])
                                          :recipe-ratio 1
                                          :input  {"copper-wire" 2}
                                          :output {"cable" 1}})
        "should have added a node that crafts iron plates")
    (is (= (get-in actual-pg [:nodes 2]) {:id 2
                                          :recipe (get-in w [:recipes "copper-wire"])
                                          :recipe-ratio 1
                                          :input  {"copper-ore" 1}
                                          :output {"copper-wire" 4}})
        "should have added a node that crafts iron ingots")))

(deftest pgraph-try-satisfy-should-satisfy-multiple-nodes
  (let [w           test-world
        factory     (get-in w [:factories "testfactory"])
        factory     (assoc factory :desired-output {"iron-plate" 123})
        pg          (pgraph/empty-pgraph-for-factory w factory)
        actual-pg   (pgraph/try-satisfy pg)]
    (is (= (:edges actual-pg) {:start {2 {"iron-ore" 246}}
                               2     {1 {"iron-ingot" 246}}
                               1     {:end {"iron-plate" 123}}})
        "should have three edges")
    (is (= (get-in actual-pg [:nodes :start]) {:output {"iron-ore" 246}})
        "should have updated the :start node's output")
    (is (= (get-in actual-pg [:nodes :end]) {:input {"iron-plate" 123}})
        "should have updated the :end node's input")
    (is (= (get-in actual-pg [:nodes 1]) {:id 1
                                          :recipe (get-in w [:recipes "iron-plate"])
                                          :recipe-ratio 123
                                          :input  {"iron-ingot" 246}
                                          :output {"iron-plate" 123}})
        "should have added a node that crafts iron plates")
    (is (= (get-in actual-pg [:nodes 2]) {:id 2
                                          :recipe (get-in w [:recipes "iron-ingot"])
                                          :recipe-ratio 246
                                          :input  {"iron-ore" 246}
                                          :output {"iron-ingot" 246}})
        "should have added a node that crafts iron ingots")))

(deftest pgraph-try-satisfy-should-reuse-existing-output-where-possible
  (let [w           test-world
        factory     (get-in w [:factories "testfactory"])
        factory     (assoc factory :desired-output {"concrete" 123 "steel-ingot" 123})
        pg          (pgraph/empty-pgraph-for-factory w factory)
        actual-pg   (pgraph/try-satisfy pg)]
    (is (= (:edges actual-pg) {:start {1     {"iron-ore" 123 "coal" 246}}
                               1     {:end {"steel-ingot" 123}
                                      2     {"slag" 123}}
                               2     {:end {"concrete" 123}}
                               3     {2     {"water" 1230}}})
        "should feed slag directly from one node to the next")))