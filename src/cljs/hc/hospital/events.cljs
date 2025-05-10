(ns hc.hospital.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [hc.hospital.db :as app-db]
            [ajax.core :as ajax]
            [taoensso.timbre :as timbre]))

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
(rf/reg-event-db
 ::update-brief-medical-history
 (fn [db [_ form-values]]
   (assoc-in db [:anesthesia :assessment :brief-medical-history] form-values)))

(rf/reg-event-db
 ::update-physical-examination
 (fn [db [_ form-values]]
   (assoc-in db [:anesthesia :assessment :physical-examination] form-values)))

(rf/reg-event-db
 ::update-lab-tests
 (fn [db [_ form-values]]
   (assoc-in db [:anesthesia :assessment :lab-tests] form-values)))

;; --- Patient List Filters and Actions ---
(rf/reg-event-db
 ::set-active-tab
 (fn [db [_ tab]]
   (assoc-in db [:anesthesia :active-tab] tab)))

(rf/reg-event-db
 ::update-search-term
 (fn [db [_ term]]
   (assoc-in db [:anesthesia :search-term] term)))

(rf/reg-event-db
 ::set-date-range
 (fn [db [_ range-values]]
   ;; range-values 是 Ant Design RangePicker 返回的 [moment, moment] 数组或 nil
   (assoc-in db [:anesthesia :date-range] range-values)))

(rf/reg-event-db
 ::set-assessment-status-filter
 (fn [db [_ status]]
   (assoc-in db [:anesthesia :assessment-status-filter] status)))

(rf/reg-event-fx
 ::sync-applications
 (fn [_ _]
   (js/alert "正在从HIS系统同步患者列表数据...")
   {})) ; no db change, just side effect

(rf/reg-event-fx
 ::scan-check-in
 (fn [_ _]
   (js/alert "请将患者二维码对准扫描设备")
   {}))

;; 这个事件由 input-search 的 onSearch 触发，可以与 update-search-term 合并
;; 或者用于显式的API搜索（如果后端支持）
(rf/reg-event-db
 ::search-patients
 (fn [db [_ term]]
   (timbre/info "显式搜索患者:" term)
   (assoc-in db [:anesthesia :search-term] term))) ; 简单地更新搜索词

;; --- Assessment Actions ---
(rf/reg-event-fx ::approve-patient (fn [{:keys [db]} _] (js/alert (str "批准患者: " (get-in db [:anesthesia :current-patient-id]))) {}))
(rf/reg-event-fx ::postpone-patient (fn [{:keys [db]} _] (js/alert (str "暂缓患者: " (get-in db [:anesthesia :current-patient-id]))) {}))
(rf/reg-event-fx ::reject-patient (fn [{:keys [db]} _] (js/alert (str "驳回患者: " (get-in db [:anesthesia :current-patient-id]))) {}))
