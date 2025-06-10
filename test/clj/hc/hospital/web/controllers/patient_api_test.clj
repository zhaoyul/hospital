(ns hc.hospital.web.controllers.patient-api-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :refer [system-fixture system-state GET PUT POST DELETE]]
            [cheshire.core :as cheshire]
            [integrant.core :as ig]
            [ring.util.codec :as codec])) ; For URL encoding if needed

(use-fixtures :once system-fixture)

(defn get-app []
  (:handler/run (system-state)))

(defn get-query-fn []
   ;; Duplicated from patient_assessment_test.clj for now, could be refactored into test-utils
  (let [system (system-state)]
    (or (-> system :db.sql/query-fn)
        (-> system :db :query-fn)
        (throw (IllegalStateException. "Query function not found in system state for test."))))))

(deftest patient-assessment-api-signature-test
  (let [app (get-app)
        query-fn (get-query-fn)
        patient-id-api-test "PATAPISIG001"
        base-assessment-data {:基本信息 {:门诊号 patient-id-api-test
                                     :姓名 "API测试患者"
                                     :评估状态 "待评估"}
                              :循环系统 {:心悸 "无"}}
        signature-base64 "data:image/png;base64,apidatasig=="
        updated-signature-base64 "data:image/png;base64,updatedapidatasig=="]

    ;; 确保测试前没有这个ID的数据 (可选，因为 system-fixture 应该重置数据库)
    (query-fn :delete-patient-assessment-by-id! {:patient_id patient-id-api-test})

    (testing "通过API提交新的评估 (不含签名)" ;; Chinese comment: Submit new assessment via API (without signature)
      (let [submit-body (cheshire/generate-string {:basic-info {:outpatient-number patient-id-api-test
                                                                :name "API测试患者"}
                                                   ;; ... other sections if needed by submit-assessment! transformation logic
                                                   })
            ;; 注意: submit-assessment! 的路由是 /api/patient/assessment (POST), 不是 /:patient-id
            response (POST app "/api/patient/assessment" submit-body {"Content-Type" "application/json"})
            status (:status response)
            body (cheshire/parse-string (:body response) keyword)]
        (is (= 200 status) "提交新评估应该成功")
        (is (= "评估提交成功！" (:message body)) "成功消息应该匹配")))

    (testing "通过API更新评估以添加签名" ;; Chinese comment: Update assessment via API to add signature
      (let [update-payload (assoc-in base-assessment-data [:基本信息 :医生签名图片] signature-base64)
            response (PUT app (str "/api/patient/assessment/" patient-id-api-test)
                          (cheshire/generate-string update-payload)
                          {"Content-Type" "application/json"})
            status (:status response)
            body (cheshire/parse-string (:body response) keyword)]
        (is (= 200 status) "更新评估以添加签名应该成功")
        (is (= "评估更新成功！" (:message body)) "成功消息应该匹配")

        ;; 直接从数据库验证
        (let [db-record (query-fn :get-patient-assessment-by-id {:patient_id patient-id-api-test})]
          (is (= signature-base64 (:doctor_signature_b64 db-record)) "数据库中的签名应该匹配"))))

    (testing "通过API获取评估并验证签名" ;; Chinese comment: Get assessment via API and verify signature
      (let [response (GET app (str "/api/patient/assessment/" patient-id-api-test) {} {})
            status (:status response)
            body (cheshire/parse-string (:body response) keyword)]
        (is (= 200 status) "获取评估应该成功")
        (is (= signature-base64 (get-in body [:基本信息 :医生签名图片])) "API返回的评估中签名应该匹配")))

    (testing "通过API更新评估以修改签名" ;; Chinese comment: Update assessment via API to modify signature
      (let [update-payload (assoc-in base-assessment-data [:基本信息 :医生签名图片] updated-signature-base64)
            response (PUT app (str "/api/patient/assessment/" patient-id-api-test)
                          (cheshire/generate-string update-payload)
                          {"Content-Type" "application/json"})
            status (:status response)
            body (cheshire/parse-string (:body response) keyword)]
        (is (= 200 status) "更新评估以修改签名应该成功")

        ;; 直接从数据库验证
        (let [db-record (query-fn :get-patient-assessment-by-id {:patient_id patient-id-api-test})]
          (is (= updated-signature-base64 (:doctor_signature_b64 db-record)) "数据库中的签名应该被修改"))))

    (testing "通过API更新评估以清除签名" ;; Chinese comment: Update assessment via API to clear signature
      (let [update-payload (assoc-in base-assessment-data [:基本信息 :医生签名图片] nil)
            response (PUT app (str "/api/patient/assessment/" patient-id-api-test)
                          (cheshire/generate-string update-payload)
                          {"Content-Type" "application/json"})
            status (:status response)
            body (cheshire/parse-string (:body response) keyword)]
        (is (= 200 status) "更新评估以清除签名应该成功")

        ;; 直接从数据库验证
        (let [db-record (query-fn :get-patient-assessment-by-id {:patient_id patient-id-api-test})]
          (is (nil? (:doctor_signature_b64 db-record)) "数据库中的签名应该为nil"))))

    ;; 清理
    (query-fn :delete-patient-assessment-by-id! {:patient_id patient-id-api-test})))

;; (run-tests 'hc.hospital.web.controllers.patient-api-test)
