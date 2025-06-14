;; test/clj/hc/hospital/db/user_test.clj
(ns hc.hospital.db.user-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :as tu]
            [hc.hospital.db.user :as user.db]
            [buddy.hashers :as hashers]
            [integrant.core :as ig]))

;; 使用 fixture 来确保测试环境的系统已启动并配置了测试数据库
(use-fixtures :once (tu/system-fixture))

(defn get-query-fn []
  ;; 从 Integrant 系统中获取 query-fn
  ;; 注意: 具体的 key 路径可能需要根据你的 system.edn 和 core.clj 中的配置调整
  ;; 在你的项目中, query-fn 是直接在 :reitit.routes/api, :reitit.routes/patient-api,
  ;; :reitit.routes/user-api 组件中作为参数传递的.
  ;; 对于直接测试 db 层, 我们需要获取到 :db.sql/query-fn 这个组件.
  (let [system (tu/system-state)]
    (or (get system :db.sql/query-fn) ; 如果直接是函数
        (get-in system [:db.sql/query-fn :query-fn])))) ; 如果是包装过的

(deftest user-db-tests
  (let [query-fn (get-query-fn)
        _ (assert query-fn "Query function should not be nil")]
    (testing "医生注册和获取"
      (let [username "testdoc1"
            password "securepass"
            name "测试医生1"
              _ (user.db/create-user! query-fn {:username username :password password :name name :signature_b64 nil})
            doc (user.db/get-user-by-username query-fn username)]
        (is (some? doc))
        (is (= username (:username doc)))
        (is (= name (:name doc)))
        (is (true? (hashers/check password (:password_hash doc)))))) ; 确保密码已哈希

    (testing "验证医生凭证"
      (let [username "testdoc_auth"
            password "authpass"
            name "认证医生"
              _ (user.db/create-user! query-fn {:username username :password password :name name :signature_b64 nil})
            verified-doc (user.db/verify-credentials query-fn username password)
            unverified-doc (user.db/verify-credentials query-fn username "wrongpass")]
        (is (some? verified-doc))
        (is (= username (:username verified-doc)))
        (is (nil? (:password_hash verified-doc))) ; 验证成功后不应返回哈希
        (is (nil? unverified-doc))))

    (testing "列出医生"
      (let [username2 "testdoc2"
              _ (user.db/create-user! query-fn {:username username2 :password "pass2" :name "医生二" :signature_b64 nil})
            doctors (user.db/list-users query-fn)]
        (is (>= (count doctors) 2)) ; 假设之前测试创建了医生
        (is (every? #(and (:username %) (not (:password_hash %))) doctors))))

    (testing "更新医生姓名和密码"
      (let [username "update_doc"
              _ (user.db/create-user! query-fn {:username username :password "oldpass" :name "旧名字" :signature_b64 nil})
            doc-before-update (user.db/get-user-by-username query-fn username)
            _ (user.db/update-user-name! query-fn (:id doc-before-update) "新名字")
            _ (user.db/update-user-password! query-fn (:id doc-before-update) "newpass")
            doc-after-update (user.db/get-user-by-username query-fn username)]
        (is (= "新名字" (:name doc-after-update)))
        (is (true? (hashers/check "newpass" (:password_hash doc-after-update))))))

    (testing "删除医生"
      (let [username "delete_doc"
              _ (user.db/create-user! query-fn {:username username :password "delpass" :name "待删除" :signature_b64 nil})
            doc-to-delete (user.db/get-user-by-username query-fn username)
            _ (user.db/delete-user! query-fn (:id doc-to-delete))
            deleted-doc (user.db/get-user-by-username query-fn username)]
        (is (some? doc-to-delete))
        (is (nil? deleted-doc))))))