# Shared RAG primitives

四个 Agent 共用的站内内容检索基础层：

- `models.ContentChunk`：统一内容片段元数据和原始内容 ID；
- `retrieval.SnapshotChunkIndex`：按标题、段落切片，默认 700 字符、80 字符重叠；
- `permissions.filter_chunks`：只允许已发布且公开的片段通过最终权限门；
- `citations.citation`：从程序绑定 `chunk_id`、内容 ID 和站内 URL，模型不能自行生成引用。

当前 SmartPosting / Recommend 已有自己的领域召回器，同时通过同一份快照目录工作；后续向量化时只需替换 `SnapshotChunkIndex.search`，不改变四个 Agent 的权限和引用协议。
