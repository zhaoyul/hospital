(ns hc.hospital.web.routes.patient-pages
  (:require
   [hc.hospital.web.controllers.patient :as patient]
   [hc.hospital.web.middleware.exception :as exception]
   [hc.hospital.web.middleware.formats :as formats]
   [hc.hospital.web.pages.layout :as layout]
   [integrant.core :as ig]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]))

(defn wrap-page-defaults []
  (let [error-page (layout/error-page
                    {:status 403
                     :title "无效的防伪令牌"})]
    #(wrap-anti-forgery % {:error-response error-page})))

;; 患者页面特定路由
(defn patient-page-routes [_opts]
  [["/patient/fill" {:get patient/patient-form-page}]]) ;; 患者表单的路由

(def route-data
  {:middleware
   [;; 页面默认中间件
    (wrap-page-defaults)
    ;; query-params & form-params
    parameters/parameters-middleware
    ;; 响应体编码
    muuntaja/format-response-middleware
    ;; 异常处理
    exception/wrap-exception]})

;; 从 :reitit/routes 派生此路由集
(derive :reitit.routes/patient-pages :reitit/routes)

(defmethod ig/init-key :reitit.routes/patient-pages
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  ;; 我们不需要在此再次初始化 selmer，这已在 :reitit.routes/pages 中完成
  (fn [] [base-path route-data (patient-page-routes opts)]))