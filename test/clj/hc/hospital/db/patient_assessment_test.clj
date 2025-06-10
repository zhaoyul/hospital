(ns hc.hospital.db.patient-assessment-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :refer [system-fixture system-state]]
            [cheshire.core :as cheshire]
            [integrant.core :as ig]))

;; 使用 system-fixture 来确保每个测试都在一个干净的、迁移过的数据库上运行
;; 并且可以访问系统组件 (如 query-fn)
(use-fixtures :once system-fixture)

(defn get-query-fn []
  (let [system (system-state)]
    ;; 通常 query-fn 存在于 :db.sql/query-fn 或类似的key路径下
    ;; 具体路径取决于 Integrant 配置 (system.edn)
    (or (-> system :db.sql/query-fn)      ;; Kit default
        (-> system :db :query-fn)         ;; Common alternative
        (throw (IllegalStateException. "Query function not found in system state for test."))))))


(deftest patient-assessment-signature-test
  (let [query-fn (get-query-fn)
        patient-id-test "PATTESTSIG001"
        initial-assessment-data {:基本信息 {:姓名 "测试签名患者"}
                                 :其他系统 {:内容 "一些初步内容"}}
        signature-base64 "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=" ;; 1x1 pixel black png
        updated-signature-base64 "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8different==" ;; "different"
        initial-assessment-json (cheshire/generate-string initial-assessment-data)]

    (testing "插入患者评估 (带签名)" ;; Chinese comment: Test inserting patient assessment (with signature)
      (let [insert-params {:patient_id patient-id-test
                           :assessment_data initial-assessment-json
                           :patient_name_pinyin "ceshiqianminghuanzhe"
                           :patient_name_initial "csqmhz"
                           :doctor_signature_b64 signature-base64}]
        (is (integer? (query-fn :insert-patient-assessment! insert-params)) "Insert should return an integer (usually rows affected or id)"))

      (let [retrieved (query-fn :get-patient-assessment-by-id {:patient_id patient-id-test})]
        (is (some? retrieved) "记录应该存在") ;; Record should exist
        (is (= patient-id-test (:patient_id retrieved)) "Patient ID should match")
        (is (= signature-base64 (:doctor_signature_b64 retrieved)) "医生签名数据应该匹配插入的值") ;; Doctor signature should match inserted value
        (is (= initial-assessment-data (cheshire/decode (:assessment_data retrieved) keyword)) "评估JSON数据应该匹配")))


    (testing "更新患者评估 (修改签名)" ;; Chinese comment: Test updating patient assessment (modify signature)
      (let [updated-assessment-data (assoc-in initial-assessment-data [:基本信息 :评估备注] "已更新备注")
            updated-assessment-json (cheshire/generate-string updated-assessment-data)
            update-params {:patient_id patient-id-test
                           :assessment_data updated-assessment-json
                           :patient_name_pinyin "ceshiqianminghuanzhe" ; Pinyin/Initial might not change in an update
                           :patient_name_initial "csqmhz"
                           :doctor_signature_b64 updated-signature-base64}]
        (is (integer? (query-fn :update-patient-assessment! update-params)) "Update should return an integer"))

      (let [retrieved (query-fn :get-patient-assessment-by-id {:patient_id patient-id-test})]
        (is (= updated-signature-base64 (:doctor_signature_b64 retrieved)) "医生签名数据应该更新") ;; Doctor signature should be updated
        (is (= updated-assessment-data (cheshire/decode (:assessment_data retrieved) keyword)) "评估JSON数据应该被更新")))


    (testing "更新患者评估 (清除签名)" ;; Chinese comment: Test updating patient assessment (clear signature)
      (let [assessment-data-no-sig (assoc-in initial-assessment-data [:基本信息 :评估备注] "签名已清除")
            assessment-json-no-sig (cheshire/generate-string assessment-data-no-sig)
            update-params-no-sig {:patient_id patient-id-test
                                  :assessment_data assessment-json-no-sig
                                  :patient_name_pinyin "ceshiqianminghuanzhe"
                                  :patient_name_initial "csqmhz"
                                  :doctor_signature_b64 nil}] ;; 清除签名
        (is (integer? (query-fn :update-patient-assessment! update-params-no-sig)) "Update (clearing signature) should return an integer"))

      (let [retrieved (query-fn :get-patient-assessment-by-id {:patient_id patient-id-test})]
        (is (nil? (:doctor_signature_b64 retrieved)) "医生签名数据应该为nil (已清除)") ;; Doctor signature should be nil (cleared)
        (is (= assessment-data-no-sig (cheshire/decode (:assessment_data retrieved) keyword)) "评估JSON数据应该被更新")))

    ;; 清理测试数据
    (testing "清理测试数据" ;; Chinese comment: Clean up test data
      (query-fn :delete-patient-assessment-by-id! {:patient_id patient-id-test})
      (let [retrieved (query-fn :get-patient-assessment-by-id {:patient_id patient-id-test})]
        (is (nil? retrieved) "记录应该已被删除"))))) ;; Record should be deleted

;; 如果需要，可以添加更多测试，例如测试 get-all-patient-assessments 是否也返回签名等。
;; (run-tests 'hc.hospital.db.patient-assessment-test) ;; For running tests individually if needed
