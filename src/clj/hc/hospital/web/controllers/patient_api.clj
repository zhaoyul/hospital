(ns hc.hospital.web.controllers.patient-api
  (:require
   [clojure.tools.logging :as log]
   [ring.util.http-response :as http-response]
   [cheshire.core :as cheshire]
   [clojure.string :as str])
  (:import [com.hankcs.hanlp HanLP])) ; Import HanLP

(defn- get-pinyin-parts [name-str]
  (if (str/blank? name-str)
    {:pinyin nil :initial nil}
    (let [pinyin-list (HanLP/convertToPinyinList name-str)
          pinyin-full (str/join "" (map #(.getPinyinWithoutTone %) pinyin-list))
          initials (str/join "" (map #(.getShengmu %) pinyin-list))]
      {:pinyin (str/lower-case pinyin-full)
       :initial (str/lower-case initials)})))

(defn submit-assessment! [{{:keys [body]} :parameters :keys [query-fn] :as _request}]
  ;; 接收和处理患者评估数据
  (log/info "接收到患者评估数据:" body)
  (try
    (let [patient-id (get-in body [:basic-info :outpatient-number])
          patient-name (get-in body [:basic-info :name] "")
          {:keys [pinyin initial]} (get-pinyin-parts patient-name)
          assessment-data-json (cheshire/generate-string body)]
      (if (str/blank? patient-id)
        (do
          (log/error "患者ID (patient_id) 未在 basic-info 中提供。")
          (http-response/bad-request {:message "提交失败，患者ID不能为空。"}))
        (let [existing-assessment (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
          (if (seq existing-assessment)
            (do
              (log/info "更新患者评估数据, 患者ID:" patient-id ", 拼音:" pinyin ", 首字母:" initial)
              (query-fn :update-patient-assessment!
                        {:patient_id patient-id
                         :assessment_data assessment-data-json
                         :patient_name_pinyin pinyin
                         :patient_name_initial initial})
              (http-response/ok {:message "评估更新成功！"}))
            (do
              (log/info "插入新的患者评估数据, 患者ID:" patient-id ", 拼音:" pinyin ", 首字母:" initial)
              (query-fn :insert-patient-assessment!
                        {:patient_id patient-id
                         :assessment_data assessment-data-json
                         :patient_name_pinyin pinyin
                         :patient_name_initial initial})
              (http-response/ok {:message "评估提交成功！"}))))))
    (catch Exception e
      (log/error e "提交/更新评估时出错" (ex-message e) (ex-data e))
      (http-response/internal-server-error {:message (str "提交/更新评估时出错: " (ex-message e))}))))

(defn get-assessment-by-patient-id [{{{:keys [patient-id]} :path} :parameters :keys [query-fn] :as _request}]
  (log/info "查询患者评估数据，患者ID:" patient-id)
  (try
    (let [assessment-data-map (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
      (if (seq assessment-data-map)
        (http-response/ok (cheshire/decode (:assessment_data assessment-data-map) keyword))
        (http-response/not-found {:message "未找到该患者的评估数据"})))
    (catch Exception e
      (log/error e "查询评估数据时出错")
      (http-response/internal-server-error {:message "查询评估数据时出错"}))))

(defn get-all-patient-assessments-handler [{:keys [query-fn] {{:keys [name_pinyin name_initial updated_from updated_to]} :query} :parameters :as _request}]
  (log/info "查询所有患者评估数据, 参数:" {:name_pinyin name_pinyin :name_initial name_initial :updated_from updated_from :updated_to updated_to})
  (try
    (let [params-for-query (cond-> {}
                             (not (str/blank? name_pinyin)) (assoc :name_pinyin (str "%" name_pinyin "%"))
                             (not (str/blank? name_initial)) (assoc :name_initial name_initial)
                             (not (str/blank? updated_from)) (assoc :updated_from updated_from)
                             (not (str/blank? updated_to)) (assoc :updated_to updated_to))
          all-assessments-raw (query-fn :get-all-patient-assessments params-for-query)
          parsed-assessments (mapv (fn [assessment]
                                     (cond-> assessment
                                       (:assessment_data assessment) (assoc :assessment_data (cheshire/decode (:assessment_data assessment) keyword))
                                       ;; Ensure pinyin and initial are strings
                                       (:patient_name_pinyin assessment) (update :patient_name_pinyin #(if (keyword? %) (name %) %))
                                       (:patient_name_initial assessment) (update :patient_name_initial #(if (keyword? %) (name %) %))))
                                   all-assessments-raw)]
      (http-response/ok parsed-assessments))
    (catch Exception e
      (log/error e "查询所有评估数据时出错")
      (http-response/internal-server-error {:message "查询所有评估数据时出错"}))))

(defn update-assessment-by-patient-id! [{{{:keys [patient-id]} :path :keys [body]} :parameters :keys [query-fn] :as _request}]
  (log/info "请求更新患者评估数据，患者ID:" patient-id)
  (try
    (let [existing-assessment (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
      (if (seq existing-assessment)
        (let [patient-name (get-in body [:basic-info :name] "") ;; 从请求体中获取患者姓名，可能已更新
              {:keys [pinyin initial]} (get-pinyin-parts patient-name)
              assessment-data-json (cheshire/generate-string body)]
          (query-fn :update-patient-assessment!
                    {:patient_id patient-id
                     :assessment_data assessment-data-json
                     :patient_name_pinyin pinyin
                     :patient_name_initial initial})
          (http-response/ok {:message "评估更新成功！"}))
        (http-response/not-found {:message "未找到该患者的评估数据，无法更新。"})))
    (catch Exception e
      (log/error e "更新评估时出错" {:patient-id patient-id :error (ex-message e) :data (ex-data e)})
      (http-response/internal-server-error {:message (str "更新评估时出错: " (ex-message e))}))))
