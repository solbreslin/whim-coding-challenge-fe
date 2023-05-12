(ns app.canvas
  (:require [goog.style :as gstyle]
            [goog.object :as gobj]
            [goog.functions :as gfunctions]
            [app.shapes :as shapes]
            [app.utils :as utils]
            [app.svg :as svg]))


(def canvas (.createElement js/document "canvas"))
(def ctx (.getContext canvas "2d"))
(def clicked-on-shape (atom false))
(def mouse-is-pressed (atom false))
(def mouse-initial-position (atom {:x 0 :y 0}))
(def dimensions (atom {:width (.-innerWidth js/window) :height (.-innerHeight js/window)}))
(def pan-offset (atom {:x 0 :y 0}))
(def pan-start (atom {:x 0 :y 0}))
(def pan-total (atom {:x 0 :y 0}))

(defn set-canvas-dimensions []
  (set! (. canvas -width) (:width @dimensions))
  (set! (. canvas -height) (:height @dimensions)))

(defn clear-canvas []
  (.clearRect ctx 0 0 (:width @dimensions) (:height @dimensions)))

(defn draw-rect [x y width height fill]
  (set! (.-fillStyle ctx) fill)
  (.fillRect ctx (+ x (:x @pan-total)) (+ y (:y @pan-total)) width height))

(defn draw-circle [x y radius fill]
  (set! (.-fillStyle ctx) fill)
  (.beginPath ctx)
  (.arc ctx (+ x (:x @pan-total)) (+ y (:y @pan-total)) radius 0 (* 2 Math/PI))
  (.fill ctx))

(defn draw-shape [shape]
  (let [shape-type (keyword (gobj/get shape "shape-type"))
        x (gobj/get shape "x")
        y (gobj/get shape "y")
        width (gobj/get shape "width")
        height (gobj/get shape "height")
        radius (gobj/get shape "radius")
        fill (gobj/get shape "fill")]
    (cond
      (= shape-type :rect)
      (draw-rect x y width height fill)
      (= shape-type :circle)
      (draw-circle x y radius fill))))

(defn draw-shapes []
  (doseq [shape (shapes/get-shapes)]
    (draw-shape shape)))

(defn draw []
  (clear-canvas)
  (when (shapes/get-shapes)
    (draw-shapes))
  (svg/reposition-svg (:x @pan-total) (:y @pan-total)))


(defn update-pan-start [client-x client-y]
  (reset! pan-start {:x (- client-x (-> @pan-offset :x))
                     :y (- client-y (-> @pan-offset :y))}))

(defn handle-mouse-down [e]
  (let [client-x (.-clientX e)
        client-y (.-clientY e)
        selected-shape (utils/find-selected-shape (shapes/get-shapes) client-x client-y (:x @pan-total) (:y @pan-total))]
    (when selected-shape
      (svg/create-svg-overlay (shapes/get-shapes) (gobj/get selected-shape "group") (:x @pan-total) (:y @pan-total)))
    (update-pan-start client-x client-y)
    (reset! clicked-on-shape (boolean selected-shape))
    (reset! mouse-is-pressed true)
    (reset! mouse-initial-position {:x client-x :y client-y})))

(defn handle-mouse-move [e]
  (when @mouse-is-pressed
    (let [mouse-x (- (.-clientX e) (-> @pan-offset :x))
          mouse-y (- (.-clientY e) (-> @pan-offset :y))
          dx (- mouse-x (-> @pan-start :x))
          dy (- mouse-y (-> @pan-start :y))]
      (reset! pan-start {:x mouse-x :y mouse-y})
      (swap! pan-total update :x + dx)
      (swap! pan-total update :y + dy)
      (gstyle/setStyle canvas (clj->js {:cursor "grabbing"}))
      (draw))))

(defn handle-mouse-up [e]
  (let [x (.-clientX e)
        y (.-clientY e)
        dx (Math/abs (- x (:x @mouse-initial-position)))
        dy (Math/abs (- y (:y @mouse-initial-position)))]
    (when (and @mouse-is-pressed (not @clicked-on-shape) (= dx 0) (= dy 0))
      (svg/clear-svg))
    (reset! mouse-is-pressed false)
    (gstyle/setStyle canvas (clj->js {:cursor "default"}))))

(defn handle-mouse-out []
  (reset! mouse-is-pressed false)
  (gstyle/setStyle canvas (clj->js {:cursor "default"})))

(defn handle-resize []
  (reset! dimensions {:width (.-innerWidth js/window) :height (.-innerHeight js/window)})
  (set-canvas-dimensions)
  (draw))

(def handle-resize-debounced (gfunctions/debounce handle-resize 100))

(defn ^:export init []
  (.appendChild (.-body js/document) canvas)
  (set-canvas-dimensions)
  (.addEventListener js/window "resize" handle-resize-debounced)
  (.addEventListener canvas "mousedown" handle-mouse-down)
  (.addEventListener canvas "mouseup" handle-mouse-up)
  (.addEventListener canvas "mouseout" handle-mouse-out)
  (.addEventListener canvas "mousemove" handle-mouse-move)
  (draw))

