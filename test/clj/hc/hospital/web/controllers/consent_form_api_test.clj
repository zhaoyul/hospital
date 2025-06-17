(ns hc.hospital.web.controllers.consent-form-api-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :refer [system-fixture *sys* GET POST]]
            [cheshire.core :as json]))

(use-fixtures :once (system-fixture))

(defn app [] (get-in @*sys* [:ring/handler :handler]))

(deftest consent-form-api
  (let [app (app)
        patient-id (str (rand-int 100000))
        qf (or (get @*sys* :db.sql/query-fn) (get-in @*sys* [:db.sql/query-fn :query-fn]))
        _ (qf :insert-patient-assessment!
              {:patient_id patient-id
               :assessment_data "{}"
               :patient_name_pinyin "p"
               :patient_name_initial "p"
               :doctor_signature_b64 nil})
        assessments (qf :get-all-patient-assessments {})
        assessment-id (:id (first (filter #(= patient-id (:patient_id %)) assessments)))
        payload {:assessment_id assessment-id :sedation_form "d" :anesthesia_form "e"}]
    (let [resp (POST app "/api/consent-forms" (json/encode payload)
                     {"content-type" "application/json"})]
      (is (= 200 (:status resp))))
    (let [resp (GET app (str "/api/consent-forms/" assessment-id))
          body (json/parse-string (:body resp) true)]
      (is (= 200 (:status resp)))
      (is (= assessment-id (:assessment_id body)))
      (is (= "e" (:anesthesia_form body))))))
