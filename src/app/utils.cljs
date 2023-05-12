(ns app.utils
  (:require
   [goog.object :as gobj]))

;; Calculate the distance between a point (mouse-x, mouse-y) and the
;; closest edge of a rectangle
(defn distance-to-rect [mouse-x mouse-y pan-offset-x pan-offset-y shape]
  (let [x (+ (gobj/get shape "x") pan-offset-x)
        y (+ (gobj/get shape "y") pan-offset-y)
        width (gobj/get shape "width")
        height (gobj/get shape "height")
        in-x (<= x mouse-x (+ x width))
        in-y (<= y mouse-y (+ y height))]
    (when (and in-x in-y)
      (let [dx (max 0 (max (- x mouse-x) (- mouse-x (+ x width))))
            dy (max 0 (max (- y mouse-y) (- mouse-y (+ y height))))
            distance (Math/sqrt (+ (* dx dx) (* dy dy)))]
        distance))))

;; Calculate the distance between a point (mouse-x, mouse-y) and the center of a circle
(defn distance-to-circle [mouse-x mouse-y pan-offset-x pan-offset-y shape]
  (let [x (+ (gobj/get shape "x") pan-offset-x)
        y (+ (gobj/get shape "y") pan-offset-y)
        radius (gobj/get shape "radius")
        dx (- mouse-x x)
        dy (- mouse-y y)
        distance (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (when (<= distance radius)
      distance)))

(defn distance-to-shape [mouse-x mouse-y pan-offset-x pan-offset-y shape]
  (let [shape-type (keyword (gobj/get shape "shape-type"))]
    (cond
      (= shape-type :rect) (distance-to-rect mouse-x mouse-y pan-offset-x pan-offset-y shape)
      (= shape-type :circle) (distance-to-circle mouse-x mouse-y pan-offset-x pan-offset-y shape))))

;; Calculate the distances from the point (x, y) to each shape
(defn calculate-distances [x y shapes pan-total-x pan-total-y]
  (map #(distance-to-shape x y pan-total-x pan-total-y %) shapes))

;; Get the minimum distance from a list of distances
(defn get-min-distance [distances]
  (->> distances
       (filter some?)
       (apply min)))

;; Filter shapes by the minimum distance
;; Return the first shape that matches the minimum distance.
(defn filter-shapes-by-min-distance [x y min-distance shapes pan-total-x pan-total-y]
  (->> shapes
       (filter #(= (distance-to-shape x y pan-total-x pan-total-y %) min-distance))
       (first)))

;; Find the shape closest to the point (x, y) and return it.
;; If there are multiple shapes with the same minimum distance, return the first one in the list.
;; If no shape is found, return nil.
(defn ^:export find-selected-shape [shapes x y pan-total-x pan-total-y]
  (let [distances (calculate-distances x y shapes pan-total-x pan-total-y)
        min-distance (get-min-distance distances)]
    (when min-distance
      (filter-shapes-by-min-distance x y min-distance shapes pan-total-x pan-total-y))))