(ns hc.hospital.db.doctor
  (:require
   [hugsql.core :as hugsql]
   ;; [hc.hospital.db.core :refer [db-spec]] ; No longer needed directly
   [buddy.hashers :as hashers]))

(defn create-doctor!
  "创建一个新医生，密码会被哈希处理。"
  [query-fn {:keys [username password name]}]
  (let [hashed-password (hashers/derive password)]
    (query-fn :create-doctor! {:username username :password_hash hashed-password :name name})))

(defn get-doctor-by-username
  "根据用户名获取医生信息。"
  [query-fn username]
  (query-fn :get-doctor-by-username {:username username}))

(defn verify-doctor-credentials
  "验证医生凭证（用户名和密码）。"
  [query-fn username password]
  (when-let [doctor (get-doctor-by-username query-fn username)]
    (when (hashers/check password (:password_hash doctor))
      (dissoc doctor :password_hash)))) ; 登录成功后不返回密码哈希

(defn list-doctors
  "列出所有医生信息 (不包含密码)。"
  [query-fn]
  (query-fn :list-doctors {}))

(defn get-doctor-by-id
  "根据ID获取医生信息 (不包含密码)。"
  [query-fn doctor-id]
  (query-fn :get-doctor-by-id {:id doctor-id}))

(defn update-doctor-password!
  "更新医生密码。"
  [query-fn doctor-id new-password]
  (let [hashed-password (hashers/derive new-password)]
    (query-fn :update-doctor-password! {:id doctor-id :password_hash hashed-password})))

(defn update-doctor-name!
  "更新医生姓名。"
  [query-fn doctor-id new-name]
  (query-fn :update-doctor-name! {:id doctor-id :name new-name}))

(defn delete-doctor!
  "根据ID删除医生。"
  [query-fn doctor-id]
  (query-fn :delete-doctor! {:id doctor-id}))
