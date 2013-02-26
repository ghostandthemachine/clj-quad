(ns clj-quad.core-test
  (:require [clojure.zip :as zip]
            [clj-quad.shape :as shape])
  (:use [clojure.test]
        [clj-quad.core]))

(defn insert-random-children [quad n]
  (reduce (fn [quad _] (insert quad (rand-node 1000 1000))) quad (range n)))

(def quad
  (quadtree
    {:depth 0
     :bounds
      {:x 0 :y 0 :width 1000 :height 1000}
     :nodes []}))

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
        (is (= [s] (:children (zip/root quad))))))
    ))



