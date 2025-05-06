(ns hc.hospital.patient.events
  (:require
   [re-frame.core :as rf]
   [hc.hospital.patient.db :as db]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [clojure.string :as str]
   [taoensso.timbre :as timbre]))

;; 初始化数据库
(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

;; 更新表单字段
(rf/reg-event-db
 ::update-form-field
 (fn [db [_ path value]]
   (assoc-in db (into [:patient-form] path) value)))

;; 下一步
(rf/reg-event-db
 ::next-step
 (fn [db _]
   (let [current-step (get-in db [:patient-form :current-step])
         max-step 6] ;; 更新为6个步骤：基本信息、一般情况、病情摘要、并存疾病、检查结果、其他信息
     (if (< current-step max-step)
       (update-in db [:patient-form :current-step] inc)
       db))))

;; 上一步
(rf/reg-event-db
 ::prev-step
 (fn [db _]
   (let [current-step (get-in db [:patient-form :current-step])]
     (if (pos? current-step)
       (update-in db [:patient-form :current-step] dec)
       db))))

;; 跳转到指定步骤
(rf/reg-event-db
 ::goto-step
 (fn [db [_ step]]
   (assoc-in db [:patient-form :current-step] step)))

;; 验证表单
(rf/reg-event-fx
 ::validate-and-submit
 (fn [{:keys [db]} _]
   (let [form-data (get-in db [:patient-form])
         basic-info (:basic-info form-data)
         general-condition (:general-condition form-data)
         ;; 扩展的验证规则
         errors (cond-> {}
                  ;; 基本信息验证
                  (str/blank? (:outpatient-number basic-info))
                  (assoc-in [:basic-info :outpatient-number] "门诊号不能为空")

                  (str/blank? (:name basic-info))
                  (assoc-in [:basic-info :name] "姓名不能为空")

                  (nil? (:gender basic-info))
                  (assoc-in [:basic-info :gender] "性别不能为空")

                  (nil? (:age basic-info))
                  (assoc-in [:basic-info :age] "年龄不能为空")

                  ;; 一般情况验证
                  (nil? (:height general-condition))
                  (assoc-in [:general-condition :height] "身高不能为空")

                  (nil? (:weight general-condition))
                  (assoc-in [:general-condition :weight] "体重不能为空")

                  (nil? (get-in general-condition [:blood-pressure :systolic]))
                  (assoc-in [:general-condition :blood-pressure :systolic] "收缩压不能为空")

                  (nil? (get-in general-condition [:blood-pressure :diastolic]))
                  (assoc-in [:general-condition :blood-pressure :diastolic] "舒张压不能为空"))]

     (if (seq errors)
       ;; 有错误，更新错误状态
       {:db (assoc-in db [:patient-form :form-errors] errors)}
       ;; 无错误，提交表单
       {:db (-> db
                (assoc-in [:patient-form :submitting?] true)
                (assoc-in [:patient-form :form-errors] {}))
        :dispatch [::submit-form]}))))

;; 提交表单
(rf/reg-event-fx
 ::submit-form
 (fn [{:keys [db]} _]
   (let [form-data (get-in db [:patient-form])]
     {:http-xhrio {:method          :post
                   :uri             "/api/patient/assessment"
                   :params          (select-keys form-data [:basic-info
                                                            :general-condition
                                                            :medical-summary
                                                            :comorbidities
                                                            :physical-examination
                                                            :auxiliary-examination
                                                            :assessment
                                                            :other-info])
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [::submit-success]
                   :on-failure      [::submit-failure]}
      :db db})))

;; 提交成功处理
(rf/reg-event-db
 ::submit-success
 (fn [db [_ response]]
   (timbre/debug "表单提交成功:" response)
   (-> db
       (assoc-in [:patient-form :submitting?] false)
       (assoc-in [:patient-form :submit-success?] true))))

;; 提交失败处理
(rf/reg-event-db
 ::submit-failure
 (fn [db [_ response]]
   (timbre/error "表单提交失败:" response)
   (-> db
       (assoc-in [:patient-form :submitting?] false)
       (assoc-in [:patient-form :submit-error] (or (get-in response [:response :message]) "提交失败，请稍后再试")))))

;; 设置当前日期为评估日期
(rf/reg-event-db
 ::set-assessment-date-today
 (fn [db _]
   (let [today (.toISOString (js/Date.))]
     (assoc-in db [:patient-form :other-info :assessment-date] (subs today 0 10)))))

;; 切换布尔值
(rf/reg-event-db
 ::toggle-boolean-field
 (fn [db [_ path]]
   (let [current-value (get-in db (into [:patient-form] path))]
     (assoc-in db (into [:patient-form] path) (not current-value)))))
