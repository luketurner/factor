(ns factor.pgraph
  "Defines functions for dealing with Production Graphs (aka pgraphs).
   Pgraphs are used to represent the flow of items throughout a factory.
   The pgraph is a directed graph, where nodes of the graph represent processing units
   (e.g. an array of smelters turning Iron Ore to Iron Ingots is a single node in the graph).
   The edges of the pgraph represent the flow of items from one processing unit's output to the next unit's input."
  (:require [factor.qmap :as qmap]
            [medley.core :refer [map-vals filter-vals]]
            [com.rpl.specter :as s]
            [factor.world :as w]
            [factor.filter :as filter]
            [factor.util :as util]
            [clojure.string :as string]
            [re-frame.core :refer [subscribe]]))

(defn all-edges
  "Returns a seq of tuples of form [lid rid  edge], where
   lid and rid are node IDs, and edge is a qmap representing
   the edge quantity. Includes one tuple for every edge in the pgraph."
  [pg]
  (for [[lid xs] (:edges pg) [rid edge] xs]
    [lid rid edge]))

(defn get-edge
  "Returns a qmap representing the edge pointing from lid to rid (if one exists.)"
  [pg lid rid]
  (get-in pg [:edges lid rid]))

(defn input-edges
  "Retrieves the input edges for given node-id.
   (In other words, edges where node-id is the right-hand node.)
   Returns a map of form {lid edge-qmap}, where lid is the ID of the node
   on the left side of the input edge."
  [pg node-id]
  (get-in pg [:edges-rev node-id]))

(defn output-edges
  "Retrieves the output edges for given node-id.
   (In other words, edges where node-id is the left-hand node.)
   Returns a map of form {rid edge-qmap}, where rid is the ID of the node
   on the right end of the output edge."
  [pg node-id]
  (get-in pg [:edges node-id]))

(defn all-nodes
  "Returns a seq of all nodes in the pgraph.
   (Includes the special nodes :missing and :excess)."
  [pg]
  (-> pg (get :nodes) (vals)))

(defn get-node
  "Given a node ID, returns the matching node map (or nil
   if the node doesn't exist in the pgraph.)"
  [pg id]
  (get-in pg [:nodes id]))

(defn desired-output-node
  "Returns the node that represents the desired output of the pgraph."
  [pg]
  (get-node pg :1))

(defn normalized-inputs
  "Returns a qmap derived by dividing each quantity of the recipe's input qmap by the recipe's duration.
   The resulting value (called the recipe's normalized input) represent the number of items the recipe requires
   per second (or per minute, depending on user's configured unit.)"
  [recipe]
  (qmap/* (:input recipe) (/ 1 (:duration recipe))))

(defn normalized-outputs
  "Returns a qmap derived by dividing each quantity of the recipe's output qmap by the recipe's duration.
   The resulting value (called the recipe's normalized output) represent the number of items the recipe produces
   per second (or per minute, depending on user's configured unit.)"
  [recipe]
  (qmap/* (:output recipe) (/ 1 (:duration recipe))))

(defn real-inputs
  "Given a `recipe` and `machine`, calculates the \"real\" input rates
   (i.e. what would actually be consumed by one machine in-game).
   This factors in the recipe input quantities, duration, and machine speed."
  [{:keys [input duration]} {:keys [speed]}]
  (qmap/* input (/ speed duration)))

(defn real-outputs
  "Given a `recipe `and `machine `calculates the \"real \" output rates
   (i.e. what would actually be produced by one machine in-game).
   This factors in the recipe input quantities, duration, and machine speed."
  [{:keys [output duration]}
   {:keys [speed]}] (qmap/* output (/ speed duration)))

(defn update-node
  "Updates data for node by ID. UPDATER can be an update function or a simple value.
   Returns an updated version of PG.
   
   Note -- this is a low-level function, to add nodes to the pgraph in a way that
   ensures edges are created properly as well, use (add-node)."
  [pg id updater]
  (let [current (get-in pg [:nodes id])
        updated (if (fn? updater) (updater current) updater)]
    (if (nil? updated)
      (update pg :nodes dissoc id)
      (assoc-in pg [:nodes id] updated))))

(defn update-edge
  "Updates the qmap for the edge between LID and RID.
   UPDATER can be an update function or a simple value.
   Returns an updated version of PG."
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

(defn connect-nodes
  "Connects the output of one node (called the LEFT node) to the input of another node (the RIGHT node).
   The qmap for the edge is specified by the QM parameter. This subtracts the value of QM from the edge going
   from :missing to RIGHT, and from the edge going from LEFT to :excess, to ensure that the edges remain balanced.
   See the following diagram:
   
   before:    
               +-----------> LEFT -----------+
               |                             |
      MISSING--+                             +->EXCESS
               |                             |
               +-----------> RIGHT ----------+

   after:
               +-----------> LEFT -----------+
               |               |             |
      MISSING--+               |             +->EXCESS
               |               v             |
               +-----------> RIGHT ----------+"
  [pg left-id right-id qm]
  (-> pg
      (update-edge :missing right-id #(qmap/- % qm))
      (update-edge left-id :excess #(qmap/- % qm))
      (update-edge left-id right-id qm)))

(defn missing-input
  "Returns a qmap representing all the aggregate missing input for all nodes for the pgraph."
  [pg]
  (-> pg
      (output-edges :missing)
      (vals)
      (->> (apply qmap/+))))

(defn missing-input-for
  "Returns a qmap representing the missing input for a given node (by ID)."
  [pg node-id]
  (get-edge pg :missing node-id))

(defn excess-output
  "Returns a qmap representing the aggregate excess output for all nodes in the pgraph."
  [pg]
  (-> pg
      (input-edges :excess)
      (vals)
      (->> (apply qmap/+))))

(defn excess-output-for
  "Returns a qmap representing the excess output for a given node (by ID)."
  [pg node-id]
  (get-edge pg node-id :excess))

(defn nodes-with-excess-outputs
  "Returns list of all the nodes that have excess outputs -- that is, nodes that have
   an output edge to :excess. Each entry in the list is vec of form [node-id edge]"
  [pg]
  (input-edges pg :excess))

(defn nodes-with-missing-inputs
  "Returns list of all the nodes that have missing inputs -- that is, nodes that have
   an input edge from :missing. Each entry in the list is vec of form [node-id edge]"
  [pg]
  (output-edges pg :missing))

(defn connect-input-edges
  "Accepts a pgraph `pg` and a `node` (which must have already been added to the pgraph with add-node),
   and creates edges in the graph to satisfy the inputs of `node`. If inputs cannot be satisfied by any other
   node, they get satisfied by :missing node.
   (Note this function DOES NOT respect existing input edges. Calling it on a node with preexisting input edges
   will result in the node having too many edges! But it *does* check whether the node at the other end of the
   edge has capacity.)"
  [pg {:keys [input id] :as node}]
  (loop [pg pg]
    (if-let [output-node+edge (->> pg
                                   (nodes-with-excess-outputs)
                                   (map-vals #(qmap/intersection input %))
                                   (filter-vals not-empty)
                                   (first))]
      (recur
       (connect-nodes pg (first output-node+edge) id (last output-node+edge)))
      pg)))

(defn connect-output-edges
  "Accepts a pgraph `pg` and a `node` (which must have already been added to the pgraph with add-node),
   and creates edges in the graph to utilize the outputs of `node`. If outputs cannot be utilized by any other
   node, they get sent to the :excess node.
   (Note this function DOES NOT respect existing output edges. Calling it on a node with preexisting output edges
   will result in the node having too many edges! But it *does* check whether the node at the other end of the
   edge has capacity.)"
  [pg {:keys [output id] :as node}]
  (loop [pg pg]
    (if-let [input-node+edge (->> pg
                                  (nodes-with-missing-inputs)
                                  (map-vals #(qmap/intersection output %))
                                  (filter-vals not-empty)
                                  (first))]
      (recur
       (connect-nodes pg id (first input-node+edge) (last input-node+edge)))
      pg)))

(defn add-node
  "Adds a new node to the pgraph. A numeric ID is generated for the node.
   Additionally, if the node has inputs or outputs, edges are created from
   :missing (for inputs) or to :excess (for outputs). This maintains the constraint
   that the sum of the edges in and out of a node must equal the node's stated input
   and output."
  [pg {:keys [input output] :as node}]
  (let [id (keyword (str (:next-node-id pg)))
        node (assoc node :id id)]
    (-> pg
        (update-node id node)
        (update :next-node-id inc)
        (update-edge :missing id input)
        (update-edge id :excess output)
        (connect-input-edges node)
        (connect-output-edges node))))

(defn preferred-machine
  "Given a recipe, returns an ID for one of the machines the recipe supports. Respects factory allow/deny lists. Picks the highest-speed machine."
  [pg recipe]
  (let [{:keys [get-machine]} pg
        {:keys [soft-denied not-denied]} (->> (:machines recipe)
                                              (filter #(not (filter/machine-id-hard-denied? (:filter pg) %)))
                                              (group-by #(if (filter/machine-id-soft-denied? (:filter pg) %)
                                                           :soft-denied
                                                           :not-denied)))]
    (->> (if (seq not-denied) not-denied soft-denied)
         (map get-machine)
         (util/pick-max :speed))))

(defn candidate-list
  "Returns a list of possible candidates for the given pgraph. A 'possible candidate' exists when there is:
   
   1. An edge from :missing to any node (such edges are called missing inputs)
   2. A recipe with *output* that overlaps with the unsatisfied input edge.

   The recipe also cannot be forbidden by a hard deny-list or hard allow-list.
   
   Note that if there are ANY recipes that are NOT soft-denied, ONLY those will be returned. If the only candidates
   are soft-denied, then they will ALL be returned. Sorry if that's confusing -- basically the returned list will
   either have NO soft-denied candidates, or ALL soft-denied candidates."
  [pg]
  (let [candidate-recipes (->> pg
                               (missing-input)
                               (map (comp (:get-recipes-with-output pg) key))
                               (apply concat)
                               (filter #(not (filter/recipe-hard-denied? (:filter pg) %))))
        candidate-for-recipe (fn [recipe]
                               {:recipe recipe
                                :machine (preferred-machine pg recipe)
                                :num (qmap/satisfying-ratio (missing-input pg) (:output recipe))})
        candidates (map candidate-for-recipe candidate-recipes)
        {:keys [soft-denied not-denied]} (group-by #(if (filter/recipe-soft-denied? (:filter pg) (:recipe %))
                                                      :soft-denied
                                                      :not-denied) candidates)]
    (if (seq not-denied)
      not-denied
      soft-denied)))

(defn weight-candidate
  "Returns a numeric weight value for the given candidate. Higher-weight candidates are preferred over lower-weight candidates."
  [pg candidate]
  ; TODO
  1)

(defn candidate
  "Returns a valid candidate to insert into given pgraph. If no candidates could be found, returns nil."
  [pg]
  (when-let [c (util/pick-max (partial weight-candidate pg) (candidate-list pg))]
    c))

(defn node-for-candidate
  "Accepts a candidate object and returns a corresponding node."
  [{:keys [recipe machine num]}]
  {:num-machines num
   :recipe       recipe
   :machine      machine
   :power        (* (:power machine) num)
   :input        (qmap/* (real-inputs recipe machine)  num)
   :output       (qmap/* (real-outputs recipe machine) num)
   :catalysts    (qmap/* (:catalysts recipe)         num)})

(defn satisfy
  "Accepts a pgraph and iteratively adds nodes until the graph is 'satisfied' -- i.e. no more nodes can be added."
  [pg]
  (loop [pg pg]
    (if-let [c (candidate pg)]
      (recur (add-node pg (node-for-candidate c)))
      pg)))

(defn pgraph
  "Creates a pgraph for the specified `desired-output` and satisfies it. Returns the satisfied pgraph.
   The optional `filter` should contain allow/deny lists (see :filter schema in factor.db) and is intended
   to be pulled from a factory object, just like the desired output.
  `get-recipes-with-output` and `get-machine` should be functions that can be called to retrieve a set of recipes
   or a machine, respectively. This is done to provide better support for reactive flow. Rather than requesting a
   list of all recipes, all machines, etc. -- which would necessitate rebuilding the pgraph whenever anything changed
   -- the pgraph will only be subscribed to the specific recipe index keys and machine keys it needs for its calculations."
  [{:keys [filter desired-output get-recipes-with-output get-machine]}]
  ; 1. construct "empty" pgraph (only has :missing and :excess nodes)
  (-> {:get-recipes-with-output get-recipes-with-output
       :get-machine get-machine
       :filter filter
       :edges     {}
       :edges-rev {}
       :next-node-id 1
       :nodes {:missing {:id :missing}
               :excess  {:id :excess}}}

      ; 2. add a node for our desired outputs
      (add-node {:input desired-output})

      ; 3. satisfy the node we just added
      (satisfy)))

(defn all-catalysts
  "Returns a qmap representing the sum of all catalysts required for all nodes in the graph.
   Note -- in the future catalysts may be bubbled up node-by-node similar to inputs instead of
   being summed this way."
  [pg]
  (->> pg (all-nodes) (s/select [s/ALL (s/view :catalysts)]) (apply qmap/+)))

; TODO -- this should not be needed in this file! Abstract better
(defn get-item-name [id] (:name @(subscribe [:item id])))

(defn pg->dot
  "Accepts a pgraph and returns a dot document representing it.
   
   WARNING: Data is not sanitized/escaped!"
  [pg]
  (let [node->dot-label #(case (:id %)
                           :missing (str "MISSING\n" (qmap/qmap->str (:output %) get-item-name "\n"))
                           :excess (str "EXCESS\n" (qmap/qmap->str (:input %) get-item-name "\n"))
                           (qmap/qmap->str (:output %) get-item-name "\n"))
        node->dot (fn [{:keys [id] :as node}]
                    (str (name id) "[label=\"" (node->dot-label node) "\"]" ";\n"))
        edge->dot (fn [[lid rid edge]]
                    (str (name lid) " -> " (name rid) "[label=\"" (qmap/qmap->str edge get-item-name "\n") "\"]" ";\n"))]
   (str
   "digraph PG {\n"
   (apply str (map node->dot (all-nodes pg)))
   (apply str (map edge->dot (all-edges pg)))
   "}")))