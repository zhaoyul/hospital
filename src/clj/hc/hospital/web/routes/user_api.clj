(ns hc.hospital.web.routes.user-api
  (:require
   [hc.hospital.web.controllers.user-api :as user-api]
   [hc.hospital.web.middleware.auth :refer [wrap-restricted]]
   [hc.hospital.web.middleware.exception :as exception]
   [hc.hospital.web.middleware.formats :as formats]
   [integrant.core :as ig]
   [reitit.coercion.malli :as malli]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]))

(def route-data
  {:muuntaja formats/instance
   :coercion   malli/coercion
   :swagger    {:id :hc.hospital.web.routes.api/api}
   :middleware [parameters/parameters-middleware
                muuntaja/format-negotiate-middleware
                muuntaja/format-response-middleware
                coercion/coerce-exceptions-middleware
                muuntaja/format-request-middleware
                exception/wrap-exception]})

;; API routes for users
(defn user-api-routes [opts]
  [["/users"
    {:get {:summary "获取用户列表 (需要认证)"
           :handler (fn [req] (user-api/list-users (assoc req :integrant-deps opts)))
           :tags ["医生用户"]
           :middleware [wrap-restricted]}
     :post {:summary "注册新用户"
           :tags ["医生用户"]
           :parameters {:body {:username string? :password string? :name string? :role string?}}
           :handler #(user-api/register-user! {:integrant-deps opts :body-params (-> % :body-params)})}}
    ]
   ["/users/login"
    {:post {:summary "用户登录"
            :tags ["医生用户"]
            :parameters {:body {:username string? :password string?}}
            :handler #(user-api/login-user! {:integrant-deps opts :body-params (-> % :body-params)})}}]
   ["/users/logout"
    {:post {:summary "用户登出 (需要认证)"
            :tags ["医生用户"]
            :handler user-api/logout-user!
            :middleware [wrap-restricted]}}]
   ["/me" ; Changed from "/user/me"
    {:get {:summary "获取当前登录用户的信息 (需要认证)"
           :handler #(user-api/get-current-user-profile
                      {:integrant-deps opts
                       :identity (-> % :identity :id)})
           :tags ["医生用户"]
           :middleware [wrap-restricted]}}]
   ["/user/:id"
    {:get {:summary "根据ID获取用户信息 (需要认证)"
           :tags ["医生用户"]
           :parameters {:path {:id int?}}
           :handler (fn [req] (user-api/get-user-by-id (assoc req :integrant-deps opts)))
           :middleware [wrap-restricted]}
     :put {:summary "更新用户信息 (需要认证，用户只能更新自己的信息)"
           :tags ["医生用户"]
           :parameters {:path {:id int?}
                        :body {:name string?
                               :role string?
                               :signature_b64 [:maybe string?]}}
           :handler (fn [req]
                      (user-api/update-user-info!
                       (assoc req :integrant-deps opts)))
           :middleware [wrap-restricted]}
     :delete {:summary "删除用户 (需要认证，通常管理员权限)"
              :tags ["医生用户"]
              :parameters {:path {:id int?}}
              :handler (fn [req] (user-api/delete-user! (assoc req :integrant-deps opts)))
              :middleware [wrap-restricted]}}]
   ["/user/:id/role"
    {:put {:summary "更新用户角色 (需要管理员)"
           :tags ["医生用户"]
           :parameters {:path {:id int?} :body {:role string?}}
           :handler (fn [req]
                      (user-api/update-user-role!
                       (assoc req :integrant-deps opts)))
           :middleware [wrap-restricted]}}]
   ["/user/:id/password"
    {:put {:summary "更新用户密码 (需要认证，用户只能更新自己的密码)"
           :tags ["医生用户"]
           :parameters {:path {:id int?} :body {:new_password string?}}
           :handler (fn [req]
                      (user-api/update-user-password!
                       (assoc req :integrant-deps opts)))
           :middleware [wrap-restricted]}}]])

;; Derive this route set from :reitit/routes
(derive :reitit.routes/user-api :reitit/routes)

(defmethod ig/init-key :reitit.routes/user-api
  [_ {:keys [base-path query-fn]
      :or {base-path "/api"}
      :as opts}]
  (fn [] [base-path route-data (user-api-routes (assoc opts :query-fn query-fn))]))
