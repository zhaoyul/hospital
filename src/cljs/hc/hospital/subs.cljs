(ns hc.hospital.subs
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [hc.hospital.utils :as utils]))

(rf/reg-sub
  ::all-patient-assessments
  (fn [db _]
    ;; This sub still provides the raw list from API or other sources
    (:all-patient-assessments db)))

(rf/reg-sub
  ::current-patient-id
  (fn [db _]
    ;; Points to the top-level current-patient-id, which is set by ::select-patient
    (:current-patient-id db)))

(rf/reg-sub
  ::active-tab
  (fn [db _]
    (get-in db [:anesthesia :active-tab] "patients")))

(rf/reg-sub
  ::search-term
  (fn [db _]
    (get-in db [:anesthesia :search-term])))

(rf/reg-sub
  ::date-range
  (fn [db _]
    (get-in db [:anesthesia :date-range])))

;; New subscription for the example patient list
(rf/reg-sub
  ::anesthesia-example-patients
  (fn [db _]
    (get-in db [:anesthesia :patients])))

(rf/reg-sub
  ::filtered-patients
  :<- [::all-patient-assessments] ;; Raw assessments from API/mock
  :<- [::search-term]
  :<- [::date-range]
  :<- [::anesthesia-example-patients] ;; Changed from [:anesthesia :patients]
  (fn [[api-assessments search-term date-range example-patients] _]
    (let [format-gender (fn [g] (case g "male" "男" "female" "女" "未知"))
          format-date (fn [d] (if d (utils/format-date d "YYYY-MM-DD") "未知日期"))
          patients-from-api (if (seq api-assessments)
                              (mapv (fn [assessment]
                                      (let [basic-info (get-in assessment [:assessment_data :basic-info] {})]
                                        {:key (:patient_id assessment) ; Ensure this is the ID used elsewhere
                                         :name (get basic-info :name "未知姓名")
                                         :sex (format-gender (get basic-info :gender))
                                         :age (get basic-info :age "未知")
                                         :type (get basic-info :planned-surgery "术前评估")
                                         :date (format-date (:created_at assessment))
                                         :status (or (:doctor_status assessment) "待评估")}))
                                    api-assessments)
                              [])
          ;; Use example patients if API data is empty and example data exists
          display-patients (if (empty? patients-from-api)
                             example-patients ; Assumes example-patients has :key, :name, :sex, :age, :type, :date, :status
                             patients-from-api)]

      (cond->> display-patients
        (and search-term (not (clojure.string/blank? search-term)))
        (filterv (fn [p] (clojure.string/includes? (str/lower-case (str (:name p) (:key p))) (str/lower-case search-term))))
        ;; Date range filtering would go here, assuming :date is comparable
        date-range
        (filterv (fn [p] (when-let [p-date (:date p)]
                           (let [start (when (first date-range) (utils/format-date (first date-range) "YYYY-MM-DD"))
                                 end (when (second date-range) (utils/format-date (second date-range) "YYYY-MM-DD"))]
                             (cond
                               (and start end) (and (>= p-date start) (<= p-date end))
                               start (>= p-date start)
                               end (<= p-date end)
                               :else true)))))
        true identity))))

;; Subscription for the entire assessment block for the selected patient
;; This is what the forms will now use for their initialValues
(rf/reg-sub
  ::selected-patient-assessment-forms-data
  (fn [db _]
    (get-in db [:anesthesia :assessment])))

;; Specific subscriptions for each form section, deriving from the main assessment block
(rf/reg-sub
  ::doctor-form-brief-medical-history
  :<- [::selected-patient-assessment-forms-data]
  (fn [assessment-data _]
    (:brief-medical-history assessment-data)))

(rf/reg-sub
  ::doctor-form-physical-examination
  :<- [::selected-patient-assessment-forms-data]
  (fn [assessment-data _]
    (:physical-examination assessment-data)))

(rf/reg-sub
  ::doctor-form-lab-tests
  :<- [::selected-patient-assessment-forms-data]
  (fn [assessment-data _]
    (:lab-tests assessment-data)))

