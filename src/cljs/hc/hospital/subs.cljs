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
  :<- [::anesthesia-example-patients]
  (fn [[api-assessments search-term date-range-moments status-filter example-patients] _]
    (letfn [(format-gender [g] (case g "male" "男" "female" "女" "未知"))
            (format-date-str [d] (when d (utils/format-date d "YYYY-MM-DD")))
            (patient-from-api-assessment [assessment]
              (let [patient-info (get-in assessment [:assessment_data :basic-info] {})
                    anesthesia-plan (get-in assessment [:assessment_data :anesthesia_plan] {})]
                {:key (:patient_id assessment) ; Assuming patient_id is at the root of the assessment object
                 :name (or (:name patient-info) "未知姓名")
                 :patient-id-display (or (:outpatient_number patient-info) (:patient_id assessment))
                 :sex (format-gender (:gender patient-info))
                 :age (str (or (:age patient-info) "未知") "岁")
                 :anesthesia-type (or (:anesthesia_type anesthesia-plan) "未知麻醉方式")
                 :date (format-date-str (:updated_at assessment)) ; Or :created_at
                 :status (or (:doctor_status assessment) "待评估")}))
            (patient-from-example [ex-patient]
              (merge {:sex (format-gender (:gender ex-patient))
                      :age (str (:age ex-patient) "岁")}
                     ex-patient))]

      (let [patients-from-api (if (seq api-assessments)
                                (mapv patient-from-api-assessment api-assessments)
                                [])
            display-patients (if (empty? patients-from-api)
                               (mapv patient-from-example example-patients)
                               patients-from-api)

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


;; ---- 评估表单相关订阅 ----
(rf/reg-sub ::selected-patient-assessment-forms-data
  (fn [db _]
    (get-in db [:anesthesia :assessment])))

(rf/reg-sub ::medical-summary-data ;; This now points to the new :form-data structure
  (fn [db _]
    (get-in db [:anesthesia :assessment :form-data])))

(rf/reg-sub ::selected-patient-raw-details
  :<- [::current-patient-id]
  :<- [::all-patient-assessments]
  :<- [::anesthesia-example-patients] ; Fallback for example data
  (fn [[patient-key api-assessments example-patients] _]
    (let [find-in-api (when patient-key (first (filter #(= (:patient_id %) patient-key) api-assessments)))
          find-in-examples (when (and patient-key (not find-in-api))
                             (first (filter #(= (:key %) patient-key) example-patients)))]
      (cond
        find-in-api find-in-api ; Return the full assessment object from API
        find-in-examples find-in-examples ; Return the example patient object
        :else nil))))

(rf/reg-sub ::anesthesia-plan-details ;; This remains as it targets a separate part of the assessment
  (fn [db _]
    (get-in db [:anesthesia :assessment :anesthesia-plan])))

(rf/reg-sub ::assessment-notes
  :<- [::anesthesia-plan-details]
  (fn [anesthesia-plan _]
    (when anesthesia-plan
      (:notes anesthesia-plan))))

(rf/reg-sub ::doctors
  (fn [db _]
    (get db :doctors []))) ; Default to empty vector if not present

(rf/reg-sub ::doctor-modal-visible?
  (fn [db _]
    (get db :doctor-modal-visible? false))) ; Default to false

(rf/reg-sub ::editing-doctor
  (fn [db _]
    (get db :editing-doctor {}))) ; Default to empty map
