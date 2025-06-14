CREATE TABLE users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  name TEXT,
  role TEXT DEFAULT '麻醉医生',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
--;;
-- 插入默认管理员账号
INSERT INTO users (username, password_hash, name, role, created_at, updated_at)
VALUES (
    'admin',
    'bcrypt+sha512$1d088d680ba16c5088e0558230ea62d2$12$50c51211700cc4a74350e1b60f2a04ab022f741da17534ca',
    '默认管理员',
    '管理员',
    datetime('now'),
    datetime('now')
);
--;;

-- 插入示例医生账号
INSERT INTO users (username, password_hash, name, role, created_at, updated_at)
VALUES (
    'doctor1',
    'bcrypt+sha512$1d088d680ba16c5088e0558230ea62d2$12$50c51211700cc4a74350e1b60f2a04ab022f741da17534ca',
    '示例医生',
    '麻醉医生',
    datetime('now'),
    datetime('now')
);
--;;
