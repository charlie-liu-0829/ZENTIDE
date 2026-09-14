# 知潮 ZENTIDE

知潮（ZENTIDE）是面向兴趣社群的内容社区与智能协作平台。用户可以创建兴趣现场、发布和讨论内容，并使用 AI 问答、发帖检查、兴趣推荐与社区治理建议。

## 核心功能

- 兴趣现场：创建、加入、邀请与成员管理
- 内容社区：富文本帖子、话题、图片/视频、评论、点赞、收藏与关注
- 智能能力：现场问答、内容检索、发帖检查、兴趣情报和治理建议
- 管理后台：现场、内容、成员、话题与治理规则管理

## 项目组成

```text
frontend/web          用户端（Vue）       http://localhost:6001
frontend/admin         管理端（Vue）       http://localhost:6002
backend/zentide-web   用户 API            http://localhost:6050/api
backend/zentide-admin 管理 API            http://localhost:6061/api
backend/agent         Python Agent（8090–8093）
```

## 快速开始

### 环境要求

JDK 21、Node.js 22、Python 3.11、MySQL 8、Redis 7。Docker 可用于启动 MySQL 和 Redis。

### 配置

在项目根目录复制配置模板并填写真实值：

```bash
cp .env.example .env
```

至少配置 MySQL 密码、管理员密码、`DASHSCOPE_API_KEY`，并为 `ZENTIDE_AGENT_INTERNAL_TOKEN` 设置随机长字符串。`.env` 不会提交到 GitHub。

Chat Agent 的知识快照路径已固定写在 `backend/agent/src/main/python/chat/config/agent.yml`，不需要配置 `ZENTIDE_KNOWLEDGE_SNAPSHOT_DIR`。如果把项目移动到其他目录，请修改该文件中的两个绝对路径，使其指向项目的 `data/knowledge-snapshots` 和 `data/.snapshot-md5.json`。

### 启动基础服务

```bash
docker compose up -d mysql redis
mysql -u root -p < zentide.sql   # 仅首次初始化空数据库
```

### 启动全部 Agent

以下命令必须在项目根目录（包含 `Makefile` 的 `ZENTIDE` 目录）执行：

```bash
set -a && source .env && set +a
python3 -m venv .venv-agents
.venv-agents/bin/python -m pip install -r backend/agent/src/main/python/requirements-agents.txt
export ZENTIDE_AGENT_PYTHON="$(pwd)/.venv-agents/bin/python"
make agent-run
```

该命令会启动 chat（8090）、SmartPosting（8091）、Recommend（8092）和 Governance（8093）。按 `Ctrl-C` 可全部停止。不在根目录时可执行：

```bash
make -C /Users/charlieliu/Desktop/java/ZENTIDE agent-run
```

### 启动后端

另开终端，加载 `.env` 后分别启动用户端和管理端：

```bash
cd /Users/charlieliu/Desktop/java/ZENTIDE
set -a && source .env && set +a
cd backend && ./mvnw -f zentide-web/pom.xml spring-boot:run
```

```bash
cd /Users/charlieliu/Desktop/java/ZENTIDE
set -a && source .env && set +a
cd backend && ./mvnw -f zentide-admin/pom.xml spring-boot:run
```

### 启动前端

```bash
cd /Users/charlieliu/Desktop/java/ZENTIDE/frontend
nvm use
npm run install:all
npm run dev
```

访问：[用户端](http://localhost:6001) · [管理端](http://localhost:6002)

## 技术栈

Java 21、Spring Boot、MyBatis、MySQL、Redis、Vue 3、Vite、Python、FastAPI、LangGraph。

## 目录

```text
backend/       Java API、公共模块和 Agent
frontend/      用户端与管理端
data/          运行时生成的知识快照
scripts/       启动及辅助脚本
zentide.sql    数据库结构快照
```

## 安全提示

请勿提交 `.env`、数据库密码、API Key、Agent Token 或本地运行数据。仓库中的 `.env.example` 仅为配置模板。

## 许可证

当前项目尚未声明开源许可证。如需使用或二次分发，请先联系作者确认授权。
