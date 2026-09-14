"""SmartPosting demo/运营预览；正式用户入口位于 Vue 发帖编辑器。"""

import os
from pathlib import Path

import streamlit as st

from review_service import PostReviewService
from snapshot_reader import PublishedSnapshotReader


snapshot_root = os.getenv(
    "ZENTIDE_KNOWLEDGE_SNAPSHOT_DIR",
    str(Path(__file__).resolve().parents[6] / "data" / "knowledge-snapshots"),
)
service = PostReviewService(PublishedSnapshotReader(snapshot_root))

st.set_page_config(page_title="知潮 · 发帖智能检查", page_icon="✓", layout="wide")
st.title("发帖智能检查 · Demo")
st.caption("生产环境由 Spring Boot 注入登录用户和现场权限；此页只用于演示规则效果。")

editor, review_panel = st.columns([2, 1])
with editor:
    scene_id = st.number_input("现场", min_value=1, value=5)
    post_type = st.selectbox("帖子类型", ["DISCUSSION", "QUESTION", "EXPERIENCE", "REVIEW", "EVENT", "POLL"])
    title = st.text_input("标题", max_chars=220)
    content = st.text_area("正文", height=320, max_chars=8000)
    submitted = st.button("智能检查", type="primary")

with review_panel:
    st.subheader("检查结果")
    if submitted:
        result = service.review(
            user_id="demo-only", scene_id=int(scene_id), title=title, content=content,
            selected_post_type=post_type, permitted_visibilities={"PUBLIC"},
        )
        if result["publish_blocked"]:
            st.error("检测到敏感信息，禁止发布；请移除或脱敏后重新检查")
        elif result["advice"] == "ready":
            st.success("可以发布")
        elif result["advice"] == "manual_review":
            st.error("需要人工审核")
        else:
            st.warning("建议发布前确认")
        if result["sensitive_information_warnings"]:
            st.write("敏感信息", result["sensitive_information_warnings"])
        if result["missing_information"]:
            st.write("建议补充", result["missing_information"])
        if result["community_rule_warnings"]:
            st.write("规则提醒", result["community_rule_warnings"])
        if result["content_improvements"]:
            st.write("内容优化建议", result["content_improvements"])
        if result["optimized_content"]:
            with st.expander("预览优化正文"):
                st.text(result["optimized_content"])
        if result["similar_posts"]:
            st.write("相似帖子")
            for post in result["similar_posts"]:
                st.write(f"- {post['title']}（{post['similarity']:.0%}）")
    else:
        st.info("填写帖子后点击“智能检查”。建议只展示，不自动修改原文。")
