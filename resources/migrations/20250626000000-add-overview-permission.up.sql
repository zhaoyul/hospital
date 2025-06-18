INSERT INTO permissions (module, action)
SELECT '纵览信息','view'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE module='纵览信息' AND action='view');
--;;
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE p.module='纵览信息' AND r.name IN ('管理员','麻醉医生')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
