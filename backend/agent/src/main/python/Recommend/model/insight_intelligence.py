"""Lazy Qwen summarizer for evidence candidates."""

from __future__ import annotations

import json
import logging
import os
import re
from typing import Any


logger = logging.getLogger(__name__)


def _json_array(text: str) -> list[dict[str, Any]]:
    text = (text or "").strip()
    match = re.search(r"```(?:json)?\s*(\[.*?\])\s*```", text, re.S | re.I)
    candidate = match.group(1) if match else text
    if not candidate.startswith("["):
        match = re.search(r"\[.*\]", candidate, re.S)
        candidate = match.group(0) if match else "[]"
    value = json.loads(candidate)
    return [item for item in value if isinstance(item, dict)] if isinstance(value, list) else []


class QwenEvidenceSummarizer:
    def __init__(self):
        self.enabled = bool(os.getenv("DASHSCOPE_API_KEY", "").strip())
        self._model = None

    def _load(self) -> bool:
        if not self.enabled:
            return False
        if self._model is not None:
            return True
        try:
            from langchain_community.chat_models.tongyi import ChatTongyi
            self._model = ChatTongyi(
                model=os.getenv("ZENTIDE_RECOMMEND_MODEL", "qwen3-max"),
                temperature=0,
                max_retries=1,
                model_kwargs={"request_timeout": 12},
            )
            return True
        except (ImportError, ValueError):
            self.enabled = False
            return False

    def __call__(self, candidates: list[dict[str, Any]]) -> list[dict[str, Any]]:
        if not candidates or not self._load():
            return []
        prompt = f"""你是知潮社区的个性化兴趣情报 Agent。请只根据候选证据生成简洁情报，不添加候选中不存在的事实。

候选证据：{json.dumps(candidates, ensure_ascii=False)}

返回 JSON 数组。每项只能包含 candidate_id、headline、summary。candidate_id 必须原样使用候选中的值；不得生成帖子 ID、评论 ID、楼层或 URL。不要输出 Markdown。"""
        try:
            response = self._model.invoke(prompt)
            content = response.content
            if isinstance(content, list):
                content = "".join(str(item.get("text", "")) if isinstance(item, dict) else str(item) for item in content)
            allowed = {item["candidate_id"] for item in candidates}
            return [item for item in _json_array(str(content)) if str(item.get("candidate_id") or "") in allowed]
        except Exception as error:
            logger.warning("Recommend summary unavailable; using evidence excerpts: %s", type(error).__name__)
            return []

    def expand_keywords(self, keywords: list[str], limit: int = 24) -> list[str]:
        """Expand user interests before retrieval; terms only, never post facts."""
        if not keywords or not self._load():
            return []
        prompt = f"""你是社区兴趣检索规划器。用户输入的兴趣词是：{json.dumps(keywords, ensure_ascii=False)}。
请为每个兴趣词生成用于社区搜索的相关表达，包括：同义词、简称、英文写法、同一主题下的具体活动/场景表达和用户可能使用的口语写法。
只生成与原兴趣同层级或更具体的表达；禁止生成上位大类、过于宽泛的词（例如“演出”不要扩展成“娱乐”“文化”“音乐”），也不要生成与兴趣无关的词、帖子、用户、事实或链接。
只返回 JSON 字符串数组，最多 {limit} 项；必须保留原始兴趣词。"""
        try:
            response = self._model.invoke(prompt)
            content = response.content
            if isinstance(content, list):
                content = "".join(str(item.get("text", "")) if isinstance(item, dict) else str(item) for item in content)
            value = json.loads(re.search(r"\[.*\]", str(content), re.S).group(0))
            return [str(item).strip() for item in value if str(item).strip()][:limit] if isinstance(value, list) else []
        except Exception as error:
            logger.warning("Recommend keyword expansion unavailable: %s", type(error).__name__)
            return []

    def expand_keyword_groups(self, keywords: list[str], per_keyword: int = 10) -> dict[str, list[str]]:
        """Keep expansions attached to their original user interest."""
        if not keywords or not self._load():
            return {}
        prompt = f"""你是社区兴趣检索规划器。目标关键词：{json.dumps(keywords, ensure_ascii=False)}。
为每个目标关键词生成同义词、简称、英文写法、同层级表达和更具体的场景词。禁止生成宽泛的上位大类。
只返回 JSON 数组，格式为：[{{"keyword":"原目标关键词","terms":["检索词"]}}]。
keyword 必须逐字来自目标关键词；每个目标最多 {per_keyword} 个 terms；terms 必须包含原目标关键词。不要输出 Markdown。"""
        try:
            response = self._model.invoke(prompt)
            content = response.content
            if isinstance(content, list):
                content = "".join(str(item.get("text", "")) if isinstance(item, dict) else str(item) for item in content)
            rows = _json_array(str(content))
            allowed = {item.casefold(): item for item in keywords}
            groups: dict[str, list[str]] = {}
            for row in rows:
                original = allowed.get(str(row.get("keyword") or "").casefold())
                if not original or not isinstance(row.get("terms"), list):
                    continue
                groups[original] = [str(term).strip() for term in row["terms"] if str(term).strip()][:per_keyword]
            return groups
        except Exception as error:
            logger.warning("Recommend grouped keyword expansion unavailable: %s", type(error).__name__)
            return {}
