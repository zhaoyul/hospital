(ns hc.hospital.web.routes.patient-api
  (:require
   [hc.hospital.web.controllers.patient-api :as patient-api]
   [hc.hospital.web.middleware.exception :as exception]
   [hc.hospital.web.middleware.formats :as formats]
   [integrant.core :as ig]
   [reitit.coercion.malli :as malli]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]))

;; API 路由数据
(def route-data
  {:coercion   malli/coercion
   :muuntaja   formats/instance

   :swagger    {:id :hc.hospital.web.routes.api/api} ;; 使用与主 API 相同的 Swagger ID
   :middleware [;; query-params & form-params
                parameters/parameters-middleware
                ;; 内容协商
                muuntaja/format-negotiate-middleware
                ;; 编码响应体
                muuntaja/format-response-middleware
                ;; 异常处理
                coercion/coerce-exceptions-middleware
                ;; 解码请求体
                muuntaja/format-request-middleware
                ;; 强制转换响应体
                coercion/coerce-response-middleware
                ;; 强制转换请求参数
                coercion/coerce-request-middleware
                ;; 异常处理
                exception/wrap-exception]})

;; 患者 API 特定路由
(defn patient-api-routes [opts]
  (let [query-fn (:query-fn opts)
        oracle-query-fn (:oracle-query-fn opts)] ; 从 opts 获取 oracle-query-fn
    [["/patient" ; Start of /patient routes group
      ["/assessment" {:post {:summary "提交患者评估信息"
                             :description "接收并存储患者填写的评估表单信息"
                             :tags ["患者"]
                             :handler (fn [request]
                                        (patient-api/submit-assessment! (assoc request :query-fn query-fn)))
                             :parameters {:body map?}
                             :responses {200 {:body {:message string?}}
                                         400 {:body {:message string?}} ; Added for bad request (e.g. missing patient-id)
                                         500 {:body {:message string?}}}}}]


      ["/assessment/:patient-id" {:get {:summary "查询指定患者的评估信息"
                                        :description "根据患者ID查询存储的评估表单信息"
                                        :tags ["患者"]
                                        :handler (fn [request]
                                                   (patient-api/get-assessment-by-patient-id (assoc request :query-fn query-fn)))
                                        :parameters {:path {:patient-id string?}}
                                        :responses {200 {:body map?} ; Changed from coll? to map? as it returns a single assessment
                                                    404 {:body {:message string?}}
                                                    500 {:body {:message string?}}}}
                                  :put {:summary "更新指定患者的评估信息"
                                        :description "根据患者ID更新已存储的评估表单信息"
                                        :tags ["患者"]
                                        :parameters {:path {:patient-id string?} :body map?}
                                        :handler (fn [request]
                                                   (patient-api/update-assessment-by-patient-id! (assoc request :query-fn query-fn)))
                                        :responses {200 {:body {:message string?}}
                                                    404 {:body {:message string?}}
                                                    500 {:body {:message string?}}}}}]
      ;; 通过患者ID (REGISTER_NO, PATIENT_ID, 或 INP_NO) 查询患者信息并创建记录的路由
      ["/find-by-id/:patientIdInput" {:get {:summary "通过ID查找患者信息"
                                            :description "根据患者的 REGISTER_NO, PATIENT_ID 或 INP_NO 查询患者信息，并在系统中创建记录"
                                            :tags ["患者"]
                                            :parameters {:path {:patientIdInput string?}}
                                            :handler (fn [request]
                                                       (patient-api/find-patient-by-id-handler
                                                        (assoc request
                                                               :query-fn query-fn
                                                               :oracle-query-fn oracle-query-fn))) ; 传递 oracle-query-fn
                                            :responses {200 {:body map?}
                                                        404 {:body {:message string?}}
                                                        500 {:body {:message string?}}}}}]

      ["/assessments" {:get {:summary "查询所有患者的评估信息列表"
                             :description "获取所有已存储的患者评估表单信息"
                             :tags ["患者"]
                             :parameters {:query [:map {:closed false}
                                                  [:name_pinyin {:optional true} string?]
                                                  [:name_initial {:optional true} string?]
                                                  [:updated_from {:optional true} string?]
                                                  [:updated_to {:optional true} string?]]}
                             :handler (fn [request]
                                        (patient-api/get-all-patient-assessments-handler (assoc request :query-fn query-fn)))
                             :responses {200 {:body coll?} ; Returns a collection of assessment objects
                                         500 {:body {:message string?}}}}}]

      ]])) ; Corrected closing bracket

;; 从 :reitit/routes 派生此路由集
(derive :reitit.routes/patient-api :reitit/routes)

(defmethod ig/init-key :reitit.routes/patient-api
  [_ {:keys [base-path] ; query-fn is available in opts
      :or   {base-path "/api"}
      :as   opts}] ; opts is the component config, including :query-fn from system.edn
  (fn [] [base-path route-data (patient-api-routes opts)])) ; Pass opts (which includes query-fn)
