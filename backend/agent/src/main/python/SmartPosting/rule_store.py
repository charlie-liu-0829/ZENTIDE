"""Reloadable community-rule repository used by the LangGraph rule node."""

from __future__ import annotations

from copy import deepcopy
from pathlib import Path
from threading import RLock
from typing import Any

try:
    import yaml
except ImportError:
    yaml = None


class CommunityRuleStore:
    def __init__(self, default_rules: list[str], default_requirements: dict[str, list[str]],
                 path: str | None = None):
        self.path = Path(path) if path else Path(__file__).resolve().parent / "config" / "community_rules.yml"
        self.default_rules = list(default_rules)
        self.default_requirements = deepcopy(default_requirements)
        self._mtime_ns = -1
        self._data: dict[str, Any] = {}
        self._lock = RLock()

    def _refresh(self) -> None:
        if yaml is None or not self.path.is_file():
            return
        mtime_ns = self.path.stat().st_mtime_ns
        if mtime_ns == self._mtime_ns:
            return
        with self._lock:
            if mtime_ns == self._mtime_ns:
                return
            loaded = yaml.safe_load(self.path.read_text(encoding="utf-8")) or {}
            self._data = loaded if isinstance(loaded, dict) else {}
            self._mtime_ns = mtime_ns

    def rules_for(self, scene_id: int) -> list[str]:
        self._refresh()
        rules = self._data.get("global_rules", self.default_rules)
        scene_rules = self._data.get("scene_rules", {}).get(str(scene_id), [])
        return list(dict.fromkeys(str(item).strip() for item in [*rules, *scene_rules] if str(item).strip()))

    def type_requirements(self) -> dict[str, list[str]]:
        self._refresh()
        configured = self._data.get("type_requirements")
        if not isinstance(configured, dict):
            return deepcopy(self.default_requirements)
        result = deepcopy(self.default_requirements)
        for type_code, fields in configured.items():
            if isinstance(fields, list):
                result[str(type_code).upper()] = [str(field).strip() for field in fields if str(field).strip()]
        return result

