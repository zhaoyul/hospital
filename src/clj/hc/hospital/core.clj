(ns hc.hospital.core
  (:require
   [taoensso.timbre :as log]
   [integrant.core :as ig]
   [hc.hospital.web.routes.pages]
   [hc.hospital.config :as config]
   [hc.hospital.env :refer [defaults]]
   [clojure.java.io :as io]

   ;; Edges
   [kit.edge.server.undertow]
   [hc.hospital.web.handler]

   ;; Routes
   [hc.hospital.web.routes.api]
   [hc.hospital.web.routes.patient-pages]
   [hc.hospital.web.routes.patient-api]
   [kit.edge.db.sql.conman]
   [hc.hospital.db.oracle]
   [kit.edge.db.sql.migratus])
  (:import [java.util Properties])
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
  (let [properties-path "hc/hospital/version.properties"]
    (try
      (if-let [resource-url (io/resource properties-path)]
        (with-open [is (io/input-stream resource-url)]
          (let [props (Properties.)]
            (.load props is)
            (println "Starting hc.hospital application...")
            (println (str "Build Timestamp: " (.getProperty props "build.timestamp" "UNKNOWN")))
            (println (str "Git Commit Hash: " (.getProperty props "git.commit.hash" "UNKNOWN")))))
        (do ;; Explicitly use do for multiple expressions in if's false branch if needed, though here only one.
          (println "Starting hc.hospital application...") ;; Print this regardless of version file
          (println (str "WARNING: Version properties file not found at " properties-path))))
      (catch Exception e
        (println "Starting hc.hospital application...") ;; Print this regardless of version file
        (println (str "ERROR: Could not load version information from " properties-path))
        (println (.getMessage e)))))
  (start-app)
  (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (stop-app) (shutdown-agents)))))
