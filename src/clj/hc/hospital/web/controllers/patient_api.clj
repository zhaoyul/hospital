(ns hc.hospital.web.controllers.patient-api
  (:require
   [cheshire.core :as cheshire]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [clojure.tools.logging :as ctl]
   [ring.util.http-response :as http-response])
  (:import
   [com.hankcs.hanlp HanLP] ; Import HanLP
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

          to-boolean (fn [val]
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
                        :评估备注 (:assessment_notes raw-basic-info)
                        ;; Fields from general-condition-card that are part of 基本信息 in spec
                        :精神状态 (:mental_state raw-basic-info)
                        :活动能力 (:activity_level raw-basic-info)
                        :血压mmHg (:blood_pressure raw-basic-info) ; Assuming a single field from patient input
                        :脉搏次每分 (parse-int-safe (:heart_rate raw-basic-info))
                        :呼吸次每分 (parse-int-safe (:respiratory_rate raw-basic-info))
                        :体温摄氏度 (try (some-> raw-basic-info :temperature str str/trim Double/parseDouble) (catch Exception _ nil))
                        :SpO2百分比 (parse-int-safe (:spo2 raw-basic-info))}
           :medical_history (ctl/spy :info {:allergy {:has_history (to-boolean (:allergy-history raw-medical-summary))
                                                      :details (:allergen raw-medical-summary)
                                                      :last_reaction_date (:allergy-date raw-medical-summary)}
                                            :smoking {:has_history (to-boolean (:smoking-history raw-medical-summary))
                                                      :years (parse-int-safe (:smoking-years raw-medical-summary))
                                                      :cigarettes_per_day (parse-int-safe (:cigarettes-per-day raw-medical-summary))}
                                            :drinking {:has_history (to-boolean (:drinking-history raw-medical-summary))
                                                       :years (parse-int-safe (:drinking-years raw-medical-summary))
                                                       :alcohol_per_day (:alcohol-per-day raw-medical-summary)}})
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
                            {:respiratory {:has (to-boolean (get-comorb-data raw-comorbidities [:respiratory-disease :has]))
                                           :details (get-comorb-data raw-comorbidities [:respiratory-disease :details])}
                             :cardiovascular {:has (to-boolean (get-comorb-data raw-comorbidities [:cardiovascular-disease :has]))
                                              :details (get-comorb-data raw-comorbidities [:cardiovascular-disease :details])}
                             :endocrine {:has (to-boolean (get-comorb-data raw-comorbidities [:endocrine-disease :has]))
                                         :details (get-comorb-data raw-comorbidities [:endocrine-disease :details])}
                             :neuro_psychiatric {:has (to-boolean (get-comorb-data raw-comorbidities [:neuropsychiatric-disease :has]))
                                                 :details (get-comorb-data raw-comorbidities [:neuropsychiatric-disease :details])}
                             :neuromuscular {:has (to-boolean (get-comorb-data raw-comorbidities [:neuromuscular-disease :has]))
                                             :details (get-comorb-data raw-comorbidities [:neuromuscular-disease :details])}
                             :hepatic {:has (to-boolean (get-comorb-data raw-comorbidities [:liver-disease :has]))
                                       :details (get-comorb-data raw-comorbidities [:liver-disease :details])}
                             :renal {:has (to-boolean (get-comorb-data raw-comorbidities [:kidney-disease :has]))
                                     :details (get-comorb-data raw-comorbidities [:kidney-disease :details])}
                             :musculoskeletal {:has (to-boolean (get-comorb-data raw-comorbidities [:skeletal-system-disease :has]))
                                               :details (get-comorb-data raw-comorbidities [:skeletal-system-disease :details])}
                             :malignant_hyperthermia_fh {:has (to-boolean (get-comorb-data raw-comorbidities [:family-malignant-hyperthermia :has]))
                                                         :details (get-comorb-data raw-comorbidities [:family-malignant-hyperthermia :details])}
                             :anesthesia_surgery_history {:has (to-boolean (get-comorb-data raw-comorbidities [:past-anesthesia-surgery :has]))
                                                          :details (get-comorb-data raw-comorbidities [:past-anesthesia-surgery :details])}
                             :special_medications {:has_taken (to-boolean (:used spec-meds-info))
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
          {:keys [pinyin initial]} (get-pinyin-parts patient-name) ; Assuming get-pinyin-parts is available
          ;; 中文注释：提取医生签名图片 (如果存在)，用于插入操作
          doctor-signature (get-in transformed-data [:基本信息 :医生签名图片])
          assessment-data-json (cheshire/generate-string transformed-data)]
      (if (str/blank? (str patient-id)) ; Ensure patient-id is treated as string for blank? check
        (do
          (log/error "患者ID (outpatient_number) 未在 basic_info 中提供。")
          (http-response/bad-request {:message "提交失败，患者ID不能为空。"}))
        (let [existing-assessment (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
          (if (seq existing-assessment)
            (do
              (log/info "更新患者评估数据, 患者ID:" patient-id ", 拼音:" pinyin ", 首字母:" initial)
              (query-fn :update-patient-assessment!
                        {:patient_id patient-id
                         :assessment_data assessment-data-json
                         :patient_name_pinyin pinyin
                         :patient_name_initial initial
                         :doctor_signature_b64 doctor-signature}) ;; 中文注释：传递签名数据给更新查询 (如果存在)
              (http-response/ok {:message "评估更新成功！"}))
            (do
              (log/info "插入新的患者评估数据, 患者ID:" patient-id ", 拼音:" pinyin ", 首字母:" initial)
              (query-fn :insert-patient-assessment!
                        {:patient_id patient-id
                         :assessment_data assessment-data-json
                         :patient_name_pinyin pinyin
                         :patient_name_initial initial
                         :doctor_signature_b64 doctor-signature}) ;; 中文注释：传递签名数据给插入查询 (如果存在)
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
              ;; 中文注释：将数据库中的 doctor_signature_b64 映射回 :医生签名图片
              full-assessment (assoc-in assessment-data [:基本信息 :医生签名图片] (:doctor_signature_b64 db-result))]
          (http-response/ok full-assessment))
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
                                           ;; 中文注释：将数据库中的 doctor_signature_b64 整合到返回的 assessment_data 中
                                           full-assessment-data (assoc-in parsed-assessment-data [:基本信息 :医生签名图片] (:doctor_signature_b64 assessment-row))]
                                       (-> assessment-row
                                           (assoc :assessment_data full-assessment-data)
                                           ;; Ensure pinyin and initial are strings
                                           (update :patient_name_pinyin #(if (keyword? %) (name %) %))
                                           (update :patient_name_initial #(if (keyword? %) (name %) %))
                                           ;; 中文注释：从结果中移除单独的 doctor_signature_b64 键，因为它已合并到 assessment_data 中
                                           (dissoc :doctor_signature_b64))))
                                   all-assessments-raw)]
      (http-response/ok parsed-assessments))
    (catch Exception e
      (log/error e "查询所有评估数据时出错")
      (http-response/internal-server-error {:message "查询所有评估数据时出错"}))))

(defn update-assessment-by-patient-id! [{{{:keys [patient-id]} :path :keys [body]} :parameters :keys [query-fn] :as _request}]
  (log/info "请求更新患者评估数据，患者ID:" patient-id ", 完整body:" body)
  (try
    (let [existing-assessment (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
      (if (seq existing-assessment)
        (let [;; 中文注释：确保评估更新时间被设置
              body-with-timestamp (assoc-in body [:基本信息 :评估更新时间] (str (Instant/now)))
              ;; 中文注释：从请求体中提取医生签名图片
              doctor-signature (get-in body-with-timestamp [:基本信息 :医生签名图片])
              patient-name (get-in body-with-timestamp [:基本信息 :姓名] "")
              {:keys [pinyin initial]} (get-pinyin-parts patient-name)
              ;; 中文注释：可以选择是否从 assessment_data JSON 中移除签名图片，如果它已作为独立列存储。
              ;; 为了简单起见，并保持其他地方的兼容性，暂时将其保留在JSON中。
              ;; 若要移除: (assoc-in body-with-timestamp [:基本信息] (dissoc (get-in body-with-timestamp [:基本信息]) :医生签名图片))
              assessment-data-json (cheshire/generate-string body-with-timestamp)]
          (log/info "准备更新数据库，患者ID:" patient-id ", 签名数据长度:" (count doctor-signature))
          (query-fn :update-patient-assessment!
                    {:patient_id patient-id
                     :assessment_data assessment-data-json
                     :patient_name_pinyin pinyin
                     :patient_name_initial initial
                     :doctor_signature_b64 doctor-signature}) ;; 中文注释：传递签名数据给数据库
          (http-response/ok {:message "评估更新成功！"}))
        (http-response/not-found {:message "未找到该患者的评估数据，无法更新。"})))
    (catch Exception e
      (log/error e "更新评估时出错" {:patient-id patient-id :error (ex-message e) :data (ex-data e)})
      (http-response/internal-server-error {:message (str "更新评估时出错: " (ex-message e))}))))
