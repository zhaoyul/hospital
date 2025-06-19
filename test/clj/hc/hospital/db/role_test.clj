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

(deftest nurse-role-permissions
  (let [query-fn (qf)
        roles (role-db/list-roles query-fn)
        nurse (first (filter #(= "护士" (:name %)) roles))]
    (is (some? nurse) "护士角色应存在")
    (let [perms (role-db/get-permissions-by-role query-fn (:id nurse))]
      (is (not-any? #(= "麻醉管理" (:module %)) perms)
          "护士不应拥有麻醉管理权限"))))
