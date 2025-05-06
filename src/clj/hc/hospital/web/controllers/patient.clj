(ns hc.hospital.web.controllers.patient
  (:require
   [hc.hospital.web.pages.layout :as layout]
   [ring.util.http-response :as http-response]))

(defn patient-form-page [request]
  ;; 渲染患者表单HTML页面
  (layout/render request "patient_form.html"))