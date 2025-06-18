(ns hc.hospital.web.routes.overview-api
  (:require [hc.hospital.web.controllers.overview-api :as overview]
            [hc.hospital.web.middleware.auth :refer [wrap-restricted]]
            [hc.hospital.web.middleware.exception :as exception]
            [hc.hospital.web.middleware.formats :as formats]
            [hc.hospital.specs.daily-stats-spec :as stats-spec]
            [integrant.core :as ig]
            [reitit.coercion.malli :as malli]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]))

(def route-data
  {:muuntaja formats/instance
   :coercion malli/coercion
   :swagger {:id :hc.hospital.web.routes.overview-api/api}
   :middleware [parameters/parameters-middleware
                muuntaja/format-negotiate-middleware
                muuntaja/format-response-middleware
                coercion/coerce-exceptions-middleware
                muuntaja/format-request-middleware
                exception/wrap-exception]})

(defn overview-api-routes [opts]
  (let [query-fn (:query-fn opts)]
    [["/overview/stats"
      {:get {:summary "获取统计信息"
             :parameters {:query [:map {:closed false}
                                  [:date {:optional true} string?]]}
             :handler (fn [req]
                        (overview/get-stats (assoc req :query-fn query-fn)))}
       :post {:summary "更新统计信息"
              :parameters {:body [:map {:closed false}
                                 [:date {:optional true} string?]
                                 [:total_visits_inc {:optional true} int?]
                                 [:patient_count_inc {:optional true} int?]
                                 [:signed_count_inc {:optional true} int?]
                                 [:inpatient_count_inc {:optional true} int?]
                                 [:outpatient_count_inc {:optional true} int?]
                                 [:assessment_count_inc {:optional true} int?]]}
              :handler (fn [req]
                         (overview/increment-stats! (assoc req :query-fn query-fn)))
              :middleware [wrap-restricted]}}]]))

(derive :reitit.routes/overview-api :reitit/routes)

(defmethod ig/init-key :reitit.routes/overview-api
  [_ {:keys [base-path query-fn]
      :or {base-path "/api"}
      :as opts}]
  (fn [] [base-path route-data (overview-api-routes {:query-fn query-fn})]))
