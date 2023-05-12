(ns app.core
  (:require [devtools.core :as devtools]
            [app.canvas :as canvas] 
            [app.shapes :as shapes]))

;;; SETUP

(devtools/install!)


;;; LIFECYCLE

(defn ^:export init [] 
  (shapes/get-data canvas/init))


