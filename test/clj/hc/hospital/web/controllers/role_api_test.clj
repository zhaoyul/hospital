(ns hc.hospital.web.controllers.role-api-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :as tu]
            [hc.hospital.db.role :as role-db]
            [cheshire.core :as json]
            [hc.hospital.web.controllers.role-api :as role-ctlr]))

(use-fixtures :once (tu/system-fixture))

(defn handler-and-query []
  (let [sys (tu/system-state)]
    {:handler (:handler/ring sys)
     :query-fn (or (get sys :db.sql/query-fn) (get-in sys [:db.sql/query-fn :query-fn]))}))

(deftest role-api-list
  (let [{:keys [handler]} (handler-and-query)]
    (let [resp (tu/GET handler "/api/roles")]
      (is (= 200 (:status resp)))))
  (let [{:keys [handler]} (handler-and-query)]
    (let [resp (tu/GET handler "/api/permissions")]
      (is (= 200 (:status resp))))))
