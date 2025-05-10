(ns hc.hospital.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [hc.hospital.db :as app-db]
            [ajax.core :as ajax]
            [taoensso.timbre :as timbre])) ; Assuming you have a db.cljs for initial app state

;; -- Initialization
(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   app-db/default-db))

;; --- Fetching All Assessments ---
(rf/reg-event-fx
 ::fetch-all-assessments
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/api/patient/assessments" ; 假设这是获取所有评估的API端点
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::set-all-assessments]
                 :on-failure      [::fetch-all-assessments-failed]}}))

(rf/reg-event-db
 ::set-all-assessments
 (fn [db [_ assessments]]
   (-> db
       (assoc :all-patient-assessments assessments)
       (assoc :current-patient-id nil) ; 清除当前选中的患者
       (assoc :fetch-assessments-error nil))))

(rf/reg-event-db
 ::fetch-all-assessments-failed
 (fn [db [_ error]]
   (timbre/error "获取所有评估失败:" error)
   (assoc db :fetch-assessments-error error)))

;; --- Patient Selection ---
(rf/reg-event-db
 ::select-patient
 (fn [db [_ patient-key]] ; patient-key 应该是列表项的 :key
   (let [all-pg-assessments (get db :all-patient-assessments [])
         example-patients (get-in db [:anesthesia :patients] [])

         ;; 尝试从 API 数据中查找
         selected-api-patient (first (filter #(= (:patient_id %) patient-key) all-pg-assessments))

         ;; 如果 API 数据中没有，则尝试从示例数据中查找（基于 :key）
         selected-example-patient (when-not selected-api-patient
                                    (first (filter #(= (:key %) patient-key) example-patients)))

         raw-assessment-data (cond
                               selected-api-patient (:assessment_data selected-api-patient)
                               selected-example-patient (get-in app-db/default-db [:anesthesia :assessment]) ; 使用默认结构填充示例
                               :else {})

         default-form-structure (get-in app-db/default-db [:anesthesia :assessment])
         final-assessment-data {:brief-medical-history (merge (:brief-medical-history default-form-structure)
                                                              (:brief-medical-history raw-assessment-data {}))
                                :physical-examination (merge (:physical-examination default-form-structure)
                                                             (:physical-examination raw-assessment-data {}))
                                :lab-tests (merge (:lab-tests default-form-structure)
                                                  (:lab-tests raw-assessment-data {}))
                                :anesthesia-plan (merge (:anesthesia-plan default-form-structure)
                                                        (:anesthesia-plan raw-assessment-data {}))}]
     (-> db
         (assoc-in [:anesthesia :current-patient-id] patient-key) ; 更新 current-patient-id
         (assoc-in [:anesthesia :assessment] final-assessment-data)))))


;; --- Updating Assessment Data from Doctor's Forms ---
(rf/reg-event-db ::update-brief-medical-history
  (fn [db [_ form-values]]
    (assoc-in db [:anesthesia :assessment :brief-medical-history] form-values)))

(rf/reg-event-db ::update-physical-examination
  (fn [db [_ form-values]]
    (assoc-in db [:anesthesia :assessment :physical-examination] form-values)))

(rf/reg-event-db ::update-lab-tests
  (fn [db [_ form-values]]
    (assoc-in db [:anesthesia :assessment :lab-tests] form-values)))

(rf/reg-event-db ::update-assessment-notes
  (fn [db [_ notes-text]]
    (assoc-in db [:anesthesia :assessment :anesthesia-plan :notes] notes-text)))


(rf/reg-event-db ::update-search-term
  (fn [db [_ term]]
    (assoc-in db [:anesthesia :search-term] term)))

(rf/reg-event-db
 ::set-date-range
 (fn [db [_ range-values]]
   ;; range-values 是 Ant Design RangePicker 返回的 [moment, moment] 数组或 nil
   (assoc-in db [:anesthesia :date-range] range-values)))

(rf/reg-event-db ::set-assessment-status-filter
  (fn [db [_ status]]
    (assoc-in db [:anesthesia :assessment-status-filter] status)))

(rf/reg-event-fx ::sync-applications
  (fn [_ _]
    (js/alert "正在从HIS系统同步患者列表数据...")
    {})) ; no db change, just side effect

(rf/reg-event-fx ::scan-check-in
  (fn [_ _]
    (js/alert "请将患者二维码对准扫描设备")
    {}))

;; 这个事件由 input-search 的 onSearch 触发，可以与 update-search-term 合并
;; 或者用于显式的API搜索（如果后端支持）
(rf/reg-event-db ::search-patients
  (fn [db [_ term]]
    (timbre/info "显式搜索患者:" term)
    (assoc-in db [:anesthesia :search-term] term))) ; 简单地更新搜索词

;; --- Assessment Actions ---
(rf/reg-event-fx ::approve-patient (fn [{:keys [db]} _] (js/alert (str "批准患者: " (get-in db [:anesthesia :current-patient-id]))) {}))
(rf/reg-event-fx ::postpone-patient (fn [{:keys [db]} _] (js/alert (str "暂缓患者: " (get-in db [:anesthesia :current-patient-id]))) {}))
(rf/reg-event-fx ::reject-patient (fn [{:keys [db]} _] (js/alert (str "驳回患者: " (get-in db [:anesthesia :current-patient-id]))) {}))

;; --- Save Final Assessment ---
(rf/reg-event-fx
 ::save-final-assessment
 (fn [{:keys [db]} _]
   (let [current-patient-id (get-in db [:anesthesia :current-patient-id])
         assessment-data (get-in db [:anesthesia :assessment])]
     (if current-patient-id
       (do
         (timbre/info "保存评估结果 для пациента:" current-patient-id "Данные:" assessment-data)
         ;; 在实际应用中，这里会有一个 :http-xhrio 调用来将数据发送到后端
         ;; 例如:
         #_{:http-xhrio {:method          :put ; 或者 :post，取决于您的API
                         :uri             (str "/api/patient/assessment/" current-patient-id)
                         :params          assessment-data
                         :response-format (ajax/json-response-format {:keywords? true})
                         :on-success      [::save-assessment-success]
                         :on-failure      [::save-assessment-failed]}}
         (js/alert (str "评估结果已保存 (模拟) 患者ID: " current-patient-id))
         {:db db}) ; 暂时不修改db，实际成功后可能需要更新列表状态等
       (do
         (js/alert "没有选中的患者，无法保存。")
         {:db db})))))

(rf/reg-event-db
 ::save-assessment-success
 (fn [db [_ response]]
   (timbre/info "评估保存成功:" response)
   ;; 可能需要更新患者列表中的状态或重新获取数据
   (rf/dispatch [:fetch-all-assessments]) ; 例如，保存后刷新列表
   db))

(rf/reg-event-db
 ::save-assessment-failed
 (fn [db [_ error]]
   (timbre/error "评估保存失败:" error)
   (js/alert (str "保存失败: " (pr-str error)))
   db))


(rf/reg-event-fx
 ::select-patient-and-load-details
 (fn [{:keys [db]} [_ patient-id]]
   (let [patient-raw-details (get-in db [:all-patients-map patient-id])] ; 假设你有这个
     {:db (-> db
              (assoc :current-patient-id patient-id)
              ;; 初始化表单数据
              (assoc ::editing-patient-info (or (:basic-info patient-raw-details) patient-raw-details)))
      ;; :dispatch-later 或 :http-xhrio 如果需要从后端获取更多详情
      })))

(rf/reg-event-db
 ::update-patient-form-field
 (fn [db [_ field-path new-value]]
   ;; field-path 可以是一个 keyword 或者一个 vector (用于嵌套数据)
   (assoc-in db [:editing-patient-info field-path] new-value)))

;; Initialize with some mock doctor data or load from db
(rf/reg-event-db
 ::initialize-doctors
 (fn [db _]
   (if (empty? (get-in db [:doctors])) ; Only initialize if not already populated
     (assoc db :doctors [{:name "马志明" :username "mazhiming" :role "麻醉医生" :signature nil}
                         {:name "李主任" :username "lizhur" :role "主任" :signature nil}
                         {:name "王护士" :username "wanghs" :role "护士" :signature nil}])
     db)))

(rf/reg-event-db ::set-active-tab
                 (fn [db [_ tab]]
                   (assoc db :active-tab tab)))

(rf/reg-event-db
 ::open-doctor-modal
 (fn [db [_ doctor-data]]
   (assoc db
          :doctor-modal-visible? true
          :editing-doctor (or doctor-data {})))) ; Initialize with empty map if new doctor

(rf/reg-event-db
 ::close-doctor-modal
 (fn [db _]
   (assoc db
          :doctor-modal-visible? false
          :editing-doctor nil))) ; Clear editing doctor on close

(rf/reg-event-db
 ::update-editing-doctor-field
 (fn [db [_ field value]]
   (assoc-in db [:editing-doctor field] value)))

(rf/reg-event-db
 ::save-doctor
 (fn [db [_ form-values]]
   (let [editing-doctor (get db :editing-doctor {}) ; Default to empty map
         doctors (get db :doctors [])
         doctor-to-save (merge editing-doctor form-values)
         is-editing (and (:username editing-doctor)
                         (some #(= (:username editing-doctor) (:username %)) doctors))]
     (if is-editing
       (assoc db :doctors (mapv (fn [doc]
                                  (if (= (:username doc) (:username doctor-to-save))
                                    doctor-to-save
                                    doc))
                                doctors)
              :doctor-modal-visible? false ::editing-doctor nil)
       (-> db
           (assoc :doctors (conj doctors doctor-to-save))
           (assoc :doctor-modal-visible? false ::editing-doctor nil))))))


(rf/reg-event-db
 ::delete-doctor
 (fn [db [_ username]]
   (update db :doctors (fn [doctors] (vec (remove #(= username (:username %)) doctors))))))
