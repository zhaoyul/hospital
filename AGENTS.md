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
    - 文件名使用 `snake_case`, 对应的命名空间为`sanke-case`。
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
    - **React Hook 组件调用:** 若组件内部使用 React 的 Hook（如 `useEffect`、`useState`），调用该组件时必须使用 `[:f> my-component]` 形式。
    - **Hook 组件状态管理:** 当组件使用 React Hooks 管理状态时，避免再使用 `reagent/atom`，统一采用 React 的 `useState` 等 API。
    - **命名空间引用:** 新建的 Clojure 命名空间, 如果有状态 `defmethod ig/init-key :reitit.routes/patient-api` 可能需要在 `core.clj` 的 `:require` 列表中显式引入，否则不会编译。

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

## 6. 环境初始化

在全新环境中启动项目前，请先确保已安装 Clojure CLI。可在仓库根目录依次执行以下命令：

```bash
curl -L -O https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh
chmod +x linux-install.sh
sudo ./linux-install.sh
```

安装完成后运行 `clj -h` 验证是否安装成功。

若需要通过代理下载依赖，请按照以下步骤配置：

```bash
mkdir -p ~/.m2
cat > ~/.m2/settings.xml <<'EOF'
<settings>
  <proxies>
    <proxy>
      <id>codexProxy</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>proxy</host>
      <port>8080</port>
    </proxy>
  </proxies>
</settings>
EOF

```
在初始化完成后，请先运行 `npm install` 安装所有 NPM 依赖，然后执行以下命令下载 ClojureScript 编译所需的 jar 包：

```bash
npx shadow-cljs deps
```

该命令会根据 `shadow-cljs.edn` 的配置解析依赖并下载相应的 jar 包，确保编译环境完整。
务必将其写入项目的 `setup` 脚本，在环境初始化阶段自动执行，以避免缺失依赖导致的编译失败。

同时，为了加快后续启动速度，还应在 `setup` 阶段使用 `clj -P` 预先下载各个 profile 的依赖。
示例命令如下：

```bash
clj -M:dev -P
clj -M:test -P
clj -M:lint -P
clj -M:nrepl -P
```

这些命令会分别准备 `:dev`、`:test`、`:lint` 与 `:nrepl` profile 需要的库，避免运行时再解析依赖。

下面给出 `setup` 脚本的示例实现，供参考：

```bash
#!/usr/bin/env bash

set -euo pipefail

# 设置 Java 代理
export JAVA_TOOL_OPTIONS='-Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080'

# git 代理
git config --global http.proxy http://127.0.0.1:8080
git config --global https.proxy http://127.0.0.1:8080

# 安装前端依赖
yarn install

# 预先下载 Clojure 各 profile 所需依赖
clj -M:dev -P
clj -M:test -P
clj -M:lint -P
clj -M:nrepl -P

# 下载 ClojureScript 编译所需的 jar 包
npx shadow-cljs deps
```


## 7. 权限与模块说明

系统使用 `roles`、`permissions` 及 `role_permissions` 三张表进行权限控制。`users` 表的 `role` 字段决定用户的角色。迁移脚本预置了 “管理员” 和 “麻醉医生” 两个角色。

`permissions` 表中的 `module` 列表示功能模块，`action` 列表示具体操作。目前系统包含以下模块：
- **纵览信息**：统计概览与图表展示，对应页面位于 `src/cljs/hc/hospital/pages/overview.cljs`。
- **麻醉管理**：医生补充患者填写的评估报告并决定麻醉适应性，主要界面在 `src/cljs/hc/hospital/pages/anesthesia.cljs`。
- **问卷列表**：查看患者问卷记录，页面在 `src/cljs/hc/hospital/pages/questionnaire.cljs`。
- **系统管理**：维护用户、角色及权限，相关页面见 `src/cljs/hc/hospital/pages/role_settings.cljs`。

新增模块或权限时，需要编写新的数据库迁移，在 `permissions` 和 `role_permissions` 中插入记录，并同步更新前端的权限树。

