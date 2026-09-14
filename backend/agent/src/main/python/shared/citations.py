from __future__ import annotations
from shared.models import ContentChunk


def citation(chunk: ContentChunk) -> dict:
    return {"chunk_id": chunk.chunk_id, "content_type": chunk.content_type, "content_id": chunk.content_id,
            "parent_post_id": chunk.parent_post_id, "scene_id": chunk.scene_id, "text": chunk.text,
            "url": chunk.source_url}
