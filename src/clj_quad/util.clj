(ns clj-quad.util
  (require [clojure.zip :as zip]))

(defn print-tree [original]
  (loop [loc (zip/seq-zip (seq original))]
    (if (zip/end? loc)
      (zip/root loc)
      (recur (zip/next
                (do (println (zip/node loc))
                    loc))))))

(def node-counter (atom 0))

(defn node-count [] (swap! node-counter inc))

(defn rand-node
  "Creates a random item for insertion. Mostly for testing."
  [x-max y-max]
  {:bounds
    {:id (node-count)
     :x (rand-int x-max)
     :y (rand-int y-max)
     :width (+ 5 (rand-int 10))
     :height (+ 5 (rand-int 10))}})