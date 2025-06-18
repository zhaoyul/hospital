DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE module='纵览信息' AND action='view');
--;;
DELETE FROM permissions WHERE module='纵览信息' AND action='view';
