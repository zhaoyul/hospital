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
    (letfn [(format-gender [g] (case g "male" "男" "female" "女" "未知"))
            (format-date-str [d] (when d (utils/format-date d "YYYY-MM-DD")))
            (patient-from-api-assessment [assessment]
              ;; Accessing canonical structure directly from :assessment_data
              (let [basic-info (get-in assessment [:assessment_data :basic_info] {})
                    anesthesia-plan (get-in assessment [:assessment_data :anesthesia_plan] {})]
                {:key (:patient_id assessment)
                 :name (or (:name basic-info) "未知姓名")
                 :patient-id-display (or (:outpatient_number basic-info) (:patient_id assessment))
                 :gender (format-gender (:gender basic-info))
                 :age (str (or (:age basic-info) "未知") "岁")
                 :anesthesia-type (or (:anesthesia_type anesthesia-plan) "未知麻醉方式")
                 ;; Timestamps are now directly in basic_info according to canonical server structure
                 :date (format-date-str (:assessment_updated_at basic-info)) ; Use assessment_updated_at
                 :status (or (:assessment_status basic-info) "待评估")}))] ; Use assessment_status

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
    (get-in db [:anesthesia :current-assessment-canonical])))

;; Basic Info
(rf/reg-sub ::canonical-basic-info
  :<- [::current-canonical-assessment]
  (fn [assessment _] (:basic_info assessment)))

(rf/reg-sub ::canonical-patient-name
  :<- [::canonical-basic-info]
  (fn [basic-info _] (:name basic-info)))

(rf/reg-sub ::canonical-patient-outpatient-number
  :<- [::canonical-basic-info]
  (fn [basic-info _] (:outpatient_number basic-info)))

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
  (fn [assessment _] (:auxiliary_examinations assessment)))

(rf/reg-sub ::canonical-auxiliary-examinations-notes
  :<- [::current-canonical-assessment]
  (fn [assessment _] (:auxiliary_examinations_notes assessment)))

;; Anesthesia Plan
(rf/reg-sub ::canonical-anesthesia-plan
  :<- [::current-canonical-assessment]
  (fn [assessment _] (:anesthesia_plan assessment)))

;; Cardiovascular System - New
(rf/reg-sub ::circulatory-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:circulatory_system assessment) {}))) ; Return empty map if nil

;; Respiratory System - New
(rf/reg-sub ::respiratory-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:respiratory_system assessment) {}))) ; Return empty map if nil

;; Mental & Neuromuscular System - New
(rf/reg-sub ::mental-neuromuscular-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:mental_neuromuscular_system assessment) {}))) ; Return empty map if nil

;; Endocrine System - New
(rf/reg-sub ::endocrine-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:endocrine_system assessment) {}))) ; Return empty map if nil

;; Liver & Kidney System - New
(rf/reg-sub ::liver-kidney-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:liver_kidney_system assessment) {}))) ; Return empty map if nil

;; Digestive System - New
(rf/reg-sub ::digestive-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:digestive_system assessment) {}))) ; Return empty map if nil

;; Hematologic System - New
(rf/reg-sub ::hematologic-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:hematologic_system assessment) {}))) ; Return empty map if nil

;; Immune System - New
(rf/reg-sub ::immune-system-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:immune_system assessment) {}))) ; Return empty map if nil

;; Special Medication History - New
(rf/reg-sub ::special-medication-history-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:special_medication_history assessment) {}))) ; Return empty map if nil

;; Special Disease History - New
(rf/reg-sub ::special-disease-history-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:special_disease_history assessment) {}))) ; Return empty map if nil

;; Nutritional Assessment - New
(rf/reg-sub ::nutritional-assessment-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:nutritional_assessment assessment) {}))) ; Return empty map if nil

(rf/reg-sub ::pregnancy-assessment-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:pregnancy_assessment_data assessment) {})))

;; Surgical Anesthesia History - New
(rf/reg-sub ::surgical-anesthesia-history-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:surgical_anesthesia_history assessment) {}))) ; Return empty map if nil

;; Airway Assessment - New
(rf/reg-sub ::airway-assessment-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:airway_assessment assessment) {}))) ; Return empty map if nil

;; Spinal Anesthesia Assessment - New
(rf/reg-sub ::spinal-anesthesia-assessment-data
  :<- [::current-canonical-assessment]
  (fn [assessment _]
    (or (:spinal_anesthesia_assessment assessment) {})))

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
             (= patient-key (get-in current-canonical-data [:basic_info :outpatient_number])))
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
