package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.dto.ZentideSourceApplicationRequest;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideTopicService;
import com.zentide.service.ZentideSourceApplicationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/zentide/v1/topics")
@Validated
public class ZentideTopicController extends ABaseController {
    private final ZentideTopicService service;
    private final ZentideSourceApplicationService sourceApplicationService;

    public ZentideTopicController(ZentideTopicService service, ZentideSourceApplicationService sourceApplicationService) {
        this.service = service;
        this.sourceApplicationService = sourceApplicationService;
    }

    @PostMapping("/discover")
    public ResponseVO discover(@RequestParam(required = false) String query, @RequestParam(required = false) Integer limit) {
        return getSuccessResponseVO(service.discover(query, limit));
    }

    @PostMapping("/{topicId}/sources/list")
    public ResponseVO<?> sources(@PathVariable @Positive Long topicId) {
        return getSuccessResponseVO(service.listSources(topicId));
    }

    @PostMapping("/{topicId}/source-applications")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> submitSourceApplication(@PathVariable @Positive Long topicId, @Valid @ModelAttribute ZentideSourceApplicationRequest request) {
        return getSuccessResponseVO(sourceApplicationService.submit(userId(), topicId, request.sourceName(), request.sourceType(), request.canonicalUrl()));
    }

    @PostMapping("/{topicId}/feed")
    public ResponseVO<?> feed(@PathVariable @Positive Long topicId, @RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return getSuccessResponseVO(service.feed(topicId, limit));
    }

    private String userId() {
        return getTokenUserInfo().getUserId();
    }
}
