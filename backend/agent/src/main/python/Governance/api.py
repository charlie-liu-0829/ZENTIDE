from __future__ import annotations

import os
import sys
import requests
import logging
import html
import re
from html.parser import HTMLParser
from pathlib import Path
from typing import Annotated, Literal

from fastapi import Depends, FastAPI, Header, HTTPException, status
from pydantic import BaseModel, Field

# Support both `python run_api.py` and direct imports in admin tests/tools.
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from governance_agent import GovernanceAgent
from shared.retrieval import SnapshotChunkIndex

logger = logging.getLogger("zentide.governance")
agent = GovernanceAgent()
app = FastAPI(title="ZENTIDE Governance Agent", version="1.0.0", docs_url=None, redoc_url=None)
SNAPSHOT_ROOT = os.getenv(
    "ZENTIDE_KNOWLEDGE_SNAPSHOT_DIR",
    str(Path(__file__).resolve().parents[6] / "data" / "knowledge-snapshots"),
)
snapshot_index = SnapshotChunkIndex(SNAPSHOT_ROOT)
CONTROL_PLANE_URL = os.getenv("ZENTIDE_GOVERNANCE_CONTROL_PLANE_URL", "http://127.0.0.1:6061/api/zentide/v1/admin/governance/internal").rstrip("/")


class _ReadableTextParser(HTMLParser):
    """Turn snapshot HTML into readable text without exposing markup."""
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.parts: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag in {"p", "div", "br", "li", "h1", "h2", "h3"}:
            self.parts.append("\n")

    def handle_endtag(self, tag: str) -> None:
        if tag in {"p", "div", "li", "h1", "h2", "h3"}:
            self.parts.append("\n")

    def handle_data(self, data: str) -> None:
        self.parts.append(data)


def readable_snapshot(value: str) -> str:
    """Remove HTML/Markdown noise and normalize a stored snapshot."""
    parser = _ReadableTextParser()
    parser.feed(value or "")
    text = html.unescape("".join(parser.parts))
    text = re.sub(r"\[([^\]]+)]\((?:https?://[^)]+)\)", r"\1", text)
    text = re.sub(r"[*_`#]+", "", text)
    lines = [re.sub(r"\s+", " ", line).strip() for line in text.splitlines()]
    return "\n".join(line for line in lines if line)


def snapshot_summary(value: str, max_length: int = 260) -> str:
    """Build an administrator-facing evidence card; internal IDs stay hidden."""
    clean = readable_snapshot(value)
    lines = clean.splitlines()
    if not lines:
        return "参考内容：暂无可读正文"
    title = lines[0][:100]
    body_lines = [line for line in lines[1:] if not line.startswith("话题：") and not line.startswith("关联活动：")]
    body = " ".join(body_lines).strip()
    if len(body) > max_length:
        body = body[:max_length].rstrip() + "…"
    return f"参考内容\n标题：{title}\n正文摘要：{body or '暂无正文'}"

def control_plane(path: str, payload: dict) -> dict:
    headers = {}
    token = os.getenv("ZENTIDE_AGENT_INTERNAL_TOKEN", "").strip()
    if token: headers["X-Zentide-Agent-Token"] = token
    try:
        response = requests.post(CONTROL_PLANE_URL + path, json=payload, headers=headers, timeout=8)
        response.raise_for_status()
    except requests.RequestException:
        logger.exception("Spring Boot 治理控制面调用失败：%s%s", CONTROL_PLANE_URL, path)
        raise
    body = response.json()
    return body.get("data", body)


def rules_from_control_plane(scene_id: int) -> list[dict]:
    result = control_plane("/rules", {"scene_id": scene_id})
    rules = result.get("rules")
    if not isinstance(rules, list):
        raise RuntimeError("Spring Boot 未返回有效治理规则")
    return rules


def persist_result(result: dict, content_id: int, scene_id: int) -> None:
    control_plane("/result", {**result, "content_id": content_id, "scene_id": scene_id})


def auth(x_zentide_agent_token: str | None = Header(default=None)) -> None:
    expected = os.getenv("ZENTIDE_AGENT_INTERNAL_TOKEN", "").strip()
    if expected and x_zentide_agent_token != expected:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Agent 服务认证失败")


class Rule(BaseModel):
    rule_id: Annotated[int, Field(gt=0)]
    violation_type: str = "community_rule"
    keywords: list[str] = Field(default_factory=list)
    rule_description: str = Field(default="", max_length=2000)
    severity: Literal["low", "medium", "high"] = "medium"


class GovernanceRequest(BaseModel):
    content_id: Annotated[int, Field(gt=0)]
    scene_id: Annotated[int, Field(gt=0)]
    title: Annotated[str, Field(min_length=1, max_length=220)]
    content: Annotated[str, Field(min_length=1, max_length=50000)]
    rules: list[Rule] = Field(default_factory=list, max_length=200)
    cases: list[dict] = Field(default_factory=list, max_length=200)


class GovernanceScanRequest(BaseModel):
    """Scan all published public posts; no post content is supplied by the UI."""
    max_posts: Annotated[int, Field(gt=0, le=1000)] = 500

class GovernanceRuleRequest(BaseModel):
    scene_id: Annotated[int, Field(gt=0)]
    violation_type: str = Field(min_length=1, max_length=80)
    rule_description: str = Field(min_length=10, max_length=2000)
    keywords: list[str] = Field(default_factory=list, max_length=50)
    severity: Literal["low", "medium", "high"] = "medium"


class ModerationFeedback(BaseModel):
    content_id: Annotated[int, Field(gt=0)]
    agent_result_id: Annotated[str, Field(pattern=r"^gov_[0-9a-f]{32}$")]
    final_action: Annotated[str, Field(min_length=1, max_length=40)]
    accepted_agent_advice: bool
    corrected_violation_types: list[str] = Field(default_factory=list, max_length=30)
    reviewer_reason: Annotated[str, Field(min_length=1, max_length=2000)]


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/v1/governance/review", dependencies=[Depends(auth)])
def review(request: GovernanceRequest) -> dict:
    # Governance always reads the current published snapshot.  The request's
    # rules/cases are optional overrides from the admin UI, never the sole
    # source of context.
    snapshot_matches = snapshot_index.search(
        f"{request.title}\n{request.content}", scene_id=request.scene_id, max_items=8
    )
    snapshot_evidence = [snapshot_summary(chunk.text) for chunk in snapshot_matches]
    rules = rules_from_control_plane(request.scene_id)
    result = agent.review(content_id=request.content_id, scene_id=request.scene_id, title=request.title,
                          content=request.content, rules=rules, cases=request.cases,
                          snapshot_evidence=snapshot_evidence)
    persist_result(result, request.content_id, request.scene_id)
    return result


@app.post("/v1/governance/scan", dependencies=[Depends(auth)])
def scan(request: GovernanceScanRequest) -> dict[str, object]:
    """Automatically review every published public post across all scenes."""
    grouped: dict[tuple[int, int], list[str]] = {}
    for chunk in snapshot_index.chunks():
        if chunk.review_status != "PUBLISHED" or chunk.visibility != "PUBLIC":
            continue
        grouped.setdefault((chunk.scene_id, chunk.content_id), []).append(chunk.text)

    rules = rules_from_control_plane(0)
    results: list[dict] = []
    for (scene_id, content_id), fragments in sorted(grouped.items())[:request.max_posts]:
        raw_text = "\n".join(dict.fromkeys(sorted(fragments))).strip()
        text = readable_snapshot(raw_text)
        lines = [line.strip() for line in text.splitlines() if line.strip()]
        title = lines[0].lstrip("# ")[:220] if lines else f"帖子 {content_id}"
        content = "\n".join(lines[1:])[:50000] if len(lines) > 1 else title
        result = agent.review(
            content_id=content_id,
            scene_id=scene_id,
            title=title,
            content=content,
            rules=rules,
            snapshot_evidence=[snapshot_summary(raw_text)],
        )
        if result["risk_level"] != "low":
            result["content_id"] = content_id
            result["scene_id"] = scene_id
            result["title"] = title
            results.append(result)
        persist_result(result, content_id, scene_id)
    results.sort(key=lambda item: (0 if item["risk_level"] == "high" else 1, -item["confidence"]))
    return {
        "scanned_count": min(len(grouped), request.max_posts),
        "risk_count": len(results),
        "results": results,
        "rules": rules,
    }

@app.get("/v1/governance/rules", dependencies=[Depends(auth)])
def list_rules(scene_id: Annotated[int, Field(gt=0)]) -> dict[str, object]:
    return {"scene_id": scene_id, "rules": rules_from_control_plane(scene_id)}

@app.post("/v1/governance/rules", dependencies=[Depends(auth)])
def create_rule(request: GovernanceRuleRequest) -> dict[str, object]:
    return control_plane("/rules", {"scene_id": request.scene_id, **request.model_dump(exclude={"scene_id"})})


@app.post("/v1/governance/feedback", dependencies=[Depends(auth)])
def feedback(request: ModerationFeedback, x_reviewer_id: str | None = Header(default=None)) -> dict[str, object]:
    raise HTTPException(status_code=status.HTTP_410_GONE, detail="治理反馈必须由 Spring Boot 管理端写入主库")
