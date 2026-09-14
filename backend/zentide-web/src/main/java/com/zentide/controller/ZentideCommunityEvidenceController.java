package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.dto.ZentideEvidenceSubmissionRequest;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideCommunityEvidenceService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/zentide/v1/changes/{changeId}/evidence-submissions")
public class ZentideCommunityEvidenceController extends ABaseController {
    private final ZentideCommunityEvidenceService service;

    public ZentideCommunityEvidenceController(ZentideCommunityEvidenceService service) {
        this.service = service;
    }

    @PostMapping
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<?> submit(@PathVariable @Positive Long changeId, @Valid @ModelAttribute ZentideEvidenceSubmissionRequest request) {
        return getSuccessResponseVO(service.submit(getTokenUserInfo().getUserId(), changeId, request.sourceUrl(), request.excerpt(), request.note()));
    }
}
