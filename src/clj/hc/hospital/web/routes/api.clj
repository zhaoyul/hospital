(ns hc.hospital.web.routes.api
  "公共 API 路由"
  (:require
   [hc.hospital.web.controllers.health :as health]
   [hc.hospital.web.middleware.exception :as exception]
   [hc.hospital.web.middleware.formats :as formats]
   [integrant.core :as ig]
   [reitit.coercion.malli :as malli]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]))

(def route-data
  {:coercion   malli/coercion
   :muuntaja   formats/instance
   :swagger    {:id ::api}
   :middleware [parameters/parameters-middleware
                muuntaja/format-negotiate-middleware
                muuntaja/format-response-middleware
                coercion/coerce-exceptions-middleware
                muuntaja/format-request-middleware
                coercion/coerce-response-middleware
                coercion/coerce-request-middleware
                exception/wrap-exception]})

(defn api-routes
  "构建 API 路由"
  [_opts]
  [["/swagger.json"
    {:get {:no-doc  true
           :swagger {:info {:title "hc.hospital API"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/health"
    ;; 使用 var 以便路由热重载
    {:get #'health/healthcheck!}]])

(derive :reitit.routes/api :reitit/routes)

(defmethod ig/init-key :reitit.routes/api
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (fn [] [base-path route-data (api-routes opts)]))
