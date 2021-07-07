(ns factor.pgraph
  "Defines functions for dealing with Production Graphs (aka pgraphs).
   Pgraphs are used to represent the flow of items throughout a factory.
   The pgraph is a directed graph, where nodes of the graph represent processing units
   (e.g. an array of smelters turning Iron Ore to Iron Ingots is a single node in the graph).
   The edges of the pgraph represent the flow of items from one processing unit's output to the next unit's input."
  (:require [factor.qmap :as qmap]
            [medley.core]
            [com.rpl.specter :as s]))

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

(defn update-node
  [pg id updater]
  (let [current (get-in pg [:nodes id])
        updated (if (fn? updater) (updater current) updater)]
    (if (nil? updated)
      (update pg :nodes dissoc id)
      (assoc-in pg [:nodes id] updated))))

(defn update-edge
  [pg lid rid updater]
  (let [current (get-in pg [:edges lid rid])
        updated (if (fn? updater) (updater current) updater)]
    (if (= 0 (apply + (vals updated)))
      (update pg :edges medley.core/dissoc-in [lid rid])
      (assoc-in pg [:edges lid rid] updated))))

(defn add-node
  [pg node]
  (let [id (:next-node-id pg)
        node (assoc node :id id)]
    [(-> pg
         (update-node id node)
         (update :next-node-id inc)) node]))

(defn get-edge
  [pg lid rid]
  (get-in pg [:edges lid rid]))

(defn get-node
  [pg id]
  (get-in pg [:nodes id]))

(defn connect-nodes
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
  (let [{left-out :output left-in :input} (get-node pg lid)
        root=>right             (get-edge pg :root rid)
        root=>right-unsatisfied (qmap/- root=>right left-out)
        root=>right-satisfied   (qmap/- root=>right root=>right-unsatisfied)
        left-out-unused         (qmap/- left-out root=>right-satisfied)]
    (-> pg
        (update-node :root       #(-> %
                                             (update :input qmap/- root=>right-satisfied)
                                             (update :input qmap/+ left-in)
                                             (update :output qmap/+ left-out-unused)))
        (update-edge :root rid   root=>right-unsatisfied)
        (update-edge lid   :root left-out-unused)
        (update-edge :root lid   left-in)
        (update-edge lid   rid   root=>right-satisfied))))

(defn add-node-for-recipe
  [pg recipe ratio]
  (add-node pg {:recipe recipe
                       :input  (qmap/* (:input recipe)  (js/Math.ceil ratio))
                       :output (qmap/* (:output recipe) (js/Math.ceil ratio))}))

(defn matching-recipe-for-node
  [pg node-id]
  (let [{:keys [world]} pg
        needed-input    (get-edge pg :root node-id)]
    (if-let [matching-recipe (s/select-first [(s/must :recipes) s/MAP-VALS #(recipe-matches-qm % needed-input)] world)]
      [matching-recipe (satisfying-ratio needed-input (:output matching-recipe))])))

(defn try-satisfy-node [pg node-id]
  ;; TODO -- should see if there are any EXISTING nodes with unused output that could be used to satisfy the node's inputs
  ;; instead of always creating a new node.
  (if-let [matching-recipe (matching-recipe-for-node pg node-id)]
    (let [[recipe ratio] matching-recipe
          [pg new-node] (add-node-for-recipe pg recipe ratio)
          pg            (connect-nodes pg (:id new-node) node-id)]
      [pg new-node])
    [pg nil]))

(defn try-satisfy-any-node [pg]
  (let [[new-pg new-node] (->> pg
                               (:nodes)
                               (keys)
                               (map (partial try-satisfy-node pg))
                               (filter (fn [[_ new-node]] (some? new-node)))
                               (first))]
    (if (some? new-node)
      [new-pg new-node]
      [pg nil])))

(defn try-satisfy
  [pg]
  (loop [pg pg]
    (let [[pg new-node] (try-satisfy-any-node pg)]
      (if (some? new-node)
        (recur pg)
        pg))))

(defn pgraph-for-factory
  [world factory]
  (try-satisfy (empty-pgraph-for-factory world factory)))