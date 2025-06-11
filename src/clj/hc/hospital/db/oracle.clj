(ns hc.hospital.db.oracle
  (:require
   [integrant.core :as ig]
   [clojure.tools.logging :as log]
   [conman.core :as conman]))

;; Conman version of init-key for :db.oracle/connection
(defmethod ig/init-key :db.oracle/connection [_ pool-spec]
  (try
    (log/info "Attempting to connect to Oracle database using conman with pool-spec:" pool-spec)
    (conman/connect! pool-spec)
    (catch Exception e
      (log/error (str "Failed to connect to Oracle database using conman. Error: " (.getMessage e)) e)
      (log/warn (str "Application will continue without Oracle DB functionality due to conman connection failure."))
      nil)))

;; Suspend database connection (noop for conman)
(defmethod ig/suspend-key! :db.oracle/connection [_ _])

;; Conman version of halt-key! for :db.oracle/connection
(defmethod ig/halt-key! :db.oracle/connection [_ conn]
  (when conn
    (try
      (log/info "Closing oracle connection pool using conman")
      (conman/disconnect! conn)
      (log/info "Oracle connection pool closed via conman.")
      (catch Exception e
        (log/warn (str "Error closing conman connection pool: " (.getMessage e)) e)))))

;; Conman version of resume-key for :db.oracle/connection
(defmethod ig/resume-key :db.oracle/connection [key opts old-opts old-impl]
  (if (= opts old-opts)
    old-impl
    (do
      (ig/halt-key! key old-impl)
      (ig/init-key key opts))))

;; Conman version of init-key for :db.oracle/query-fn
(defmethod ig/init-key :db.oracle/query-fn
  [_ {:keys [conn options filename filenames env] :or {options {}}}]
  (let [filenames-vec (or filenames [filename])
        load-queries (fn []
                       (log/info "Loading HugSQL queries from files:" filenames-vec)
                       (apply conman/bind-connection-map conn options filenames-vec))]
    ;; queries-dev and queries-prod definitions
    (defn queries-dev [load-fn]
      (fn
        ([query params]
         (conman/query (load-fn) query params))
        ([db query params & opts]
         (apply conman/query db (load-fn) query params opts))))
    (defn queries-prod [load-fn]
      (let [queries (load-fn)]
        (fn
          ([query params]
           (conman/query queries query params))
          ([db query params & opts]
           (apply conman/query db queries query params opts)))))
    ;; Actual return logic:
    (if-not conn
      (do (log/warn "Oracle DB connection is not available for query-fn. Query functions will not be operational.")
          (fn [& args]
            (log/error "Query function called but Oracle connection is not available. Args:" args)
            nil))
      (with-meta
        (if (= env :dev)
          (do
            (log/info "Oracle query-fn running in :dev mode (queries will be reloaded on each call).")
            (queries-dev load-queries))
          (do
            (log/info "Oracle query-fn running in :prod mode (queries loaded once).")
            (queries-prod load-queries)))
        {})))) ; Closes with-meta, if-not's else branch, and the let block

;; Suspend key for query-fn
(defmethod ig/suspend-key! :db.oracle/query-fn [_ _])

;; Resume key for query-fn
(defmethod ig/resume-key :db.oracle/query-fn
  [k {:keys [filename filenames] :as opts} old-opts old-impl]
  (if (= opts old-opts)
    (do (log/info k "resume check for :db.oracle/query-fn. Same configuration, re-using existing component.") old-impl)
    (do (log/info k "resume check for :db.oracle/query-fn. Configuration changed, re-initializing component.")
        (ig/halt-key! k old-impl)
        (ig/init-key k opts))))
