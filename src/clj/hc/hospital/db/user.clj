(ns hc.hospital.db.user
  (:require [buddy.hashers :as hashers]))

(defn create-user!
  "创建新用户, 密码会被哈希处理."
  [query-fn {:keys [username password name role signature_b64]}]
  (let [hashed (hashers/derive password)
        r (or role "麻醉医生")]
    (query-fn :create-user!
              {:username username
               :password_hash hashed
               :name name
               :role r
               :signature_b64 signature_b64})))

(defn get-user-by-username [query-fn username]
  (query-fn :get-user-by-username {:username username}))

(defn verify-credentials [query-fn username password]
  (when-let [user (get-user-by-username query-fn username)]
    (when (hashers/check password (:password_hash user))
      (dissoc user :password_hash))))

(defn list-users [query-fn]
  (query-fn :list-users {}))

(defn get-user-by-id [query-fn user-id]
  (query-fn :get-user-by-id {:id user-id}))

(defn update-user-password! [query-fn user-id new-password]
  (let [hashed (hashers/derive new-password)]
    (query-fn :update-user-password! {:id user-id :password_hash hashed})))

(defn update-user-name! [query-fn user-id new-name]
  (query-fn :update-user-name! {:id user-id :name new-name}))

(defn update-user-role! [query-fn user-id new-role]
  (query-fn :update-user-role! {:id user-id :role new-role}))

(defn update-user-info!
  "同时更新用户姓名、角色和签名"
  [query-fn user-id {:keys [name role signature_b64]}]
  (query-fn :update-user-info!
            {:id user-id :name name :role role :signature_b64 signature_b64}))

(defn delete-user! [query-fn user-id]
  (query-fn :delete-user! {:id user-id}))
