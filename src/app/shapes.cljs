(ns app.shapes
  (:require [cljs.reader :as reader]))


(defonce shapes (atom nil))

(defn ^:export get-data [callback]
  (-> (.fetch js/window "shapes.edn")
      (.then (fn [response]
               (if (.-ok response)
                 (.text response)
                 (throw (js/Error. "Problem fetching data")))))
      (.then (fn [data]
               (let [fetched-shapes (-> data reader/read-string clj->js)]
                 (reset! shapes fetched-shapes)
                 (callback))))
      (.catch (fn [error]
                (js/console.error "Error:" error)))))

(defn ^:export get-shapes []
  @shapes)
