from api import AnswerBlock, ConversationMemory, answer_blocks
from agent.react_agent import sanitize_answer


def test_answer_links_become_structured_reference_blocks():
    blocks = answer_blocks(
        "可以阅读[现场视野](http://localhost:6001/community/posts/12)继续了解。"
    )
    assert blocks == [
        AnswerBlock(type="text", text="可以阅读"),
        AnswerBlock(type="reference", entity_type="POST", entity_id=12, label="现场视野"),
        AnswerBlock(type="text", text="继续了解。"),
    ]


def test_conversation_memory_is_scoped_and_bounded():
    memory = ConversationMemory(max_conversations=2, max_messages=2)
    first = ("u1", "conversation1", 5, "scene", None)
    second = ("u1", "conversation2", 5, "scene", None)
    third = ("u2", "conversation3", 6, "scene", None)
    memory.append(first, "问题", "回答")
    memory.append(second, "问题2", "回答2")
    memory.append(third, "问题3", "回答3")
    assert memory.history(first) == []
    assert memory.history(third)[-1]["content"] == "回答3"


def test_sanitize_answer_hides_internal_identifiers():
    answer = sanitize_answer("当前兴趣现场（scene_id=5）可参考 post_id=12，user_id=u1；我会聚焦帖子 #18。")
    assert "scene_id" not in answer.lower()
    assert "post_id" not in answer.lower()
    assert "user_id" not in answer.lower()
    assert "5" not in answer
    assert "帖子 #18" not in answer


def test_untrusted_post_citation_does_not_expose_numeric_id():
    from agent.react_agent import format_answer

    assert format_answer("可以参考[帖子 18]。", allowed_post_ids=set()) == "可以参考相关帖子。"
