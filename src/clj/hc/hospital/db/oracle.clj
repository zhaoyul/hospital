(ns hc.hospital.db.oracle
  (:require
   [integrant.core :as ig]
   [clojure.tools.logging :as log]
   [hugsql.core :as hugsql]) ;; Added HugSQL
  (:import (oracle.jdbc.pool OracleDataSource)))

(defmethod ig/init-key :db.oracle/connection
  [_ {:keys [jdbc-url user password]}]
  (try
    (log/info "Attempting to connect to Oracle database directly using OracleDataSource:" jdbc-url)
    (let [ods (doto (OracleDataSource.)
                (.setURL jdbc-url)
                (.setUser user)
                (.setPassword password)
                (.setConnectionCachingEnabled true)
                (.setConnectionCacheName "OracleHISCache")
                ;; Optional: Add more cache properties if needed via .setConnectionCacheProperties
                )]
      ;; To actually start the cache and make it effective,
      ;; OracleDataSource might need its connection pool properties set,
      ;; (e.g., MinLimit, MaxLimit) via a properties object
      ;; passed to setConnectionCacheProperties, and then startImplicitConnectionCache
      ;; However, setConnectionCachingEnabled alone might be enough for basic behavior.
      ;; For robust pooling, one might need:
      ;; (let [cacheProps (java.util.Properties.)]
      ;;  (.setProperty cacheProps "MinLimit" "3")
      ;;  (.setProperty cacheProps "MaxLimit" "10")
      ;;  (.setConnectionCacheProperties ods cacheProps)
      ;;  (.startImplicitConnectionCache ods) ; May not be needed if properties auto-start
      ;; )
      ;; For now, keeping it simple as per plan.
      (log/info "Successfully configured OracleDataSource for:" jdbc-url)
      ods)
    (catch Exception e
      (log/error (str "Failed to configure OracleDataSource (" jdbc-url "). Error: " (.getMessage e)) e) ; Log full exception
      (log/warn (str "Application will continue without Oracle DB functionality due to OracleDataSource setup failure."))
      nil)))

(defmethod ig/halt-key! :db.oracle/connection
  [_ ^OracleDataSource datasource] ; Type hint OracleDataSource
  (when datasource
    (try
      (log/info "Closing OracleDataSource and its implicit connection cache.")
      ;; If implicit cache was explicitly started with startImplicitConnectionCache(),
      ;; it should be stopped with stopImplicitConnectionCache().
      ;; However, .close() on the OracleDataSource itself is generally the way
      ;; to release its resources, including any implicit pool.
      (.close datasource)
      (log/info "OracleDataSource closed.")
      (catch Exception e
        (log/warn (str "Error closing OracleDataSource: " (.getMessage e)) e)))))

;; Function to create a NOP (no-operation) query function map
(defn create-nop-oracle-query-fn [filename]
  (log/warn (str "Oracle DB connection is not available. Using NOP query functions for file: " filename))
  (fn
    ([_query-name] (log/warn "Oracle NOP query called") nil)
    ([_query-name _params] (log/warn "Oracle NOP query called with params") nil)))

(defmethod ig/init-key :db.oracle/query-fn
  [_ {:keys [conn filename options] :as config}]
  (if conn
    (do
      (log/info "Oracle connection available, initializing HugSQL functions for file:" filename)
      ;; This assumes that standard HugSQL functions are generated using map-of-db-fns
      ;; and then they use the provided :conn.
      ;; The actual Conman/Kit integration might use a slightly different helper,
      ;; but the principle is that it needs the 'conn'.
      ;; We are essentially re-implementing what conman's query-fn-component would do here.
      (hugsql/map-of-db-fns filename (assoc options :db conn)))
    (create-nop-oracle-query-fn filename)))

;; No halt-key! needed for :db.oracle/query-fn as it doesn't own resources itself.
;; The connection it uses is managed by :db.oracle/connection's halt-key!
