-- Place your queries here. Docs available https://www.hugsql.org/

-- 插入患者评估
-- :name insert-patient-assessment! :! :n
-- :doc 插入一个新的患者评估记录
INSERT INTO patient_assessments
(patient_id, assessment_data, patient_name, assessment_status, patient_name_pinyin, patient_name_initial, doctor_signature_b64, created_at, updated_at) -- 中文注释：新增姓名和状态字段
VALUES (:patient_id, :assessment_data, :patient_name, :assessment_status, :patient_name_pinyin, :patient_name_initial, :doctor_signature_b64, datetime('now'), datetime('now')); -- 中文注释：添加 doctor_signature_b64 参数

-- 更新患者评估
-- :name update-patient-assessment! :! :n
-- :doc 更新现有的患者评估记录
UPDATE patient_assessments
SET assessment_data = :assessment_data,
    patient_name = :patient_name,
    assessment_status = :assessment_status,
    patient_name_pinyin = :patient_name_pinyin,
    patient_name_initial = :patient_name_initial,
    doctor_signature_b64 = :doctor_signature_b64, -- 中文注释：添加 doctor_signature_b64 更新
    updated_at = datetime('now')
WHERE patient_id = :patient_id;

-- 根据患者ID获取患者评估
-- :name get-patient-assessment-by-id :? :1
-- :doc 通过 patient_id 检索患者评估
SELECT id, patient_id, assessment_data, patient_name, assessment_status, patient_name_pinyin, patient_name_initial, doctor_signature_b64, created_at, updated_at -- 中文注释：选取 doctor_signature_b64 列
FROM patient_assessments WHERE patient_id = :patient_id;

-- :name get-patient-assessment-by-assessment-id :? :1
-- :doc 通过评估ID检索患者评估
SELECT id, patient_id, assessment_data, patient_name, assessment_status, patient_name_pinyin, patient_name_initial, doctor_signature_b64, created_at, updated_at
FROM patient_assessments WHERE id = :assessment_id;

-- 获取所有患者评估信息
-- :name get-all-patient-assessments :? :*
-- :doc 检索所有患者的评估数据, 可选根据姓名、拼音、首字母、状态及更新时间范围进行筛选
SELECT * FROM patient_assessments
WHERE 1=1
--~ (when (:name params) " AND patient_name LIKE (:name params)")
--~ (when (:status params) " AND assessment_status = (:status params)")
--~ (when (:name_pinyin params) " AND clojure.string/lower-case(patient_name_pinyin) LIKE lower(:name_pinyin)")
--~ (when (:name_initial params) " AND clojure.string/lower-case(patient_name_initial) = lower(:name_initial params)")
--~ (when (:updated_from params) " AND updated_at >= (:updated_from params)")
--~ (when (:updated_to params) " AND updated_at <= (:updated_to params)")
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


-- 知情同意书相关操作
-- :name upsert-consent-form! :! :n
-- :doc 保存或更新评估对应的知情同意书
INSERT INTO consent_forms (assessment_id, sedation_form, pre_anesthesia_form, anesthesia_form, created_at, updated_at)
VALUES (:assessment_id, :sedation_form, :pre_anesthesia_form, :anesthesia_form, datetime('now'), datetime('now'))
ON CONFLICT(assessment_id) DO UPDATE SET
  sedation_form = excluded.sedation_form,
  pre_anesthesia_form = excluded.pre_anesthesia_form,
  anesthesia_form = excluded.anesthesia_form,
  updated_at = datetime('now');

-- :name get-consent-form-by-assessment :? :1
-- :doc 根据评估ID获取知情同意书
SELECT * FROM consent_forms WHERE assessment_id = :assessment_id;

-- :name update-sedation-consent! :! :n
-- :doc 更新镇静知情同意书内容
UPDATE consent_forms
SET sedation_form = :sedation_form,
    updated_at = datetime('now')
WHERE assessment_id = :assessment_id;

-- :name update-pre-anesthesia-consent! :! :n
-- :doc 更新术前知情同意书内容
UPDATE consent_forms
SET pre_anesthesia_form = :pre_anesthesia_form,
    updated_at = datetime('now')
WHERE assessment_id = :assessment_id;

-- :name update-anesthesia-consent! :! :n
-- :doc 更新麻醉知情同意书内容
UPDATE consent_forms
SET anesthesia_form = :anesthesia_form,
    updated_at = datetime('now')
WHERE assessment_id = :assessment_id;

-- 角色与权限相关操作

-- :name list-roles :? :*
-- :doc 获取所有角色列表
SELECT * FROM roles ORDER BY id;

-- :name create-role! :! :n
-- :doc 创建新角色
INSERT INTO roles (name) VALUES (:name);

-- :name update-role-name! :! :n
-- :doc 更新角色名称
UPDATE roles SET name = :name WHERE id = :id;

-- :name delete-role! :! :n
-- :doc 删除角色
DELETE FROM roles WHERE id = :id;

-- :name list-permissions :? :*
-- :doc 获取权限列表
SELECT * FROM permissions ORDER BY id;

-- :name get-permissions-by-role :? :*
-- :doc 根据角色获取权限
SELECT p.* FROM permissions p
JOIN role_permissions rp ON p.id = rp.permission_id
WHERE rp.role_id = :role_id;

-- :name delete-role-permissions! :! :n
-- :doc 删除角色的所有权限
DELETE FROM role_permissions WHERE role_id = :role_id;

-- :name add-role-permission! :! :n
-- :doc 为角色添加权限
INSERT INTO role_permissions (role_id, permission_id)
VALUES (:role_id, :permission_id);
