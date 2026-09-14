package com.zentide.controller;

import com.zentide.controller.ABaseController;
import com.zentide.entity.dto.ZentideResearchRequest;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideFactQueryService;
import com.zentide.service.ZentideResearchService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/zentide/v1")
@Validated
public class ZentideFactController extends ABaseController {
    private final ZentideFactQueryService service;
    private final ZentideResearchService researchService;

    public ZentideFactController(ZentideFactQueryService service, ZentideResearchService researchService) {
        this.service = service;
        this.researchService = researchService;
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

    @PostMapping("/research/search")
    public ResponseVO<?> searchResearch(@Valid @ModelAttribute ZentideResearchRequest request, @RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return getSuccessResponseVO(researchService.search(request.query(), limit));
    }
}
