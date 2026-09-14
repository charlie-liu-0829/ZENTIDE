"""Evidence-first personalized community insight agent.

The deterministic retrieval path is deliberately independent from the LLM.  The
model may summarize already selected evidence, but it can never invent post or
comment references.
"""

from __future__ import annotations

from dataclasses import asdict, dataclass
from datetime import datetime, timezone
import html
import math
from pathlib import Path
import re
from typing import Any, Iterable, Protocol, TypedDict


_FRONTMATTER = re.compile(r"^---\s*\n(.*?)\n---\s*\n", re.S)
_COMMENT = re.compile(
    r"###\s*评论\s+(?P<floor>\d+)\s*\n"
    r"comment_id:\s*(?P<comment_id>\S+)\s*\n"
    r"author_id:\s*(?P<author_id>\S+)\s*\n"
    r"author_name:\s*(?P<author_name>.*?)\s*\n"
    r"created_at:\s*(?P<created_at>.*?)\s*\n\s*(?P<body>.*?)(?=\n###\s*评论|\Z)",
    re.S,
)
_ALIASES = {
    "人工智能": ("ai", "llm", "大语言模型", "agent"),
    "ai": ("人工智能", "llm", "大语言模型", "agent"),
    "spring boot": ("springboot", "spring-boot"),
    # 演出相关表达统一扩展，避免用户使用“演出”时漏掉“演唱会”内容。
    "演出": ("演唱会", "巡演", "现场音乐", "live"),
    "演唱会": ("演出", "巡演", "现场音乐", "live"),
    "巡演": ("演出", "演唱会", "现场音乐", "live"),
    "现场音乐": ("演出", "演唱会", "巡演", "live"),
    "live": ("演出", "演唱会", "巡演", "现场音乐"),
}


def _plain(value: str) -> str:
    value = html.unescape(re.sub(r"<[^>]+>", " ", value or ""))
    return re.sub(r"\s+", " ", value).strip()


def _yaml_value(value: str) -> Any:
    value = value.strip().strip('"\'')
    if value.lower() in {"null", "none", ""}:
        return None
    return value


def _parse_frontmatter(text: str) -> tuple[dict[str, Any], str]:
    match = _FRONTMATTER.search(text)
    if not match:
        return {}, text
    metadata: dict[str, Any] = {}
    for line in match.group(1).splitlines():
        if ":" in line:
            key, value = line.split(":", 1)
            metadata[key.strip()] = _yaml_value(value)
    return metadata, text[match.end():]


@dataclass(frozen=True)
class SnapshotComment:
    comment_id: str
    floor_no: int
    author_id: str | None
    author_name: str | None
    created_at: str | None
    body: str


@dataclass(frozen=True)
class SnapshotPost:
    post_id: int
    scene_id: int
    scene_name: str
    title: str
    body: str
    topics: tuple[str, ...]
    post_type: str | None
    visibility: str
    status: str
    created_at: str | None
    updated_at: str | None
    url: str
    comments: tuple[SnapshotComment, ...] = ()


@dataclass(frozen=True)
class Evidence:
    evidence_id: str
    post_id: int
    comment_id: str | None
    floor_no: int | None
    title: str
    excerpt: str
    matched_keywords: tuple[str, ...]
    url: str
    scene_id: int
    scene_name: str

    def to_dict(self) -> dict[str, Any]:
        data = asdict(self)
        data["matched_keywords"] = list(self.matched_keywords)
        return data


@dataclass(frozen=True)
class InsightItem:
    item_id: str
    headline: str
    summary: str
    confidence: float
    evidence: tuple[Evidence, ...]

    def to_dict(self) -> dict[str, Any]:
        target_keywords = list(dict.fromkeys(keyword for item in self.evidence for keyword in item.matched_keywords))
        return {
            "item_id": self.item_id,
            "headline": self.headline,
            "summary": self.summary,
            "confidence": self.confidence,
            "target_keywords": target_keywords,
            "evidence": [item.to_dict() for item in self.evidence],
        }


@dataclass(frozen=True)
class InsightResult:
    keywords: tuple[str, ...]
    items: tuple[InsightItem, ...]
    matched_count: int
    generated_at: str
    snapshot_version: str
    warning: str | None = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "keywords": list(self.keywords),
            "items": [item.to_dict() for item in self.items],
            "matched_count": self.matched_count,
            "generated_at": self.generated_at,
            "snapshot_version": self.snapshot_version,
            "warning": self.warning,
        }


class InsightSummarizer(Protocol):
    def __call__(self, candidates: list[dict[str, Any]]) -> list[dict[str, Any]]: ...


class InsightState(TypedDict, total=False):
    keywords: tuple[str, ...]
    keyword_groups: dict[str, tuple[str, ...]]
    scene_ids: set[int] | None
    permitted_post_ids: set[int] | None
    seen_post_ids: set[int]
    max_items: int
    max_evidence: int
    now: datetime
    candidates: list[dict[str, Any]]
    items: list[InsightItem]


class SnapshotReader:
    """Reads immutable, published-only knowledge snapshots."""

    def __init__(self, root: str | Path):
        self.root = Path(root)

    def list_posts(
        self, scene_ids: set[int] | None = None, permitted_post_ids: set[int] | None = None
    ) -> list[SnapshotPost]:
        posts: list[SnapshotPost] = []
        if not self.root.exists():
            return posts
        for path in sorted(self.root.glob("scene-*/post-*.md")):
            post = self._read(path)
            if post is None or post.status.upper() != "PUBLISHED":
                continue
            if scene_ids is not None and post.scene_id not in scene_ids:
                continue
            # Explicit post IDs are calculated by Spring Boot for the current user. Without
            # that context, fail closed to public snapshots only.
            if permitted_post_ids is not None:
                if post.post_id not in permitted_post_ids:
                    continue
            elif post.visibility.upper() != "PUBLIC":
                continue
            posts.append(post)
        return posts

    def snapshot_version(self, scene_ids: set[int] | None = None) -> str:
        paths = sorted(self.root.glob("scene-*/post-*.md"))
        parts = []
        for path in paths:
            match = re.search(r"scene-(\d+)", str(path.parent))
            if scene_ids is not None and (not match or int(match.group(1)) not in scene_ids):
                continue
            parts.append(f"{path}:{path.stat().st_mtime_ns}:{path.stat().st_size}")
        import hashlib
        return hashlib.sha256("\n".join(parts).encode()).hexdigest()[:16]

    def _read(self, path: Path) -> SnapshotPost | None:
        try:
            metadata, body = _parse_frontmatter(path.read_text(encoding="utf-8"))
            post_id = int(metadata["post_id"])
            scene_id = int(metadata["scene_id"])
        except (OSError, KeyError, TypeError, ValueError):
            return None
        title_match = re.search(r"^#\s+(.+?)\s*$", body, re.M)
        title = _plain(title_match.group(1)) if title_match else ""
        content = body.split("## 评论区", 1)[0]
        content = re.split(r"\n\s*\*\*话题\*\*：|\n\s*\*\*关联活动\*\*：", content, maxsplit=1)[0]
        content = re.sub(r"^#\s+.+?$", "", content, count=1, flags=re.M)
        topics_match = re.search(r"\*\*话题\*\*：(.+)", body)
        topics = tuple(item.strip() for item in (topics_match.group(1).split(",") if topics_match else []) if item.strip())
        comments = []
        for floor_no, match in enumerate(_COMMENT.finditer(body), start=1):
            comments.append(SnapshotComment(
                comment_id=match.group("comment_id"), floor_no=floor_no,
                author_id=match.group("author_id"), author_name=match.group("author_name").strip(),
                created_at=match.group("created_at").strip(), body=_plain(match.group("body")),
            ))
        return SnapshotPost(
            post_id=post_id, scene_id=scene_id, scene_name=str(metadata.get("scene_name") or ""),
            title=title, body=_plain(content), topics=topics,
            post_type=_yaml_value(str(metadata.get("post_type") or "")),
            visibility=str(metadata.get("visibility") or "PUBLIC"), status=str(metadata.get("status") or ""),
            created_at=str(metadata.get("created_at") or "") or None,
            updated_at=str(metadata.get("updated_at") or "") or None,
            url=f"/community/posts/{post_id}", comments=tuple(comments),
        )


def _normalize_keywords(keywords: Iterable[str]) -> tuple[str, ...]:
    result: list[str] = []
    for keyword in keywords:
        value = re.sub(r"\s+", " ", str(keyword or "")).strip()
        if value and value.casefold() not in {item.casefold() for item in result}:
            result.append(value[:80])
    return tuple(result[:30])


def _matched(text: str, keywords: tuple[str, ...]) -> tuple[str, ...]:
    folded = text.casefold()
    matched = []
    for keyword in keywords:
        normalized = keyword.casefold()
        variants = (normalized,) + _ALIASES.get(normalized, ())
        if any(variant.casefold() in folded for variant in variants):
            matched.append(keyword)
    return tuple(matched)


def _matched_targets(text: str, groups: dict[str, tuple[str, ...]]) -> tuple[str, ...]:
    """Return user target keywords, while expanded terms remain internal."""
    return tuple(target for target, terms in groups.items() if _matched(text, terms))


def _excerpt(text: str, keyword: str, limit: int = 180) -> str:
    text = _plain(text)
    index = text.casefold().find(keyword.casefold())
    if index < 0 or len(text) <= limit:
        return text[:limit]
    start = max(0, index - limit // 3)
    return ("…" if start else "") + text[start:start + limit].strip() + ("…" if start + limit < len(text) else "")


def _recency_score(value: str | None, now: datetime) -> float:
    if not value:
        return 0.5
    try:
        created = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if created.tzinfo is None:
            created = created.replace(tzinfo=timezone.utc)
        age_days = max(0.0, (now - created).total_seconds() / 86400)
        return math.exp(-age_days / 14)
    except ValueError:
        return 0.5


class PersonalizedInsightAgent:
    def __init__(self, reader: SnapshotReader, summarizer: InsightSummarizer | None = None):
        self.reader = reader
        self.summarizer = summarizer

    def generate(
        self,
        keywords: Iterable[str],
        *,
        scene_ids: set[int] | None = None,
        seen_post_ids: set[int] | None = None,
        permitted_post_ids: set[int] | None = None,
        max_items: int = 10,
        max_evidence_per_item: int = 5,
    ) -> InsightResult:
        normalized = _normalize_keywords(keywords)
        if not normalized:
            return InsightResult((), (), 0, datetime.now(timezone.utc).isoformat(), self.reader.snapshot_version(scene_ids), "请至少添加一个兴趣关键词")
        now = datetime.now(timezone.utc)
        keyword_groups = {keyword: (keyword,) for keyword in normalized}
        expand_groups = getattr(self.summarizer, "expand_keyword_groups", None)
        if callable(expand_groups):
            try:
                generated = expand_groups(list(normalized)) or {}
                keyword_groups = {
                    keyword: _normalize_keywords([keyword, *generated.get(keyword, [])])
                    for keyword in normalized
                }
            except Exception:
                pass
        state: InsightState = {
            "keywords": normalized, "keyword_groups": keyword_groups, "scene_ids": scene_ids,
            "permitted_post_ids": permitted_post_ids,
            "seen_post_ids": seen_post_ids or set(),
            "max_items": max_items, "max_evidence": max_evidence_per_item, "now": now,
        }
        state = self._execute_graph(state)
        candidates = state["candidates"]
        items = state["items"]
        return InsightResult(
            keywords=normalized, items=tuple(items), matched_count=len(candidates),
            generated_at=now.isoformat(), snapshot_version=self.reader.snapshot_version(scene_ids),
            warning=None if items else "当前可见的已发布内容中没有命中这些关键词",
        )

    def _deterministic_recall(self, state: InsightState) -> dict[str, Any]:
        groups = state["keyword_groups"]
        candidates: list[dict[str, Any]] = []
        for post in self.reader.list_posts(state.get("scene_ids"), state.get("permitted_post_ids")):
            if post.post_id in state["seen_post_ids"]:
                continue
            title_hits = _matched_targets(post.title, groups)
            body_hits = _matched_targets(post.body, groups)
            topic_hits = _matched_targets(" ".join(post.topics), groups)
            post_hits = tuple(dict.fromkeys(title_hits + body_hits + topic_hits))
            if post_hits:
                candidates.append(self._candidate(post, None, post_hits, state["now"], title_hits, body_hits, topic_hits))
            for comment in post.comments:
                comment_hits = _matched_targets(comment.body, groups)
                if comment_hits:
                    candidates.append(self._candidate(post, comment, comment_hits, state["now"], (), comment_hits, ()))
        candidates.sort(key=lambda item: item["score"], reverse=True)
        return {"candidates": candidates[: max(20, state["max_items"] * 4)]}

    def _summarize_candidates(self, state: InsightState) -> dict[str, Any]:
        candidates = state["candidates"]
        if self.summarizer and candidates:
            candidates = self._apply_summary(candidates, state["max_items"])
        return {"candidates": candidates}

    def _bind_evidence(self, state: InsightState) -> dict[str, Any]:
        return {
            "items": self._group_candidates(
                state["candidates"], state["max_items"], state["max_evidence"]
            )
        }

    def _execute_graph(self, state: InsightState) -> InsightState:
        nodes = [self._deterministic_recall, self._summarize_candidates, self._bind_evidence]
        try:
            from langgraph.graph import END, START, StateGraph
        except ImportError:
            for node in nodes:
                state.update(node(state))
            return state
        graph = StateGraph(InsightState)
        graph.add_node("deterministic_recall", self._deterministic_recall)
        graph.add_node("candidate_summary", self._summarize_candidates)
        graph.add_node("evidence_binding", self._bind_evidence)
        graph.add_edge(START, "deterministic_recall")
        graph.add_edge("deterministic_recall", "candidate_summary")
        graph.add_edge("candidate_summary", "evidence_binding")
        graph.add_edge("evidence_binding", END)
        return graph.compile().invoke(state)

    @staticmethod
    def _candidate(post: SnapshotPost, comment: SnapshotComment | None, hits: tuple[str, ...], now: datetime, title_hits: tuple[str, ...], body_hits: tuple[str, ...], topic_hits: tuple[str, ...]) -> dict[str, Any]:
        source = comment.body if comment else post.body
        score = min(1.0, 0.38 * min(1.0, len(hits) / 3) + 0.18 * bool(title_hits) + 0.14 * bool(body_hits) + 0.12 * bool(topic_hits) + 0.18 * _recency_score(post.updated_at or post.created_at, now))
        evidence_id = f"comment:{post.post_id}:{comment.comment_id}" if comment else f"post:{post.post_id}"
        evidence = Evidence(evidence_id, post.post_id, comment.comment_id if comment else None, comment.floor_no if comment else None, post.title, _excerpt(source, hits[0]), hits, post.url, post.scene_id, post.scene_name)
        return {"post_id": post.post_id, "headline": post.title, "score": score, "evidence": evidence, "source": source}

    def _apply_summary(self, candidates: list[dict[str, Any]], max_items: int) -> list[dict[str, Any]]:
        payload = [{"candidate_id": item["evidence"].evidence_id, "headline": item["headline"], "excerpt": item["evidence"].excerpt, "matched_keywords": list(item["evidence"].matched_keywords)} for item in candidates]
        try:
            generated = self.summarizer(payload[: max_items * 3]) or []
        except Exception:
            return candidates
        by_id = {item["evidence"].evidence_id: item for item in candidates}
        result = []
        for entry in generated:
            candidate_id = str(entry.get("candidate_id") or "")
            if candidate_id not in by_id:
                continue
            item = dict(by_id[candidate_id])
            item["summary"] = str(entry.get("summary") or item["evidence"].excerpt)[:500]
            item["headline"] = str(entry.get("headline") or item["headline"])[:120]
            result.append(item)
        return result or candidates

    @staticmethod
    def _group_candidates(candidates: list[dict[str, Any]], max_items: int, max_evidence: int) -> list[InsightItem]:
        grouped: dict[int, list[dict[str, Any]]] = {}
        for candidate in candidates:
            grouped.setdefault(candidate["post_id"], []).append(candidate)
        items = []
        for index, values in enumerate(grouped.values()):
            values.sort(key=lambda item: item["score"], reverse=True)
            first = values[0]
            evidence = tuple(dict.fromkeys(item["evidence"] for item in values))[:max_evidence]
            summary = first.get("summary") or first["evidence"].excerpt
            if len(evidence) > 1 and "summary" not in first:
                summary = f"围绕“{first['headline']}”发现 {len(evidence)} 条相关内容，涉及：" + "、".join(sorted({key for item in evidence for key in item.matched_keywords}))
            items.append(InsightItem(f"insight:{first['post_id']}", first["headline"], summary, round(min(0.99, 0.52 + first["score"] * 0.45), 2), evidence))
        items.sort(key=lambda item: item.confidence, reverse=True)
        return items[:max_items]
