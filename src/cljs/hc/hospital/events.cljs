;; filepath: /Users/a123/sandbox/rc/hospital/src/cljs/hc/hospital/events.cljs
(ns hc.hospital.events
  (:require [re-frame.core :as rf]
            [hc.hospital.db :as db]
            [cljs.spec.alpha :as s]
            [clojure.string :as string]
            [reagent.core :as r]))

;; -- Initialization
(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

;; -- Patient List Management
(rf/reg-event-db
 ::select-patient
 (fn [db [_ patient-id]]
   (assoc db :current-patient-id patient-id)))

(rf/reg-event-db
 ::update-search-term
 (fn [db [_ term]]
   (assoc db :search-term term)))

(rf/reg-event-db
 ::set-date-range
 (fn [db [_ range]]
   (assoc db :date-range range)))

;; TODO: Implement actual search logic
(rf/reg-event-db
 ::search-patients
 (fn [db [_ term]]
   ;; Placeholder: just logs the search term for now
   (js/console.log "Searching for:" term)
   db))

;; -- Tab Navigation
(rf/reg-event-db
 ::set-active-tab
 (fn [db [_ tab-key]]
   (assoc db :active-tab tab-key)))

(rf/reg-event-db
 ::set-active-assessment-tab
 (fn [db [_ tab-key]]
   (assoc db :active-assessment-tab tab-key)))

;; -- Assessment Actions (Placeholders)
(rf/reg-event-db
 ::approve-patient
 (fn [db _]
   (js/console.log "Patient approved (placeholder)")
   ;; Potentially update patient status in db
   db))

(rf/reg-event-db
 ::postpone-patient
 (fn [db _]
   (js/console.log "Patient postponed (placeholder)")
   ;; Potentially update patient status in db
   db))

(rf/reg-event-db
 ::reject-patient
 (fn [db _]
   (js/console.log "Patient rejected (placeholder)")
   ;; Potentially update patient status in db
   db))


;; -- Form Data Updates (Refactored)

(defn- parse-form-values [values]
  ;; Helper to potentially parse/clean form values if needed
  ;; For now, just handles the checkbox group structure
  (cond-> values
    (get-in values [:personal-history :smoking-drinking])
    (assoc-in [:personal-history :smoking] (boolean (some #{"smoke"} (get-in values [:personal-history :smoking-drinking]))))
    (get-in values [:personal-history :smoking-drinking])
    (assoc-in [:personal-history :drinking] (boolean (some #{"drink"} (get-in values [:personal-history :smoking-drinking]))))
    true (update :personal-history dissoc :smoking-drinking) ; Remove the temporary key

    ;; Handle radio + description fields
    (:past-history-radio values)
    (assoc :past-history {:value (:past-history-radio values)
                          :description (if (= (:past-history-radio values) "yes")
                                         (:past-history-desc values)
                                         "")})
    true (dissoc values :past-history-radio :past-history-desc)

    (:allergic-history-radio values)
    (assoc :allergic-history {:value (:allergic-history-radio values)
                              :description (if (= (:allergic-history-radio values) "yes")
                                             (:allergic-history-desc values)
                                             "")})
    true (dissoc values :allergic-history-radio :allergic-history-desc)

    (:surgery-anesthesia-history-radio values)
    (assoc :surgery-anesthesia-history {:value (:surgery-anesthesia-history-radio values)
                                        :description (if (= (:surgery-anesthesia-history-radio values) "yes")
                                                       (:surgery-anesthesia-history-desc values)
                                                       "")})
    true (dissoc values :surgery-anesthesia-history-radio :surgery-anesthesia-history-desc)

    (:pregnancy-radio values)
    (assoc :pregnancy {:value (:pregnancy-radio values)
                       :description (if (= (:pregnancy-radio values) "yes")
                                      (:pregnancy-desc values)
                                      "")})
    true (dissoc values :pregnancy-radio :pregnancy-desc)

    (:blood-transfusion-history-radio values)
    (assoc :blood-transfusion-history {:value (:blood-transfusion-history-radio values)
                                       :description (if (= (:blood-transfusion-history-radio values) "yes")
                                                      (:blood-transfusion-history-desc values)
                                                      "")})
    true (dissoc values :blood-transfusion-history-radio :blood-transfusion-history-desc)

    (:menstrual-period-radio values)
    (assoc :menstrual-period {:value (:menstrual-period-radio values)
                              :description (if (= (:menstrual-period-radio values) "yes")
                                             (:menstrual-period-desc values)
                                             "")})
    true (dissoc values :menstrual-period-radio :menstrual-period-desc)

    ;; Handle related history 'other' checkbox
    (contains? (get values :related-history) :other-checkbox)
    (update :related-history (fn [rh]
                               (if (:other-checkbox rh)
                                 (dissoc rh :other-checkbox) ; Keep :other if checked
                                 (assoc (dissoc rh :other-checkbox) :other false)))) ; Set :other to false if unchecked

    ))

(rf/reg-event-db
 ::update-brief-medical-history
 (fn [db [_ form-values]]
   (let [parsed-values (parse-form-values form-values)]
     (update-in db [:assessment-data :brief-medical-history] merge parsed-values))))

(rf/reg-event-db
 ::update-physical-examination
 (fn [db [_ form-values]]
   (let [parsed-values (parse-form-values form-values)]
     (update-in db [:assessment-data :physical-examination] merge parsed-values))))

(rf/reg-event-db
 ::update-lab-tests
 (fn [db [_ form-values]]
   ;; Assuming lab test values don't need complex parsing like the others for now
   (update-in db [:assessment-data :lab-tests] merge form-values)))


;; -- Old Granular Event Handlers (To be removed or commented out)
#_(rf/reg-event-db
   ::update-medical-history-option
   (fn [db [_ field value description]]
     (assoc-in db [:assessment-data :brief-medical-history field] {:value value :description description})))

#_(rf/reg-event-db
   ::update-personal-history
   (fn [db [_ history-map]]
     (update-in db [:assessment-data :brief-medical-history :personal-history] merge history-map)))

#_(rf/reg-event-db
   ::update-other-description
   (fn [db [_ field value]]
     (assoc-in db [:assessment-data :brief-medical-history field] value)))

#_(rf/reg-event-db
   ::update-physical-exam
   (fn [db [_ field value]]
     (assoc-in db [:assessment-data :physical-examination field] value)))

#_(rf/reg-event-db
   ::update-blood-pressure
   (fn [db [_ systolic diastolic]]
     (assoc-in db [:assessment-data :physical-examination :bp] {:systolic systolic :diastolic diastolic})))

#_(rf/reg-event-db
   ::update-related-history
   (fn [db [_ field value]]
     (assoc-in db [:assessment-data :physical-examination :related-history field] value)))

#_(rf/reg-event-db
   ::update-lab-test
   (fn [db [_ path value]]
     (assoc-in db (concat [:assessment-data :lab-tests] path) value)))
