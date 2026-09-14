# 知潮 ZENTIDE

知潮（ZENTIDE）是一个面向兴趣社群的内容交流与智能协作平台。用户可以创建或加入兴趣现场，发布富文本帖子，关联话题，参与评论、点赞、收藏、关注和私信；平台通过独立 Agent 提供现场问答、智能发帖检查、个性化兴趣情报和社区治理能力。

## 功能概览

- 兴趣现场：创建、审核、邀请、成员申请和成员管理
- 内容社区：富文本帖子、图片/视频、话题、评论、点赞、收藏和关注
- 内容检索：社区搜索、话题聚合、知识快照和证据引用
- AI 能力：现场/帖子问答、智能发帖检查、兴趣情报和治理建议
- 运营后台：兴趣现场、帖子、评论、成员、话题、帖子类型和治理规则管理

## 系统架构

```text
Vue 用户端 :6001 ──▶ Spring Boot Web :6050 ──▶ Python Agent：8090-8093
Vue 管理端 :6002 ──▶ Spring Boot Admin :6061 ──▶ MySQL + Redis
```

Agent 只接受 Spring Boot 服务端调用，浏览器不会直接连接 Agent。用户身份、兴趣现场范围和内容可见性由后端校验后透传。

## 目录结构

```text
backend/
├── zentide-common/              公共模型、Mapper、服务和数据库迁移
├── zentide-web/                 用户端 API，默认端口 6050
├── zentide-admin/               管理端 API，默认端口 6061
└── agent/                       MCP 适配器和 Python Agent
    └── src/main/python/
        ├── chat/                社区助手，默认端口 8090
        ├── SmartPosting/        智能发帖检查，默认端口 8091
        ├── Recommend/           个性化兴趣情报，默认端口 8092
        ├── Governance/          社区治理，默认端口 8093
        └── shared/              Agent 公共能力

frontend/
├── web/                         用户端，默认端口 6001
└── admin/                       管理端，默认端口 6002

data/knowledge-snapshots/       已发布帖子的 Markdown 知识快照
scripts/                         启动、演示数据和数据库校验脚本
docs/                            数据库运维文档
zentide.sql                     全新数据库结构快照
```

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.5、MyBatis、MySQL 8、Redis、Flyway、Maven |
| 前端 | Vue 3、Vite、Pinia、Element Plus、Tiptap |
| Agent | Python 3.11、FastAPI、LangChain、LangGraph、Chroma、DashScope |
| 工程化 | Docker Compose、GitHub Actions、Vitest、Playwright |

## 环境要求

- JDK 21
- Maven 3.9.11（推荐使用 `backend/mvnw`）
- Node.js 22.14.0、npm 10.9.2
- MySQL 8.0+
- Redis 7+
- Python 3.11

版本由 `.java-version`、`.tool-versions`、`frontend/.nvmrc` 和项目配置文件锁定。

## 环境变量

仓库只提供 `.env.example`，本地复制为 `.env` 后加载：

```bash
cp .env.example .env
set -a && source .env && set +a
```

至少配置数据库、Redis、Agent Token 和 DashScope API Key：

```env
ZENTIDE_DB_USERNAME=root
ZENTIDE_DB_PASSWORD=your-mysql-password
ZENTIDE_DB_ROOT_PASSWORD=your-mysql-root-password
ZENTIDE_REDIS_HOST=127.0.0.1
ZENTIDE_REDIS_PORT=6379
ZENTIDE_AGENT_INTERNAL_TOKEN=replace-with-a-random-long-string
DASHSCOPE_API_KEY=your-dashscope-api-key
ZENTIDE_ADMIN_ACCOUNT=admin
ZENTIDE_ADMIN_PASSWORD=change-me-now
```

`.env`、真实密码、API Key 和本地运行数据不会提交到 GitHub。

## 启动项目

所有命令均从项目根目录执行。

### 1. 启动 MySQL 和 Redis

可以使用本机服务，也可以使用 Docker：

```bash
docker compose up -d mysql redis
```

初始化全新数据库：

```bash
mysql -u root -p < zentide.sql
```

`zentide.sql` 只用于全新或空数据库。已有数据库升级时，使用 `backend/zentide-common/src/main/resources/db/migration/` 中的 Flyway 迁移。

### 2. 安装并启动全部 Agent

Agent 与用户端、管理端独立运行。一次启动四个 Agent：

```bash
python3 -m venv .venv-agents
.venv-agents/bin/python -m pip install --upgrade pip
.venv-agents/bin/python -m pip install -r backend/agent/src/main/python/requirements-agents.txt

export ZENTIDE_AGENT_PYTHON="$(pwd)/.venv-agents/bin/python"
make agent-run
```

`make agent-run` 会启动 chat（8090）、SmartPosting（8091）、Recommend（8092）和 Governance（8093）。按 `Ctrl-C` 会统一停止全部 Agent。需要单独调试时，可使用 `make post-review-run`、`make recommend-run` 或 `make governance-run`。

### 3. 启动 Java 后端

```bash
cd backend
./mvnw -f zentide-web/pom.xml spring-boot:run
```

另开终端启动管理端：

```bash
cd backend
./mvnw -f zentide-admin/pom.xml spring-boot:run
```

Web/Admin 不会自动启动或停止 Agent，只调用已经运行的 Agent 服务。

### 4. 启动前端

```bash
cd frontend
nvm use
npm run install:all
npm run dev
```

也可以单独启动某一端：`npm run dev:web` 或 `npm run dev:admin`。

## 常用命令

```bash
make install       # 安装前端依赖
make agent-install # 安装全部 Agent 依赖
make agent-run     # 启动全部 Agent
make test          # 后端和前端单元测试
make lint          # 前端 ESLint
make build         # 后端和前端构建
make verify        # lint、格式检查、测试和构建
make schema-check  # 校验数据库快照与 Flyway 结构
make e2e           # Playwright E2E 测试
make ci            # schema-check + verify + e2e
```

## 默认地址

| 模块 | 地址 |
| --- | --- |
| 用户端 | http://localhost:6001 |
| 管理端 | http://localhost:6002 |
| 用户端 API | http://localhost:6050/api |
| 管理端 API | http://localhost:6061/api |
| chat Agent | http://127.0.0.1:8090 |
| SmartPosting Agent | http://127.0.0.1:8091 |
| Recommend Agent | http://127.0.0.1:8092 |
| Governance Agent | http://127.0.0.1:8093 |

## 测试与 CI

提交前建议执行：

```bash
make ci
```

该命令会执行数据库结构校验、Java 测试与构建、前端 lint/格式检查/单元测试、前端生产构建和 Playwright E2E 测试。GitHub Actions 使用同一套命令进行持续集成。

## 数据与安全边界

- `zentide.sql`：最终数据库结构快照，可提交到仓库。
- `data/knowledge-snapshots/`：Agent 检索所需的已发布内容快照。
- `.env`、数据库密码、模型 API Key、Agent Token：禁止提交。
- `data/file/`、`data/logs/`、本地 SQLite/Chroma 数据库、虚拟环境、`node_modules`、`target` 和测试报告：本地运行产物，禁止提交。
- Agent 只返回后端权限范围内的内容；最终治理动作由管理员确认。
