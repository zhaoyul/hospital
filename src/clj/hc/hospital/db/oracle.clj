(ns hc.hospital.db.oracle
  (:require
   [integrant.core :as ig]
   [clojure.tools.logging :as log]
   [hugsql.core :as hugsql]) ;; Added HugSQL
  (:import (oracle.jdbc.pool OracleDataSource)))

(defmethod ig/init-key :db.oracle/connection
  [_ {:keys [jdbc-url user password]}]
  (try
    (log/info "Attempting to connect to Oracle database directly using OracleDataSource and configure implicit caching:" jdbc-url)
    (let [ods (OracleDataSource.)]
      (.setURL ods jdbc-url)
      (.setUser ods user)
      (.setPassword ods password)

      (let [cacheProps (java.util.Properties.)]
        (.setProperty cacheProps "ConnectionCachingEnabled" "true")
        (.setProperty cacheProps "ConnectionCacheName" "OracleHISCache")
        (.setProperty cacheProps "MinLimit" "1")
        (.setProperty cacheProps "MaxLimit" "10")
        ;; Add other properties like "InitialLimit", "MaxStatementsLimit" if desired
        (.setConnectionCacheProperties ods cacheProps))

      (.startImplicitConnectionCache ods) ; Start the cache

      (log/info "Successfully configured OracleDataSource and started implicit cache for:" jdbc-url)
      ods)
    (catch Exception e
      (log/error (str "Failed to configure OracleDataSource or start implicit cache (" jdbc-url "). Error: " (.getMessage e)) e)
      (log/warn (str "Application will continue without Oracle DB functionality due to OracleDataSource setup failure."))
      nil)))

(defmethod ig/halt-key! :db.oracle/connection
  [_ ^OracleDataSource datasource]
  (when datasource
    (try
      (log/info "Stopping OracleDataSource implicit connection cache...")
      (.stopImplicitConnectionCache datasource)
      (log/info "Implicit connection cache stopped.")
      (log/info "Closing OracleDataSource...")
      (.close datasource)
      (log/info "OracleDataSource closed.")
      (catch Exception e
        (log/warn (str "Error stopping/closing OracleDataSource: " (.getMessage e)) e)))))

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
