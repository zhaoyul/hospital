(ns hc.hospital.patient.subs
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str])) ;; 添加字符串工具

(rf/reg-sub
 ::patient-form
 (fn [db]
   (get db :patient-form)))

(rf/reg-sub
 ::basic-info
 :<- [::patient-form]
 (fn [form]
   (:basic-info form)))

;; The ::general-condition subscription is not used by the current patient-form view.
;; You can keep it if it's used elsewhere, or remove it if it was specific to a
;; now-removed part of this form.
(rf/reg-sub
 ::general-condition
 :<- [::patient-form]
 (fn [form]
   (:general-condition form)))


(rf/reg-sub
 ::medical-summary
 :<- [::patient-form]
 (fn [form]
   (:medical-summary form)))

(rf/reg-sub
 ::comorbidities
 :<- [::patient-form]
 (fn [form]
   (:comorbidities form)))

(rf/reg-sub
 ::physical-examination
 :<- [::patient-form]
 (fn [form]
   (:physical-examination form)))

(rf/reg-sub
 ::auxiliary-examination
 :<- [::patient-form]
 (fn [form]
   (:auxiliary-examination form)))

;; Subscriptions like ::anesthesia-plan, ::assessment, ::other-info
;; are not used by the current patient-form view.
;; Consider removing them if they are not used by other patient app features.
(rf/reg-sub
 ::anesthesia-plan
 :<- [::patient-form]
 (fn [form]
   (:anesthesia-plan form)))

(rf/reg-sub
 ::assessment
 :<- [::patient-form]
 (fn [form]
   (:assessment form)))

(rf/reg-sub
 ::other-info
 :<- [::patient-form]
 (fn [form]
   (:other-info form)))


(rf/reg-sub
 ::current-step
 :<- [::patient-form]
 (fn [form]
   (:current-step form)))

(rf/reg-sub
 ::form-errors
 :<- [::patient-form]
 (fn [form]
   (:form-errors form)))

(rf/reg-sub
 ::submitting?
 :<- [::patient-form]
 (fn [form]
   (:submitting? form)))

(rf/reg-sub
 ::submit-success?
 :<- [::patient-form]
 (fn [form]
   (:submit-success? form)))

(rf/reg-sub
 ::submit-error
 :<- [::patient-form]
 (fn [form]
   (:submit-error form)))

(rf/reg-sub
 ::patient-form-submit-success?
 (fn [db _]
   (get-in db [:patient-form :submit-success?])))

;; 添加一个新的订阅函数来检查当前步骤必填字段是否完成
(rf/reg-sub
 ::current-step-required-fields-filled?
 :<- [::current-step]
 :<- [::basic-info]
 :<- [::medical-summary]
 :<- [::comorbidities]
 (fn [[current-step basic-info medical-summary comorbidities] _]
   (case current-step
     ;; 第一步：基本信息
     0 (and (not (str/blank? (:outpatient-number basic-info)))
            (not (str/blank? (:name basic-info)))
            (not (str/blank? (:id-number basic-info)))
            (not (str/blank? (:phone basic-info)))
            (not (nil? (:gender basic-info)))
            (not (nil? (:age basic-info)))
            (not (or (nil? (:hospital-district basic-info)) 
                     (str/blank? (:hospital-district basic-info)))))
     ;; 其他步骤可以在这里添加必填字段检查逻辑
     ;; 1 (and ...)
     ;; 2 (and ...)
     ;; 如果没有必填字段或不需要验证，返回true
     true)))