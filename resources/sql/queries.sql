-- Place your queries here. Docs available https://www.hugsql.org/

-- 插入患者评估
-- :name insert-patient-assessment! :! :n
-- :doc 插入一个新的患者评估记录
INSERT INTO patient_assessments
(patient_id, assessment_data, patient_name_pinyin, patient_name_initial, created_at, updated_at)
VALUES (:patient_id, :assessment_data, :patient_name_pinyin, :patient_name_initial, datetime('now'), datetime('now'));

-- 更新患者评估
-- :name update-patient-assessment! :! :n
-- :doc 更新现有的患者评估记录
UPDATE patient_assessments
SET assessment_data = :assessment_data,
    patient_name_pinyin = :patient_name_pinyin,
    patient_name_initial = :patient_name_initial,
    updated_at = datetime('now')
WHERE patient_id = :patient_id;

-- 根据患者ID获取患者评估
-- :name get-patient-assessment-by-id :? :1
-- :doc 通过 patient_id 检索患者评估
SELECT assessment_data FROM patient_assessments WHERE patient_id = :patient_id;

-- 获取所有患者评估信息
-- :name get-all-patient-assessments :*
-- :doc 检索所有患者的评估数据, 可选通过拼音、首字母和更新时间范围进行筛选
SELECT * FROM patient_assessments
WHERE 1=1
--~ (when (:name_pinyin params) "AND lower(patient_name_pinyin) LIKE lower(:name_pinyin)")
--~ (when (:name_initial params) "AND lower(patient_name_initial) = lower(:name_initial)")
--~ (when (:updated_from params) "AND updated_at >= :updated_from")
--~ (when (:updated_to params) "AND updated_at <= :updated_to")
ORDER BY updated_at DESC;

-- :name delete-patient-assessment-by-id! :! :n
-- :doc Deletes a patient assessment by patient_id
DELETE FROM patient_assessments WHERE patient_id = :patient_id;

-- 医生相关操作

-- :name create-doctor! :! :n
-- :doc 创建一个新医生
INSERT INTO doctors (username, password_hash, name)
VALUES (:username, :password_hash, :name);

-- :name get-doctor-by-username :? :1
-- :doc 根据用户名获取医生信息
SELECT * FROM doctors WHERE username = :username;

-- :name get-doctor-by-id :? :1
-- :doc 根据ID获取医生信息
SELECT id, username, name, created_at, updated_at FROM doctors WHERE id = :id;

-- :name list-doctors :*
-- :doc 列出所有医生信息 (不包含密码)
SELECT id, username, name, created_at, updated_at FROM doctors ORDER BY created_at DESC;

-- :name update-doctor-password! :! :n
-- :doc 更新医生密码
UPDATE doctors
SET password_hash = :password_hash, updated_at = datetime('now')
WHERE id = :id;

-- :name update-doctor-name! :! :n
-- :doc 更新医生姓名
UPDATE doctors
SET name = :name, updated_at = datetime('now')
WHERE id = :id;

-- :name delete-doctor! :! :n
-- :doc 根据ID删除医生
DELETE FROM doctors WHERE id = :id;
