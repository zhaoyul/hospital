(ns hc.hospital.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[hospital starting]=-"))
   :start      (fn []
                 (log/info "\n-=[hospital started successfully]=-"))
   :stop       (fn []
                 (log/info "\n-=[hospital has shut down successfully]=-"))
   :middleware (fn [handler _] handler)
   :opts       {:profile :prod}})
