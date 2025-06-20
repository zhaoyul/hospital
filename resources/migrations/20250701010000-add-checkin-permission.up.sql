INSERT INTO permissions (module, action)
SELECT '签到登记', 'view'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE module='签到登记' AND action='view');
--;;
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name='护士' AND p.module='签到登记' AND p.action='view'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
