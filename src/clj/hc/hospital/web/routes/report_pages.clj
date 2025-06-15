(ns hc.hospital.web.routes.report-pages
  (:require
   [hc.hospital.web.controllers.report :as report]
   [hc.hospital.web.middleware.exception :as exception]
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

(defn report-page-routes [_opts]
  [["/report/sedation-consent" {:get report/sedation-consent-page}]])

(def route-data
  {:middleware [(wrap-page-defaults)
                parameters/parameters-middleware
                muuntaja/format-response-middleware
                exception/wrap-exception]})

(derive :reitit.routes/report-pages :reitit/routes)

(defmethod ig/init-key :reitit.routes/report-pages
  [_ {:keys [base-path]
      :or {base-path ""}
      :as opts}]
  (fn [] [base-path route-data (report-page-routes opts)]))
