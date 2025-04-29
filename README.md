# hospital

一个使用 Kit 框架构建的 Web 应用程序。

## 先决条件

在开始之前, 请确保你已经安装了以下软件:

-   Java Development Kit (JDK) 11 或更高版本 (推荐 OpenJDK)
-   Clojure CLI 工具 (按照 Kit 文档的说明安装)
-   Node.js 和 Yarn (或 npm) (用于 ClojureScript 部分)

## 快速开始

1.  **克隆仓库** (如果需要)

2.  **安装 Clojure 依赖**: Clojure CLI 会自动处理 (`deps.edn`)

3.  **安装 Javascript 依赖**:

    ```bash
    yarn install # 或者 npm install
    ```

4.  **启动 Clojure REPL**: 在你的编辑器 (如 CIDER, Cursive) 或终端中启动
    REPL。推荐的命令:

    ```bash
    # 通用开发 REPL
    clj -M:dev

    # 或者使用 nREPL (适用于 Calva, Cursive 或终端)
    clj -M:dev:nrepl
    # 或者 make repl

    # 或者使用 CIDER nREPL (适用于 Emacs CIDER)
    clj -M:dev:cider
    ```

    **注意**: 请参考下面的 [*编辑器设置*]{.spurious-link
    target="*编辑器设置"} 部分以获取 Emacs/CIDER 的特定配置。

5.  **启动 ClojureScript 编译器**: 在一个单独的终端中运行:

    ```bash
    npx shadow-cljs watch app
    ```

    这将以监听模式编译 ClojureScript 代码。

6.  **启动应用程序**: 在 Clojure REPL 中运行:

    ```clojure
    (go)
    ```

7.  **访问应用**:

    -   前端页面: <http://localhost:3000>
    -   API 端点 (健康检查): <http://localhost:3000/api/health>

## 开发工作流

-   **启动/重启应用**: 在 Clojure REPL 中使用:

    ``` clojure
    (go)    ; 启动系统
    (reset) ; 应用代码更改后重新加载系统
    (halt)  ; 停止系统
    ```

-   **ClojureScript REPL**:

    -   启动 ClojureScript 编译器后 (`npx shadow-cljs watch app`),
        连接到 7002 端口的 nREPL。
    -   在 REPL 中运行 `(shadow.cljs.devtools.api/repl :app)`
        连接到浏览器。
    -   测试连接: `(js/alert "Hello from CLJS!")`

## 项目结构 (hc/hospital)

``` text
├── Dockerfile             # 用于构建 Docker 镜像
├── Makefile               # 常用开发命令快捷方式
├── README.md              # (英文) 简要说明
├── bb.edn                 # Babashka 任务配置
├── build.clj              # Clojure Tools Build 构建脚本
├── deps.edn               # Clojure 项目依赖和配置
├── env                    # 环境特定配置
│   ├── dev                # 开发环境
│   │   ├── clj
│   │   │   ├── hc/hospital # 开发环境特定 Clojure 代码
│   │   │   └── user.clj    # REPL 辅助函数
│   │   └── resources       # 开发环境特定资源 (如 logback.xml)
│   ├── prod               # 生产环境
│   │   ├── clj            # 生产环境特定 Clojure 代码
│   │   └── resources       # 生产环境特定资源
│   └── test               # 测试环境资源
├── kit.edn                # Kit 框架模块配置
├── package.json           # Node.js 依赖 (用于 CLJS)
├── readme.org             # 本文件
├── resources              # 应用资源
│   ├── html               # Selmer HTML 模板
│   ├── public             # 静态文件 (CSS, JS, 图片)
│   ├── sql                # SQL 查询 (HugSQL)
│   └── system.edn         # Integrant 系统配置
├── shadow-cljs.edn        # Shadow-cljs 编译器配置
├── src                    # 源代码
│   ├── clj                # Clojure 源代码 (后端)
│   │   └── hc/hospital
│   │       ├── config.clj # 配置加载
│   │       ├── core.clj   # 应用入口 (启动/停止)
│   │       └── web        # Web 相关代码
│   │           ├── controllers # 控制器 (业务逻辑处理)
│   │           ├── handler.clj # Ring/Reitit 处理程序设置
│   │           ├── middleware  # 中间件
│   │           ├── pages       # 页面布局 (Selmer)
│   │           └── routes      # 路由定义 (Reitit)
│   └── cljs               # ClojureScript 源代码 (前端)
│       └── hc/hospital
│           ├── components # Reagent/Ant Design 组件
│           ├── core.cljs  # CLJS 应用入口
│           ├── db.cljs    # Re-frame 应用数据库 schema
│           ├── events.cljs # Re-frame 事件处理器
│           ├── pages      # 页面视图组件
│           └── subs.cljs   # Re-frame 订阅
├── test                   # 测试代码
│   └── clj                # Clojure 测试
│       └── hc/hospital
└── yarn.lock              # Yarn 锁定文件
```

## 配置

-   **系统配置**: 主要配置文件是 `resources/system.edn`。它使用
    Integrant 定义系统组件 (服务器, 数据库连接, 路由等) 并通过 Aero
    读取配置值。
-   **环境变量**: 配置文件可以使用 `#env`
    标签读取环境变量。重要的环境变量包括:
    -   `PORT`: HTTP 服务器端口 (默认 3000)。
    -   `COOKIE~SECRET~`: 会话 Cookie 的密钥 (默认
        "ARHWQVLGPBGEFRZW")。*生产环境中必须更改此值!*
    -   `JDBC~URL~`: 生产数据库的连接 URL (开发/测试环境使用 SQLite
        文件)。
-   **环境特定配置**: `env/dev` 和 `env/prod`
    目录包含特定于环境的配置和代码, 例如 `env.clj` 和
    `dev~middleware~.clj`。

## 数据库

-   **类型**: 使用 SQLite 进行开发和测试, 生产环境配置为读取
    `JDBC~URL~` 环境变量 (预期为兼容 JDBC 的数据库, 如 PostgreSQL)。
-   **交互**: 使用 HugSQL 定义 SQL 查询 (`resources/sql/queries.sql`),
    通过 Conman 执行。
-   **迁移**: 使用 Migratus 管理数据库 schema 迁移。迁移脚本位于
    `resources/migrations` (如果存在)。默认在启动时自动运行迁移
    (`:migrate-on-init? true`)。

## 测试

-   **运行测试**:

    ``` {.bash org-language="sh"}
    clj -M:test
    # 或者
    make test
    ```

-   **测试工具**: 使用 `clojure.test`。测试工具函数位于
    `test/clj/hc/hospital/test~utils~.clj`。

-   **测试环境**: 测试默认使用 `:test` profile, 其配置 (如数据库连接)
    在 `system.edn` 中定义。

## 构建和部署

-   **构建 Uberjar**:

    ``` {.bash org-language="sh"}
    clj -T:build all
    # 或者
    make uberjar
    ```

    这会生成一个包含所有依赖的独立 jar 包
    (`target/hospital-standalone.jar`)。

-   **运行 Uberjar**:

    ``` {.bash org-language="sh"}
    # 需要设置生产环境所需的环境变量
    export JDBC_URL="<your_production_database_url>"
    export COOKIE_SECRET="<your_strong_production_secret>"
    export PORT=8080 # (可选)
    java -jar target/hospital-standalone.jar
    ```

-   **Docker**: 项目包含一个 `Dockerfile`, 可以用于构建 Docker
    镜像进行容器化部署。

## 前端 (ClojureScript)**

-   **编译器**: Shadow-cljs
-   **框架**: Reagent 和 Re-frame
-   **UI 库**: Ant Design (通过
    `src/cljs/hc/hospital/components/antd.cljs` 封装)
-   **开发构建**: `npx shadow-cljs watch app`
-   **生产构建**: 由 `clj -T:build all` 触发 (`build.clj` 中的
    `build-cljs` 函数)。
-   **依赖管理**: 使用 `package.json` (通过 Yarn/npm 管理 JS 库) 和
    `shadow-cljs.edn` (管理 CLJS 库)。

## 编辑器设置**

### Emacs + CIDER

为了让 CIDER 正确识别开发和测试的源路径和别名,
在项目根目录创建或确保存在 `.dir-locals.el` 文件, 内容如下:

``` elisp
((clojure-mode . ((cider-preferred-build-tool . clojure-cli)
                  (cider-clojure-cli-aliases . ":dev:test"))))
```

## 主要技术栈

-   Clojure / ClojureScript
-   Kit Framework
-   Ring / Reitit (路由)
-   Undertow (Web 服务器)
-   Integrant (组件管理)
-   Selmer (HTML 模板 - 后端)
-   Shadow-cljs (CLJS 构建)
-   Reagent / Re-frame (CLJS UI)
-   Ant Design (UI 组件库)
-   HugSQL / Conman / Migratus (SQL 数据库交互)
-   SQLite (开发/测试数据库)

