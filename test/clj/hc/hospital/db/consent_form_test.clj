(ns hc.hospital.db.consent-form-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :refer [system-fixture *sys*]]
            [hc.hospital.db.consent-form :as cf]))

(use-fixtures :once (system-fixture))

(defn query-fn []
  (let [sys @*sys*]
    (or (get sys :db.sql/query-fn) (get-in sys [:db.sql/query-fn :query-fn]))))

(deftest consent-form-db-test
  (let [qf (query-fn)
        patient-id (str (java.util.UUID/randomUUID))
        ;; 创建评估记录以获取 assessment_id
        _ (qf :insert-patient-assessment!
              {:patient_id patient-id
               :assessment_data "{}"
               :patient_name "测试"
               :assessment_status "待评估"
               :patient_name_pinyin "p"
               :patient_name_initial "p"
               :doctor_signature_b64 nil
               :checkin_time "2099-01-01T00:00:00Z"
               :身份证号 nil
               :手机号 nil
               :院区 nil})
        assessments (qf :get-all-patient-assessments {})
        assessment-id (:id (first (filter #(= patient-id (:patient_id %)) assessments)))
        form {:assessment_id assessment-id :sedation_form "html" :pre_anesthesia_form nil :anesthesia_form "a"}]
    (is (= 1 (cf/save-consent-form! qf form)))
    (let [saved (cf/get-consent-form qf assessment-id)]
      (is (= assessment-id (:assessment_id saved)))
      (is (= "html" (:sedation_form saved)))
      (is (= "a" (:anesthesia_form saved))))))
