(ns hc.hospital.events
  "事件处理逻辑，负责与后端通信并更新应用状态。"
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [hc.hospital.db :as app-db]
            [ajax.core :as ajax]
            [clojure.string :as str]
            [hc.hospital.utils :as utils]
            [hc.hospital.router :as router]
            [taoensso.timbre :as timbre]))

;; 默认的规范评估数据结构
(def default-canonical-assessment
  "系统初始化时使用的评估模板。"
  {:基本信息 {:门诊号 nil, :姓名 nil, :身份证号 nil, :手机号 nil, :性别 nil,
            :年龄 nil, :院区 nil, :患者提交时间 nil, :评估更新时间 nil,
            :评估状态 "待评估", :医生姓名 nil, :评估备注 nil, :身高cm nil,
            :体重kg nil, :精神状态 nil, :活动能力 nil, :血压mmHg nil,
            :脉搏次每分 nil, :呼吸次每分 nil, :体温摄氏度 nil, :SpO2百分比 nil,
            :术前诊断 nil, :拟施手术 nil}
   :循环系统 {}
   :呼吸系统 {}
   :精神及神经肌肉系统 {}
   :内分泌系统 {}
   :肝肾病史 {}
   :消化系统 {}
   :血液系统 {}
   :免疫系统 {}
   :特殊用药史 {}
   :特殊疾病病史 {}
   :营养评估 {}
   :妊娠 {}
   :手术麻醉史 {}
   :气道评估 {}
   :椎管内麻醉相关评估 {}
   :辅助检查集 []
   :辅助检查备注 nil
   :麻醉评估与医嘱 {:ASA分级 nil, :心功能分级NYHA nil, :拟行麻醉方式 nil,
                :监测项目 nil, :特殊技术 nil, :其他麻醉相关 nil,
                :术前麻醉医嘱 nil, :术日晨继续应用药物 nil, :术日晨停用药物 nil,
                :麻醉前用药 nil, :麻醉中需注意的问题 nil}
   ;; 根据 患者评估数据Spec 添加可能需要的顶层key，如果它们需要在UI加载时有默认结构
   ;; :审批状态 {} ; 根据需要取消注释并提供默认结构
   ;; :知情同意书 {} ; 根据需要取消注释并提供默认结构
   })


;; -- Initialization
(rf/reg-event-db ::initialize-db
  (fn [_ _]
    (-> app-db/default-db
        (assoc :anesthesia {:current-assessment-canonical default-canonical-assessment
                            :current-assessment-id nil
                            :date-range [(utils/now) (utils/now)]
                            :active-tab "overview"})
        ;; 初始化二维码扫描模态框相关状态
        (assoc :qr-scan-modal-visible? false)
        (assoc :qr-scan-input-value ""))))


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
        (assoc-in [:anesthesia :current-assessment-id] nil)
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
          (assoc-in [:anesthesia :current-assessment-id] nil)
          (assoc-in [:anesthesia :current-assessment-canonical] default-canonical-assessment))
      (let [all-assessments (get-in db [:anesthesia :all-patient-assessments])
            selected-full-assessment (first (filter #(= (:patient_id %) patient-key) all-assessments))
            canonical-assessment-data (when selected-full-assessment
                                        (get selected-full-assessment :assessment_data {}))]
        (-> db
            (assoc-in [:anesthesia :current-patient-id] patient-key)
            (assoc-in [:anesthesia :current-assessment-id] (:id selected-full-assessment))
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
    (timbre/info "::update-canonical-assessment-section: Event triggered. Section-key:" section-key ", Incoming section-data:" (clj->js section-data))

    (let [path [:anesthesia :current-assessment-canonical section-key]
          current-data-before-merge (get-in db path)]
      (timbre/info "::update-canonical-assessment-section: Data for section" section-key "BEFORE merge:" (clj->js current-data-before-merge))

      (let [updated-db (if (get-in db [:anesthesia :current-assessment-canonical section-key])
                         (update-in db path merge section-data)
                         (update-in db path (fnil merge {}) section-data))]
        (timbre/info "::update-canonical-assessment-section: Data for section" section-key "AFTER merge:" (clj->js (get-in updated-db path)))
        updated-db))))

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

;; 事件：更新签名数据到 app-db
(rf/reg-event-db
 ::update-signature-data
 (fn [db [_ signature-data]]
   (timbre/info "Updating signature data in app-db:" signature-data)
   (assoc-in db [:anesthesia :current-assessment-canonical :基本信息 :医生签名图片] signature-data)))

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

(rf/reg-event-db ::search-patients
  (fn [db [_ term]]
    (timbre/info "显式搜索患者:" term)
    (assoc-in db [:anesthesia :search-term] term)))

;; --- Assessment Actions ---
(rf/reg-event-fx ::approve-patient
  (fn [{:keys [db]} _]
    (let [patient-id (get-in db [:anesthesia :current-patient-id])]
      (if patient-id
        {:fx [[:dispatch [::update-canonical-assessment-field [:基本信息 :评估状态] "已批准"]]
              [:dispatch [::save-final-assessment]]]}
        (do (timbre/warn "No patient selected to approve.")
            {})))))

(rf/reg-event-fx ::postpone-patient
  (fn [{:keys [db]} _]
    (let [patient-id (get-in db [:anesthesia :current-patient-id])]
      (if patient-id
        {:fx [[:dispatch [::update-canonical-assessment-field [:基本信息 :评估状态] "暂缓"]]
              [:dispatch [::save-final-assessment]]]}
        (do (timbre/warn "No patient selected to postpone.")
            {})))))

(rf/reg-event-fx ::reject-patient
  (fn [{:keys [db]} _]
    (let [patient-id (get-in db [:anesthesia :current-patient-id])]
      (if patient-id
        {:fx [[:dispatch [::update-canonical-assessment-field [:基本信息 :评估状态] "驳回"]]
              [:dispatch [::save-final-assessment]]]}
        (do (timbre/warn "No patient selected to reject.")
            {})))))

;; --- Save Final Assessment ---
(rf/reg-event-fx ::save-final-assessment-later
  (fn [_ _]
    {:dispatch-later [{:ms 30 :dispatch [::save-final-assessment]}]}))

(rf/reg-event-fx ::save-final-assessment
  [(when ^boolean goog.DEBUG re-frame.core/debug)]
  (fn [{:keys [db]} _]
    (let [current-patient-id (get-in db [:anesthesia :current-patient-id])
          ;; The entire canonical assessment is now the payload
          assessment-payload (get-in db [:anesthesia :current-assessment-canonical])
          signature-present (not (str/blank? (get-in assessment-payload [:基本信息 :医生签名图片] "")))]
      (timbre/info "Saving final assessment for patient-id:" current-patient-id "Signature present?:" signature-present)
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
  [(when ^boolean goog.DEBUG re-frame.core/debug)]
  (fn [{:keys [db]} _]
    {:http-xhrio {:method          :post
                  :uri             "/api/users/logout"
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::logout-finished]
                  :on-failure      [::logout-finished]} ; Always proceed to clear client session
     }))

(rf/reg-event-fx ::logout-finished
  (fn [{:keys [db]} [_ response-data]]
    (timbre/info "Logout processed. Response (if any):" response-data)
    (js/window.location.assign "/login") ; Redirect to /login page
    {:dispatch [::clear-current-doctor]}))


(rf/reg-event-fx ::initialize-users
  (fn [_ _]
    {:dispatch [::fetch-users]}))

(rf/reg-event-fx ::fetch-users
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/users"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::set-users]
                  :on-failure [::fetch-users-failed]}}))

(rf/reg-event-db ::set-users
  (fn [db [_ {:keys [doctors]}]]
    (assoc db :users doctors)))

(rf/reg-event-db ::fetch-users-failed
  (fn [db [_ error]]
    (timbre/error "获取医生列表失败" error)
    db))

(rf/reg-event-db ::set-active-tab
  (fn [db [_ tab]]
    (assoc-in db [:anesthesia :active-tab] tab))) ; Ensure path is correct

(rf/reg-fx
  ::navigate-tab
  (fn [tab]
    (router/navigate! tab)))

(rf/reg-event-fx ::navigate-tab
  (fn [{:keys [db]} [_ tab]]
    {::navigate-tab tab
     :db (assoc-in db [:anesthesia :active-tab] tab)}))

(rf/reg-event-db ::open-user-modal
  (fn [db [_ user-data]]
    (assoc db
           :user-modal-visible? true
           :editing-user (or user-data {}))))

(rf/reg-event-db ::close-user-modal
  (fn [db _]
    (assoc db
           :user-modal-visible? false
           :editing-user nil)))

(rf/reg-event-db ::update-editing-user-field
  (fn [db [_ field value]]
    (assoc-in db [:editing-user field] value)))

(rf/reg-event-fx ::save-user
  (fn [{:keys [db]} [_ form-values]]
    (let [editing-user (get db :editing-user {})
          id (:id editing-user)
          payload (assoc form-values :signature_b64 (:signature form-values))]
      (if id
        {:http-xhrio {:method :put
                      :uri (str "/api/user/" id)
                      :params payload
                      :format (ajax/json-request-format)
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [::save-user-success]
                      :on-failure [::save-user-failed]}}
        {:http-xhrio {:method :post
                      :uri "/api/users"
                      :params (assoc payload :password "defaultpass")
                      :format (ajax/json-request-format)
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [::save-user-success]
                      :on-failure [::save-user-failed]}}))))

(rf/reg-event-fx ::save-user-success
  (fn [{:keys [db]} _]
    {:dispatch-n [[::close-user-modal]
                  [::fetch-users]]}))

(rf/reg-event-db ::save-user-failed
  (fn [db [_ error]]
    (timbre/error "保存医生失败" error)
    db))

(rf/reg-event-fx ::delete-user
  (fn [{:keys [db]} [_ id]]
    {:http-xhrio {:method :delete
                  :uri (str "/api/user/" id)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::delete-user-success]
                  :on-failure [::delete-user-failed]}}))

(rf/reg-event-fx ::delete-user-success
  (fn [_ _]
    {:dispatch [::fetch-users]}))

(rf/reg-event-db ::delete-user-failed
  (fn [db [_ error]]
    (timbre/error "删除医生失败" error)
    db))

;; ---- 角色管理事件 ----
(rf/reg-event-fx ::initialize-roles
  (fn [_ _]
    {:dispatch [::fetch-roles]}))

(rf/reg-event-fx ::fetch-roles
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/roles"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::set-roles]}}))

(rf/reg-event-db ::set-roles
  (fn [db [_ {:keys [roles]}]]
    (assoc db :roles roles)))

(rf/reg-event-db ::open-role-modal
  (fn [db [_ role]]
    (assoc db :role-modal-visible? true :editing-role role)))

(rf/reg-event-db ::close-role-modal
  (fn [db _]
    (assoc db :role-modal-visible? false :editing-role nil)))

(rf/reg-event-fx ::fetch-role-permissions
  (fn [_ [_ role-id]]
    {:http-xhrio {:method :get
                  :uri (str "/api/roles/" role-id "/permissions")
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::set-editing-role-perms]}}))

(rf/reg-event-db ::set-editing-role-perms
  (fn [db [_ {:keys [permissions]}]]
    (assoc-in db [:editing-role :permissions] (mapv :id permissions))))

(rf/reg-event-fx ::save-role-permissions
  (fn [_ [_ role-id perm-ids]]
    {:http-xhrio {:method :put
                  :uri (str "/api/roles/" role-id "/permissions")
                  :params {:permission_ids perm-ids}
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::close-role-modal]}}))


;; --- 二维码扫描模态框事件 ---
;; 打开二维码扫描模态框
(rf/reg-event-db ::open-qr-scan-modal
  (fn [db _]
    (assoc db :qr-scan-modal-visible? true)))

;; 关闭二维码扫描模态框
(rf/reg-event-db ::close-qr-scan-modal
  (fn [db _]
    (-> db
        (assoc :qr-scan-modal-visible? false)
        (assoc :qr-scan-input-value "")))) ; 关闭时清空输入值

;; 设置二维码扫描输入框的值
(rf/reg-event-db ::set-qr-scan-input
  (fn [db [_ value]]
    (assoc db :qr-scan-input-value value)))

;; 通过患者ID（从二维码扫描获得）查询HIS系统中的患者信息
(rf/reg-event-fx ::find-patient-by-id-in-his
  (fn [{:keys [db]} [_ patient-id-input]]
    (if (str/blank? patient-id-input)
      (do
        (timbre/warn "HIS患者查询：输入的ID为空。")
        ;; 输入为空时也关闭模态框并清空输入值
        {:dispatch [::close-qr-scan-modal]})
      {:http-xhrio {:method          :get
                    :uri             (str "/api/patient/find-by-id/" patient-id-input)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::find-patient-in-his-success patient-id-input] ; 传递原始输入ID作为上下文
                    :on-failure      [::find-patient-in-his-failure patient-id-input]}})))

;; HIS患者查询成功事件
(rf/reg-event-db ::find-patient-in-his-success
  (fn [db [_ patient-id-input api-response]] ; patient-id-input is original, api-response is from server
    (timbre/info "成功从HIS查询/创建患者 " patient-id-input ". API响应:" api-response)
    (let [new-assessment-record (:assessment api-response)]
      (if new-assessment-record
        (let [current-assessments (get-in db [:anesthesia :all-patient-assessments] [])
              ;; API返回的 :patient_id 通常就是 patient-id-input，但使用记录中的ID更准确
              record-patient-id (:patient_id new-assessment-record)
              patient-exists? (some #(= (:patient_id %) record-patient-id) current-assessments)
              updated-assessments (if patient-exists?
                                    (mapv #(if (= (:patient_id %) record-patient-id)
                                             new-assessment-record ; 更新已存在的记录
                                             %)
                                          current-assessments)
                                    (conj current-assessments new-assessment-record))] ; 添加新记录
          ;; 使用 new-assessment-record 中的姓名进行提示
          (js/alert (str "成功处理患者信息：" (get-in new-assessment-record [:assessment_data :基本信息 :姓名])))
          (rf/dispatch [::close-qr-scan-modal])
          (rf/dispatch [::select-patient record-patient-id]) ; 使用记录中的ID确保选中正确的患者
          (assoc-in db [:anesthesia :all-patient-assessments] updated-assessments))
        (do
          (timbre/warn "从API获取的患者评估数据 (:assessment) 为空，无法更新UI列表。Patient ID:" patient-id-input ", Response:" api-response)
          ;; 此处不关闭模态框，允许用户看到错误或取消
          ;; 显示一个更通用的错误消息，因为具体原因可能在日志中
          (js/alert (str "处理患者 " patient-id-input " 信息时发生错误，未能获取完整的评估数据。详情请查看日志。"))
          db)))))

;; HIS患者查询失败事件
(rf/reg-event-db ::find-patient-in-his-failure
  (fn [db [_ patient-id-input error]]
    (let [error-message (or (-> error :response :message) (:status-text error) "查询失败")]
      (timbre/error "HIS患者查询失败 " patient-id-input ":" error)
      (js/alert (str "查询患者 " patient-id-input " 失败: " error-message))
      ;; 查询失败时不关闭模态框，允许用户重试或取消
      db)))
