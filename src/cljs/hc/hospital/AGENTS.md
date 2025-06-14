# AGENT INSTRUCTIONS: Frontend (ClojureScript)

## 1. 架构概览

本目录包含两个独立的 Re-frame 前端应用：

1.  **:app** (医生管理端): 位于 `src/cljs/hc/hospital/` 下的 `core.cljs`, `db.cljs`, `events.cljs`, `subs.cljs`, `pages/` 等。
2.  **:patient-app** (患者问卷端): 位于 `src/cljs/hc/hospital/patient/` 目录下。

在修改代码时，必须明确目标是哪个应用，并修改对应的文件。

## 2. 编码规则 (Re-frame)

- **状态管理:**
    - 应用的全局状态 schema 定义在 `db.cljs` (或 `patient/db.cljs`) 中。
- **事件 (Events):**
    - **更新状态:** 所有对应用状态的修改 **必须** 通过 `reg-event-db` 或 `reg-event-fx` 定义的事件来完成。事件定义在 `events.cljs` (或 `patient/events.cljs`)。
    - **副作用:** API 调用等副作用应放在 `-fx` 事件处理器中。
- **订阅 (Subscriptions):**
    - UI 组件 **必须** 通过 `reg-sub` 定义的订阅来获取数据。订阅定义在 `subs.cljs` (或 `patient/subs.cljs`)。这能确保数据流的响应性和可追溯性。
- **视图 (Views):**
    - UI 组件使用 Reagent 编写。
    - **医生端页面** 位于 `pages/` 目录下。
    - **患者端视图** 位于 `patient/views.cljs`。
- **UI 组件:**
    - 优先使用 `components/antd.cljs` 中已封装好的 Ant Design 组件。
    - 若需创建新的、可复用的表单组件或 UI 元素，应将其添加到 `components/form_components.cljs` 或其他合适的 `components` 命名空间下。

## 3. Storybook 使用说明

- 项目已集成 Storybook，便于在浏览器中预览和调试 UI 组件。
- 如需展示 ClojureScript 组件，请在 `src/cljs/hc/hospital/stories/` 下创建示例文件，并在 `src/stories/` 中编写对应的 `*.stories.js(x)`。
- Storybook 脚本会先运行 `shadow-cljs compile storybook`，生成 `target/storybook/stories.js`。
- 首次运行前请执行 `yarn install` 安装依赖（包含提供 `storybook` 命令的 `@storybook/cli`）。
- 随后可通过 `yarn storybook` 启动本地预览（默认端口 6006），或执行 `yarn build-storybook` 构建静态站点。
