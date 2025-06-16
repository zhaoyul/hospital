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
