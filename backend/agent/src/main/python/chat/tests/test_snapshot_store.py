from pathlib import Path

from agent.snapshot_store import SnapshotStore


def _snapshot(scene_id: int, post_id: int) -> str:
    return f"""---
post_id: {post_id}
scene_id: {scene_id}
visibility: PUBLIC
status: PUBLISHED
---

# 测试帖子

来自现场 {scene_id} 的内容。
"""


def test_scans_scene_directories_and_filters_by_scene(tmp_path: Path):
    first = tmp_path / "scene-5" / "post-12.md"
    second = tmp_path / "scene-6" / "post-13.md"
    first.parent.mkdir()
    second.parent.mkdir()
    first.write_text(_snapshot(5, 12), encoding="utf-8")
    second.write_text(_snapshot(6, 13), encoding="utf-8")

    store = SnapshotStore(str(tmp_path))

    assert store.refresh()["scanned"] == 2
    assert [post.post_id for post in store.list_posts(5)] == [12]
    assert [post.post_id for post in store.list_posts(6)] == [13]


def test_rejects_snapshot_placed_in_wrong_scene_directory(tmp_path: Path):
    misplaced = tmp_path / "scene-6" / "post-12.md"
    misplaced.parent.mkdir()
    misplaced.write_text(_snapshot(5, 12), encoding="utf-8")

    store = SnapshotStore(str(tmp_path))

    assert store.refresh()["changed"] == 0
    assert store.list_posts(5) == []
