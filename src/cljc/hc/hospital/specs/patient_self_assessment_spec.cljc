(ns hc.hospital.specs.patient-self-assessment-spec
  "患者端自评数据的 Malli Schema，字段与医生端保持一致，避免后端额外转换。"
  (:require [hc.hospital.specs.assessment-complete-cn-spec :as cn-spec]))

(def PatientSelfAssessmentSpec
  "患者自评数据结构，复用医生端的 `患者评估数据Spec`。"
  cn-spec/患者评估数据Spec)

