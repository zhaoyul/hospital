(ns hc.hospital.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [hc.hospital.db :as app-db] ; Changed alias and ns to root app db
            [ajax.core :as ajax]))

;; -- Initialization
(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    app-db/default-db)) ; Use app-db for initialization

;; --- Fetching All Assessments ---
(rf/reg-event-fx
  ::fetch-all-assessments
  (fn [_ _] ; Removed unused :keys [db]
    {:http-xhrio {:method          :get
                  :uri             "/api/patient/assessments"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::set-all-assessments]
                  :on-failure      [::fetch-all-assessments-failed]}}))

(rf/reg-event-db
 ::set-all-assessments
 (fn [db [_ assessments]]
   (assoc db :all-patient-assessments assessments :current-patient-id nil)))

(rf/reg-event-db
 ::fetch-all-assessments-failed
 (fn [db [_ error]]
   (js/console.error "Failed to fetch all assessments:" error)
   (assoc db :fetch-assessments-error error)))


;; --- Patient Selection ---
(rf/reg-event-db
 ::select-patient
 (fn [db [_ patient-id]]
   (let [all-pg-assessments (get db :all-patient-assessments []) ; Data from API
         example-patients (get-in db [:anesthesia :patients] []) ; Example data from app-db/default-db

         selected-api-patient (first (filter #(= (:patient_id %) patient-id) all-pg-assessments))
         selected-example-patient (when-not selected-api-patient
                                    (first (filter #(= (:key %) patient-id) example-patients)))

         raw-assessment-data (cond
                               selected-api-patient (:assessment_data selected-api-patient)
                               ;; If using example patients, their structure might need mapping or they should conform
                               selected-example-patient (select-keys selected-example-patient [:brief-medical-history :physical-examination :lab-tests])
                               :else {})

         ;; Get the pristine default structure for the assessment forms from app-db
         default-form-structure (get-in app-db/default-db [:anesthesia :assessment])

         ;; Merge: Start with defaults, then overlay with patient's specific assessment data.
         ;; This ensures the form data structure is always complete.
         ;; Assumes raw-assessment-data's sub-keys (if present) match default-form-structure's sub-keys.
         final-assessment-data {:brief-medical-history (merge (:brief-medical-history default-form-structure)
                                                              (:brief-medical-history raw-assessment-data))
                                :physical-examination (merge (:physical-examination default-form-structure)
                                                             (:physical-examination raw-assessment-data))
                                :lab-tests (merge (:lab-tests default-form-structure)
                                                  (:lab-tests raw-assessment-data))
                                :anesthesia-plan (merge (:anesthesia-plan default-form-structure) ;; Keep anesthesia plan
                                                        (:anesthesia-plan raw-assessment-data))}]

     (-> db
         (assoc :current-patient-id patient-id)
         (assoc-in [:anesthesia :assessment] final-assessment-data)))))

;; --- Updating Assessment Data from Doctor's Forms ---
(rf/reg-event-db
  ::update-brief-medical-history
  (fn [db [_ form-values]]
    ;; form-values from AntD should directly match the structure of [:anesthesia :assessment :brief-medical-history]
    (assoc-in db [:anesthesia :assessment :brief-medical-history] form-values)))

(rf/reg-event-db
 ::update-physical-examination
 (fn [db [_ form-values]]
   (assoc-in db [:anesthesia :assessment :physical-examination] form-values)))

(rf/reg-event-db
 ::update-lab-tests
 (fn [db [_ form-values]]
   (assoc-in db [:anesthesia :assessment :lab-tests] form-values)))

;; Other events like set-active-tab, update-search-term, etc. remain
(rf/reg-event-db ::set-active-tab (fn [db [_ tab]] (assoc-in db [:anesthesia :active-tab] tab)))
(rf/reg-event-db ::update-search-term (fn [db [_ term]] (assoc-in db [:anesthesia :search-term] term)))
(rf/reg-event-db ::set-date-range (fn [db [_ range]] (assoc-in db [:anesthesia :date-range] range)))
;; Mock events for approve/postpone/reject for now
(rf/reg-event-fx ::approve-patient (fn [{:keys [db]} _] (js/alert (str "Approved patient: " (get-in db [:anesthesia :current-patient-id]))) {}))
(rf/reg-event-fx ::postpone-patient (fn [{:keys [db]} _] (js/alert (str "Postponed patient: " (get-in db [:anesthesia :current-patient-id]))) {}))
(rf/reg-event-fx ::reject-patient (fn [{:keys [db]} _] (js/alert (str "Rejected patient: " (get-in db [:anesthesia :current-patient-id]))) {}))

;; Event for search (currently not implemented, but good to have a placeholder)
(rf/reg-event-db ::search-patients (fn [db [_ term]] (js/console.log "Searching for " term) db))
