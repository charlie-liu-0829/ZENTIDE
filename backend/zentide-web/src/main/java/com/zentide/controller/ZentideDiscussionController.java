package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.dto.ZentideDiscussionCreateRequest;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideDiscussionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/zentide/v1/changes/{changeId}/discussions")
@Validated
public class ZentideDiscussionController extends ABaseController {
    private final ZentideDiscussionService service;

    public ZentideDiscussionController(ZentideDiscussionService service) {
        this.service = service;
    }
    //查找全部评论
    @PostMapping("/list")
    public ResponseVO<?> list(@PathVariable @Positive Long changeId, @RequestParam(required = false) @Min(1) @Max(200) Integer limit) {
        return getSuccessResponseVO(service.list(changeId, limit));
    }

    @PostMapping
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> publish(@PathVariable @Positive Long changeId, @Valid @ModelAttribute ZentideDiscussionCreateRequest request) {
        return getSuccessResponseVO(service.publish(getTokenUserInfo().getUserId(), changeId, request.parentDiscussionId(), request.evidenceId(), request.body()));
    }

    @PostMapping("/{discussionId}/remove")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> remove(@PathVariable @Positive Long discussionId) {
        service.remove(getTokenUserInfo().getUserId(), discussionId);
        return getSuccessResponseVO(null);
    }
}
