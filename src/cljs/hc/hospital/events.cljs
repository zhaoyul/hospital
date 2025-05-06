;; Assuming hc.hospital.events is the correct namespace for your events
(ns hc.hospital.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [hc.hospital.patient.db :as db]
            [ajax.core :as ajax]
            [hc.hospital.subs :as subs] ; For referring to subs if needed
            [hc.hospital.utils :as utils])) ; Assuming you have a utils ns for date formatting etc.

;; -- Initialization
(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))


;; --- Fetching All Assessments ---
(rf/reg-event-fx
  ::fetch-all-assessments
  (fn [{:keys [db]} _]
    {:http-xhrio {:method          :get
                  :uri             "/api/patient/assessments"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::set-all-assessments]
                  :on-failure      [::fetch-all-assessments-failed]}}))

(rf/reg-event-db
 ::set-all-assessments
 (fn [db [_ assessments]]
   ;; Ensure current-patient-id is cleared or handled if the list changes
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
   (assoc db :current-patient-id patient-id)))

;; --- Updating Assessment Data from Doctor's Forms ---
;; Helper to update nested assessment data for the current patient
(defn update-current-patient-assessment [db path-to-value new-value]
  (if-let [current-id (:current-patient-id db)]
    (if-let [patient-idx (first (keep-indexed (fn [idx p] (when (= (:patient_id p) current-id) idx))
                                             (:all-patient-assessments db)))]
      (let [base-path [:all-patient-assessments patient-idx :assessment_data]]
        (assoc-in db (concat base-path path-to-value) new-value))
      (do (js/console.warn "No patient found for ID:" current-id "in update-current-patient-assessment") db))
    (do (js/console.warn "No current-patient-id in update-current-patient-assessment") db)))

;; Specific update events for each form section in the doctor's view
;; These will take the `all-values` from AntD form's onValuesChange
;; and map them to the patient's assessment_data structure.

(rf/reg-event-db
  ::update-brief-medical-history
  (fn [db [_ form-values]]
    (let [current-id (:current-patient-id db)]
      (if-not current-id
        (do (js/console.error "No patient selected for updating brief medical history") db)
        (let [patient-idx (first (keep-indexed (fn [idx p] (when (= (:patient_id p) current-id) idx))
                                               (:all-patient-assessments db)))
              transform-val (fn [prefix]
                              {:yes-no (get-in form-values [prefix :yes-no])
                               :description (get-in form-values [prefix :description])})
              allergy-data (transform-val :allergic-history)
              surgery-data (transform-val :surgery-anesthesia-history)
              pregnancy-data (transform-val :pregnancy)
              transfusion-data (transform-val :blood-transfusion-history)
              menstrual-data (transform-val :menstrual-period)
              past-history-data (transform-val :past-history)
              other-data (get-in form-values [:other :description])]

          (if patient-idx
            (-> db
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :medical-summary :allergy-history] (:yes-no allergy-data))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :medical-summary :allergen] (:description allergy-data))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :comorbidities :past-anesthesia-surgery] (:yes-no surgery-data))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :comorbidities :past-anesthesia-surgery-detail] (:description surgery-data))
                ;; Assuming these map to new fields or specific comorbidities
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :comorbidities :pregnancy-history] (:yes-no pregnancy-data))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :comorbidities :pregnancy-detail] (:description pregnancy-data))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :comorbidities :transfusion-history] (:yes-no transfusion-data))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :comorbidities :transfusion-detail] (:description transfusion-data))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :comorbidities :menstrual-period-active] (:yes-no menstrual-data))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :comorbidities :menstrual-period-detail] (:description menstrual-data))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :comorbidities :general-past-history] (:yes-no past-history-data))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :comorbidities :general-past-history-detail] (:description past-history-data))

                (assoc-in [:all-patient-assessments patient-idx :assessment_data :medical-summary :smoking-history] (some #{"smoke"} (get form-values :personal-history)))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :medical-summary :drinking-history] (some #{"drink"} (get form-values :personal-history)))
                (assoc-in [:all-patient-assessments patient-idx :assessment_data :comorbidities :other-history-detail] other-data))
            db))))))

(rf/reg-event-db
 ::update-physical-examination
 (fn [db [_ form-values]]
   ;; This needs careful mapping from form-values to nested assessment_data
   ;; Example: (update-current-patient-assessment db [:general-condition :height] (:height form-values))
   ;; This will be extensive due to the number of fields.
   (let [current-id (:current-patient-id db)]
     (if-not current-id
       (do (js/console.error "No patient selected for updating physical exam") db)
       (let [patient-idx (first (keep-indexed (fn [idx p] (when (= (:patient_id p) current-id) idx))
                                               (:all-patient-assessments db)))]
         (if patient-idx
           (let [path-prefix [:all-patient-assessments patient-idx :assessment_data]]
             (-> db
                 (assoc-in (concat path-prefix [:general-condition :doctor-assessment :general-condition-overall]) (:general-condition form-values))
                 (assoc-in (concat path-prefix [:general-condition :height]) (:height form-values))
                 (assoc-in (concat path-prefix [:general-condition :weight]) (:weight form-values))
                 (assoc-in (concat path-prefix [:general-condition :blood-pressure :systolic]) (get-in form-values [:bp :systolic]))
                 (assoc-in (concat path-prefix [:general-condition :blood-pressure :diastolic]) (get-in form-values [:bp :diastolic]))
                 (assoc-in (concat path-prefix [:general-condition :pulse]) (:heart-rate form-values)) ; Map heart-rate to pulse
                 (assoc-in (concat path-prefix [:general-condition :respiration]) (:respiratory-rate form-values))
                 (assoc-in (concat path-prefix [:general-condition :temperature]) (:temperature form-values))
                 (assoc-in (concat path-prefix [:general-condition :mental-state-doctor]) (:mental-state form-values)) ; Distinguish from patient's mental-state if needed
                 (assoc-in (concat path-prefix [:physical-examination-doctor :head-neck]) (:head-neck form-values))
                 (assoc-in (concat path-prefix [:physical-examination-doctor :mouth-opening]) (:mouth-opening form-values))
                 (assoc-in (concat path-prefix [:physical-examination-doctor :mallampati-score]) (:mallampati-score form-values))
                 (assoc-in (concat path-prefix [:physical-examination-doctor :thyromental-distance]) (:thyromental-distance form-values))
                 (assoc-in (concat path-prefix [:physical-examination-doctor :chest]) (:chest form-values))
                 ;; Related History - map these to appropriate places in comorbidities or physical-examination-doctor
                 (assoc-in (concat path-prefix [:comorbidities :difficult-airway-history]) (get-in form-values [:related-history :difficult-airway]))
                 (assoc-in (concat path-prefix [:comorbidities :postoperative-nausea-history]) (get-in form-values [:related-history :postoperative-nausea]))
                 (assoc-in (concat path-prefix [:comorbidities :malignant-hyperthermia-personal-history]) (get-in form-values [:related-history :malignant-hyperthermia]))
                 (assoc-in (concat path-prefix [:comorbidities :other-related-history]) (get-in form-values [:related-history :other]))
                 ;; ... other fields
                 ))
           db))))))

(rf/reg-event-db
 ::update-lab-tests
 (fn [db [_ form-values]]
   ;; Similar mapping for lab tests
   (let [current-id (:current-patient-id db)]
     (if-not current-id
       (do (js/console.error "No patient selected for updating lab tests") db)
       (let [patient-idx (first (keep-indexed (fn [idx p] (when (= (:patient_id p) current-id) idx))
                                               (:all-patient-assessments db)))]
         (if patient-idx
           (let [path-prefix [:all-patient-assessments patient-idx :assessment_data]]
             (-> db
                 (assoc-in (concat path-prefix [:auxiliary-examination :complete-blood-count :hemoglobin]) (get-in form-values [:complete-blood-count :hemoglobin]))
                 (assoc-in (concat path-prefix [:auxiliary-examination :complete-blood-count :hematocrit]) (get-in form-values [:complete-blood-count :hematocrit]))
                 (assoc-in (concat path-prefix [:auxiliary-examination :complete-blood-count :platelets]) (get-in form-values [:complete-blood-count :platelets]))
                 (assoc-in (concat path-prefix [:auxiliary-examination :complete-blood-count :wbc]) (get-in form-values [:complete-blood-count :wbc]))
                 (assoc-in (concat path-prefix [:auxiliary-examination :blood-type]) (:blood-type form-values))
                 (assoc-in (concat path-prefix [:auxiliary-examination :rh-factor]) (:rh form-values))
                 (assoc-in (concat path-prefix [:auxiliary-examination :coagulation-status]) (:coagulation form-values))
                 (assoc-in (concat path-prefix [:auxiliary-examination :biochemistry :glucose]) (get-in form-values [:biochemistry :glucose]))
                 (assoc-in (concat path-prefix [:auxiliary-examination :biochemistry :alt]) (get-in form-values [:biochemistry :alt]))
                 (assoc-in (concat path-prefix [:auxiliary-examination :biochemistry :ast]) (get-in form-values [:biochemistry :ast]))
                 (assoc-in (concat path-prefix [:auxiliary-examination :biochemistry :sodium]) (get-in form-values [:biochemistry :sodium]))
                 (assoc-in (concat path-prefix [:auxiliary-examination :biochemistry :potassium]) (get-in form-values [:biochemistry :potassium]))
                 (assoc-in (concat path-prefix [:auxiliary-examination :ecg-summary]) (:ecg form-values)) ; Store text summary
                 (assoc-in (concat path-prefix [:auxiliary-examination :chest-xray-summary]) (:chest-xray form-values)) ; Store text summary
                 ;; ... other fields
                 ))
           db))))))


;; Other events like set-active-tab, update-search-term, etc. remain
(rf/reg-event-db ::set-active-tab (fn [db [_ tab]] (assoc db :active-tab tab)))
(rf/reg-event-db ::update-search-term (fn [db [_ term]] (assoc db :search-term term)))
(rf/reg-event-db ::set-date-range (fn [db [_ range]] (assoc db :date-range range)))
;; Mock events for approve/postpone/reject for now
(rf/reg-event-fx ::approve-patient (fn [{:keys [db]} _] (js/alert (str "Approved patient: " (:current-patient-id db))) {}))
(rf/reg-event-fx ::postpone-patient (fn [{:keys [db]} _] (js/alert (str "Postponed patient: " (:current-patient-id db))) {}))
(rf/reg-event-fx ::reject-patient (fn [{:keys [db]} _] (js/alert (str "Rejected patient: " (:current-patient-id db))) {}))

;; Event for search (currently not implemented, but good to have a placeholder)
(rf/reg-event-db ::search-patients (fn [db [_ term]] (js/console.log "Searching for " term) db))
