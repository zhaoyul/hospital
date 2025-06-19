INSERT INTO roles (name)
SELECT '护士'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name='护士');
--;;
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name='护士' AND p.module IN ('纵览信息','问卷列表')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
--;;
INSERT INTO users (username, password_hash, name, role, created_at, updated_at)
SELECT 'nurse1',
       'bcrypt+sha512$1d088d680ba16c5088e0558230ea62d2$12$50c51211700cc4a74350e1b60f2a04ab022f741da17534ca',
       '示例护士',
       '护士',
       datetime('now'),
       datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='nurse1');
