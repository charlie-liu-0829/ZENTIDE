package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.dto.ZentideChangeStanceRequest;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideChangeStanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/zentide/v1/changes/{changeId}/stances")
@Validated
public class ZentideChangeStanceController extends ABaseController {
    private final ZentideChangeStanceService service;

    public ZentideChangeStanceController(ZentideChangeStanceService service) {
        this.service = service;
    }

    @PostMapping("/summary")
    public ResponseVO<?> summary(@PathVariable @Positive Long changeId) {
        return getSuccessResponseVO(service.summary(changeId));
    }

    @PostMapping("/mine")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> mine(@PathVariable @Positive Long changeId) {
        return getSuccessResponseVO(service.mine(getTokenUserInfo().getUserId(), changeId));
    }

    @PostMapping
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> choose(@PathVariable @Positive Long changeId,
                                @Valid @ModelAttribute ZentideChangeStanceRequest request) {
        return getSuccessResponseVO(service.choose(getTokenUserInfo().getUserId(), changeId, request.stanceType()));
    }
}
