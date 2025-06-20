DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE module='系统管理' AND action IN ('view-users','create-user','edit-user','delete-user','view-roles','edit-role'));
--;;
DELETE FROM permissions WHERE module='系统管理' AND action IN ('view-users','create-user','edit-user','delete-user','view-roles','edit-role');
