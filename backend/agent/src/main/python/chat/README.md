<div align="center">

# LangChain ReAct Agent · 智能客服

**基于 LangChain + ReAct 范式 + RAG 检索增强的智能客服系统，并支持从兴趣现场 Markdown 快照进行问答**

[![Python](https://img.shields.io/badge/Python-3.10+-blue)](https://www.python.org/)
&nbsp;
[![LangChain](https://img.shields.io/badge/LangChain-0.3-green)](https://www.langchain.com/)
&nbsp;
[![LangGraph](https://img.shields.io/badge/LangGraph-0.2-orange)](https://github.com/langchain-ai/langgraph)
&nbsp;
[![Streamlit](https://img.shields.io/badge/Streamlit-1.40-red)](https://streamlit.io/)
&nbsp;
[![License](https://img.shields.io/badge/License-MIT-yellow)](./LICENSE)

</div>

---

## 项目简介

基于 LangChain 框架实现的 **ReAct（Reasoning + Acting）Agent**，集成 RAG 检索增强、多工具调用和动态提示词切换。系统能根据用户意图自动判断任务类型（知识问答 / 报告生成），调用合适的工具和知识库完成推理，并通过 Streamlit 流式界面实时展示 Agent 的思考与执行过程。

## 效果展示

<div align="center">

<img src="assets/chat1.png" alt="问答界面" width="85%">

*图1. 普通问答 — RAG 检索知识库回复*

&nbsp;

<img src="assets/chat2.png" alt="工具调用" width="85%">

*图2. Agent 工具调用 — 实时展示推理与工具执行链路*

&nbsp;

<img src="assets/chat3.png" alt="工具调用详情" width="85%">

*图3. 工具调用详情 — 多步推理与中间结果可视化*

&nbsp;


</div>

## 技术架构

<div align="center">

```
用户输入 (Streamlit)
      │
      ▼
┌─────────────────────────────────────────┐
│            ReAct Agent                   │
│                                          │
│  ┌──────────┐    ┌──────────────────┐   │
│  │ Thought  │───→│     Action       │   │
│  │ (推理)    │    │ (工具调用 / RAG)  │   │
│  └──────────┘    └────────┬─────────┘   │
│       ↑                   │              │
│       └─── Observation ◄──┘              │
│                                          │
│   Middleware: 工具监控 · 动态提示词切换    │
└─────────────────────────────────────────┘
      │                │              │
      ▼                ▼              ▼
┌──────────┐   ┌────────────┐  ┌──────────┐
│ Snapshot │   │   Tools    │  │  Prompt  │
│ Markdown │   │ 现场检索等  │  │ 证据约束  │
│ 快照读取  │   │ 上下文/活动 │  │ 模板管理  │
└──────────┘   └────────────┘  └──────────┘
```

</div>

### 核心特性

| 特性 | 说明 |
|---|---|
| **ReAct 范式** | Thought → Action → Observation 循环，Agent 自主推理并决定调用哪个工具 |
| **快照检索** | 从帖子 Markdown 快照解析帖子、评论、话题和活动，不访问业务数据库 |
| **现场工具调用** | 现场检索 / 帖子上下文 / 活动 / 兴趣对象，工具按权限上下文返回数据 |
| **证据约束提示词** | 只引用当前现场可见快照，资料不足时拒绝编造 |
| **流式对话界面** | Streamlit 构建，支持流式逐字输出、历史消息留存、Agent 推理过程可见 |
| **社区引用跳转** | 回答中的帖子引用会以帖子标题显示并安全跳转到知潮帖子详情页，不执行模型生成的任意 URL |
| **模块化结构** | Agent / RAG / Model / Tools / Middleware 独立模块，配置 YAML 驱动 |

## 技术栈

| 层级 | 技术 |
|---|---|
| LLM | 通义千问（DashScope / ChatTongyi） |
| Agent 框架 | LangChain + LangGraph |
| 向量数据库 | Chroma |
| 文档处理 | PyPDF + RecursiveCharacterTextSplitter |
| 前端 | Streamlit |
| 配置 | YAML 驱动（Agent / RAG / Chroma / Prompts） |

### 兴趣现场快照问答

现场问答工具只读取 `data/knowledge-snapshots/scene-{sceneId}/post-{postId}.md`，不提供通用 SQL
或数据库工具。快照中的 front matter、帖子正文、话题、活动和评论楼层会被解析为
可检索证据；`ReactAgent.execute_stream` 可接收服务端注入的 `scene_id`、
`current_post_id`、`conversation_id` 和可见性集合。正式接入时，`user_id` 应来自
Spring Security Principal，不要从前端请求体读取。

快照按兴趣现场分目录存储；读取端会递归扫描现场目录，并在滚动升级期间兼容根目录中的旧格式文件。
快照目录采用增量扫描：首次扫描配置目录中的所有 Markdown 文件并在本项目
`data/.snapshot-md5.json` 写入 MD5 清单，后续只重新解析新增或 MD5 发生变化的文件，
并自动移除已删除文件。由于 Java 快照目录可能是只读挂载，MD5 清单不写回源目录。

可用内置样例验证快照读取（不需要模型 API Key）：

```bash
python -m scripts.snapshot_demo --snapshot-dir data/demo-snapshots --scene-id 5 --query 张杰
```

前端请求体建议仅包含：

```json
{
  "scene_id": 5,
  "current_post_id": 4,
  "question": "这个现场最近讨论了什么？",
  "conversation_id": "conv_abc123"
}
```

请求范围有两种：`scene` 表示检索现场内所有帖子，`post` 表示只读取指定帖子。
后端调用时传入 `mode="scene"` 或 `mode="post"`；`post` 模式必须同时提供
`current_post_id`，工具层会拒绝访问其它帖子。

#### 回答中的帖子链接

Agent 内部使用 `[帖子 123]` 或 `[帖子 123 · 2 楼]` 作为证据引用，程序渲染时会用工具
返回的帖子标题替代编号作为可见链接文本。程序只会
将本次工具结果中真实存在、且当前用户可见的正整数 `post_id` 转换为
`http://localhost:6001/community/posts/123` 这样的帖子详情链接，点击后打开知潮社区
的 `/community/posts/:postId` 页面；不会信任模型自行生成的 URL 或外部 Markdown 链接。
部署到其它地址时设置：

```bash
export ZENTIDE_COMMUNITY_URL="http://localhost:6001"
```

当前只支持帖子引用，后续可以扩展兴趣现场、话题、活动等引用类型。模型输出的外部链接
会被降级为普通文本。

## 快速开始

### 环境要求

- **Python** ≥ 3.10
- **DashScope API Key**（[阿里云百炼](https://bailian.console.aliyun.com/) 申请）

### 1. 克隆仓库

```bash
git clone https://github.com/lhh737/LangChain-ReAct-Agent.git
cd LangChain-ReAct-Agent
```

### 2. 安装依赖

```bash
pip install -r requirements.txt
```

### 3. 配置 API Key

参考 `.env.example`，设置阿里云百炼 API Key：

```bash
# Linux / macOS
export DASHSCOPE_API_KEY="your-api-key"

# Windows (CMD)
set DASHSCOPE_API_KEY=your-api-key
```

> 申请地址：[阿里云百炼控制台](https://bailian.console.aliyun.com/)

### 4. 初始化知识库（首次运行）

```bash
python -c "from rag.vector_store import VectorStoreService; VectorStoreService().load_document()"
```

### 5. 启动知潮站内 Agent API

```bash
export ZENTIDE_AGENT_INTERNAL_TOKEN="本机服务间共享的随机长字符串"
python run_api.py
```

API 默认仅监听 `http://127.0.0.1:8090`，由 `zentide-web` 转发请求。网站使用两个独立的
Vue 页面路由：`/community/assistant/:hubId` 负责整个兴趣现场，
`/community/assistant/:hubId/post/:postId` 负责单篇帖子及其评论；两者不在页面内混合切换，
也不嵌入 Streamlit。`app.py` 只保留为 Agent 开发调试演示，不属于正式网站页面。

### 验证运行

启动后在聊天框输入以下测试问题：

- *社区最近有哪些主要内容？*（RAG 知识库问答）
- *如果机器人无法正常回充，该如何处理？*（故障排查）
- *请根据用户数据生成一份个性化使用报告*（报告生成 + 工具调用）

### DashScope 连接失败（ProxyError）

如果聊天时出现 `requests.exceptions.ProxyError`，说明请求尚未到达模型服务，通常是
`HTTP_PROXY`、`HTTPS_PROXY` 或 `ALL_PROXY` 指向了不可用的代理。请关闭失效代理或设置
可用代理后重启 Streamlit，并先验证网络连通性：

```bash
curl -I https://dashscope.aliyuncs.com
```

确认 `DASHSCOPE_API_KEY` 已在启动 Streamlit 的同一终端中设置。应用会在聊天窗口中保留
一条可读的错误提示，不会把原始 Python traceback 展示给最终用户。

## 项目结构

```
LangChain-ReAct-Agent/
│
├── agent/                          # Agent 核心
│   ├── react_agent.py              #   ReAct Agent 主逻辑（流式执行）
│   └── tools/
│       ├── agent_tools.py          #   现场快照工具
│       └── middleware.py           #   工具监控与模型调用日志
│
├── rag/                            # RAG 检索增强
│   ├── vector_store.py             #   Chroma 向量库 · 文档加载 · MD5 去重
│   └── rag_service.py              #   RAG 检索 → LLM 总结服务
│
├── model/
│   └── factory.py                  # 模型工厂（ChatTongyi + DashScopeEmbedding）
│
├── config/                         # YAML 配置文件
│   ├── agent.yml                   #   Agent 行为与工具配置
│   ├── chroma.yml                  #   向量库与检索参数
│   ├── prompts.yml                 #   提示词模板
│   └── rag.yml                     #   RAG 模型与参数
│
├── prompts/                        # 提示词模板
│   ├── main_prompt.txt             #   普通问答 System Prompt
│   ├── rag_summarize.txt           #   RAG 总结 Prompt
│   └── report_prompt.txt           #   报告生成 System Prompt
│
├── utils/                          # 工具函数
│   ├── config_handler.py           #   YAML 配置加载
│   ├── file_handler.py             #   文件解析（PDF/TXT）
│   ├── logger_handler.py           #   日志管理
│   ├── path_tool.py                #   路径工具
│   └── prompt_loader.py            #   提示词加载
│
├── data/                           # 知识库文档（社区内容相关）
├── assets/                         # 效果展示截图
├── app.py                          # Streamlit 应用入口
├── requirements.txt
└── README.md
```

## 配置说明

项目通过 `config/` 目录下的 YAML 文件统一管理配置：

| 文件 | 说明 |
|---|---|
| `rag.yml` | 对话模型名称、Embedding 模型名称 |
| `chroma.yml` | Chroma 持久化路径、分块大小、检索 Top-K、支持的文件类型 |
| `prompts.yml` | 各场景提示词模板文件路径 |
| `agent.yml` | Agent 超时时间、外部数据路径等 |

首次运行只需确保 **DashScope API Key 已设置** 且 `data/` 目录下有知识库文档即可。

## License

MIT © [lhh737](https://github.com/lhh737)
