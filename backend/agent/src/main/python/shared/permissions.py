from __future__ import annotations
from shared.models import ContentChunk


def filter_chunks(chunks: list[ContentChunk], *, scene_id: int | None = None,
                  permitted_post_ids: set[int] | None = None) -> list[ContentChunk]:
    """Final permission gate; never rely on model-generated citations."""
    return [chunk for chunk in chunks if chunk.review_status == "PUBLISHED" and chunk.visibility == "PUBLIC"
            and (scene_id is None or chunk.scene_id == scene_id)
            and (permitted_post_ids is None or (chunk.parent_post_id or chunk.content_id) in permitted_post_ids)]
