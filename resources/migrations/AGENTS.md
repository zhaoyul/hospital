# AGENT INSTRUCTIONS: Database Migrations

## 1. 工具

本目录使用 `Migratus` 管理数据库 schema 迁移。

## 2. 规则

- **文件名:**
    - 创建新的迁移文件时，文件名必须遵循 `YYYYMMDDHHMMSS-descriptive-name.up.sql` 的格式。时间戳要确保唯一且按时间顺序排列。
- **原子性:**
    - 每个迁移文件应只包含一个逻辑上的、原子的 DDL 变更（如创建一个表、添加一个字段）。
- **回滚:**
    - 对于每一个 `.up.sql` 文件，都 **必须** 创建一个对应的 `.down.sql` 文件，用于撤销该次变更。
- **幂等性:**
    - 迁移脚本应尽可能编写为可重复执行而不会出错（例如，使用 `CREATE TABLE IF NOT EXISTS`）。
- **添加默认用户:**
    - 如需添加新用户，应遵循 `readme.org` 中描述的流程：在 REPL 中使用 `buddy.hashers/derive` 生成密码哈希，然后将 `INSERT` 语句放入新的迁移文件中。
