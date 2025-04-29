(ns hc.hospital.events
  (:require
   [re-frame.core :as rf]
   [hc.hospital.db :as db]))

;; 初始化应用状态
(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

;; 切换主标签页
(rf/reg-event-db
 ::set-active-tab
 (fn [db [_ tab-key]]
   (assoc-in db [:anesthesia :active-tab] tab-key)))

;; 切换评估标签页
(rf/reg-event-db
 ::set-assessment-tab
 (fn [db [_ tab-key]]
   (assoc-in db [:anesthesia :ui :active-assessment-tab] tab-key)))

;; 设置当前评估标签页
(rf/reg-event-db
 ::set-active-assessment-tab
 (fn [db [_ tab-key]]
   (assoc-in db [:anesthesia :ui :active-assessment-tab] tab-key)))

;; 选择患者
(rf/reg-event-db
 ::select-patient
 (fn [db [_ patient-id]]
   (assoc-in db [:anesthesia :current-patient-id] patient-id)))

;; 搜索患者
(rf/reg-event-db
 ::set-search-term
 (fn [db [_ search-term]]
   (assoc-in db [:anesthesia :search-term] search-term)))

;; 更新搜索词
(rf/reg-event-db
 ::update-search-term
 (fn [db [_ search-term]]
   (assoc-in db [:anesthesia :search-term] search-term)))

;; 执行患者搜索
(rf/reg-event-db
 ::search-patients
 (fn [db [_ search-term]]
   (-> db
       (assoc-in [:anesthesia :search-term] search-term)
       (assoc-in [:anesthesia :ui :is-searching] true))))

;; 设置日期范围
(rf/reg-event-db
 ::set-date-range
 (fn [db [_ date-range]]
   (assoc-in db [:anesthesia :date-range] date-range)))

;; 更新简要病史中的选项
(rf/reg-event-db
 ::update-medical-history-option
 (fn [db [_ field value description]]
   (-> db
       (assoc-in [:anesthesia :assessment :brief-medical-history field :value] value)
       (assoc-in [:anesthesia :assessment :brief-medical-history field :description] description))))

;; 更新个人史
(rf/reg-event-db
 ::update-personal-history
 (fn [db [_ values]]
   (assoc-in db [:anesthesia :assessment :brief-medical-history :personal-history] values)))

;; 更新其他描述
(rf/reg-event-db
 ::update-other-description
 (fn [db [_ field value]]
   (assoc-in db [:anesthesia :assessment :brief-medical-history field] value)))

;; 更新体格检查数据
(rf/reg-event-db
 ::update-physical-exam
 (fn [db [_ field value]]
   (assoc-in db [:anesthesia :assessment :physical-examination field] value)))

;; 更新血压
(rf/reg-event-db
 ::update-blood-pressure
 (fn [db [_ systolic diastolic]]
   (-> db
       (assoc-in [:anesthesia :assessment :physical-examination :bp :systolic] systolic)
       (assoc-in [:anesthesia :assessment :physical-examination :bp :diastolic] diastolic))))

;; 更新相关病史
(rf/reg-event-db
 ::update-related-history
 (fn [db [_ field value]]
   (assoc-in db [:anesthesia :assessment :physical-examination :related-history field] value)))

;; 更新实验室检查数据
(rf/reg-event-db
 ::update-lab-test
 (fn [db [_ field value]]
   (assoc-in db [:anesthesia :assessment :lab-tests field] value)))

;; 批准患者
(rf/reg-event-fx
 ::approve-patient
 (fn [{:keys [db]} _]
   (let [current-id (get-in db [:anesthesia :current-patient-id])]
     (if current-id
       {:db (assoc-in db [:anesthesia :patients] 
                    (map #(if (= (:key %) current-id) 
                            (assoc % :status "已批准") 
                            %) 
                         (get-in db [:anesthesia :patients])))
        :dispatch [:show-message {:type "success" :content "患者已批准"}]}
       {:dispatch [:show-message {:type "warning" :content "请先选择一名患者"}]}))))

;; 暂缓患者
(rf/reg-event-fx
 ::postpone-patient
 (fn [{:keys [db]} _]
   (let [current-id (get-in db [:anesthesia :current-patient-id])]
     (if current-id
       {:db (assoc-in db [:anesthesia :patients] 
                    (map #(if (= (:key %) current-id) 
                            (assoc % :status "已暂缓") 
                            %) 
                         (get-in db [:anesthesia :patients])))
        :dispatch [:show-message {:type "info" :content "患者已暂缓"}]}
       {:dispatch [:show-message {:type "warning" :content "请先选择一名患者"}]}))))

;; 驳回患者
(rf/reg-event-fx
 ::reject-patient
 (fn [{:keys [db]} _]
   (let [current-id (get-in db [:anesthesia :current-patient-id])]
     (if current-id
       {:db (assoc-in db [:anesthesia :patients] 
                    (map #(if (= (:key %) current-id) 
                            (assoc % :status "已驳回") 
                            %) 
                         (get-in db [:anesthesia :patients])))
        :dispatch [:show-message {:type "error" :content "患者已驳回"}]}
       {:dispatch [:show-message {:type "warning" :content "请先选择一名患者"}]}))))