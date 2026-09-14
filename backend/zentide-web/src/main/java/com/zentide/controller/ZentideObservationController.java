package com.zentide.controller;

import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideObservationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/zentide/v1/observations")
@Validated
public class ZentideObservationController extends ABaseController {
    private final ZentideObservationService service;

    public ZentideObservationController(ZentideObservationService service) {
        this.service = service;
    }

    @PostMapping("/activities/list")
    public ResponseVO<?> list(@RequestParam(required = false) @Positive Long signalId,
                              @RequestParam(required = false) @Positive Long changeId,
                              @RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return getSuccessResponseVO(service.list(signalId, changeId, limit));
    }
}
