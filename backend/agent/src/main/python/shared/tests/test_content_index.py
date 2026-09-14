from shared.retrieval import SnapshotChunkIndex


def test_index_chunks_and_permission_filter(tmp_path):
    scene = tmp_path / "scene-5"; scene.mkdir()
    (scene / "post-18.md").write_text("""---
post_id: 18
scene_id: 5
visibility: PUBLIC
status: PUBLISHED
---

# 演唱会

现场音乐活动安排。
""", encoding="utf-8")
    results = SnapshotChunkIndex(tmp_path).search("现场音乐", scene_id=5)
    assert results and results[0].content_id == 18 and results[0].source_url.endswith("/18")
