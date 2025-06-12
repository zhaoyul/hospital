# AGENT INSTRUCTIONS: HugSQL Queries

## 1. 工具

本目录下的 `.sql` 文件使用 `HugSQL` 进行解析。

## 2. 规则

- **查询定义:**
    - 每个 SQL 查询 **必须** 有一个元数据头部，用于定义其名称和类型。
    - `-- :name my-query-name :?`  (查询单条记录, a map)
    - `-- :name my-query-name :*`  (查询多条记录, a vector of maps)
    - `-- :name my-query-name :!`  (执行 DML/DDL, returns count)
- **参数:**
    - 使用 `:param_name` 的形式来表示参数。
- **语法:**
    - SQL 语法需要与开发/测试环境的 SQLite 兼容。
- **组织:**
    - 将相关的查询（例如，所有与 `doctors` 表相关的查询）放在一起，并使用注释进行分组。
