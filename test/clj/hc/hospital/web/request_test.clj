(ns hc.hospital.web.request-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [hc.hospital.test-utils :refer [system-state system-fixture GET POST]]
            [cheshire.core :as cheshire]))

(use-fixtures :once (system-fixture))

(deftest health-request-test []
  (testing "happy path"
    (let [handler (:handler/ring (system-state))
          params {}
          headers {}
          response (GET handler "/api/health" params headers)]
      (is (= 200 (:status response))))))

(deftest patient-assessment-cardiovascular-test
  (testing "Submit and retrieve patient assessment with detailed cardiovascular and cardiac data"
    (let [handler (:handler/ring (system-state))
          patient-id "cardio-test-001"
          sample-assessment-data {:basic-info {:outpatient-number patient-id
                                               :name "Cardio Test User"}
                                  :comorbidities {:cardiovascular-disease
                                                  {:has true
                                                   :coronary-artery-disease {:has true
                                                                             :symptoms "稳定性心绞痛"
                                                                             :stent true
                                                                             :treatment-status "好转"
                                                                             :medication "阿司匹林"}
                                                   :arrhythmia {:has true
                                                                :type "低危型"
                                                                :treatment-status "仍有症状"
                                                                :medication "美托洛尔"}
                                                   :cardiomyopathy {:has true
                                                                    :treatment-status "治愈"
                                                                    :medication "依那普利"}
                                                   :valvular-heart-disease {:has true
                                                                            :treatment-status "好转"
                                                                            :medication "华法林"}
                                                   :congenital-heart-disease {:has true
                                                                              :treatment-status "仍有症状"
                                                                              :medication "暂无"}
                                                   :congestive-heart-failure {:has true
                                                                              :last-episode-date "2023-01-15"
                                                                              :treatment-status "好转"
                                                                              :medication "呋塞米"}
                                                   :pulmonary-hypertension {:has true
                                                                            :treatment-status "未治疗"
                                                                            :medication "西地那非"}}}
                                  :cardiac-assessment {:pacemaker-history {:has true
                                                                           :type "permanent"
                                                                           :working-status "良好"}
                                                       :cardiac-ultrasound-findings {:details "左心室肥厚"}
                                                       :coronary-cta-angiography-results {:details "LAD 70% 狭窄"}
                                                       :cardiac-function-assessment {:class "II"}
                                                       :exercise-capacity-assessment {:level "mildly-reduced"}
                                                       :other-cardiac-info {:details "偶有心悸"}}}
          ;; Submit the assessment
          submit-response (POST handler "/api/patient/assessment" (cheshire/generate-string sample-assessment-data) {})
          _ (is (<= 200 (:status submit-response) 299) (str "Submit response: " submit-response))

          ;; Retrieve the assessment
          retrieve-response (GET handler (str "/api/patient/assessment/" patient-id) {} {})
          _ (is (= 200 (:status retrieve-response)) (str "Retrieve response: " retrieve-response))
          retrieved-data (cheshire/parse-string (:body retrieve-response) true)]

      ;; Assert cardiovascular comorbidities
      (let [comorbs (get-in retrieved-data [:comorbidities])
            cardio-base (get comorbs :cardiovascular)
            cardio-details (get cardio-base :details)]
        (is (= true (:has cardio-base)) "Top-level cardiovascular :has")

        (let [cad (:coronary_artery_disease cardio-details)]
          (is (= true (:has cad)) "CAD :has")
          (is (= "稳定性心绞痛" (:symptoms cad)) "CAD :symptoms")
          (is (= true (:stent cad)) "CAD :stent")
          (is (= "好转" (:treatment_status cad)) "CAD :treatment_status")
          (is (= "阿司匹林" (:medication cad)) "CAD :medication"))

        (let [arrhythmia (:arrhythmia cardio-details)]
          (is (= true (:has arrhythmia)) "Arrhythmia :has")
          (is (= "低危型" (:type arrhythmia)) "Arrhythmia :type")
          (is (= "仍有症状" (:treatment_status arrhythmia)) "Arrhythmia :treatment_status")
          (is (= "美托洛尔" (:medication arrhythmia)) "Arrhythmia :medication"))

        (let [cardiomyopathy (:cardiomyopathy cardio-details)]
          (is (= true (:has cardiomyopathy)) "Cardiomyopathy :has")
          (is (= "治愈" (:treatment_status cardiomyopathy)) "Cardiomyopathy :treatment_status")
          (is (= "依那普利" (:medication cardiomyopathy)) "Cardiomyopathy :medication"))

        (let [vhd (:valvular_heart_disease cardio-details)]
          (is (= true (:has vhd)) "VHD :has")
          (is (= "好转" (:treatment_status vhd)) "VHD :treatment_status")
          (is (= "华法林" (:medication vhd)) "VHD :medication"))

        (let [chd (:congenital_heart_disease cardio-details)]
          (is (= true (:has chd)) "CHD :has")
          (is (= "仍有症状" (:treatment_status chd)) "CHD :treatment_status")
          (is (= "暂无" (:medication chd)) "CHD :medication"))

        (let [chf (:congestive_heart_failure cardio-details)]
          (is (= true (:has chf)) "CHF :has")
          (is (= "2023-01-15" (:last_episode_date chf)) "CHF :last_episode_date")
          (is (= "好转" (:treatment_status chf)) "CHF :treatment_status")
          (is (= "呋塞米" (:medication chf)) "CHF :medication"))

        (let [ph (:pulmonary_hypertension cardio-details)]
          (is (= true (:has ph)) "PH :has")
          (is (= "未治疗" (:treatment_status ph)) "PH :treatment_status")
          (is (= "西地那非" (:medication ph)) "PH :medication")))

      ;; Assert other cardiac assessment sections
      (let [pacemaker (:pacemaker_history retrieved-data)]
        (is (= true (:has pacemaker)))
        (is (= "permanent" (:type pacemaker)))
        (is (= "良好" (:working_status pacemaker))))
      (is (= "左心室肥厚" (get-in retrieved-data [:cardiac_ultrasound_findings :details])))
      (is (= "LAD 70% 狭窄" (get-in retrieved-data [:coronary_cta_angiography_results :details])))
      (is (= "II" (get-in retrieved-data [:cardiac_function_assessment :class])))
      (is (= "mildly-reduced" (get-in retrieved-data [:exercise_capacity_assessment :level])))
      (is (= "偶有心悸" (get-in retrieved-data [:other_cardiac_info :details]))))))
