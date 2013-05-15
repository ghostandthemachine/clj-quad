(ns clj-quad.core
  (:refer-clojure :exclude [children insert root contains?])
  (:require [clojure.zip :as zip])
  (:use [clj-quad.util]))


(defn in-from-left?
  [bounds point]
  (>= (first point) (:x bounds)))

(defn in-from-right?
  [bounds point]
  (<= (first point) (+ (:x bounds) (:width bounds))))

(defn in-from-top?
  [bounds point]
  (>= (second point) (:y bounds)))

(defn in-from-bottom?
  [bounds point]
  (<= (second point) (+ (:y bounds) (:height bounds))))

(defn contains-point?
  [bounds point]
  (let [left?     (in-from-left? bounds point)
        right?    (in-from-right? bounds point)
        top?      (in-from-top? bounds point)
        bottom?   (in-from-bottom? bounds point)
        in-bounds [left? right? top? bottom?]]
    (reduce #(and %1 %2) in-bounds)))

(defn intersection
  [s1 s2]
  (let [bounds1 (:bounds s1)
        bounds2 (:bounds s2)
        p1      [(:x bounds2) (:y bounds2)]
        p2      [(+ (:x bounds2) (:width bounds2)) (:y bounds2)]
        p3      [(+ (:x bounds2) (:width bounds2)) (+ (:y bounds2) (:height bounds2))]
        p4      [(:x bounds2) (+ (:y bounds2) (:height bounds2))]
        points  [p1 p2 p3 p4]]
    (do
      (map #(contains-point? (:bounds s1) %) points))))

(defn intersects?
  [s1 s2]
  (not
    (nil?
      (some #{true} (intersection s1 s2)))))

(defn contains-shape?
  [s1 s2]
  (reduce
    #(and %1 %2)
    (intersection s1 s2)))

(defn get-intersecting-shapes
  [shape shapes]
  (filter #(intersects? shape %) shapes))

(defn get-containing-shapes
  [shape shapes]
  (filter #(contains-shape? shape %) shapes))

; ;========================================================================================
; ; Node
; ;========================================================================================

(def TOP_LEFT 0)
(def TOP_RIGHT 1)
(def BOTTOM_LEFT 2)
(def BOTTOM_RIGHT 3)

(defn bounds
  "Creates a bounds map. Takes x position, y position, with and height."
  [x y w h]
  {:x x :y y :width w :height h})

(defn point
  [x y]
  {:bounds (bounds x y 1 1)})

(defn rect
  [x y w h]
  {:bounds (bounds x y w h)})

(defn node
  "Creates a basic node."
  [& opts]
  (merge
    {:bounds         {:x nil :y nil :width nil :height nil}
     :children       []
     :step-children  []
     :nodes          []
     :max-depth      4
     :max-children   4
     :depth          0}
     (first opts)))

(defn bounds-node [& opts]
  (node (first opts)))

(defn get-node
  [tree node-id]
  (get-in tree [:nodes node-id]))

(defn add-node
  [tree n]
  (assoc-in tree [:nodes (:id n)] n))

(defn find-index
  "Given a nodes and child, returns the index (quad index between 0 and 3) of the child."
  [n child]
  (let [node-bounds (:bounds n)
        child-bounds (:bounds child)
        left? (if (> (:x child-bounds) (+ (:x node-bounds) (/ (:width node-bounds) 2)))
                false
                true)
        top? (if (> (:y child-bounds) (+ (:y node-bounds) (/ (:height node-bounds) 2)))
                false
                true)]
    (if left?
      (if top?
        TOP_LEFT
        BOTTOM_LEFT)
      (if top?
        TOP_RIGHT
        BOTTOM_RIGHT))))

; (defn print-tree [original]
;   (loop [loc (zip/seq-zip (seq original))]
;     (if (zip/end? loc)
;       (zip/root loc)
;       (recur (zip/next
;                 (do (println (zip/node loc))
;                     loc))))))


(defn do-tree
  [quad-zip tree-fn]
  (let [node (zip/node quad-zip)]
    (when (empty? (:nodes node))
      (tree-fn node)
      (do
        (do-tree (-> quad-zip zip/down) tree-fn)
        (do-tree (-> quad-zip zip/down zip/right) tree-fn)
        (do-tree (-> quad-zip zip/down zip/right zip/right) tree-fn)
        (do-tree (-> quad-zip zip/down zip/right zip/right zip/right) tree-fn)))))

(defn tree-branch?
  [n]
  true)

(defn node-adder
  "Add a node to a given parent."
  [n children]
  (assoc n :nodes children))

(defn quad-zipper
  "Helper function for wrapping quad tree map with a zipper."
  [root-node]
  (zip/zipper tree-branch? :nodes node-adder root-node))


(defn subdivide-node
  "Subdivide a given node in four sub nodes of equal size."
  [z]
  (let [[n loc]  z
        depth    (+ (:depth n) 1)
        node-bounds   (:bounds n)
        bx       (:x node-bounds)
        by       (:y node-bounds)
        b-w-h    (or (/ (:width node-bounds) 2) 0)
        b-h-h    (or (/ (:height node-bounds) 2) 0)
        bx-b-w-h (+ bx b-w-h)
        by-b-h-h (+ by b-h-h)]
    (-> z
      (zip/append-child
        (bounds-node {:depth depth
                      :bounds
                      {:x bx :y by :width b-w-h :height b-h-h}}))
      (zip/append-child
        (bounds-node {:depth depth
                      :bounds
                      {:x bx-b-w-h :y by :width b-w-h :height b-h-h}}))
      (zip/append-child
        (bounds-node {:depth depth
                      :bounds
                      {:x bx :y by-b-h-h :width b-w-h :height b-h-h}}))
      (zip/append-child
        (bounds-node {:depth depth
                      :bounds
                      {:x bx-b-w-h :y by-b-h-h :width b-w-h :height b-h-h}})))))

(defn remove-item
  "Remove an item from a given node."
  [node item child-type]
  ; (println "remove-item" item child-type)
  (-> node
    (zip/edit
      (fn [n]
        (update-in n [child-type]
          (fn [children] (filter #(not= item %) children)))))))

(defn remove-item-by-id
  [node id child-type]
  (let [match-child (fn [child id]
                      (not= id (:id child)))
        filter-out-child (fn [items id]
                          (filter #(match-child % id) items))
        children (filter-out-child (child-type node) id)]
    (-> node
      (zip/edit #(assoc % child-type children)))))


(defn insert
  "Recursively insert a new item into a given node. This function also manages the division of parent nodes."
  [node-zip item]
  (let [node   (zip/node node-zip)
        nodes  (:nodes node)]
    (if (empty? nodes)
      ; if this node has NO sub nodes, add the new item to children.
      ; Check if there are more than max-children. If so, subdivide this node and insert into the children
      (do
        (let [children       (conj (:children node) item)
              child-count    (count children)
              subdivide?     (and
                                (not (>= (:depth node) (:max-depth node)))
                                (> child-count (:max-children node)))]
        (if subdivide?
          ;; subdivide this node and insert into the children
          (let [divided-node (subdivide-node (zip/edit node-zip #(assoc % :children [])))
                remove-reinsert (fn [n child-item]
                                      (-> n
                                        (remove-item child-item :children)
                                        (insert child-item)))
                reinsert-children (fn [node]
                                      (reduce
                                        remove-reinsert
                                        divided-node
                                        children))]
            (-> divided-node
              reinsert-children
              zip/root
              quad-zipper))
          ;; otherwise just add the item and return.
          (do
            (-> node-zip
              (zip/edit #(assoc % :children children))
              zip/root
              (update-in [:lookup-table] assoc (:id item) item)
              quad-zipper
              )))))
      ; else if this node has sub nodes, find which node the new item
      ; will be inserted into and call recursive insert.
      (do
        (let [idx             (find-index node item)
              idx-node        (get (into [] nodes) idx)
              node-bounds     (:bounds idx-node)
              item-bounds     (:bounds item)
              in-from-left?   (>= (:x item-bounds) (:x node-bounds))
              in-from-right?  (<=
                                (+ (:x item-bounds) (:width item-bounds))
                                (+ (:x node-bounds) (:width node-bounds)))
                                ; (+ (:x node-bounds) (:width node-bounds)))
              in-from-top?    (>= (:y item-bounds) (:y node-bounds))
              in-from-bottom? (<= (+ (:y item-bounds) (:height item-bounds)) (+ (:y node-bounds) (:height node-bounds)))
              item-in-bounds? (and in-from-left? in-from-right? in-from-top? in-from-bottom?)]
          (if item-in-bounds?
            (cond
              (= idx TOP_LEFT)      (insert (-> node-zip zip/down) item)
              (= idx TOP_RIGHT)     (insert (-> node-zip zip/down zip/right) item)
              (= idx BOTTOM_LEFT)   (insert (-> node-zip zip/down zip/right zip/right) item)
              (= idx BOTTOM_RIGHT)  (insert (-> node-zip zip/down zip/right zip/right zip/right) item))
            (let [step-children (conj (:step-children node) item)]
              ;; else add to step-children (children not all the way in bounds but touching).
              (-> node-zip
                (zip/edit #(assoc % :step-children step-children))
                zip/root
                (update-in [:lookup-table] assoc (:id item) item)
                quad-zipper))))))))

(defn insert-children
  "Add a list of children to a given quadtree. Takes the tree root and a seq of children."
  [root & children]
  (reduce
      (fn [quad child]
        (insert quad child))
      root
      (flatten (into [] children))))

(defn retrieve-point
"Retrieves all items / points in the same node as the specified item / point. If the specified item
 overlaps the bounds of a node, then all children in both nodes will be returned.
 Takes a map representing a 2D coordinate point (with x, y properties), or a shape
 with dimensions (x, y, width, height) properties."
  [node-zip item]
  (let [node (zip/node node-zip)
        children (concat
                  (:children node)
                  (:step-children node))]
    (if (empty? (:nodes node))
      children
      (let [node-index (find-index node item)
            node-children (cond
                            (= node-index TOP_LEFT)
                              (retrieve-point (-> node-zip zip/down) item)
                            (= node-index TOP_RIGHT)
                              (retrieve-point (-> node-zip zip/down zip/right) item)
                            (= node-index BOTTOM_LEFT)
                              (retrieve-point (-> node-zip zip/down zip/right zip/right) item)
                            (= node-index BOTTOM_RIGHT)
                              (retrieve-point (-> node-zip zip/down zip/right zip/right zip/right) item))]
        (concat children node-children)))))


(defn retrieve-rect
  "Retrieve all items for a given rectangle shape (via bounds)."
  [node-zip item]
  (let [node (zip/node node-zip)
        item-bounds (:bounds item)
        points [[(:x item-bounds) (:y item-bounds)]
                [(+ (:x item-bounds) (:width item-bounds)) (:y item-bounds)]
                [(:x item-bounds) (+ (:y item-bounds) (:height item-bounds))]
                [(+ (:x item-bounds) (:width item-bounds)) (+ (:y item-bounds) (:height item-bounds))]]
        retrieved-items (flatten
                          (distinct
                            (reduce
                              (fn [children [x y]]
                                (conj children (retrieve-point node-zip {:bounds (bounds x y 1 1)})))
                              []
                              points)))]
    (distinct retrieved-items)))

(defn lookup-child
  [node id]
  (get (:lookup-table node) id))

(defn remove-from-lookup-table
  [quad-zip child]
  (-> quad-zip
    (zip/edit (fn [n]
                (println "in remove-from-lookup-table " n)
                (update-in n [:lookup-table] dissoc (:id child) child)))))

(defn remove-child
  ([quad-zip id]
  (remove-child quad-zip id (lookup-child (zip/root quad-zip) id)))
  ([quad-zip id child]
    (let [node (zip/node quad-zip)]
      (cond
        (not (empty? (:nodes node)))
          (do
              (remove-child (-> quad-zip zip/down) id child)
              (remove-child (-> quad-zip zip/down zip/right) id child)
              (remove-child (-> quad-zip zip/down zip/right zip/right) id child)
              (remove-child (-> quad-zip zip/down zip/right zip/right zip/right) id child))
        (filter #(= (:id %) id) (:children node))
          (do
            (let [without-child (remove-item-by-id quad-zip id :children)
                  without-lookup (remove-from-lookup-table without-child child)]
              (-> without-lookup
                zip/root
                quad-zipper)))
        (filter #(= (:id %) id) (:step-children node))
          (do
            (-> quad-zip
              (remove-item-by-id id :step-children)
              zip/root
              (remove-from-lookup-table child)
              quad-zipper))))))

(defn update-child
  [quad-zip child]
  (-> quad-zip
    (remove-child (:id child))
    (insert child)))

(defn quadtree
  "Creates a quadtree. Takes a map defining the root node of the tree.
  The root should have a :bounds entry with a map of bounds data (x, y, width, height)."
  [root]
  (quad-zipper (bounds-node (merge root {:lookup-table {}}))))

