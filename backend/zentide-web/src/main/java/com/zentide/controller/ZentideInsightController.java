package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideInsightService;
import com.zentide.service.ZentideBriefingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/zentide/v1")
public class ZentideInsightController extends ABaseController {
    private final ZentideInsightService service;
    private final ZentideBriefingService briefingService;

    public ZentideInsightController(ZentideInsightService service, ZentideBriefingService briefingService) {
        this.service = service;
        this.briefingService = briefingService;
    }

    @PostMapping("/insights/list")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO listInsights(@RequestParam(required = false) Integer limit) {
        return getSuccessResponseVO(service.refreshAndList(ownerId(), limit));
    }

    @PostMapping("/insights/feedback")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO feedback(@RequestParam Long insightId, @RequestParam String feedbackType,
                               @RequestParam(required = false) String note) {
        service.feedback(ownerId(), insightId, feedbackType, note);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/briefings/daily")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO dailyBriefing() {
        return getSuccessResponseVO(briefingService.daily(ownerId()));
    }

    private String ownerId() {
        return getTokenUserInfo().getUserId();
    }
}
