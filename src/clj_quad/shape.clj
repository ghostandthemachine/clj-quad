(ns clj-quad.shape)

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