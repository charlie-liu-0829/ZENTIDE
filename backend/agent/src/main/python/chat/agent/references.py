"""安全地将 Agent 帖子引用转换为知潮社区详情链接。"""

from __future__ import annotations

import os
import re
from collections.abc import Iterable
from urllib.parse import urlsplit, urlunsplit


_POST_CITATION = re.compile(
    r"\[(?P<label>帖子\s+(?P<post_id>\d+)"
    r"(?P<floor>\s*·\s*\d+\s*楼)?)\]"
)
_MARKDOWN_LINK = re.compile(
    r"\[(?P<label>[^\]\n]+)\]\(\s*(?P<target>[^\s)]+)"
    r"(?:\s+[^)]*)?\)"
)


def community_base_url(value: str | None = None) -> str:
    """返回规范化的社区绝对地址，不保留 query、fragment 或用户凭据。"""
    raw = (value or os.getenv("ZENTIDE_COMMUNITY_URL") or "http://localhost:6001").strip()
    parsed = urlsplit(raw)
    if (parsed.scheme not in {"http", "https"} or not parsed.netloc
            or parsed.username is not None or parsed.password is not None):
        raise ValueError("ZENTIDE_COMMUNITY_URL 必须是无用户凭据的 http(s) 绝对地址")
    return urlunsplit((parsed.scheme, parsed.netloc, parsed.path.rstrip("/"), "", ""))


def post_url(post_id: int | str, base_url: str | None = None) -> str:
    """为正整数帖子 ID 构造唯一允许的详情路由。"""
    if isinstance(post_id, bool):
        raise ValueError("帖子 ID 必须是正整数")
    if isinstance(post_id, int):
        numeric_id = post_id
    elif isinstance(post_id, str) and re.fullmatch(r"[1-9]\d*", post_id.strip()):
        numeric_id = int(post_id)
    else:
        raise ValueError("帖子 ID 必须是正整数")
    if numeric_id <= 0:
        raise ValueError("帖子 ID 必须是正整数")
    return f"{community_base_url(base_url)}/community/posts/{numeric_id}"


def _allowed_post_id_set(allowed_post_ids: Iterable[int | str] | None) -> set[int] | None:
    if allowed_post_ids is None:
        return None
    result: set[int] = set()
    for value in allowed_post_ids:
        if isinstance(value, bool):
            continue
        if isinstance(value, int):
            numeric_id = value
        elif isinstance(value, str) and re.fullmatch(r"[1-9]\d*", value.strip()):
            numeric_id = int(value)
        else:
            continue
        if numeric_id > 0:
            result.add(numeric_id)
    return result


def _allowed_post_titles(post_titles: dict[int | str, str] | None) -> dict[int, str]:
    result: dict[int, str] = {}
    for raw_id, title in (post_titles or {}).items():
        if not isinstance(title, str) or not title.strip():
            continue
        try:
            post_id = int(raw_id)
        except (TypeError, ValueError):
            continue
        if post_id > 0:
            result[post_id] = title.strip()
    return result


def linkify_post_citations(
    text: str,
    base_url: str | None = None,
    *,
    allowed_post_ids: Iterable[int | str] | None = None,
    post_titles: dict[int | str, str] | None = None,
) -> str:
    """转换安全帖子引用，并用可信标题作为链接文本。"""
    if not text:
        return ""

    safe_base = community_base_url(base_url)
    allowed_ids = _allowed_post_id_set(allowed_post_ids)
    title_by_id = _allowed_post_titles(post_titles)

    def strip_unapproved_link(match: re.Match[str]) -> str:
        """Do not allow the model to smuggle arbitrary URLs into the UI."""
        target = match.group("target")
        if re.fullmatch(rf"{re.escape(safe_base)}/community/posts/[1-9]\d*", target):
            return match.group(0)
        return match.group("label")

    # Remove arbitrary model-generated links before turning bare citations into
    # links. An external target is never carried into the rendered Markdown.
    text = _MARKDOWN_LINK.sub(strip_unapproved_link, text)

    def replace(match: re.Match[str]) -> str:
        post_id = int(match.group("post_id"))
        if post_id <= 0 or (allowed_ids is not None and post_id not in allowed_ids):
            return match.group(0)
        title = title_by_id.get(post_id)
        # Agent 执行时必须有工具返回的标题，避免回退显示数字编号。
        if allowed_ids is not None and not title:
            return match.group(0)
        # 已经是 Markdown 链接的引用不能再次嵌套。
        if match.end() < len(text) and text[match.end()] == "(":
            return match.group(0)
        visible_label = title or match.group("label")
        if title and match.group("floor"):
            visible_label = f"{title}{match.group('floor')}"
        visible_label = re.sub(r"([\\\[\]()*_`])", r"\\\1", visible_label)
        return f"[{visible_label}]({post_url(post_id, safe_base)})"

    return _POST_CITATION.sub(replace, text)
