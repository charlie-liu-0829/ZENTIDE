package com.zentide.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.zentide.entity.po.ZentideInterestEvent;
import com.zentide.entity.po.ZentideInterestEventStats;
import com.zentide.entity.po.ZentideCommunityHighlight;
import com.zentide.entity.po.ZentideInterestComment;
import com.zentide.entity.po.ZentideInterestHub;
import com.zentide.entity.po.ZentideInterestPost;
import com.zentide.entity.po.ZentideCommunityUserProfile;
import com.zentide.entity.po.ZentideInterestEntity;
import com.zentide.entity.po.ZentideInterestTopic;
import com.zentide.entity.po.ZentideInterestChangeContext;
import com.zentide.entity.po.ZentideHubCategory;
import com.zentide.entity.po.ZentideHubInvitation;
import com.zentide.entity.po.ZentideHubMemberRequest;
import com.zentide.entity.po.ZentidePostType;
import com.zentide.entity.po.ZentideHubDirection;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideInterestMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import java.util.UUID;
import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

@Service
public class ZentideInterestCommunityService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern HANDLE = Pattern.compile("[a-z0-9_]{3,30}");
    private static final Set<String> HUB_PERMISSIONS = Set.of("REVIEW_MEMBERS", "MANAGE_EVENTS", "MANAGE_POST_TYPES", "INVITE_MEMBERS");
    private final ZentideInterestMapper mapper;
    private final ZentideKnowledgeSnapshotService knowledgeSnapshots;
    public ZentideInterestCommunityService(ZentideInterestMapper mapper) { this(mapper, null); }
    @Autowired
    public ZentideInterestCommunityService(ZentideInterestMapper mapper, ZentideKnowledgeSnapshotService knowledgeSnapshots) {
        this.mapper = mapper;
        this.knowledgeSnapshots = knowledgeSnapshots;
    }
    public List<ZentideInterestHub> hubs(String userId) { return mapper.listHubs(userId); }
    public Set<String> agentVisibilities(String userId, Long hubId, Long postId) {
        ZentideInterestHub hub = mapper.findHub(hubId);
        if (hub == null) throw new BusinessException("兴趣现场不存在");
        if (userId == null || mapper.countActiveHubMember(hubId, userId) != 1) {
            throw new BusinessException("请先加入兴趣现场再使用社区小助手");
        }
        if (postId != null) {
            ZentideInterestPost post = mapper.findPost(postId, userId);
            if (post == null || !hubId.equals(post.getHubId())) {
                throw new BusinessException("帖子不属于当前兴趣现场");
            }
        }
        Set<String> visibilities = new LinkedHashSet<>();
        visibilities.add("PUBLIC");
        if (hub.getVisibility() != null && !hub.getVisibility().isBlank()) {
            visibilities.add(hub.getVisibility().trim().toUpperCase(Locale.ROOT));
        }
        return visibilities;
    }
    public void validateReviewTopics(Long hubId, List<Long> topicIds) {
        if (topicIds == null) return;
        for (Long topicId : new LinkedHashSet<>(topicIds)) {
            if (topicId == null || mapper.findHubTopic(hubId, topicId) == null) {
                throw new BusinessException("话题不属于当前兴趣现场");
            }
        }
    }
    public List<ZentideHubDirection> hubDirections() { return mapper.listHubDirections(false); }
    public List<ZentideInterestHub> discoverHubs(String userId, String query, String category, Integer requestedLimit) {
        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        String cleanCategory = category == null || category.isBlank() ? null : category.trim().toUpperCase(Locale.ROOT);
        int limit = requestedLimit == null ? 60 : Math.max(1, Math.min(100, requestedLimit));
        return mapper.discoverHubs(userId, cleanQuery, cleanCategory, limit);
    }
    @Transactional public ZentideInterestHub createHub(String ownerId, String name, String description, String category, String coverUrl, String joinPolicy) { return createHub(ownerId,name,description,category,coverUrl,joinPolicy,null,null); }
    @Transactional public ZentideInterestHub createHub(String ownerId, String name, String description, String category, String coverUrl, String joinPolicy, String joinQuestion, String joinAnswer) {
        String cleanName=name == null ? "" : name.trim();
        if (cleanName.length()<2 || cleanName.length()>40) throw new BusinessException("兴趣现场名称需要是 2 到 40 个字符");
        String cleanDescription=description == null ? "" : description.trim();
        if (cleanDescription.length()>500) throw new BusinessException("兴趣现场简介不能超过 500 个字符");
        String normalizedCategory = category == null || category.isBlank() ? "GENERAL" : normalizeDirectionCode(category);
        if (mapper.findActiveHubDirection(normalizedCategory) == null) throw new BusinessException("所属方向不存在或已停用，请重新选择");
        String normalizedJoinPolicy = joinPolicy == null || joinPolicy.isBlank() ? "OPEN" : joinPolicy.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("OPEN","APPROVAL","QUESTION").contains(normalizedJoinPolicy)) throw new BusinessException("不支持的加入方式");
        ZentideInterestHub hub=new ZentideInterestHub();
        hub.setSlug("custom-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)); hub.setOwnerId(ownerId); hub.setName(cleanName); hub.setDescription(cleanDescription); hub.setCategory(normalizedCategory); hub.setCoverUrl(normalizeStoredImage(coverUrl)); hub.setJoinPolicy(normalizedJoinPolicy); hub.setJoinQuestion(boundedText(joinQuestion,200,"审批问题不能超过 200 个字符")); hub.setJoinAnswer(boundedText(joinAnswer,200,"审批答案不能超过 200 个字符")); if ("QUESTION".equals(normalizedJoinPolicy) && (hub.getJoinQuestion()==null || hub.getJoinAnswer()==null)) throw new BusinessException("问答审批需要设置问题和答案");
        mapper.insertHub(hub); mapper.joinHubAsOwner(hub.getHubId(), ownerId); mapper.refreshMemberCount(hub.getHubId());
        hub.setStatus("ACTIVE"); hub.setVisibility("PUBLIC"); hub.setMemberCount(1); hub.setPostCount(0); hub.setJoined(true); hub.setOwned(true); hub.setMembershipStatus("ACTIVE"); hub.setMemberRole("OWNER"); return hub;
    }
    @Transactional public boolean deleteHub(String ownerId, Long hubId) {
        requireHubOwner(ownerId, hubId);
        List<Long> postIds = mapper.listPostIdsForSnapshotByHub(hubId);
        if (mapper.deleteOwnedHub(hubId, ownerId) != 1) throw new BusinessException("兴趣现场已不存在或无法删除");
        if (postIds != null) postIds.forEach(postId -> deleteKnowledgeSnapshot(hubId, postId));
        return true;
    }
    public List<ZentideInterestEntity> entities(Long hubId, String userId, Integer requestedLimit) { if (hubId != null && mapper.findHub(hubId) == null) throw new BusinessException("兴趣圈不存在"); return mapper.listEntities(hubId, userId, requestedLimit == null ? 12 : Math.max(1, Math.min(50, requestedLimit))); }
    public List<ZentideInterestTopic> topics(Long hubId, Integer requestedLimit) { if (hubId == null || mapper.findHub(hubId) == null) throw new BusinessException("兴趣圈不存在"); return mapper.listHubTopics(hubId, requestedLimit == null ? 20 : Math.max(1, Math.min(50, requestedLimit))); }
    @Transactional public ZentideInterestTopic createHubTopic(String userId, Long hubId, String name) {
        requireActiveHubMember(userId, hubId);
        String cleanName = name == null ? "" : name.trim().replaceAll("^#+", "").trim();
        if (cleanName.length() < 2 || cleanName.length() > 40) throw new BusinessException("话题名称需要是 2 到 40 个字符");
        Long topicId = mapper.findTopicIdByName(cleanName);
        ZentideInterestTopic topic = new ZentideInterestTopic();
        topic.setCanonicalName(cleanName); topic.setTopicType("INTEREST"); topic.setHubId(hubId); topic.setPostCount(0); topic.setFeatured(false);
        if (topicId == null) { mapper.insertCommunityTopic(topic); topicId = topic.getTopicId(); } else topic.setTopicId(topicId);
        mapper.attachHubTopic(hubId, topicId);
        return topic;
    }
    public List<ZentideInterestEvent> events(Long hubId, String userId, Integer requestedLimit) { if (hubId != null && mapper.findHub(hubId) == null) throw new BusinessException("兴趣圈不存在"); return mapper.listEvents(hubId, userId, requestedLimit == null ? 12 : Math.max(1, Math.min(50, requestedLimit))); }
    public List<ZentideCommunityHighlight> todayHighlights(String userId, String metric, Integer requestedMinCount, Integer requestedLimit) {
        String normalizedMetric = metric == null ? "BOOKMARKS" : metric.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("BOOKMARKS", "COMMENTS", "VIEWS").contains(normalizedMetric)) normalizedMetric = "BOOKMARKS";
        int minCount = requestedMinCount == null ? 0 : Math.max(0, Math.min(1000000, requestedMinCount));
        int limit = requestedLimit == null ? 20 : Math.max(1, Math.min(30, requestedLimit));
        if (!mapper.hasViewCountColumn()) {
            // Older installations can still browse today's selection while V43
            // is pending. Views will become available as soon as the migration runs.
            return mapper.listTodayHighlightsLegacy(userId, normalizedMetric, minCount, limit);
        }
        return mapper.listTodayHighlights(userId, normalizedMetric, minCount, limit);
    }
    public List<ZentideInterestEvent> ownedEvents(String ownerId, Long hubId) { requireHubPermission(ownerId, hubId, "MANAGE_EVENTS"); return mapper.listOwnedEvents(hubId, ownerId); }
    @Transactional public ZentideInterestEvent createEvent(String ownerId, Long hubId, String title, String description, String startsAt, String endsAt, String venue, String sourceUrl) {
        requireHubPermission(ownerId, hubId, "MANAGE_EVENTS");
        ZentideInterestEvent event = new ZentideInterestEvent(); event.setHubId(hubId); event.setTitle(normalizeEventTitle(title)); event.setDescription(boundedText(description, 2000, "活动说明不能超过 2000 个字符")); event.setStartsAt(parseDateTime(startsAt, "开始时间格式不正确")); event.setEndsAt(parseDateTime(endsAt, "结束时间格式不正确")); if (event.getStartsAt() != null && event.getEndsAt() != null && event.getEndsAt().isBefore(event.getStartsAt())) throw new BusinessException("结束时间不能早于开始时间"); event.setVenue(boundedText(venue, 220, "活动地点不能超过 220 个字符")); event.setSourceUrl(normalizeOptionalUrl(sourceUrl)); mapper.insertInterestEvent(event); event.setStatus("UPCOMING"); event.setHubName(mapper.findHub(hubId).getName()); return event;
    }
    @Transactional public ZentideInterestEvent updateEvent(String ownerId, Long eventId, String title, String description, String startsAt, String endsAt, String venue, String sourceUrl, String status) {
        Long hubId = mapper.findEventHub(eventId); if (hubId == null) throw new BusinessException("活动不存在"); requireHubPermission(ownerId, hubId, "MANAGE_EVENTS"); ZentideInterestEvent event = new ZentideInterestEvent(); event.setEventId(eventId); event.setHubId(hubId); event.setTitle(normalizeEventTitle(title)); event.setDescription(boundedText(description, 2000, "活动说明不能超过 2000 个字符")); event.setStartsAt(parseDateTime(startsAt, "开始时间格式不正确")); event.setEndsAt(parseDateTime(endsAt, "结束时间格式不正确")); if (event.getStartsAt() != null && event.getEndsAt() != null && event.getEndsAt().isBefore(event.getStartsAt())) throw new BusinessException("结束时间不能早于开始时间"); event.setVenue(boundedText(venue, 220, "活动地点不能超过 220 个字符")); event.setSourceUrl(normalizeOptionalUrl(sourceUrl)); String normalizedStatus = status == null || status.isBlank() ? "UPCOMING" : status.trim().toUpperCase(Locale.ROOT); event.setStatus(Set.of("UPCOMING", "CANCELLED", "ENDED").contains(normalizedStatus) ? normalizedStatus : "UPCOMING"); if (mapper.updateOwnedInterestEvent(event, ownerId) != 1) throw new BusinessException("活动不存在或无权修改"); return event;
    }
    @Transactional public boolean deleteEvent(String ownerId, Long eventId) { Long hubId = mapper.findEventHub(eventId); if (hubId == null) throw new BusinessException("活动不存在"); requireHubPermission(ownerId, hubId, "MANAGE_EVENTS"); if (mapper.cancelOwnedInterestEvent(eventId, ownerId) != 1) throw new BusinessException("活动不存在或无权删除"); return true; }
    public ZentideInterestEventStats eventStats(String userId, Long eventId) { Long hubId = mapper.findEventHub(eventId); if (hubId == null) throw new BusinessException("活动不存在"); if (mapper.findHub(hubId) == null) throw new BusinessException("兴趣现场不存在"); return mapper.eventParticipationStats(eventId); }
    public List<ZentidePostType> postTypes(Long hubId) { if (hubId != null && mapper.findHub(hubId) == null) throw new BusinessException("兴趣现场不存在"); return mapper.listAvailablePostTypes(hubId); }
    public List<ZentidePostType> hubCustomPostTypes(String ownerId, Long hubId) { requireHubPermission(ownerId, hubId, "MANAGE_POST_TYPES"); return mapper.listHubCustomPostTypes(hubId); }
    @Transactional public ZentidePostType createHubPostType(String ownerId, Long hubId, String displayName, String description) {
        requireHubPermission(ownerId, hubId, "MANAGE_POST_TYPES");
        List<ZentidePostType> existing = mapper.listHubCustomPostTypes(hubId);
        String cleanName = normalizePostTypeName(displayName);
        if (existing.stream().anyMatch(type -> type.getDisplayName().equalsIgnoreCase(cleanName))) throw new BusinessException("这个现场已经有同名帖子类型");
        ZentidePostType type = buildPostType(hubId, ownerId, cleanName, description, false, existing.size() * 10 + 100);
        mapper.insertPostType(type);
        return type;
    }
    @Transactional public boolean deleteHubPostType(String ownerId, Long hubId, Long postTypeId) {
        requireHubPermission(ownerId, hubId, "MANAGE_POST_TYPES");
        if (mapper.deleteHubPostType(hubId, postTypeId) != 1) throw new BusinessException("帖子类型不存在或已经删除");
        return true;
    }
    @Transactional public ZentidePostType updateHubPostType(String ownerId, Long hubId, Long postTypeId, String displayName, String description) {
        requireHubPermission(ownerId, hubId, "MANAGE_POST_TYPES");
        String cleanName = normalizePostTypeName(displayName);
        if (mapper.updateHubPostType(hubId, postTypeId, cleanName, boundedText(description, 160, "类型说明不能超过 160 个字符")) != 1) throw new BusinessException("帖子类型不存在或已经删除");
        ZentidePostType type = new ZentidePostType(); type.setPostTypeId(postTypeId); type.setHubId(hubId); type.setDisplayName(cleanName); type.setDescription(description); type.setStatus("ACTIVE"); return type;
    }
    public List<ZentidePostType> systemPostTypes() { return mapper.listSystemPostTypes(); }
    @Transactional public ZentidePostType createSystemPostType(String adminAccount, String displayName, String description) {
        List<ZentidePostType> existing = mapper.listSystemPostTypes();
        String cleanName = normalizePostTypeName(displayName);
        if (existing.stream().anyMatch(type -> type.getDisplayName().equalsIgnoreCase(cleanName))) throw new BusinessException("已经存在同名固定帖子类型");
        ZentidePostType type = buildPostType(null, adminAccount, cleanName, description, true, existing.size() * 10 + 10);
        mapper.insertPostType(type);
        return type;
    }
    @Transactional public ZentidePostType updateSystemPostType(Long postTypeId, String displayName, String description, boolean active, Integer sortOrder) {
        String cleanName = normalizePostTypeName(displayName);
        if (mapper.listSystemPostTypes().stream().anyMatch(type -> !type.getPostTypeId().equals(postTypeId) && type.getDisplayName().equalsIgnoreCase(cleanName))) throw new BusinessException("已经存在同名固定帖子类型");
        String cleanDescription = boundedText(description, 160, "类型说明不能超过 160 个字符");
        ZentidePostType type = new ZentidePostType();
        type.setPostTypeId(postTypeId); type.setDisplayName(cleanName); type.setDescription(cleanDescription); type.setStatus(active ? "ACTIVE" : "DISABLED"); type.setSortOrder(sortOrder == null ? 0 : Math.max(0, Math.min(9999, sortOrder)));
        if (mapper.updateSystemPostType(type) != 1) throw new BusinessException("固定帖子类型不存在");
        return type;
    }
    @Transactional public boolean deleteSystemPostType(Long postTypeId) { if (mapper.deleteSystemPostType(postTypeId) != 1) throw new BusinessException("固定帖子类型不存在或已经删除"); return true; }
    public List<ZentideInterestChangeContext> changeContexts(Long changeId) { return mapper.listChangeContexts(changeId); }
    public List<ZentideInterestPost> feed(Long hubId, String type, Long topicId, Long changeId, String userId, Integer requestedLimit) {
        if (hubId != null && mapper.findHub(hubId) == null) throw new BusinessException("兴趣圈不存在");
        if (topicId != null && (hubId == null || mapper.findHubTopic(hubId, topicId) == null)) throw new BusinessException("话题不属于当前兴趣圈");
        String normalized = type == null || type.isBlank() ? null : type.trim().toUpperCase();
        if (normalized != null && mapper.countActivePostTypeCode(normalized) != 1) throw new BusinessException("帖子类型不存在或已经停用");
        int limit = requestedLimit == null ? 30 : Math.max(1, Math.min(100, requestedLimit));
        List<ZentideInterestPost> posts=mapper.listPosts(hubId, normalized, topicId, changeId, userId, limit); posts.forEach(this::enrichPostTopics); return posts;
    }
    private ZentideInterestPost loadPost(Long postId, String userId) {
        ZentideInterestPost post = mapper.findPost(postId, userId);
        if (post == null) throw new BusinessException("内容不存在");
        enrichPostTopics(post);
        return post;
    }
    public ZentideInterestPost post(Long postId, String userId) {
        ZentideInterestPost post = loadPost(postId, userId);
        try {
            if (mapper.incrementPostView(postId) == 1) post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        } catch (DataAccessException ignored) {
            // Keep existing databases usable until V43 is applied.
        }
        return post;
    }
    @Transactional public ZentideInterestPost publish(String authorId, Long hubId, String type, String title, String body, Long entityId, Long eventId, Long topicId, Long changeId, String mediaJson, String coverUrl) {
        requireActiveHubMember(authorId, hubId);
        if (eventId != null && !hubId.equals(mapper.findEventHub(eventId))) throw new BusinessException("活动不属于当前兴趣圈");
        if (topicId != null && mapper.findHubTopic(hubId, topicId) == null) throw new BusinessException("话题不属于当前兴趣圈");
        if (changeId != null && (topicId == null || mapper.countVerifiedChangeInHubTopic(hubId, topicId, changeId) != 1)) throw new BusinessException("这条变化不属于当前兴趣现场的话题，无法作为讨论依据");
        String normalizedType = normalizePostTypeForHub(hubId, type);
        String cleanBody = PostBodySanitizer.sanitize(body);
        String cleanTitle = normalizePostTitle(title);
        ZentideInterestPost post = new ZentideInterestPost(); post.setHubId(hubId); post.setAuthorId(authorId); post.setPostType(normalizedType); post.setTitle(cleanTitle); post.setBody(cleanBody); post.setEntityId(entityId); post.setEventId(eventId); post.setChangeId(changeId); post.setMediaJson(normalizeMediaJson(mediaJson)); post.setCoverUrl(normalizeStoredImage(coverUrl)); post.setStatus("PUBLISHED"); mapper.insertPost(post); mapper.refreshPostCount(hubId); if (topicId != null) { mapper.attachPostTopic(post.getPostId(), topicId); mapper.refreshTopicPostCount(hubId, topicId); } post.setAuthorLabel("社区成员"); refreshKnowledgeSnapshot(post.getPostId()); return post;
    }
    @Transactional public ZentideInterestPost publishForManualReview(String authorId, Long hubId, String type, String title, String body, Long entityId, Long eventId, Long topicId, Long changeId, String mediaJson, String coverUrl) {
        requireActiveHubMember(authorId, hubId);
        if (eventId != null && !hubId.equals(mapper.findEventHub(eventId))) throw new BusinessException("活动不属于当前兴趣圈");
        if (topicId != null && mapper.findHubTopic(hubId, topicId) == null) throw new BusinessException("话题不属于当前兴趣圈");
        if (changeId != null && (topicId == null || mapper.countVerifiedChangeInHubTopic(hubId, topicId, changeId) != 1)) throw new BusinessException("这条变化不属于当前兴趣现场的话题，无法作为讨论依据");
        String normalizedType = normalizePostTypeForHub(hubId, type);
        String cleanBody = PostBodySanitizer.sanitize(body);
        String cleanTitle = normalizePostTitle(title);
        ZentideInterestPost post = new ZentideInterestPost(); post.setHubId(hubId); post.setAuthorId(authorId); post.setPostType(normalizedType); post.setTitle(cleanTitle); post.setBody(cleanBody); post.setEntityId(entityId); post.setEventId(eventId); post.setChangeId(changeId); post.setMediaJson(normalizeMediaJson(mediaJson)); post.setCoverUrl(normalizeStoredImage(coverUrl)); post.setStatus("HIDDEN"); mapper.insertPost(post); if (topicId != null) mapper.attachPostTopic(post.getPostId(), topicId); post.setAuthorLabel("社区成员"); return post;
    }
    @Transactional public ZentideInterestPost updatePost(String authorId, Long postId, String type, String title, String body, Long eventId, Long topicId, String mediaJson, String coverUrl) {
        ZentideInterestPost existing = requireOwnedPost(authorId, postId, "只能修改自己发布的帖子");
        Long hubId = existing.getHubId();
        if (eventId != null && !hubId.equals(mapper.findEventHub(eventId))) throw new BusinessException("活动不属于当前兴趣圈");
        if (topicId != null && mapper.findHubTopic(hubId, topicId) == null) throw new BusinessException("话题不属于当前兴趣圈");
        if (existing.getChangeId() != null && (topicId == null || mapper.countVerifiedChangeInHubTopic(hubId, topicId, existing.getChangeId()) != 1)) throw new BusinessException("关联变化的帖子必须保留对应话题");
        String normalizedType = type == null || type.isBlank() ? existing.getPostType() : type.trim().toUpperCase(Locale.ROOT);
        if (!normalizedType.equals(existing.getPostType()) && mapper.countActivePostTypeForHub(hubId, normalizedType) != 1) throw new BusinessException("帖子类型不属于当前兴趣现场或已经停用");
        String cleanBody = PostBodySanitizer.sanitize(body);
        String cleanTitle = normalizePostTitle(title);

        List<Long> oldTopicIds = mapper.listPostTopicIds(postId);
        ZentideInterestPost update = new ZentideInterestPost();
        update.setPostId(postId); update.setAuthorId(authorId); update.setPostType(normalizedType); update.setTitle(cleanTitle); update.setBody(cleanBody); update.setEventId(eventId); update.setMediaJson(normalizeMediaJson(mediaJson)); update.setCoverUrl(normalizeStoredImage(coverUrl));
        if (mapper.updateOwnedPost(update) != 1) throw new BusinessException("帖子已不存在或无法修改");
        mapper.detachPostTopics(postId);
        if (topicId != null) mapper.attachPostTopic(postId, topicId);
        oldTopicIds.forEach(oldTopicId -> mapper.refreshTopicPostCount(hubId, oldTopicId));
        if (topicId != null && !oldTopicIds.contains(topicId)) mapper.refreshTopicPostCount(hubId, topicId);
        refreshKnowledgeSnapshot(postId);
        return loadPost(postId, authorId);
    }
    @Transactional public boolean deletePost(String authorId, Long postId) {
        ZentideInterestPost existing = requireOwnedPost(authorId, postId, "只能删除自己发布的帖子");
        List<Long> topicIds = mapper.listPostTopicIds(postId);
        if (mapper.deleteOwnedPost(postId, authorId) != 1) throw new BusinessException("帖子已不存在或无法删除");
        mapper.refreshPostCount(existing.getHubId());
        topicIds.forEach(topicId -> mapper.refreshTopicPostCount(existing.getHubId(), topicId));
        deleteKnowledgeSnapshot(existing.getHubId(), postId);
        return true;
    }
    public List<ZentideInterestComment> comments(Long postId, String userId, Integer requestedLimit) { loadPost(postId, null); return mapper.listComments(postId, userId, requestedLimit == null ? 100 : Math.max(1, Math.min(300, requestedLimit))); }
    @Transactional public ZentideInterestComment comment(String authorId, Long postId, Long parentId, String body) { loadPost(postId, null); if (parentId != null && !postId.equals(mapper.findCommentPostId(parentId))) throw new BusinessException("回复的评论不属于当前帖子"); String clean = body == null ? "" : body.trim(); if (clean.length() < 2 || clean.length() > 4000) throw new BusinessException("评论需要是 2 到 4000 个字符"); ZentideInterestComment c = new ZentideInterestComment(); c.setPostId(postId); c.setAuthorId(authorId); c.setParentCommentId(parentId); c.setBody(clean); mapper.insertComment(c); mapper.refreshCommentCount(postId); c.setStatus("PUBLISHED"); c.setLikeCount(0); c.setLiked(false); c.setAuthorLabel("社区成员"); refreshKnowledgeSnapshot(postId); return c; }
    @Transactional public boolean toggleLike(String userId, Long postId, boolean active) { loadPost(postId, null); if (active) mapper.react(postId, userId, "LIKE"); else mapper.unreact(postId, userId); mapper.refreshLikeCount(postId); return active; }
    @Transactional public boolean toggleBookmark(String userId, Long postId, boolean active) { loadPost(postId, null); if (active) mapper.bookmark(postId, userId); else mapper.unbookmark(postId, userId); return active; }
    @Transactional public boolean toggleCommentLike(String userId, Long commentId, boolean active) { if (mapper.findCommentPostId(commentId) == null) throw new BusinessException("评论不存在"); if (active) mapper.reactComment(commentId, userId); else mapper.unreactComment(commentId, userId); mapper.refreshCommentLikeCount(commentId); return active; }
    @Transactional public String setAction(String userId, Long postId, String action) { loadPost(postId, null); if (action == null || action.isBlank()) { mapper.clearAction(postId, userId); return null; } String normalized = action.trim().toUpperCase(); if (!Set.of("WANT", "ATTENDED", "USING", "WATCHING", "RECOMMEND", "AVOID").contains(normalized)) throw new BusinessException("不支持的兴趣状态"); mapper.action(postId, userId, normalized); return normalized; }

    private void refreshKnowledgeSnapshot(Long postId) {
        if (knowledgeSnapshots == null) return;
        runAfterCommit(() -> knowledgeSnapshots.refreshPost(postId));
    }

    private void deleteKnowledgeSnapshot(Long hubId, Long postId) {
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

    private String normalizeMediaJson(String requested) {
        if (requested == null || requested.isBlank()) return "[]";
        try {
            JsonNode root = JSON.readTree(requested);
            if (!root.isArray() || root.size() > 9) throw new BusinessException("帖子最多上传 9 张图片");
            ArrayNode clean = JSON.createArrayNode();
            for (JsonNode node : root) {
                if (!node.isTextual()) throw new BusinessException("图片列表格式不正确");
                clean.add(normalizeStoredImage(node.asText()));
            }
            return JSON.writeValueAsString(clean);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("图片列表格式不正确");
        }
    }

    private void enrichPostTopics(ZentideInterestPost post) {
        List<Long> topicIds = mapper.listPostTopicIds(post.getPostId());
        post.setTopicId(topicIds.isEmpty() ? null : topicIds.get(0));
        post.setTopicNames(String.join(" · ", mapper.listPostTopicNames(post.getPostId())));
    }

    private ZentideInterestPost requireOwnedPost(String authorId, Long postId, String message) {
        ZentideInterestPost existing = mapper.findPost(postId, authorId);
        if (existing == null) throw new BusinessException("内容不存在");
        if (authorId == null || !authorId.equals(existing.getAuthorId())) throw new BusinessException(message);
        return existing;
    }

    private String normalizeStoredImage(String requested) {
        if (requested == null || requested.isBlank()) return null;
        String value = requested.trim();
        // 图片必须来自站内上传目录；不接受外链、data URL 或可穿越路径。
        if (value.length() > 2048 || value.startsWith("/") || value.contains("..") || value.contains("\\") || !value.matches("\\d{6}/[A-Za-z0-9_-]{8,80}\\.[A-Za-z0-9]{2,8}")) throw new BusinessException("图片地址不正确，请重新上传");
        return value;
    }

    public ZentideCommunityUserProfile userProfile(String userId, String viewerId) { ZentideCommunityUserProfile profile = mapper.findUserProfile(userId, viewerId); if (profile == null) throw new BusinessException("用户不存在"); return profile; }
    public List<ZentideInterestPost> userPosts(String userId, Integer requestedLimit) { userProfile(userId, null); return mapper.listUserPosts(userId, requestedLimit == null ? 50 : Math.max(1, Math.min(100, requestedLimit))); }
    public List<ZentideInterestComment> userComments(String userId, Integer requestedLimit) { userProfile(userId, null); return mapper.listUserComments(userId, requestedLimit == null ? 50 : Math.max(1, Math.min(100, requestedLimit))); }
    public List<ZentideInterestPost> bookmarkedPosts(String userId, Integer requestedLimit) { if (userId == null) throw new BusinessException("请先登录"); return mapper.listBookmarkedPosts(userId, requestedLimit == null ? 100 : Math.max(1, Math.min(200, requestedLimit))); }
    @Transactional public ZentideCommunityUserProfile updateUserProfile(String userId, String nickname, String handle, String avatar, String coverUrl, String bio, String location, String websiteUrl) {
        String cleanNickname = nickname == null ? "" : nickname.trim();
        if (cleanNickname.length() < 2 || cleanNickname.length() > 20) throw new BusinessException("显示名需要是 2 到 20 个字符");
        if (mapper.countOtherNickname(userId, cleanNickname) > 0) throw new BusinessException("这个显示名已经被使用");
        String cleanHandle = handle == null ? "" : handle.trim().toLowerCase(Locale.ROOT).replaceFirst("^@", "");
        if (!HANDLE.matcher(cleanHandle).matches()) throw new BusinessException("用户名需要是 3 到 30 位小写字母、数字或下划线");
        if (mapper.countOtherHandle(userId, cleanHandle) > 0) throw new BusinessException("这个用户名已经被使用");
        String cleanBio = boundedText(bio, 500, "个人简介不能超过 500 个字符");
        String cleanLocation = boundedText(location, 80, "所在地不能超过 80 个字符");
        String cleanWebsite = normalizeWebsite(websiteUrl);
        String cleanAvatar = normalizeStoredImage(avatar);
        String cleanCover = normalizeStoredImage(coverUrl);
        mapper.updateUserIdentity(userId, cleanNickname, cleanAvatar);
        ZentideCommunityUserProfile profile = new ZentideCommunityUserProfile();
        profile.setUserId(userId); profile.setNickName(cleanNickname); profile.setHandle(cleanHandle); profile.setAvatar(cleanAvatar); profile.setCoverUrl(cleanCover); profile.setBio(cleanBio); profile.setLocation(cleanLocation); profile.setWebsiteUrl(cleanWebsite);
        mapper.upsertUserProfile(profile);
        return userProfile(userId, userId);
    }
    @Transactional public boolean toggleFollow(String followerId, String followingId, boolean active) { if (followerId == null || followerId.equals(followingId)) throw new BusinessException("不能关注自己"); userProfile(followingId, followerId); if (active) mapper.follow(followerId, followingId); else mapper.unfollow(followerId, followingId); return active; }
    @Transactional public String setAttendance(String userId, Long eventId, String status) { if (mapper.findEvent(eventId) == null) throw new BusinessException("活动不存在或已结束"); if (status == null || status.isBlank()) { mapper.clearAttendance(eventId, userId); return null; } String normalized = status.trim().toUpperCase(); if (!Set.of("WANT", "ATTENDING", "ATTENDED").contains(normalized)) throw new BusinessException("不支持的参与状态"); mapper.setAttendance(eventId, userId, normalized); return normalized; }
    @Transactional public String toggleHubMembership(String userId, Long hubId, boolean active) { return toggleHubMembership(userId, hubId, active, null, null); }
    @Transactional public String toggleHubMembership(String userId, Long hubId, boolean active, String answer, String applicationNote) {
        ZentideInterestHub hub = mapper.findHub(hubId);
        if (hub == null) throw new BusinessException("兴趣现场不存在");
        if (!active) {
            if (userId.equals(hub.getOwnerId())) throw new BusinessException("创建者不能退出自己的兴趣现场");
            mapper.leaveHub(hubId, userId);
            mapper.refreshMemberCount(hubId);
            return "NONE";
        }
        if ("QUESTION".equalsIgnoreCase(hub.getJoinPolicy())) {
            if (answer == null || hub.getJoinAnswer() == null || !hub.getJoinAnswer().trim().equalsIgnoreCase(answer.trim())) throw new BusinessException("回答不正确，请重新输入");
            mapper.requestHubMembershipWithNote(hubId, userId, "ACTIVE", "问答加入");
            mapper.refreshMemberCount(hubId);
            return "ACTIVE";
        }
        String status = "APPROVAL".equalsIgnoreCase(hub.getJoinPolicy()) ? "PENDING" : "ACTIVE";
        if ("PENDING".equals(status)) { if (applicationNote == null || applicationNote.isBlank()) mapper.requestHubMembership(hubId, userId, status); else mapper.requestHubMembershipWithNote(hubId, userId, status, boundedText(applicationNote, 1000, "申请说明不能超过 1000 个字符")); } else mapper.requestHubMembership(hubId, userId, status);
        mapper.refreshMemberCount(hubId);
        return status;
    }
    public List<ZentideHubMemberRequest> pendingHubMembers(String ownerId, Long hubId) { requireHubPermission(ownerId, hubId, "REVIEW_MEMBERS"); return mapper.listPendingHubMembers(hubId); }
    public List<ZentideHubMemberRequest> hubMembers(String operatorId, Long hubId) { requireHubManager(operatorId, hubId); return mapper.listHubMembers(hubId); }
    @Transactional public ZentideHubMemberRequest updateHubMemberAdmin(String ownerId, Long hubId, String memberId, String role, String permissionsJson) {
        requireHubOwner(ownerId, hubId);
        ZentideHubMemberRequest target = mapper.findHubMember(hubId, memberId);
        if (target == null || !"ACTIVE".equalsIgnoreCase(target.getMembershipStatus())) throw new BusinessException("成员不存在或尚未加入");
        if ("OWNER".equalsIgnoreCase(target.getRole()) || ownerId.equals(memberId)) throw new BusinessException("创建者不能被修改管理员身份");
        String normalizedRole = role == null ? "MEMBER" : role.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ADMIN", "MEMBER").contains(normalizedRole)) throw new BusinessException("不支持的成员角色");
        String normalizedPermissions = normalizePermissions(permissionsJson, normalizedRole);
        if (mapper.updateHubMemberRole(hubId, memberId, normalizedRole, normalizedPermissions) != 1) throw new BusinessException("成员身份更新失败");
        target.setRole(normalizedRole); target.setPermissionsJson(normalizedPermissions); return target;
    }
    @Transactional public boolean removeHubMember(String ownerId, Long hubId, String memberId) {
        requireHubOwner(ownerId, hubId);
        if (memberId == null || memberId.isBlank()) throw new BusinessException("成员不存在");
        ZentideHubMemberRequest target = mapper.findHubMember(hubId, memberId);
        if (target == null || !"ACTIVE".equalsIgnoreCase(target.getMembershipStatus())) {
            throw new BusinessException("成员不存在或尚未加入");
        }
        if ("OWNER".equalsIgnoreCase(target.getRole()) || ownerId.equals(memberId)) {
            throw new BusinessException("创建者不能被移除");
        }
        if (mapper.removeHubMember(hubId, memberId) != 1) throw new BusinessException("成员不存在或无法移除");
        mapper.refreshMemberCount(hubId);
        return true;
    }
    @Transactional public boolean reviewHubMember(String ownerId, Long hubId, String memberId, boolean approve) {
        requireHubPermission(ownerId, hubId, "REVIEW_MEMBERS");
        if (mapper.updateMembershipStatus(hubId, memberId, approve ? "ACTIVE" : "REJECTED") != 1) throw new BusinessException("这条加入申请已处理或不存在");
        mapper.refreshMemberCount(hubId);
        return approve;
    }
    @Transactional public ZentideHubInvitation createHubInvitation(String ownerId, Long hubId) {
        requireHubPermission(ownerId, hubId, "INVITE_MEMBERS");
        ZentideHubInvitation invitation = new ZentideHubInvitation();
        invitation.setHubId(hubId);
        invitation.setCreatedBy(ownerId);
        invitation.setInviteToken(UUID.randomUUID().toString().replace("-", ""));
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitation.setStatus("ACTIVE");
        invitation.setUseCount(0);
        mapper.insertHubInvitation(invitation);
        return invitation;
    }
    public ZentideHubInvitation invitation(String token) {
        String cleanToken = token == null ? "" : token.trim();
        if (!cleanToken.matches("[A-Za-z0-9_-]{20,64}")) throw new BusinessException("邀请链接无效");
        ZentideHubInvitation invitation = mapper.findValidInvitation(cleanToken);
        if (invitation == null) throw new BusinessException("邀请链接已失效或已过期");
        return invitation;
    }
    @Transactional public ZentideInterestHub redeemHubInvitation(String userId, String token) {
        ZentideHubInvitation invitation = invitation(token);
        mapper.requestHubMembership(invitation.getHubId(), userId, "ACTIVE");
        if (mapper.consumeInvitation(invitation.getInvitationId()) != 1) throw new BusinessException("邀请链接已失效，请重新获取");
        mapper.refreshMemberCount(invitation.getHubId());
        return mapper.discoverHubs(userId, null, null, 100).stream().filter(h -> invitation.getHubId().equals(h.getHubId())).findFirst().orElseGet(() -> mapper.findHub(invitation.getHubId()));
    }
    public List<ZentideHubCategory> hubCategories(String userId) { return mapper.listHubCategories(userId); }
    @Transactional public ZentideHubCategory createHubCategory(String userId, String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.length() < 1 || cleanName.length() > 20) throw new BusinessException("分类名称需要是 1 到 20 个字符");
        ZentideHubCategory category = new ZentideHubCategory();
        category.setUserId(userId); category.setName(cleanName); category.setSortOrder(mapper.listHubCategories(userId).size()); category.setHubCount(0);
        try { mapper.insertHubCategory(category); } catch (RuntimeException exception) { throw new BusinessException("这个分类已经存在"); }
        return category;
    }
    @Transactional public boolean deleteHubCategory(String userId, Long categoryId) {
        if (mapper.deleteHubCategory(categoryId, userId) != 1) throw new BusinessException("分类不存在");
        return true;
    }
    @Transactional public Long assignHubCategory(String userId, Long hubId, Long categoryId) {
        requireActiveHubMember(userId, hubId);
        if (categoryId != null && mapper.findUserHubCategory(categoryId, userId) == null) throw new BusinessException("分类不存在");
        if (mapper.assignHubCategory(hubId, userId, categoryId) != 1) throw new BusinessException("无法更新兴趣现场分类");
        return categoryId;
    }
    @Transactional public boolean toggleEntityFollow(String userId, Long entityId, boolean active) { if (mapper.findEntity(entityId) == null) throw new BusinessException("兴趣对象不存在"); if (active) mapper.followEntity(entityId, userId); else mapper.unfollowEntity(entityId, userId); return active; }
    @Transactional public String setEntityAction(String userId, Long entityId, String action) { if (mapper.findEntity(entityId) == null) throw new BusinessException("兴趣对象不存在"); if (action == null || action.isBlank()) { mapper.clearEntityAction(entityId, userId); return null; } String normalized=action.trim().toUpperCase(); if (!Set.of("WANT", "ATTENDED", "USING", "WATCHING", "RECOMMEND", "AVOID").contains(normalized)) throw new BusinessException("不支持的兴趣状态"); mapper.actionEntity(entityId, userId, normalized); return normalized; }
    private void requireHubOwner(String userId, Long hubId) { if (userId == null || mapper.countOwnedHub(hubId, userId) != 1) throw new BusinessException("只有兴趣现场创建者可以执行这个操作"); }
    private void requireHubManager(String userId, Long hubId) {
        if (userId != null && mapper.countOwnedHub(hubId, userId) == 1) return;
        ZentideHubMemberRequest member = userId == null ? null : mapper.findHubMember(hubId, userId);
        if (member == null || !"ACTIVE".equalsIgnoreCase(member.getMembershipStatus()) || !("OWNER".equalsIgnoreCase(member.getRole()) || "ADMIN".equalsIgnoreCase(member.getRole()))) throw new BusinessException("只有创建者或现场管理员可以执行这个操作");
    }
    private void requireHubPermission(String userId, Long hubId, String permission) {
        if (userId != null && mapper.countOwnedHub(hubId, userId) == 1) return;
        ZentideHubMemberRequest member = userId == null ? null : mapper.findHubMember(hubId, userId);
        if (member == null || !"ACTIVE".equalsIgnoreCase(member.getMembershipStatus())) throw new BusinessException("请先加入兴趣现场");
        if ("OWNER".equalsIgnoreCase(member.getRole())) return;
        if (!"ADMIN".equalsIgnoreCase(member.getRole()) || !hasPermission(member.getPermissionsJson(), permission)) throw new BusinessException("你没有执行此操作的权限");
    }
    private boolean hasPermission(String permissionsJson, String permission) {
        if (permissionsJson == null || permissionsJson.isBlank()) return false;
        try { JsonNode node = JSON.readTree(permissionsJson); if (!node.isArray()) return false; for (JsonNode item : node) if (permission.equalsIgnoreCase(item.asText())) return true; } catch (Exception ignored) { }
        return false;
    }
    private String normalizePermissions(String permissionsJson, String role) {
        if (!"ADMIN".equals(role)) return "[]";
        try {
            JsonNode node = permissionsJson == null || permissionsJson.isBlank() ? JSON.createArrayNode() : JSON.readTree(permissionsJson);
            if (!node.isArray()) throw new BusinessException("权限格式不正确");
            ArrayNode result = JSON.createArrayNode();
            java.util.Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) { String value = iterator.next().asText("").trim().toUpperCase(Locale.ROOT); if (!value.isBlank() && HUB_PERMISSIONS.contains(value) && !hasPermission(result.toString(), value)) result.add(value); }
            return JSON.writeValueAsString(result);
        } catch (BusinessException ex) { throw ex; } catch (Exception ex) { throw new BusinessException("权限格式不正确"); }
    }
    private void requireActiveHubMember(String userId, Long hubId) { if (mapper.findHub(hubId) == null) throw new BusinessException("兴趣现场不存在"); if (userId == null || mapper.countActiveHubMember(hubId, userId) != 1) throw new BusinessException("请先加入兴趣现场再参与讨论"); }
    private String normalizePostTypeForHub(Long hubId, String requested) {
        String code = requested == null || requested.isBlank() ? mapper.listAvailablePostTypes(hubId).stream().findFirst().map(ZentidePostType::getTypeCode).orElse(null) : requested.trim().toUpperCase(Locale.ROOT);
        if (code == null || mapper.countActivePostTypeForHub(hubId, code) != 1) throw new BusinessException("帖子类型不属于当前兴趣现场或已经停用");
        return code;
    }
    private ZentidePostType buildPostType(Long hubId, String createdBy, String displayName, String description, boolean systemFixed, int sortOrder) {
        ZentidePostType type = new ZentidePostType();
        type.setHubId(hubId); type.setTypeCode((systemFixed ? "SYS_" : "HUB_") + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT)); type.setDisplayName(normalizePostTypeName(displayName)); type.setDescription(boundedText(description, 160, "类型说明不能超过 160 个字符")); type.setSystemFixed(systemFixed); type.setStatus("ACTIVE"); type.setSortOrder(sortOrder); type.setCreatedBy(createdBy);
        return type;
    }
    private String normalizePostTypeName(String requested) { String value = requested == null ? "" : requested.trim(); if (value.length() < 1 || value.length() > 20) throw new BusinessException("类型名称需要是 1 到 20 个字符"); return value; }
    private String normalizePostTitle(String requested) { String value = requested == null ? "" : requested.trim(); if (value.isBlank()) throw new BusinessException("请填写标题"); if (value.length() > 220) throw new BusinessException("标题不能超过 220 个字符"); return value; }
    private String normalizeDirectionCode(String requested) {
        String value = requested == null ? "" : requested.trim().toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z][A-Z0-9_]{1,39}")) throw new BusinessException("所属方向标识格式不正确");
        return value;
    }
    private String boundedText(String requested, int max, String message) { String value = requested == null ? "" : requested.trim(); if (value.length() > max) throw new BusinessException(message); return value.isBlank() ? null : value; }
    private String normalizeWebsite(String requested) { if (requested == null || requested.isBlank()) return null; String value=requested.trim(); if (value.length()>2048) throw new BusinessException("个人链接过长"); try { URI uri=URI.create(value); if (!Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null) throw new BusinessException("个人链接需要是有效的 http 或 https 地址"); return value; } catch (IllegalArgumentException exception) { throw new BusinessException("个人链接格式不正确"); } }
    private String normalizeEventTitle(String requested) { String value = requested == null ? "" : requested.trim(); if (value.length() < 2 || value.length() > 220) throw new BusinessException("活动标题需要是 2 到 220 个字符"); return value; }
    private LocalDateTime parseDateTime(String requested, String message) { if (requested == null || requested.isBlank()) return null; try { return LocalDateTime.parse(requested.trim().replace(' ', 'T')); } catch (Exception e) { throw new BusinessException(message); } }
    private String normalizeOptionalUrl(String requested) { if (requested == null || requested.isBlank()) return null; String value = requested.trim(); try { URI uri = URI.create(value); if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null || value.length() > 2048) throw new BusinessException("活动链接格式不正确"); return value; } catch (IllegalArgumentException e) { throw new BusinessException("活动链接格式不正确"); } }
}
