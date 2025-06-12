(ns hc.hospital.env
  (:require
    [taoensso.timbre :as log]
    [hc.hospital.logging :as logging]
    [hc.hospital.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init       (fn []
                 (logging/configure-logging!)
                 (log/info "\n-=[hospital starting using the development or test profile]=-"))
   :start      (fn []
                (log/info "\n-=[hospital started successfully using the development or test profile]=-"))
   :stop       (fn []
                (log/info "\n-=[hospital has shut down successfully]=-"))
   :middleware wrap-dev
   :opts       {:profile       :dev}})
