package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.dto.ZentideRadarCreateRequest;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideRadarService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/zentide/v1/radars")
@Validated
public class ZentideRadarController extends ABaseController {
    private final ZentideRadarService service;

    public ZentideRadarController(ZentideRadarService service) {
        this.service = service;
    }

    @PostMapping("/list")
    @GlobalInterceptor(checkLogin=true)
    public ResponseVO list() {
        return getSuccessResponseVO(service.list(userId()));
    }

    @PostMapping
    @GlobalInterceptor(checkLogin=true)
    public ResponseVO<?> create(@Valid @ModelAttribute ZentideRadarCreateRequest request) {
        return getSuccessResponseVO(service.create(userId(), request));
    }

    @PostMapping("/{radarId}/plan")
    @GlobalInterceptor(checkLogin=true)
    public ResponseVO<?> update(@PathVariable Long radarId, @Valid @ModelAttribute ZentideRadarCreateRequest request) {
        return getSuccessResponseVO(service.update(userId(), radarId, request));
    }

    @PostMapping("/{radarId}/topics/{topicId}")
    @GlobalInterceptor(checkLogin=true)
    public ResponseVO<?> addTopic(@PathVariable @Positive Long radarId, @PathVariable @Positive Long topicId) {
        return getSuccessResponseVO(service.addTopic(userId(), radarId, topicId));
    }

    private String userId() {
        return getTokenUserInfo().getUserId();
    }
}
