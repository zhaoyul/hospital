(ns hc.hospital.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [hc.hospital.db :as app-db]
            [ajax.core :as ajax]
            [taoensso.timbre :as timbre]))

;; -- Initialization
(rf/reg-event-db ::initialize-db
  (fn [_ _]
    app-db/default-db))

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
    (let [selected-assessment (first (filter #(= (:patient_id %) patient-key) (:all-patient-assessments db)))
          form-data-from-api (when selected-assessment (get selected-assessment :assessment_form_data {}))
          default-form-data (get-in app-db/default-db [:anesthesia :assessment :form-data])]
      (-> db
          (assoc-in [:anesthesia :current-patient-id] patient-key)
          (assoc-in [:anesthesia :assessment :form-data]
                    (if (seq form-data-from-api) ; Check if API data is not empty
                      form-data-from-api
                      default-form-data))))))


;; --- Updating Assessment Data from Doctor's Forms ---
;; Generic updater for the new form structure within :form-data
(rf/reg-event-db ::update-medical-summary-field
  (fn [db [_ field-path new-value]]
    ;; field-path is a vector, e.g., [:allergy :has] or [:comorbidities :respiratory :details]
    (assoc-in db (concat [:anesthesia :assessment :form-data] field-path) new-value)))

;; Specific handlers for auxiliary exam file list management
(rf/reg-event-db ::handle-aux-exam-files-change
  (fn [db [_ exam-type file-list-info]]
    ;; file-list-info is the {file: ..., fileList: ...} object from Ant Design's onChange
    (assoc-in db [:anesthesia :assessment :form-data :aux-exams exam-type] (:fileList file-list-info))))

(rf/reg-event-db ::remove-aux-exam-file
  (fn [db [_ exam-type file-uid]]
    (update-in db [:anesthesia :assessment :form-data :aux-exams exam-type]
               (fn [files] (vec (remove #(= (:uid %) file-uid) files))))))

;; This event updates notes within :anesthesia-plan, which is separate from :form-data
(rf/reg-event-db ::update-assessment-notes
  (fn [db [_ notes-text]]
    (assoc-in db [:anesthesia :assessment :anesthesia-plan :notes] notes-text)))


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
(rf/reg-event-fx ::save-final-assessment
  (fn [{:keys [db]} _]
    (let [current-patient-id (get-in db [:anesthesia :current-patient-id])
          assessment-payload {;; Data from the four new cards
                              :form_data (get-in db [:anesthesia :assessment :form-data])
                              ;; Data from the separate anesthesia plan section
                              :anesthesia_plan (get-in db [:anesthesia :assessment :anesthesia-plan])}]
      (if current-patient-id
        {:http-xhrio {:method          :put ; Or :post if creating a new assessment
                      :uri             (str "/api/patient/assessments/" current-patient-id)
                      :params          assessment-payload
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success      [::save-assessment-success]
                      :on-failure      [::save-assessment-failed]}}
        (do (timbre/warn "No patient selected, cannot save assessment.")
            {})))))

(rf/reg-event-db ::save-assessment-success
  (fn [db [_ response]]
    (timbre/info "评估保存成功:" response)
    (rf/dispatch [::fetch-all-assessments]) ; Refresh list after saving
    db))

(rf/reg-event-db ::save-assessment-failed
  (fn [db [_ error]]
    (timbre/error "评估保存失败:" error)
    (js/alert (str "保存失败: " (pr-str error)))
    db))


;; This event seems to be for editing basic patient info, not the detailed assessment form.
;; If it's still used for a separate patient editing modal/form, it can remain.
;; Otherwise, if all patient data is part of the main assessment form, it might be redundant.
(rf/reg-event-db ::update-patient-form-field
  (fn [db [_ field-path new-value]]
    (assoc-in db [:editing-patient-info field-path] new-value)))

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
    (when-not (or (= 401 (:status error-details)) (= 404 (:status error-details)))
      (timbre/error "Unexpected error during session check:" error-details))
    (js/window.location.assign "/login.html") ; Redirect to login
    {:dispatch [::clear-current-doctor]})) ; clear-current-doctor now also sets session-check-pending? to false


;; --- Login Events ---
(rf/reg-event-db ::set-current-doctor
  (fn [db [_ doctor-data]]
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
    (timbre/info "Login successful:" response-data)
    ;; Assuming the server returns {:message "登录成功" :doctor {...}}
    ;; If navigation is handled by a specific re-frame fx, replace js/window.location.href
    (js/window.location.assign "/") ;; More standard way to navigate
    ;; ::set-current-doctor will handle setting session-check-pending to false
    {:dispatch [::set-current-doctor (:doctor response-data)]
     ;; :dispatch-later [{:ms 100 :dispatch [::navigate "/"]}] ; Example if navigation needs delay or is an effect
     }))

;; --- Logout Events ---
(rf/reg-event-db ::clear-current-doctor
  (fn [db _]
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
    (js/window.location.assign "/login.html") ; Redirect to login.html page
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
