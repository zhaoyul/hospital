(ns hc.hospital.web.routes.consent-form-api
  (:require
   [hc.hospital.web.controllers.consent-form-api :as cf]
   [hc.hospital.web.middleware.exception :as exception]
   [hc.hospital.web.middleware.formats :as formats]
   [integrant.core :as ig]
   [reitit.coercion.malli :as malli]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [hc.hospital.specs.consent-form-spec :as cf-spec]))

(def route-data
  {:coercion   malli/coercion
   :muuntaja   formats/instance
   :swagger    {:id :hc.hospital.web.routes.api/api}
   :middleware [parameters/parameters-middleware
                muuntaja/format-negotiate-middleware
                muuntaja/format-response-middleware
                coercion/coerce-exceptions-middleware
                muuntaja/format-request-middleware
                coercion/coerce-response-middleware
                coercion/coerce-request-middleware
                exception/wrap-exception]})

(defn consent-form-api-routes [opts]
  (let [query-fn (:query-fn opts)]
    [["/consent-forms"
      {:post {:summary "保存知情同意书"
              :parameters {:body cf-spec/ConsentFormSpec}
              :handler (fn [req]
                         (cf/save-consent-form! (assoc req :query-fn query-fn)))
              :responses {200 {:body {:message string?}}
                          500 {:body {:message string?}}}}}
      ["/:assessment-id"
       {:get {:summary "获取评估关联的知情同意书"
              :parameters {:path {:assessment-id string?}}
              :handler (fn [req]
                         (cf/get-consent-form (assoc req :query-fn query-fn)))
              :responses {200 {:body map?}
                          404 {:body {:message string?}}
                          500 {:body {:message string?}}}}
        :put {:summary "更新知情同意书"
              :parameters {:path {:assessment-id string?}
                           :body cf-spec/ConsentFormSpec}
              :handler (fn [req]
                         (cf/update-consent-form! (assoc req :query-fn query-fn)))
              :responses {200 {:body {:message string?}}
                          500 {:body {:message string?}}}}}]]]))

(derive :reitit.routes/consent-form-api :reitit/routes)

(defmethod ig/init-key :reitit.routes/consent-form-api
  [_ {:keys [base-path]
      :or {base-path "/api"}
      :as opts}]
  (fn [] [base-path route-data (consent-form-api-routes opts)]))
