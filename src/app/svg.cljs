(ns app.svg
  (:require [goog.style :as gstyle]
            [goog.object :as gobj]))

(def svg-namespace "http://www.w3.org/2000/svg")
(def svg-offset 8)

(def svg-overlay (atom nil))
(def svg-overlay-coords (atom {:x 0 :y 0}))

(defn ^:export clear-svg []
  (when @svg-overlay
    (.remove @svg-overlay)
    (reset! svg-overlay nil)
    (reset! svg-overlay-coords {:x 0 :y 0})))

(defn draw-svg [x y width height pan-total-x pan-total-y]
  (let [svg (.createElementNS js/document svg-namespace "svg")
        rect (.createElementNS js/document svg-namespace "rect")]
    (set! (.-style svg) "position: absolute; overflow: visible; pointer-events: none;")
    (.setAttribute svg "width" width)
    (.setAttribute svg "height" height)
    (set! (.-left (.-style svg)) (str (+ x pan-total-x) "px"))
    (set! (.-top (.-style svg)) (str (+ y pan-total-y) "px")) 
    (.setAttribute rect "width" width)
    (.setAttribute rect "height" height)
    (set! (.-style rect) "fill: none; stroke: #B571EB; stroke-width: 4")
    (.appendChild svg rect)
    (.appendChild (.-body js/document) svg)
    svg))

(defn update-svg [svg x y width height pan-total-x pan-total-y]
  (let [rect (first (array-seq (.getElementsByTagName svg "rect"))) new-left (+ x pan-total-x)
        new-top (+ y pan-total-y)]
    (gstyle/setStyle svg (clj->js {:left (str new-left "px")
                                   :top (str new-top "px")})
                     (.setAttribute svg "width" width)
                     (.setAttribute svg "height" height)
                     (.setAttribute rect "width" width)
                     (.setAttribute rect "height" height))))

(defn ^:export reposition-svg [pan-total-x pan-total-y]
  (when @svg-overlay
    (let [left (:x @svg-overlay-coords)
          top (:y @svg-overlay-coords)
          new-left (+ left pan-total-x)
          new-top (+ top pan-total-y)]
      (gstyle/setStyle @svg-overlay (clj->js {:left (str new-left "px")
                                              :top (str new-top "px")})))))

;; Calculate the minimum and maximum x and y coordinates of the shapes in a group
(defn get-group-bounds [shapes]
  (let [min-x (apply min (map #(gobj/get % "x") shapes))
        min-y (apply min (map #(gobj/get % "y") shapes))
        max-x (apply max (map #(if (= (keyword (gobj/get % "shape-type")) :rect)
                                 (+ (gobj/get % "x") (gobj/get % "width"))
                                 (+ (gobj/get % "x") (gobj/get % "radius"))) shapes))
        max-y (apply max (map #(if (= (keyword (gobj/get % "shape-type")) :rect)
                                 (+ (gobj/get % "y") (gobj/get % "height"))
                                 (+ (gobj/get % "y") (gobj/get % "radius"))) shapes))]
    {:min-x min-x, :min-y min-y, :max-x max-x, :max-y max-y}))

(defn ^:export create-svg-overlay [shapes group pan-total-x pan-total-y]
  (let [groups (filter #(= (gobj/get % "group") group) shapes)
        {:keys [min-x min-y max-x max-y]} (get-group-bounds groups)
        width (+ (- max-x min-x) (* 2 svg-offset))
        height (+ (- max-y min-y) (* 2 svg-offset))
        adjusted-min-x (- min-x svg-offset)
        adjusted-min-y (- min-y svg-offset)]
    (reset! svg-overlay-coords {:x adjusted-min-x :y adjusted-min-y})
    (if @svg-overlay
      (update-svg @svg-overlay adjusted-min-x adjusted-min-y width height pan-total-x pan-total-y)
      (reset! svg-overlay (draw-svg adjusted-min-x adjusted-min-y width height pan-total-x pan-total-y)))))