(ns hc.hospital.patient.events
  (:require
   [re-frame.core :as rf]
   [hc.hospital.patient.db :as db] ;; Ensure this refers to your updated db.cljs
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [clojure.string :as str]
   [taoensso.timbre :as timbre]))

;; 总步骤数 (0-indexed, so 3 steps means max index is 2)
(def patient-form-total-steps 3)
(def patient-form-max-step-idx (dec patient-form-total-steps))

;; 初始化数据库
(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db)) ;; Use the new default_db

;; 更新表单字段
(rf/reg-event-db
 ::update-form-field
 (fn [db [_ path value]]
   (assoc-in db (into [:patient-form] path) value)))

;; 下一步
(rf/reg-event-db
 ::next-step
 (fn [db _]
   (let [current-step (get-in db [:patient-form :current-step])]
     (if (< current-step patient-form-max-step-idx)
       (update-in db [:patient-form :current-step] inc)
       db))))

;; 上一步
(rf/reg-event-db
 ::prev-step
 (fn [db _]
   (let [current-step (get-in db [:patient-form :current-step])]
     (if (> current-step 0)
       (update-in db [:patient-form :current-step] dec)
       db))))

;; 跳转到指定步骤
(rf/reg-event-db
 ::goto-step
 (fn [db [_ step]]
   (if (and (>= step 0) (< step patient-form-total-steps))
     (assoc-in db [:patient-form :current-step] step)
     db)))

;; 验证并提交表单
(rf/reg-event-fx
 ::validate-and-submit
 (fn [{:keys [db]} _]
   (let [form-data (get-in db [:patient-form])
         basic-info (:basic-info form-data)
         ;; medical-summary (:medical-summary form-data) ; Example: access other sections
         ;; comorbidities (:comorbidities form-data)
         ;; physical-examination (:physical-examination form-data)
         ;; auxiliary-examination (:auxiliary-examination form-data)
         errors (transient {})]

     ;; --- 示例验证逻辑 ---
     ;; 您需要根据实际需求扩展此处的验证
     (when (str/blank? (:name basic-info))
       (assoc! errors [:basic-info :name] "姓名不能为空"))
     (when (str/blank? (:outpatient-number basic-info))
       (assoc! errors [:basic-info :outpatient-number] "门诊号不能为空"))
     (when (nil? (:age basic-info))
       (assoc! errors [:basic-info :age] "年龄不能为空"))
     (when (nil? (:gender basic-info))
       (assoc! errors [:basic-info :gender] "性别不能为空"))
     ;; ... 在此添加更多针对 medical-summary, comorbidities 等的验证规则

     (let [final-errors (persistent! errors)]
       (if (empty? final-errors)
         {:db (-> db
                  (assoc-in [:patient-form :submitting?] true)
                  (assoc-in [:patient-form :form-errors] {})) ; 清除旧错误
          :dispatch [::submit-form]}
         {:db (assoc-in db [:patient-form :form-errors] final-errors)})))))

;; 提交表单 (Effect)
(rf/reg-event-fx
 ::submit-form
 (fn [{:keys [db]} _]
   (let [form-data-to-submit (get-in db [:patient-form])]
     ;; 从 form-data-to-submit 中移除 :form-errors, :submitting?, :submit-success?, :submit-error, :current-step
     ;; 以免将它们发送到后端。后端通常只需要实际的表单数据。
     {:http-xhrio {:method          :post
                   :uri             "/api/patient/assessment"
                   :params          (dissoc form-data-to-submit
                                           :form-errors :submitting? :submit-success? :submit-error :current-step)
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [::submit-success]
                   :on-failure      [::submit-failure]}})))

;; 提交成功
(rf/reg-event-fx ; Changed from reg-event-db
 ::submit-success
 (fn [{:keys [db]} [_ response]]
   (timbre/debug "表单提交成功:" response)
   {:db (-> db
            (assoc-in [:patient-form :submitting?] false)
            (assoc-in [:patient-form :submit-success?] true))
    :dispatch-later [{:ms 3000 :dispatch [::reset-patient-form-success]}]}))

;; 提交失败
(rf/reg-event-db
 ::submit-failure
 (fn [db [_ response]]
   (timbre/error "表单提交失败:" response)
   (-> db
       (assoc-in [:patient-form :submitting?] false)
       (assoc-in [:patient-form :submit-success?] false)
       (assoc-in [:patient-form :submit-error] (or (:message (:response response)) "提交失败，请稍后再试")))))

;; 新增事件：重置表单提交成功状态
(rf/reg-event-db
 ::reset-patient-form-success
 (fn [db _]
   (assoc-in db [:patient-form :submit-success?] false)))

;; 如果 hc.hospital.patient.events/toggle-boolean-field 仍在使用，它应该可以正常工作
(rf/reg-event-db
  ::toggle-boolean-field
  (fn [db [_ path]]
    (let [full-path (into [:patient-form] path)
          current-value (get-in db full-path)]
      (assoc-in db full-path (not current-value)))))

;; 如果 hc.hospital.patient.events/set-assessment-date-today 仍需要，
;; 它需要一个在当前表单结构中有效的目标路径。
;; 当前 patient-form 视图未包含评估日期字段。
;; (rf/reg-event-db
;;  ::set-assessment-date-today
;;  (fn [db _]
;;    (let [today (.toISOString (js/Date.))]
;;      ;; 修改路径以匹配新的DB结构 (如果需要此功能)
;;      (assoc-in db [:patient-form :some-new-date-field] (subs today 0 10)))))
