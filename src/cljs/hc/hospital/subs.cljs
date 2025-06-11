(ns hc.hospital.subs
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [hc.hospital.utils :as utils]
            [taoensso.timbre :as timbre :refer [spy]]
            [dayjs :as dayjs])) ; 确保引入 dayjs

(rf/reg-sub ::all-patient-assessments
  (fn [db _]
    (get-in db [:anesthesia :all-patient-assessments])))

(rf/reg-sub ::current-patient-id
  (fn [db _]
    (get-in db [:anesthesia :current-patient-id]))) ; 从 :anesthesia 模块获取

(rf/reg-sub ::active-tab
  (fn [db _]
    (get-in db [:anesthesia :active-tab] "patients")))

(rf/reg-sub ::search-term
  (fn [db _]
    (get-in db [:anesthesia :search-term])))

(rf/reg-sub ::date-range
  (fn [db _]
    (get-in db [:anesthesia :date-range])))

(rf/reg-sub ::assessment-status-filter
  (fn [db _]
    (get-in db [:anesthesia :assessment-status-filter] "all")))

(rf/reg-sub ::anesthesia-example-patients
  (fn [db _]
    (get-in db [:anesthesia :patients])))

(rf/reg-sub ::filtered-patients
  :<- [::all-patient-assessments]
  :<- [::search-term]
  :<- [::date-range]
  :<- [::assessment-status-filter]
  ;; Removed :<- [::anesthesia-example-patients]
  (fn [[api-assessments search-term date-range-moments status-filter] _] ;; Removed example-patients
    (letfn [(format-gender [g] (case g "男" "男" "女" "女" "其他" "其他" "未知")) ; Updated to match spec if different
            (format-date-str [d] (when d (utils/format-date d "YYYY-MM-DD")))
            (patient-from-api-assessment [assessment]
              ;; Accessing canonical structure directly from :assessment_data, now using Chinese keys
              (let [basic-info (get-in assessment [:assessment_data :基本信息] {})
                    anesthesia-plan (get-in assessment [:assessment_data :麻醉评估与医嘱] {})]
                {:key (:patient_id assessment) ; patient_id is from the wrapper, not assessment_data
                 :name (or (:姓名 basic-info) "未知姓名")
                 :patient-id-display (or (:门诊号 basic-info) (:patient_id assessment))
                 :gender (format-gender (:性别 basic-info))
                 :age (str (or (:年龄 basic-info) "未知") "岁")
                 :anesthesia-type (or (:拟行麻醉方式 anesthesia-plan) "未知麻醉方式")
                 ;; Timestamps are now directly in basic_info according to canonical server structure
                 :date (format-date-str (:评估更新时间 basic-info)) ; Use 评估更新时间
                 :status (or (:评估状态 basic-info) "待评估")}))] ; Use 评估状态

      (let [patients-from-api (if (seq api-assessments)
                                (mapv patient-from-api-assessment api-assessments)
                                [])
            ;; Removed example patients logic as primary display
            display-patients patients-from-api

            [start-moment end-moment] date-range-moments
            start-date-str (when start-moment (.format start-moment "YYYY-MM-DD"))
            end-date-str (when end-moment (.format end-moment "YYYY-MM-DD"))]

        (cond->> display-patients
          (and search-term (not (str/blank? search-term)))
          (filterv (fn [p]
                     (let [search-lower (str/lower-case search-term)]
                       (or (str/includes? (str/lower-case (str (:name p))) search-lower)
                           (str/includes? (str/lower-case (str (:patient-id-display p))) search-lower)))))

          (and start-date-str end-date-str)
          (filterv (fn [p] (when-let [p-date (:date p)]
                             (and (>= p-date start-date-str)
                                  (<= p-date end-date-str)))))

          (and status-filter (not= status-filter "all"))
          (filterv (fn [p] (= (:status p) status-filter)))

          true identity)))))

(rf/reg-sub ::doctor-form-physical-examination
  (fn [db _]
    (get-in db [:anesthesia :assessment :form-data])))


;; ---- Canonical Assessment Subscriptions ----
(rf/reg-sub ::current-canonical-assessment
  (fn [db _]
    (let [assessment (get-in db [:anesthesia :current-assessment-canonical])]
      (timbre/info "::current-canonical-assessment: Subscription triggered. Returning assessment:" (clj->js assessment))
      assessment)))

;; Basic Info
(rf/reg-sub ::canonical-basic-info
  :<- [::current-canonical-assessment]
  (fn [assessment k] (:基本信息 assessment )))

(rf/reg-sub ::canonical-patient-name
  :<- [::canonical-basic-info]
  (fn [basic-info _] (:姓名 basic-info)))

(rf/reg-sub ::canonical-patient-outpatient-number
  :<- [::canonical-basic-info]
  (fn [basic-info _] (:门诊号 basic-info)))

;; New subscription for doctor's signature image
(rf/reg-sub ::doctor-signature-image
  :<- [::canonical-basic-info]
  (fn [basic-info _]
    (get basic-info :医生签名图片)))

;; Medical History
(rf/reg-sub ::canonical-medical-history
  :<- [::current-canonical-assessment]
  (fn [assessment _] (:medical_history assessment)))

;; Physical Examination
(rf/reg-sub ::canonical-physical-examination
  :<- [::current-canonical-assessment]
  (fn [assessment _] (:physical_examination assessment)))

;; Comorbidities
(rf/reg-sub ::canonical-comorbidities
  :<- [::current-canonical-assessment]
  (fn [assessment _] (:comorbidities assessment)))

;; Auxiliary Examinations
(rf/reg-sub ::canonical-auxiliary-examinations
  :<- [::current-canonical-assessment]
  (fn [assessment _] (:辅助检查集 assessment))) ;; Updated to :辅助检查集

(rf/reg-sub ::canonical-auxiliary-examinations-notes
  :<- [::current-canonical-assessment]
  (fn [assessment _] (:辅助检查备注 assessment))) ;; Updated to :辅助检查备注

;; Anesthesia Plan
(rf/reg-sub ::canonical-anesthesia-plan
  :<- [::current-canonical-assessment]
  (fn [assessment _] (:麻醉评估与医嘱 assessment))) ;; Updated to :麻醉评估与医嘱

;; Cardiovascular System - New
(rf/reg-sub ::circulatory-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:循环系统 assessment) {})))

;; Respiratory System - New
(rf/reg-sub ::respiratory-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:呼吸系统 assessment) {}))) ; Return empty map if nil

;; Mental & Neuromuscular System - New
(rf/reg-sub ::mental-neuromuscular-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _] ; assessment here is the result from ::current-canonical-assessment
    (timbre/info "::mental-neuromuscular-system-data: Subscription triggered. Input assessment (from ::current-canonical-assessment):" (clj->js assessment))
    (let [mn-data (or (:精神神经肌肉系统 assessment) {})]
      (timbre/info "::mental-neuromuscular-system-data: Returning mn-data:" (clj->js mn-data))
      mn-data)))

;; Endocrine System - New
(rf/reg-sub ::endocrine-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:内分泌系统 assessment) {}))) ; Return empty map if nil

;; Liver & Kidney System - New
(rf/reg-sub ::liver-kidney-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:肝肾系统 assessment) {}))) ; Return empty map if nil

;; Digestive System - New
(rf/reg-sub ::digestive-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:消化系统 assessment) {}))) ; Return empty map if nil

;; Hematologic System - New
(rf/reg-sub ::hematologic-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:血液系统 assessment) {}))) ; Return empty map if nil

;; Immune System - New
(rf/reg-sub ::immune-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:免疫系统 assessment) {}))) ; Return empty map if nil

;; Special Medication History - New
(rf/reg-sub ::special-medication-history-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:特殊用药史 assessment) {}))) ; Return empty map if nil

;; Special Disease History - New
(rf/reg-sub ::special-disease-history-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:特殊病史 assessment) {}))) ; Return empty map if nil

;; Nutritional Assessment - New
(rf/reg-sub ::nutritional-assessment-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:营养评估 assessment) {}))) ; Return empty map if nil

(rf/reg-sub ::pregnancy-assessment-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:妊娠 assessment) {}))) ;; Updated to :妊娠

;; Surgical Anesthesia History - New
(rf/reg-sub ::surgical-anesthesia-history-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:手术麻醉史 assessment) {}))) ; Return empty map if nil

;; Airway Assessment - New
(rf/reg-sub ::airway-assessment-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:气道评估 assessment) {}))) ; Return empty map if nil

;; Spinal Anesthesia Assessment - New
(rf/reg-sub ::spinal-anesthesia-assessment-data
  :<- [::current-canonical-assessment]
  (fn [assessment _] ; assessment here is the result from ::current-canonical-assessment
    (timbre/info "::spinal-anesthesia-assessment-data: Subscription triggered. Input assessment (from ::current-canonical-assessment):" (clj->js assessment))
    (let [saa-data (or (:椎管内麻醉评估 assessment) {})]
      (timbre/info "::spinal-anesthesia-assessment-data: Returning saa-data:" (clj->js saa-data))
      saa-data)))

;; ---- Existing subscriptions - Review/Refactor as needed ----
;; DEPRECATED by ::canonical-basic-info, ::canonical-medical-history etc.
;; (rf/reg-sub ::selected-patient-assessment-forms-data
;;   (fn [db _]
;;     (get-in db [:anesthesia :assessment])))

;; DEPRECATED by ::canonical-medical-history etc.
;; (rf/reg-sub ::medical-summary-data
;;   (fn [db _]
;;     (get-in db [:anesthesia :assessment :form-data])))

;; DEPRECATED by ::canonical-physical-examination etc.
;; (rf/reg-sub ::doctor-form-physical-examination
;;   (fn [db _]
;;     (get-in db [:anesthesia :assessment :form-data])))


(rf/reg-sub ::selected-patient-raw-details ;; Provides the full canonical data for the selected patient
  :<- [::current-patient-id]
  :<- [::current-canonical-assessment]
  :<- [::all-patient-assessments]
  (fn [[patient-key current-canonical-data all-assessments] _]
    (if (and patient-key current-canonical-data
             (= patient-key (get-in current-canonical-data [:基本信息 :门诊号]))) ;; Updated path
      current-canonical-data ;; If current canonical matches selected ID, use it directly
      (when patient-key ;; Otherwise, find in the main list and get its assessment_data
        (some-> (filter #(= (:patient_id %) patient-key) all-assessments)
                first
                :assessment_data)))))

;; DEPRECATED by ::canonical-anesthesia-plan
;; (rf/reg-sub ::anesthesia-plan-details
;;   (fn [db _]
;;     (get-in db [:anesthesia :assessment :anesthesia-plan])))

;; DEPRECATED by ::canonical-anesthesia-plan or ::canonical-auxiliary-examinations-notes etc.
;; (rf/reg-sub ::assessment-notes
;;   :<- [::anesthesia-plan-details]
;;   (fn [anesthesia-plan _]
;;     (when anesthesia-plan
;;       (:notes anesthesia-plan))))

(rf/reg-sub ::doctors
  (fn [db _]
    (get db :doctors []))) ; Default to empty vector if not present

(rf/reg-sub ::doctor-modal-visible?
  (fn [db _]
    (get db :doctor-modal-visible? false))) ; Default to false

(rf/reg-sub ::editing-doctor
  (fn [db _]
    (get db :editing-doctor {}))) ; Default to empty map

(rf/reg-sub ::current-doctor
  (fn [db _]
    (get db :current-doctor)))

(rf/reg-sub ::is-logged-in
  (fn [db _]
    (get db :is-logged-in false)))

(rf/reg-sub ::session-check-pending?
  (fn [db _]
    (get db :session-check-pending? true))) ; Default to true if not found, matches db.cljs

;; --- 二维码扫描模态框订阅 ---
;; 订阅二维码扫描模态框的可见状态
(rf/reg-sub ::qr-scan-modal-visible?
  (fn [db _]
    (get db :qr-scan-modal-visible? false))) ; 默认值为 false

;; 订阅二维码扫描模态框输入框的值
(rf/reg-sub ::qr-scan-input-value
  (fn [db _]
    (get db :qr-scan-input-value ""))) ; 默认值为空字符串
