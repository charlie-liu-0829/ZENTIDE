package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.dto.ZentideChangeWatchRequest;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideChangeWatchService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/zentide/v1/changes")
@Validated
public class ZentideChangeWatchController extends ABaseController {
    private final ZentideChangeWatchService service;

    public ZentideChangeWatchController(ZentideChangeWatchService service) {
        this.service = service;
    }

    @PostMapping("/watching/list")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> list(@RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return getSuccessResponseVO(service.list(getTokenUserInfo().getUserId(), limit));
    }

    @PostMapping("/{changeId}/watch")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> watch(@PathVariable @Positive Long changeId, @Valid @ModelAttribute ZentideChangeWatchRequest request) {
        service.watch(getTokenUserInfo().getUserId(), changeId, request.watchMode());
        return getSuccessResponseVO(null);
    }

    @PostMapping("/{changeId}/unwatch")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> stopWatching(@PathVariable @Positive Long changeId) {
        service.stopWatching(getTokenUserInfo().getUserId(), changeId);
        return getSuccessResponseVO(null);
    }
}
