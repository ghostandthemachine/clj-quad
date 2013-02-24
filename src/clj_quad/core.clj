(ns clj-quad.core
  (:refer-clojure :exclude [children insert])
  (:require [clojure.zip :as zip]))


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

(defn node
  "Creates a basic node."
  [& opts]
  (merge
    {:bounds         {:x nil :y nil :width nil :height nil}
     :children       []
     :step-children []
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

(defn print-tree [original]
  (loop [loc (zip/seq-zip (seq original))]
    (if (zip/end? loc)
      (zip/root loc)
      (recur (zip/next
                (do (println (zip/node loc))
                    loc))))))

(defn tree-branch?
  [n]
  true)

(defn node-adder
  "Add a node to a given parent."
  [n children]
  (assoc n :nodes children))

(defn map-zipper
  "Helper function for wrapping quad tree map with a zipper."
  [root-node]
  (zip/zipper tree-branch? :nodes node-adder root-node))

(def node-counter (atom 0))

(defn node-count [] (swap! node-counter inc))

(defn rand-node
  "Creates a random item for insertion. Mostly for testing."
  []
  {:bounds
    {:id (node-count)
     :x (rand-int 1000)
     :y (rand-int 1000)
     :width (+ 5 (rand-int 10))
     :height (+ 5 (rand-int 10))}})


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
  [node item]
  (-> node
    (zip/edit
      (fn [n]
        (update-in n [:children]
          (fn [children] (filter #(not= item %) children)))))))

(defn insert
  "Recursively insert a new item into a given node. This function also manages the division of parent nodes."
  [node-zip item]
  (let [node   (zip/node node-zip)
        nodes  (:nodes node)]
    (if (empty? nodes)
      ; if this node has NO sub nodes, add the new item to children.
      ; Check if there are more than max-children. If so, subdivide this node and insert into the children
      (do
        (let [children          (conj (:children node) item)
              child-count    (count children)
              subdivide?     (and
                                (not (>= (:depth node) (:max-depth node)))
                                (> child-count (:max-children node)))]
        (if subdivide?
          ;; subdivide this node and insert into the children
          (let [divided-node (subdivide-node (zip/edit node-zip #(assoc % :children [])))
                remove-reinsert (fn [n child-item]
                                      (-> n
                                        (remove-item child-item)
                                        (insert child-item)))
                reinsert-children (fn [node]
                                      (reduce
                                        remove-reinsert
                                        divided-node
                                        children))]
            (-> divided-node
              reinsert-children
              zip/root
              map-zipper))
          ;; otherwise just add the item and return.
          (do
            (-> node-zip
              (zip/edit #(assoc % :children children))
              zip/root
              map-zipper)))))
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
                map-zipper))))))))

(defn retrieve-point
"Retrieves all items / points in the same node as the specified item / point. If the specified item
 overlaps the bounds of a node, then all children in both nodes will be returned.
 Takes a map representing a 2D coordinate point (with x, y properties), or a shape
 with dimensions (x, y, width, height) properties."
  [node-zip item]
  (let [node (zip/node node-zip)
        children (concat
                  (:children node)
                  (:stuck-children node))]
    (if (empty? (:nodes node))
      children
      (let [node-index (find-index node item)
            node-children
              (cond
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
                [(+ (:x item-bounds) (:width item-bounds)) (+ (:y item-bounds) (:height item-bounds))]]]
    (flatten
      (distinct
        (reduce
          (fn [children [x y]]
            (conj children (retrieve-point node-zip {:bounds (bounds x y 1 1)})))
          []
          points)))))





(comment
  (def quad
    (map-zipper
      (bounds-node
        {:depth 0
         :bounds
          {:x 0 :y 0 :width 1000 :height 1000}
         :nodes []})))

  (defn insert-random-children [quad n]
    (reduce
      (fn [quad i]
        (insert quad (rand-node)))
      quad
      (range n)))

  (def updated-quad (insert-random-children quad 100))

  (doseq [child (retrieve-rect updated-quad {:bounds (bounds 250 250 50 500)})]
    (println child))
  )

