(ns hc.hospital.core
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [hc.hospital.web.routes.pages]
   [hc.hospital.config :as config]
   [hc.hospital.env :refer [defaults]]

   ;; Edges
   [kit.edge.server.undertow]
   [hc.hospital.web.handler]

   ;; Routes
   [hc.hospital.web.routes.api]
   [hc.hospital.web.routes.patient-pages]
   [hc.hospital.web.routes.patient-api]
   [kit.edge.db.sql.conman]
   [kit.edge.db.sql.migratus])
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (fn [thread ex]
   (log/error {:what :uncaught-exception
               :exception ex
               :where (str "Uncaught exception on" (.getName thread))})))

(defonce system (atom nil))

(defn stop-app []
  ((or (:stop defaults) (fn [])))
  (some-> (deref system) (ig/halt!)))

(defn start-app [& [params]]
  ((or (:start params) (:start defaults) (fn [])))
  (->> (config/system-config (or (:opts params) (:opts defaults) {}))
       (ig/expand)
       (ig/init)
       (reset! system)))

(defn -main [& _]
  (start-app)
  (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (stop-app) (shutdown-agents)))))
