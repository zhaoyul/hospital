(ns hc.hospital.web.controllers.role-api
  (:require [hc.hospital.db.role :as role.db]
            [ring.util.http-response :as http-response]))

(defn list-roles
  [{:keys [query-fn]}]
  (http-response/ok {:roles (role.db/list-roles query-fn)}))

(defn list-permissions
  [{:keys [query-fn]}]
  (http-response/ok {:permissions (role.db/list-permissions query-fn)}))

(defn get-role-permissions
  [{:keys [query-fn] :as req}]
  (let [role-id (-> req :path-params :id Integer/parseInt)
        perms   (role.db/get-permissions-by-role query-fn role-id)]
    (http-response/ok {:permissions perms})))

(defn update-role-permissions!
  [{:keys [query-fn] :as req}]
  (let [role-id (-> req :path-params :id Integer/parseInt)
        {:keys [permission_ids]} (:body-params req)]
    (role.db/set-role-permissions! query-fn role-id permission_ids)
    (http-response/ok {:message "权限已更新"})))

(defn create-role!
  [{:keys [query-fn] :as req}]
  (let [name (get-in req [:body-params :name])]
    (if (clojure.string/blank? name)
      (http-response/bad-request {:error "角色名不能为空"})
      (do (role.db/create-role! query-fn name)
          (http-response/ok {:message "角色创建成功"})))))

(defn delete-role!
  [{:keys [query-fn] :as req}]
  (let [role-id (-> req :path-params :id Integer/parseInt)]
    (role.db/delete-role! query-fn role-id)
    (http-response/ok {:message "角色已删除"})))
