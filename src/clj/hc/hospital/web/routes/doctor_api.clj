(ns hc.hospital.web.routes.doctor-api
  (:require
   [hc.hospital.web.controllers.doctor-api :as doctor-api]
   [hc.hospital.web.middleware.auth :refer [wrap-restricted]]
   [hc.hospital.web.middleware.exception :as exception]
   [reitit.ring.coercion :as coercion]
   [hc.hospital.web.middleware.formats :as formats]
   [integrant.core :as ig]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]))

(def route-data
  {:muuntaja formats/instance
   :swagger    {:id :hc.hospital.web.routes.api/api}
   :middleware [parameters/parameters-middleware
                muuntaja/format-negotiate-middleware
                muuntaja/format-response-middleware
                coercion/coerce-exceptions-middleware
                muuntaja/format-request-middleware
                exception/wrap-exception]})

;; API routes for doctors
(defn doctor-api-routes [_opts]
  [["/doctors"
    {:get {:summary "获取医生列表 (需要认证)"
           :handler doctor-api/list-doctors
           :middleware [wrap-restricted]}}
    {:post {:summary "注册新医生"
            :parameters {:body {:username string? :password string? :name string?}}
            :handler doctor-api/register-doctor!}}]
   ["/doctors/login"
    {:post {:summary "医生登录"
            :parameters {:body {:username string? :password string?}}
            :handler doctor-api/login-doctor!}}]
   ["/doctors/logout"
    {:post {:summary "医生登出 (需要认证)"
            :handler doctor-api/logout-doctor!
            :middleware [wrap-restricted]}}]
   ["/doctors/:id"
    {:get {:summary "根据ID获取医生信息 (需要认证)"
           :parameters {:path {:id int?}}
           :handler doctor-api/get-doctor-by-id
           :middleware [wrap-restricted]}}
    {:put {:summary "更新医生姓名 (需要认证，医生只能更新自己的信息)"
           :parameters {:path {:id int?} :body {:name string?}}
           :handler doctor-api/update-doctor-name!
           :middleware [wrap-restricted]}}
    {:delete {:summary "删除医生 (需要认证，通常管理员权限)"
              :parameters {:path {:id int?}}
              :handler doctor-api/delete-doctor!
              :middleware [wrap-restricted]}}]
   ["/doctors/:id/password"
    {:put {:summary "更新医生密码 (需要认证，医生只能更新自己的密码)"
           :parameters {:path {:id int?} :body {:new_password string?}}
           :handler doctor-api/update-doctor-password!
           :middleware [wrap-restricted]}}]])

;; Derive this route set from :reitit/routes
(derive :reitit.routes/doctor-api :reitit/routes)

(defmethod ig/init-key :reitit.routes/doctor-api
  [_ {:keys [base-path query-fn] 
      :or {base-path "/api"}
      :as opts}]
  (fn [] [base-path route-data (doctor-api-routes (assoc opts :query-fn query-fn))]))
