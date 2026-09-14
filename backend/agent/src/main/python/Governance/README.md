# ZENTIDE Governance Agent

社区治理 Agent 只输出风险评估和建议动作，不直接删除内容、封禁用户或执行其他不可逆操作。所有高风险结果进入暂时隐藏/人工审核流程，最终动作由管理员确认。

每次审核都会读取 `data/knowledge-snapshots/scene-*/post-*.md` 中的已发布快照，并将可读摘要作为证据上下文返回。治理规则为全站规则，由 Spring Boot 统一保存和提供；快照只接受 `PUBLISHED + PUBLIC` 内容。扫描接口会遍历全部公开已发布帖子，管理员请求中的 `rules` 和 `cases` 只是额外规则/案例，不是唯一数据来源。

## 启动

```bash
cd /path/to/ZENTIDE
make governance-run
```

默认地址：`http://127.0.0.1:8093`。

接口：

```text
POST /v1/governance/review
POST /v1/governance/feedback
```

审核结果和管理员反馈由 Spring Boot 主库统一保存，Agent 服务本身不持久化业务数据。
