(ns hc.hospital.test-utils
  (:require
   [hc.hospital.core :as core]
   [peridot.core :as p]
   [migratus.core :refer [rollback reset]]
   [byte-streams :as bs]
   [integrant.repl.state :as state]))

(defn system-state
  []
  (or @core/system state/system))

(defn system-fixture
  []
  (fn [f]
    (core/start-app {:opts {:profile :test}})
    (reset (:db.sql/migrations (system-state)))
    (f)
    (core/stop-app)))

(defn get-response [ctx]
  (-> ctx
      :response
      (update :body (fnil bs/to-string ""))))

(defn GET [app path params headers]
  (-> (p/session app)
      (p/request path
                 :request-method :get
                 :content-type "application/edn"
                 :headers headers
                 :params params)
      (get-response)))

(defn POST [app path body headers]
  (-> (p/session app)
      (p/request path
                 :request-method :post
                 :content-type "application/json" ; Assuming JSON for POST
                 :headers headers
                 :body (if (string? body) body (bs/to-string body))) ; Ensure body is a string
      (get-response)))

(defn PUT [app path body headers]
  (-> (p/session app)
      (p/request path
                 :request-method :put
                 :content-type "application/json" ; Assuming JSON for PUT
                 :headers headers
                 :body (if (string? body) body (bs/to-string body))) ; Ensure body is a string
      (get-response)))

(defn DELETE [app path headers]
  (-> (p/session app)
      (p/request path
                 :request-method :delete
                 :headers headers)
      (get-response)))
