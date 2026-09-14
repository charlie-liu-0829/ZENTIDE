"""Small SQLite persistence layer for user interest keywords and feedback."""

from __future__ import annotations

from contextlib import contextmanager
from datetime import datetime, timezone
import json
import sqlite3
from pathlib import Path
from typing import Any, Iterator


class InterestStore:
    def __init__(self, path: str | Path = "data/interests.sqlite3"):
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.initialize()

    @contextmanager
    def _connection(self) -> Iterator[sqlite3.Connection]:
        connection = sqlite3.connect(self.path)
        connection.row_factory = sqlite3.Row
        try:
            yield connection
            connection.commit()
        finally:
            connection.close()

    def initialize(self) -> None:
        with self._connection() as connection:
            connection.execute("""
                CREATE TABLE IF NOT EXISTS user_interest_keyword (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id TEXT NOT NULL,
                    keyword TEXT NOT NULL,
                    enabled INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    last_processed_at TEXT,
                    UNIQUE(user_id, keyword COLLATE NOCASE)
                )
            """)
            connection.execute("""
                CREATE TABLE IF NOT EXISTS interest_feedback (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    action TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    UNIQUE(user_id, item_id, action)
                )
            """)
            connection.execute("""
                CREATE TABLE IF NOT EXISTS saved_insight (
                    user_id TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    payload_json TEXT NOT NULL,
                    saved_at TEXT NOT NULL,
                    PRIMARY KEY(user_id, item_id)
                )
            """)

    def list_keywords(self, user_id: str, enabled_only: bool = False) -> list[dict]:
        query = "SELECT * FROM user_interest_keyword WHERE user_id = ?"
        params: list[object] = [user_id]
        if enabled_only:
            query += " AND enabled = 1"
        query += " ORDER BY updated_at DESC, id DESC"
        with self._connection() as connection:
            return [dict(row) for row in connection.execute(query, params)]

    def add_keyword(self, user_id: str, keyword: str) -> dict:
        value = " ".join(str(keyword or "").split()).strip()
        if not value or len(value) > 80:
            raise ValueError("关键词不能为空且不能超过 80 个字符")
        now = datetime.now(timezone.utc).isoformat()
        with self._connection() as connection:
            existing = connection.execute(
                "SELECT id FROM user_interest_keyword WHERE user_id=? AND keyword=? COLLATE NOCASE",
                (user_id, value),
            ).fetchone()
            if existing:
                connection.execute(
                    "UPDATE user_interest_keyword SET enabled=1, updated_at=? WHERE id=?",
                    (now, existing["id"]),
                )
            else:
                count = connection.execute(
                    "SELECT COUNT(*) FROM user_interest_keyword WHERE user_id=?", (user_id,)
                ).fetchone()[0]
                if count >= 30:
                    raise ValueError("最多只能关注 30 个关键词")
                connection.execute("""
                    INSERT INTO user_interest_keyword(user_id, keyword, enabled, created_at, updated_at)
                    VALUES (?, ?, 1, ?, ?)
                """, (user_id, value, now, now))
            row = connection.execute(
                "SELECT * FROM user_interest_keyword WHERE user_id=? AND keyword=? COLLATE NOCASE",
                (user_id, value),
            ).fetchone()
            return dict(row)

    def set_enabled(self, user_id: str, keyword_id: int, enabled: bool) -> bool:
        with self._connection() as connection:
            cursor = connection.execute("UPDATE user_interest_keyword SET enabled=?, updated_at=? WHERE id=? AND user_id=?", (int(enabled), datetime.now(timezone.utc).isoformat(), keyword_id, user_id))
            return cursor.rowcount == 1

    def delete_keyword(self, user_id: str, keyword_id: int) -> bool:
        with self._connection() as connection:
            cursor = connection.execute("DELETE FROM user_interest_keyword WHERE id=? AND user_id=?", (keyword_id, user_id))
            return cursor.rowcount == 1

    def record_feedback(self, user_id: str, item_id: str, action: str) -> bool:
        if action not in {"viewed", "saved", "not_interested", "ignored"}:
            raise ValueError("不支持的反馈类型")
        with self._connection() as connection:
            connection.execute("INSERT OR IGNORE INTO interest_feedback(user_id,item_id,action,created_at) VALUES (?,?,?,?)", (user_id, item_id, action, datetime.now(timezone.utc).isoformat()))
            return True

    def feedback_items(self, user_id: str, action: str = "viewed") -> set[str]:
        with self._connection() as connection:
            return {row[0] for row in connection.execute("SELECT item_id FROM interest_feedback WHERE user_id=? AND action=?", (user_id, action))}

    def remove_feedback(self, user_id: str, item_id: str, action: str) -> bool:
        with self._connection() as connection:
            cursor = connection.execute("DELETE FROM interest_feedback WHERE user_id=? AND item_id=? AND action=?", (user_id, item_id, action))
            return cursor.rowcount > 0

    def save_insight(self, user_id: str, item_id: str, item: dict[str, Any]) -> None:
        if str(item.get("item_id") or "") != item_id:
            raise ValueError("收藏内容与情报 ID 不一致")
        now = datetime.now(timezone.utc).isoformat()
        payload = json.dumps(item, ensure_ascii=False, separators=(",", ":"))
        if len(payload.encode("utf-8")) > 100_000:
            raise ValueError("收藏内容过大")
        with self._connection() as connection:
            connection.execute("""
                INSERT INTO saved_insight(user_id,item_id,payload_json,saved_at)
                VALUES (?,?,?,?)
                ON CONFLICT(user_id,item_id) DO UPDATE SET payload_json=excluded.payload_json,saved_at=excluded.saved_at
            """, (user_id, item_id, payload, now))

    def list_saved_insights(self, user_id: str) -> list[dict[str, Any]]:
        with self._connection() as connection:
            rows = connection.execute(
                "SELECT payload_json,saved_at FROM saved_insight WHERE user_id=? ORDER BY saved_at DESC",
                (user_id,),
            )
            items = []
            for row in rows:
                try:
                    item = json.loads(row["payload_json"])
                    item["saved_at"] = row["saved_at"]
                    items.append(item)
                except (json.JSONDecodeError, TypeError):
                    continue
            return items

    def remove_saved_insight(self, user_id: str, item_id: str) -> bool:
        with self._connection() as connection:
            connection.execute(
                "DELETE FROM interest_feedback WHERE user_id=? AND item_id=? AND action='saved'",
                (user_id, item_id),
            )
            cursor = connection.execute(
                "DELETE FROM saved_insight WHERE user_id=? AND item_id=?", (user_id, item_id)
            )
            return cursor.rowcount > 0
