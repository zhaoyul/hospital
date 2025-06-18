CREATE TABLE IF NOT EXISTS roles (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT UNIQUE NOT NULL,
  description TEXT
);
--;;
CREATE TABLE IF NOT EXISTS permissions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  module TEXT NOT NULL,
  action TEXT,
  UNIQUE(module, action)
);
--;;
CREATE TABLE IF NOT EXISTS role_permissions (
  role_id INTEGER NOT NULL,
  permission_id INTEGER NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  FOREIGN KEY(role_id) REFERENCES roles(id) ON DELETE CASCADE,
  FOREIGN KEY(permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);
--;;
INSERT INTO roles (name) VALUES ('管理员'),('麻醉医生');
--;;
INSERT INTO permissions (module, action) VALUES
('麻醉管理','view'),
('问卷列表','view'),
('系统管理','view');
--;;
INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id FROM roles, permissions WHERE roles.name='管理员';
--;;
INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id FROM roles, permissions
WHERE roles.name='麻醉医生' AND permissions.module IN ('麻醉管理','问卷列表');
