(ns hc.hospital.web.controllers.patient-api
  (:require
   [cheshire.core :as cheshire]
   [cheshire.core :as cheshire]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [ring.util.http-response :as http-response]
   [hc.hospital.db.his-patient-queries :as his-queries]) ; 新增的 require
  (:import
   [com.hankcs.hanlp HanLP]
   [java.time Instant]))

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
    (let [current-time (str (Instant/now))
          raw-basic-info (:basic-info body {})
          raw-medical-summary (:medical-summary body {})
          raw-comorbidities (:comorbidities body {})
          raw-aux-exams (:auxiliary-examination body {})
          raw-physical-exam (:physical-examination body {})

          boolean (fn [val]
                    (cond
                      (boolean? val) val
                      (string? val) (not (or (= (str/lower-case val) "no")
                                             (= (str/lower-case val) "false")
                                             (str/blank? val)))
                      :else false))

          parse-int-safe (fn [val]
                           (try (some-> val str str/trim Integer/parseInt) (catch Exception _ nil)))

          transformed-data
          {:基本信息 {:门诊号 (:outpatient-number raw-basic-info)
                      :姓名 (:name raw-basic-info)
                      :性别 (:gender raw-basic-info)
                      :年龄 (parse-int-safe (:age raw-basic-info))
                      :院区 (:department raw-basic-info) ; Assuming :department maps to :院区
                      :身份证号 (:id-number raw-basic-info) ; Assuming :id-number maps to :身份证号
                      :手机号 (:phone-number raw-basic-info) ; Assuming :phone-number maps to :手机号
                      :术前诊断 (:diagnosis raw-basic-info)
                      :拟施手术 (:planned_surgery raw-basic-info)
                      :身高cm (parse-int-safe (:height raw-basic-info))
                      :体重kg (parse-int-safe (:weight raw-basic-info))
                      :患者提交时间 current-time
                      :评估更新时间 current-time
                      :评估状态 "待评估"
                      :医生姓名 (:doctor_name raw-basic-info)
                      :医生签名图片 (:doctor_signature_b64 raw-basic-info) ; Added for signature
                      :评估备注 (:assessment_notes raw-basic-info)
                      ;; Fields from general-condition-card that are part of 基本信息 in spec
                      :精神状态 (:mental_state raw-basic-info)
                      :活动能力 (:activity_level raw-basic-info)
                      :血压mmHg (:blood_pressure raw-basic-info) ; Assuming a single field from patient input
                      :脉搏次每分 (parse-int-safe (:heart_rate raw-basic-info))
                      :呼吸次每分 (parse-int-safe (:respiratory_rate raw-basic-info))
                      :体温摄氏度 (try (some-> raw-basic-info :temperature str str/trim Double/parseDouble) (catch Exception _ nil))
                      :SpO2百分比 (parse-int-safe (:spo2 raw-basic-info))}
           :medical_history {:allergy {:has_history (boolean (:allergy-history raw-medical-summary))
                                       :details (:allergen raw-medical-summary)
                                       :last_reaction_date (:allergy-date raw-medical-summary)}
                             :smoking {:has_history (boolean (:smoking-history raw-medical-summary))
                                       :years (parse-int-safe (:smoking-years raw-medical-summary))
                                       :cigarettes_per_day (parse-int-safe (:cigarettes-per-day raw-medical-summary))}
                             :drinking {:has_history (boolean (:drinking-history raw-medical-summary))
                                        :years (parse-int-safe (:drinking-years raw-medical-summary))
                                        :alcohol_per_day (:alcohol-per-day raw-medical-summary)}}
           :physical_examination {:mental_state nil
                                  :activity_level nil
                                  :bp_systolic nil
                                  :bp_diastolic nil
                                  :heart_rate nil
                                  :respiratory_rate nil
                                  :temperature nil
                                  :spo2 nil
                                  :heart {:status (:heart raw-physical-exam)
                                          :notes (:heart-detail raw-physical-exam)}
                                  :lungs {:status (:lungs raw-physical-exam)
                                          :notes (:lungs-detail raw-physical-exam)}
                                  :airway {:status (:airway raw-physical-exam)
                                           :notes (:airway-detail raw-physical-exam)}
                                  :teeth {:status (:teeth raw-physical-exam)
                                          :notes (:teeth-detail raw-physical-exam)}
                                  :spine_limbs {:status (:spine-limbs raw-physical-exam)
                                                :notes (:spine-limbs-detail raw-physical-exam)}
                                  :neuro {:status (:nervous raw-physical-exam)
                                          :notes (:nervous-detail raw-physical-exam)}
                                  :other_findings (:other raw-physical-exam)}
           :comorbidities (let [get-comorb-data (fn [comorb-map key-path] (get-in comorb-map key-path))
                                spec-meds-info (:special-medications raw-comorbidities {})]
                            {:respiratory {:has (boolean (get-comorb-data raw-comorbidities [:respiratory-disease :has]))
                                           :details (get-comorb-data raw-comorbidities [:respiratory-disease :details])}
                             :cardiovascular {:has (boolean (get-comorb-data raw-comorbidities [:cardiovascular-disease :has]))
                                              :details (get-comorb-data raw-comorbidities [:cardiovascular-disease :details])}
                             :endocrine {:has (boolean (get-comorb-data raw-comorbidities [:endocrine-disease :has]))
                                         :details (get-comorb-data raw-comorbidities [:endocrine-disease :details])}
                             :neuro_psychiatric {:has (boolean (get-comorb-data raw-comorbidities [:neuropsychiatric-disease :has]))
                                                 :details (get-comorb-data raw-comorbidities [:neuropsychiatric-disease :details])}
                             :neuromuscular {:has (boolean (get-comorb-data raw-comorbidities [:neuromuscular-disease :has]))
                                             :details (get-comorb-data raw-comorbidities [:neuromuscular-disease :details])}
                             :hepatic {:has (boolean (get-comorb-data raw-comorbidities [:liver-disease :has]))
                                       :details (get-comorb-data raw-comorbidities [:liver-disease :details])}
                             :renal {:has (boolean (get-comorb-data raw-comorbidities [:kidney-disease :has]))
                                     :details (get-comorb-data raw-comorbidities [:kidney-disease :details])}
                             :musculoskeletal {:has (boolean (get-comorb-data raw-comorbidities [:skeletal-system-disease :has]))
                                               :details (get-comorb-data raw-comorbidities [:skeletal-system-disease :details])}
                             :malignant_hyperthermia_fh {:has (boolean (get-comorb-data raw-comorbidities [:family-malignant-hyperthermia :has]))
                                                         :details (get-comorb-data raw-comorbidities [:family-malignant-hyperthermia :details])}
                             :anesthesia_surgery_history {:has (boolean (get-comorb-data raw-comorbidities [:past-anesthesia-surgery :has]))
                                                          :details (get-comorb-data raw-comorbidities [:past-anesthesia-surgery :details])}
                             :special_medications {:has_taken (boolean (:used spec-meds-info))
                                                   :details (:details spec-meds-info)
                                                   :last_dose_time (:last-time spec-meds-info)}})
           :auxiliary_examinations (if (seq raw-aux-exams)
                                     (mapv (fn [[exam-key exam-path]]
                                             {:type (name exam-key)
                                              :filename (some-> exam-path (str/split #"/") last)
                                              :url exam-path
                                              :uploaded_by "patient"
                                              :uploaded_at current-time})
                                           raw-aux-exams)
                                     [])
           :auxiliary_examinations_notes nil
           :anesthesia_plan {:asa_rating nil, :anesthesia_type nil, :preoperative_instructions nil}} ; This is old anesthesia_plan, should be :麻醉评估与医嘱 eventually

          patient-id (get-in transformed-data [:基本信息 :门诊号]) ; Updated path
          patient-name (get-in transformed-data [:基本信息 :姓名] "") ; Updated path
          doctor-signature (get-in transformed-data [:基本信息 :医生签名图片]) ; Extract signature
          {:keys [pinyin initial]} (get-pinyin-parts patient-name) ; Assuming get-pinyin-parts is available
          assessment-data-json (cheshire/generate-string (update transformed-data :基本信息 dissoc  :医生签名图片))] ; Remove signature from JSON
      (if (str/blank? (str patient-id)) ; Ensure patient-id is treated as string for blank? check
        (do
          (log/error "患者ID (outpatient_number) 未在 basic_info 中提供。")
          (http-response/bad-request {:message "提交失败，患者ID不能为空。"}))
        (let [existing-assessment (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
          (if (seq existing-assessment)
            (do
              (log/info "更新患者评估数据, 患者ID:" patient-id ", 拼音:" pinyin ", 首字母:" initial)
              (log/info "医生签名图片长度:" (if doctor-signature (count doctor-signature) 0))
              (query-fn :update-patient-assessment!
                        {:patient_id patient-id
                         :assessment_data assessment-data-json
                         :patient_name_pinyin pinyin
                         :patient_name_initial initial
                         :doctor_signature_b64 doctor-signature}) ; Pass signature to DB
              (http-response/ok {:message "评估更新成功！"}))
            (do
              (log/info "插入新的患者评估数据, 患者ID:" patient-id ", 拼音:" pinyin ", 首字母:" initial)
              (log/info "医生签名图片长度:" (if doctor-signature (count doctor-signature) 0))
              (query-fn :insert-patient-assessment!
                        {:patient_id patient-id
                         :assessment_data assessment-data-json
                         :patient_name_pinyin pinyin
                         :patient_name_initial initial
                         :doctor_signature_b64 doctor-signature}) ; Pass signature to DB
              (http-response/ok {:message "评估提交成功！"}))))))
    (catch Exception e
      (log/error e "提交/更新评估时出错" (ex-message e) (ex-data e))
      (http-response/internal-server-error {:message (str "提交/更新评估时出错: " (ex-message e))}))))



(defn get-assessment-by-patient-id [{{{:keys [patient-id]} :path} :parameters :keys [query-fn] :as _request}]
  (log/info "查询患者评估数据，患者ID:" patient-id)
  (try
    (let [db-result (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
      (if (seq db-result)
        (let [assessment-data (cheshire/decode (:assessment_data db-result) keyword)
              ; 将医生签名添加到 assessment_data 的 :基本信息 下
              updated-assessment-data (assoc-in assessment-data [:基本信息 :医生签名图片] (:doctor_signature_b64 db-result))
              final-result (dissoc db-result :doctor_signature_b64 :assessment_data)]  ; 从顶层移除原始签名和JSON字符串
          (http-response/ok (assoc final-result :assessment_data updated-assessment-data))) ; 返回带有嵌套签名的完整评估
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
          parsed-assessments (mapv (fn [assessment-row]
                                     (let [parsed-assessment-data (cheshire/decode (:assessment_data assessment-row) keyword)
                                           ; 将医生签名添加到 parsed-assessment-data 的 :基本信息 下
                                           updated-assessment-data (assoc-in parsed-assessment-data [:基本信息 :医生签名图片] (:doctor_signature_b64 assessment-row))
                                           final-row (-> assessment-row
                                                           (assoc :assessment_data updated-assessment-data)
                                                           (dissoc :doctor_signature_b64))]
                                       ;; Ensure pinyin and initial are strings for final row
                                       (cond-> final-row
                                         (:patient_name_pinyin final-row) (update :patient_name_pinyin #(if (keyword? %) (name %) %))
                                         (:patient_name_initial final-row) (update :patient_name_initial #(if (keyword? %) (name %) %)))))
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
        (let [;; It's important to decide where doctor_signature_b64 comes from in the `body`
              ;; Assuming it's at the top level of `body` as :doctor_signature_b64 for this example
              ;; Or, if it's nested, e.g., (get-in body [:基本信息 :医生签名图片])
              doctor-signature (get body :doctor_signature_b64) ; Placeholder: adjust key as per actual client data
              _ (log/info "医生签名图片长度 (update):" (if doctor-signature (count doctor-signature) 0))

              ;; Remove signature from body before generating JSON to avoid storing it inside assessment_data JSON
              body-without-signature (dissoc body :doctor_signature_b64)
              ;; also if it was nested under :基本信息 :医生签名图片
              body-without-signature (update body-without-signature [:基本信息] dissoc :医生签名图片)

              updated-body (assoc-in body-without-signature [:基本信息 :评估更新时间] (str (Instant/now)))
              patient-name (get-in updated-body [:基本信息 :姓名] "")
              {:keys [pinyin initial]} (get-pinyin-parts patient-name)
              assessment-data-json (cheshire/generate-string updated-body)]
          (query-fn :update-patient-assessment!
                    {:patient_id patient-id
                     :assessment_data assessment-data-json
                     :patient_name_pinyin pinyin
                     :patient_name_initial initial
                     :doctor_signature_b64 doctor-signature}) ; Pass signature to DB
          (http-response/ok {:message "评估更新成功！"}))
        (http-response/not-found {:message "未找到该患者的评估数据，无法更新。"})))
    (catch Exception e
      (log/error e "更新评估时出错" {:patient-id patient-id :error (ex-message e) :data (ex-data e)})
      (http-response/internal-server-error {:message (str "更新评估时出错: " (ex-message e))}))))

(defn find-patient-by-id-handler
  "根据患者ID输入（REGISTER_NO, PATIENT_ID, 或 INP_NO）查找患者信息，
  如果找到，则在本地数据库创建或更新患者记录，并返回患者信息。
  此函数是 /api/patient/find-by-id/:patientIdInput 端点的处理器。"
  [{{{:keys [patientIdInput]} :path} :parameters
    :keys [query-fn oracle-query-fn] :as _request}] ; query-fn 用于本地数据库, oracle-query-fn 用于HIS
  (log/info "接收到查询患者请求，输入ID:" patientIdInput)
  (if-not oracle-query-fn
    (do
      (log/error "Oracle query function (oracle-query-fn) 未提供给 find-patient-by-id-handler")
      (http-response/internal-server-error {:message "系统错误：HIS数据库查询功能未配置。"}));; 优先检查 oracle-query-fn 是否存在
    (try
      (let [patient-info-from-his (his-queries/find-patient-in-his oracle-query-fn patientIdInput)]
        (if patient-info-from-his
          (do
            (log/info "HIS中找到患者信息:" patient-info-from-his)
            (let [his-name (get patient-info-from-his :name) ; HIS视图字段通常大写
                  his-sex (get patient-info-from-his :sex)
                  his-dob (get patient-info-from-his :date_of_birth) ; 可能需要日期格式转换
                  current-time-str (str (Instant/now))

                  ;; 构建用于本地存储的 assessment_data
                  basic-info {:姓名 his-name
                              :性别 his-sex
                              :出生日期 (if his-dob (str his-dob) nil) ; 确保为字符串或nil
                              :门诊号 patientIdInput ; 使用查询ID作为本地的门诊号/患者ID
                              :患者来源 "HIS扫码"
                              :评估状态 "待评估" ; 初始状态
                              :患者提交时间 current-time-str
                              :评估更新时间 current-time-str}
                  assessment-map {:基本信息 basic-info
                                  :medical_history {}
                                  :physical_examination {}
                                  :comorbidities {}
                                  :auxiliary_examinations []
                                  :anesthesia_plan {}}
                  assessment-data-json (cheshire/generate-string assessment-map)

                  ;; 检查本地数据库是否已存在该患者记录
                  existing-local-assessment (query-fn :get-patient-assessment-by-id {:patient_id patientIdInput})
                  was-inserted? (not (seq existing-local-assessment))]

              (if was-inserted?
                (do
                  (log/info "本地不存在患者评估，将插入新记录，患者ID:" patientIdInput)
                  (query-fn :insert-patient-assessment!
                            {:patient_id patientIdInput
                             :assessment_data assessment-data-json
                             :patient_name_pinyin nil
                             :patient_name_initial nil
                             :doctor_signature_b64 nil}))
                (log/info "本地已存在患者评估，患者ID:" patientIdInput))

              ;; 无论插入还是已存在，都重新获取并返回完整的本地记录
              (let [final-local-assessment (query-fn :get-patient-assessment-by-id {:patient_id patientIdInput})
                    parsed-assessment-data (when (:assessment_data final-local-assessment)
                                             (cheshire/parse-string (:assessment_data final-local-assessment) true))]
                (if final-local-assessment
                  (http-response/ok {:message (if was-inserted?
                                                (str "成功从HIS获取并创建新患者记录: " patientIdInput)
                                                (str "成功从HIS获取患者信息，本地记录已存在: " patientIdInput))
                                     :patientIdInput patientIdInput
                                     ;; 返回从HIS直接获取的基础信息，以及完整的本地评估记录（包含了解析后的assessment_data）
                                     :his_info patient-info-from-his ; 保留原始HIS信息以供参考
                                     :assessment (assoc final-local-assessment :assessment_data parsed-assessment-data)
                                     :data_source "HIS_AND_LOCAL_DB"})
                  (do
                    (log/error "在插入/确认后，无法在本地数据库中检索到患者评估，患者ID：" patientIdInput)
                    (http-response/internal-server-error
                     {:message (str "处理患者信息后无法在本地检索: " patientIdInput)}))))))))
      (catch Exception e
        (log/error e (str "查询HIS或处理本地患者记录时出错，输入ID: " patientIdInput) (ex-message e))
        (http-response/internal-server-error {:message "处理患者信息时发生内部错误"})))))
