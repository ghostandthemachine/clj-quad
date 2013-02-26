(ns clj-quad.collision-test
  (:refer-clojure :exclude [contains?])
  (:require [clj-quad.shape :as shape])
  (:use [clojure.test]
        [clj-quad.collision]))

(deftest point-intersection
  (let [shape (shape/rect 100 100 300 300)]

    (testing "A point inside of a shape."
      (is
        (= true
          (contains-point? (:bounds shape) [150 150]))))

    (testing "A point outside of a shape."
      (is
        (= false
          (contains-point? (:bounds shape) [50 50]))))))


(deftest shape-intersection
  (let [shape (shape/rect 100 100 400 400)]

    (testing "Shape contains shape based on bounds."
      (is
        (= true
          (contains-shape? shape (shape/rect 200 200 100 100)))))

    (testing "Shape does not contain shape based on bounds."
      (is
        (= false
          (contains-shape? shape (shape/rect 500 500 400 400)))))

    (testing "Shape does not contain shape off too left."
      (is
        (= false
          (contains-shape? shape (shape/rect 0 100 40 40)))))

    (testing "Shape does not contain shape off too right."
      (is
        (= false
          (contains-shape? shape (shape/rect 500 100 40 40)))))

    (testing "Shape does not contain shape above top."
      (is
        (= false
          (contains-shape? shape (shape/rect 100 0 40 40)))))

    (testing "Shape does not contain shape above top."
      (is
        (= false
          (contains-shape? shape (shape/rect 100 500 40 40)))))))

(deftest shape-list-filtering
  (let [base-shape (shape/rect 100 100 200 200)
        s1 (shape/rect 0 0 50 50)      ; out top left
        s2 (shape/rect 150 125 20 20)  ; in top left
        s3 (shape/rect 150 275 20 20)  ; intersecting bottom right
        s4 (shape/rect 500 500 50 50)  ; out bottom right
        s5 (shape/rect 75 175 50 50)   ; intersecting left
        s6 (shape/rect 500 500 50 50)
        shapes [s1 s2 s3 s4 s5 s6]]

    (testing "Should filter out shapes not contained in the base shape."
      (is
        (=
          [s2 s3]
          (get-containing-shapes base-shape shapes))))

    (testing "Should filter out shapes not interecting the base shape."
      (is
        (=
          [s2 s3 s5]
          (get-intersecting-shapes base-shape shapes))))

    ))

