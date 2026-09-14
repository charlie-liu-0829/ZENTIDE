"""Read-only access to Zentide knowledge snapshots.

Snapshots are the integration boundary for the scene agent.  This module does
not know about (or connect to) the application database; it only reads the
published Markdown files written by ``ZentideKnowledgeSnapshotService``.
"""

from __future__ import annotations

from dataclasses import asdict, dataclass
from datetime import datetime
import hashlib
import json
import re
from threading import RLock
from pathlib import Path
from typing import Any

import yaml

from utils.path_tool import get_abs_path
from utils.config_handler import agent_conf


@dataclass(frozen=True)
class CommentSnapshot:
    comment_id: int
    author_id: str | None
    author_name: str | None
    parent_comment_id: int | None
    created_at: str | None
    content: str
    floor_no: int


@dataclass(frozen=True)
class PostSnapshot:
    post_id: int
    scene_id: int
    scene_name: str | None
    visibility: str
    status: str
    author_id: str | None
    author_name: str | None
    post_type: str | None
    created_at: str | None
    updated_at: str | None
    title: str
    body: str
    topics: tuple[str, ...]
    activities: tuple[str, ...]
    comments: tuple[CommentSnapshot, ...]
    source_path: str


def _as_int(value: Any) -> int | None:
    try:
        return int(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def _parse_frontmatter(text: str) -> tuple[dict[str, Any], str]:
    match = re.match(r"\A---\s*\n(.*?)\n---\s*\n?(.*)\Z", text, re.DOTALL)
    if not match:
        return {}, text
    metadata = yaml.safe_load(match.group(1)) or {}
    return metadata if isinstance(metadata, dict) else {}, match.group(2)


def _field(block: str, name: str) -> str | None:
    match = re.search(rf"(?m)^\s*{re.escape(name)}\s*:\s*(.*?)\s*$", block)
    return match.group(1).strip() if match else None


def _parse_comments(body: str) -> tuple[CommentSnapshot, ...]:
    marker = re.search(r"(?m)^##\s+评论区\s*$", body)
    if not marker:
        return ()
    comments_text = body[marker.end():]
    sections = list(re.finditer(r"(?m)^###\s+评论\s+(\d+)\s*$", comments_text))
    comments: list[CommentSnapshot] = []
    for index, section in enumerate(sections):
        start = section.end()
        end = sections[index + 1].start() if index + 1 < len(sections) else len(comments_text)
        block = comments_text[start:end].strip()
        # Metadata is emitted as key/value lines followed by the comment text.
        metadata_lines = []
        content_lines = []
        metadata_keys = {"comment_id", "author_id", "author_name", "parent_comment_id", "created_at"}
        for line in block.splitlines():
            key = line.split(":", 1)[0].strip() if ":" in line else ""
            if key in metadata_keys:
                metadata_lines.append(line)
            elif line.strip():
                content_lines.append(line)
        metadata_block = "\n".join(metadata_lines)
        comment_id = _as_int(_field(metadata_block, "comment_id"))
        if comment_id is None:
            comment_id = int(section.group(1))
        comments.append(CommentSnapshot(
            comment_id=comment_id,
            author_id=_field(metadata_block, "author_id"),
            author_name=_field(metadata_block, "author_name"),
            parent_comment_id=_as_int(_field(metadata_block, "parent_comment_id")),
            created_at=_field(metadata_block, "created_at"),
            content="\n".join(content_lines).strip(),
            floor_no=len(comments) + 1,
        ))
    return tuple(comments)


class SnapshotStore:
    """A small, deterministic read-only repository backed by Markdown files."""

    def __init__(self, snapshot_dir: str | None = None):
        # The Java service's exported directory is configured in agent.yml.
        # Keep the relative default only for backwards-compatible local demos.
        configured_dir = snapshot_dir or agent_conf.get("file_path", "data/knowledge-snapshots")
        self.snapshot_dir = Path(configured_dir)
        if not self.snapshot_dir.is_absolute():
            self.snapshot_dir = Path(get_abs_path(configured_dir))
        configured_manifest = agent_conf.get("snapshot_manifest_path", "data/.snapshot-md5.json")
        self.manifest_path = Path(configured_manifest)
        if not self.manifest_path.is_absolute():
            self.manifest_path = Path(get_abs_path(configured_manifest))
        self._file_md5: dict[str, str] = {}
        self._file_post_ids: dict[str, int] = {}
        self._posts: dict[int, PostSnapshot] = {}
        self._initialized = False
        self._lock = RLock()

    def _all_markdown_paths(self) -> list[Path]:
        if not self.snapshot_dir.is_dir():
            return []
        # Java stores posts by scene: scene-{sceneId}/post-{postId}.md.
        # Only the scene-scoped layout is authoritative. Root-level legacy files
        # are ignored so a stale file cannot leak into another scene.
        return sorted(
            path for path in self.snapshot_dir.rglob("post-*.md")
            if path.is_file() and len(path.relative_to(self.snapshot_dir).parts) == 2
        )

    @staticmethod
    def _md5(path: Path) -> str:
        digest = hashlib.md5()
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
        return digest.hexdigest()

    def refresh(self, force: bool = False) -> dict[str, int]:
        """Incrementally scan snapshots and parse only new/changed files.

        The first call reads every Markdown file and persists a filename→MD5
        manifest. Later calls compare MD5 values, reparse changed files and
        evict files removed from the directory.
        """
        with self._lock:
            paths = self._all_markdown_paths()
            current = {str(path): self._md5(path) for path in paths}
            changed = set(current) if force or not self._initialized else {
                filename for filename, checksum in current.items()
                if self._file_md5.get(filename) != checksum
            }
            removed = set(self._file_md5) - set(current)
            for filename in removed:
                self._remove_file_index(filename)
            loaded = 0
            for filename in changed:
                post_id = self._post_id_from_path(filename)
                if post_id is None:
                    continue
                try:
                    parsed = self._parse_post(Path(filename), post_id)
                    if parsed is not None:
                        self._remove_file_index(filename)
                        self._remove_duplicate_post_index(parsed.post_id, filename)
                        self._posts[parsed.post_id] = parsed
                        self._file_post_ids[filename] = parsed.post_id
                        loaded += 1
                except (OSError, UnicodeError, TypeError, ValueError, yaml.YAMLError):
                    # A malformed file is excluded until its contents change.
                    self._remove_file_index(filename)
                    continue
            manifest_changed = current != self._file_md5
            self._file_md5 = current
            self._file_post_ids = {
                filename: post_id for filename, post_id in self._file_post_ids.items()
                if filename in current
            }
            self._initialized = True
            if self.snapshot_dir.is_dir() and (manifest_changed or force):
                self.manifest_path.parent.mkdir(parents=True, exist_ok=True)
                self.manifest_path.write_text(json.dumps(current, ensure_ascii=False, indent=2), encoding="utf-8")
            return {"scanned": len(paths), "changed": loaded, "removed": len(removed)}

    def _remove_file_index(self, filename: str) -> None:
        post_id = self._file_post_ids.pop(filename, None) or self._post_id_from_path(filename)
        if post_id is not None and not any(value == post_id for value in self._file_post_ids.values()):
            self._posts.pop(post_id, None)

    def _remove_duplicate_post_index(self, post_id: int, except_filename: str) -> None:
        for filename, indexed_id in list(self._file_post_ids.items()):
            if filename != except_filename and indexed_id == post_id:
                self._file_post_ids.pop(filename, None)

    @staticmethod
    def _post_id_from_path(filename: str) -> int | None:
        match = re.match(r"post-(\d+)\.md$", Path(filename).name)
        return int(match.group(1)) if match else None

    def _parse_post(self, path: Path, post_id: int) -> PostSnapshot | None:
        metadata, body = _parse_frontmatter(path.read_text(encoding="utf-8"))
        if metadata.get("scene_id") is None:
            return None
        scene_id = int(metadata["scene_id"])
        relative = path.relative_to(self.snapshot_dir)
        expected_directory = f"scene-{scene_id}"
        if len(relative.parts) != 2 or relative.parts[-2] != expected_directory:
            return None
        metadata_post_id = _as_int(metadata.get("post_id"))
        if metadata_post_id is not None and metadata_post_id != post_id:
            return None
        title_match = re.search(r"(?m)^#\s+(.+?)\s*$", body)
        title = title_match.group(1).strip() if title_match else f"帖子 {post_id}"
        content_end = re.search(r"(?m)^##\s+评论区\s*$", body)
        post_body = body[:content_end.start()].strip() if content_end else body.strip()
        topics = tuple(m.group(1).strip() for m in re.finditer(r"(?m)^\*\*话题\*\*：(.+?)\s*$", post_body))
        activities = tuple(m.group(1).strip() for m in re.finditer(r"(?m)^\*\*关联活动\*\*：(.+?)\s*$", post_body))
        return PostSnapshot(
            post_id=int(metadata.get("post_id", post_id)), scene_id=scene_id,
            scene_name=metadata.get("scene_name"), visibility=str(metadata.get("visibility", "PUBLIC")).upper(),
            status=str(metadata.get("status", "PUBLISHED")).upper(),
            author_id=str(metadata["author_id"]) if metadata.get("author_id") is not None else None,
            author_name=metadata.get("author_name"), post_type=metadata.get("post_type"),
            created_at=str(metadata["created_at"]) if metadata.get("created_at") is not None else None,
            updated_at=str(metadata["updated_at"]) if metadata.get("updated_at") is not None else None,
            title=title, body=post_body, topics=topics, activities=activities,
            comments=_parse_comments(body), source_path=str(path),
        )

    def load_post(self, post_id: int) -> PostSnapshot | None:
        self.refresh()
        with self._lock:
            return self._posts.get(post_id)

    def list_posts(self, scene_id: int, permitted_visibilities: set[str] | None = None) -> list[PostSnapshot]:
        self.refresh()
        allowed = {v.upper() for v in (permitted_visibilities or {"PUBLIC"})}
        with self._lock:
            return [
                post for post in self._posts.values()
                if post and post.scene_id == scene_id
                and post.visibility in allowed and post.status == "PUBLISHED"
            ]

    @staticmethod
    def _tokens(text: str) -> set[str]:
        # Keep Chinese runs and latin/numeric words; this is intentionally
        # lightweight because the source of truth is the snapshot, not a DB.
        return {token.lower() for token in re.findall(r"[\u4e00-\u9fff]{1,}|[a-zA-Z0-9_]+", text) if token.strip()}

    def search(self, scene_id: int, query: str, content_types: list[str] | None = None,
               time_range: str | None = None, permitted_visibilities: set[str] | None = None,
               limit: int = 10) -> list[dict[str, Any]]:
        allowed_types = {t.lower() for t in (content_types or ["post", "comment"])}
        query_tokens = self._tokens(query)
        cutoff: datetime | None = None
        if time_range:
            # Accept common ISO date/time strings as an explicit lower bound.
            try:
                cutoff = datetime.fromisoformat(time_range.replace("Z", "+00:00"))
            except ValueError:
                cutoff = None
        results: list[tuple[float, dict[str, Any]]] = []
        for post in self.list_posts(scene_id, permitted_visibilities):
            candidates: list[tuple[str, int | None, str, str, str | None, int | None]] = []
            if "post" in allowed_types:
                candidates.append(("post", None, f"{post.title}\n{post.body}", post.title, post.created_at, None))
            if "comment" in allowed_types:
                candidates.extend(("comment", c.comment_id, c.content, post.title, c.created_at, c.floor_no) for c in post.comments)
            for content_type, content_id, text, title, created_at, floor_no in candidates:
                if cutoff and created_at:
                    try:
                        created = datetime.fromisoformat(created_at.replace("Z", "+00:00"))
                        if created < cutoff:
                            continue
                    except (TypeError, ValueError):
                        pass
                tokens = self._tokens(text)
                lexical = len(query_tokens & tokens) / max(len(query_tokens), 1)
                substring = 0.25 if query.strip() and query.strip().lower() in text.lower() else 0.0
                if lexical == 0 and substring == 0:
                    continue
                results.append((lexical + substring, {
                    "content_type": content_type,
                    "content_id": post.post_id if content_type == "post" else content_id,
                    "post_id": post.post_id,
                    "scene_id": post.scene_id,
                    "title": title,
                    "excerpt": text[:500],
                    "floor_no": floor_no,
                    "created_at": created_at,
                }))
        results.sort(key=lambda item: item[0], reverse=True)
        return [item[1] for item in results[: max(1, min(limit, 50))]]

    def thread_context(self, post_id: int, comment_ids: list[int] | None = None,
                       permitted_visibilities: set[str] | None = None) -> dict[str, Any] | None:
        post = self.load_post(post_id)
        if not post or post.visibility not in {v.upper() for v in (permitted_visibilities or {"PUBLIC"})} or post.status != "PUBLISHED":
            return None
        selected = set(comment_ids or [c.comment_id for c in post.comments])
        comments = [
            {
                "comment_id": comment.comment_id,
                "author_name": comment.author_name,
                "parent_comment_id": comment.parent_comment_id,
                "created_at": comment.created_at,
                "content": comment.content,
                "floor_no": comment.floor_no,
            }
            for comment in post.comments if comment.comment_id in selected
        ]
        return {
            "post": {
                "post_id": post.post_id,
                "scene_name": post.scene_name,
                "post_type": post.post_type,
                "created_at": post.created_at,
                "updated_at": post.updated_at,
                "title": post.title,
                "body": post.body,
                "topics": list(post.topics),
                "activities": list(post.activities),
            },
            "comments": comments,
        }
