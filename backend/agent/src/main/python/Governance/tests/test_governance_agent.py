from governance_agent import GovernanceAgent


def test_low_risk_allows():
    result = GovernanceAgent().review(content_id=1, scene_id=5, title="经验分享", content="这是一个正常的经验分享。")
    assert result["risk_level"] == "low"
    assert result["suggested_action"] == "allow"


def test_secret_is_high_and_temporary_hidden():
    result = GovernanceAgent().review(content_id=2, scene_id=5, title="配置", content="api_key=sk-abcdefghijklmnopqrstuvwxyz")
    assert result["risk_level"] == "high"
    assert result["suggested_action"] == "temporary_hide"
    assert "credential_or_secret" in result["violation_types"]


def test_rule_evidence_is_bound():
    result = GovernanceAgent().review(content_id=3, scene_id=5, title="活动", content="请加微信报名", rules=[{"rule_id": 7, "keywords": ["加微信"], "violation_type": "off_platform_contact", "severity": "medium"}])
    assert result["matched_rule_ids"] == [7]
    assert result["suggested_action"] == "manual_review"


def test_case_ids_are_returned():
    result = GovernanceAgent().review(content_id=4, scene_id=5, title="活动", content="请加微信报名", cases=[{"case_id": 12, "keywords": ["加微信"], "violation_type": "off_platform_contact"}])
    assert result["matched_case_ids"] == [12]


def test_abusive_language_is_flagged_for_manual_review():
    result = GovernanceAgent().review(content_id=11, scene_id=5, title="讨论", content="我不是傻子，她妈的不知道")
    assert result["risk_level"] == "medium"
    assert result["suggested_action"] == "manual_review"
    assert "abusive_language" in result["violation_types"]


def test_ad_spam_rule_uses_natural_language_description():
    rule = {
        "rule_id": 21,
        "violation_type": "广告刷屏",
        "rule_description": "禁止在社区内重复发布未经允许的商品推广、促销广告或引流内容；同一账号短时间内连续发布相同营销文案，应判定为广告刷屏。",
        "severity": "high",
    }
    result = GovernanceAgent().review(
        content_id=21,
        scene_id=5,
        title="限时促销！全网最低价，马上下单",
        content=(
            "新品推广，今天下单立减 50 元，欢迎购买。\n"
            "加微信：promo_example 获取优惠券，点击链接立即下单。\n"
            "新品推广，今天下单立减 50 元，欢迎购买。\n"
            "加微信：promo_example 获取优惠券，点击链接立即下单。"
        ),
        rules=[rule],
    )
    assert result["risk_level"] == "high"
    assert result["suggested_action"] == "temporary_hide"
    assert result["matched_rule_ids"] == [21]
    assert "广告刷屏" in result["violation_types"]
    assert any("规则说明" in item for item in result["evidence_fragments"])
