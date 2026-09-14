"""Evidence-first community governance agent.

The agent recommends a moderation action. It never deletes content, bans users,
or performs any irreversible operation.
"""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
import re
from typing import Any, TypedDict
from uuid import NAMESPACE_URL, uuid4, uuid5


class GovernanceState(TypedDict, total=False):
    title: str
    content: str
    rules: list[dict[str, Any]]
    cases: list[dict[str, Any]]
    snapshot_evidence: list[str]
    matches: list[dict[str, Any]]
    result: dict[str, Any]


@dataclass(frozen=True)
class GovernanceResult:
    result_id: str
    risk_level: str
    violation_types: list[str]
    matched_rule_ids: list[int]
    matched_case_ids: list[int]
    evidence_fragments: list[str]
    confidence: float
    suggested_action: str

    def to_dict(self) -> dict[str, Any]:
        return {"result_id": self.result_id, "risk_level": self.risk_level,
                "violation_types": self.violation_types, "matched_rule_ids": self.matched_rule_ids,
                "matched_case_ids": self.matched_case_ids, "evidence_fragments": self.evidence_fragments,
                "confidence": self.confidence, "suggested_action": self.suggested_action}


SECRET_PATTERNS = {
    "credential_or_secret": re.compile(r"(?i)(sk-[a-z0-9]{20,}|api[_ -]?key\s*[:=]\s*\S+|password\s*[:=]\s*\S+|-----begin [^-]+ key-----)"),
    "phone_number": re.compile(r"(?<!\d)1[3-9]\d{9}(?!\d)"),
    "email": re.compile(r"\b[\w.+-]+@[\w-]+\.[\w.-]+\b"),
    # 基础社区文明规则：命中后进入人工审核，最终是否隐藏由管理员确认。
    "abusive_language": re.compile(r"(?i)(她妈的|他妈的|傻子|脑残|操你|妈的|去死)"),
}


class GovernanceAgent:
    def __init__(self, store=None):
        self.store = store

    def review(self, *, content_id: int, scene_id: int, title: str, content: str,
               rules: list[dict[str, Any]] | None = None, cases: list[dict[str, Any]] | None = None,
               snapshot_evidence: list[str] | None = None) -> dict[str, Any]:
        state: GovernanceState = {"title": title.strip(), "content": content.strip(),
                                  "rules": rules or [], "cases": cases or [],
                                  "snapshot_evidence": snapshot_evidence or []}
        for node in (self._hard_rules, self._community_rules, self._decide):
            state.update(node(state))
        # 相同帖子、相同规则和相同内容重复扫描时复用结果 ID，
        # 让控制面执行幂等 upsert，不会因为每次扫描生成随机 ID 而产生重复结果。
        rule_fingerprint = "|".join(
            f"{item.get('rule_id')}:{item.get('rule_description', '')}:{item.get('severity')}"
            for item in sorted(state.get("rules", []), key=lambda item: str(item.get("rule_id", "")))
        )
        stable_key = f"governance|{scene_id}|{content_id}|{title.strip()}|{content.strip()}|{rule_fingerprint}"
        result = GovernanceResult(f"gov_{uuid5(NAMESPACE_URL, stable_key).hex}", **state["result"])
        payload = result.to_dict()
        if self.store:
            self.store.save_result(result.result_id, content_id, scene_id, payload)
        return payload

    def _hard_rules(self, state: GovernanceState) -> dict[str, Any]:
        text = f"{state['title']}\n{state['content']}"
        matches = []
        for kind, pattern in SECRET_PATTERNS.items():
            for hit in pattern.finditer(text):
                fragment = hit.group(0)[:160]
                matches.append({"type": kind, "fragment": fragment,
                                "severity": "medium" if kind == "abusive_language" else "high"})
        return {"matches": matches}

    def _community_rules(self, state: GovernanceState) -> dict[str, Any]:
        matches = list(state.get("matches", []))
        text = f"{state['title']}\n{state['content']}".casefold()
        for rule in state.get("rules", []):
            rule_id = rule.get("rule_id")
            description = str(rule.get("rule_description") or "").strip()
            terms = rule.get("keywords") or rule.get("terms") or []
            if not isinstance(rule_id, int) or not isinstance(terms, list):
                continue
            hits = [str(term) for term in terms if str(term).strip() and str(term).casefold() in text]
            if hits:
                matches.append({"type": str(rule.get("violation_type") or "community_rule"),
                                "fragment": f"命中规则 {rule_id}：" + "、".join(hits),
                                "severity": str(rule.get("severity") or "medium"), "rule_id": rule_id})
            elif description:
                meaningful = [part.casefold() for part in re.findall(r"[\u4e00-\u9fff]{2,}|[a-zA-Z]{3,}", description)]
                # 将常见的自然语言概念展开为帖子中的同义表达，作为规则推理的语义预筛。
                concept_aliases = {
                    "离开平台": ("加微信", "加qq", "私聊", "外部链接", "站外"),
                    "进行交易": ("购买", "付款", "转账", "卖", "买"),
                    "人身攻击": ("傻子", "脑残", "废物", "辱骂"),
                    "泄露隐私": ("手机号", "身份证", "住址", "邮箱"),
                }
                type_aliases = {
                    "广告刷屏": ("广告", "推广", "促销", "优惠", "下单", "购买", "加微信", "链接"),
                    "垃圾广告": ("广告", "推广", "促销", "优惠", "下单", "购买", "加微信", "链接"),
                }
                matched = [part for part in meaningful if part in text]
                for concept, aliases in concept_aliases.items():
                    if concept in description:
                        matched.extend(alias for alias in aliases if alias in text)
                for alias in type_aliases.get(str(rule.get("violation_type") or ""), ()):
                    if alias in description or str(rule.get("violation_type") or "") in description:
                        if alias in text:
                            matched.append(alias)
                prohibition = any(word in description for word in ("禁止", "不得", "不允许", "违规", "严禁"))
                if prohibition and len(set(matched)) >= 1:
                    matches.append({"type": str(rule.get("violation_type") or "community_rule"),
                                    "fragment": f"依据规则说明判断：{description[:180]}",
                                    "severity": str(rule.get("severity") or "medium"), "rule_id": rule_id})
        for case in state.get("cases", []):
            case_id = case.get("case_id")
            terms = case.get("keywords") or case.get("terms") or []
            if not isinstance(case_id, int) or not isinstance(terms, list):
                continue
            hits = [str(term) for term in terms if str(term).strip() and str(term).casefold() in text]
            if hits:
                matches.append({"type": str(case.get("violation_type") or "similar_moderation_case"),
                                "fragment": f"与案例 {case_id} 的触发词相似：" + "、".join(hits),
                                "severity": str(case.get("severity") or "medium"), "case_id": case_id})
        return {"matches": matches}

    def _decide(self, state: GovernanceState) -> dict[str, Any]:
        matches = state.get("matches", [])
        high = [item for item in matches if item.get("severity") == "high"]
        medium = [item for item in matches if item.get("severity") == "medium"]
        risk = "high" if high else "medium" if medium else "low"
        confidence = 0.96 if high else 0.88 if medium else 0.72
        action = "temporary_hide" if high else "manual_review" if medium else "allow"
        return {"result": {"risk_level": risk,
            "violation_types": list(dict.fromkeys(str(item["type"]) for item in matches)),
            "matched_rule_ids": list(dict.fromkeys(item["rule_id"] for item in matches if isinstance(item.get("rule_id"), int))),
            "matched_case_ids": list(dict.fromkeys(item["case_id"] for item in matches if isinstance(item.get("case_id"), int))),
            "evidence_fragments": (list(dict.fromkeys(str(item["fragment"]) for item in matches))
                                   + list(dict.fromkeys(str(item) for item in state.get("snapshot_evidence", []))))[:20],
            "confidence": confidence, "suggested_action": action}}
