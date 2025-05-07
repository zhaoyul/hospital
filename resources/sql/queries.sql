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
