from agent.references import linkify_post_citations, post_url


def test_post_citation_becomes_internal_link():
    assert linkify_post_citations("依据[帖子 123]。") == (
        "依据[帖子 123](http://localhost:6001/community/posts/123)。"
    )


def test_floor_citation_points_to_same_post():
    assert linkify_post_citations("依据[帖子 123 · 2 楼]。") == (
        "依据[帖子 123 · 2 楼](http://localhost:6001/community/posts/123)。"
    )


def test_post_title_replaces_numeric_label():
    assert linkify_post_citations(
        "依据[帖子 123]。",
        allowed_post_ids={123},
        post_titles={123: "小场馆观演，提前选位置真的很重要"},
    ) == (
        "依据[小场馆观演，提前选位置真的很重要](http://localhost:6001/community/posts/123)。"
    )


def test_floor_is_preserved_after_title_replacement():
    assert linkify_post_citations(
        "依据[帖子 123 · 2 楼]。",
        allowed_post_ids={123},
        post_titles={123: "现场视野"},
    ) == "依据[现场视野 · 2 楼](http://localhost:6001/community/posts/123)。"


def test_non_positive_or_non_numeric_ids_are_not_linked():
    text = "[帖子 0] [帖子 -1] [帖子 abc]"
    assert linkify_post_citations(text) == text


def test_external_markdown_link_is_removed():
    assert linkify_post_citations("[危险链接](https://evil.example/a)") == "危险链接"


def test_existing_post_link_is_not_nested():
    text = "[帖子 123](http://localhost:6001/community/posts/123)"
    result = linkify_post_citations(text)
    assert result == text
    assert result.count("community/posts/123") == 1


def test_custom_community_url(monkeypatch):
    monkeypatch.setenv("ZENTIDE_COMMUNITY_URL", "https://community.example/app/")
    assert linkify_post_citations("[帖子 7]") == (
        "[帖子 7](https://community.example/app/community/posts/7)"
    )


def test_tool_backed_ids_can_be_restricted():
    assert linkify_post_citations("[帖子 7] [帖子 8]", allowed_post_ids={7}) == (
        "[帖子 7](http://localhost:6001/community/posts/7) [帖子 8]"
    )


def test_post_url_rejects_non_positive_ids():
    for value in (0, -1, 1.2, True, "abc"):
        try:
            post_url(value)
        except ValueError:
            continue
        raise AssertionError("post_url should reject non-positive/non-numeric IDs")
