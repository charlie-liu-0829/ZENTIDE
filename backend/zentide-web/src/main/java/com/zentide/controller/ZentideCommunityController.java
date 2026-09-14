package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideInterestCommunityService;
import com.zentide.service.ZentideSearchService;
import com.zentide.service.ZentidePostReviewGatewayService;
import com.zentide.service.ZentideGovernanceGatewayService;
import com.zentide.service.PostReviewPublishPolicy;
import com.zentide.exception.BusinessException;
import com.zentide.component.RedisComponent;
import com.zentide.entity.dto.TokenUserInfoDTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/zentide/v1/community")
@Validated
public class ZentideCommunityController extends ABaseController {
    private static final Logger LOG = LoggerFactory.getLogger(ZentideCommunityController.class);
    private final ZentideInterestCommunityService service;
    private final RedisComponent redisComponent;
    private final ZentideSearchService searchService;
    private final ZentidePostReviewGatewayService reviewGateway;
    private final ZentideGovernanceGatewayService governanceGateway;
    public ZentideCommunityController(ZentideInterestCommunityService service, RedisComponent redisComponent,
                                      ZentideSearchService searchService, ZentidePostReviewGatewayService reviewGateway, ZentideGovernanceGatewayService governanceGateway) {
        this.service = service; this.redisComponent = redisComponent; this.searchService = searchService;
        this.reviewGateway = reviewGateway; this.governanceGateway = governanceGateway;
    }

    @PostMapping("/search")
    public ResponseVO<?> search(@RequestParam @Size(min = 1, max = 100) String query, @RequestParam(defaultValue = "post") String type,
                                @RequestParam(required = false) @Min(1) @Max(50) Integer limit) {
        return getSuccessResponseVO(searchService.search(currentUserId(), type, query, limit == null ? 20 : limit));
    }

    @PostMapping("/search/history")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> searchHistory() { return getSuccessResponseVO(searchService.history(currentUserId())); }

    @PostMapping("/search/history/clear")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> clearSearchHistory() { searchService.clearHistory(currentUserId()); return getSuccessResponseVO(true); }

    @PostMapping("/hubs")
    public ResponseVO<?> hubs() { return getSuccessResponseVO(service.hubs(currentUserId())); }

    @PostMapping("/hub-directions")
    public ResponseVO<?> hubDirections() { return getSuccessResponseVO(service.hubDirections()); }

    @PostMapping("/hubs/discover")
    public ResponseVO<?> discoverHubs(@RequestParam(required = false) String query, @RequestParam(required = false) String category, @RequestParam(required = false) @Min(1) @Max(100) Integer limit) { return getSuccessResponseVO(service.discoverHubs(currentUserId(), query, category, limit)); }

    @PostMapping("/hubs/create")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> createHub(@RequestParam String name, @RequestParam(required = false) String description, @RequestParam(required = false) String category, @RequestParam(required = false) String coverUrl, @RequestParam(required = false) String joinPolicy, @RequestParam(required = false) String joinQuestion, @RequestParam(required = false) String joinAnswer) { return getSuccessResponseVO(service.createHub(currentUserId(), name, description, category, coverUrl, joinPolicy, joinQuestion, joinAnswer)); }

    @PostMapping("/hubs/{hubId}/delete")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> deleteHub(@PathVariable @Positive Long hubId) { return getSuccessResponseVO(service.deleteHub(currentUserId(), hubId)); }

    @PostMapping("/hubs/{hubId}/membership")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> membership(@PathVariable @Positive Long hubId, @RequestParam(defaultValue = "true") boolean active, @RequestParam(required = false) String answer, @RequestParam(required = false) String applicationNote) { return getSuccessResponseVO(service.toggleHubMembership(currentUserId(), hubId, active, answer, applicationNote)); }

    @PostMapping("/hubs/{hubId}/members/pending")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> pendingMembers(@PathVariable @Positive Long hubId) { return getSuccessResponseVO(service.pendingHubMembers(currentUserId(), hubId)); }

    @PostMapping("/hubs/{hubId}/members")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> members(@PathVariable @Positive Long hubId) { return getSuccessResponseVO(service.hubMembers(currentUserId(), hubId)); }

    @PostMapping("/hubs/{hubId}/members/{memberId}/admin")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> updateMemberAdmin(@PathVariable @Positive Long hubId, @PathVariable String memberId,
                                           @RequestParam(defaultValue = "MEMBER") String role,
                                           @RequestParam(required = false) String permissionsJson) {
        return getSuccessResponseVO(service.updateHubMemberAdmin(currentUserId(), hubId, memberId, role, permissionsJson));
    }

    @PostMapping("/hubs/{hubId}/members/{memberId}/remove")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> removeMember(@PathVariable @Positive Long hubId, @PathVariable String memberId) {
        return getSuccessResponseVO(service.removeHubMember(currentUserId(), hubId, memberId));
    }

    @PostMapping("/hubs/{hubId}/members/{memberId}/review")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> reviewMember(@PathVariable @Positive Long hubId, @PathVariable String memberId, @RequestParam boolean approve) { return getSuccessResponseVO(service.reviewHubMember(currentUserId(), hubId, memberId, approve)); }

    @PostMapping("/hubs/{hubId}/invitations")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> createInvitation(@PathVariable @Positive Long hubId) { return getSuccessResponseVO(service.createHubInvitation(currentUserId(), hubId)); }

    @PostMapping("/invitations/{token}/redeem")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> redeemInvitation(@PathVariable String token) { return getSuccessResponseVO(service.redeemHubInvitation(currentUserId(), token)); }

    @PostMapping("/invitations/{token}")
    public ResponseVO<?> invitation(@PathVariable String token) { return getSuccessResponseVO(service.invitation(token)); }

    @PostMapping("/hub-categories")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> hubCategories() { return getSuccessResponseVO(service.hubCategories(currentUserId())); }

    @PostMapping("/hub-categories/create")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> createHubCategory(@RequestParam String name) { return getSuccessResponseVO(service.createHubCategory(currentUserId(), name)); }

    @PostMapping("/hub-categories/{categoryId}/delete")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> deleteHubCategory(@PathVariable @Positive Long categoryId) { return getSuccessResponseVO(service.deleteHubCategory(currentUserId(), categoryId)); }

    @PostMapping("/hubs/{hubId}/category")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> assignHubCategory(@PathVariable @Positive Long hubId, @RequestParam(required = false) Long categoryId) { return getSuccessResponseVO(service.assignHubCategory(currentUserId(), hubId, categoryId)); }

    @PostMapping("/entities")
    public ResponseVO<?> entities(@RequestParam(required = false) Long hubId, @RequestParam(required = false) @Min(1) @Max(50) Integer limit) { return getSuccessResponseVO(service.entities(hubId, currentUserId(), limit)); }

    /** Reserved contract for the future Agent-generated "everyone is watching" summary. */
    @PostMapping("/hubs/{hubId}/attention-summary")
    public ResponseVO<?> attentionSummary(@PathVariable @Positive Long hubId) {
        service.events(hubId, currentUserId(), 1); // validates that the target hub is still active
        return getSuccessResponseVO(java.util.Map.of("status", "PLANNED", "items", java.util.List.of()));
    }

    @PostMapping("/topics")
    public ResponseVO<?> topics(@RequestParam Long hubId, @RequestParam(required = false) @Min(1) @Max(50) Integer limit) { return getSuccessResponseVO(service.topics(hubId, limit)); }

    @PostMapping("/hubs/{hubId}/topics/create")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> createTopic(@PathVariable @Positive Long hubId, @RequestParam String name) { return getSuccessResponseVO(service.createHubTopic(currentUserId(), hubId, name)); }

    @PostMapping("/post-types")
    public ResponseVO<?> postTypes(@RequestParam(required = false) Long hubId) { return getSuccessResponseVO(service.postTypes(hubId)); }

    @PostMapping("/hubs/{hubId}/post-types")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> hubPostTypes(@PathVariable @Positive Long hubId) { return getSuccessResponseVO(service.hubCustomPostTypes(currentUserId(), hubId)); }

    @PostMapping("/hubs/{hubId}/post-types/create")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> createHubPostType(@PathVariable @Positive Long hubId, @RequestParam String displayName, @RequestParam(required = false) String description) { return getSuccessResponseVO(service.createHubPostType(currentUserId(), hubId, displayName, description)); }

    @PostMapping("/hubs/{hubId}/post-types/{postTypeId}/delete")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> deleteHubPostType(@PathVariable @Positive Long hubId, @PathVariable @Positive Long postTypeId) { return getSuccessResponseVO(service.deleteHubPostType(currentUserId(), hubId, postTypeId)); }

    @PostMapping("/hubs/{hubId}/post-types/{postTypeId}/update")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> updateHubPostType(@PathVariable @Positive Long hubId, @PathVariable @Positive Long postTypeId, @RequestParam String displayName, @RequestParam(required = false) String description) { return getSuccessResponseVO(service.updateHubPostType(currentUserId(), hubId, postTypeId, displayName, description)); }

    @PostMapping("/changes/{changeId}/context")
    public ResponseVO<?> changeContexts(@PathVariable @Positive Long changeId) { return getSuccessResponseVO(service.changeContexts(changeId)); }

    @PostMapping("/entities/{entityId}/follow")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> followEntity(@PathVariable @Positive Long entityId, @RequestParam(defaultValue = "true") boolean active) { return getSuccessResponseVO(service.toggleEntityFollow(currentUserId(), entityId, active)); }

    @PostMapping("/entities/{entityId}/action")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> entityAction(@PathVariable @Positive Long entityId, @RequestParam(required = false) String action) { return getSuccessResponseVO(service.setEntityAction(currentUserId(), entityId, action)); }

    @PostMapping("/feed")
    public ResponseVO<?> feed(@RequestParam(required = false) Long hubId, @RequestParam(required = false) String type, @RequestParam(required = false) Long topicId, @RequestParam(required = false) Long changeId, @RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return getSuccessResponseVO(service.feed(hubId, type, topicId, changeId, currentUserId(), limit));
    }

    @PostMapping("/events")
    public ResponseVO<?> events(@RequestParam(required = false) Long hubId, @RequestParam(required = false) @Min(1) @Max(50) Integer limit) {
        return getSuccessResponseVO(service.events(hubId, currentUserId(), limit));
    }

    @PostMapping("/highlights/today")
    public ResponseVO<?> todayHighlights(@RequestParam(required = false) String metric,
                                         @RequestParam(required = false) @Min(0) Integer minCount,
                                         @RequestParam(required = false) @Min(1) @Max(30) Integer limit) {
        return getSuccessResponseVO(service.todayHighlights(currentUserId(), metric, minCount, limit));
    }

    @PostMapping("/hubs/{hubId}/events/managed")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> managedEvents(@PathVariable @Positive Long hubId) {
        return getSuccessResponseVO(service.ownedEvents(currentUserId(), hubId));
    }

    @PostMapping("/hubs/{hubId}/events")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> createEvent(@PathVariable @Positive Long hubId,
                                     @RequestParam String title,
                                     @RequestParam(required = false) String description,
                                     @RequestParam(required = false) String startsAt,
                                     @RequestParam(required = false) String endsAt,
                                     @RequestParam(required = false) String venue,
                                     @RequestParam(required = false) String sourceUrl) {
        return getSuccessResponseVO(service.createEvent(currentUserId(), hubId, title, description, startsAt, endsAt, venue, sourceUrl));
    }

    @PostMapping("/events/{eventId}/update")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> updateEvent(@PathVariable @Positive Long eventId,
                                     @RequestParam String title,
                                     @RequestParam(required = false) String description,
                                     @RequestParam(required = false) String startsAt,
                                     @RequestParam(required = false) String endsAt,
                                     @RequestParam(required = false) String venue,
                                     @RequestParam(required = false) String sourceUrl,
                                     @RequestParam(required = false) String status) {
        return getSuccessResponseVO(service.updateEvent(currentUserId(), eventId, title, description, startsAt, endsAt, venue, sourceUrl, status));
    }

    @PostMapping("/events/{eventId}/delete")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> deleteEvent(@PathVariable @Positive Long eventId) {
        return getSuccessResponseVO(service.deleteEvent(currentUserId(), eventId));
    }

    @PostMapping("/events/{eventId}/stats")
    public ResponseVO<?> eventStats(@PathVariable @Positive Long eventId) {
        return getSuccessResponseVO(service.eventStats(currentUserId(), eventId));
    }

    @PostMapping("/posts/{postId}")
    public ResponseVO<?> post(@PathVariable @Positive Long postId) { return getSuccessResponseVO(service.post(postId, currentUserId())); }

    @PostMapping("/posts")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> publish(@RequestParam @Positive Long hubId, @RequestParam(required = false) String type, @RequestParam @NotBlank @Size(max = 220) String title, @RequestParam String body, @RequestParam(required = false) Long entityId, @RequestParam(required = false) Long eventId, @RequestParam(required = false) Long topicId, @RequestParam(required = false) Long changeId, @RequestParam(required = false) String mediaJson, @RequestParam(required = false) String coverUrl,
                                      @RequestParam String reviewId, @RequestParam(defaultValue = "false") boolean forcePublish) {
        String userId = currentUserId();
        java.util.Map<String,Object> governance = governanceGateway.review(java.util.Map.of("content_id", reviewContentId(userId, hubId, title, body), "scene_id", hubId,
                "title", title, "content", body == null ? "" : body, "rules", java.util.List.of(), "cases", java.util.List.of()));
        if ("high".equals(String.valueOf(governance.get("risk_level"))) || "temporary_hide".equals(String.valueOf(governance.get("suggested_action")))) {
            throw new BusinessException("帖子包含高风险内容，已禁止发布，请根据治理提示修改后重试");
        }
        java.util.List<Long> topicIds = topicId == null ? java.util.List.of() : java.util.List.of(topicId);
        java.util.Set<String> visibilities = service.agentVisibilities(userId, hubId, null);
        service.validateReviewTopics(hubId, topicIds);
        java.util.Map<String, Object> review = reviewGateway.validate(reviewId, userId, hubId, title, body, type, topicIds, visibilities);
        if (!Boolean.TRUE.equals(review.get("valid"))) throw new BusinessException(String.valueOf(review.get("reason")));
        PostReviewPublishPolicy.enforce(review, forcePublish);
        String advice = String.valueOf(review.getOrDefault("advice", "revise"));
        if ("manual_review".equals(advice)) {
            LOG.info("event=manual_review_triggered userId={} sceneId={} reviewId={}", userId, hubId, reviewId);
            return getSuccessResponseVO(service.publishForManualReview(userId, hubId, type, title, body, entityId, eventId, topicId, changeId, mediaJson, coverUrl));
        }
        if (forcePublish) LOG.info("event=force_publish userId={} sceneId={} reviewId={} advice={}", userId, hubId, reviewId, advice);
        LOG.info("event=publish_after_review userId={} sceneId={} reviewId={} finalPostType={} suggestedPostType={} duplicateProbability={}",
                userId, hubId, reviewId, type, review.get("suggested_post_type"), review.get("duplicate_probability"));
        return getSuccessResponseVO(service.publish(userId, hubId, type, title, body, entityId, eventId, topicId, changeId, mediaJson, coverUrl));
    }

    private long reviewContentId(String userId, Long hubId, String title, String body) {
        try {
            String source = userId + "|" + hubId + "|" + (title == null ? "" : title) + "|" + (body == null ? "" : body);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            long value = 0L;
            for (int i = 0; i < 8; i++) value = (value << 8) | (digest[i] & 0xffL);
            return value == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(value) + 1;
        } catch (Exception e) {
            return Math.abs((userId + hubId + title + body).hashCode()) + 1L;
        }
    }

    @PostMapping("/posts/{postId}/update")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> updatePost(@PathVariable @Positive Long postId, @RequestParam(required = false) String type, @RequestParam @NotBlank @Size(max = 220) String title, @RequestParam String body, @RequestParam(required = false) Long eventId, @RequestParam(required = false) Long topicId, @RequestParam(required = false) String mediaJson, @RequestParam(required = false) String coverUrl) {
        return getSuccessResponseVO(service.updatePost(currentUserId(), postId, type, title, body, eventId, topicId, mediaJson, coverUrl));
    }

    @PostMapping("/posts/{postId}/delete")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> deletePost(@PathVariable @Positive Long postId) {
        return getSuccessResponseVO(service.deletePost(currentUserId(), postId));
    }

    @PostMapping("/posts/{postId}/comments")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> comment(@PathVariable @Positive Long postId, @RequestParam(required = false) Long parentCommentId, @RequestParam String body) {
        return getSuccessResponseVO(service.comment(currentUserId(), postId, parentCommentId, body));
    }

    @PostMapping("/posts/{postId}/comments/list")
    public ResponseVO<?> comments(@PathVariable @Positive Long postId, @RequestParam(required = false) @Min(1) @Max(300) Integer limit) { return getSuccessResponseVO(service.comments(postId, currentUserId(), limit)); }

    @PostMapping("/comments/{commentId}/like")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> likeComment(@PathVariable @Positive Long commentId, @RequestParam(defaultValue = "true") boolean active) { return getSuccessResponseVO(service.toggleCommentLike(currentUserId(), commentId, active)); }

    @PostMapping("/posts/{postId}/like")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> like(@PathVariable @Positive Long postId, @RequestParam(defaultValue = "true") boolean active) { return getSuccessResponseVO(service.toggleLike(currentUserId(), postId, active)); }

    @PostMapping("/posts/{postId}/bookmark")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> bookmark(@PathVariable @Positive Long postId, @RequestParam(defaultValue = "true") boolean active) { return getSuccessResponseVO(service.toggleBookmark(currentUserId(), postId, active)); }

    @PostMapping("/posts/{postId}/action")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> action(@PathVariable @Positive Long postId, @RequestParam(required = false) String action) { return getSuccessResponseVO(service.setAction(currentUserId(), postId, action)); }

    @PostMapping("/users/{userId}")
    public ResponseVO<?> user(@PathVariable String userId) { return getSuccessResponseVO(service.userProfile(userId, currentUserId())); }

    @PostMapping("/users/{userId}/posts")
    public ResponseVO<?> userPosts(@PathVariable String userId, @RequestParam(required = false) @Min(1) @Max(100) Integer limit) { return getSuccessResponseVO(service.userPosts(userId, limit)); }

    @PostMapping("/users/{userId}/comments")
    public ResponseVO<?> userComments(@PathVariable String userId, @RequestParam(required = false) @Min(1) @Max(100) Integer limit) { return getSuccessResponseVO(service.userComments(userId, limit)); }

    @PostMapping("/users/me/bookmarks")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> bookmarks(@RequestParam(required = false) @Min(1) @Max(200) Integer limit) { return getSuccessResponseVO(service.bookmarkedPosts(currentUserId(), limit)); }

    @PostMapping("/users/me/profile")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> updateProfile(@RequestParam String nickName, @RequestParam String handle, @RequestParam(required = false) String avatar, @RequestParam(required = false) String coverUrl, @RequestParam(required = false) String bio, @RequestParam(required = false) String location, @RequestParam(required = false) String websiteUrl) {
        var profile = service.updateUserProfile(currentUserId(), nickName, handle, avatar, coverUrl, bio, location, websiteUrl);
        TokenUserInfoDTO token = getTokenUserInfo(); token.setNickName(profile.getNickName()); token.setAvatar(profile.getAvatar()); redisComponent.updateTokenInfo(token);
        return getSuccessResponseVO(profile);
    }

    @PostMapping("/users/{userId}/follow")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> follow(@PathVariable String userId, @RequestParam(defaultValue = "true") boolean active) { return getSuccessResponseVO(service.toggleFollow(currentUserId(), userId, active)); }

    @PostMapping("/events/{eventId}/attendance")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> attendance(@PathVariable @Positive Long eventId, @RequestParam(required = false) String status) { return getSuccessResponseVO(service.setAttendance(currentUserId(), eventId, status)); }

    private String currentUserId() { return getTokenUserInfo() == null ? null : getTokenUserInfo().getUserId(); }
}
