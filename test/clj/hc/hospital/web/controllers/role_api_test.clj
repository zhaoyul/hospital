(ns hc.hospital.web.controllers.role-api-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :as tu]
            [hc.hospital.db.role :as role-db]
            [cheshire.core :as json]
            [ring.mock.request :as mock]
            [hc.hospital.web.controllers.role-api :as role-ctlr]))

(use-fixtures :once (tu/system-fixture))

(defn handler-and-query []
  (let [sys (tu/system-state)]
    {:handler (:handler/ring sys)
     :query-fn (or (get sys :db.sql/query-fn) (get-in sys [:db.sql/query-fn :query-fn]))}))

(deftest role-api-list
  (let [{:keys [handler query-fn]} (handler-and-query)
        req (-> (mock/request :get "/api/roles")
                 (assoc :identity {:id 1})
                 (assoc :integrant-deps {:query-fn query-fn}))
        resp (handler req)]
    (is (= 200 (:status resp))))
  (let [{:keys [handler query-fn]} (handler-and-query)
        req (-> (mock/request :get "/api/permissions")
                 (assoc :identity {:id 1})
                 (assoc :integrant-deps {:query-fn query-fn}))
        resp (handler req)]
    (is (= 200 (:status resp)))))
