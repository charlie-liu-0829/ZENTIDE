from dataclasses import dataclass

from review_service import PostReviewService, ReviewStore, content_fingerprint
from model.intelligence import ModelAnalysis


@dataclass
class Post:
    post_id: int
    title: str
    body: str
    topics: tuple[str, ...] = ()


class Snapshots:
    def __init__(self):
        self.posts = [Post(18, "Spring Boot 调用 LangChain 微服务", "使用 WebClient 调用 Python 服务")]

    def list_posts(self, scene_id, permitted_visibilities):
        assert scene_id == 5
        assert permitted_visibilities == {"PUBLIC"}
        return self.posts

    def load_post(self, post_id):
        return next((post for post in self.posts if post.post_id == post_id), None)


class FakeModels:
    def __init__(self):
        self.embed_calls = 0
        self.analyze_calls = 0

    def embed(self, texts):
        self.embed_calls += 1
        return [[1.0, 0.0] if "Spring" in text or "LangChain" in text or "WebClient" in text else [0.0, 1.0]
                for text in texts]

    def analyze(self, **kwargs):
        self.analyze_calls += 1
        return ModelAnalysis(
            suggested_post_type="QUESTION",
            missing_information=["预期结果", "实际结果", "已尝试方案"],
            suggested_title="Spring Boot 如何通过 WebClient 调用 Python LangChain 服务？",
            suggested_topic_ids=[12],
            community_rule_warnings=[],
            optimized_content="我使用 Spring Boot 的 WebClient 请求 LangChain，但没有得到预期响应。\n\n应该如何排查？",
            content_improvements=["重组问题背景与核心诉求", "拆分段落以提升可读性"],
        )


class RulesOnlyModels:
    def embed(self, texts):
        return None

    def analyze(self, **kwargs):
        return None


def test_review_detects_duplicate_and_question_completeness():
    result = PostReviewService(Snapshots(), models=RulesOnlyModels()).review(
        user_id="u1", scene_id=5,
        title="Spring Boot 怎么调用 Python LangChain？",
        content="我想通过 WebClient 调用 Python 的 LangChain 服务。",
        selected_post_type="QUESTION", permitted_visibilities={"PUBLIC"},
        available_topics=[{"topic_id": 12, "name": "LangChain"}, {"topic_id": 25, "name": "摄影"}],
    )
    assert result["similar_posts"][0]["post_id"] == 18
    assert "复现步骤" in result["missing_information"]
    assert result["suggested_topic_ids"] == [12]
    assert result["advice"] in {"revise", "view_similar_posts"}


def test_sensitive_secret_blocks_publish_and_skips_external_models():
    models = FakeModels()
    result = PostReviewService(Snapshots(), models=models).review(
        user_id="u1", scene_id=5, title="接口调用失败",
        content="这是足够长的描述，api_key: sk-secret-value，请帮我检查具体的调用错误。",
        selected_post_type="QUESTION",
    )
    assert result["publish_blocked"] is True
    assert result["advice"] == "revise"
    assert result["sensitive_information_warnings"]
    assert result["blocking_reasons"] == result["sensitive_information_warnings"]
    assert result["optimized_content"] is None
    assert result["similar_posts"] == []
    assert models.embed_calls == 0
    assert models.analyze_calls == 0


def test_secret_in_rich_text_link_is_also_blocked():
    models = FakeModels()
    result = PostReviewService(Snapshots(), models=models).review(
        user_id="u1", scene_id=5, title="接口调用参考链接",
        content='<p>请查看<a href="https://example.test/callback?token=secret-value">调用文档</a></p>',
        selected_post_type="QUESTION",
    )
    assert result["publish_blocked"] is True
    assert models.embed_calls == 0
    assert models.analyze_calls == 0


def test_semantic_model_drives_type_and_suggestions_but_not_duplicate_score():
    result = PostReviewService(Snapshots(), models=FakeModels()).review(
        user_id="u1", scene_id=5, title="Spring 调用 AI 服务",
        content="WebClient 请求 LangChain 时没有得到预期响应，应该如何排查？",
        selected_post_type="DISCUSSION", selected_topic_ids=[], permitted_visibilities={"PUBLIC"},
        available_topics=[{"topic_id": 12, "name": "LangChain"}],
    )
    assert result["analysis_mode"] == "llm"
    assert result["suggested_post_type"] == "QUESTION"
    assert result["suggested_title"].startswith("Spring Boot")
    assert result["suggested_topic_ids"] == [12]
    assert result["optimized_content"].startswith("我使用 Spring Boot")
    assert result["content_improvements"]
    assert result["duplicate_probability"] >= 0.65
    assert "标题语义高度接近" in result["similar_posts"][0]["reason"]


def test_experience_and_event_use_different_completeness_templates():
    service = PostReviewService(models=RulesOnlyModels())
    experience = service.review(user_id="u1", scene_id=5, title="我的使用经验分享",
                                content="这里记录一段比较简短的个人经历。",
                                selected_post_type="EXPERIENCE")
    event = service.review(user_id="u1", scene_id=5, title="周末交流活动通知",
                           content="欢迎大家参加这次线下交流。", selected_post_type="EVENT")
    assert "使用限制" in experience["missing_information"]
    assert "报名截止时间" in event["missing_information"]


def test_review_is_bound_to_user_scene_and_exact_content():
    service = PostReviewService(store=ReviewStore(ttl_seconds=60), models=RulesOnlyModels())
    content = "这是一段长度足够的正文，用来验证审核结果与原始内容严格绑定，不允许检查后替换。"
    result = service.review(user_id="u1", scene_id=5, title="一个完整标题", content=content,
                            selected_post_type="DISCUSSION", selected_topic_ids=[12])
    valid = service.validate(review_id=result["review_id"], user_id="u1", scene_id=5,
                             title="一个完整标题", content=content,
                             selected_post_type="DISCUSSION", selected_topic_ids=[12])
    changed = service.validate(review_id=result["review_id"], user_id="u1", scene_id=5,
                               title="偷偷替换的标题", content=content,
                               selected_post_type="DISCUSSION", selected_topic_ids=[12])
    assert valid["valid"] is True
    assert changed["valid"] is False


def test_fingerprint_normalizes_topic_order_and_html_text():
    assert content_fingerprint(" 标题 ", "<p>正文</p>", "question", [2, 1]) == content_fingerprint(
        "标题", "正文", "QUESTION", [1, 2]
    )


def test_optimization_that_invents_concrete_facts_is_discarded():
    class HallucinatingModels(FakeModels):
        def analyze(self, **kwargs):
            analysis = super().analyze(**kwargs)
            return ModelAnalysis(
                suggested_post_type=analysis.suggested_post_type,
                missing_information=analysis.missing_information,
                suggested_title=analysis.suggested_title,
                suggested_topic_ids=analysis.suggested_topic_ids,
                community_rule_warnings=analysis.community_rule_warnings,
                optimized_content="我在 Spring Boot 3.5.9 中调用服务，错误发生于 2026-09-08。",
                content_improvements=["补充了版本与日期"],
            )

    result = PostReviewService(models=HallucinatingModels()).review(
        user_id="u1", scene_id=5, title="Spring 服务调用异常",
        content="我在 Spring Boot 中调用服务时发生异常，希望优化问题描述。",
        selected_post_type="QUESTION",
    )
    assert result["optimized_content"] is None
