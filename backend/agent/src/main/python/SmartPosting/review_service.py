"""发布前帖子质量检查与内容优化 Agent。"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
import hashlib
import html
import re
from threading import RLock
from typing import Any, TypedDict
from uuid import uuid4

from model.intelligence import SmartPostingModels, cosine
from rule_store import CommunityRuleStore


_TOKEN = re.compile(r"[\u4e00-\u9fff]{1,}|[a-zA-Z0-9_]+")
_PHONE = re.compile(r"(?<!\d)(?:\+?86[- ]?)?1[3-9]\d{9}(?!\d)")
_EMAIL = re.compile(r"\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b", re.I)
_SECRET = re.compile(
    r"(?i)(?:api[_ -]?key|access[_ -]?token|secret|password|私钥|密钥|密码)\s*[:=：]\s*[^\s,，;；]+"
)
_URL_SECRET = re.compile(r"https?://[^\s]+(?:token|secret|password|api[_-]?key)=[^\s&]+", re.I)
_JWT = re.compile(r"\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\b")
_BEARER = re.compile(r"(?i)\bbearer\s+[A-Za-z0-9._~+/=-]{12,}")
_PRIVATE_KEY = re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----")
_CLOUD_ACCESS_KEY = re.compile(r"\b(?:AKIA[0-9A-Z]{16}|LTAI[A-Za-z0-9]{12,24})\b")
_CONCRETE_FACT = re.compile(
    r"https?://[^\s]+|(?<![\w.])v?\d+(?:\.\d+){1,3}(?![\w.])|"
    r"(?<!\d)\d{4}[-年/]\d{1,2}(?:[-月/]\d{1,2}日?)?(?!\d)|"
    r"(?<!\d)\d{1,2}:\d{2}(?!\d)|(?<!\d)\d{5,}(?!\d)", re.I
)

DEFAULT_COMMUNITY_RULES = [
    "不得发布手机号、密码、访问令牌、私钥等敏感信息",
    "标题应准确描述正文，不使用与内容无关的夸张表述",
    "引用他人内容时应说明来源",
    "发布问题前应说明已尝试的方法以及仍未解决之处",
]
TYPE_REQUIREMENTS = {
    "QUESTION": ["问题描述", "运行环境", "复现步骤", "预期结果", "实际结果", "已尝试方案"],
    "EXPERIENCE": ["适用场景", "具体方案", "优缺点", "使用限制", "参考内容"],
    "EVENT": ["时间", "地点", "参与方式", "人数限制", "报名截止时间"],
    "REVIEW": ["评测对象", "使用场景", "评价维度", "优点", "缺点", "结论"],
    "POLL": ["调查问题", "选项说明", "截止时间"],
    "DISCUSSION": ["讨论背景", "核心观点或问题"],
}
FIELD_PATTERNS = {
    "问题描述": r"问题|现象|怎么|如何|无法|失败|报错|错误|异常",
    "运行环境": r"环境|版本|系统|设备|运行|python|java|node|浏览器|spring",
    "复现步骤": r"步骤|复现|操作|命令|代码|先.+再|执行",
    "预期结果": r"预期|期望|应该|希望",
    "实际结果": r"实际|结果|报错|错误|异常|却|但是",
    "已尝试方案": r"尝试|试过|排查|搜索|查阅|仍然|无效|未解决",
    "适用场景": r"场景|适用|用于|面对|当.+时",
    "具体方案": r"方案|步骤|做法|实现|配置|代码",
    "优缺点": r"优点|缺点|优势|不足|好处|代价",
    "使用限制": r"限制|前提|仅适用|不适用|注意",
    "参考内容": r"参考|来源|链接|文档|https?://",
    "时间": r"时间|日期|\d{1,2}[月/-]\d{1,2}|\d{1,2}:\d{2}",
    "地点": r"地点|地址|线上|线下|会议室|场馆",
    "参与方式": r"参与|报名|加入|扫码|联系",
    "人数限制": r"人数|名额|限\d+|上限",
    "报名截止时间": r"截止|报名至|截至",
    "评测对象": r"评测|测评|产品|设备|软件|版本",
    "使用场景": r"场景|使用|体验|测试环境",
    "评价维度": r"维度|性能|体验|质量|价格|功能",
    "优点": r"优点|优势|值得|满意",
    "缺点": r"缺点|不足|问题|遗憾",
    "结论": r"结论|总结|推荐|不推荐|总体",
    "调查问题": r"投票|调查|选择|你会|你认为",
    "选项说明": r"选项|A[.、:]|B[.、:]|①|②",
    "截止时间": r"截止|截至|结束时间",
    "讨论背景": r"背景|最近|目前|因为|关于",
    "核心观点或问题": r"观点|认为|问题|讨论|怎么看|是否",
}


class ReviewState(TypedDict, total=False):
    scene_id: int
    title: str
    content: str
    sensitive_source: str
    post_type: str
    normalized_type: str
    topic_ids: list[int]
    permitted_visibilities: set[str]
    available_topics: list[dict[str, Any]]
    missing: list[str]
    sensitive: list[str]
    community_warnings: list[str]
    looks_like_question: bool
    similar_posts: list[dict[str, Any]]
    duplicate_probability: float
    suggested_title: str | None
    suggested_post_type: str | None
    suggested_topic_ids: list[int]
    advice: str
    type_requirements: dict[str, list[str]]
    community_rules: list[str]
    model_analysis: Any
    analysis_mode: str
    optimized_content: str | None
    content_improvements: list[str]
    publish_blocked: bool
    blocking_reasons: list[str]


def _plain(text: str) -> str:
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", text or ""))).strip()


def _plain_content(text: str) -> str:
    value = re.sub(r"(?i)<\s*br\s*/?\s*>", "\n", text or "")
    value = re.sub(r"(?i)</\s*(?:p|div|h[1-6]|li|blockquote|pre)\s*>", "\n", value)
    value = html.unescape(re.sub(r"<[^>]+>", "", value))
    value = "\n".join(re.sub(r"[ \t]+", " ", line).strip() for line in value.splitlines())
    return re.sub(r"\n{3,}", "\n\n", value).strip()


def _safe_optimized_content(original: str, candidate: str | None) -> str | None:
    """Reject optimization output that introduces obvious concrete facts."""
    if not candidate:
        return None
    optimized = _plain_content(candidate)[:8000]
    if not optimized or optimized == original:
        return None
    original_facts = {item.casefold().rstrip(".,，。;；") for item in _CONCRETE_FACT.findall(original)}
    optimized_facts = {item.casefold().rstrip(".,，。;；") for item in _CONCRETE_FACT.findall(optimized)}
    if not optimized_facts.issubset(original_facts):
        return None
    # Links are operational content. An optimization may reorganize them, but must not silently drop them.
    original_links = {item for item in original_facts if item.startswith(("http://", "https://"))}
    if not original_links.issubset(optimized_facts):
        return None
    return optimized


def _tokens(text: str) -> set[str]:
    result: set[str] = set()
    for value in _TOKEN.findall(_plain(text)):
        value = value.lower().strip()
        if not value:
            continue
        if re.fullmatch(r"[\u4e00-\u9fff]+", value) and len(value) > 2:
            result.update(value[index:index + 2] for index in range(len(value) - 1))
        else:
            result.add(value)
    return result


def content_fingerprint(title: str, content: str, post_type: str | None,
                       topic_ids: list[int] | None) -> str:
    normalized = "\n".join([
        _plain(title).casefold(), _plain(content), (post_type or "").strip().upper(),
        ",".join(str(value) for value in sorted(set(topic_ids or []))),
    ])
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()


@dataclass(frozen=True)
class ReviewRecord:
    review_id: str
    user_id: str
    scene_id: int
    fingerprint: str
    created_at: datetime
    result: dict[str, Any]


class ReviewStore:
    def __init__(self, ttl_seconds: int = 15 * 60):
        self.ttl = timedelta(seconds=ttl_seconds)
        self._items: dict[str, ReviewRecord] = {}
        self._lock = RLock()

    def put(self, record: ReviewRecord) -> None:
        with self._lock:
            self._purge()
            self._items[record.review_id] = record

    def get(self, review_id: str, user_id: str, scene_id: int,
            fingerprint: str | None = None) -> ReviewRecord | None:
        with self._lock:
            self._purge()
            record = self._items.get(review_id)
            if not record or record.user_id != user_id or record.scene_id != scene_id:
                return None
            if fingerprint is not None and record.fingerprint != fingerprint:
                return None
            return record

    def _purge(self) -> None:
        cutoff = datetime.now(timezone.utc) - self.ttl
        self._items = {key: value for key, value in self._items.items() if value.created_at >= cutoff}


def _similarity(title: str, content: str, post: Any, *, title_semantic: float | None = None,
                body_semantic: float | None = None,
                selected_topic_names: set[str] | None = None) -> tuple[float, str]:
    current_title = _tokens(title)
    current_body = _tokens(content)
    old_title = _tokens(getattr(post, "title", ""))
    old_body = _tokens(getattr(post, "body", ""))
    title_lexical = len(current_title & old_title) / max(len(current_title | old_title), 1)
    body_lexical = len(current_body & old_body) / max(len(current_body | old_body), 1)
    keyword_score = len((current_title | current_body) & (old_title | old_body)) / max(
        len(current_title | current_body), 1
    )
    title_score = title_semantic if title_semantic is not None else title_lexical
    body_score = body_semantic if body_semantic is not None else body_lexical
    post_topics = {str(topic).strip().casefold() for topic in getattr(post, "topics", ()) if str(topic).strip()}
    topic_score = 1.0 if post_topics & (selected_topic_names or set()) else 0.0
    accepted_score = 1.0 if bool(getattr(post, "has_accepted_answer", False)) else 0.0
    # LLM 不参与分数计算；五个分量均可回溯和调参。
    score = min(1.0, 0.35 * title_score + 0.30 * body_score + 0.20 * keyword_score
                + 0.10 * topic_score + 0.05 * accepted_score)
    reasons = []
    if title_score >= 0.70:
        reasons.append("标题语义高度接近")
    elif title_lexical >= 0.30:
        reasons.append("标题关键词重合")
    if body_score >= 0.68:
        reasons.append("正文语义接近")
    elif body_lexical >= 0.22:
        reasons.append("正文关键词重合")
    if topic_score:
        reasons.append("属于同一话题")
    if accepted_score:
        reasons.append("已有采纳答案")
    return round(score, 2), "、".join(reasons) or "内容主题相近"


class PostReviewService:
    def __init__(self, snapshot_store: Any | None = None, store: ReviewStore | None = None,
                 models: SmartPostingModels | None = None,
                 rule_store: CommunityRuleStore | None = None):
        self.snapshot_store = snapshot_store
        self.store = store or ReviewStore()
        self.models = models or SmartPostingModels()
        self.rule_store = rule_store or CommunityRuleStore(DEFAULT_COMMUNITY_RULES, TYPE_REQUIREMENTS)

    def _hard_rules(self, state: dict[str, Any]) -> dict[str, Any]:
        missing: list[str] = []
        if not state["title"]:
            missing.append("标题")
        elif len(state["title"]) < 6:
            missing.append("更明确的标题")
        if len(state["content"]) < 2:
            missing.append("正文")
        elif len(state["content"]) < 30:
            missing.append("更多背景或具体细节")
        return {"missing": missing}

    def _sensitive_information(self, state: dict[str, Any]) -> dict[str, Any]:
        # Scan the raw rich-text source as well as visible text so secrets in link URLs/attributes
        # cannot disappear during HTML-to-text normalization.
        text = state.get("sensitive_source") or f'{state["title"]} {state["content"]}'
        warnings: list[str] = []
        if _PHONE.search(text):
            warnings.append("检测到手机号，请移除或脱敏后重新检查")
        if _EMAIL.search(text):
            warnings.append("检测到邮箱地址，请移除或脱敏后重新检查")
        if _SECRET.search(text) or _URL_SECRET.search(text):
            warnings.append("检测到疑似密钥或密码，请先移除敏感值")
        if _JWT.search(text) or _BEARER.search(text):
            warnings.append("检测到疑似访问令牌，请先移除敏感值")
        if _PRIVATE_KEY.search(text):
            warnings.append("检测到私钥内容，请立即移除")
        if _CLOUD_ACCESS_KEY.search(text):
            warnings.append("检测到疑似云服务访问密钥，请先移除敏感值")
        return {"sensitive": list(dict.fromkeys(warnings))}

    @staticmethod
    def _blocked_result(state: dict[str, Any]) -> dict[str, Any]:
        reasons = list(state.get("sensitive", []))
        return {
            "similar_posts": [],
            "duplicate_probability": 0.0,
            "suggested_title": None,
            "suggested_post_type": state.get("post_type") or None,
            "suggested_topic_ids": list(dict.fromkeys(state.get("topic_ids", [])))[:3],
            "community_warnings": [],
            "optimized_content": None,
            "content_improvements": ["先移除或脱敏所有风险内容，再重新运行智能检查"],
            "analysis_mode": "rules",
            "advice": "revise",
            "publish_blocked": True,
            "blocking_reasons": reasons,
        }

    @staticmethod
    def _route_after_sensitive(state: dict[str, Any]) -> str:
        return "blocked" if state.get("sensitive") else "continue"

    def _scene_rules(self, state: dict[str, Any]) -> dict[str, Any]:
        looks_like_question = bool(re.search(
            r"[？?]|怎么|如何|为什么|求助|报错", f'{state["title"]} {state["content"]}'
        ))
        warnings = []
        if looks_like_question and state["normalized_type"] not in {"QUESTION", "问题", "Q&A", "QA"}:
            warnings.append("检测到内容可能是问题帖，建议选择“问题”类型")
        return {"looks_like_question": looks_like_question, "community_warnings": warnings}

    def _recall_similar_posts(self, state: dict[str, Any]) -> dict[str, Any]:
        similar_posts: list[dict[str, Any]] = []
        if self.snapshot_store is not None:
            posts = self.snapshot_store.list_posts(state["scene_id"], state["permitted_visibilities"])
            texts = [state["title"], state["content"]]
            for post in posts:
                texts.extend((getattr(post, "title", ""), getattr(post, "body", "")))
            vectors = self.models.embed(texts)
            selected_ids = set(state["topic_ids"])
            selected_topic_names = {
                str(topic.get("name") or "").strip().casefold()
                for topic in state.get("available_topics", [])
                if str(topic.get("topic_id") or "").isdigit()
                and int(topic["topic_id"]) in selected_ids
                and str(topic.get("name") or "").strip()
            }
            for index, post in enumerate(posts):
                title_semantic = cosine(vectors[0], vectors[2 + index * 2]) if vectors else None
                body_semantic = cosine(vectors[1], vectors[3 + index * 2]) if vectors else None
                score, reason = _similarity(
                    state["title"], state["content"], post,
                    title_semantic=title_semantic, body_semantic=body_semantic,
                    selected_topic_names=selected_topic_names,
                )
                if score >= 0.18:
                    similar_posts.append({
                        "post_id": post.post_id, "title": post.title, "similarity": score,
                        "reason": reason,
                        "has_accepted_answer": bool(getattr(post, "has_accepted_answer", False)),
                    })
        similar_posts.sort(key=lambda item: item["similarity"], reverse=True)
        return {"similar_posts": similar_posts[:5]}

    @staticmethod
    def _duplicate_score(state: dict[str, Any]) -> dict[str, Any]:
        posts = state.get("similar_posts", [])
        return {"duplicate_probability": posts[0]["similarity"] if posts else 0.0}

    def _post_type(self, state: dict[str, Any]) -> dict[str, Any]:
        analysis = self.models.analyze(
            title=state["title"], content=state["content"], selected_post_type=state["post_type"],
            type_requirements=state["type_requirements"], available_topics=state["available_topics"],
            community_rules=state["community_rules"], similar_posts=state.get("similar_posts", []),
        )
        if analysis is not None:
            warnings = list(dict.fromkeys(state.get("community_warnings", []) + analysis.community_rule_warnings))
            suggested_type = analysis.suggested_post_type or state["post_type"] or "DISCUSSION"
            if state["post_type"] and suggested_type != state["normalized_type"]:
                warnings.append(f"当前内容更符合“{suggested_type}”类型")
            return {
                "model_analysis": analysis,
                "analysis_mode": "llm",
                "suggested_post_type": suggested_type,
                "community_warnings": list(dict.fromkeys(warnings)),
            }
        text = f'{state["title"]} {state["content"]}'
        if state.get("looks_like_question"):
            suggested = "QUESTION"
        elif re.search(r"报名|活动|地点|举办|参与", text):
            suggested = "EVENT"
        elif re.search(r"评测|测评|优点|缺点|评分", text):
            suggested = "REVIEW"
        elif re.search(r"投票|调查|选项|你会选择", text):
            suggested = "POLL"
        elif re.search(r"经验|教程|实践|踩坑|方案", text):
            suggested = "EXPERIENCE"
        else:
            suggested = state["post_type"] or "DISCUSSION"
        warnings = list(state.get("community_warnings", []))
        if state["post_type"] and suggested != state["normalized_type"]:
            warnings.append(f"当前内容更符合“{suggested}”类型")
        return {"model_analysis": None, "analysis_mode": "rules", "suggested_post_type": suggested,
                "community_warnings": list(dict.fromkeys(warnings))}

    @staticmethod
    def _completeness(state: dict[str, Any]) -> dict[str, Any]:
        missing = list(state.get("missing", []))
        effective_type = state.get("suggested_post_type") or state["normalized_type"] or "DISCUSSION"
        requirements = state["type_requirements"].get(effective_type, [])
        text = f'{state["title"]} {state["content"]}'
        for field in requirements:
            pattern = FIELD_PATTERNS.get(field)
            if pattern and not re.search(pattern, text, re.I):
                missing.append(field)
        analysis = state.get("model_analysis")
        if analysis is not None:
            missing.extend(item for item in analysis.missing_information if item in requirements)
        return {"missing": list(dict.fromkeys(missing))}

    @staticmethod
    def _suggestions(state: dict[str, Any]) -> dict[str, Any]:
        analysis = state.get("model_analysis")
        suggested_title = analysis.suggested_title if analysis is not None else state["title"]
        if suggested_title and len(suggested_title) < 12 and state["content"]:
            first_sentence = re.split(r"[。！？.!?\n]", state["content"])[0].strip()
            if first_sentence and first_sentence.casefold() not in suggested_title.casefold():
                suggested_title = f"{suggested_title}：{first_sentence[:48]}"
        suggested_topics = list(dict.fromkeys(state["topic_ids"]))
        if analysis is not None:
            suggested_topics.extend(
                topic_id for topic_id in analysis.suggested_topic_ids if topic_id not in suggested_topics
            )
        content_tokens = _tokens(f'{state["title"]} {state["content"]}')
        topic_scores: list[tuple[int, int]] = []
        for topic in state.get("available_topics", []):
            try:
                topic_id = int(topic.get("topic_id"))
            except (AttributeError, TypeError, ValueError):
                continue
            overlap = len(content_tokens & _tokens(str(topic.get("name") or "")))
            if topic_id > 0 and overlap > 0 and topic_id not in suggested_topics:
                topic_scores.append((overlap, topic_id))
        topic_scores.sort(reverse=True)
        suggested_topics.extend(topic_id for _, topic_id in topic_scores[:3 - len(suggested_topics)])
        optimized_content = _safe_optimized_content(
            state["content"], analysis.optimized_content if analysis is not None else None
        )
        if analysis is not None:
            improvements = list(dict.fromkeys(analysis.content_improvements))
        else:
            improvements = []
            if state.get("missing"):
                improvements.append(f"补充{ '、'.join(state['missing'][:4]) }，让读者更容易理解和回应")
            if len(state["content"]) < 120:
                improvements.append("增加必要背景和具体细节，避免正文过于简略")
            if not re.search(r"[。！？.!?]\s*\n|\n\n", state["content"]):
                improvements.append("按背景、过程和结论分段，提升可读性")
        return {
            "suggested_title": suggested_title or None,
            "suggested_topic_ids": suggested_topics[:3],
            "optimized_content": optimized_content,
            "content_improvements": improvements[:6],
        }

    @staticmethod
    def _advice(state: dict[str, Any]) -> dict[str, Any]:
        if state.get("duplicate_probability", 0.0) >= 0.65:
            advice = "view_similar_posts"
        elif state.get("missing") or state.get("community_warnings"):
            advice = "revise"
        else:
            advice = "ready"
        return {"advice": advice, "publish_blocked": False, "blocking_reasons": []}

    def _execute_graph(self, state: dict[str, Any]) -> dict[str, Any]:
        initial_nodes = [
            ("hard_rules", self._hard_rules),
            ("sensitive_information", self._sensitive_information),
        ]
        analysis_nodes = [
            ("scene_rules", self._scene_rules),
            ("similar_posts", self._recall_similar_posts),
            ("duplicate_score", self._duplicate_score),
            ("post_type", self._post_type),
            ("completeness", self._completeness),
            ("suggestions", self._suggestions),
            ("advice", self._advice),
        ]
        try:
            from langgraph.graph import END, START, StateGraph
        except ImportError:
            # Keeps local rule tests usable before optional runtime dependencies are installed.
            for _, node in initial_nodes:
                state.update(node(state))
            if state.get("sensitive"):
                state.update(self._blocked_result(state))
                return state
            for _, node in analysis_nodes:
                state.update(node(state))
            return state
        graph = StateGraph(ReviewState)
        for name, node in initial_nodes + analysis_nodes:
            graph.add_node(name, node)
        graph.add_node("blocked_result", self._blocked_result)
        graph.add_edge(START, "hard_rules")
        graph.add_edge("hard_rules", "sensitive_information")
        graph.add_conditional_edges(
            "sensitive_information", self._route_after_sensitive,
            {"blocked": "blocked_result", "continue": analysis_nodes[0][0]},
        )
        for (current, _), (following, _) in zip(analysis_nodes, analysis_nodes[1:]):
            graph.add_edge(current, following)
        graph.add_edge(analysis_nodes[-1][0], END)
        graph.add_edge("blocked_result", END)
        return graph.compile().invoke(state)

    def review(self, *, user_id: str, scene_id: int, title: str, content: str,
               selected_post_type: str | None = None, selected_topic_ids: list[int] | None = None,
               permitted_visibilities: set[str] | None = None,
               available_topics: list[dict[str, Any]] | None = None,
               draft_id: str | None = None) -> dict[str, Any]:
        sensitive_source = html.unescape(f"{title or ''} {content or ''}")
        title = _plain(title)
        content = _plain_content(content)
        post_type = (selected_post_type or "").strip()
        topic_ids = [int(value) for value in (selected_topic_ids or []) if int(value) > 0]
        normalized_type = post_type.upper()
        state = self._execute_graph({
            "scene_id": scene_id, "title": title, "content": content,
            "sensitive_source": sensitive_source,
            "post_type": post_type, "normalized_type": normalized_type, "topic_ids": topic_ids,
            "permitted_visibilities": permitted_visibilities or {"PUBLIC"},
            "available_topics": available_topics or [],
            "type_requirements": self.rule_store.type_requirements(),
            "community_rules": self.rule_store.rules_for(scene_id),
        })
        result = {
            "review_id": "review_" + uuid4().hex,
            "duplicate_probability": round(float(state["duplicate_probability"]), 2),
            "similar_posts": state["similar_posts"],
            "suggested_title": state["suggested_title"],
            "suggested_post_type": state["suggested_post_type"],
            "suggested_topic_ids": state["suggested_topic_ids"],
            "missing_information": state["missing"],
            "sensitive_information_warnings": state["sensitive"],
            "community_rule_warnings": state["community_warnings"],
            "advice": state["advice"],
            "analysis_mode": state["analysis_mode"],
            "optimized_content": state.get("optimized_content"),
            "content_improvements": state.get("content_improvements", []),
            "publish_blocked": bool(state.get("publish_blocked")),
            "blocking_reasons": state.get("blocking_reasons", []),
            "draft_id": draft_id,
            "checked_at": datetime.now(timezone.utc).isoformat(),
        }
        fingerprint = content_fingerprint(title, content, post_type, topic_ids)
        self.store.put(ReviewRecord(result["review_id"], user_id, scene_id, fingerprint,
                                    datetime.now(timezone.utc), result))
        return result

    def validate(self, *, review_id: str, user_id: str, scene_id: int, title: str, content: str,
                 selected_post_type: str | None, selected_topic_ids: list[int] | None) -> dict[str, Any]:
        fingerprint = content_fingerprint(title, content, selected_post_type, selected_topic_ids)
        record = self.store.get(review_id, user_id, scene_id, fingerprint)
        if record is None:
            return {"valid": False, "reason": "审核结果不存在、已过期，或与当前内容不一致"}
        return {"valid": True, **record.result}
