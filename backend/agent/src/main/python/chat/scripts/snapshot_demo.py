"""Run a dependency-light smoke demo against the configured snapshot folder.

Usage: python scripts/snapshot_demo.py --scene-id 5 --query 张杰
"""

from __future__ import annotations

import argparse

from agent.snapshot_store import SnapshotStore


def main() -> None:
    parser = argparse.ArgumentParser(description="兴趣现场 Markdown 快照演示")
    parser.add_argument("--scene-id", type=int, required=True)
    parser.add_argument("--query", required=True)
    parser.add_argument("--snapshot-dir", help="可选：覆盖 agent.yml 中的快照目录，用于本地演示")
    args = parser.parse_args()

    store = SnapshotStore(args.snapshot_dir)
    stats = store.refresh()
    results = store.search(args.scene_id, args.query, ["post", "comment"], limit=10)
    print(f"快照目录: {store.snapshot_dir}")
    print(f"扫描: {stats['scanned']}，变更: {stats['changed']}，删除: {stats['removed']}")
    if not results:
        print("当前快照没有找到相关内容")
        return
    for item in results:
        location = f"帖子 {item['post_id']}"
        if item.get("floor_no") is not None:
            location += f" · {item['floor_no']}楼"
        print(f"[{location}] {item['excerpt']}")


if __name__ == "__main__":
    main()
