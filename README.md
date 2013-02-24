# clj-quad

Clj-quad is a purely functional implementation of the [Quadtree](http://en.wikipedia.org/wiki/Quadtree) data structure. Quadtrees are commonly used for spatial partitioning of two dimensional space for fast lookup of elements based on location. As elements are inserted into the tree, the tree subdivides recursively into four quadrants until specified a maximum depth is reached. This essentially means that every time a fifth elements is inserted into a quadrant the quadrant is subdivided into four child quadrants and the child elements are redistributed.

![Quadtree bounds from wikipedia](http://upload.wikimedia.org/wikipedia/commons/thumb/8/8b/Point_quadtree.svg/300px-Point_quadtree.svg.png)

This structure allows for fast look up and retrieval of elements based on location and/or area. This is especially beneficial for tasks like collision detection on many shapes. Given a tree and a map of position and dimensions, clj-quad will return a small list of elements in the relevant quadrant.


# usage

## creation

create a new quadtree with a specified dimension.

````clojure
    => (def quad
         (quadtree
           {:depth 0
            :bounds
             {:x 0 :y 0 :width 1000 :height 1000}
            :nodes []}))

    => (pprint quad)

    [{:bounds {:width 1000, :y 0, :x 0, :height 1000},
      :children [],
      :step-children [],
      :nodes [],
      :max-depth 4,
      :max-children 4,
      :depth 0}
     nil]

create some elements to insert

````clojure
    => (def elements [{:bounds {:x 50 :y 100 :width 10 :height 5}}
                      ;; or use the bounds function
                      {:bounds (bounds 200 150 8 8)}
                      {:bounds (bounds 200 800 8 8)}
                      {:bounds (bounds 790 434 8 8)}
                      {:bounds (bounds 346 124 8 8)}
                      {:bounds (bounds 15 900 8 8)}])

## insertion

insert single elements with insert or a seq of elements with insert-children

````clojure
    => (insert quad {:bounds (bounds 200 800 8 8)})

    [{:bounds {:width 1000, :y 0, :x 0, :height 1000},
      :children [{:bounds {:x 200, :y 800, :width 8, :height 8}}],
      :step-children [],
      :nodes [],
      :max-depth 4,
      :max-children 4,
      :depth 0}
     nil]
    nil

    => (insert-children quad elements)

    [{:bounds {:width 1000, :y 0, :x 0, :height 1000},
      :children (),
      :step-children [],
      :nodes
      ({:bounds {:x 0, :y 0, :width 500, :height 500},
        :children
        [{:bounds {:width 10, :y 100, :x 50, :height 5}}
         {:bounds {:x 200, :y 150, :width 8, :height 8}}
         {:bounds {:x 346, :y 124, :width 8, :height 8}}],
        :step-children [],
        :nodes [],
        :max-depth 4,
        :max-children 4,
        :depth 1}
       {:bounds {:x 500, :y 0, :width 500, :height 500},
        :children [{:bounds {:x 790, :y 434, :width 8, :height 8}}],
        :step-children [],
        :nodes [],
        :max-depth 4,
        :max-children 4,
        :depth 1}
       {:bounds {:x 0, :y 500, :width 500, :height 500},
        :children
        [{:bounds {:x 200, :y 800, :width 8, :height 8}}
         {:bounds {:x 15, :y 900, :width 8, :height 8}}],
        :step-children [],
        :nodes [],
        :max-depth 4,
        :max-children 4,
        :depth 1}
       {:bounds {:x 500, :y 500, :width 500, :height 500},
        :children [],
        :step-children [],
        :nodes [],
        :max-depth 4,
        :max-children 4,
        :depth 1}),
      :max-depth 4,
      :max-children 4,
      :depth 0}
     nil]

## retrieval

retrieve elements from a quad based on a point

````clojure
    => (retrieve-point quad {:bounds (bounds 100 100 1 1)})

    ({:bounds {:width 10, :y 100, :x 50, :height 5}}
     {:bounds {:x 200, :y 150, :width 8, :height 8}}
     {:bounds {:x 346, :y 124, :width 8, :height 8}})

or retrieve elements based on a rectangle

````clojure
    => (retrieve-rect quad {:bounds (bounds 100 100 600 600)})

    ({:bounds {:width 10, :y 100, :x 50, :height 5}}
     {:bounds {:x 200, :y 150, :width 8, :height 8}}
     {:bounds {:x 346, :y 124, :width 8, :height 8}}
     {:bounds {:x 790, :y 434, :width 8, :height 8}}
     {:bounds {:x 200, :y 800, :width 8, :height 8}}
     {:bounds {:x 15, :y 900, :width 8, :height 8}})

In the future paths will be supported as well as primitive shape areas.

Feel free to use or modify please just share improvements.


### Road map
* Child updates
* Utility functions for collision detection
* Support for path intersection


## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
