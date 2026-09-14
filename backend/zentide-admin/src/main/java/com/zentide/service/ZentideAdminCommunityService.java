package com.zentide.service;

import com.zentide.entity.po.ZentideHubMemberRequest;
import com.zentide.entity.po.ZentideInterestHub;
import com.zentide.entity.po.ZentideInterestPost;
import com.zentide.entity.po.ZentideInterestComment;
import com.zentide.entity.po.ZentideInterestTopic;
import com.zentide.entity.po.ZentideHubDirection;
import com.zentide.mapper.ZentideInterestMapper;
import com.zentide.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Moderation operations for the community control plane. */
@Service
public class ZentideAdminCommunityService {
    private static final Logger log = LoggerFactory.getLogger(ZentideAdminCommunityService.class);
    private static final Set<String> HUB_STATUSES = Set.of("ACTIVE", "HIDDEN", "ARCHIVED");
    private static final Set<String> POST_STATUSES = Set.of("PUBLISHED", "HIDDEN", "REMOVED");
    private static final Set<String> MEMBER_STATUSES = Set.of("ACTIVE", "REJECTED");

    private final ZentideInterestMapper mapper;
    private final ZentideKnowledgeSnapshotService knowledgeSnapshots;

    public ZentideAdminCommunityService(ZentideInterestMapper mapper) {
        this(mapper, null);
    }

    @Autowired
    public ZentideAdminCommunityService(ZentideInterestMapper mapper, ZentideKnowledgeSnapshotService knowledgeSnapshots) {
        this.mapper = mapper;
        this.knowledgeSnapshots = knowledgeSnapshots;
    }

    public List<ZentideInterestHub> listHubs(String query, String status, Integer limit) {
        return mapper.adminListHubs(clean(query), normalize(status), bounded(limit, 200));
    }

    public List<ZentideInterestPost> listPosts(String query, String status, Long hubId, Integer limit) {
        return mapper.adminListPosts(clean(query), normalize(status), hubId, bounded(limit, 300));
    }

    public List<ZentideHubMemberRequest> listPendingMembers(Integer limit) {
        return mapper.adminListPendingHubMembers(bounded(limit, 300));
    }

    public List<ZentideInterestComment> listComments(String query, String status, Long hubId, Integer limit) {
        return mapper.adminListComments(clean(query), normalize(status), hubId, bounded(limit, 300));
    }

    public void updateCommentStatus(Long commentId, String status) {
        String value = requiredStatus(status, POST_STATUSES, "评论状态不正确");
        Long postId = mapper.adminFindCommentPostId(commentId);
        if (postId == null) throw new BusinessException("评论不存在");
        if (mapper.adminUpdateCommentStatus(commentId, value) != 1) throw new BusinessException("评论不存在");
        mapper.refreshCommentCount(postId);
        refreshSnapshot(postId);
    }

    public List<ZentideInterestTopic> listTopics(String query, String status, Integer limit) {
        return mapper.adminListTopics(clean(query), normalize(status), bounded(limit, 300));
    }

    public void updateTopicStatus(Long topicId, String status) {
        String value = requiredStatus(status, Set.of("ACTIVE", "HIDDEN"), "话题状态不正确");
        if (mapper.adminUpdateTopicStatus(topicId, value) != 1) throw new BusinessException("话题不存在");
    }

    public Map<String, Integer> overview() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("hubs", mapper.countAdminHubs("ACTIVE"));
        result.put("hiddenHubs", mapper.countAdminHubs("HIDDEN"));
        result.put("posts", mapper.countAdminPosts("PUBLISHED"));
        result.put("pendingPosts", mapper.countAdminPosts("HIDDEN"));
        result.put("comments", mapper.countAdminComments("PUBLISHED"));
        result.put("hiddenComments", mapper.countAdminComments("HIDDEN"));
        result.put("pendingMembers", mapper.countAdminPendingMembers());
        return result;
    }

    public void updateHubStatus(Long hubId, String status) {
        String value = requiredStatus(status, HUB_STATUSES, "兴趣现场状态不正确");
        if (mapper.adminUpdateHubStatus(hubId, value) != 1) throw new BusinessException("兴趣现场不存在");
        List<Long> postIds = mapper.listPostIdsForSnapshotByHub(hubId);
        if ("ACTIVE".equals(value)) {
            if (postIds != null) postIds.forEach(this::refreshSnapshot);
        } else if (postIds != null) {
            postIds.forEach(postId -> deleteSnapshot(hubId, postId));
        }
    }

    public void updatePostStatus(Long postId, String status) {
        String value = requiredStatus(status, POST_STATUSES, "帖子状态不正确");
        Long hubId = mapper.adminFindPostHubId(postId);
        if (hubId == null) throw new BusinessException("帖子不存在");
        int affected = mapper.adminUpdatePostStatus(postId, value);
        log.info("管理员更新帖子状态: postId={}, status={}, affectedRows={}", postId, value, affected);
        if (affected != 1) throw new BusinessException("帖子不存在或状态未改变");
        mapper.refreshPostCount(hubId);
        List<Long> topicIds = mapper.listPostTopicIds(postId);
        if (topicIds != null) topicIds.forEach(topicId -> mapper.refreshTopicPostCount(hubId, topicId));
        if ("PUBLISHED".equals(value)) refreshSnapshot(postId); else deleteSnapshot(hubId, postId);
    }

    public void reviewMember(Long hubId, String userId, String status) {
        String value = requiredStatus(status, MEMBER_STATUSES, "成员处理状态不正确");
        if (mapper.updateMembershipStatus(hubId, userId, value) != 1) throw new BusinessException("加入申请不存在或已处理");
        mapper.refreshMemberCount(hubId);
    }

    public List<ZentideHubDirection> listDirections() {
        return mapper.listHubDirections(true);
    }

    public ZentideHubDirection createDirection(String code, String displayName, String description, Integer sortOrder) {
        ZentideHubDirection direction = new ZentideHubDirection();
        direction.setCode(normalizeDirectionCode(code));
        direction.setDisplayName(normalizeDirectionName(displayName));
        direction.setDescription(cleanDescription(description));
        direction.setStatus("ACTIVE");
        direction.setSortOrder(sortOrder == null ? 0 : Math.max(0, Math.min(9999, sortOrder)));
        direction.setCreatedBy("admin");
        try {
            mapper.insertHubDirection(direction);
        } catch (RuntimeException ex) {
            throw new BusinessException("所属方向标识已经存在");
        }
        return direction;
    }

    public void updateDirection(Long directionId, String displayName, String description, boolean active, Integer sortOrder) {
        ZentideHubDirection direction = new ZentideHubDirection();
        direction.setDirectionId(directionId);
        direction.setDisplayName(normalizeDirectionName(displayName));
        direction.setDescription(cleanDescription(description));
        direction.setStatus(active ? "ACTIVE" : "DISABLED");
        direction.setSortOrder(sortOrder == null ? 0 : Math.max(0, Math.min(9999, sortOrder)));
        if (mapper.updateHubDirection(direction) != 1) throw new BusinessException("所属方向不存在");
    }

    public void deleteDirection(Long directionId) {
        ZentideHubDirection direction = mapper.listHubDirections(true).stream()
                .filter(item -> directionId.equals(item.getDirectionId())).findFirst().orElse(null);
        if (direction == null) throw new BusinessException("所属方向不存在");
        updateDirection(directionId, direction.getDisplayName(), direction.getDescription(), false, direction.getSortOrder());
    }

    private String requiredStatus(String value, Set<String> allowed, String message) {
        String normalized = normalize(value);
        if (normalized == null || !allowed.contains(normalized)) throw new BusinessException(message);
        return normalized;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeDirectionCode(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_]{1,39}")) throw new BusinessException("方向标识需使用 2-40 位大写字母、数字或下划线");
        return normalized;
    }

    private String normalizeDirectionName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() < 1 || normalized.length() > 40) throw new BusinessException("方向名称需要是 1 到 40 个字符");
        return normalized;
    }

    private String cleanDescription(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > 160) throw new BusinessException("方向说明不能超过 160 个字符");
        return normalized.isBlank() ? null : normalized;
    }

    private int bounded(Integer value, int max) {
        return value == null ? 100 : Math.max(1, Math.min(max, value));
    }

    private void refreshSnapshot(Long postId) {
        if (knowledgeSnapshots == null) return;
        runAfterCommit(() -> knowledgeSnapshots.refreshPost(postId));
    }

    private void deleteSnapshot(Long hubId, Long postId) {
        if (knowledgeSnapshots == null) return;
        runAfterCommit(() -> knowledgeSnapshots.deletePost(hubId, postId));
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
