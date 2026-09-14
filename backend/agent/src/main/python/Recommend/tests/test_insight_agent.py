from pathlib import Path

from insight_agent import PersonalizedInsightAgent, SnapshotReader
from interest_store import InterestStore


ROOT = Path(__file__).resolve().parents[7] / "data" / "knowledge-snapshots"


def test_snapshot_reader_keeps_post_and_comment_evidence():
    result = PersonalizedInsightAgent(SnapshotReader(ROOT)).generate(["想去"], scene_ids={5})
    assert result.items
    item = result.items[0]
    assert item.evidence[0].post_id == 18
    assert item.evidence[0].url == "/community/posts/18"
    # The post evidence is grouped with the comment evidence under one insight.
    assert any(e.comment_id == "15" and e.floor_no == 1 for e in item.evidence)


def test_keyword_matching_is_deterministic_and_supports_seen_content():
    agent = PersonalizedInsightAgent(SnapshotReader(ROOT))
    result = agent.generate(["LangChain", "Spring Boot"], scene_ids={5}, seen_post_ids={18})
    assert result.items == ()
    assert result.warning


def test_keyword_store_is_user_scoped(tmp_path):
    store = InterestStore(tmp_path / "interests.sqlite3")
    row = store.add_keyword("u1", "LangChain")
    store.add_keyword("u2", "LangChain")
    assert [item["keyword"] for item in store.list_keywords("u1")] == ["LangChain"]
    assert store.delete_keyword("u1", row["id"])
    assert store.list_keywords("u1") == []
    assert len(store.list_keywords("u2")) == 1


def test_private_snapshot_requires_explicit_post_permission(tmp_path):
    scene = tmp_path / "scene-9"
    scene.mkdir()
    (scene / "post-99.md").write_text("""---
post_id: 99
scene_id: 9
scene_name: 私密现场
visibility: PRIVATE
status: PUBLISHED
created_at: 2026-09-08T10:00:00
---

# LangChain 内部实践

这里记录 LangChain 私密项目经验。

## 评论区

暂无已发布评论
""", encoding="utf-8")
    agent = PersonalizedInsightAgent(SnapshotReader(tmp_path))
    assert agent.generate(["LangChain"], scene_ids={9}).items == ()
    assert agent.generate(["LangChain"], scene_ids={9}, permitted_post_ids={99}).items


def test_interest_aliases_expand_deterministically(tmp_path):
    scene = tmp_path / "scene-2"
    scene.mkdir()
    (scene / "post-8.md").write_text("""---
post_id: 8
scene_id: 2
scene_name: 技术
visibility: PUBLIC
status: PUBLISHED
---

# LLM Agent 实践

分享大语言模型应用经验。

## 评论区

暂无已发布评论
""", encoding="utf-8")
    result = PersonalizedInsightAgent(SnapshotReader(tmp_path)).generate(["人工智能"])
    assert result.items[0].evidence[0].matched_keywords == ("人工智能",)


def test_performance_alias_matches_concert_content(tmp_path):
    scene = tmp_path / "scene-5"
    scene.mkdir()
    (scene / "post-18.md").write_text("""---
post_id: 18
scene_id: 5
scene_name: 音乐现场
visibility: PUBLIC
status: PUBLISHED
---

# 演唱会

想去看这场现场音乐演出。
""", encoding="utf-8")
    result = PersonalizedInsightAgent(SnapshotReader(tmp_path)).generate(["演出"])
    assert result.items and result.items[0].evidence[0].post_id == 18


def test_expanded_terms_report_original_target_keyword(tmp_path):
    class Expander:
        def expand_keyword_groups(self, keywords):
            return {"现场活动": ["现场活动", "演唱会"]}

        def __call__(self, candidates):
            return []

    scene = tmp_path / "scene-6"
    scene.mkdir()
    (scene / "post-20.md").write_text("""---
post_id: 20
scene_id: 6
scene_name: 城市生活
visibility: PUBLIC
status: PUBLISHED
---

# 周末演唱会

本周六开场。
""", encoding="utf-8")
    result = PersonalizedInsightAgent(SnapshotReader(tmp_path), Expander()).generate(["现场活动"])
    assert result.items[0].to_dict()["target_keywords"] == ["现场活动"]
    assert result.items[0].evidence[0].matched_keywords == ("现场活动",)
