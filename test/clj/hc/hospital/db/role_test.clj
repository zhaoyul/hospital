(ns hc.hospital.db.role-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :as tu]
            [hc.hospital.db.role :as role-db]))

(use-fixtures :once (tu/system-fixture))

(defn qf []
  (let [sys (tu/system-state)]
    (or (get sys :db.sql/query-fn) (get-in sys [:db.sql/query-fn :query-fn]))))

(deftest role-db-basic
  (let [query-fn (qf)]
    (is (seq (role-db/list-roles query-fn)))
    (is (seq (role-db/list-permissions query-fn)))))
