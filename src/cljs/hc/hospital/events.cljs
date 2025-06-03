(ns hc.hospital.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [hc.hospital.db :as app-db]
            [ajax.core :as ajax]
            [clojure.string :as str] ; Added for filename extraction if needed
            [taoensso.timbre :as timbre]))

;; -- Default Canonical Assessment Structure --
(def default-canonical-assessment
  {:basic_info {:outpatient_number nil, :name nil, :gender nil, :age nil,
                :department nil, :health_card_number nil, :diagnosis nil,
                :planned_surgery nil, :height nil, :weight nil,
                :patient_submitted_at nil, :assessment_updated_at nil,
                :assessment_status "待评估", :doctor_name nil, :assessment_notes nil}
   :medical_history {:allergy {:has_history false, :details nil, :last_reaction_date nil}
                     :smoking {:has_history false, :years nil, :cigarettes_per_day nil}
                     :drinking {:has_history false, :years nil, :alcohol_per_day nil}}
   :physical_examination {:mental_state nil, :activity_level nil,
                          :bp_systolic nil, :bp_diastolic nil, :heart_rate nil,
                          :respiratory_rate nil, :temperature nil, :spo2 nil,
                          :heart {:status nil, :notes nil}, :lungs {:status nil, :notes nil},
                          :airway {:status nil, :notes nil}, :teeth {:status nil, :notes nil},
                          :spine_limbs {:status nil, :notes nil}, :neuro {:status nil, :notes nil},
                          :other_findings nil}
   :comorbidities {:respiratory {:has false, :details nil}
                   :cardiovascular {:has false, :details nil}
                   :endocrine {:has false, :details nil}
                   :neuro_psychiatric {:has false, :details nil}
                   :neuromuscular {:has false, :details nil}
                   :hepatic {:has false, :details nil}
                   :renal {:has false, :details nil}
                   :musculoskeletal {:has false, :details nil}
                   :malignant_hyperthermia_fh {:has false, :details nil}
                   :anesthesia_surgery_history {:has false, :details nil}
                   :special_medications {:has_taken false, :details nil, :last_dose_time nil}}
   :auxiliary_examinations []
   :auxiliary_examinations_notes nil
   :anesthesia_plan {:asa_rating nil, :anesthesia_type nil, :preoperative_instructions nil}
   ;; Adding new assessment sections, initialized to empty maps
   :circulatory_system {}
   :respiratory_system {}
   :mental_neuromuscular_system {}
   :endocrine_system {}
   :liver_kidney_system {}
   :digestive_system {}
   :hematologic_system {}
   :immune_system {}
   :special_medication_history {}
   :special_disease_history {}
   :nutritional_assessment {}
   :pregnancy_assessment {}
   :surgical_anesthesia_history {}
   :airway_assessment {}
   :spinal_anesthesia_assessment {}})


;; -- Initialization
(rf/reg-event-db ::initialize-db
  (fn [_ _]
    (assoc app-db/default-db :anesthesia {:current-assessment-canonical default-canonical-assessment})))


;; --- Fetching All Assessments ---
(rf/reg-event-fx ::fetch-all-assessments
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/api/patient/assessments"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::set-all-assessments]
                  :on-failure      [::fetch-all-assessments-failed]}}))

(rf/reg-event-db ::set-all-assessments
  (fn [db [_ assessments]]
    (-> db
        (assoc-in [:anesthesia :all-patient-assessments] assessments)
        (assoc-in [:anesthesia :current-patient-id] nil) ; Corrected path
        (assoc :fetch-assessments-error nil))))

(rf/reg-event-db ::fetch-all-assessments-failed
  (fn [db [_ error]]
    (timbre/error "获取所有评估失败:" error)
    (assoc db :fetch-assessments-error error)))

;; --- Patient Selection ---
(rf/reg-event-db ::select-patient
  (fn [db [_ patient-key]]
    (if (nil? patient-key) ;; Handle deselection
      (-> db
          (assoc-in [:anesthesia :current-patient-id] nil)
          (assoc-in [:anesthesia :current-assessment-canonical] default-canonical-assessment))
      (let [all-assessments (get-in db [:anesthesia :all-patient-assessments])
            selected-full-assessment (first (filter #(= (:patient_id %) patient-key) all-assessments))
            canonical-assessment-data (when selected-full-assessment
                                        (get selected-full-assessment :assessment_data {}))]
        (-> db
            (assoc-in [:anesthesia :current-patient-id] patient-key)
            (assoc-in [:anesthesia :current-assessment-canonical]
                      (if (seq canonical-assessment-data)
                        canonical-assessment-data
                        default-canonical-assessment)))))))


;; --- Updating Canonical Assessment Data ---
(rf/reg-event-db ::update-canonical-assessment-field
  (fn [db [_ path value]]
    (assoc-in db (concat [:anesthesia :current-assessment-canonical] path) value)))

(rf/reg-event-db ::update-canonical-assessment-section
  [(when ^boolean goog.DEBUG re-frame.core/debug)]
  (fn [db [_ section-key section-data]]
    ;; Ensure the section-key exists before trying to merge
    (if (get-in db [:anesthesia :current-assessment-canonical section-key])
      (update-in db [:anesthesia :current-assessment-canonical section-key] merge section-data)
      ;; If section-key somehow doesn't exist (e.g. typo), log error and return db unchanged
      ;; or initialize it: (assoc-in db [:anesthesia :current-assessment-canonical section-key] section-data)
      ;; For now, let's be safe and merge, assuming keys are pre-defined in default-canonical-assessment
      (update-in db [:anesthesia :current-assessment-canonical section-key] (fnil merge {}) section-data))))

;; Specific handlers for auxiliary exam file list management (canonical structure)
(rf/reg-event-db ::add-aux-exam-file
  (fn [db [_ file-map]]
    ;; file-map should be {:type "...", :filename "...", :url "...", :uploaded_by "doctor/patient", :uploaded_at "timestamp", :uid "ant-design-uid"}
    ;; For new uploads by doctor, :uploaded_by should be "doctor" and :uploaded_at should be current time.
    ;; The actual file upload to server and getting the final URL is handled by upload-props' :customRequest
    ;; This event primarily adds the file metadata to the list for display and later saving.
    (update-in db [:anesthesia :current-assessment-canonical :auxiliary_examinations] conj file-map)))

(rf/reg-event-db ::update-aux-exam-file-list
  (fn [db [_ file-list]]
    ;; Used by Ant Design's Upload when fileList is managed externally.
    ;; file-list is the full list of files from AntD, each needs to be in our canonical format.
    ;; This event assumes that the items in file-list are already (or can be easily transformed into)
    ;; our canonical map structure for auxiliary_examinations.
    ;; For simplicity, this example assumes file-list items are compatible.
    ;; A more robust version would transform each item.
    (assoc-in db [:anesthesia :current-assessment-canonical :auxiliary_examinations] file-list)))

(rf/reg-event-db ::remove-aux-exam-file
  (fn [db [_ file-uid-or-url]]
    ;; file-uid-or-url can be the :uid (for new files) or :url (for existing files from server)
    (update-in db [:anesthesia :current-assessment-canonical :auxiliary_examinations]
               (fn [files] (vec (remove #(or (= (:uid %) file-uid-or-url) (= (:url %) file-uid-or-url)) files))))))

;; Removed ::update-assessment-notes, use ::update-canonical-assessment-field for this.
;; e.g. (rf/dispatch [::update-canonical-assessment-field [:anesthesia_plan :notes] new-notes-text])
;; or (rf/dispatch [::update-canonical-assessment-field [:auxiliary_examinations_notes] new-notes-text])

(rf/reg-event-db ::update-search-term
  (fn [db [_ term]]
    (assoc-in db [:anesthesia :search-term] term)))

(rf/reg-event-db ::set-date-range
  (fn [db [_ range-values]]
    (assoc-in db [:anesthesia :date-range] range-values)))

(rf/reg-event-db ::set-assessment-status-filter
  (fn [db [_ status]]
    (assoc-in db [:anesthesia :assessment-status-filter] status)))

(rf/reg-event-fx ::sync-applications
  (fn [_ _]
    (js/alert "正在从HIS系统同步患者列表数据...")
    {:dispatch [::fetch-all-assessments]})) ; Optionally, trigger a fetch after sync indication

(rf/reg-event-fx ::scan-check-in
  (fn [_ _]
    (js/alert "请将患者二维码对准扫描设备")
    {}))

(rf/reg-event-db ::search-patients
  (fn [db [_ term]]
    (timbre/info "显式搜索患者:" term)
    (assoc-in db [:anesthesia :search-term] term)))

;; --- Assessment Actions ---
(rf/reg-event-fx ::approve-patient (fn [{:keys [db]} _] (js/alert (str "批准患者: " (get-in db [:anesthesia :current-patient-id]))) {}))
(rf/reg-event-fx ::postpone-patient (fn [{:keys [db]} _] (js/alert (str "暂缓患者: " (get-in db [:anesthesia :current-patient-id]))) {}))
(rf/reg-event-fx ::reject-patient (fn [{:keys [db]} _] (js/alert (str "驳回患者: " (get-in db [:anesthesia :current-patient-id]))) {}))

;; --- Save Final Assessment ---
(rf/reg-event-fx ::save-final-assessment-later
  (fn [_ _]
    {:dispatch-later [{:ms 30 :dispatch [::save-final-assessment]}]}))

(rf/reg-event-fx ::save-final-assessment
  [(when ^boolean goog.DEBUG re-frame.core/debug)]
  (fn [{:keys [db]} _]
    (let [current-patient-id (get-in db [:anesthesia :current-patient-id])
          ;; The entire canonical assessment is now the payload
          assessment-payload (get-in db [:anesthesia :current-assessment-canonical])]
      (if current-patient-id
        (if assessment-payload
          {:http-xhrio {:method          :put
                        :uri             (str "/api/patient/assessment/" current-patient-id)
                        :params          assessment-payload ;; Send the whole canonical structure
                        :format          (ajax/json-request-format) ;; <--- ADD THIS LINE
                        :response-format (ajax/json-response-format {:keywords? true})
                        :on-success      [::save-assessment-success]
                        :on-failure      [::save-assessment-failed]}}
          (do (timbre/warn "Assessment data is nil, cannot save.")
              {}))
        (do (timbre/warn "No patient selected, cannot save assessment.")
            {})))))

(rf/reg-event-db ::save-assessment-success
  (fn [db [_ response]]
    (timbre/info "评估保存成功:" response)
    ;; Optionally, update the local canonical form with any response data if needed,
    ;; e.g., if backend returns updated timestamps or generated IDs.
    ;; For now, just refresh.
    (rf/dispatch [::fetch-all-assessments]) ; Refresh list after saving
    db))

(rf/reg-event-db ::save-assessment-failed
  (fn [db [_ error]]
    (timbre/error "评估保存失败:" error)
    (js/alert (str "保存失败: " (pr-str (:response error) (:status error)))) ; Show more details
    db))

;; Removed ::update-patient-form-field as it's replaced by ::update-canonical-assessment-field
;; Kept session, login, logout, doctor management events as they are unrelated to assessment structure.

;; --- Session Check Events ---
(rf/reg-event-fx ::check-session
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/api/me" ; Changed from /api/user/me
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::set-current-doctor-from-session] ; New event to handle session data
                  :on-failure      [::session-check-failed]}}))

;; Helper to set doctor from session and ensure session check pending is false
(rf/reg-event-db ::set-current-doctor-from-session
  (fn [db [_ {:keys [doctor]}]]
    (timbre/info "Session check success. Doctor:" doctor ", Logged in:" (some? doctor))
    (assoc db
           :current-doctor doctor
           :is-logged-in (some? doctor)
           :login-error nil
           :session-check-pending? false)))

(rf/reg-event-fx ::session-check-failed
  (fn [{:keys [db]} [_ error-details]]
    (timbre/warn "Session check failed or no active session:" error-details)
    ;; For 401 or 404 (no user /me), it's an expected "not logged in" state.
    ;; For other errors, it's unexpected, but we still clear session.
    ;;(js-debugger)
    (when-not (or (= 401 (:status error-details)) (= 404 (:status error-details)))
      (timbre/error "Unexpected error during session check:" error-details))
    (timbre/info "Redirecting to /login due to session check failure.")
    (js/window.location.assign "/login") ; Redirect to /login
    {:dispatch [::clear-current-doctor]})) ; clear-current-doctor now also sets session-check-pending? to false


;; --- Login Events ---
(rf/reg-event-db ::set-current-doctor
  (fn [db [_ doctor-data]]
    (timbre/info "Setting current doctor. Doctor data available:" (some? doctor-data) ", Is logged in set to true.")
    (assoc db
           :current-doctor doctor-data
           :is-logged-in true
           :login-error nil
           :session-check-pending? false))) ; Ensure session check is marked complete

(rf/reg-event-db ::login-failure
  (fn [db [_ error-details]]
    (timbre/error "Login failed:" error-details)
    (assoc db
           :current-doctor nil
           :is-logged-in false
           :login-error (or (:error (:response error-details)) (:message error-details) "Login failed")
           ;; Do not set session-check-pending here, as login might fail and we'd want to stay on login page
           )))

(rf/reg-event-fx ::handle-login
  (fn [{:keys [db]} [_ {:keys [username password]}]]
    {:http-xhrio {:method          :post
                  :uri             "/api/users/login"
                  :params          {:username username :password password}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::login-success]
                  :on-failure      [::login-failure]}}))

;; Helper event for login success to handle multiple dispatches
(rf/reg-event-fx ::login-success
  (fn [{:keys [db]} [_ response-data]]
    (timbre/info "Login successful. Setting justLoggedIn flag. Response:" response-data ". Navigating to / shortly.")
    ;; Assuming the server returns {:message "登录成功" :doctor {...}}
    ;; Navigation is now handled by ::navigate-to-app-root
    ;; ::set-current-doctor will handle setting session-check-pending to false
    (js/localStorage.setItem "justLoggedIn" "true")
    {:dispatch [::set-current-doctor (:doctor response-data)]
     :dispatch-later [{:ms 30 :dispatch [::navigate-to-app-root]}]
     }))

;; --- Navigation Events ---
(rf/reg-event-fx ::navigate-to-app-root
  (fn [_ _]
    (js/window.location.assign "/")
    {})) ; No change to db

;; Event to handle post-login initialization
(rf/reg-event-db ::handle-just-logged-in
  (fn [db _]
    (timbre/info "Handling post-login navigation: session-check-pending? set to false.")
    (assoc db :session-check-pending? false)))

;; --- Logout Events ---
(rf/reg-event-db ::clear-current-doctor
  (fn [db _]
    (timbre/info "Clearing current doctor. Is logged in set to false. Session check pending set to false.")
    (assoc db
           :current-doctor nil
           :is-logged-in false
           :login-error nil
           :session-check-pending? false))) ; Ensure session check is marked complete

(rf/reg-event-fx ::handle-logout
  (fn [{:keys [db]} _]
    {:http-xhrio {:method          :post
                  :uri             "/api/users/logout"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::logout-finished]
                  :on-failure      [::logout-finished]} ; Always proceed to clear client session
     }))

(rf/reg-event-fx ::logout-finished
  (fn [{:keys [db]} [_ response-data]]
    (timbre/info "Logout processed. Response (if any):" response-data)
    (js/window.location.assign "/login") ; Redirect to /login page
    {:dispatch [::clear-current-doctor]}))


;; Doctor management events - seem unrelated to patient assessment form changes, so keep them.
(rf/reg-event-db ::initialize-doctors
  (fn [db _]
    (if (empty? (get-in db [:doctors]))
      (assoc db :doctors [{:name "马志明" :username "mazhiming" :role "麻醉医生" :signature nil}]
             ;; Add more mock doctors if needed or ensure this is loaded from backend
             )
      db)))

(rf/reg-event-db ::set-active-tab
  (fn [db [_ tab]]
    (assoc-in db [:anesthesia :active-tab] tab))) ; Ensure path is correct

(rf/reg-event-db ::open-doctor-modal
  (fn [db [_ doctor-data]]
    (assoc db
           :doctor-modal-visible? true
           :editing-doctor (or doctor-data {}))))

(rf/reg-event-db ::close-doctor-modal
  (fn [db _]
    (assoc db
           :doctor-modal-visible? false
           :editing-doctor nil)))

(rf/reg-event-db ::update-editing-doctor-field
  (fn [db [_ field value]]
    (assoc-in db [:editing-doctor field] value)))

(rf/reg-event-db ::save-doctor
  (fn [db [_ form-values]]
    (let [editing-doctor (get db :editing-doctor {})
          doctors (get db :doctors [])
          username-to-save (:username form-values)]
      (if (or (empty? (:username editing-doctor)) (= (:username editing-doctor) username-to-save)) ; New or same username
        (let [existing-doctor-idx (if-not (empty? (:username editing-doctor))
                                    (.findIndex doctors #(= (:username %) (:username editing-doctor))))]
          (if (not= -1 existing-doctor-idx)
            (assoc-in db [:doctors existing-doctor-idx] form-values) ; Update existing
            (update db :doctors conj form-values))) ; Add new
        (timbre/error "Cannot change username during edit yet.") ; Simple prevention for now
        )
      (assoc db :doctor-modal-visible? false :editing-doctor nil))))

(rf/reg-event-db ::delete-doctor
  (fn [db [_ username]]
    (update db :doctors (fn [doctors] (vec (remove #(= username (:username %)) doctors))))))
