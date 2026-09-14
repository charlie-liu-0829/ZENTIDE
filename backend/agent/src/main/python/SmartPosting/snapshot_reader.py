"""Read-only reader for Java-exported published post snapshots."""

from __future__ import annotations

from dataclasses import dataclass
import re
from pathlib import Path
from typing import Any

try:
    import yaml
    YAML_ERROR = yaml.YAMLError
except ImportError:  # The exported frontmatter only needs simple scalar parsing.
    yaml = None
    YAML_ERROR = ValueError


@dataclass(frozen=True)
class PublishedPost:
    post_id: int
    scene_id: int
    title: str
    body: str
    topics: tuple[str, ...]
    visibility: str
    status: str
    has_accepted_answer: bool = False


class PublishedSnapshotReader:
    """Only accepts ``scene-N/post-N.md`` snapshots written by the Java service."""

    def __init__(self, snapshot_dir: str):
        self.snapshot_dir = Path(snapshot_dir).expanduser().resolve()

    def _parse(self, path: Path) -> PublishedPost | None:
        text = path.read_text(encoding="utf-8")
        match = re.match(r"\A---\s*\n(.*?)\n---\s*\n?(.*)\Z", text, re.DOTALL)
        if not match:
            return None
        if yaml is not None:
            metadata: dict[str, Any] = yaml.safe_load(match.group(1)) or {}
        else:
            metadata = {}
            for line in match.group(1).splitlines():
                if ":" not in line:
                    continue
                key, value = line.split(":", 1)
                metadata[key.strip()] = value.strip().strip('"\'')
        post_id = int(metadata.get("post_id") or 0)
        scene_id = int(metadata.get("scene_id") or 0)
        relative = path.relative_to(self.snapshot_dir)
        if post_id <= 0 or scene_id <= 0 or relative.parts != (f"scene-{scene_id}", f"post-{post_id}.md"):
            return None
        body = match.group(2)
        comments = re.search(r"(?m)^##\s+评论区\s*$", body)
        body = body[:comments.start()] if comments else body
        title_match = re.search(r"(?m)^#\s+(.+?)\s*$", body)
        topics = tuple(item.strip() for item in re.findall(r"(?m)^\*\*话题\*\*：(.+?)\s*$", body))
        return PublishedPost(
            post_id=post_id, scene_id=scene_id,
            title=title_match.group(1).strip() if title_match else f"帖子 {post_id}",
            body=body.strip(), topics=topics,
            visibility=str(metadata.get("visibility", "PUBLIC")).upper(),
            status=str(metadata.get("status", "PUBLISHED")).upper(),
            has_accepted_answer=str(metadata.get("has_accepted_answer", "false")).lower() in {"1", "true", "yes"},
        )

    def list_posts(self, scene_id: int, permitted_visibilities: set[str]) -> list[PublishedPost]:
        allowed = {item.upper() for item in permitted_visibilities}
        scene_dir = self.snapshot_dir / f"scene-{scene_id}"
        if not scene_dir.is_dir():
            return []
        posts: list[PublishedPost] = []
        for path in sorted(scene_dir.glob("post-*.md")):
            try:
                post = self._parse(path)
            except (OSError, UnicodeError, TypeError, ValueError, YAML_ERROR):
                continue
            if post and post.status == "PUBLISHED" and post.visibility in allowed:
                posts.append(post)
        return posts

    def load_post(self, post_id: int) -> PublishedPost | None:
        if not self.snapshot_dir.is_dir():
            return None
        for path in self.snapshot_dir.glob(f"scene-*/post-{post_id}.md"):
            try:
                post = self._parse(path)
            except (OSError, UnicodeError, TypeError, ValueError, YAML_ERROR):
                continue
            if post:
                return post
        return None
