(ns hc.hospital.db.patient-assessment-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :refer [system-fixture *sys*]]
            [cheshire.core :as json]
            [integrant.core :as ig]))

(use-fixtures :once system-fixture)

(defn get-query-fn []
  (get-in @*sys* [:db.sql/query-fn :query-fn]))

(deftest patient-assessment-signature-test
  (let [query-fn (get-query-fn)
        patient-id (str (java.util.UUID/randomUUID))
        initial-assessment-data {:form "initial-form-data"}
        initial-signature "data:image/png;base64,INITIAL_SIGNATURE_DATA"
        updated-signature "data:image/png;base64,UPDATED_SIGNATURE_DATA"
        assessment-data-json (json/generate-string initial-assessment-data)]

    (testing "插入患者评估 (带签名)"
      (let [insert-result (query-fn :insert-patient-assessment!
                                    {:patient_id patient-id
                                     :assessment_data assessment-data-json
                                     :patient_name_pinyin "ceshipinyin"
                                     :patient_name_initial "cspy"
                                     :doctor_signature_b64 initial-signature})
            retrieved (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
        (is (= 1 insert-result) "插入应返回影响的行数为1")
        (is (some? retrieved) "应能检索到插入的评估")
        (is (= assessment-data-json (:assessment_data retrieved)) "评估数据应匹配")
        (is (= initial-signature (:doctor_signature_b64 retrieved)) "医生签名应匹配")))

    (testing "更新患者评估 (修改签名)"
      (let [updated-assessment-data {:form "updated-form-data"}
            updated-assessment-data-json (json/generate-string updated-assessment-data)
            update-result (query-fn :update-patient-assessment!
                                    {:patient_id patient-id
                                     :assessment_data updated-assessment-data-json
                                     :patient_name_pinyin "ceshipinyingx"
                                     :patient_name_initial "cspygx"
                                     :doctor_signature_b64 updated-signature})
            retrieved (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
        (is (= 1 update-result) "更新应返回影响的行数为1")
        (is (= updated-assessment-data-json (:assessment_data retrieved)) "更新后的评估数据应匹配")
        (is (= updated-signature (:doctor_signature_b64 retrieved)) "更新后的医生签名应匹配")))

    (testing "更新患者评估 (清除签名)"
      (let [update-result (query-fn :update-patient-assessment!
                                    {:patient_id patient-id
                                     :assessment_data assessment-data-json ; Revert data, focus on signature
                                     :patient_name_pinyin "ceshipinyin"
                                     :patient_name_initial "cspy"
                                     :doctor_signature_b64 nil}) ; 清除签名
            retrieved (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
        (is (= 1 update-result) "更新（清除签名）应返回影响的行数为1")
        (is (nil? (:doctor_signature_b64 retrieved)) "医生签名应为nil (已清除)")))

    ;; 清理测试数据 - 注意: HuggingSQL/Korma 等通常不直接提供 DELETE ALL 功能
    ;; 通常在测试环境中，数据库会在每次测试运行前后被重置或通过特定脚本清理
    ;; 这里我们尝试删除刚刚创建的特定记录
    (testing "清理测试数据"
      (let [delete-result (query-fn :delete-patient-assessment-by-id! {:patient_id patient-id})
            retrieved-after-delete (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
        ;; 假设 :delete-patient-assessment-by-id! 返回影响的行数
        (is (= 1 delete-result) "删除操作应返回影响的行数为1")
        (is (nil? retrieved-after-delete) "删除后应无法检索到评估")))))
