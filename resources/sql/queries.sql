-- Place your queries here. Docs available https://www.hugsql.org/

-- 获取患者评估
-- :name get-patient-assessment :? :1
-- :doc 通过 patient_id 获取患者评估
SELECT * FROM patient_assessments
WHERE patient_id = :patient_id;

-- 插入患者评估
-- :name insert-patient-assessment! :! :n
-- :doc 插入一个新的患者评估记录
INSERT INTO patient_assessments
(patient_id, assessment_data, created_at)
VALUES (:patient_id, :assessment_data, now());

-- 更新患者评估
-- :name update-patient-assessment! :! :n
-- :doc 更新现有的患者评估记录
UPDATE patient_assessments
SET assessment_data = :assessment_data, updated_at = now()
WHERE patient_id = :patient_id;
