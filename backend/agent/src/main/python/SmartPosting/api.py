"""Internal HTTP API consumed by the authenticated Spring Boot application."""

from __future__ import annotations

import os
from pathlib import Path
from typing import Annotated, Literal

from fastapi import Depends, FastAPI, Header, HTTPException, status
from pydantic import BaseModel, Field, field_validator

from review_service import PostReviewService
from snapshot_reader import PublishedSnapshotReader


class TopicOption(BaseModel):
    topic_id: Annotated[int, Field(gt=0)]
    name: Annotated[str, Field(min_length=1, max_length=80)]


class SimilarPost(BaseModel):
    post_id: Annotated[int, Field(gt=0)]
    title: str
    similarity: Annotated[float, Field(ge=0, le=1)]
    reason: str
    has_accepted_answer: bool = False


class PostReviewResult(BaseModel):
    review_id: str
    duplicate_probability: Annotated[float, Field(ge=0, le=1)]
    similar_posts: list[SimilarPost]
    suggested_title: str | None
    suggested_post_type: str | None
    suggested_topic_ids: list[int]
    missing_information: list[str]
    sensitive_information_warnings: list[str]
    community_rule_warnings: list[str]
    optimized_content: str | None
    content_improvements: list[str]
    publish_blocked: bool
    blocking_reasons: list[str]
    advice: Literal["ready", "revise", "view_similar_posts", "manual_review"]
    draft_id: str | None = None
    checked_at: str
    analysis_mode: Literal["llm", "rules"]


class ReviewRequest(BaseModel):
    scene_id: Annotated[int, Field(gt=0)]
    title: Annotated[str, Field(min_length=1, max_length=220)]
    content: Annotated[str, Field(min_length=1, max_length=50000)]
    selected_post_type: Annotated[str | None, Field(max_length=24)] = None
    selected_topic_ids: list[Annotated[int, Field(gt=0)]] = Field(default_factory=list, max_length=20)
    draft_id: Annotated[str | None, Field(max_length=100)] = None
    user_id: Annotated[str, Field(min_length=1, max_length=64)]
    permitted_visibilities: list[str] = Field(default_factory=lambda: ["PUBLIC"], max_length=10)
    available_topics: list[TopicOption] = Field(default_factory=list, max_length=100)

    @field_validator("title", "content")
    @classmethod
    def strip_text(cls, value: str) -> str:
        return value.strip()


class ValidateRequest(ReviewRequest):
    review_id: Annotated[str, Field(pattern=r"^review_[0-9a-f]{32}$")]


class ReviewEventRequest(BaseModel):
    review_id: Annotated[str, Field(pattern=r"^review_[0-9a-f]{32}$")]
    user_id: Annotated[str, Field(min_length=1, max_length=64)]
    scene_id: Annotated[int, Field(gt=0)]
    event: Literal[
        "similar_post_clicked", "suggested_title_accepted", "suggested_topic_accepted",
        "optimized_content_accepted",
    ]
    target_id: Annotated[int | None, Field(gt=0)] = None


def verify_internal_token(x_zentide_agent_token: str | None = Header(default=None)) -> None:
    configured = os.getenv("ZENTIDE_AGENT_INTERNAL_TOKEN", "").strip()
    if configured and x_zentide_agent_token != configured:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Agent 服务认证失败")


snapshot_root = os.getenv(
    "ZENTIDE_KNOWLEDGE_SNAPSHOT_DIR",
    str(Path(__file__).resolve().parents[6] / "data" / "knowledge-snapshots"),
)
review_service = PostReviewService(PublishedSnapshotReader(snapshot_root))
app = FastAPI(title="ZENTIDE Smart Posting Review API", version="1.0.0", docs_url=None, redoc_url=None)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post(
    "/v1/posts/review",
    response_model=PostReviewResult,
    dependencies=[Depends(verify_internal_token)],
)
def review(request: ReviewRequest) -> dict:
    return review_service.review(
        user_id=request.user_id, scene_id=request.scene_id, title=request.title, content=request.content,
        selected_post_type=request.selected_post_type, selected_topic_ids=request.selected_topic_ids,
        permitted_visibilities=set(request.permitted_visibilities),
        available_topics=[topic.model_dump() for topic in request.available_topics],
        draft_id=request.draft_id,
    )


@app.post("/v1/posts/review/validate", dependencies=[Depends(verify_internal_token)])
def validate_review(request: ValidateRequest) -> dict:
    return review_service.validate(
        review_id=request.review_id, user_id=request.user_id, scene_id=request.scene_id,
        title=request.title, content=request.content, selected_post_type=request.selected_post_type,
        selected_topic_ids=request.selected_topic_ids,
    )


@app.post("/v1/posts/review/events", dependencies=[Depends(verify_internal_token)])
def review_event(request: ReviewEventRequest) -> dict[str, bool]:
    record = review_service.store.get(request.review_id, request.user_id, request.scene_id)
    if record is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="审核结果不存在或已过期")
    return {"accepted": True}
