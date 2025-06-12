-- :name find-patient-in-pat-register-by-reg-no :? :1
-- :doc 从 VIEW_PAT_REGISTER 按挂号流水号查询患者
SELECT NAME, SEX, DATE_OF_BIRTH
FROM VIEW_PAT_REGISTER
WHERE REGISTER_NO = :register_no

-- :name find-patient-in-pat-register-by-patient-id :? :1
-- :doc 从 VIEW_PAT_REGISTER 按病人唯一标识号查询患者
SELECT NAME, SEX, DATE_OF_BIRTH
FROM VIEW_PAT_REGISTER
WHERE PATIENT_ID = :patient_id

-- :name find-patient-in-hospital-by-reg-no :? :1
-- :doc 从 VIEW_IN_HOSPITAL 按住院流水号查询患者
SELECT NAME, SEX, DATE_OF_BIRTH
FROM VIEW_IN_HOSPITAL
WHERE REGISTER_NO = :register_no

-- :name find-patient-in-hospital-by-patient-id :? :1
-- :doc 从 VIEW_IN_HOSPITAL 按病人唯一标识号查询患者
SELECT NAME, SEX, DATE_OF_BIRTH
FROM VIEW_IN_HOSPITAL
WHERE PATIENT_ID = :patient_id

-- :name find-patient-in-hospital-by-inp-no :? :1
-- :doc 从 VIEW_IN_HOSPITAL 按病人住院号查询患者
SELECT NAME, SEX, DATE_OF_BIRTH
FROM VIEW_IN_HOSPITAL
WHERE INP_NO = :inp_no
