"""Tools exposed to the interest-scene question answering agent.

All scene data comes from Markdown knowledge snapshots.  The authenticated
application sets the request context before invoking the graph; tools never
accept a user id or permission list from the model.
"""

from __future__ import annotations

from contextvars import ContextVar
from datetime import datetime
from typing import Any

from langchain_core.tools import tool

from agent.snapshot_store import SnapshotStore, _parse_frontmatter
from utils.logger_handler import logger


snapshot_store = SnapshotStore()
_snapshot_context: ContextVar[dict[str, Any]] = ContextVar("snapshot_context", default={})


def set_snapshot_context(*, scene_id: int, scope: str = "scene",
                         current_post_id: int | None = None,
                         selected_post_ids: list[int] | None = None,
                         user_id: str | None = None,
                         permitted_visibilities: set[str] | None = None):
    """Set server-controlled authorization context for one graph invocation."""
    return _snapshot_context.set({
        "scene_id": scene_id,
        "scope": scope,
        "current_post_id": current_post_id,
        "selected_post_ids": set(selected_post_ids or ([current_post_id] if current_post_id else [])),
        "user_id": user_id,
        "permitted_visibilities": {v.upper() for v in (permitted_visibilities or {"PUBLIC"})},
    })


def reset_snapshot_context(token) -> None:
    # Streaming graph execution can cross Context boundaries.  Fall back to
    # clearing the current context when the original token belongs elsewhere.
    try:
        _snapshot_context.reset(token)
    except ValueError:
        _snapshot_context.set({})


def _context_for_scene(scene_id: int) -> dict[str, Any] | None:
    context = _snapshot_context.get()
    if context.get("scene_id") != scene_id:
        logger.warning("拒绝跨现场快照访问：请求现场=%s，工具现场=%s", context.get("scene_id"), scene_id)
        return None
    return context


@tool(description="检索当前兴趣现场中用户可见的已发布帖子或评论。范围由服务端自动限定。")
def search_scene_content(query: str, content_types: list[str],
                         time_range: str | None = None) -> list[dict[str, Any]]:
    context = _snapshot_context.get()
    scene_id = context.get("scene_id")
    if scene_id is None or not query.strip():
        return []
    return snapshot_store.search(
        scene_id=scene_id,
        query=query,
        content_types=content_types,
        time_range=time_range,
        permitted_visibilities=context["permitted_visibilities"],
        limit=10,
    )


@tool(description="读取当前页面对应的帖子及命中评论上下楼层关系。范围由服务端自动限定。")
def get_thread_context(post_id: int, comment_ids: list[int]) -> dict[str, Any]:
    context = _snapshot_context.get()
    if post_id <= 0 or context.get("scene_id") is None:
        return {}
    if context.get("scope") == "post" and post_id not in context.get("selected_post_ids", set()):
        return {}
    post = snapshot_store.load_post(post_id)
    if not post or post.scene_id != context["scene_id"]:
        return {}
    return snapshot_store.thread_context(
        post_id=post_id,
        comment_ids=comment_ids,
        permitted_visibilities=context["permitted_visibilities"],
    ) or {}


@tool(description="读取用户通过 @ 关联的当前帖子及其评论。无需传编号，范围由服务端自动限定。")
def get_current_posts_context() -> dict[str, Any]:
    context = _snapshot_context.get()
    scene_id = context.get("scene_id")
    post_ids = context.get("selected_post_ids", set())
    if scene_id is None or not post_ids:
        return {"posts": []}
    posts = []
    for post_id in sorted(post_ids):
        post = snapshot_store.load_post(post_id)
        if not post or post.scene_id != scene_id:
            continue
        thread = snapshot_store.thread_context(
            post_id=post_id,
            permitted_visibilities=context["permitted_visibilities"],
        )
        if thread:
            posts.append(thread)
    return {"posts": posts}


@tool(description="查询当前兴趣现场中关联的活动。范围由服务端自动限定。")
def get_scene_activities(start_at: str | None = None) -> list[dict[str, Any]]:
    context = _snapshot_context.get()
    scene_id = context.get("scene_id")
    if scene_id is None:
        return []
    cutoff = None
    if start_at:
        try:
            cutoff = datetime.fromisoformat(start_at.replace("Z", "+00:00"))
        except ValueError:
            logger.warning("忽略无法解析的活动起始时间：%s", start_at)
    activities: list[dict[str, Any]] = []
    seen: set[tuple[int, str]] = set()
    for post in snapshot_store.list_posts(scene_id, context["permitted_visibilities"]):
        if cutoff and post.created_at:
            try:
                if datetime.fromisoformat(post.created_at.replace("Z", "+00:00")) < cutoff:
                    continue
            except ValueError:
                pass
        for name in post.activities:
            key = (post.post_id, name)
            if key not in seen:
                seen.add(key)
                activities.append({"name": name, "post_id": post.post_id,
                                   "created_at": post.created_at})
    return activities


@tool(description="读取已授权的兴趣对象 Markdown 快照资料。")
def get_interest_object(object_id: int) -> dict[str, Any]:
    if object_id <= 0:
        return {}
    snapshot_store.refresh()
    path = snapshot_store.snapshot_dir / f"interest-object-{object_id}.md"
    if not path.is_file():
        return {}
    raw = path.read_text(encoding="utf-8")
    metadata, _ = _parse_frontmatter(raw)
    context = _snapshot_context.get()
    if metadata.get("scene_id") is not None:
        try:
            if int(metadata["scene_id"]) != int(context.get("scene_id", 0)):
                return {}
        except (TypeError, ValueError):
            return {}
    if str(metadata.get("visibility", "PUBLIC")).upper() not in context.get("permitted_visibilities", {"PUBLIC"}):
        return {}
    return {"object_id": object_id, "content": raw}
