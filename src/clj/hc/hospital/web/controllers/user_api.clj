(ns hc.hospital.web.controllers.user-api
  (:require
   [hc.hospital.db.user :as user.db]
   [ring.util.http-response :as http-response]
   [buddy.auth :as auth]))

(defn register-user!
  "注册新用户 API"
  [{{:keys [username password name role signature_b64]} :body-params
    {:keys [query-fn]} :integrant-deps}]
  (if (or (empty? username) (empty? password))
    (http-response/bad-request {:error "用户名和密码不能为空"})
    (try
      (user.db/create-user! query-fn {:username username
                                      :password password
                                      :name name
                                      :role (or role "麻醉医生")
                                      :signature_b64 signature_b64})
      (http-response/ok {:message "用户注册成功"})
      (catch Exception e
        (if (re-find #"UNIQUE constraint failed: doctors.username" (.getMessage e))
          (http-response/conflict {:error "用户名已存在"})
          (do
            (println "注册用户时发生错误:" e)
            (http-response/internal-server-error {:error "注册用户失败，请查看服务器日志。"})))))))

(defn login-user!
  "用户登录 API"
  [{{:keys [username password]} :body-params
    :keys [session]
    {:keys [query-fn]} :integrant-deps :as m}]
  (if (or (empty? username) (empty? password))
    (http-response/bad-request {:error "用户名和密码不能为空"})
    (if-let [doctor (user.db/verify-credentials query-fn username password)]
      (let [session (assoc session :identity (:id doctor))] ; buddy.auth 使用 :identity
        (-> (http-response/ok {:message "登录成功" :doctor (dissoc doctor :password_hash)})
            (assoc :session session)))
      (http-response/unauthorized {:error "用户名或密码错误"}))))

(defn logout-user!
  "用户登出 API"
  [_req]
  (-> (http-response/ok {:message "登出成功"})
      (assoc :session nil) ; Important for wrap-defaults to clear its idea of the session
      (assoc ::force-expire-cookie true))) ; Marker for our custom middleware

(defn get-current-user-profile
  [{{:keys [query-fn]} :integrant-deps
    authenticated-doctor-id :identity}] ; :identity is injected by Buddy auth
  (if authenticated-doctor-id
    (if-let [doctor (user.db/get-user-by-id query-fn authenticated-doctor-id)]
      (http-response/ok {:doctor (dissoc doctor :password_hash)}) ; Return doctor details
      (http-response/not-found {:error "Logged-in doctor data not found in DB."})) ; Should be rare if ID is from session
    (http-response/unauthorized {:error "Not authenticated"})))

(defn list-users
  "获取用户列表 API (需要认证)"
  [{{:keys [query-fn]} :integrant-deps}]
  (if-let [doctors (user.db/list-users query-fn)]
    (http-response/ok {:doctors doctors})
    (http-response/internal-server-error {:error "获取医生列表失败"})))

(defn get-user-by-id
  "根据ID获取用户信息 API (需要认证)"
  [{{:keys [id]} :path-params
    {:keys [query-fn]} :integrant-deps}]
  (if-let [doctor (user.db/get-user-by-id query-fn (Integer/parseInt id))]
    (http-response/ok {:doctor doctor})
    (http-response/not-found {:error "未找到指定ID的医生"})))

(defn update-user-info!
  "更新用户信息 API (需要认证, 允许修改姓名、角色和签名)"
  [{{:keys [id]} :path-params
    {:keys [name role signature_b64]} :body-params
    {:keys [query-fn]} :integrant-deps
    _authenticated-doctor :identity}]
  (try
    (user.db/update-user-info!
     query-fn (Integer/parseInt id)
     {:name name :role role :signature_b64 signature_b64})
    (http-response/ok {:message "用户信息更新成功"})
    (catch Exception e
      (println "更新用户信息时发生错误:" e)
      (http-response/internal-server-error {:error "更新用户信息失败"}))))

(defn update-user-password!
  "更新用户密码 API (需要认证, 假设只允许用户更新自己的密码)"
  [{{:keys [id]} :path-params
    {:keys [new_password]} :body-params ; 注意前端发送的字段名
    {:keys [query-fn]} :integrant-deps
    authenticated-doctor :identity}]
  (if (empty? new_password)
    (http-response/bad-request {:error "新密码不能为空"})
    (if-not (= (Integer/parseInt id) authenticated-doctor)
      (http-response/forbidden {:error "无权修改其他医生密码"})
      (try
        (user.db/update-user-password! query-fn (Integer/parseInt id) new_password)
        (http-response/ok {:message "用户密码更新成功"})
        (catch Exception e
          (println "更新用户密码时发生错误:" e)
          (http-response/internal-server-error {:error "更新用户密码失败"}))))))

(defn update-user-role!
  "更新用户角色 API (需要管理员权限)"
  [{{:keys [id]} :path-params
    {:keys [role]} :body-params
    {:keys [query-fn]} :integrant-deps}]
  (try
    (user.db/update-user-role! query-fn (Integer/parseInt id) role)
    (http-response/ok {:message "角色更新成功"})
    (catch Exception e
      (println "更新用户角色时发生错误:" e)
      (http-response/internal-server-error {:error "更新用户角色失败"}))))

(defn delete-user!
  "删除用户 API (需要认证, 通常应限制为管理员权限)"
  [{{:keys [id]} :path-params
    {:keys [query-fn]} :integrant-deps
    authenticated-doctor :identity}] ; 示例中简单检查是否登录，实际应有更严格权限
   ;; 实际应用中，删除操作应有更严格的权限控制，例如检查 authenticated-doctor 是否为管理员
  (try
    (user.db/delete-user! query-fn (Integer/parseInt id))
    (http-response/ok {:message "用户删除成功"})
    (catch Exception e
      (println "删除用户时发生错误:" e)
      (http-response/internal-server-error {:error "删除用户失败"}))))
