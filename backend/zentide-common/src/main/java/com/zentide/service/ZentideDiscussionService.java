package com.zentide.service;

import com.zentide.entity.po.ZentideChangeDiscussion;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideDiscussionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ZentideDiscussionService {
    private final ZentideDiscussionMapper mapper;

    public ZentideDiscussionService(ZentideDiscussionMapper mapper) {
        this.mapper = mapper;
    }

    public List<ZentideChangeDiscussion> list(Long changeId, Integer requestedLimit) {
        requirePublishedChange(changeId);
        int limit = requestedLimit == null ? 100 : Math.max(1, Math.min(200, requestedLimit));
        return mapper.listPublished(changeId, limit);
    }

    @Transactional
    public ZentideChangeDiscussion publish(String authorId, Long changeId, Long parentDiscussionId, Long evidenceId, String requestedBody) {
        requirePublishedChange(changeId);
        String body = requestedBody == null ? "" : requestedBody.trim();
        if (body.length() < 2 || body.length() > 2_000) {
            throw new BusinessException("讨论内容需要是 2 到 2000 个字符");
        }
        if (parentDiscussionId != null && mapper.findPublishedInChange(changeId, parentDiscussionId) == null) {
            throw new BusinessException("回复目标不存在或不属于该 Change");
        }
        if (evidenceId != null && mapper.countEvidenceForPublishedChange(changeId, evidenceId) != 1) {
            throw new BusinessException("引用 Evidence 不存在或不属于该 Change");
        }
        ZentideChangeDiscussion discussion = new ZentideChangeDiscussion();
        discussion.setChangeId(changeId);
        discussion.setAuthorId(authorId);
        discussion.setParentDiscussionId(parentDiscussionId);
        discussion.setEvidenceId(evidenceId);
        discussion.setBody(body);
        mapper.insert(discussion);
        discussion.setAuthorLabel("社区成员");
        discussion.setStatus("PUBLISHED");
        return discussion;
    }

    @Transactional
    public void remove(String authorId, Long discussionId) {
        if (discussionId == null || mapper.removeOwned(authorId, discussionId) != 1) {
            throw new BusinessException("讨论不存在或无权移除");
        }
    }

    private void requirePublishedChange(Long changeId) {
        if (changeId == null || mapper.countPublishedChange(changeId) != 1) {
            throw new BusinessException("Change 不存在或尚未验证发布");
        }
    }
}
