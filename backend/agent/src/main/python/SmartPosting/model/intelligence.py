"""Lazy model boundary for semantic embeddings and structured draft analysis."""

from __future__ import annotations

from dataclasses import dataclass
import json
import logging
import math
import os
import re
from threading import RLock
from typing import Any


logger = logging.getLogger(__name__)


def cosine(left: list[float] | None, right: list[float] | None) -> float:
    if not left or not right or len(left) != len(right):
        return 0.0
    numerator = sum(a * b for a, b in zip(left, right))
    denominator = math.sqrt(sum(a * a for a in left)) * math.sqrt(sum(b * b for b in right))
    return max(0.0, min(1.0, numerator / denominator)) if denominator else 0.0


def _json_object(text: str) -> dict[str, Any]:
    text = (text or "").strip()
    fenced = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", text, re.DOTALL | re.I)
    candidate = fenced.group(1) if fenced else text
    if not candidate.startswith("{"):
        match = re.search(r"\{.*\}", candidate, re.DOTALL)
        candidate = match.group(0) if match else "{}"
    value = json.loads(candidate)
    return value if isinstance(value, dict) else {}


@dataclass(frozen=True)
class ModelAnalysis:
    suggested_post_type: str | None
    missing_information: list[str]
    suggested_title: str | None
    suggested_topic_ids: list[int]
    community_rule_warnings: list[str]
    optimized_content: str | None
    content_improvements: list[str]


class SmartPostingModels:
    """Initializes DashScope-backed LangChain clients only when credentials exist."""

    def __init__(self):
        self.enabled = bool(os.getenv("DASHSCOPE_API_KEY", "").strip())
        self._chat = None
        self._embeddings = None
        self._embedding_cache: dict[str, list[float]] = {}
        self._lock = RLock()

    def _load(self) -> bool:
        if not self.enabled:
            return False
        with self._lock:
            if self._chat is not None and self._embeddings is not None:
                return True
            try:
                from langchain_community.chat_models.tongyi import ChatTongyi
                from langchain_community.embeddings import DashScopeEmbeddings
                self._chat = ChatTongyi(
                    model=os.getenv("ZENTIDE_POST_REVIEW_MODEL", "qwen3-max"),
                    temperature=0,
                    max_retries=1,
                    model_kwargs={"request_timeout": 12},
                )
                self._embeddings = DashScopeEmbeddings(
                    model=os.getenv("ZENTIDE_POST_REVIEW_EMBEDDING_MODEL", "text-embedding-v4")
                )
                return True
            except (ImportError, ValueError):
                self.enabled = False
                return False

    def embed(self, texts: list[str]) -> list[list[float]] | None:
        if not texts or not self._load():
            return None
        missing = [text for text in dict.fromkeys(texts) if text not in self._embedding_cache]
        try:
            if missing:
                vectors = []
                for start in range(0, len(missing), 10):
                    vectors.extend(self._embeddings.embed_documents(missing[start:start + 10]))
                with self._lock:
                    for text, vector in zip(missing, vectors):
                        self._embedding_cache[text] = vector
                    # Bound the process cache; snapshot content is immutable between exports.
                    while len(self._embedding_cache) > 2000:
                        self._embedding_cache.pop(next(iter(self._embedding_cache)))
            return [self._embedding_cache[text] for text in texts]
        except Exception as error:
            logger.warning("SmartPosting embedding unavailable; using deterministic similarity: %s",
                           type(error).__name__)
            return None

    def analyze(self, *, title: str, content: str, selected_post_type: str,
                type_requirements: dict[str, list[str]], available_topics: list[dict[str, Any]],
                community_rules: list[str], similar_posts: list[dict[str, Any]]) -> ModelAnalysis | None:
        if not self._load():
            return None
        topic_catalog = [
            {"topic_id": item.get("topic_id"), "name": item.get("name")}
            for item in available_topics[:100]
        ]
        prompt = f"""你是知潮社区发帖编辑器中的智能发帖副驾。分析草稿，并提供一份可由用户主动采纳的正文优化稿。

返回一个 JSON 对象，且只能包含这些字段：
- suggested_post_type: 最合适的类型代码，必须来自 {list(type_requirements)}
- missing_information: 按该类型检查后缺少的信息名称数组
- suggested_title: 精确、自然、不过度承诺的中文标题，最长 80 字
- suggested_topic_ids: 只能从话题目录选择，最多 3 个整数 ID
- community_rule_warnings: 明确违反或可能违反规则的提醒数组；没有则为空
- optimized_content: 优化后的纯文本正文；只调整结构、段落、语气和清晰度，不需要优化则为 null
- content_improvements: 最多 6 条简短说明，解释优化了什么或建议用户补充什么

类型及完整性要求：{json.dumps(type_requirements, ensure_ascii=False)}
社区规则：{json.dumps(community_rules, ensure_ascii=False)}
可选话题：{json.dumps(topic_catalog, ensure_ascii=False)}
已召回的相似帖子：{json.dumps(similar_posts[:5], ensure_ascii=False)}

用户已选类型：{selected_post_type or '未选择'}
标题：{title}
正文：{content}

优化正文时必须遵守：
1. 保留原文事实、数字、版本、时间、地点、代码、链接和用户立场；
2. 不得补写草稿中不存在的事实，不得把 missing_information 猜成具体答案；
3. 不得添加“待补充”等占位内容；信息缺失只放入 missing_information 和 content_improvements；
4. 使用纯文本和自然换行，不输出 Markdown 或 HTML，最长 8000 字。

不要输出重复概率，不要输出 JSON 以外的内容。"""
        try:
            response = self._chat.invoke(prompt)
            raw = response.content
            if isinstance(raw, list):
                raw = "".join(str(item.get("text", "")) if isinstance(item, dict) else str(item) for item in raw)
            value = _json_object(str(raw))
            type_code = str(value.get("suggested_post_type") or "").strip().upper() or None
            if type_code not in type_requirements:
                type_code = None
            allowed_topic_ids = {int(item["topic_id"]) for item in topic_catalog if item.get("topic_id")}
            topic_ids = []
            for raw_id in value.get("suggested_topic_ids", []):
                try:
                    topic_id = int(raw_id)
                except (TypeError, ValueError):
                    continue
                if topic_id in allowed_topic_ids and topic_id not in topic_ids:
                    topic_ids.append(topic_id)
            return ModelAnalysis(
                suggested_post_type=type_code,
                missing_information=[str(item).strip() for item in value.get("missing_information", []) if str(item).strip()][:12],
                suggested_title=str(value.get("suggested_title") or "").strip()[:80] or None,
                suggested_topic_ids=topic_ids[:3],
                community_rule_warnings=[str(item).strip() for item in value.get("community_rule_warnings", []) if str(item).strip()][:8],
                optimized_content=str(value.get("optimized_content") or "").strip()[:8000] or None,
                content_improvements=[str(item).strip() for item in value.get("content_improvements", []) if str(item).strip()][:6],
            )
        except Exception as error:
            logger.warning("SmartPosting structured analysis unavailable; using rule fallback: %s",
                           type(error).__name__)
            return None
