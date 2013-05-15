(ns clj-quad.collision
  (:refer-clojure :exclude [contains? intersection]))

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
