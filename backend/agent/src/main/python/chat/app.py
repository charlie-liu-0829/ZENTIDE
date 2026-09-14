import os
from urllib.parse import urlsplit, urlunsplit

import requests
import streamlit as st

from agent.react_agent import ReactAgent, sanitize_answer
from agent.snapshot_store import SnapshotStore

st.set_page_config(page_title="问问现场", page_icon=":material/forum:")
st.title("问问现场")
st.caption("基于兴趣现场内容的社区问答")
st.divider()


def _proxy_env_names() -> list[str]:
    """Return proxy variables that can affect the DashScope request."""
    return [name for name in (
        "HTTPS_PROXY", "HTTP_PROXY", "ALL_PROXY",
        "https_proxy", "http_proxy", "all_proxy",
    )
            if os.environ.get(name)]


def _redact_proxy(value: str) -> str:
    """Keep diagnostics useful without displaying proxy credentials."""
    try:
        parsed = urlsplit(value)
        if parsed.hostname:
            host = parsed.hostname
            if parsed.port:
                host = f"{host}:{parsed.port}"
            return urlunsplit((parsed.scheme, host, parsed.path, parsed.query, parsed.fragment))
    except ValueError:
        pass
    return "<已配置但格式无法识别>"


def _model_error_message(error: Exception) -> str:
    """Convert common model transport errors into actionable UI text."""
    if isinstance(error, requests.exceptions.ProxyError):
        proxy_names = _proxy_env_names()
        proxy_hint = "、".join(
            f"{name}={_redact_proxy(os.environ[name])}" for name in proxy_names
        ) or "未检测到代理环境变量"
        return (
            "无法连接阿里云 DashScope：当前请求被代理服务器中断（ProxyError）。\n\n"
            f"检测到：{proxy_hint}。请关闭失效代理或配置可用的 HTTPS 代理后重启 Streamlit；"
            "也可以先运行 `curl -I https://dashscope.aliyuncs.com` 验证网络。"
        )
    if isinstance(error, requests.exceptions.RequestException):
        return f"无法连接阿里云 DashScope（{type(error).__name__}）。请检查网络、代理和 DASHSCOPE_API_KEY。"
    return f"模型调用失败（{type(error).__name__}）：{error}"


with st.sidebar:
    st.subheader("问问现场")
    scope_label = st.selectbox("回答范围", ["现场全部帖子", "指定帖子"])
    mode = "scene" if scope_label == "现场全部帖子" else "post"
    scene_id = st.number_input("现场 ID", min_value=1, value=1, step=1)
    current_post_id = st.number_input(
        "帖子 ID（指定帖子模式）", min_value=1, value=1, step=1,
        disabled=mode != "post",
    )
    if "snapshot_store" not in st.session_state:
        st.session_state["snapshot_store"] = SnapshotStore()
    snapshot_stats = st.session_state["snapshot_store"].refresh()
    st.caption(f"快照目录：{st.session_state['snapshot_store'].snapshot_dir}")
    st.caption(f"已扫描 {snapshot_stats['scanned']} 个文件，本次更新 {snapshot_stats['changed']} 个。")
    st.caption("正式接口应由 Spring Security 注入用户身份和可见性权限。")

if "agent" not in st.session_state:
    st.session_state["agent"] = ReactAgent()

if "messages" not in st.session_state:
    st.session_state["messages"] = []

for message in st.session_state["messages"]:
    content = message["content"]
    if message["role"] == "assistant":
        content = sanitize_answer(content)
    message_view = st.chat_message(message["role"])
    if message.get("error"):
        message_view.error(content)
    else:
        message_view.markdown(content, unsafe_allow_html=False)

prompt = st.chat_input()

if prompt:
    st.chat_message("user").write(prompt)
    st.session_state["messages"].append({"role": "user", "content": prompt})

    response_messages: list[str] = []
    response_error = False
    with st.spinner("智能客服思考中..."):
        try:
            res_stream = st.session_state["agent"].execute_stream(
                prompt,
                scene_id=int(scene_id),
                mode=mode,
                current_post_id=int(current_post_id) if mode == "post" else None,
            )

            def stream_generator(generator, cache_list):
                """逐字流式输出，同时缓存完整响应"""
                for chunk in generator:
                    cache_list.append(chunk)
                    for char in chunk:
                        yield char

            st.chat_message("assistant").write_stream(
                stream_generator(res_stream, response_messages)
            )
            answer = "".join(response_messages).strip()
        except Exception as error:
            answer = _model_error_message(error)
            response_error = True
            st.chat_message("assistant").error(answer)

        st.session_state["messages"].append({
            "role": "assistant",
            "content": answer,
            "error": response_error,
        })
        st.rerun()
