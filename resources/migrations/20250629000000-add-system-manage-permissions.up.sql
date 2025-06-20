INSERT INTO permissions (module, action)
SELECT '系统管理', 'view-users'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE module='系统管理' AND action='view-users');
--;;
INSERT INTO permissions (module, action)
SELECT '系统管理', 'create-user'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE module='系统管理' AND action='create-user');
--;;
INSERT INTO permissions (module, action)
SELECT '系统管理', 'edit-user'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE module='系统管理' AND action='edit-user');
--;;
INSERT INTO permissions (module, action)
SELECT '系统管理', 'delete-user'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE module='系统管理' AND action='delete-user');
--;;
INSERT INTO permissions (module, action)
SELECT '系统管理', 'view-roles'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE module='系统管理' AND action='view-roles');
--;;
INSERT INTO permissions (module, action)
SELECT '系统管理', 'edit-role'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE module='系统管理' AND action='edit-role');
