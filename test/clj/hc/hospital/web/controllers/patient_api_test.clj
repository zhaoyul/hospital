(ns hc.hospital.web.controllers.patient-api-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :refer [system-fixture *sys* GET PUT POST DELETE]] ; Added DELETE
            [cheshire.core :as json]
            [integrant.core :as ig]
            [clojure.java.io :as io]
            [ring.util.codec :as codec]
            [hc.hospital.web.controllers.patient-api :as patient-api]))

(use-fixtures :once (system-fixture))

(defn get-app []
  (:handler/ring @*sys*))

(defn get-query-fn []
  ;; In some setups the query-fn is stored directly under :db.sql/query-fn,
  ;; while in others it is nested under :query-fn.  Support both to avoid nil
  ;; dereferences during tests.
  (let [system @*sys*]
    (or (get system :db.sql/query-fn)
        (get-in system [:db.sql/query-fn :query-fn]))))

(deftest patient-assessment-api-signature-test
  (let [app (get-app)
        query-fn (get-query-fn)
        patient-id (str "PAT" (rand-int 100000)) ; More realistic patient ID
        initial-assessment-data {:基本信息 {:门诊号 patient-id
                                   :姓名 "张三"
                                   :年龄 30
                                   :性别 "男"
                                   :身份证号 nil
                                   :手机号 nil
                                   :院区 nil
                                   :签到时间 "2099-01-01T00:00:00Z"}
                                 :medical_history {:allergy {:has_history false}}}
        signature-key-path [:基本信息 :医生签名图片] ; Path to the signature in the response

        ;; Helper to parse response body supporting different body types
        parse-body (fn [body]
                     (cond
                       (string? body) (json/parse-string body true)
                       (instance? java.io.InputStream body) (json/parse-stream (io/reader body) true)
                       (map? body) body
                       :else (json/parse-string (slurp body) true)))]

    ;; 0. Initial cleanup (in case of previous test failure)
    (query-fn :delete-patient-assessment-by-id! {:patient_id patient-id})

    (testing "通过API提交新的评估 (不含签名)"
      (let [response (POST app (str "/api/patient/assessment") (json/encode initial-assessment-data))
            body (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= "评估提交成功！" (:message body)))
        ;; Verify in DB
        (let [db-assessment (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
          (is (some? db-assessment))
          (is (nil? (:doctor_signature_b64 db-assessment)) "数据库中医生签名应为空"))))

    (testing "通过API更新评估以添加签名"
      (let [signature-to-add "data:image/png;base64,ADDED_SIGNATURE"
            updated-data-with-signature (assoc-in initial-assessment-data signature-key-path signature-to-add)
              response (PUT app (str "/api/patient/assessment/" patient-id) (json/encode updated-data-with-signature))
            body (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= "评估更新成功！" (:message body)))
        ;; Verify in DB
        (let [db-assessment (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
          (is (= signature-to-add (:doctor_signature_b64 db-assessment)) "数据库中医生签名应已添加"))))

    (testing "通过API获取评估并验证签名"
      (let [response (GET app (str "/api/patient/assessment/" patient-id))
            body (parse-body (:body response))
            retrieved-signature (get-in (:assessment_data body) signature-key-path)]
        (is (= 200 (:status response)))
        (is (some? (:assessment_data body)) "获取到的评估数据不应为空")
        (is (= "data:image/png;base64,ADDED_SIGNATURE" retrieved-signature) "获取到的评估中医生签名应正确")))

    (testing "通过API更新评估以修改签名"
      (let [signature-to-modify "data:image/png;base64,MODIFIED_SIGNATURE"
            updated-data-with-modified-signature (assoc-in initial-assessment-data signature-key-path signature-to-modify)
              response (PUT app (str "/api/patient/assessment/" patient-id) (json/encode updated-data-with-modified-signature))
            body (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= "评估更新成功！" (:message body)))
        ;; Verify in DB
        (let [db-assessment (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
          (is (= signature-to-modify (:doctor_signature_b64 db-assessment)) "数据库中医生签名应已修改"))))

    (testing "通过API更新评估以清除签名"
      (let [updated-data-without-signature (assoc-in initial-assessment-data signature-key-path nil) ; Or remove the key
              response (PUT app (str "/api/patient/assessment/" patient-id) (json/encode updated-data-without-signature))
            body (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= "评估更新成功！" (:message body)))
        ;; Verify in DB
        (let [db-assessment (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
          (is (nil? (:doctor_signature_b64 db-assessment)) "数据库中医生签名应已清除"))
        ;; Verify in API GET response
        (let [get-response (GET app (str "/api/patient/assessment/" patient-id))
              get-body (parse-body (:body get-response))
              retrieved-signature (get-in (:assessment_data get-body) signature-key-path)]
          (is (nil? retrieved-signature) "API获取评估时医生签名应为nil"))))

    (testing "清理测试数据"
      (let [delete-result (query-fn :delete-patient-assessment-by-id! {:patient_id patient-id})]
        (is (= 1 delete-result) "删除操作应返回影响的行数为1")
        (let [retrieved-after-delete (query-fn :get-patient-assessment-by-id {:patient_id patient-id})]
          (is (nil? retrieved-after-delete) "删除后应无法从数据库检索到评估")))))

(deftest patient-find-and-list-test
  (let [query-fn (get-query-fn)
        patient-id "HISPAT001"
        _ (query-fn :delete-patient-assessment-by-id! {:patient_id patient-id})
        oracle-stub (fn [& _] {:name "李四" :sex "女" :date_of_birth "1985-01-01" :id_no "ID01"})]

    (testing "通过HIS查询并创建本地记录"
      (let [resp (patient-api/find-patient-by-id-handler {:parameters {:path {:patientIdInput patient-id}}
                                                         :query-fn query-fn
                                                         :oracle-query-fn oracle-stub})
            body (:body resp)]
        (is (= 200 (:status resp)))
        (is (= patient-id (:patientIdInput body)))
        (is (= "李四" (get-in body [:his_info :name])))
        (is (some? (query-fn :get-patient-assessment-by-id {:patient_id patient-id}))))

    (testing "获取所有患者评估列表"
      (let [resp (patient-api/get-all-patient-assessments-handler {:parameters {:query {}}
                                                                  :query-fn query-fn})
            body (:body resp)]
        (is (= 200 (:status resp)))
        (is (vector? body))
        (is (pos? (count body)))))

    (testing "清理创建的记录"
      (query-fn :delete-patient-assessment-by-id! {:patient_id patient-id}))))

(deftest patient-assessment-filter-test
  (let [query-fn (get-query-fn)
        id1 "FILTER001"
        id2 "FILTER002"
        data1 {:基本信息 {:门诊号 id1 :姓名 "王五" :评估状态 "已批准" :评估更新时间 "2024-01-01"}}
        data2 {:基本信息 {:门诊号 id2 :姓名 "赵六" :评估状态 "待评估" :评估更新时间 "2024-01-02"}}
        json1 (json/generate-string data1)
        json2 (json/generate-string data2)]
    (query-fn :insert-patient-assessment! {:patient_id id1
                                           :assessment_data json1
                                           :patient_name "王五"
                                           :assessment_status "已批准"
                                           :patient_name_pinyin "wangwu"
                                           :patient_name_initial "ww"
                                           :doctor_signature_b64 nil})
    (query-fn :insert-patient-assessment! {:patient_id id2
                                           :assessment_data json2
                                           :patient_name "赵六"
                                           :assessment_status "待评估"
                                           :patient_name_pinyin "zhaoliu"
                                           :patient_name_initial "zl"
                                           :doctor_signature_b64 nil})

    (let [resp (patient-api/get-all-patient-assessments-handler {:parameters {:query {:status "已批准" :name_initial "ww"}}
                                                                 :query-fn query-fn})
          body (:body resp)]
      (is (= 200 (:status resp)))
      (is (= 1 (count body)))
      (is (= id1 (:patient_id (first body)))))

    (query-fn :delete-patient-assessment-by-id! {:patient_id id1})
    (query-fn :delete-patient-assessment-by-id! {:patient_id id2})))
)
)
