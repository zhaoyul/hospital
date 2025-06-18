(ns hc.hospital.web.routes.role-api
  (:require [hc.hospital.web.controllers.role-api :as role-api]
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
   :coercion malli/coercion
   :swagger {:id :hc.hospital.web.routes.role-api/role-api}
   :middleware [parameters/parameters-middleware
                muuntaja/format-negotiate-middleware
                muuntaja/format-response-middleware
                coercion/coerce-exceptions-middleware
                muuntaja/format-request-middleware
                exception/wrap-exception]})

(defn role-api-routes [opts]
  [["/roles"
    {:get {:summary "获取角色列表"
           :handler (fn [req]
                      (role-api/list-roles (assoc req :integrant-deps opts)))
           :middleware [wrap-restricted]}
     :post {:summary "创建角色"
            :parameters {:body {:name string?}}
            :handler (fn [req]
                       (role-api/create-role! {:integrant-deps opts
                                               :body-params (:body-params req)}))
            :middleware [wrap-restricted]}}]
   ["/roles/:id/permissions"
    {:get {:summary "获取角色权限"
           :parameters {:path {:id int?}}
           :handler (fn [req]
                      (role-api/get-role-permissions {:integrant-deps opts
                                                       :path-params (:path-params req)}))
           :middleware [wrap-restricted]}
     :put {:summary "更新角色权限"
           :parameters {:path {:id int?}
                        :body {:permission_ids [int?]}}
           :handler (fn [req]
                      (role-api/update-role-permissions! {:integrant-deps opts
                                                           :path-params (:path-params req)
                                                           :body-params (:body-params req)}))
           :middleware [wrap-restricted]}}]
   ["/roles/:id"
    {:delete {:summary "删除角色"
              :parameters {:path {:id int?}}
              :handler (fn [req]
                         (role-api/delete-role! {:integrant-deps opts
                                                  :path-params (:path-params req)}))
              :middleware [wrap-restricted]}}]
   ["/permissions"
    {:get {:summary "获取权限列表"
           :handler (fn [req]
                      (role-api/list-permissions (assoc req :integrant-deps opts)))
           :middleware [wrap-restricted]}}]])

(derive :reitit.routes/role-api :reitit/routes)

(defmethod ig/init-key :reitit.routes/role-api
  [_ {:keys [base-path query-fn]
      :or {base-path "/api"}
      :as opts}]
  (fn [] [base-path route-data (role-api-routes {:query-fn query-fn})]))
