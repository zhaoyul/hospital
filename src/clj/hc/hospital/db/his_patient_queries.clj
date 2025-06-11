(ns hc.hospital.db.his-patient-queries
  (:require [clojure.tools.logging :as log]))

;; 这些函数期望接收一个 oracle-query-fn，该函数应与 oracle_his_queries.sql 文件绑定
;; 并且配置为 hugsql.core/def-db-fns 生成的函数集合中的特定查询函数。
;; 例如, 如果 hugsql.core/def-db-fns 基于 "oracle_his_queries.sql" 生成了查询函数，
;; oracle-query-fn 可能是这些生成函数中的一个，或者是一个能够调用它们的通用函数。
;; 这里的实现假设 oracle-query-fn 是一个可以接受查询名关键字和参数映射的函数。

(defn find-in-pat-register-by-reg-no
  "从 VIEW_PAT_REGISTER 按挂号流水号查询患者。"
  [oracle-query-fn register-no]
  (log/info "HIS查询 (VIEW_PAT_REGISTER) by REGISTER_NO:" register-no)
  (oracle-query-fn :find-patient-in-pat-register-by-reg-no {:register_no register-no}))

(defn find-in-pat-register-by-patient-id
  "从 VIEW_PAT_REGISTER 按病人唯一标识号查询患者。"
  [oracle-query-fn patient-id]
  (log/info "HIS查询 (VIEW_PAT_REGISTER) by PATIENT_ID:" patient-id)
  (oracle-query-fn :find-patient-in-pat-register-by-patient-id {:patient_id patient-id}))

(defn find-in-hospital-by-reg-no
  "从 VIEW_IN_HOSPITAL 按住院流水号查询患者。"
  [oracle-query-fn register-no]
  (log/info "HIS查询 (VIEW_IN_HOSPITAL) by REGISTER_NO:" register-no)
  (oracle-query-fn :find-patient-in-hospital-by-reg-no {:register_no register-no}))

(defn find-in-hospital-by-patient-id
  "从 VIEW_IN_HOSPITAL 按病人唯一标识号查询患者。"
  [oracle-query-fn patient-id]
  (log/info "HIS查询 (VIEW_IN_HOSPITAL) by PATIENT_ID:" patient-id)
  (oracle-query-fn :find-patient-in-hospital-by-patient-id {:patient_id patient-id}))

(defn find-in-hospital-by-inp-no
  "从 VIEW_IN_HOSPITAL 按病人住院号查询患者。"
  [oracle-query-fn inp-no]
  (log/info "HIS查询 (VIEW_IN_HOSPITAL) by INP_NO:" inp-no)
  (oracle-query-fn :find-patient-in-hospital-by-inp-no {:inp_no inp-no}))

(defn find-patient-in-his
  "按顺序查询HIS视图以查找患者信息。
  参数:
    oracle-query-fn (fn): 用于HIS数据库的查询函数。它应该接受一个查询关键字和参数map。
    patient-id-input (string): 可能是REGISTER_NO, PATIENT_ID或INP_NO。
  返回:
    找到的患者信息 (map)，如果未找到则返回 nil。"
  [oracle-query-fn patient-id-input]
  (log/info "在HIS中查询患者，输入ID:" patient-id-input)
  (or
   (find-in-pat-register-by-reg-no oracle-query-fn patient-id-input)
   (find-in-pat-register-by-patient-id oracle-query-fn patient-id-input)
   (find-in-hospital-by-reg-no oracle-query-fn patient-id-input)
   (find-in-hospital-by-patient-id oracle-query-fn patient-id-input)
   (find-in-hospital-by-inp-no oracle-query-fn patient-id-input)))
