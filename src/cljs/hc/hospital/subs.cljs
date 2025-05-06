;; Assuming hc.hospital.subs is the correct namespace
(ns hc.hospital.subs
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [hc.hospital.utils :as utils])) ; For date formatting

(rf/reg-sub
  ::all-patient-assessments
  (fn [db _]
    (:all-patient-assessments db)))

(rf/reg-sub
  ::current-patient-id
  (fn [db _]
    (:current-patient-id db)))

(rf/reg-sub
  ::active-tab
  (fn [db _]
    (:active-tab db "patients"))) ; Default to "patients"

(rf/reg-sub
  ::search-term
  (fn [db _]
    (:search-term db)))

(rf/reg-sub
  ::date-range
  (fn [db _]
    (:date-range db)))


(rf/reg-sub
  ::filtered-patients
  :<- [::all-patient-assessments]
  :<- [::search-term]
  :<- [::date-range]
  (fn [[assessments search-term date-range] _]
    (let [format-gender (fn [g] (case g "male" "男" "female" "女" "未知"))
          format-date (fn [d] (if d (utils/format-date d "YYYY-MM-DD") "未知日期")) ; Assuming utils/format-date-string
          patients (if (seq assessments)
                     (mapv (fn [assessment]
                             (let [basic-info (get-in assessment [:assessment_data :basic-info] {})]
                               {:key (:patient_id assessment)
                                :name (get basic-info :name "未知姓名")
                                :sex (format-gender (get basic-info :gender))
                                :age (get basic-info :age "未知")
                                :type (get basic-info :planned-surgery "术前评估") ; Or other relevant field
                                :date (format-date (:created_at assessment)) ; Or another date from assessment_data
                                :status (or (:doctor_status assessment) "待评估")})) ; Assuming a status field, else default
                           assessments)
                     [])]
      ;; Implement search and date filtering here if needed
      ;; For now, returning all transformed patients
      (cond->> patients
        (and search-term (not (clojure.string/blank? search-term)))
        (filterv (fn [p] (clojure.string/includes? (str/lower-case (:name p)) (str/lower-case search-term))))
        ;; Date range filtering would go here
        true identity))))


(rf/reg-sub
  ::selected-patient-assessment-data
  :<- [::all-patient-assessments]
  :<- [::current-patient-id]
  (fn [[assessments current-id] _]
    (when current-id
      (some #(when (= (:patient_id %) current-id) (:assessment_data %)) assessments))))

;; Subscriptions to provide initialValues for doctor's forms
;; These will take data from ::selected-patient-assessment-data and transform it

(rf/reg-sub
  ::doctor-form-brief-medical-history
  :<- [::selected-patient-assessment-data]
  (fn [adata _]
    (if adata
      (let [medical-summary (get adata :medical-summary {})
            comorbidities (get adata :comorbidities {})
            yes-no-desc (fn [val desc-val] {:yes-no (true? val) :description desc-val})]
        {:past-history (yes-no-desc (get comorbidities :general-past-history) (get comorbidities :general-past-history-detail))
         :allergic-history (yes-no-desc (get medical-summary :allergy-history) (get medical-summary :allergen))
         :surgery-anesthesia-history (yes-no-desc (get comorbidities :past-anesthesia-surgery) (get comorbidities :past-anesthesia-surgery-detail))
         :pregnancy (yes-no-desc (get comorbidities :pregnancy-history) (get comorbidities :pregnancy-detail))
         :blood-transfusion-history (yes-no-desc (get comorbidities :transfusion-history) (get comorbidities :transfusion-detail))
         :menstrual-period (yes-no-desc (get comorbidities :menstrual-period-active) (get comorbidities :menstrual-period-detail))
         :personal-history (cond-> []
                             (true? (get medical-summary :smoking-history)) (conj "smoke")
                             (true? (get medical-summary :drinking-history)) (conj "drink"))
         :other {:description (get comorbidities :other-history-detail)}})
      {})))


(rf/reg-sub
  ::doctor-form-physical-examination
  :<- [::selected-patient-assessment-data]
  (fn [adata _]
    (if adata
      (let [gc (get adata :general-condition {})
            pe-doc (get adata :physical-examination-doctor {}) ; Doctor specific observations
            como (get adata :comorbidities {})]
        {:general-condition (get-in gc [:doctor-assessment :general-condition-overall])
         :height (:height gc)
         :weight (:weight gc)
         :bp {:systolic (get-in gc [:blood-pressure :systolic])
              :diastolic (get-in gc [:blood-pressure :diastolic])}
         :heart-rate (:pulse gc) ; map pulse to heart-rate
         :respiratory-rate (:respiration gc)
         :temperature (:temperature gc)
         :mental-state (get gc :mental-state-doctor)
         :head-neck (get pe-doc :head-neck)
         :mouth-opening (get pe-doc :mouth-opening)
         :mallampati-score (get pe-doc :mallampati-score)
         :thyromental-distance (get pe-doc :thyromental-distance)
         :related-history {:difficult-airway (true? (get como :difficult-airway-history))
                           :postoperative-nausea (true? (get como :postoperative-nausea-history))
                           :malignant-hyperthermia (true? (get como :malignant-hyperthermia-personal-history))
                           :other (get como :other-related-history)
                           :other-checkbox (some? (get como :other-related-history))}
         :chest (get pe-doc :chest)})
      {})))

(rf/reg-sub
  ::doctor-form-lab-tests
  :<- [::selected-patient-assessment-data]
  (fn [adata _]
    (if adata
      (let [aux (get adata :auxiliary-examination {})
            cbc (get aux :complete-blood-count {})
            bio (get aux :biochemistry {})]
        {:complete-blood-count {:hemoglobin (:hemoglobin cbc)
                                :hematocrit (:hematocrit cbc)
                                :platelets (:platelets cbc)
                                :wbc (:wbc cbc)}
         :blood-type (:blood-type aux)
         :rh (:rh-factor aux)
         :coagulation (:coagulation-status aux)
         :biochemistry {:glucose (:glucose bio)
                        :alt (:alt bio)
                        :ast (:ast bio)
                        :sodium (:sodium bio)
                        :potassium (:potassium bio)}
         :ecg (:ecg-summary aux)
         :chest-xray (:chest-xray-summary aux)})
      {})))

