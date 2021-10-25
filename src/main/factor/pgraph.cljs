(ns factor.pgraph
  "Defines functions for dealing with Production Graphs (aka pgraphs).
   Pgraphs are used to represent the flow of items throughout a factory.
   The pgraph is a directed graph, where nodes of the graph represent processing units
   (e.g. an array of smelters turning Iron Ore to Iron Ingots is a single node in the graph).
   The edges of the pgraph represent the flow of items from one processing unit's output to the next unit's input."
  (:require [factor.qmap :as qmap]
            [medley.core]
            [com.rpl.specter :as s]
            [factor.world :as w]
            [clojure.string :as string]
            [re-frame.core :refer [subscribe]]))

(defn satisfying-ratio [goal base]
  (js/Math.ceil (apply max (for [[k v] base]
                             (when (contains? goal k)
                               (/ (goal k) v))))))

(defn normalized-inputs [recipe] (qmap/* (:input recipe) (/ 1 (:duration recipe))))
(defn normalized-outputs [recipe] (qmap/* (:output recipe) (/ 1 (:duration recipe))))

(defn recipe-matches-qm [recipe qm]
  (some true? (for [[k v] qm] (contains? (:output recipe) k))))

(defn empty-pgraph-for-factory
  [world {:keys [desired-output] :as factory}]
  {:world world
   :factory factory
   :edges     {:start {:end   desired-output}}
   :edges-rev {:end   {:start desired-output}}
   :next-node-id 1
   :nodes {:start {:id :start :output desired-output}
           :end   {:id :end   :input  desired-output}}})

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
      (-> pg
          (update :edges medley.core/dissoc-in [lid rid])
          (update :edges-rev medley.core/dissoc-in [rid lid]))
      (-> pg
          (assoc-in [:edges lid rid] updated)
          (assoc-in [:edges-rev rid lid] updated)))))

(defn add-node
  [pg node]
  (let [id (keyword (str (:next-node-id pg)))
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
           START (input) -------> (input) RIGHT (output) -------> (output) END
   
   after:
                                                              (any excess output from LEFT)
                                                   +-------------------------------------------------+
                                                   |                                                 V
           START (output) -------> (input) LEFT (output) -------> (input) RIGHT (output) -------> (input) END
                    |                                               ^
                    +-----------------------------------------------+
                     (any of RIGHT's input that LEFT doesn't supply)
   "
  [pg lid rid]
  (when (= lid :end) (throw "Cannot connect ending node to another node."))
  (let [{left-out :output left-in :input} (get-node pg lid)
        start=>right             (get-edge pg :start rid)
        start=>right-unsatisfied (qmap/- start=>right left-out)
        start=>right-satisfied   (qmap/- start=>right start=>right-unsatisfied)
        left-out-unused          (qmap/- left-out start=>right-satisfied)]
    (-> pg
        (update-node :start #(-> %
                                 (update :output qmap/- start=>right-satisfied)
                                 (update :output qmap/+ left-in)))
        (update-node :end   #(update % :input qmap/+ left-out-unused))
        (update-edge :start rid   start=>right-unsatisfied)
        (update-edge lid    :end  left-out-unused)
        (update-edge :start lid   left-in)
        (update-edge lid    rid   (partial qmap/+ start=>right-satisfied)))))

(defn add-node-for-recipe
  [pg recipe ratio]
  (add-node pg {:recipe recipe
                :recipe-ratio ratio
                :input     (qmap/* (normalized-inputs recipe)  ratio)
                :output    (qmap/* (normalized-outputs recipe) ratio)
                :catalysts (qmap/* (:catalysts recipe)         ratio)}))

(defn matching-recipe-for-node
  [pg node-id]
  (let [{:keys [world]} pg
        needed-input    (get-edge pg :start node-id)]
    (if-let [matching-recipe (s/select-first [(s/must :recipes) s/MAP-VALS #(recipe-matches-qm % needed-input)] world)]
      [matching-recipe (satisfying-ratio needed-input (normalized-outputs matching-recipe))])))

(defn edges
  [pg]
  (for [[lid xs] (:edges pg) [rid edge] xs]
    [lid rid edge]))

(defn matching-node-for-node
  [pg node-id]
  (let [needed-input    (get-edge pg :start node-id)]
    (if-let [matching-node-id (->> (edges pg)
                                   (some (fn [[lid rid edge]]
                                           (when (and (= rid :end)
                                                      (not= lid :start)
                                                      (qmap/intersects? edge needed-input))
                                             lid))))]
      (get-node pg matching-node-id))))

(defn try-satisfy-node [pg node-id]
  (if-let [existing-node (matching-node-for-node pg node-id)]
    [(connect-nodes pg (:id existing-node) node-id) existing-node]
    (if-let [matching-recipe (matching-recipe-for-node pg node-id)]
      (let [[recipe ratio] matching-recipe
            [pg new-node] (add-node-for-recipe pg recipe ratio)
            pg            (connect-nodes pg (:id new-node) node-id)]
        [pg new-node])
      [pg nil])))

(defn try-satisfy-any-node [pg]
  (let [[new-pg new-node] (->> pg
                               (:nodes)
                               (keys)
                               (filter #(not= % :start))
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

(defn input-edges
  [pg node-id]
  (get-in pg [:edges-rev node-id]))

(defn output-edges
  [pg node-id]
  (get-in pg [:edges node-id]))

(defn node-recipe
  [node]
  [(:recipe node) (:recipe-ratio node)])

(defn node-machine
  [{:keys [factory] :as pg} node]
  (let [[recipe ratio] (node-recipe node)]
    [(w/machine-for-factory-recipe factory recipe) ratio]))

(defn all-nodes
  [pg]
  (-> pg (get :nodes) (vals)))

(defn is-empty?
  [pg]
  (and (= 2 (count (all-nodes pg)))
       (empty? (:input (get-node pg :end)))))

(defn all-catalysts
  "Returns a qmap representing the sum of all catalysts required for all nodes in the graph.
   Note -- in the future catalysts may be bubbled up node-by-node similar to inputs instead of
   being summed this way."
  [pg]
  (->> pg (all-nodes) (s/select [s/ALL (s/view :catalysts)]) (apply qmap/+)))

; TODO -- this should not be needed in this file! Abstract better
(defn get-item-name [id] (:name @(subscribe [:item id])))

(defn node->dot-label
  [node]
  (case (:id node)
    :start (str "FACTORY INPUT\n" (qmap/qmap->str (:output node) get-item-name "\n"))
    :end (str "FACTORY OUTPUT\n" (qmap/qmap->str (:input node) get-item-name "\n"))
    (qmap/qmap->str (:output node) get-item-name "\n")))

(defn node->dot
  [{:keys [id] :as node}]
  (println node)
  (str (name id) "[label=\"" (node->dot-label node) "\"]" ";\n"))

(defn edge->dot-label
  [edge]
  (qmap/qmap->str edge get-item-name "\n"))

(defn edge->dot
  [[lid rid edge]]
  (str (name lid) " -> " (name rid) "[label=\"" (edge->dot-label edge) "\"]" ";\n"))

(defn pg->dot
  "Accepts a pgraph and returns a dot document representing it.
   
   WARNING: Data is not sanitized/escaped!"
  [pg]
  (str
   "digraph PG {\n"
   (apply str (map node->dot (all-nodes pg)))
   (apply str (map edge->dot (edges pg)))
   "}"))