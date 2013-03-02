(ns clj-quad.core-test
  (:require [clojure.zip :as zip]
            [clj-quad.shape :as shape]
            [clojure.pprint])
  (:use [clojure.test]
        [clj-quad.core]))

; (defn insert-random-children [quad n]
;   (reduce (fn [quad _] (insert quad (rand-node 1000 1000))) quad (range n)))

(def quad
  (quadtree
    {:depth 0
     :bounds
      {:x 0 :y 0 :width 1000 :height 1000}}))

(defn compare-node
  [n1 n2 ks]
  (= (get-in n1 ks)
     (get-in n2 ks)))

(defn compare-nodes
  [parent nodes ks]
  (let [results (reduce
                  (fn [bs n]
                    (conj bs (compare-node parent n [:bounds :width]))) [] nodes)
        equal? (reduce #(and %1 %2) results)]
    equal?))

(deftest quad-structure
  (testing "Quadtree is currectly formatted."
    (is
      (=
        quad
        (quad-zipper {:bounds {:x 0 :y 0 :width 1000 :height 1000}
                      :children       []
                      :step-children []
                      :nodes          []
                      :max-depth      4
                      :max-children   4
                      :depth          0
                      :lookup-table {} })))))

(deftest divide-node-count
  (testing "Subdivide a node into four quadrants."
    (let [updated-quad (insert-random-children quad 100)
          node (zip/root updated-quad)]
      (is (= 4 (count (:nodes node)))))))

(deftest node-insertion
  (let [quad (quadtree {:max-depth 8 :max-children 10 :bounds (shape/bounds 0 0 100 100)})]

    (testing "Insert four step-children then a step child."
      (let [s (shape/rect 10 10 10 10)
            quad (insert quad s)]
        (is (= [s] (:children (zip/root quad))))))

    (testing "Insert a node. Should be inserted into children."
      (let [s (shape/rect 10 10 10 10)
            quad (insert quad s)]
        (is (= [s] (:children (zip/root quad))))))))

(deftest lookup-table
  (let [rand-child {:id 1 :bounds (bounds 0 0 100 100)}
        quad (insert quad rand-child)]
    (testing "Makes sure an item is inserted into the lookup-table as well as the quadtree."
      (let [table (:lookup-table (zip/root quad))
            child (get table 1)]
        (is
          (= rand-child child))))))

(deftest remove-elements
 (let [child-id 1000001
       child {:id child-id :bounds (bounds 0 0 100 100)}
       new-quad (insert quad child)
       quad0 (insert-random-children quad 5)
       quad1 (insert quad0 child)
       quad2 (remove-child quad1 (:id child))]
    (testing "Should remove a child after being inserted."
      (is
        (= quad (remove-child new-quad child-id))))


    (testing "Should remove a child after many children have been inserted."
      (is
        (= quad0 quad2))

    (clojure.pprint/pprint quad0)
    (clojure.pprint/pprint quad2))))

(deftest updating
  (let [child {:id 1 :bounds (bounds 50 50 50 50)}
        child2 {:id 1 :bounds (bounds 200 200 50 50)}
        quad1 (insert quad child)
        quad2 (insert quad child2)]

    (testing "Updating a node should remove old and insert new correctly."
      (is (= (update-child quad1 child2) quad2)))

    ; (testing "Updating with many children."
    ;   (let [child {:id 1000000 :bounds (bounds 50 50 50 50)}
    ;         child2 {:id 1000000 :bounds (bounds 200 200 50 50)}
    ;         quad0 (insert-random-children quad 5)
    ;         quad1 (insert quad0 child)
    ;         quad2 (insert quad0 child2)
    ;         quad3 (update-child quad1 child2)]
    ;         (clojure.pprint/pprint quad1)
    ;         (clojure.pprint/pprint quad2)
    ;         (clojure.pprint/pprint quad3)
    ;     (is (= quad3 quad2))))
    ))