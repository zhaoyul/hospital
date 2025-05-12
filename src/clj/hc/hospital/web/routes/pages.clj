(ns hc.hospital.web.routes.pages
  (:require
   [clojure.tools.logging :as log]
   [hc.hospital.web.controllers.auth :as auth] ; Added auth controller
   [hc.hospital.web.middleware.auth :refer [wrap-restricted]]
   [hc.hospital.web.middleware.exception :as exception]
   [hc.hospital.web.pages.layout :as layout]
   [integrant.core :as ig]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]])) ; Added for protecting home

(defn wrap-page-defaults []
  (let [error-page (layout/error-page
                    {:status 403
                     :title "Invalid anti-forgery token"})]
    #(wrap-anti-forgery % {:error-response error-page})))

(defn home [request]
  (log/info "--- Home Page Request ---") ; << 2. 日志开始标记
  (log/info "Request Keys:" (keys request)) ; 查看请求中所有键
  (log/info "Session from request:" (:session request)) ; << 3. 打印 :session
  (log/info "Identity from request:" (:identity request)) ; << 4. 打印 :identity

  (layout/render request "home.html"))

;; Routes
(defn page-routes [opts]
  [["/" {:get home
         ;;:middleware [wrap-restricted]
         }] ; Protect the home page
   ["/login" {:get auth/login-page
              :post #(auth/handle-login! (assoc % :integrant-deps opts))}]
   ["/logout" {:get auth/handle-logout!}]])

(def route-data
  {:middleware
   [;; Default middleware for pages
    (wrap-page-defaults)
    ;; query-params & form-params
    parameters/parameters-middleware
    ;; encoding response body
    muuntaja/format-response-middleware ; Ensure this is present for HTML responses
    exception/wrap-exception]})

(derive :reitit.routes/pages :reitit/routes)

(defmethod ig/init-key :reitit.routes/pages
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (layout/init-selmer! opts)
  (fn [] [base-path route-data (page-routes opts)]))

