from __future__ import annotations
from dataclasses import dataclass, field
from datetime import datetime


@dataclass(frozen=True)
class ContentChunk:
    chunk_id: str
    scene_id: int
    content_type: str
    content_id: int
    text: str
    parent_post_id: int | None = None
    author_id: int = 0
    topic_ids: list[int] = field(default_factory=list)
    visibility: str = "PUBLIC"
    review_status: str = "PUBLISHED"
    created_at: datetime | None = None
    updated_at: datetime | None = None
    source_url: str = ""
