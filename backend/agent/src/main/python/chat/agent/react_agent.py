import json
import re
from collections.abc import Mapping

from langchain.agents import create_agent
from utils.prompt_loader import load_system_prompts
from agent.tools.agent_tools import (search_scene_content, get_thread_context,
                                     get_current_posts_context,
                                     get_scene_activities, get_interest_object,
                                     set_snapshot_context, reset_snapshot_context)
from agent.tools.middleware import (monitor_tool, log_before_model, content_to_text,
                                    set_tool_budget, reset_tool_budget)
from agent.references import linkify_post_citations


def sanitize_answer(text: str) -> str:
    """Remove accidental exposure of internal context/tool payloads."""
    if not text:
        return ""
    text = re.sub(
        r"\[服务端上下文：.*?\]\s*.*?(?=\{\s*[\"']post[\"']\s*:)",
        "",
        text,
        flags=re.DOTALL,
    )
    text = re.sub(r"\[服务端上下文：.*?\]\s*", "", text, flags=re.DOTALL)
    text = re.sub(r"当前兴趣现场\s*[（(]?\s*scene[_\\ -]?id\s*[=:：]\s*\d+\s*[）)]?", "当前兴趣现场", text, flags=re.IGNORECASE)
    text = re.sub(r"当前为(?:指定帖子|现场)模式，.*?禁止(?:在最终回答中)?回显。?\s*", "", text, flags=re.DOTALL)
    # Internal identifiers must never reach the user, even if the model ignores
    # the system instruction. Citation syntax such as [帖子 12] is handled
    # separately and is intentionally preserved for trusted link conversion.
    text = re.sub(r"(?i)\bscene[ _-]?id\s*[=:：]\s*[0-9]+", "", text)
    text = re.sub(r"(?i)\bpost[ _-]?id\s*[=:：]\s*[0-9]+", "", text)
    text = re.sub(r"(?i)\buser[ _-]?id\s*[=:：]\s*[A-Za-z0-9_-]+", "", text)
    text = re.sub(r"(?i)\b(?:scene|post|user)\s*(?:编号|id)\s*[=:：]?\s*[A-Za-z0-9_-]+", "", text)
    # Remove identifier terminology even when it is written as a request for
    # input (for example, "请提供帖子编号（post_id）"). The user should
    # navigate by title/context, never by an implementation identifier.
    text = re.sub(r"(?i)\b(?:scene|post|comment|user)[ _-]?id\b", "", text)
    text = re.sub(r"(?i)(?:现场|帖子|评论|用户)\s*(?:编号|ID|id)", "相关内容", text)
    text = re.sub(r"(?i)\b(?:编号|id)\b", "", text)
    text = re.sub(r"相关内容\s*[（(]\s*[）)]", "相关内容", text)
    text = re.sub(r"\(\s*\)|（\s*）", "", text)
    text = re.sub(r"(?<!\[)我会聚焦帖子\s*#\s*\d+\s*及其评论回答", "我会聚焦当前帖子及其评论回答", text)
    text = re.sub(r"(?<!\[)帖子\s*#\s*\d+", "当前帖子", text)
    text = re.sub(r"(?<!\[)帖子\s*\d+", "相关帖子", text)
    text = re.sub(r"第\s*\d+\s*号帖子", "相关帖子", text)
    markers = [index for index in (text.find('{"post"'), text.find("{'post'")) if index >= 0]
    marker = min(markers) if markers else -1
    if marker >= 0:
        depth = 0
        in_string = False
        escaped = False
        end = None
        quote = '"' if text.startswith('{"post"', marker) else "'"
        for index in range(marker, len(text)):
            char = text[index]
            if in_string:
                if escaped:
                    escaped = False
                elif char == "\\":
                    escaped = True
                elif char == quote:
                    in_string = False
                continue
            if char in ('"', "'"):
                in_string = True
                quote = char
            elif char == "{":
                depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0:
                    end = index + 1
                    break
        if end is not None:
            text = text[:marker] + text[end:]
    text = re.sub(r"(?:file://)?/[^\s`]*knowledge-snapshots/(?:scene-\d+/)?post-\d+\.md", "", text)
    # 快照是内部实现，不向用户暴露。统一改成面向用户的“现场资料”。
    text = re.sub(r"(?i)markdown\s+knowledge\s+snapshots?", "现场资料", text)
    text = text.replace("Markdown 知识快照", "现场资料")
    text = text.replace("知识快照", "现场资料")
    text = text.replace("快照资料", "现场资料")
    text = text.replace("当前快照", "当前现场")
    text = text.replace("快照", "现场资料")
    text = re.sub(r"(?i)\bsnapshots?\b", "现场资料", text)
    # Older model prompts produced a generic capability card containing
    # internal terminology. Keep a concise, useful answer if that card is
    # returned from a running model or restored conversation.
    text = re.sub(
        r"(?im)^\s*[•·-]\s*(?:查找现场内容|总结指定帖子|查询现场活动|了解兴趣对象)\s*(?:（[^\n]*）)?\s*[:：]?.*$",
        "",
        text,
    )
    text = re.sub(r"(?im)^\s*我可以帮助你\s*[:：]?\s*$", "我可以帮你查找和整理当前现场的内容。", text)
    text = re.sub(r"(?im)^\s*你可以告诉我你想了解的具体内容.*$", "", text)
    return text.strip()


def _post_ids_from_tool_value(value) -> set[int]:
    """Extract only positive post_id fields from trusted tool results."""
    ids: set[int] = set()
    if isinstance(value, str):
        try:
            parsed = json.loads(value)
        except (TypeError, ValueError):
            return ids
        return _post_ids_from_tool_value(parsed)
    if isinstance(value, Mapping):
        for key, nested in value.items():
            if key == "post_id":
                if isinstance(nested, bool):
                    continue
                if isinstance(nested, int):
                    post_id = nested
                elif isinstance(nested, str) and re.fullmatch(r"[1-9]\d*", nested.strip()):
                    post_id = int(nested)
                else:
                    continue
                if post_id > 0:
                    ids.add(post_id)
            else:
                ids.update(_post_ids_from_tool_value(nested))
    elif isinstance(value, (list, tuple)):
        for nested in value:
            ids.update(_post_ids_from_tool_value(nested))
    return ids


def _post_titles_from_tool_value(value) -> dict[int, str]:
    """Extract title alongside post_id from trusted tool results."""
    titles: dict[int, str] = {}
    if isinstance(value, str):
        try:
            return _post_titles_from_tool_value(json.loads(value))
        except (TypeError, ValueError):
            return titles
    if isinstance(value, Mapping):
        raw_id = value.get("post_id")
        title = value.get("title")
        if isinstance(raw_id, int) and raw_id > 0 and isinstance(title, str) and title.strip():
            titles[raw_id] = title.strip()
        elif isinstance(raw_id, str) and re.fullmatch(r"[1-9]\d*", raw_id.strip()) \
                and isinstance(title, str) and title.strip():
            titles[int(raw_id)] = title.strip()
        for key, nested in value.items():
            if key not in {"post_id", "title"}:
                titles.update(_post_titles_from_tool_value(nested))
    elif isinstance(value, (list, tuple)):
        for nested in value:
            titles.update(_post_titles_from_tool_value(nested))
    return titles


def format_answer(
    text: str,
    *,
    community_url: str | None = None,
    allowed_post_ids: set[int] | None = None,
    post_titles: dict[int, str] | None = None,
) -> str:
    """Sanitize model text and link only citations backed by tool results."""
    formatted = linkify_post_citations(
        sanitize_answer(text),
        community_url,
        allowed_post_ids=allowed_post_ids,
        post_titles=post_titles,
    )
    # Any citation that was not backed by a trusted tool result must not expose
    # its numeric internal identifier in the user-visible answer.
    return re.sub(r"\[帖子\s*#?\d+(?:\s*[·.]\s*\d+\s*楼)?\]", "相关帖子", formatted)


class ReactAgent:
    # Compatibility alias for callers that previously used
    # ``ReactAgent.sanitize_answer(...)``.
    sanitize_answer = staticmethod(sanitize_answer)

    def __init__(self):
        # Keep health checks available even before a model credential exists.
        from model.factory import chat_model

        self.agent = create_agent(
            model=chat_model,
            system_prompt=load_system_prompts(),
            tools=[search_scene_content, get_thread_context, get_current_posts_context, get_scene_activities,
                   get_interest_object],
            middleware=[monitor_tool, log_before_model],
        )

    def execute_stream(self, query: str, *, scene_id: int | None = None,
                       mode: str = "scene",
                       user_id: str | None = None,
                       conversation_id: str | None = None,
                       current_post_id: int | None = None,
                       selected_post_ids: list[int] | None = None,
                       permitted_visibilities: set[str] | None = None,
                       history: list[dict[str, str]] | None = None):
        if scene_id is None:
            raise ValueError("scene_id 是现场问答必填参数")
        if mode not in {"scene", "post"}:
            raise ValueError("mode 必须是 scene 或 post")
        if mode == "post" and current_post_id is None:
            raise ValueError("指定帖子模式必须提供 current_post_id")
        token = set_snapshot_context(scene_id=scene_id, scope=mode,
                                     current_post_id=current_post_id,
                                     selected_post_ids=selected_post_ids,
                                     user_id=user_id,
                                     permitted_visibilities=permitted_visibilities)
        messages = [{"role": "system", "content": self._request_context_text(mode)}]
        # Deterministic prefetch prevents the model from answering with the
        # empty-data fallback before it has inspected the current scene.
        if mode == "scene":
            try:
                context = search_scene_content.invoke({"query": query, "content_types": ["post", "comment"]})
                if context:
                    messages.append({"role": "system", "content":
                                     "服务端已检索到以下当前现场资料。只能据此回答，不要向用户展示原始 JSON：\n"
                                     + json.dumps(context, ensure_ascii=False)})
            except Exception:
                pass
        for item in (history or [])[-6:]:
            role = item.get("role") if isinstance(item, dict) else None
            content = item.get("content") if isinstance(item, dict) else None
            if role in {"user", "assistant"} and isinstance(content, str) and content.strip():
                messages.append({"role": role, "content": content.strip()[:3000]})
        messages.append({"role": "user", "content": query})
        input_dict = {"messages": messages}

        budget_token = set_tool_budget(2)
        try:
            runtime_context = {"scope": mode}
            allowed_post_ids: set[int] = set()
            post_titles: dict[int, str] = {}
            raw_answer = ""
            emitted_answer = ""
            for message, _metadata in self.agent.stream(
                    input_dict,
                    stream_mode="messages",
                    context=runtime_context,
                    config={"recursion_limit": 6},
            ):
                message_type = getattr(message, "type", "")
                if message_type == "tool":
                    allowed_post_ids.update(_post_ids_from_tool_value(message.content))
                    post_titles.update(_post_titles_from_tool_value(message.content))
                    continue
                if message_type not in {"AIMessage", "AIMessageChunk"}:
                    continue
                if getattr(message, "tool_calls", None) or getattr(message, "tool_call_chunks", None):
                    continue
                content = content_to_text(getattr(message, "content", ""))
                if not content:
                    continue
                raw_answer += content
                cleaned = sanitize_answer(raw_answer)
                if cleaned.startswith(emitted_answer):
                    delta = cleaned[len(emitted_answer):]
                else:
                    # A sanitizer can remove a token that arrived in the
                    # previous chunk. Emit only the unseen suffix when the
                    # common prefix is still stable.
                    common = 0
                    while common < len(cleaned) and common < len(emitted_answer) \
                            and cleaned[common] == emitted_answer[common]:
                        common += 1
                    delta = cleaned[common:]
                emitted_answer = cleaned
                if delta:
                    yield {"type": "delta", "text": delta}
            final_answer = format_answer(
                raw_answer,
                allowed_post_ids=allowed_post_ids,
                post_titles=post_titles,
            ).strip()
            if final_answer:
                yield {"type": "final", "text": final_answer}
        finally:
            reset_tool_budget(budget_token)
            reset_snapshot_context(token)

    @staticmethod
    def _request_context_text(mode: str) -> str:
        if mode == "post":
            return ("当前为指定帖子模式，只能回答当前帖子及其评论。"
                    "当前帖子和现场范围由服务端工具自动限定，不要猜测、询问或输出内部实现信息。")
        return ("当前为现场模式，只能检索当前兴趣现场的可见帖子和评论。"
                "现场范围由服务端工具自动限定，不要猜测、询问或输出内部实现信息。")
