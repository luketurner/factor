(ns factor.world
  (:require [clojure.edn :as edn]
            [factor.util :refer [new-uuid dissoc-in]]
            [medley.core :refer [filter-vals map-vals]]
            [com.rpl.specter :as s]))

(def empty-world {:items {} :machines {} :recipes {} :factories {}})

(defn update-item [world item]
  (-> world
      (assoc-in [:items (:id item)] item)))

(defn update-machine [world machine]
  (-> world
      (assoc-in [:machines (:id machine)] machine)))

(defn update-recipe [world recipe]
  (-> world
      (assoc-in [:recipes (:id recipe)] recipe)))

(defn recipe-for-item [recipes item-id]
  (some (fn [[k {:keys [output]}]]
          (when (contains? output item-id) k)) recipes))

(defn quantity-set+
  "Returns a QuantityMap representing the sum of the quantities in the provided QuantityMaps."
  [& ms]
  (apply merge-with + ms))

(defn quantity-set-
  "Returns a QuantityMap representing the difference of the QuantityMap `m` and one or more QuantityMaps `ms`.
   If a quantity goes negative, the quantity is removed from the map."
  [m & ms]
  (-> m
      (quantity-set+ (map-vals - (apply quantity-set+ ms)))
      (->> (filter-vals pos?))))

(defn quantity-set*
  "Multiplies all the values in the quantity set by a scalar ratio"
  [m n]
  (map-vals (partial * n) m))

(defn apply-recipe [factory recipe-id recipe times]
  (let [input-change (map-vals #(* % times) (:input recipe))
        output-change (map-vals #(* % times) (:output recipe))
        machine-id (first (:machines recipe))]
    (cond-> factory
      input-change (update :input quantity-set+ input-change)
      output-change (update :input quantity-set- output-change)
      output-change (update :output quantity-set+ output-change)
      machine-id (update-in [:machines machine-id] + times)
      true (update-in [:recipes recipe-id] + times))))

(defn unsatisfied-output [{:keys [output desired-output input]}]
  (->> output (quantity-set- desired-output) (filter-vals pos?) (quantity-set+ input)))

(defn satisfy-desired-machines [world factory]
  factory)

(defn recipe-satisfies? [recipe id-type id]
  (-> recipe (get id-type) (contains? id)))

(defn satisfying-recipe [world id-type id]
  (->> world
       (:recipes)
       (some #(when (recipe-satisfies? (second %) id-type id) %))))

(defn satisfying-recipe-for-set [world id-type ids]
  (some #(satisfying-recipe world id-type %) ids))

(defn satisfying-ratio [goal base]
  (apply max (for [[k v] base]
               (when (contains? goal k)
                 (/ (goal k) v)))))

(defn recipe-matches-qm [recipe qm]
  (some true? (for [[k v] qm] (contains? (:output recipe) k))))

(defn empty-pgraph-for-factory
  [world {:keys [desired-output]}]
  {:world world
   :edges {:root {:root desired-output}}
   :next-node-id 1
   :nodes {:root {:input  desired-output
                  :output desired-output}}})

(defn pgraph-update-node
  [pg id updater]
  (let [current (get-in pg [:nodes id])
        updated (if (fn? updater) (updater current) updater)]
    (if (nil? updated)
      (update pg :nodes dissoc id)
      (assoc-in pg [:nodes id] updated))))

(defn pgraph-update-edge
  [pg lid rid updater]
  (let [current (get-in pg [:edges lid rid])
        updated (if (fn? updater) (updater current) updater)]
    (if (= 0 (apply + (vals updated)))
      (update pg :edges medley.core/dissoc-in [lid rid])
      (assoc-in pg [:edges lid rid] updated))))

(defn pgraph-add-node
  [pg node]
  (let [id (:next-node-id pg)
        node (assoc node :id id)]
    [(-> pg
         (pgraph-update-node id node)
         (update :next-node-id inc)) node]))

(defn pgraph-get-edge
  [pg lid rid]
  (get-in pg [:edges lid rid]))

(defn pgraph-get-node
  [pg id]
  (get-in pg [:nodes id]))

(defn pgraph-connect-nodes
  "Connects the output of one node (called the LEFT node) to the input of another node (the RIGHT node).
   All the output of LEFT will be used to fulfill any missing input on RIGHT. Since all inputs of a node
   must be accounted for with an edge, the 'missing' input on RIGHT is all the input coming from the ROOT node.
   The LEFT node will connect itself between ROOT and RIGHT. However, because LEFT may produce more or less output
   than needed by RIGHT's input, additional connections to ROOT will be maintained. See the following diagram:
   
   before:    
           ROOT (input) -------> (input) RIGHT (output) -------> (output) ROOT
   
   after:
                                                             (any excess output from LEFT)
                                                  +-------------------------------------------------+
                                                  |                                                 V
           ROOT (input) -------> (input) LEFT (output) -------> (input) RIGHT (output) -------> (output) ROOT
                   |                                               ^
                   +-----------------------------------------------+
                    (any of RIGHT's input that LEFT doesn't supply)
   "
  [pg lid rid]
  (let [{left-out :output left-in :input} (pgraph-get-node pg lid)
        root=>right             (pgraph-get-edge pg :root rid)
        root=>right-unsatisfied (quantity-set- root=>right left-out)
        root=>right-satisfied   (quantity-set- root=>right root=>right-unsatisfied)
        left-out-unused         (quantity-set- left-out root=>right-satisfied)]
    (-> pg
        (pgraph-update-node :root       #(-> % 
                                             (update :input quantity-set- root=>right-satisfied)
                                             (update :input quantity-set+ left-in)
                                             (update :output quantity-set+ left-out-unused)))
        (pgraph-update-edge :root rid   root=>right-unsatisfied)
        (pgraph-update-edge lid   :root left-out-unused)
        (pgraph-update-edge :root lid   left-in)
        (pgraph-update-edge lid   rid   root=>right-satisfied))))

(defn pgraph-add-node-for-recipe
  [pg recipe ratio]
  (pgraph-add-node pg {:recipe recipe
                       :input  (quantity-set* (:input recipe)  (js/Math.ceil ratio))
                       :output (quantity-set* (:output recipe) (js/Math.ceil ratio))}))

(defn pgraph-matching-recipe-for-node
  [pg node-id]
  (let [{:keys [world]} pg
        needed-input    (pgraph-get-edge pg :root node-id)]
    (if-let [matching-recipe (s/select-first [(s/must :recipes) s/MAP-VALS #(recipe-matches-qm % needed-input)] world)]
      [matching-recipe (satisfying-ratio needed-input (:output matching-recipe))])))

(defn pgraph-try-satisfy-node [pg node-id]
  (if-let [matching-recipe (pgraph-matching-recipe-for-node pg node-id)]
    (let [[recipe ratio] matching-recipe
          [pg new-node] (pgraph-add-node-for-recipe pg recipe ratio)
          pg            (pgraph-connect-nodes pg (:id new-node) node-id)]
      [pg new-node])
    [pg nil]))

(defn pgraph-try-satisfy-any-node [pg]
  (let [[new-pg new-node] (->> pg
                           (:nodes)
                           (keys)
                           (map (partial pgraph-try-satisfy-node pg))
                           (filter (fn [[_ new-node]] (some? new-node)))
                           (first))]
    (if (some? new-node)
      [new-pg new-node]
      [pg nil])))

(defn pgraph-try-satisfy
  [pg]
  (loop [pg pg]
    (let [[pg new-node] (pgraph-try-satisfy-any-node pg)]
      (if (some? new-node)
        (recur pg)
        pg))))

(defn pgraph-for-factory
  [world factory]
  (pgraph-try-satisfy (empty-pgraph-for-factory world factory)))

(defn satisfy-desired-output [world factory]
  (loop [satisfied-factory factory
         unsatisfied (unsatisfied-output factory)]
    (if (empty? unsatisfied)
      satisfied-factory
      (if-let [[next-recipe-id, next-recipe] (->> unsatisfied (map first) (satisfying-recipe-for-set world :output))]
        (let [times (->> next-recipe (:output) (satisfying-ratio unsatisfied))
              updated-factory (apply-recipe factory next-recipe-id next-recipe times)]
          (recur
           updated-factory
           (unsatisfied-output updated-factory)))
        ;; no recipes left to satisfy the output
        ;; remaining unsatisfied output needs to be provided as input to the factory
        (-> satisfied-factory
            (update :input #(quantity-set+ % unsatisfied))
            (update :output #(quantity-set+ % unsatisfied)))))))

(defn satisfy-desired-input [world factory]
  factory)

(defn satisfy-factory [world factory]
  (->> factory
       (satisfy-desired-machines world)
       (satisfy-desired-input world)))
       ;(satisfy-desired-output world)
       


(defn update-factory [world factory]
  (-> world
      (assoc-in [:factories (:id factory)] factory)))


(defn recipe-without-item [recipe item-id]
  (-> recipe
      (dissoc-in [:input] item-id)
      (dissoc-in [:output] item-id)))

(defn recipe-without-machine [recipe machine-id]
  (-> recipe
      (dissoc-in [:machines] machine-id)))

(defn factory-without-machine [factory machine-id world]
  (let [updated-factory (-> factory
                            (dissoc-in [:machines] machine-id))]
    (if (= factory updated-factory) factory (satisfy-factory world factory))))

(defn factory-without-recipe [factory recipe-id world]
  (let [updated-factory (-> factory
                            (dissoc-in [:recipes] recipe-id))]
    (if (= factory updated-factory) factory (satisfy-factory world factory))))

(defn factory-without-item [factory item-id world]
  (let [updated-factory (-> factory
                            (dissoc-in [:desired-output] item-id))]
    (if (= factory updated-factory) factory (satisfy-factory world factory))))




(defn remove-factory-by-id [world id]
  (-> world
      (dissoc-in [:factories] id)))

(defn remove-item-by-id [world id]
  (-> world
      (update :recipes   (partial map-vals #(recipe-without-item  % id)))
      (update :factories (partial map-vals #(factory-without-item % id world)))
      (dissoc-in [:items] id)))

(defn remove-machine-by-id [world id]
  (-> world
      (update :recipes   (partial map-vals #(recipe-without-machine  % id)))
      (update :factories (partial map-vals #(factory-without-machine % id world)))
      (dissoc-in [:machines] id)))

(defn remove-recipe-by-id [world id]
  (-> world
      (update :factories (partial map-vals #(factory-without-recipe % id world)))
      (dissoc-in [:recipes] id)))



(defn new-factory
  ([] (new-factory {}))
  ([opts] (merge {:id (new-uuid)
                  :name "Unnamed factory"
                  :desired-output {}
                  :input {}
                  :output {}
                  :machines {}
                  :recipes {}} opts)))

(defn new-item
  ([] (new-item {}))
  ([opts] (merge {:id (new-uuid)
                  :name "Unnamed item"} opts)))


(defn new-machine 
  ([] (new-machine {}))
  ([opts] (merge {:id (new-uuid)
                  :name "Unnamed machine"
                  :power 0
                  :speed 1} opts)))

(defn new-recipe
  ([] (new-recipe {}))
  ([opts] (merge {:id (new-uuid)
                  :name "Unnamed recipe"
                  :input {}
                  :output {}
                  :machines #{}} opts)))


(defn world->str [world] (pr-str world))
(defn str->world [s] (edn/read-string s))
