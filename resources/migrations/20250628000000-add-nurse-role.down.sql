DELETE FROM users WHERE username='nurse1';
--;;
DELETE FROM role_permissions WHERE role_id IN (SELECT id FROM roles WHERE name='护士');
--;;
DELETE FROM roles WHERE name='护士';
