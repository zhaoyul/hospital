# AGENT INSTRUCTIONS: Backend (Clojure)

## 1. 架构概览

本目录包含所有后端 Clojure 代码。系统基于 Integrant 进行组件化管理，并使用 Reitit 进行路由。

## 2. 编码规则

- **路由:**
    - 新的 API 端点或页面路由应添加到 `web/routes/` 目录下的相应文件中。
    - API 路由返回 JSON，页面路由使用 Selmer 渲染 HTML。
- **业务逻辑:**
    - 请求处理的业务逻辑应放在 `web/controllers/` 目录中。保持控制器函数的职责单一。
- **数据库交互:**
    - **严禁** 在业务逻辑代码中拼接 SQL 字符串。
    - 所有 SQL 查询必须在 `resources/sql/queries.sql` 中使用 HugSQL 定义。
    - 在 Clojure 代码中通过 Conman 提供的函数调用这些已定义的查询。
- **组件:**
    - 如果需要添加新的常驻服务或组件（如数据库连接池、定时任务等），应在 `resources/system.edn` 中定义，并实现相应的 `integrant.core/init-key` 和 `halt-key!` 方法。
- **配置:**
    - 应用配置通过 `config.clj` 加载，并优先从环境变量读取。
- **中间件:**
    - 需要对请求/响应进行通用处理（如认证、格式化、异常捕获），应在 `web/middleware/` 目录下创建或修改中间件。
