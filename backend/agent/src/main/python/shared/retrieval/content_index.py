"""Small deterministic snapshot index shared by all four agents.

It is intentionally lexical first: embeddings can be added behind this API
without changing permissions, metadata or citation contracts.
"""
from __future__ import annotations
from datetime import datetime
import re
from pathlib import Path
from shared.models import ContentChunk


class SnapshotChunkIndex:
    def __init__(self, root: str | Path, chunk_size: int = 700, overlap: int = 80):
        self.root = Path(root)
        self.chunk_size = max(500, chunk_size)
        self.overlap = max(0, min(overlap, self.chunk_size // 3))
        self._chunks: list[ContentChunk] | None = None

    def chunks(self) -> list[ContentChunk]:
        if self._chunks is not None:
            return self._chunks
        output: list[ContentChunk] = []
        for path in sorted(self.root.glob("scene-*/post-*.md")):
            post = self._read_post(path)
            if post:
                output.extend(post)
        self._chunks = output
        return output

    def search(self, query: str, *, scene_id: int | None = None,
               permitted_post_ids: set[int] | None = None, max_items: int = 10) -> list[ContentChunk]:
        terms = [term.casefold() for term in re.findall(r"[\w\u4e00-\u9fff]+", query) if len(term.strip()) > 1]
        ranked = []
        for chunk in self.chunks():
            if chunk.review_status != "PUBLISHED" or chunk.visibility != "PUBLIC":
                continue
            if scene_id is not None and chunk.scene_id != scene_id:
                continue
            if permitted_post_ids is not None and (chunk.parent_post_id or chunk.content_id) not in permitted_post_ids:
                continue
            folded = chunk.text.casefold()
            score = sum(folded.count(term) for term in terms)
            if score:
                ranked.append((score, chunk))
        ranked.sort(key=lambda item: (-item[0], item[1].chunk_id))
        return [chunk for _, chunk in ranked[:max_items]]

    def _read_post(self, path: Path) -> list[ContentChunk]:
        text = path.read_text(encoding="utf-8")
        front = re.match(r"^---\s*\n(.*?)\n---\s*\n", text, re.S)
        metadata = self._yaml(front.group(1) if front else "")
        try:
            post_id = int(metadata["post_id"]); scene_id = int(metadata["scene_id"])
        except (KeyError, ValueError):
            return []
        body = text[front.end():] if front else text
        title_match = re.search(r"^#\s+(.+)$", body, re.M)
        title = title_match.group(1).strip() if title_match else ""
        body = re.sub(r"^#\s+.+$", "", body, count=1, flags=re.M).strip()
        body = re.split(r"\n##\s*评论区", body, maxsplit=1)[0].strip()
        source = f"{title}\n{body}".strip()
        chunks = self._split(source)
        created = self._date(metadata.get("created_at")); updated = self._date(metadata.get("updated_at"))
        return [ContentChunk(chunk_id=f"post:{post_id}:chunk:{index}", scene_id=scene_id, content_type="post",
            content_id=post_id, parent_post_id=None, text=chunk, visibility=metadata.get("visibility", "PUBLIC"),
            review_status=metadata.get("status", "PUBLISHED"), created_at=created, updated_at=updated,
            source_url=f"/community/posts/{post_id}") for index, chunk in enumerate(chunks)]

    def _split(self, text: str) -> list[str]:
        paragraphs = [part.strip() for part in re.split(r"\n\s*\n", text) if part.strip()]
        chunks, current = [], ""
        for paragraph in paragraphs:
            if current and len(current) + len(paragraph) + 1 > self.chunk_size:
                chunks.append(current)
                current = current[-self.overlap:] + "\n" + paragraph
            else:
                current = f"{current}\n{paragraph}".strip()
        if current: chunks.append(current)
        return chunks or [text[:self.chunk_size]]

    @staticmethod
    def _yaml(value: str) -> dict[str, str]:
        result = {}
        for line in value.splitlines():
            if ":" in line:
                key, item = line.split(":", 1); result[key.strip()] = item.strip().strip("\"'")
        return result

    @staticmethod
    def _date(value: str | None) -> datetime | None:
        try: return datetime.fromisoformat(value.replace("Z", "+00:00")) if value else None
        except ValueError: return None
