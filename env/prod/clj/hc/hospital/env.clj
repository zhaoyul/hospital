(ns hc.hospital.env
  (:require [taoensso.timbre :as log]
            [hc.hospital.logging :as logging]))

(def defaults
  {:init       (fn []
                 (logging/configure-logging!)
                 (log/info "\n-=[hospital starting]=-"))
   :start      (fn []
                (log/info "\n-=[hospital started successfully]=-"))
   :stop       (fn []
                (log/info "\n-=[hospital has shut down successfully]=-"))
   :middleware (fn [handler _] handler)
   :opts       {:profile :prod}})
