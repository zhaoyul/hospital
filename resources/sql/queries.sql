-- Place your queries here. Docs available https://www.hugsql.org/

-- 插入患者评估
-- :name insert-patient-assessment! :! :n
-- :doc 插入一个新的患者评估记录
INSERT INTO patient_assessments
(patient_id, assessment_data, patient_name_pinyin, patient_name_initial, doctor_signature_b64, created_at, updated_at) -- 中文注释：添加 doctor_signature_b64 列
VALUES (:patient_id, :assessment_data, :patient_name_pinyin, :patient_name_initial, :doctor_signature_b64, datetime('now'), datetime('now')); -- 中文注释：添加 doctor_signature_b64 参数

-- 更新患者评估
-- :name update-patient-assessment! :! :n
-- :doc 更新现有的患者评估记录
UPDATE patient_assessments
SET assessment_data = :assessment_data,
    patient_name_pinyin = :patient_name_pinyin,
    patient_name_initial = :patient_name_initial,
    doctor_signature_b64 = :doctor_signature_b64, -- 中文注释：添加 doctor_signature_b64 更新
    updated_at = datetime('now')
WHERE patient_id = :patient_id;

-- 根据患者ID获取患者评估
-- :name get-patient-assessment-by-id :? :1
-- :doc 通过 patient_id 检索患者评估
SELECT patient_id, assessment_data, patient_name_pinyin, patient_name_initial, doctor_signature_b64, created_at, updated_at -- 中文注释：选取 doctor_signature_b64 列
FROM patient_assessments WHERE patient_id = :patient_id;

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

-- 用户相关操作

-- :name create-user! :! :n
-- :doc 创建一个新用户
INSERT INTO users (username, password_hash, name, role, signature_b64)
VALUES (:username, :password_hash, :name, :role, :signature_b64);

-- :name get-user-by-username :? :1
-- :doc 根据用户名获取用户信息
SELECT * FROM users WHERE username = :username;

-- :name get-user-by-id :? :1
-- :doc 根据ID获取用户信息
SELECT id, username, name, role, signature_b64, created_at, updated_at FROM users WHERE id = :id;

-- :name list-users :*
-- :doc 列出所有用户信息 (不包含密码)
SELECT id, username, name, role, signature_b64, created_at, updated_at FROM users ORDER BY created_at DESC;

-- :name update-user-password! :! :n
-- :doc 更新用户密码
UPDATE users
SET password_hash = :password_hash, updated_at = datetime('now')
WHERE id = :id;

-- :name update-user-name! :! :n
-- :doc 更新用户姓名
UPDATE users
SET name = :name, updated_at = datetime('now')
WHERE id = :id;

-- :name update-user-role! :! :n
-- :doc 更新用户角色
UPDATE users
SET role = :role, updated_at = datetime('now')
WHERE id = :id;

-- :name update-user-info! :! :n
-- :doc 更新用户姓名、角色和签名
UPDATE users
SET name = :name,
    role = :role,
    signature_b64 = :signature_b64,
    updated_at = datetime('now')
WHERE id = :id;

-- :name delete-user! :! :n
-- :doc 根据ID删除用户
DELETE FROM users WHERE id = :id;
