"""Production-facing API boundary for personalized insights.

Spring Boot should inject X-User-Id after authenticating the request.  The
browser never supplies a trusted user identity.
"""

from __future__ import annotations

import os
from pathlib import Path
from typing import Annotated, Any

try:
    from fastapi import Depends, FastAPI, Header, HTTPException, status
    from pydantic import BaseModel, Field
except ImportError:  # keep the deterministic core importable without web extras
    FastAPI = None  # type: ignore

from insight_agent import PersonalizedInsightAgent, SnapshotReader
from interest_store import InterestStore
from model.insight_intelligence import QwenEvidenceSummarizer


SNAPSHOT_ROOT = os.getenv("ZENTIDE_KNOWLEDGE_SNAPSHOT_DIR", str(Path(__file__).resolve().parents[6] / "data" / "knowledge-snapshots"))
reader = SnapshotReader(SNAPSHOT_ROOT)
agent = PersonalizedInsightAgent(reader, QwenEvidenceSummarizer())
store = InterestStore(os.getenv("ZENTIDE_INTEREST_DB", str(Path(__file__).resolve().parent / "data" / "interests.sqlite3")))


def _require_user(x_user_id: str | None, x_internal_token: str | None) -> str:
    configured = os.getenv("ZENTIDE_AGENT_INTERNAL_TOKEN", "").strip()
    if configured and x_internal_token != configured:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="服务认证失败")
    if not x_user_id or len(x_user_id) > 64:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="缺少登录用户")
    return x_user_id


if FastAPI is not None:
    class InsightRequest(BaseModel):
        scene_id: Annotated[int | None, Field(gt=0)] = None
        keywords: list[Annotated[str, Field(min_length=1, max_length=80)]] = Field(default_factory=list, max_length=30)
        seen_post_ids: list[Annotated[int, Field(gt=0)]] = Field(default_factory=list, max_length=500)
        permitted_post_ids: list[Annotated[int, Field(gt=0)]] | None = Field(default=None, max_length=5000)
        max_items: Annotated[int, Field(ge=1, le=30)] = 10

    class KeywordRequest(BaseModel):
        keyword: Annotated[str, Field(min_length=1, max_length=80)]

    class KeywordStatusRequest(BaseModel):
        enabled: bool

    class FeedbackRequest(BaseModel):
        item_id: Annotated[str, Field(min_length=1, max_length=100)]
        action: Annotated[str, Field(pattern=r"^(viewed|saved|not_interested|ignored)$")]
        item: dict[str, Any] | None = None

    app = FastAPI(title="ZENTIDE Personalized Insight Agent", version="1.0.0", docs_url=None, redoc_url=None)

    def current_user(x_user_id: str | None = Header(default=None), x_internal_token: str | None = Header(default=None)) -> str:
        return _require_user(x_user_id, x_internal_token)

    @app.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok"}

    @app.post("/v1/users/insights")
    def insights(request: InsightRequest, user_id: str = Depends(current_user)) -> dict:
        keywords = request.keywords or [row["keyword"] for row in store.list_keywords(user_id, enabled_only=True)]
        scene_ids = {request.scene_id} if request.scene_id else None
        result = agent.generate(
            keywords, scene_ids=scene_ids,
            permitted_post_ids=set(request.permitted_post_ids) if request.permitted_post_ids is not None else None,
            # viewed 只用于行为统计，不作为永久过滤条件；用户看过的内容仍可在后续情报中再次出现。
            # 只有调用方显式传入的 seen_post_ids 才会被排除。
            seen_post_ids=set(request.seen_post_ids),
            max_items=request.max_items,
        )
        return result.to_dict()

    @app.get("/v1/users/interest-keywords")
    def list_keywords(user_id: str = Depends(current_user)) -> dict:
        return {"items": store.list_keywords(user_id)}

    @app.post("/v1/users/interest-keywords")
    def add_keyword(request: KeywordRequest, user_id: str = Depends(current_user)) -> dict:
        try:
            return store.add_keyword(user_id, request.keyword)
        except ValueError as error:
            raise HTTPException(status_code=400, detail=str(error)) from error

    @app.delete("/v1/users/interest-keywords/{keyword_id}")
    def delete_keyword(keyword_id: int, user_id: str = Depends(current_user)) -> dict[str, bool]:
        return {"deleted": store.delete_keyword(user_id, keyword_id)}

    @app.patch("/v1/users/interest-keywords/{keyword_id}")
    def set_keyword_status(
        keyword_id: int, request: KeywordStatusRequest, user_id: str = Depends(current_user)
    ) -> dict[str, bool]:
        return {"updated": store.set_enabled(user_id, keyword_id, request.enabled)}

    @app.post("/v1/users/insights/feedback")
    def feedback(request: FeedbackRequest, user_id: str = Depends(current_user)) -> dict[str, bool]:
        try:
            store.record_feedback(user_id, request.item_id, request.action)
            if request.action == "saved":
                if request.item is None:
                    raise ValueError("收藏情报时缺少情报内容")
                store.save_insight(user_id, request.item_id, request.item)
        except ValueError as error:
            raise HTTPException(status_code=400, detail=str(error)) from error
        return {"accepted": True}

    @app.get("/v1/users/insights/saved")
    def saved_insights(user_id: str = Depends(current_user)) -> dict:
        items = store.list_saved_insights(user_id)
        return {"item_ids": [item["item_id"] for item in items], "items": items}

    @app.delete("/v1/users/insights/saved/{item_id}")
    def unsave_insight(item_id: str, user_id: str = Depends(current_user)) -> dict[str, bool]:
        return {"deleted": store.remove_saved_insight(user_id, item_id)}
else:
    app = None
