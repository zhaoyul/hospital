# AGENT INSTRUCTIONS: hospital 项目

## 1. 项目目标

本项目是一个基于 Clojure Kit 框架的门诊手术麻醉系统。核心功能包括患者端问卷填写、医生端数据查看与编辑、知情同意书生成，以及后台管理。AI 代理的任务是协助开发和维护此系统。

## 2. 核心技术栈

在生成任何代码或提供建议时，必须严格遵循以下技术栈：

- **后端:** Clojure, Kit Framework, Integrant, Ring, Reitit
- **前端:** ClojureScript, Reagent, Re-frame, Shadow-cljs
- **UI 组件库:** Ant Design
- **数据库:**
    - 开发/测试: SQLite
    - 生产: 任何兼容 JDBC 的数据库 (代码需通用)
    - 交互: Conman, HugSQL
    - 迁移: Migratus
- **认证:** Buddy Auth
- **中文处理:** HanLP
- **构建/依赖:** Clojure CLI (`deps.edn`), Node.js/Yarn (`package.json`)

## 3. 通用规则与约定

- **语言:** 所有代码注释、文档字符串 (docstrings) 和提交信息都必须使用 **简体中文**。
- **编码风格:**
    - 严格遵循 [Clojure 社区风格指南](https://guide.clojure.style/)。
    - 优先使用纯函数和不可变数据结构。避免不必要的 `atom`、`ref` 等。
    - 命名空间和文件名使用 `snake_case`。
- **开发工作流:**
    - 后端开发在 REPL 中进行，使用 `(go)`, `(reset)`, `(halt)`。
    - 前端开发使用 `npx shadow-cljs watch <app-name>` 进行实时编译。
- **依赖管理:**
    - Clojure 依赖通过在 `deps.edn` 中添加 `lib/version` 键值对来管理。
    - Javascript 依赖通过 `yarn add <package>` 管理。
- **测试:**
    - 所有新功能或 bug 修复都应伴随相应的测试。
    - 如果修改了前端代码，请在生成 PR 之前先运行 `yarn install` 确认依赖完整，再执行 `npx shadow-cljs compile app`，确保编译顺利完成。
    - 运行后端测试的命令是 `clj -M:test`。
    - **注意:** `readme.org` 中已指出当前测试套件存在已知失败。在修复相关模块前，请不要尝试“修复”这些已知的测试失败。

## 4. 关键配置文件

在进行修改时，请特别注意以下核心配置文件：

- `deps.edn`: Clojure 依赖与别名。
- `resources/system.edn`: Integrant 系统定义，是整个应用的粘合剂。
- `shadow-cljs.edn`: ClojureScript 构建配置，定义了 `:app` 和 `:patient-app` 两个目标。
- `kit.edn`: Kit 框架模块配置。
- `build.clj`: Uberjar 构建脚本。
- `resources/html/report/`: 存放麻醉/镇静知情同意书模板，对应路由在 `web/routes/report_pages.clj` 中定义。

## 5. 提交和 PR 流程

在修改 `src/clj` 或 `resources` 目录下的后端代码后，必须遵循以下步骤：

1. 执行 `clj -M:test` 运行所有后端测试。
2. 仅当测试全部通过时，才可生成并提交 Pull Request。
3. 如因已知的失败测试导致测试未通过，应在说明中提及，不要尝试修复这些测试。

