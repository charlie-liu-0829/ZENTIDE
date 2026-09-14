package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.exception.BusinessException;
import com.zentide.service.ZentideInterestCommunityService;
import com.zentide.service.ZentidePostReviewGatewayService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping({"/zentide/v1/posts", "/posts"})
public class ZentidePostReviewController extends ABaseController {
    private static final Logger LOG = LoggerFactory.getLogger(ZentidePostReviewController.class);
    private static final Set<String> CLIENT_EVENTS = Set.of(
            "similar_post_clicked", "suggested_title_accepted", "suggested_topic_accepted",
            "optimized_content_accepted");
    private final ZentidePostReviewGatewayService reviewGateway;
    private final ZentideInterestCommunityService communityService;

    public ZentidePostReviewController(ZentidePostReviewGatewayService reviewGateway,
                                       ZentideInterestCommunityService communityService) {
        this.reviewGateway = reviewGateway;
        this.communityService = communityService;
    }

    @PostMapping("/review")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> review(@Valid @RequestBody ReviewRequest request) {
        String userId = getTokenUserInfo().getUserId();
        long startedAt = System.nanoTime();
        LOG.info("event=review_started userId={} sceneId={}", userId, request.sceneId());
        Set<String> visibilities = communityService.agentVisibilities(userId, request.sceneId(), null);
        communityService.validateReviewTopics(request.sceneId(), request.selectedTopicIds());
        List<Map<String, Object>> availableTopics = communityService.topics(request.sceneId(), 50).stream()
                .map(topic -> Map.<String, Object>of("topic_id", topic.getTopicId(), "name", topic.getCanonicalName()))
                .toList();
        Map<String, Object> result = reviewGateway.review(
                userId, request.sceneId(), request.title(), request.content(), request.selectedPostType(),
                request.selectedTopicIds(), request.draftId(), visibilities, availableTopics);
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
        LOG.info("event=review_completed userId={} sceneId={} reviewId={} advice={} analysisMode={} duplicateProbability={} suggestedPostType={} durationMs={}",
                userId, request.sceneId(), result.get("review_id"), result.get("advice"),
                result.get("analysis_mode"), result.get("duplicate_probability"),
                result.get("suggested_post_type"), durationMs);
        return getSuccessResponseVO(result);
    }

    @PostMapping("/review/events")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Boolean> trackEvent(@Valid @RequestBody ReviewEventRequest request) {
        if (!CLIENT_EVENTS.contains(request.event())) throw new BusinessException("不支持的审核事件");
        String userId = getTokenUserInfo().getUserId();
        communityService.agentVisibilities(userId, request.sceneId(), null);
        reviewGateway.trackEvent(request.reviewId(), userId, request.sceneId(), request.event(), request.targetId());
        LOG.info("event={} userId={} reviewId={} targetId={}", request.event(),
                userId, request.reviewId(), request.targetId());
        return getSuccessResponseVO(true);
    }

    public record ReviewRequest(
            @NotNull @Positive Long sceneId,
            @NotBlank @Size(max = 220) String title,
            @NotBlank @Size(max = 50000) String content,
            @Size(max = 24) String selectedPostType,
            @Size(max = 20) List<@Positive Long> selectedTopicIds,
            @Size(max = 100) String draftId) {
        public ReviewRequest {
            selectedTopicIds = selectedTopicIds == null ? List.of() : List.copyOf(selectedTopicIds);
        }
    }

    public record ReviewEventRequest(
            @NotBlank @Size(max = 40) String event,
            @NotBlank @Size(max = 48) String reviewId,
            @NotNull @Positive Long sceneId,
            @Positive Long targetId) {
    }
}
