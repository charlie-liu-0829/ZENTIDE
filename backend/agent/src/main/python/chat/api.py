"""HTTP integration boundary for the ZENTIDE community assistant.

The Vue application never embeds the legacy Streamlit UI. Requests arrive via
the authenticated Spring Boot web service, which supplies the trusted user,
scene and visibility context used by the snapshot tools.
"""

from __future__ import annotations

from collections import OrderedDict
from dataclasses import dataclass, field
import logging
import os
import re
import json
from requests.exceptions import ReadTimeout, ConnectionError as RequestsConnectionError
from threading import Lock
from typing import Annotated, Literal
from uuid import uuid4

from fastapi import Depends, FastAPI, Header, HTTPException, status
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field, model_validator

from agent.snapshot_store import SnapshotStore
from agent.react_agent import sanitize_answer


logger = logging.getLogger(__name__)
CONVERSATION_ID = re.compile(r"^[A-Za-z0-9_-]{8,80}$")
POST_LINK = re.compile(
    r"\[(?P<label>(?:\\.|[^\]])+)\]"
    r"\((?P<url>https?://[^\s)]+/community/posts/(?P<post_id>[1-9]\d*))\)"
)


class AgentChatRequest(BaseModel):
    scene_id: Annotated[int, Field(gt=0)]
    question: Annotated[str, Field(min_length=1, max_length=2000)]
    mode: Literal["scene", "post"] = "scene"
    current_post_id: Annotated[int | None, Field(gt=0)] = None
    current_post_ids: list[Annotated[int, Field(gt=0)]] = Field(default_factory=list, max_length=8)
    conversation_id: str | None = None
    user_id: Annotated[str, Field(min_length=1, max_length=64)]
    permitted_visibilities: list[str] = Field(default_factory=lambda: ["PUBLIC"])

    @model_validator(mode="after")
    def validate_context(self):
        self.question = self.question.strip()
        if not self.question:
            raise ValueError("问题不能为空")
        if self.mode == "post" and self.current_post_id is None:
            if not self.current_post_ids:
                raise ValueError("帖子模式必须提供当前帖子")
            self.current_post_id = self.current_post_ids[0]
        if self.current_post_id and self.current_post_id not in self.current_post_ids:
            self.current_post_ids.insert(0, self.current_post_id)
        if self.conversation_id and not CONVERSATION_ID.fullmatch(self.conversation_id):
            raise ValueError("conversation_id 格式不正确")
        self.permitted_visibilities = sorted({
            value.strip().upper()
            for value in self.permitted_visibilities
            if isinstance(value, str) and value.strip()
        }) or ["PUBLIC"]
        return self


class AnswerBlock(BaseModel):
    type: Literal["text", "reference"]
    text: str | None = None
    entity_type: Literal["POST"] | None = None
    entity_id: int | None = None
    label: str | None = None


class AgentChatResponse(BaseModel):
    conversation_id: str
    answer: str
    blocks: list[AnswerBlock]


@dataclass
class ConversationMemory:
    max_conversations: int = 500
    max_messages: int = 6
    _items: OrderedDict[tuple[str, str, int, str, int | None], list[dict[str, str]]] = field(
        default_factory=OrderedDict
    )
    _lock: Lock = field(default_factory=Lock)

    def history(self, key: tuple[str, str, int, str, int | None]) -> list[dict[str, str]]:
        with self._lock:
            messages = list(self._items.get(key, []))
            if key in self._items:
                self._items.move_to_end(key)
            return messages

    def append(self, key: tuple[str, str, int, str, int | None], question: str, answer: str) -> None:
        with self._lock:
            messages = self._items.setdefault(key, [])
            messages.extend((
                {"role": "user", "content": question[:1200]},
                {"role": "assistant", "content": answer[:2400]},
            ))
            self._items[key] = messages[-self.max_messages:]
            self._items.move_to_end(key)
            while len(self._items) > self.max_conversations:
                self._items.popitem(last=False)


def answer_blocks(answer: str) -> list[AnswerBlock]:
    """Convert only validated internal post links into structured UI blocks."""
    answer = sanitize_answer(answer)
    blocks: list[AnswerBlock] = []
    cursor = 0
    for match in POST_LINK.finditer(answer):
        if match.start() > cursor:
            blocks.append(AnswerBlock(type="text", text=answer[cursor:match.start()]))
        label = re.sub(r"\\([\\\[\]()*_`])", r"\1", match.group("label"))
        blocks.append(AnswerBlock(
            type="reference",
            entity_type="POST",
            entity_id=int(match.group("post_id")),
            label=label,
        ))
        cursor = match.end()
    if cursor < len(answer):
        blocks.append(AnswerBlock(type="text", text=answer[cursor:]))
    return blocks or [AnswerBlock(type="text", text=answer)]


memory = ConversationMemory()
_agent: object | None = None
_agent_lock = Lock()
_active_requests: dict[tuple[str, str, int, str, int | None], Lock] = {}
_active_requests_lock = Lock()


def get_agent():
    global _agent
    if _agent is None:
        with _agent_lock:
            if _agent is None:
                from agent.react_agent import ReactAgent

                _agent = ReactAgent()
    return _agent


def verify_internal_token(x_zentide_agent_token: str | None = Header(default=None)) -> None:
    configured = os.getenv("ZENTIDE_AGENT_INTERNAL_TOKEN", "").strip()
    if configured and x_zentide_agent_token != configured:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Agent 服务认证失败")


def _request_key(request: AgentChatRequest, conversation_id: str):
    return (request.user_id, conversation_id, request.scene_id, request.mode,
            request.current_post_id, tuple(request.current_post_ids))


def _lock_for(key):
    with _active_requests_lock:
        return _active_requests.setdefault(key, Lock())


def _event(event_type: str, **payload) -> str:
    payload = {"event": event_type, **payload}
    return f"event: {event_type}\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"


def _run_chat(request: AgentChatRequest, conversation_id: str):
    key = _request_key(request, conversation_id)
    chunks = get_agent().execute_stream(
        request.question,
        scene_id=request.scene_id,
        mode=request.mode,
        user_id=request.user_id,
        conversation_id=conversation_id,
        current_post_id=request.current_post_id,
        selected_post_ids=request.current_post_ids,
        permitted_visibilities=set(request.permitted_visibilities),
        history=memory.history(key),
    )
    answer = ""
    for chunk in chunks:
        if isinstance(chunk, dict):
            kind = chunk.get("type")
            value = str(chunk.get("text") or "")
            if kind == "delta":
                answer += value
                if value:
                    yield "delta", value, answer
            elif kind == "final":
                answer = value
                yield "final", value, answer
            continue
        value = str(chunk or "")
        if value:
            answer += value
            yield "delta", value, answer
    answer = answer.strip() or "当前现场没有足够资料回答这个问题。"
    history_answer = POST_LINK.sub(lambda match: match.group("label"), answer)
    memory.append(key, request.question, history_answer)
    yield "done", answer, answer


app = FastAPI(
    title="ZENTIDE Community Agent API",
    version="1.0.0",
    docs_url=None,
    redoc_url=None,
)


@app.get("/health")
def health() -> dict[str, object]:
    stats = SnapshotStore().refresh()
    return {"status": "ok", "snapshots": stats}


@app.post(
    "/v1/chat",
    response_model=AgentChatResponse,
    dependencies=[Depends(verify_internal_token)],
)
def chat(request: AgentChatRequest) -> AgentChatResponse:
    conversation_id = request.conversation_id or uuid4().hex
    request_lock = _lock_for(_request_key(request, conversation_id))
    if not request_lock.acquire(blocking=False):
        raise HTTPException(status_code=409, detail="当前会话正在处理上一条问题，请稍候")
    try:
        answer = ""
        for event_type, value, complete in _run_chat(request, conversation_id):
            if event_type in {"final", "done"}:
                answer = complete
        blocks = answer_blocks(answer)
        return AgentChatResponse(
            conversation_id=conversation_id,
            answer=answer,
            blocks=blocks,
        )
    except (ValueError, FileNotFoundError) as error:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(error)) from error
    except Exception as error:
        logger.exception("Community agent request failed")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="社区小助手暂时无法回答，请稍后再试",
        ) from error
    finally:
        request_lock.release()


@app.post(
    "/v1/chat/stream",
    dependencies=[Depends(verify_internal_token)],
)
def chat_stream(request: AgentChatRequest) -> StreamingResponse:
    conversation_id = request.conversation_id or uuid4().hex
    request_lock = _lock_for(_request_key(request, conversation_id))
    if not request_lock.acquire(blocking=False):
        raise HTTPException(status_code=409, detail="当前会话正在处理上一条问题，请稍候")

    def generate():
        answer = ""
        try:
            yield _event("start", conversation_id=conversation_id)
            for event_type, value, complete in _run_chat(request, conversation_id):
                if event_type == "delta":
                    answer = complete
                    yield _event("delta", text=value)
                else:
                    answer = complete
            yield _event(
                "done",
                conversation_id=conversation_id,
                answer=answer,
                blocks=[block.model_dump() for block in answer_blocks(answer)],
            )
        except ReadTimeout:
            logger.warning("Community agent model request timed out")
            yield _event("error", code="MODEL_TIMEOUT", message="模型响应超时，请稍后重试")
        except RequestsConnectionError:
            logger.warning("Community agent model connection failed")
            yield _event("error", code="MODEL_CONNECTION", message="模型服务暂时无法连接，请稍后重试")
        except Exception:
            logger.exception("Community agent streaming request failed")
            yield _event("error", message="社区小助手暂时无法回答，请稍后再试")
        finally:
            request_lock.release()

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
