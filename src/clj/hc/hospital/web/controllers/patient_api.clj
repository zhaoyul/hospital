(ns hc.hospital.web.controllers.patient-api
  (:require
   [clojure.tools.logging :as log]
   [ring.util.http-response :as http-response]
   ;; Removed [hc.hospital.db.core :as db]
   [cheshire.core :as cheshire]))

(defn submit-assessment! [{{:keys [body]} :parameters :keys [query-fn] :as _request}]
  ;; 接收和处理患者评估数据
  (log/info "接收到患者评估数据:" body)
  (try
    (let [patient-id (get-in body [:basic-info :outpatient-number])
          assessment-data-json (cheshire/generate-string body)]
      (if (nil? patient-id)
        (do
          (log/error "患者ID (patient_id) 未在 basic-info 中提供。")
          (http-response/bad-request {:message "提交失败，患者ID不能为空。"}))
        (do
          ;; Corrected database call to use query-fn directly
          (query-fn :insert-patient-assessment! {:patient_id patient-id
                                                 :assessment_data assessment-data-json})
          (http-response/ok {:message "评估提交成功！"}))))
    (catch Exception e
      (log/error e "提交评估时出错")
      (http-response/internal-server-error {:message "提交评估时出错"}))))

(defn get-assessment-by-patient-id [{{{:keys [patient-id]} :path} :parameters :keys [query-fn] :as _request}]
  (log/info "查询患者评估数据，患者ID:" patient-id)
  (try
    (let [assessment-data (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
      (if (seq assessment-data)
        (http-response/ok (cheshire/decode (:assessment_data assessment-data) keyword))
        (http-response/not-found {:message "未找到该患者的评估数据"})))
    (catch Exception e
      (log/error e "查询评估数据时出错")
      (http-response/internal-server-error {:message "查询评估数据时出错"}))))

(defn get-all-patient-assessments-handler [{:keys [query-fn] :as _request}]
  (log/info "查询所有患者评估数据")
  (try
    (let [all-assessments-raw (query-fn :get-all-patient-assessments {})
          parsed-assessments (mapv (fn [assessment]
                                     (if (:assessment_data assessment)
                                       (assoc assessment :assessment_data (cheshire/decode (:assessment_data assessment) keyword))
                                       assessment))
                                   all-assessments-raw)]
      (http-response/ok parsed-assessments))
    (catch Exception e
      (log/error e "查询所有评估数据时出错")
      (http-response/internal-server-error {:message "查询所有评估数据时出错"}))))
