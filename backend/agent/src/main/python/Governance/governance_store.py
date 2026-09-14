"""Legacy SQLite store kept only for offline migration tooling.

The running Governance API deliberately does not import or instantiate this
class. Rules, review results, and moderation feedback are persisted by the
Spring Boot control plane so there is a single authoritative data source.
"""
from __future__ import annotations

from contextlib import contextmanager
from datetime import datetime, timezone
import json
import sqlite3
from pathlib import Path
from typing import Any, Iterator


class GovernanceStore:
    def __init__(self, path: str | Path = "data/governance.sqlite3"):
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.initialize()

    @contextmanager
    def connection(self) -> Iterator[sqlite3.Connection]:
        conn = sqlite3.connect(self.path)
        conn.row_factory = sqlite3.Row
        try:
            yield conn
            conn.commit()
        finally:
            conn.close()

    def initialize(self) -> None:
        with self.connection() as conn:
            conn.execute("""CREATE TABLE IF NOT EXISTS governance_result (
                result_id TEXT PRIMARY KEY, content_id INTEGER NOT NULL, scene_id INTEGER NOT NULL,
                payload_json TEXT NOT NULL, created_at TEXT NOT NULL)""")
            conn.execute("""CREATE TABLE IF NOT EXISTS moderation_feedback (
                id INTEGER PRIMARY KEY AUTOINCREMENT, content_id INTEGER NOT NULL,
                agent_result_id TEXT NOT NULL, final_action TEXT NOT NULL,
                accepted_agent_advice INTEGER NOT NULL, corrected_violation_types TEXT NOT NULL,
                reviewer_reason TEXT NOT NULL, reviewer_id TEXT NOT NULL, created_at TEXT NOT NULL)""")
            conn.execute("""CREATE TABLE IF NOT EXISTS governance_rule (
                rule_id INTEGER PRIMARY KEY AUTOINCREMENT, scene_id INTEGER NOT NULL,
                violation_type TEXT NOT NULL, keywords_json TEXT NOT NULL,
                severity TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 1,
                updated_at TEXT NOT NULL)""")

    def list_rules(self, scene_id: int) -> list[dict[str, Any]]:
        with self.connection() as conn:
            rows = conn.execute("SELECT * FROM governance_rule WHERE scene_id=? AND enabled=1 ORDER BY rule_id", (scene_id,)).fetchall()
        return [{"rule_id": row["rule_id"], "violation_type": row["violation_type"],
                 "keywords": json.loads(row["keywords_json"]), "severity": row["severity"]} for row in rows]

    def save_rule(self, scene_id: int, data: dict[str, Any]) -> dict[str, Any]:
        with self.connection() as conn:
            cur = conn.execute("INSERT INTO governance_rule(scene_id,violation_type,keywords_json,severity,enabled,updated_at) VALUES (?,?,?,?,1,?)",
                               (scene_id, data["violation_type"], json.dumps(data["keywords"], ensure_ascii=False), data["severity"], datetime.now(timezone.utc).isoformat()))
            return {"rule_id": int(cur.lastrowid), "scene_id": scene_id, **data, "enabled": True}

    def save_result(self, result_id: str, content_id: int, scene_id: int, payload: dict[str, Any]) -> None:
        with self.connection() as conn:
            conn.execute("INSERT OR REPLACE INTO governance_result VALUES (?,?,?,?,?)", (
                result_id, content_id, scene_id, json.dumps(payload, ensure_ascii=False),
                datetime.now(timezone.utc).isoformat()))

    def save_feedback(self, data: dict[str, Any], reviewer_id: str) -> int:
        with self.connection() as conn:
            cursor = conn.execute("""INSERT INTO moderation_feedback
                (content_id,agent_result_id,final_action,accepted_agent_advice,corrected_violation_types,reviewer_reason,reviewer_id,created_at)
                VALUES (?,?,?,?,?,?,?,?)""", (data["content_id"], data["agent_result_id"], data["final_action"],
                int(data["accepted_agent_advice"]), json.dumps(data["corrected_violation_types"], ensure_ascii=False),
                data["reviewer_reason"], reviewer_id, datetime.now(timezone.utc).isoformat()))
            return int(cursor.lastrowid)
