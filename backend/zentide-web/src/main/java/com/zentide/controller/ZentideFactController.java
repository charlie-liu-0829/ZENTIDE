package com.zentide.controller;

import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideFactQueryService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/zentide/v1")
@Validated
public class ZentideFactController extends ABaseController {
    private final ZentideFactQueryService service;

    public ZentideFactController(ZentideFactQueryService service) {
        this.service = service;
    }

    @PostMapping("/changes/list")
    public ResponseVO<?> listChanges(@RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return getSuccessResponseVO(service.listChanges(limit));
    }

    @PostMapping("/changes/{changeId}")
    public ResponseVO<?> getChange(@PathVariable @Positive Long changeId) {
        return getSuccessResponseVO(service.getChange(changeId));
    }

    @PostMapping("/changes/{changeId}/evidence")
    public ResponseVO<?> listEvidence(@PathVariable @Positive Long changeId) {
        return getSuccessResponseVO(service.listEvidence(changeId));
    }

}
