(ns hc.hospital.db.oracle
  (:require
   [integrant.core :as ig]
   [hikari-cp.core :as hikari]
   [clojure.tools.logging :as log]
   [hugsql.core :as hugsql]) ;; Added HugSQL
  (:import (com.zaxxer.hikari HikariDataSource)))

(defmethod ig/init-key :db.oracle/connection
  [_ {:keys [jdbc-url user password]}]
  (try
    (let [options {:auto-commit        true
                   :read-only          false
                   :connection-timeout 5000
                   :validation-timeout 2500
                   :idle-timeout       600000
                   :max-lifetime       1800000
                   :minimum-idle       10
                   :maximum-pool-size  20
                   :pool-name          "db-oracle-pool"
                   :adapter            "oracle"
                   :username           user
                   :password           password
                   :jdbc-url           jdbc-url}]
      (log/info "Attempting to connect to Oracle database:" jdbc-url)
      (let [datasource (hikari/make-datasource options)]
        (log/info "Successfully connected to Oracle database:" jdbc-url)
        datasource))
    (catch Exception e
      (log/warn (str "Failed to connect to Oracle database (" jdbc-url "). Application will continue without Oracle DB functionality. Error: " (.getMessage e)))
      nil)))

(defmethod ig/halt-key! :db.oracle/connection
  [_ datasource]
  (when datasource
    (log/info "Closing Oracle database connection pool.")
    (.close ^HikariDataSource datasource)))

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
