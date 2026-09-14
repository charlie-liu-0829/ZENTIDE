from contextvars import ContextVar
from typing import Callable
from langchain.agents import AgentState
from langchain.agents.middleware import wrap_tool_call, before_model
from langchain.tools.tool_node import ToolCallRequest
from langchain_core.messages import ToolMessage
from langgraph.runtime import Runtime
from langgraph.types import Command
from utils.logger_handler import logger


_tool_budget: ContextVar[dict[str, int] | None] = ContextVar("agent_tool_budget", default=None)


def set_tool_budget(limit: int = 2):
    return _tool_budget.set({"limit": max(1, limit), "used": 0})


def reset_tool_budget(token) -> None:
    # LangGraph may resume a streaming generator in a copied Context.  A
    # token created in the caller's Context cannot be reset there.  Clearing
    # the request-local value in the current Context is safe and prevents the
    # cleanup path from turning a successful model response into an error.
    try:
        _tool_budget.reset(token)
    except ValueError:
        _tool_budget.set(None)


def content_to_text(content) -> str:
    """Normalize LangChain string or content-block messages for logging."""
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts = []
        for block in content:
            if isinstance(block, str):
                parts.append(block)
            elif isinstance(block, dict):
                value = block.get("text") or block.get("content")
                if isinstance(value, str):
                    parts.append(value)
        return "".join(parts)
    return str(content) if content is not None else ""


@wrap_tool_call
def monitor_tool(
        # 请求的数据封装
        request: ToolCallRequest,
        # 执行的函数本身
        handler: Callable[[ToolCallRequest], ToolMessage | Command],
) -> ToolMessage | Command:             # 工具执行的监控
    budget = _tool_budget.get()
    if budget is not None and budget["used"] >= budget["limit"]:
        logger.warning("[tool monitor]达到单次请求工具调用上限：%s", budget["limit"])
        return ToolMessage(
            content="工具调用预算已用尽。请仅根据已经返回的资料直接回答用户，不要继续调用工具。",
            tool_call_id=request.tool_call.get("id", "budget-exhausted"),
        )
    if budget is not None:
        budget["used"] += 1
    logger.info(f"[tool monitor]执行工具：{request.tool_call['name']}")
    logger.info(f"[tool monitor]传入参数：{request.tool_call['args']}")

    try:
        result = handler(request)
        logger.info(f"[tool monitor]工具{request.tool_call['name']}调用成功")

        return result
    except Exception as e:
        logger.error(f"工具{request.tool_call['name']}调用失败，原因：{str(e)}")
        raise e


@before_model
def log_before_model(
        state: AgentState,          # 整个Agent智能体中的状态记录
        runtime: Runtime,           # 记录了整个执行过程中的上下文信息
):         # 在模型执行前输出日志
    logger.info(f"[log_before_model]即将调用模型，带有{len(state['messages'])}条消息。")

    content = content_to_text(state["messages"][-1].content).strip()
    logger.debug(f"[log_before_model]{type(state['messages'][-1]).__name__} | {content}")

    return None
