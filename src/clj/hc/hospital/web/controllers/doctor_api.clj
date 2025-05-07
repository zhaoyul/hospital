(ns hc.hospital.web.controllers.doctor-api
  (:require
   [hc.hospital.db.doctor :as doctor.db]
   [ring.util.http-response :as http-response]
   [buddy.auth :as auth]))

(defn register-doctor!
  "注册新医生 API"
  [{{:keys [username password name]} :body-params
    {:keys [query-fn]} :integrant-deps}]
  (if (or (empty? username) (empty? password))
    (http-response/bad-request {:error "用户名和密码不能为空"})
    (try
      (doctor.db/create-doctor! query-fn {:username username :password password :name name})
      (http-response/ok {:message "医生注册成功"})
      (catch Exception e
        (if (re-find #"UNIQUE constraint failed: doctors.username" (.getMessage e))
          (http-response/conflict {:error "用户名已存在"})
          (do
            (println "注册医生时发生错误:" e)
            (http-response/internal-server-error {:error "注册医生失败，请查看服务器日志。"})))))))

(defn login-doctor!
  "医生登录 API"
  [{{:keys [username password]} :body-params
    :keys [session]
    {:keys [query-fn]} :integrant-deps :as m}]
  (if (or (empty? username) (empty? password))
    (http-response/bad-request {:error "用户名和密码不能为空"})
    (if-let [doctor (doctor.db/verify-doctor-credentials query-fn username password)]
      (let [session (assoc session :identity (:id doctor))] ; buddy.auth 使用 :identity
        (-> (http-response/ok {:message "登录成功" :doctor (dissoc doctor :password_hash)})
            (assoc :session session)))
      (http-response/unauthorized {:error "用户名或密码错误"}))))

(defn logout-doctor!
  "医生登出 API"
  [_req]
  (-> (http-response/ok {:message "登出成功"})
      (assoc :session nil))) ; 清除 session 中的 :identity

(defn list-doctors
  "获取医生列表 API (需要认证)"
  [{{:keys [query-fn]} :integrant-deps}]
  (if-let [doctors (doctor.db/list-doctors query-fn)]
    (http-response/ok {:doctors doctors})
    (http-response/internal-server-error {:error "获取医生列表失败"})))

(defn get-doctor-by-id
  "根据ID获取医生信息 API (需要认证)"
  [{{:keys [id]} :path-params
    {:keys [query-fn]} :integrant-deps}]
  (if-let [doctor (doctor.db/get-doctor-by-id query-fn (Integer/parseInt id))]
    (http-response/ok {:doctor doctor})
    (http-response/not-found {:error "未找到指定ID的医生"})))

(defn update-doctor-name!
  "更新医生姓名 API (需要认证, 假设只允许医生更新自己的名字或管理员操作)"
  [{{:keys [id]} :path-params
    {:keys [name]} :body-params
    {:keys [query-fn]} :integrant-deps
    authenticated-doctor :identity}] ; 从 session 中获取已认证的医生ID
  ;; 简单示例：这里可以添加权限检查，例如是否是管理员，或者医生ID是否匹配 authenticated-doctor
  (if-not (= (Integer/parseInt id) authenticated-doctor)
    (http-response/forbidden {:error "无权修改其他医生信息"})
    (try
      (doctor.db/update-doctor-name! query-fn (Integer/parseInt id) name)
      (http-response/ok {:message "医生姓名更新成功"})
      (catch Exception e
        (println "更新医生姓名时发生错误:" e)
        (http-response/internal-server-error {:error "更新医生姓名失败"})))))

(defn update-doctor-password!
  "更新医生密码 API (需要认证, 假设只允许医生更新自己的密码)"
  [{{:keys [id]} :path-params
    {:keys [new_password]} :body-params ; 注意前端发送的字段名
    {:keys [query-fn]} :integrant-deps
    authenticated-doctor :identity}]
  (if (empty? new_password)
    (http-response/bad-request {:error "新密码不能为空"})
    (if-not (= (Integer/parseInt id) authenticated-doctor)
      (http-response/forbidden {:error "无权修改其他医生密码"})
      (try
        (doctor.db/update-doctor-password! query-fn (Integer/parseInt id) new_password)
        (http-response/ok {:message "医生密码更新成功"})
        (catch Exception e
          (println "更新医生密码时发生错误:" e)
          (http-response/internal-server-error {:error "更新医生密码失败"}))))))


(defn delete-doctor!
  "删除医生 API (需要认证, 通常应限制为管理员权限)"
  [{{:keys [id]} :path-params
    {:keys [query-fn]} :integrant-deps
    authenticated-doctor :identity}] ; 示例中简单检查是否登录，实际应有更严格权限
   ;; 实际应用中，删除操作应有更严格的权限控制，例如检查 authenticated-doctor 是否为管理员
  (try
    (doctor.db/delete-doctor! query-fn (Integer/parseInt id))
    (http-response/ok {:message "医生删除成功"})
    (catch Exception e
      (println "删除医生时发生错误:" e)
      (http-response/internal-server-error {:error "删除医生失败"}))))
