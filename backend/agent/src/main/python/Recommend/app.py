"""Streamlit demo for the personalized insight agent."""

import os
from pathlib import Path

import streamlit as st

from insight_agent import PersonalizedInsightAgent, SnapshotReader


snapshot_root = os.getenv(
    "ZENTIDE_KNOWLEDGE_SNAPSHOT_DIR",
    str(Path(__file__).resolve().parents[6] / "data" / "knowledge-snapshots"),
)
agent = PersonalizedInsightAgent(SnapshotReader(snapshot_root))

st.set_page_config(page_title="知潮 · 我的兴趣情报", page_icon="◌", layout="wide")
st.title("我的兴趣情报")
st.caption("从你有权限查看的已发布内容中筛选动态，并为每条结论保留原帖和评论证据。")

with st.sidebar:
    st.subheader("兴趣设置")
    keywords_text = st.text_area("关键词", "演唱会, 张杰", help="使用逗号或换行分隔")
    scene_id = st.number_input("现场 ID（0 表示全部可见现场）", min_value=0, value=5)
    max_items = st.slider("最多返回", 1, 20, 8)
    generate = st.button("生成我的情报摘要", type="primary", use_container_width=True)

if generate:
    keywords = [
        item.strip()
        for item in keywords_text.replace("，", ",").replace("\n", ",").split(",")
        if item.strip()
    ]
    result = agent.generate(
        keywords, scene_ids={int(scene_id)} if scene_id else None, max_items=max_items
    )
    if result.warning:
        st.info(result.warning)
    st.caption(f"找到 {result.matched_count} 条证据 · 快照版本 {result.snapshot_version}")
    for item in result.items:
        with st.container(border=True):
            st.subheader(item.headline)
            st.write(item.summary)
            st.caption(f"置信度 {item.confidence:.0%}")
            with st.expander(f"查看 {len(item.evidence)} 条原始证据"):
                for evidence in item.evidence:
                    source = f"第 {evidence.floor_no} 楼评论" if evidence.comment_id else "主帖"
                    st.markdown(f"**{source} · 帖子 {evidence.post_id}**")
                    st.write(evidence.excerpt)
                    st.caption("命中：" + "、".join(evidence.matched_keywords))
                    st.code(evidence.url, language=None)
else:
    st.info("设置兴趣关键词后生成摘要。Agent 不会让模型扫描全部内容，也不会生成没有来源的引用。")
