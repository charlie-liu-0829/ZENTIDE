# SmartPosting：发布前质量检查服务

SmartPosting 嵌入知潮发帖编辑器，负责发布前的质量与风险检查。它不是聊天机器人，也不会自动修改用户原文。

当前同步版本包括：

- Qwen 结构化分析帖子类型、缺失信息、标题、话题和社区规则风险
- 问题、经验、活动、评测、投票、讨论六套独立完整性模板
- 手机号、邮箱、密码、访问令牌、私钥和云访问密钥风险检查；命中后硬阻断发布
- 敏感草稿在规则节点直接结束，不发送给 Embedding 或 LLM
- 基于当前现场“已发布快照”的语义相似帖子召回
- 标题语义、正文语义、关键词、同话题、采纳答案五因子重复度（LLM 不参与打分）
- 标题、类型、话题、社区规则和正文表达建议
- Qwen 生成可预览的正文优化稿，用户逐项采纳，不自动覆盖；具体数字、版本、日期和链接会经过防编造校验
- `ready`、`revise`、`view_similar_posts`、`manual_review` 四种发布建议
- 审核结果与用户、现场、标题、正文、类型、话题的内容指纹绑定（默认 15 分钟有效）

## 调用链

```text
Vue 发帖编辑器
  -> POST /api/posts/review
Spring Boot（注入登录用户、校验现场成员和话题权限）
  -> POST /v1/posts/review
SmartPosting FastAPI（LangGraph 编排，只读取 Java 导出的已发布快照）
```

发布时，Vue 把 `reviewId` 交给现有 `/api/zentide/v1/community/posts` 接口。Spring Boot 再向 SmartPosting 验证内容指纹：内容改变、审核过期、用户或现场不一致都会拒绝发布。`publish_blocked=true` 的结果无论是否传入 `forcePublish=true` 都禁止发布；`manual_review` 进入隐藏状态等待人工审核，其他建议状态需要显式传入 `forcePublish=true`。

正文优化结果通过 `optimized_content` 和 `content_improvements` 返回。前端在右侧面板先展示预览，用户采纳后正文指纹会变化，因此必须重新运行审核。

## 启动

```bash
cd backend/agent/src/main/python/SmartPosting
pip install -r requirements.txt
python run_api.py
```

默认监听 `127.0.0.1:8091`。可配置：

- `ZENTIDE_POST_REVIEW_HOST`
- `ZENTIDE_POST_REVIEW_PORT`
- `ZENTIDE_KNOWLEDGE_SNAPSHOT_DIR`
- `ZENTIDE_AGENT_INTERNAL_TOKEN`（应与 Spring Boot 一致）
- `ZENTIDE_POST_REVIEW_URL`（Spring Boot 配置，默认 `http://127.0.0.1:8091`）
- `DASHSCOPE_API_KEY`（启用完整语义 Agent；未配置或调用失败时返回基础规则结果）
- `ZENTIDE_POST_REVIEW_MODEL`（默认 `qwen3-max`）
- `ZENTIDE_POST_REVIEW_EMBEDDING_MODEL`（默认 `text-embedding-v4`）

现场规则位于 `config/community_rules.yml`，支持按 `scene_id` 增加规则，服务会在文件变更后自动重新加载。

Streamlit 仅作为 Demo/运营预览：

```bash
streamlit run app.py
```

## 测试

```bash
pip install -r requirements-dev.txt
python -m pytest tests
```

后续异步阶段可以增加 `POST /v1/posts/review/async` 与 `GET /v1/posts/review/{review_id}`，而不改变当前审核结果结构。

Spring Boot 以结构化日志记录 `review_started`、`review_completed`、`similar_post_clicked`、`suggested_title_accepted`、`suggested_topic_accepted`、`optimized_content_accepted`、`publish_after_review`、`force_publish` 和 `manual_review_triggered`；日志只包含用户、现场、审核和目标标识，不包含草稿正文。
