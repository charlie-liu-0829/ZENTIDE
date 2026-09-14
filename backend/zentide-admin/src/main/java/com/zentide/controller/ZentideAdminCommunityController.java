package com.zentide.controller;

import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideAdminCommunityService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/zentide/v1/admin/community")
@Validated
public class ZentideAdminCommunityController extends ABaseController {
    private final ZentideAdminCommunityService service;

    public ZentideAdminCommunityController(ZentideAdminCommunityService service) {
        this.service = service;
    }

    @PostMapping("/hubs/list")
    public ResponseVO<?> hubs(@RequestParam(required = false) String query,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) @Min(1) @Max(200) Integer limit) {
        getAdminAccount();
        return getSuccessResponseVO(service.listHubs(query, status, limit));
    }

    @PostMapping("/hubs/{hubId}/status")
    public ResponseVO<?> hubStatus(@PathVariable @Positive Long hubId, @RequestParam String status) {
        getAdminAccount();
        service.updateHubStatus(hubId, status);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/posts/list")
    public ResponseVO<?> posts(@RequestParam(required = false) String query,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) @Positive Long hubId,
                               @RequestParam(required = false) @Min(1) @Max(300) Integer limit) {
        getAdminAccount();
        return getSuccessResponseVO(service.listPosts(query, status, hubId, limit));
    }

    @PostMapping("/posts/{postId}/status")
    public ResponseVO<?> postStatus(@PathVariable @Positive Long postId, @RequestParam String status) {
        getAdminAccount();
        service.updatePostStatus(postId, status);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/comments/list")
    public ResponseVO<?> comments(@RequestParam(required = false) String query,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) @Positive Long hubId,
                                  @RequestParam(required = false) @Min(1) @Max(300) Integer limit) {
        getAdminAccount();
        return getSuccessResponseVO(service.listComments(query, status, hubId, limit));
    }

    @PostMapping("/comments/{commentId}/status")
    public ResponseVO<?> commentStatus(@PathVariable @Positive Long commentId, @RequestParam String status) {
        getAdminAccount();
        service.updateCommentStatus(commentId, status);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/topics/list")
    public ResponseVO<?> topics(@RequestParam(required = false) String query,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) @Min(1) @Max(300) Integer limit) {
        getAdminAccount();
        return getSuccessResponseVO(service.listTopics(query, status, limit));
    }

    @PostMapping("/topics/{topicId}/status")
    public ResponseVO<?> topicStatus(@PathVariable @Positive Long topicId, @RequestParam String status) {
        getAdminAccount();
        service.updateTopicStatus(topicId, status);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/overview")
    public ResponseVO<?> overview() {
        getAdminAccount();
        return getSuccessResponseVO(service.overview());
    }

    @PostMapping("/members/pending")
    public ResponseVO<?> pendingMembers(@RequestParam(required = false) @Min(1) @Max(300) Integer limit) {
        getAdminAccount();
        return getSuccessResponseVO(service.listPendingMembers(limit));
    }

    @PostMapping("/hubs/{hubId}/members/{userId}/status")
    public ResponseVO<?> memberStatus(@PathVariable @Positive Long hubId,
                                      @PathVariable String userId,
                                      @RequestParam String status) {
        getAdminAccount();
        service.reviewMember(hubId, userId, status);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/directions/list")
    public ResponseVO<?> directions() {
        getAdminAccount();
        return getSuccessResponseVO(service.listDirections());
    }

    @PostMapping("/directions/create")
    public ResponseVO<?> createDirection(@RequestParam String code,
                                         @RequestParam String displayName,
                                         @RequestParam(required = false) String description,
                                         @RequestParam(required = false) Integer sortOrder) {
        getAdminAccount();
        return getSuccessResponseVO(service.createDirection(code, displayName, description, sortOrder));
    }

    @PostMapping("/directions/{directionId}/update")
    public ResponseVO<?> updateDirection(@PathVariable @Positive Long directionId,
                                         @RequestParam String displayName,
                                         @RequestParam(required = false) String description,
                                         @RequestParam(defaultValue = "true") boolean active,
                                         @RequestParam(required = false) Integer sortOrder) {
        getAdminAccount();
        service.updateDirection(directionId, displayName, description, active, sortOrder);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/directions/{directionId}/delete")
    public ResponseVO<?> deleteDirection(@PathVariable @Positive Long directionId) {
        getAdminAccount();
        service.deleteDirection(directionId);
        return getSuccessResponseVO(null);
    }
}
